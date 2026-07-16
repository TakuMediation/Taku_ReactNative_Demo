package com.anythink.reactnative.nativead;


import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

/**
 * Native ViewManager（旧架构壳）：@ReactProp ×5 + getCommandsMap + 手写 receiveCommand（String/int 双重载）
 * 分发 updateAssetView / renderNativeAd，全部转发共享 Impl。与 newarch 壳逐项等价。
 */
@ReactModule(name = ATNativeAdViewManager.NAME)
public class ATNativeAdViewManager extends ViewGroupManager<ATNativeContainer> {

    public static final String NAME = "ATNativeAdView";

    @Override
    public String getName() {
        return NAME;
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

    @ReactProp(name = "placementID")
    public void setPlacementID(ATNativeContainer view, String value) {
        ATNativeAdViewManagerImpl.stateOf(view).placementId = value;
        ATNativeAdViewManagerImpl.maybeAttach(view);
    }

    @ReactProp(name = "adId")
    public void setAdId(ATNativeContainer view, String value) {
        ATNativeAdViewManagerImpl.stateOf(view).adId = value;
        view.adId = value;
        ATNativeAdViewManagerImpl.maybeAttach(view);
    }

    @ReactProp(name = "isAdaptiveHeight")
    public void setIsAdaptiveHeight(ATNativeContainer view, boolean value) {
        ATNativeAdViewManagerImpl.stateOf(view).adaptive = value;
    }

    @ReactProp(name = "adWidth")
    public void setAdWidth(ATNativeContainer view, int value) {
        ATNativeAdViewManagerImpl.stateOf(view).adWidth = value;
    }

    @ReactProp(name = "adHeight")
    public void setAdHeight(ATNativeContainer view, int value) {
        ATNativeAdViewManagerImpl.stateOf(view).adHeight = value;
    }

    // 旧架构命令名→id 注册（JS codegenNativeCommands fallback 走 UIManager.dispatchViewManagerCommand）
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("updateAssetView", 0, "renderNativeAd", 1);
    }

    // 旧架构不同 RN 版本可能下发 String 命令名或 int id，两个重载都实现，统一分发。
    @Override
    public void receiveCommand(ATNativeContainer root, String commandId, ReadableArray args) {
        dispatch(root, commandId, args);
    }

    @Override
    public void receiveCommand(ATNativeContainer root, int commandId, ReadableArray args) {
        dispatch(root, String.valueOf(commandId), args);
    }

    private void dispatch(ATNativeContainer root, String cmd, ReadableArray args) {
        switch (cmd) {
            case "updateAssetView":
            case "0":
                if (args != null) {
                    ATNativeAdViewManagerImpl.updateAssetView(root, args.getInt(0), args.getString(1));
                }
                break;
            case "renderNativeAd":
            case "1":
                ATNativeAdViewManagerImpl.renderNativeAd(root);
                break;
            default:
                break;
        }
    }
}
