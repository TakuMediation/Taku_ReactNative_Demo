package com.anythink.reactnative.nativead;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.anythink.nativead.api.ATNativeImageView;
import com.anythink.nativead.api.ATNativeMaterial;
import com.anythink.nativead.api.ATNativePrepareExInfo;
import com.anythink.nativead.api.ATNativePrepareInfo;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自渲染 native 重建（模型 A）：按 RN AssetView 的位置/尺寸/样式，用原生 view 重建一套素材，
 * 装进返回的 FrameLayout（不属于 RN 视图树）。供 ATNativeHelper 传给 SDK renderAdContainer，
 * 让 GDT 等需独占 NativeAdContainer 的 ADN 也能注册点击/曝光；快手等 ADN 同样适用。
 *
 * 文本/图 url 来自 SDK material；位置/尺寸/样式 measure 自 RN AssetView（换算到 container 坐标系）。
 */
public final class NativeSelfRenderRebuilder {
    private NativeSelfRenderRebuilder() {}

    /** 重建根的 tag 标记：重渲染前据此移除上一次的重建层，防多套重叠重影。 */
    public static final String REBUILD_ROOT_TAG = "at_selfrender_root";

    /** RN AssetView 相对 container 的位置/尺寸/样式（measure 结果）。 */
    static final class Box {
        int x, y, w, h;
        float textSizePx;
        int textColor;
        boolean hasText;
        boolean hasBg;
        int bgColor;
        int padL, padT, padR, padB;
        int gravity;
        boolean hasGravity;
    }

    /**
     * 读 RN AssetView 的背景色（让开发者写在 AssetView 上的 backgroundColor 被 native 重建还原，如 CTA 蓝底）。
     * RN 背景在不同版本/架构是不同 drawable：
     *  - ColorDrawable：旧/简单情况，直接 getColor。
     *  - CompositeBackgroundDrawable（RN 0.85 Fabric 复合背景）：纯色在内部 background:BackgroundDrawable 的 backgroundColor。
     *  - 其它带 getColor() 的：反射兜底。
     * 取不到（圆角/渐变等复杂背景）返回 0（透明），由调用方决定兜底。
     */
    static int readBgColor(android.graphics.drawable.Drawable bg) {
        if (bg == null) {
            return 0;
        }
        if (bg instanceof android.graphics.drawable.ColorDrawable) {
            return ((android.graphics.drawable.ColorDrawable) bg).getColor();
        }
        // CompositeBackgroundDrawable.getBackground() → BackgroundDrawable.getBackgroundColor()
        try {
            java.lang.reflect.Method getBg = bg.getClass().getMethod("getBackground");
            Object inner = getBg.invoke(bg);
            if (inner != null) {
                java.lang.reflect.Method getColor = inner.getClass().getMethod("getBackgroundColor");
                Object c = getColor.invoke(inner);
                if (c instanceof Integer) {
                    return (Integer) c;
                }
            }
        } catch (Throwable ignore) {
        }
        // 兜底：直接 getColor()
        try {
            java.lang.reflect.Method m = bg.getClass().getMethod("getColor");
            Object c = m.invoke(bg);
            if (c instanceof Integer) {
                return (Integer) c;
            }
        } catch (Throwable ignore) {
        }
        return 0;
    }

    /** measure 一个 RN AssetView 相对 container 的 box（含 TextView 样式 + 背景色 + padding）。w/h<=0 视为无效。 */
    static Box measure(View container, View asset) {
        Box b = new Box();
        if (asset == null) {
            return b;
        }
        b.w = asset.getWidth();
        b.h = asset.getHeight();
        int[] cLoc = new int[2];
        int[] aLoc = new int[2];
        container.getLocationInWindow(cLoc);
        asset.getLocationInWindow(aLoc);
        b.x = aLoc[0] - cLoc[0];
        b.y = aLoc[1] - cLoc[1];
        // 背景色：RN 的 backgroundColor 在新旧架构渲染成 ColorDrawable 或 ReactViewBackgroundDrawable/
        // CSSBackgroundDrawable（带 getColor()）。前者直接取，后者反射 getColor()，让开发者写在 AssetView
        // 上的 backgroundColor 能被还原（如 CTA 蓝底）。
        b.bgColor = readBgColor(asset.getBackground());
        b.hasBg = (b.bgColor >>> 24) != 0;
        b.padL = asset.getPaddingLeft();
        b.padT = asset.getPaddingTop();
        b.padR = asset.getPaddingRight();
        b.padB = asset.getPaddingBottom();
        if (asset instanceof TextView) {
            TextView tv = (TextView) asset;
            b.textSizePx = tv.getTextSize();
            b.textColor = tv.getCurrentTextColor();
            b.hasText = true;
            b.gravity = tv.getGravity();
            b.hasGravity = true;
        }
        return b;
    }

    /**
     * 重建素材。container 内已有 RN 渲染的 AssetView（findViewById 可取）。
     * @param container   ATNativeContainer（measure 基准 + 文本样式来源）
     * @param material    SDK 素材（文本/图 url 来源）
     * @param assetTags   react tag → 类型名（title/desc/cta/appIcon/mainImage/mediaView/dislike）
     * @param mediaView   SDK getAdMediaView 结果（可空；视频/大图 surface）
     * @param info        待填充的 prepareInfo（setXxxView）
     * @param clickViewsOut 收集的可点 view（最终 setClickViewList）
     * @return native 根 FrameLayout（绝对定位子 view），供 renderAdContainer 用
     */
    public static FrameLayout build(ATNativeContainer container,
                                    ATNativeMaterial material,
                                    Map<Integer, String> assetTags,
                                    Map<Integer, Integer> assetColors,
                                    View mediaView,
                                    ATNativePrepareInfo info,
                                    List<View> clickViewsOut) {
        Context ctx = container.getContext();
        FrameLayout root = new FrameLayout(ctx);
        root.setTag(REBUILD_ROOT_TAG); // 标记重建层，重渲染时据此移除旧的，防重叠重影
        View closeViewForFront = null;

        for (Map.Entry<Integer, String> e : assetTags.entrySet()) {
            Integer tag = e.getKey();
            String type = e.getValue();
            if (tag == null || type == null) {
                continue;
            }
            Integer jsColor = assetColors != null ? assetColors.get(tag) : null;
            View rnView = container.findViewById(tag);
            Box box = measure(container, rnView);
            // JS 传来的 style.color 优先于 measure 量到的色。
            if (jsColor != null) {
                box.textColor = jsColor;
                box.hasText = true;
            }
            if (box.w <= 0 || box.h <= 0) {
                MsgTools.printMsg("rebuild skip " + type + " (no size)");
                continue;
            }
            if (Const.Native.title.equals(type)) {
                TextView tv = newText(ctx, material.getTitle(), box);
                place(root, tv, box);
                info.setTitleView(tv);
                clickViewsOut.add(tv);
            } else if (Const.Native.desc.equals(type)) {
                TextView tv = newText(ctx, material.getDescriptionText(), box);
                place(root, tv, box);
                info.setDescView(tv);
                clickViewsOut.add(tv);
            } else if (Const.Native.cta.equals(type)) {
                // newText 已套 measure 到的背景(box.hasBg→RN backgroundColor)+ padding + 文字色。
                // RN AssetView 已隐藏，CTA 背景全靠 measure 还原（CompositeBackgroundDrawable 已能读色）。
                TextView tv = newText(ctx, material.getCallToActionText(), box);
                // JS 没传色、measure 又只量到默认半透明黑/0 时兜底白字，保证按钮文字清晰。
                if (jsColor == null && (box.textColor == 0x8a000000 || box.textColor == 0)) {
                    tv.setTextColor(0xffffffff);
                }
                place(root, tv, box);
                info.setCtaView(tv);
                clickViewsOut.add(tv);
            } else if (Const.Native.icon.equals(type)) {
                ATNativeImageView iv = newImage(ctx, material.getIconImageUrl(), box);
                if (iv != null) {
                    place(root, iv, box);
                    info.setIconView(iv);
                    clickViewsOut.add(iv);
                }
            } else if (Const.Native.mainImage.equals(type)) {
                ATNativeImageView iv = newImage(ctx, material.getMainImageUrl(), box);
                if (iv != null) {
                    place(root, iv, box);
                    info.setMainImageView(iv);
                    clickViewsOut.add(iv);
                }
            } else if (Const.Native.dislike.equals(type)) {
                View closeView = newClose(ctx, box);
                place(root, closeView, box);
                info.setCloseView(closeView);
                closeViewForFront = closeView;
                // 关闭按钮走 setCloseView + DislikeListener，不加入 clickViewList
            }
        }

        // media：SDK 原生 view 直接放进重建层对应位置（视频/大图 surface）
        if (mediaView != null) {
            View mv = mediaView;
            if (mv.getParent() instanceof ViewGroup) {
                ((ViewGroup) mv.getParent()).removeView(mv);
            }
            Box mbox = null;
            for (Map.Entry<Integer, String> e : assetTags.entrySet()) {
                if (Const.Native.mediaView.equals(e.getValue()) && e.getKey() != null) {
                    mbox = measure(container, container.findViewById(e.getKey()));
                    break;
                }
            }
            if (mbox != null && mbox.w > 0 && mbox.h > 0) {
                place(root, mv, mbox);
            } else {
                root.addView(mv, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            }
            clickViewsOut.add(mv);
        }

        // media 后 addView 会盖住先放的 close；关闭按钮必须在最上层才能点到
        if (closeViewForFront != null) {
            closeViewForFront.bringToFront();
        }

        clickViewsOut.add(root);
        info.setClickViewList(clickViewsOut);
        MsgTools.printMsg("rebuild done: clickViews=" + clickViewsOut.size());
        return root;
    }

    private static TextView newText(Context ctx, String text, Box box) {
        TextView tv = new TextView(ctx);
        tv.setText(text != null ? text : "");
        if (box.hasText) {
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, box.textSizePx);
            tv.setTextColor(box.textColor);
        }
        if (box.hasGravity) {
            tv.setGravity(box.gravity);
        }
        applyBgAndPadding(tv, box);
        return tv;
    }

    /** 还原 AssetView 自身的背景色 + padding（开发者把背景写在 AssetView 上才被重建）。 */
    private static void applyBgAndPadding(View v, Box box) {
        if (box.hasBg) {
            v.setBackgroundColor(box.bgColor);
        }
        v.setPadding(box.padL, box.padT, box.padR, box.padB);
    }

    private static ATNativeImageView newImage(Context ctx, String url, Box box) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        ATNativeImageView iv = new ATNativeImageView(ctx);
        iv.setLayoutParams(new FrameLayout.LayoutParams(box.w, box.h));
        // CENTER_CROP 填满不变形（否则方图拉伸/留边露底色）
        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        iv.setImage(url);
        return iv;
    }

    /** 关闭/dislike 按钮：按 RN ATNativeCloseView 的位置/尺寸重建，圆形底 + 系统 close 图标。 */
    private static View newClose(Context ctx, Box box) {
        android.widget.ImageView iv = new android.widget.ImageView(ctx);
        iv.setImageResource(resolveSystemCloseDrawable());
        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        int bgColor = box.hasBg ? box.bgColor : 0x66000000;
        applyCircularBackground(iv, bgColor);
        int pad = Math.max(2, Math.min(box.w, box.h) / 6);
        iv.setPadding(
                box.padL > 0 ? box.padL : pad,
                box.padT > 0 ? box.padT : pad,
                box.padR > 0 ? box.padR : pad,
                box.padB > 0 ? box.padB : pad);
        return iv;
    }

    /** 圆形半透明底（对齐 JS borderRadius: size/2；避免 setBackgroundColor 变方角）。 */
    private static void applyCircularBackground(View v, int color) {
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(color);
        v.setBackground(bg);
    }

    /** 系统 close 图标：优先 ic_clear，回退 ic_menu_close_clear_cancel。 */
    private static int resolveSystemCloseDrawable() {
        try {
            return android.R.drawable.class.getField("ic_clear").getInt(null);
        } catch (Throwable ignore) {
            return android.R.drawable.ic_menu_close_clear_cancel;
        }
    }

    /** 把重建的素材 view 按 measure 出的位置/尺寸绝对定位进重建根。 */
    private static void place(FrameLayout root, View v, Box box) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(box.w, box.h);
        lp.leftMargin = box.x;
        lp.topMargin = box.y;
        root.addView(v, lp);
    }
}
