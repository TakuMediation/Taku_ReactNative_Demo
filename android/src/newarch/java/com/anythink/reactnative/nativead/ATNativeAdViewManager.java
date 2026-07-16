package com.anythink.reactnative.nativead;


import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.ViewManagerDelegate;
import com.facebook.react.viewmanagers.ATNativeAdViewManagerDelegate;
import com.facebook.react.viewmanagers.ATNativeAdViewManagerInterface;

/**
 * Native Fabric ViewManager（新架构壳）：implements codegen Interface + Delegate；
 * props / 2 command（updateAssetView / renderNativeAd）转发共享 Impl。
 */
@ReactModule(name = ATNativeAdViewManager.NAME)
public class ATNativeAdViewManager extends ViewGroupManager<ATNativeContainer>
        implements ATNativeAdViewManagerInterface<ATNativeContainer> {

    public static final String NAME = "ATNativeAdView";

    private final ViewManagerDelegate<ATNativeContainer> mDelegate = new ATNativeAdViewManagerDelegate<>(this);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected ViewManagerDelegate<ATNativeContainer> getDelegate() {
        return mDelegate;
    }

    @Override
    protected ATNativeContainer createViewInstance(ThemedReactContext ctx) {
        return ATNativeAdViewManagerImpl.createView(ctx);
    }

    @Override
    protected void onAfterUpdateTransaction(ATNativeContainer view) {
        super.onAfterUpdateTransaction(view);
        ATNativeAdViewManagerImpl.onAfterUpdateTransaction(view);
    }

    @Override
    public void onDropViewInstance(ATNativeContainer view) {
        ATNativeAdViewManagerImpl.onDrop(view);
        super.onDropViewInstance(view);
    }

    @Override
    public void setPlacementID(ATNativeContainer view, String value) {
        ATNativeAdViewManagerImpl.stateOf(view).placementId = value;
        ATNativeAdViewManagerImpl.maybeAttach(view);
    }

    @Override
    public void setAdId(ATNativeContainer view, String value) {
        ATNativeAdViewManagerImpl.stateOf(view).adId = value;
        view.adId = value;
        ATNativeAdViewManagerImpl.maybeAttach(view);
    }

    @Override
    public void setIsAdaptiveHeight(ATNativeContainer view, boolean value) {
        ATNativeAdViewManagerImpl.stateOf(view).adaptive = value;
    }

    @Override
    public void setAdWidth(ATNativeContainer view, int value) {
        ATNativeAdViewManagerImpl.stateOf(view).adWidth = value;
    }

    @Override
    public void setAdHeight(ATNativeContainer view, int value) {
        ATNativeAdViewManagerImpl.stateOf(view).adHeight = value;
    }

    @Override
    public void updateAssetView(ATNativeContainer view, int assetViewTag, String assetViewName) {
        ATNativeAdViewManagerImpl.updateAssetView(view, assetViewTag, assetViewName);
    }

    @Override
    public void renderNativeAd(ATNativeContainer view) {
        ATNativeAdViewManagerImpl.renderNativeAd(view);
    }

    @Override
    public void receiveCommand(ATNativeContainer root, String commandId, ReadableArray args) {
        mDelegate.receiveCommand(root, commandId, args);
    }
}
