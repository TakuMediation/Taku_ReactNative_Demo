package com.anythink.reactnative.banner;

import android.widget.FrameLayout;


import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManagerDelegate;
import com.facebook.react.viewmanagers.ATBannerViewManagerDelegate;
import com.facebook.react.viewmanagers.ATBannerViewManagerInterface;

/**
 * Banner Fabric ViewManager（新架构壳）：implements codegen Interface，props 转发共享 Impl。
 */
@ReactModule(name = ATBannerViewManager.NAME)
public class ATBannerViewManager extends SimpleViewManager<FrameLayout>
        implements ATBannerViewManagerInterface<FrameLayout> {

    public static final String NAME = "ATBannerView";

    private final ViewManagerDelegate<FrameLayout> mDelegate = new ATBannerViewManagerDelegate<>(this);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected ViewManagerDelegate<FrameLayout> getDelegate() {
        return mDelegate;
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

    @Override
    public void setPlacementID(FrameLayout view, String value) {
        ATBannerViewManagerImpl.stateOf(view).placementId = value;
        ATBannerViewManagerImpl.maybeAttach(view);
    }

    @Override
    public void setIsAdaptiveHeight(FrameLayout view, boolean value) {
        ATBannerViewManagerImpl.stateOf(view).adaptive = value;
    }

    @Override
    public void setAdWidth(FrameLayout view, int value) {
        ATBannerViewManagerImpl.stateOf(view).adWidth = value;
        ATBannerViewManagerImpl.maybeAttach(view);
    }

    @Override
    public void setAdHeight(FrameLayout view, int value) {
        ATBannerViewManagerImpl.stateOf(view).adHeight = value;
        ATBannerViewManagerImpl.maybeAttach(view);
    }
}
