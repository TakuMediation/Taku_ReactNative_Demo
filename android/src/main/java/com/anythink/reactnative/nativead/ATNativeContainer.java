package com.anythink.reactnative.nativead;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import com.anythink.nativead.api.ATNativeAdView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Native 容器（共享，新旧架构通用）= SDK 埋点容器 ATNativeAdView 子类。
 * RN asset 子节点（Text/Image）天然渲染在其内；持 asset tag↔类型注册表 + 关联 Helper + 绑定的 adId。
 * 字段 public：供共享 Impl（com.anythink.reactnative.nativead.ATNativeAdViewManagerImpl）与 Helper 跨类访问。
 */
public class ATNativeContainer extends ATNativeAdView {
    /** asset 注册表：react tag → 类型名（"title"/"appIcon"/"mediaView"...）。LinkedHashMap 保序便于诊断。 */
    public final Map<Integer, String> assetTags = new LinkedHashMap<>();
    /** 文字类 asset 的 JS 侧 style.color：react tag → 颜色 int。自渲染重建直接用。 */
    public final Map<Integer, Integer> assetColors = new LinkedHashMap<>();
    public ATNativeHelper helper;
    /** 本视图绑定的广告实例引用（来自 JS 的 ad.adId）。 */
    public String adId;

    public ATNativeContainer(Context context) {
        super(context);
    }

    /** 自渲染重建生效后置 true：drawChild 跳过 RN 子树绘制，只画 native 子（GDT 容器/重建层）。 */
    public boolean selfRenderActive = false;

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        // 自渲染下：RN AssetView 嵌套层级不定（孙/曾孙）无法按 tag 跳过；重建层又被 renderAdContainer
        // 重挂进 GDT 容器（不再是直接子）。故按 class 区分：跳过 RN 渲染的直接子（com.facebook.react.*，
        // 即 AssetView 树根），保留 native 子（GDT NativeAdContainer / 重建层）继续画 → RN 不可见、重建层可见。
        if (selfRenderActive && child.getClass().getName().startsWith("com.facebook.react")) {
            return false;
        }
        return super.drawChild(canvas, child, drawingTime);
    }
}
