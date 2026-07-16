package com.anythink.reactnative.nativead;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.anythink.core.api.ATAdInfo;
import com.anythink.nativead.api.ATNative;
import com.anythink.nativead.api.ATNativePrepareInfo;
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
 * Native 命令式路由。一 placementId 一 {@link ATNativeHelper}（池化）；
 * Fabric ViewManager 与命令式共用同一 Helper（getOrCreateHelperForView）。
 */
public class ATNativeManager {

    private static volatile ATNativeManager sInstance;
    private final ConcurrentHashMap<String, ATNativeHelper> pidHelperMap = new ConcurrentHashMap<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private volatile ReactApplicationContext mReactContext;

    private ATNativeManager() {
    }

    public static ATNativeManager getInstance() {
        if (sInstance == null) {
            synchronized (ATNativeManager.class) {
                if (sInstance == null) {
                    sInstance = new ATNativeManager();
                }
            }
        }
        return sInstance;
    }

    public void setReactContext(ReactApplicationContext reactContext) {
        mReactContext = reactContext;
    }

    public ReactApplicationContext reactContext() {
        return mReactContext;
    }

    private Context appContext() {
        return mReactContext != null ? mReactContext.getApplicationContext() : null;
    }

    private ATNativeHelper getHelper(String placementId) {
        ATNativeHelper helper = pidHelperMap.get(placementId);
        if (helper == null) {
            helper = new ATNativeHelper(appContext(), placementId);
            pidHelperMap.put(placementId, helper);
        }
        return helper;
    }

    public ATNativeHelper getOrCreateHelperForView(String placementId) {
        return getHelper(placementId);
    }

    public void load(String placementId, ReadableMap settings) {
        if (TextUtils.isEmpty(placementId)) {
            MsgTools.printMsg("loadNativeAd: empty placementId");
            return;
        }
        MsgTools.printMsg("loadNativeAd placementId=" + placementId);
        getHelper(placementId).load(RNMapUtil.readableMapToHashMap(settings));
    }

    public void getNativeAd(String placementId, Promise promise) {
        ATNativeHelper helper = pidHelperMap.get(placementId);
        String adId = helper != null ? helper.fetchNativeAd() : null;
        MsgTools.printMsg("getNativeAd placementId=" + placementId + ", adId=" + adId);
        if (adId != null) {
            WritableMap map = Arguments.createMap();
            map.putBoolean("hasAd", true);
            // adId = 跨桥 NativeAd 对象引用，JS 透传、用户无感
            map.putString("adId", adId);
            // 回传 offer 类型：express(模板) → JS 隐藏 AssetView 让 SDK 画模板；否则自渲染走 AssetView
            map.putBoolean("isExpress", helper.isNativeExpress(adId));
            promise.resolve(map);
        } else {
            promise.resolve(null);
        }
    }

    /**
     * @deprecated 自渲染绑定改由 Fabric command（updateAssetView/renderNativeAd）驱动，
     * 不再走 prepareNativeAd bridge。保留空实现以兼容旧 spec 方法签名（L1 NativeAd.prepare 已弃用）。
     */
    @Deprecated
    public void prepare(final String placementId, final ReadableMap prepareInfo) {
        MsgTools.printMsg("prepareNativeAd is deprecated (use updateAssetView/renderNativeAd commands): " + placementId);
    }

    public WritableMap checkAdStatus(String placementId) {
        ATNativeHelper helper = pidHelperMap.get(placementId);
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
        ATNativeHelper helper = pidHelperMap.get(placementId);
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

    public void getAdMaterial(String placementId, String adId, Promise promise) {
        // 自渲染素材（title/desc/cta/iconUrl/mainImageUrl/starRating/advertiser/isMediaViewAvailable）→ JS AssetView 填值
        ATNativeHelper helper = pidHelperMap.get(placementId);
        Map<String, Object> material = helper != null ? helper.getAdMaterialMap(adId) : null;
        if (material == null) {
            promise.resolve(null);
            return;
        }
        promise.resolve(Arguments.makeNativeMap(material));
    }

    public void nativeAdOnResume(String placementId, String adId) {
        ATNativeHelper helper = pidHelperMap.get(placementId);
        if (helper != null) {
            helper.onResume(adId);
        }
    }

    public void nativeAdOnPause(String placementId, String adId) {
        ATNativeHelper helper = pidHelperMap.get(placementId);
        if (helper != null) {
            helper.onPause(adId);
        }
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

    public void entryScenario(String placementId, String scenario, ReadableMap tkExtra) {
        MsgTools.printMsg("entryNativeScenario: " + placementId + ", " + scenario);
        Map<String, Object> tk = RNMapUtil.readableMapToHashMap(tkExtra);
        if (tk == null || tk.isEmpty()) {
            ATNative.entryAdScenario(placementId, scenario);
        } else {
            ATNative.entryAdScenario(placementId, scenario, tk);
        }
    }

    /** 销毁一条广告（adId 空→最近一条）；不移除 Helper（同位还有其他条/可继续 getNativeAd）。 */
    public void destroy(String placementId, String adId) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        ATNativeHelper helper = pidHelperMap.get(placementId);
        if (helper != null) {
            helper.destroy(adId);
        }
    }

    /** 整位销毁（清 Helper 所有条 + 移除池）。 */
    public void destroyAll(String placementId) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        ATNativeHelper helper = pidHelperMap.remove(placementId);
        if (helper != null) {
            helper.destroyAll();
        }
    }
}
