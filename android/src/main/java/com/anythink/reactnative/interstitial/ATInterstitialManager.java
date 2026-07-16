package com.anythink.reactnative.interstitial;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATAdInfo;
import com.anythink.interstitial.api.ATInterstitial;
import com.anythink.reactnative.init.ATInitManager;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.RNMapUtil;
import com.anythink.reactnative.utils.Utils;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插屏路由。
 * 一 placementId 一 {@link ATInterstitialHelper}（池化）；load/show 失败经事件回传；未 init / 无 Activity 才 reject。
 */
public class ATInterstitialManager {

    private static volatile ATInterstitialManager sInstance;
    private final ConcurrentHashMap<String, ATInterstitialHelper> pidHelperMap = new ConcurrentHashMap<>();
    private volatile ReactApplicationContext mReactContext;

    private ATInterstitialManager() {
    }

    public static ATInterstitialManager getInstance() {
        if (sInstance == null) {
            synchronized (ATInterstitialManager.class) {
                if (sInstance == null) {
                    sInstance = new ATInterstitialManager();
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

    private ATInterstitialHelper getHelper(String placementId) {
        ATInterstitialHelper helper = pidHelperMap.get(placementId);
        if (helper == null) {
            helper = new ATInterstitialHelper(appContext());
            pidHelperMap.put(placementId, helper);
        }
        return helper;
    }

    public void load(String placementId, ReadableMap settings) {
        if (TextUtils.isEmpty(placementId)) {
            MsgTools.printMsg("loadInterstitial: empty placementId");
            return;
        }
        Map<String, Object> map = RNMapUtil.readableMapToHashMap(settings);
        getHelper(placementId).loadInterstitial(placementId, map, currentActivity());
    }

    public void show(String placementId, String scenario, Promise promise) {
        if (!ATInitManager.getInstance().isInitialized()) {
            promise.reject(Const.BridgeError.SDK_NOT_INITIALIZED, "SDK not initialized");
            return;
        }
        Activity activity = currentActivity();
        if (activity == null || activity.isFinishing()) {
            promise.reject(Const.BridgeError.ACTIVITY_NULL, "No foreground Activity");
            return;
        }
        ATInterstitialHelper helper = pidHelperMap.get(placementId);
        if (helper == null) {
            promise.reject(Const.BridgeError.INVALID_PLACEMENT, "No ad for placementId=" + placementId);
            return;
        }
        helper.show(activity, scenario);
        promise.resolve(null);
    }

    public void showWithConfig(String placementId, ReadableMap showConfig, Promise promise) {
        if (!ATInitManager.getInstance().isInitialized()) {
            promise.reject(Const.BridgeError.SDK_NOT_INITIALIZED, "SDK not initialized");
            return;
        }
        Activity activity = currentActivity();
        if (activity == null || activity.isFinishing()) {
            promise.reject(Const.BridgeError.ACTIVITY_NULL, "No foreground Activity");
            return;
        }
        ATInterstitialHelper helper = pidHelperMap.get(placementId);
        if (helper == null) {
            promise.reject(Const.BridgeError.INVALID_PLACEMENT, "No ad for placementId=" + placementId);
            return;
        }
        Map<String, Object> cfg = RNMapUtil.readableMapToHashMap(showConfig);
        String scenario = cfg.containsKey("scenarioId") ? String.valueOf(cfg.get("scenarioId"))
                : (cfg.containsKey(Const.SCENE_ID) ? String.valueOf(cfg.get(Const.SCENE_ID)) : "");
        String showCustomExt = cfg.containsKey(Const.SHOW_CUSTOM_EXT) ? String.valueOf(cfg.get(Const.SHOW_CUSTOM_EXT)) : null;
        Object atCustom = cfg.get(Const.AT_CUSTOM_CONTENT_RESULT);
        helper.showConfig(activity, scenario, showCustomExt, atCustom);
        promise.resolve(null);
    }

    public boolean isAdReady(String placementId) {
        ATInterstitialHelper helper = pidHelperMap.get(placementId);
        return helper != null && helper.isAdReady();
    }

    public WritableMap checkAdStatus(String placementId) {
        ATInterstitialHelper helper = pidHelperMap.get(placementId);
        Map<String, Object> map;
        if (helper != null) {
            map = helper.checkAdStatus();
        } else {
            map = new HashMap<>(2);
            map.put("isLoading", false);
            map.put("isReady", false);
        }
        return Arguments.makeNativeMap(map);
    }

    public void getValidAds(String placementId, Promise promise) {
        WritableArray array = Arguments.createArray();
        ATInterstitialHelper helper = pidHelperMap.get(placementId);
        if (helper != null) {
            List<ATAdInfo> ads = helper.validAdCaches();
            if (ads != null) {
                for (ATAdInfo ad : ads) {
                    try {
                        array.pushMap(Arguments.makeNativeMap(Utils.jsonStrToMap(ad.toString())));
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
        MsgTools.printMsg("entryInterstitialScenario: " + placementId + ", " + scenario);
        Map<String, Object> tk = tkExtra != null ? RNMapUtil.readableMapToHashMap(tkExtra) : null;
        if (tk != null && !tk.isEmpty()) {
            ATInterstitial.entryAdScenario(placementId, scenario, tk);
        } else {
            ATInterstitial.entryAdScenario(placementId, scenario);
        }
    }

    // —— AutoLoad ——

    public void addAutoLoad(String placementId, ReadableMap settings) {
        ATAutoLoadInterstitialHelper.getInstance()
                .autoLoad(placementId, RNMapUtil.readableMapToHashMap(settings), currentActivity());
    }

    public void removeAutoLoad(String placementId) {
        ATAutoLoadInterstitialHelper.getInstance().removePlacement(placementId);
    }

    public boolean autoLoadReady(String placementId) {
        return ATAutoLoadInterstitialHelper.getInstance().isAdReady(placementId);
    }

    public void showAutoLoad(String placementId, String scenario, Promise promise) {
        if (!ATInitManager.getInstance().isInitialized()) {
            promise.reject(Const.BridgeError.SDK_NOT_INITIALIZED, "SDK not initialized");
            return;
        }
        Activity activity = currentActivity();
        if (activity == null || activity.isFinishing()) {
            promise.reject(Const.BridgeError.ACTIVITY_NULL, "No foreground Activity");
            return;
        }
        Map<String, Object> configMap = parseAutoLoadScenarioConfig(scenario);
        if (configMap != null) {
            ATAutoLoadInterstitialHelper.getInstance().showWithConfigMap(
                    placementId, configMap, activity);
        } else {
            ATAutoLoadInterstitialHelper.getInstance().show(placementId, scenario, activity);
        }
        promise.resolve(null);
    }

    public void showAutoLoadWithConfig(String placementId, ReadableMap showConfig, Promise promise) {
        if (!ATInitManager.getInstance().isInitialized()) {
            promise.reject(Const.BridgeError.SDK_NOT_INITIALIZED, "SDK not initialized");
            return;
        }
        Activity activity = currentActivity();
        if (activity == null || activity.isFinishing()) {
            promise.reject(Const.BridgeError.ACTIVITY_NULL, "No foreground Activity");
            return;
        }
        ATAutoLoadInterstitialHelper.getInstance().showWithConfigMap(
                placementId, RNMapUtil.readableMapToHashMap(showConfig), activity);
        promise.resolve(null);
    }

    /** scenario 为 showConfig JSON 时解析为 Map（兼容旧桥无 showAutoLoadInterstitialWithConfig）。 */
    private static Map<String, Object> parseAutoLoadScenarioConfig(String scenario) {
        if (TextUtils.isEmpty(scenario)) {
            return null;
        }
        String trimmed = scenario.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        try {
            return Utils.jsonStrToMap(trimmed);
        } catch (JSONException e) {
            return null;
        }
    }
}
