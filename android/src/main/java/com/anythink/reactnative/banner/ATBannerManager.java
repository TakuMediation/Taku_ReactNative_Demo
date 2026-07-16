package com.anythink.reactnative.banner;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.anythink.banner.api.ATBannerView;
import com.anythink.core.api.ATAdInfo;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.RNMapUtil;
import com.anythink.reactnative.utils.Utils;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Banner 命令式路由。一 placementId 一 {@link ATBannerHelper}（池化）；
 * Fabric ViewManager 与命令式桥共用同一 Helper（getOrCreateHelperForView）。
 */
public class ATBannerManager {

    private static volatile ATBannerManager sInstance;
    private final ConcurrentHashMap<String, ATBannerHelper> pidHelperMap = new ConcurrentHashMap<>();
    private volatile ReactApplicationContext mReactContext;

    private ATBannerManager() {
    }

    public static ATBannerManager getInstance() {
        if (sInstance == null) {
            synchronized (ATBannerManager.class) {
                if (sInstance == null) {
                    sInstance = new ATBannerManager();
                }
            }
        }
        return sInstance;
    }

    public void setReactContext(ReactApplicationContext reactContext) {
        mReactContext = reactContext;
    }

    public Activity currentActivity() {
        return mReactContext != null ? mReactContext.getCurrentActivity() : null;
    }

    private Context appContext() {
        return mReactContext != null ? mReactContext.getApplicationContext() : null;
    }

    private ATBannerHelper getHelper(String placementId) {
        ATBannerHelper helper = pidHelperMap.get(placementId);
        if (helper == null) {
            helper = new ATBannerHelper(appContext(), placementId);
            pidHelperMap.put(placementId, helper);
        }
        return helper;
    }

    /** Fabric ViewManager 取同一 Helper（与命令式共池）。 */
    public ATBannerHelper getOrCreateHelperForView(String placementId) {
        return getHelper(placementId);
    }

    public void load(String placementId, ReadableMap settings) {
        if (TextUtils.isEmpty(placementId)) {
            MsgTools.printMsg("loadBannerAd: empty placementId");
            return;
        }
        MsgTools.printMsg("loadBannerAd placementId=" + placementId);
        Map<String, Object> map = RNMapUtil.readableMapToHashMap(settings);
        getHelper(placementId).load(map, currentActivity());
    }

    public WritableMap checkAdStatus(String placementId) {
        ATBannerHelper helper = pidHelperMap.get(placementId);
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
        ATBannerHelper helper = pidHelperMap.get(placementId);
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

    public void setShowConfig(String placementId, ReadableMap map) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        getHelper(placementId).setShowConfig(RNMapUtil.readableMapToHashMap(map));
    }

    public void setTKExtra(String placementId, ReadableMap map) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        getHelper(placementId).setTKExtra(RNMapUtil.readableMapToHashMap(map));
    }

    /** 场景入口上报（与 Reward/Native 一致，调 SDK static entryAdScenario）。 */
    public void entryScenario(String placementId, String scenario, ReadableMap tkExtra) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        MsgTools.printMsg("entryBannerScenario: " + placementId + ", " + scenario);
        Map<String, Object> tk = tkExtra != null ? RNMapUtil.readableMapToHashMap(tkExtra) : null;
        if (tk != null && !tk.isEmpty()) {
            ATBannerView.entryAdScenario(placementId, scenario, tk);
        } else {
            ATBannerView.entryAdScenario(placementId, scenario);
        }
    }

    public void destroy(String placementId) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        ATBannerHelper helper = pidHelperMap.remove(placementId);
        if (helper != null) {
            helper.destroy();
        }
    }
}
