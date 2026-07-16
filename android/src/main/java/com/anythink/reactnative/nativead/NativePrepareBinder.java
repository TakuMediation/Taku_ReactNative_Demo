package com.anythink.reactnative.nativead;

import android.view.View;
import android.view.ViewGroup;

import com.anythink.nativead.api.ATNativePrepareExInfo;
import com.anythink.nativead.api.ATNativePrepareInfo;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自渲染绑定：JS 经 command 注册的 asset {react tag → 类型}，
 * native `container.findViewById(tag)`（Fabric 下 view.id==react tag）取到 RN 渲染好的 view，
 * 按类型 setXxxView + 全部收进 clickViewList → SDK 据此做点击/曝光（广告源 registerViewForInteraction）。
 *
 * 注：RN view 由 RN 自己布局，这里只注册引用、不 re-parent，故无任何 relayout 需求。
 */
public final class NativePrepareBinder {

    private NativePrepareBinder() {
    }

    /**
     * @param container  ATNativeAdView 容器（asset 子节点在其内，findViewById 取得到）
     * @param assetTags  react tag → 类型名（Const.Native.* 值：title/desc/cta/appIcon/mainImage/mediaView...）
     * @param mediaView  SDK 的 getAdMediaView() 结果（已被 addView 进 mediaView 占位的，加入 clickViewList）；可空
     */
    public static ATNativePrepareInfo build(ViewGroup container, Map<Integer, String> assetTags, View mediaView) {
        ATNativePrepareInfo info = new ATNativePrepareExInfo();
        List<View> clickViews = new ArrayList<>();
        if (container == null || assetTags == null) {
            return info;
        }

        for (Map.Entry<Integer, String> e : assetTags.entrySet()) {
            int tag = e.getKey() != null ? e.getKey() : 0;
            String type = e.getValue();
            if (tag <= 0 || type == null) {
                continue;
            }
            View v = container.findViewById(tag);
            MsgTools.printMsg("bind " + type + " tag=" + tag + " -> "
                    + (v != null ? v.getClass().getSimpleName() : "null"));
            if (v == null) {
                continue;
            }
            bindByType(info, type, v);
            // 关闭按钮只走 setCloseView，不进 clickViewList
            if (!Const.Native.dislike.equals(type)) {
                clickViews.add(v);
            }
        }

        // SDK mediaView（视频/大图 surface）也是可点区域
        if (mediaView != null) {
            clickViews.add(mediaView);
        }
        // 容器自身加入（整块可点，兜底曝光区域）
        clickViews.add(container);

        info.setClickViewList(clickViews);
        MsgTools.printMsg("clickViewList size=" + clickViews.size());
        return info;
    }

    private static void bindByType(ATNativePrepareInfo info, String type, View v) {
        if (Const.Native.title.equals(type)) {
            info.setTitleView(v);
        } else if (Const.Native.desc.equals(type)) {
            info.setDescView(v);
        } else if (Const.Native.cta.equals(type)) {
            info.setCtaView(v);
        } else if (Const.Native.icon.equals(type)) {
            info.setIconView(v);
        } else if (Const.Native.mainImage.equals(type)) {
            info.setMainImageView(v);
        } else if (Const.Native.adLogo.equals(type)) {
            info.setAdFromView(v);
        } else if (Const.Native.dislike.equals(type)) {
            info.setCloseView(v);
        }
        // mediaView 类型不 setXxxView（SDK view 由 getAdMediaView 提供并 addView），仅作 clickView
    }
}
