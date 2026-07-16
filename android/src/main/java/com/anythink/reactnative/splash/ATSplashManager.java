package com.anythink.reactnative.splash;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.anythink.reactnative.init.ATInitManager;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.RNMapUtil;
import com.anythink.splashad.api.ATSplashAd;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开屏路由。一 placementId 一 {@link ATSplashHelper}（池化）。
 * load 失败/超时经事件回传；未 init / 无 Activity 才 reject。show 容器高度可配（heightRatio/heightPx）。
 */
public class ATSplashManager {

    private static volatile ATSplashManager sInstance;
    private final ConcurrentHashMap<String, ATSplashHelper> pidHelperMap = new ConcurrentHashMap<>();
    private volatile ReactApplicationContext mReactContext;

    private ATSplashManager() {
    }

    public static ATSplashManager getInstance() {
        if (sInstance == null) {
            synchronized (ATSplashManager.class) {
                if (sInstance == null) {
                    sInstance = new ATSplashManager();
                }
            }
        }
        return sInstance;
    }

    public void setReactContext(ReactApplicationContext reactContext) {
        mReactContext = reactContext;
    }

    private Activity currentActivity() {
        return mReactContext != null ? mReactContext.getCurrentActivity() : null;
    }

    private Context appContext() {
        return mReactContext != null ? mReactContext.getApplicationContext() : null;
    }

    private ATSplashHelper getHelper(String placementId) {
        ATSplashHelper helper = pidHelperMap.get(placementId);
        if (helper == null) {
            helper = new ATSplashHelper(appContext());
            pidHelperMap.put(placementId, helper);
        }
        return helper;
    }

    public void load(String placementId, ReadableMap settings) {
        if (TextUtils.isEmpty(placementId)) {
            MsgTools.printMsg("loadSplash: empty placementId");
            return;
        }
        Map<String, Object> map = RNMapUtil.readableMapToHashMap(settings);
        getHelper(placementId).loadSplash(placementId, map, currentActivity());
    }

    public void show(String placementId, String scenario, Promise promise) {
        showInternal(placementId, scenario, -1, null, null, promise);
    }

    public void showWithConfig(String placementId, ReadableMap showConfig, Promise promise) {
        Map<String, Object> cfg = RNMapUtil.readableMapToHashMap(showConfig);
        String scenario = scenarioOf(cfg);
        String showCustomExt = cfg.containsKey(Const.SHOW_CUSTOM_EXT) ? String.valueOf(cfg.get(Const.SHOW_CUSTOM_EXT)) : null;
        Object atCustom = cfg.get(Const.AT_CUSTOM_CONTENT_RESULT);
        int heightPx = resolveHeightPx(cfg);
        showInternal(placementId, scenario, heightPx, showCustomExt, atCustom, promise);
    }

    /** ATShowConfig.scenarioId（兜底旧 sceneID）。 */
    private static String scenarioOf(Map<String, Object> cfg) {
        if (cfg.containsKey("scenarioId")) {
            return String.valueOf(cfg.get("scenarioId"));
        }
        if (cfg.containsKey(Const.SCENE_ID)) {
            return String.valueOf(cfg.get(Const.SCENE_ID));
        }
        return "";
    }

    private void showInternal(String placementId, String scenario, int heightPx,
                              String showCustomExt, Object atCustom, Promise promise) {
        if (!ATInitManager.getInstance().isInitialized()) {
            promise.reject(Const.BridgeError.SDK_NOT_INITIALIZED, "SDK not initialized");
            return;
        }
        Activity activity = currentActivity();
        if (activity == null || activity.isFinishing()) {
            promise.reject(Const.BridgeError.ACTIVITY_NULL, "No foreground Activity");
            return;
        }
        ATSplashHelper helper = pidHelperMap.get(placementId);
        if (helper == null) {
            promise.reject(Const.BridgeError.INVALID_PLACEMENT, "No ad for placementId=" + placementId);
            return;
        }
        helper.show(activity, scenario, heightPx, showCustomExt, atCustom);
        promise.resolve(null);
    }

    /** heightRatio(0~1) 优先；否则 heightPx(dp)→px；都没有返回 -1（全屏）。 */
    private int resolveHeightPx(Map<String, Object> cfg) {
        Activity activity = currentActivity();
        if (activity == null) {
            return -1;
        }
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        try {
            if (cfg.containsKey("heightRatio")) {
                double ratio = Double.parseDouble(String.valueOf(cfg.get("heightRatio")));
                if (ratio > 0 && ratio <= 1) {
                    return (int) (dm.heightPixels * ratio);
                }
            }
            if (cfg.containsKey("heightPx")) {
                double dp = Double.parseDouble(String.valueOf(cfg.get("heightPx")));
                if (dp > 0) {
                    return (int) (dp * dm.density);
                }
            }
        } catch (Throwable ignore) {
        }
        return -1;
    }

    public boolean isAdReady(String placementId) {
        ATSplashHelper helper = pidHelperMap.get(placementId);
        return helper != null && helper.isAdReady();
    }

    public boolean isAdReadyOf(String placementId) {
        return isAdReady(placementId);
    }

    public com.facebook.react.bridge.WritableMap checkAdStatus(String placementId) {
        ATSplashHelper helper = pidHelperMap.get(placementId);
        Map<String, Object> map;
        if (helper != null) {
            map = helper.checkAdStatus();
        } else {
            map = new java.util.HashMap<>(2);
            map.put("isLoading", false);
            map.put("isReady", false);
        }
        return com.facebook.react.bridge.Arguments.makeNativeMap(map);
    }

    public void getValidAds(String placementId, Promise promise) {
        com.facebook.react.bridge.WritableArray array = com.facebook.react.bridge.Arguments.createArray();
        ATSplashHelper helper = pidHelperMap.get(placementId);
        if (helper != null) {
            java.util.List<com.anythink.core.api.ATAdInfo> ads = helper.validAdCaches();
            if (ads != null) {
                for (com.anythink.core.api.ATAdInfo ad : ads) {
                    try {
                        array.pushMap(com.facebook.react.bridge.Arguments.makeNativeMap(
                                com.anythink.reactnative.utils.Utils.jsonStrToMap(ad.toString())));
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
        promise.resolve(array);
    }

    public void setLocalExtra(String placementId, ReadableMap map) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        getHelper(placementId).setLocalExtra(RNMapUtil.readableMapToHashMap(map));
    }

    public void setTKExtra(String placementId, ReadableMap map) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        getHelper(placementId).setTKExtra(RNMapUtil.readableMapToHashMap(map));
    }

    public void entryScenario(String placementId, String scenario, ReadableMap tkExtra) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        MsgTools.printMsg("entrySplashScenario: " + placementId + ", " + scenario);
        Map<String, Object> tk = tkExtra != null ? RNMapUtil.readableMapToHashMap(tkExtra) : null;
        if (tk != null && !tk.isEmpty()) {
            ATSplashAd.entryAdScenario(placementId, scenario, tk);
        } else {
            ATSplashAd.entryAdScenario(placementId, scenario);
        }
    }
}
