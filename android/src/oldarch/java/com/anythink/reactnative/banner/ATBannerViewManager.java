package com.anythink.reactnative.banner;

import android.widget.FrameLayout;


import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

/**
 * Banner ViewManager（旧架构壳）：@ReactProp ×4 转发共享 Impl（无 command）。
 * 与 newarch 壳逐 prop 等价，仅 props 暴露方式不同（@ReactProp vs codegen Interface @Override）。
 */
@ReactModule(name = ATBannerViewManager.NAME)
public class ATBannerViewManager extends SimpleViewManager<FrameLayout> {

    public static final String NAME = "ATBannerView";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected FrameLayout createViewInstance(ThemedReactContext ctx) {
        return ATBannerViewManagerImpl.createView(ctx);
    }

    @Override
    protected void onAfterUpdateTransaction(FrameLayout view) {
        super.onAfterUpdateTransaction(view);
        ATBannerViewManagerImpl.onAfterUpdateTransaction(view);
    }

    @Override
    public void onDropViewInstance(FrameLayout view) {
        ATBannerViewManagerImpl.onDrop(view);
        super.onDropViewInstance(view);
    }

    @ReactProp(name = "placementID")
    public void setPlacementID(FrameLayout view, String value) {
        ATBannerViewManagerImpl.stateOf(view).placementId = value;
        ATBannerViewManagerImpl.maybeAttach(view);
    }

    @ReactProp(name = "isAdaptiveHeight")
    public void setIsAdaptiveHeight(FrameLayout view, boolean value) {
        ATBannerViewManagerImpl.stateOf(view).adaptive = value;
    }

    @ReactProp(name = "adWidth")
    public void setAdWidth(FrameLayout view, int value) {
        ATBannerViewManagerImpl.stateOf(view).adWidth = value;
        ATBannerViewManagerImpl.maybeAttach(view);
    }

    @ReactProp(name = "adHeight")
    public void setAdHeight(FrameLayout view, int value) {
        ATBannerViewManagerImpl.stateOf(view).adHeight = value;
        ATBannerViewManagerImpl.maybeAttach(view);
    }
}
