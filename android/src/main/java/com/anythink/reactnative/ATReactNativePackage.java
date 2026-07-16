package com.anythink.reactnative;

import com.anythink.reactnative.banner.ATBannerViewManager;
import com.anythink.reactnative.nativead.ATNativeAdViewManager;
import com.anythink.reactnative.bridge.ATReactNativeBridgeModule;
import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RN 插件入口
 * 注册唯一的 TurboModule 入口 {@link ATReactNativeBridgeModule}。
 */
public class ATReactNativePackage extends TurboReactPackage {

  @Override
  public NativeModule getModule(String name, ReactApplicationContext reactContext) {
    if (name.equals(ATReactNativeBridgeModule.NAME)) {
      return new ATReactNativeBridgeModule(reactContext);
    }
    return null;
  }

  @Override
  public ReactModuleInfoProvider getReactModuleInfoProvider() {
    return new ReactModuleInfoProvider() {
      @Override
      public Map<String, ReactModuleInfo> getReactModuleInfos() {
        Map<String, ReactModuleInfo> map = new HashMap<String, ReactModuleInfo>();
        boolean isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;
        map.put(
            ATReactNativeBridgeModule.NAME,
            new ReactModuleInfo(
                ATReactNativeBridgeModule.NAME, // name
                ATReactNativeBridgeModule.NAME, // className
                false, // canOverrideExistingModule
                false, // needsEagerInit
                true, // hasConstants
                false, // isCxxModule
                isTurboModule // isTurboModule
            ));
        return map;
      }
    };
  }

  /** Fabric 视图：注册 Banner + Native ViewManager。 */
  @Override
  public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
    List<ViewManager> managers = new ArrayList<>();
    managers.add(new ATBannerViewManager());
    managers.add(new ATNativeAdViewManager());
    return managers;
  }
}
