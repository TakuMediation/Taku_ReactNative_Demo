package com.anythink.reactnative.init;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.anythink.core.api.ATAdFilter;
import com.anythink.core.api.ATAreaCallback;
import com.anythink.core.api.ATGDPRConsentDismissListener;
import com.anythink.core.api.ATSDK;
import com.anythink.core.api.ATSDKInitListener;
import com.anythink.core.api.ATSharedPlacementConfig;
import com.anythink.core.api.NetTrafficeCallback;
import com.anythink.debug.api.ATDebuggerUITest;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.utils.BridgeJsonMapUtil;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.RNMapUtil;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Init / Core 路由。
 * 仅做转发，调用真实 `com.anythink.core.api.ATSDK`。Context/Activity 桥内获取。
 */
public class ATInitManager {

    private static volatile ATInitManager sInstance;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private volatile ReactApplicationContext mReactContext;
    private volatile boolean mInitialized = false;

    /** init 是否已调用（激励等广告桥方法的 SDK_NOT_INITIALIZED 门控用）。 */
    public boolean isInitialized() {
        return mInitialized;
    }

    private ATInitManager() {
    }

    public static ATInitManager getInstance() {
        if (sInstance == null) {
            synchronized (ATInitManager.class) {
                if (sInstance == null) {
                    sInstance = new ATInitManager();
                }
            }
        }
        return sInstance;
    }

    public void setReactContext(ReactApplicationContext reactContext) {
        mReactContext = reactContext;
    }

    private Context appContext() {
        return mReactContext != null ? mReactContext.getApplicationContext() : null;
    }

    private Activity currentActivity() {
        return mReactContext != null ? mReactContext.getCurrentActivity() : null;
    }

    // —— 初始化 / 版本 ——

    public void initAnyThinkSDK(String appId, String appKey) {
        MsgTools.printMsg("initAnyThinkSDK appId=" + appId);
        mInitialized = true;
        // 开发框架标识：上报 os_fw=5（React Native）
        ATSDK.setSystemDevFragmentType("5");
        Context ctx = appContext();
        if (ctx == null) {
            return;
        }
        // init 完成经 InitCallName 事件回传 JS（点亮上行事件通道；P1-F06）
        ATSDK.init(ctx, appId, appKey, null, new ATSDKInitListener() {
            @Override
            public void onSuccess() {
                MsgTools.printMsg("init onSuccess");
                ATReactNativeEventEmitter.getInstance().sendCallback(
                        Const.CallbackMethodCall.InitCallName,
                        Const.InitCallback.sdkInitSuccessCallbackKey, "", null, null, null);
            }

            @Override
            public void onFail(String errorMsg) {
                MsgTools.printMsg("init onFail: " + errorMsg);
                ATReactNativeEventEmitter.getInstance().sendCallback(
                        Const.CallbackMethodCall.InitCallName,
                        Const.InitCallback.sdkInitFailCallbackKey, "", null, errorMsg, null);
            }
        });
    }

    public void start() {
        MsgTools.printMsg("start");
        ATSDK.start();
    }

    public String getSDKVersionName() {
        return ATSDK.getSDKVersionName();
    }

    // —— 日志 / 渠道 / 自定义参数 ——

    public void setLogEnabled(boolean enabled) {
        // 日志双开：先开桥层 MsgTools（确保下面这行可见），再开 SDK 网络日志
        MsgTools.setLogDebug(enabled);
        ATSDK.setNetworkLogDebug(enabled);
        // 回读 SDK 的 flag，证明确实写进了 SDKContext
        MsgTools.printMsg("setLogEnabled " + enabled
                + " -> ATSDK.isNetworkLogDebug()=" + ATSDK.isNetworkLogDebug());
    }

    public void setChannelStr(String channel) {
        MsgTools.printMsg("setChannelStr " + channel);
        ATSDK.setChannel(channel);
    }

    public void setCustomDataDic(ReadableMap customMap) {
        MsgTools.printMsg("setCustomDataDic customMap=" + RNMapUtil.readableMapToJsonString(customMap));
        ATSDK.initCustomMap(RNMapUtil.readableMapToHashMap(customMap));
    }

    public void setPlacementCustomData(String placementId, ReadableMap customMap) {
        MsgTools.printMsg("setPlacementCustomData placementId=" + placementId
                + " customMap=" + RNMapUtil.readableMapToJsonString(customMap));
        ATSDK.initPlacementCustomMap(placementId, RNMapUtil.readableMapToHashMap(customMap));
    }

    // —— GDPR / 隐私 ——

    public void getGDPRLevel(Promise promise) {
        Context ctx = appContext();
        if (ctx == null) {
            promise.reject(Const.BridgeError.SDK_NOT_INITIALIZED, "react context null");
            return;
        }
        promise.resolve(ATSDK.getGDPRDataLevel(ctx));
    }

    public void setDataConsentSet(int level) {
        MsgTools.printMsg("setDataConsentSet " + level);
        Context ctx = appContext();
        if (ctx != null) {
            ATSDK.setGDPRUploadDataLevel(ctx, level);
        }
    }

    public void showGDPRConsentDialog(final String appId) {
        MsgTools.printMsg("showGDPRConsentDialog appId=" + appId);
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Activity activity = currentActivity();
                if (activity == null) {
                    MsgTools.printMsg("showGDPRConsentDialog: currentActivity null, abort");
                    return;
                }
                ATGDPRConsentDismissListener listener = new ATGDPRConsentDismissListener() {
                    @Override
                    public void onDismiss(ConsentDismissInfo info) {
                        Map<String, Object> extraDic = new HashMap<>();
                        if (info != null) {
                            extraDic.put("infoMsg", info.getInfoMsg());
                            extraDic.put("dismissType", info.getDismissType());
                        }
                        ATReactNativeEventEmitter.getInstance().sendCallback(
                                Const.CallbackMethodCall.InitCallName,
                                Const.InitCallback.consentDismissCallbackKey,
                                "", extraDic, null, null);
                    }
                };
                if (!TextUtils.isEmpty(appId)) {
                    ATSDK.showGDPRConsentDialog(activity, listener, appId);
                } else {
                    ATSDK.showGDPRConsentDialog(activity, listener);
                }
            }
        });
    }

    // —— 过滤 / 策略 / 调试 ——

    public void putFilter(String placementId, ReadableMap filterSpec) {
        MsgTools.printMsg("putFilter placementId=" + placementId
                + " filterSpec=" + RNMapUtil.readableMapToJsonString(filterSpec));
        try {
            // 复杂嵌套参数：ReadableMap → JSON 字符串 → BridgeJsonMapUtil（复用既有 JSON 解析）
            String json = RNMapUtil.readableMapToJsonString(filterSpec);
            ATAdFilter filter = BridgeJsonMapUtil.waterfallFilterFromGroupsJson(json);
            if (filter != null) {
                ATSDK.putFilter(placementId, filter);
            }
        } catch (Throwable e) {
            MsgTools.printMsg("putFilter error: " + e.getMessage());
        }
    }

    public void removeFilters() {
        MsgTools.printMsg("removeFilters");
        ATSDK.removeFilters();
    }

    public void removeFilterWithPlacementId(String placementId) {
        MsgTools.printMsg("removeFilterWithPlacementId placementId=" + placementId);
        ATSDK.removeFilterWithPlacementId(placementId);
    }

    public void setAdSourcePrivacyPolicy(String policyJson) {
        MsgTools.printMsg("setAdSourcePrivacyPolicy: "
                + (policyJson == null ? "null" : ("len=" + policyJson.length())));
        if (TextUtils.isEmpty(policyJson)) {
            AdSourcePrivacyPolicyStore.setPolicyJson(null);
        } else {
            AdSourcePrivacyPolicyStore.setPolicyJson(policyJson);
        }
    }

    public void showDebuggerUI(final String debugKey) {
        MsgTools.printMsg("showDebuggerUI");
        final Context ctx = appContext();
        if (ctx == null) {
            return;
        }
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ATDebuggerUITest.showDebuggerUI(ctx, debugKey);
                } catch (Throwable e) {
                    MsgTools.printMsg("showDebuggerUI error: " + e.getMessage());
                }
            }
        });
    }

    // —— ATSDK 补全 ——

    private static List<String> toStringList(ReadableArray arr) {
        List<String> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                list.add(arr.getString(i));
            }
        }
        return list;
    }

    public void showGDPRConsentSecondDialog(final String appId) {
        MsgTools.printMsg("showGDPRConsentSecondDialog appId=" + appId);
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Activity activity = currentActivity();
                if (activity == null) {
                    MsgTools.printMsg("showGDPRConsentSecondDialog: activity null, abort");
                    return;
                }
                ATGDPRConsentDismissListener listener = new ATGDPRConsentDismissListener() {
                    @Override
                    public void onDismiss(ConsentDismissInfo info) {
                        Map<String, Object> extraDic = new HashMap<>();
                        if (info != null) {
                            extraDic.put("infoMsg", info.getInfoMsg());
                            extraDic.put("dismissType", info.getDismissType());
                        }
                        ATReactNativeEventEmitter.getInstance().sendCallback(
                                Const.CallbackMethodCall.InitCallName,
                                Const.InitCallback.consentDismissCallbackKey, "", extraDic, null, null);
                    }
                };
                ATSDK.showGDPRConsentSecondDialog(activity, listener, appId != null ? appId : "");
            }
        });
    }

    public void showGdprAuth() {
        MsgTools.printMsg("showGdprAuth");
        Context ctx = appContext();
        if (ctx != null) {
            ATSDK.showGdprAuth(ctx);
        }
    }

    public void checkIsEuTraffic(final Promise promise) {
        Context ctx = appContext();
        if (ctx == null) {
            promise.resolve(false);
            return;
        }
        ATSDK.checkIsEuTraffic(ctx, new NetTrafficeCallback() {
            @Override
            public void onResultCallback(boolean isEUTraffic) {
                promise.resolve(isEUTraffic);
            }

            @Override
            public void onErrorCallback(String msg) {
                MsgTools.printMsg("checkIsEuTraffic error: " + msg);
                promise.resolve(false);
            }
        });
    }

    public void isEUTraffic(Promise promise) {
        Context ctx = appContext();
        promise.resolve(ctx != null && ATSDK.isEUTraffic(ctx));
    }

    public void deniedUploadDeviceInfo(ReadableArray keys) {
        MsgTools.printMsg("deniedUploadDeviceInfo deviceInfoKeys="
                + RNMapUtil.readableArrayToJsonString(keys));
        List<String> list = toStringList(keys);
        ATSDK.deniedUploadDeviceInfo(list.toArray(new String[0]));
    }

    public void setPersonalizedAdStatus(int status) {
        MsgTools.printMsg("setPersonalizedAdStatus " + status);
        ATSDK.setPersonalizedAdStatus(status);
    }

    public void setFilterAdSourceIdList(String placementId, ReadableArray list) {
        MsgTools.printMsg("setFilterAdSourceIdList placementId=" + placementId
                + " adSourceIdList=" + RNMapUtil.readableArrayToJsonString(list));
        ATSDK.setFilterAdSourceIdList(placementId, toStringList(list));
    }

    public void setFilterNetworkFirmIdList(String placementId, ReadableArray list) {
        MsgTools.printMsg("setFilterNetworkFirmIdList placementId=" + placementId
                + " networkFirmIdList=" + RNMapUtil.readableArrayToJsonString(list));
        ATSDK.setFilterNetworkFirmIdList(placementId, toStringList(list));
    }

    public void setForbidNetworkFirmIdList(ReadableArray list) {
        MsgTools.printMsg("setForbidNetworkFirmIdList networkFirmIdList="
                + RNMapUtil.readableArrayToJsonString(list));
        ATSDK.setForbidNetworkFirmIdList(toStringList(list));
    }

    public void setForbidShowNetworkFirmIdList(String placementId, ReadableArray list) {
        MsgTools.printMsg("setForbidShowNetworkFirmIdList placementId=" + placementId
                + " networkFirmIdList=" + RNMapUtil.readableArrayToJsonString(list));
        ATSDK.setForbidShowNetworkFirmIdList(placementId, toStringList(list));
    }

    public void setAllowedShowNetworkFirmIdList(String placementId, ReadableArray list) {
        MsgTools.printMsg("setAllowedShowNetworkFirmIdList placementId=" + placementId
                + " networkFirmIdList=" + RNMapUtil.readableArrayToJsonString(list));
        ATSDK.setAllowedShowNetworkFirmIdList(placementId, toStringList(list));
    }

    public void setRiskFilterNetworkFirmIdList(int risk, ReadableArray list) {
        MsgTools.printMsg("setRiskFilterNetworkFirmIdList risk=" + risk
                + " networkFirmIdList=" + RNMapUtil.readableArrayToJsonString(list));
        ATSDK.setRiskFilterNetworkFirmIdList(risk, toStringList(list));
    }

    public void getArea(final Promise promise) {
        ATSDK.getArea(new ATAreaCallback() {
            @Override
            public void onResultCallback(String area) {
                promise.resolve(area);
            }

            @Override
            public void onErrorCallback(String msg) {
                MsgTools.printMsg("getArea error: " + msg);
                promise.resolve("");
            }
        });
    }

    public void isCnSDK(Promise promise) {
        promise.resolve(ATSDK.isCnSDK());
    }

    public void setLocation(ReadableMap location) {
        if (location == null) {
            MsgTools.printMsg("setLocation location=null");
            return;
        }
        double latitude = location.hasKey("latitude") ? location.getDouble("latitude") : 0;
        double longitude = location.hasKey("longitude") ? location.getDouble("longitude") : 0;
        MsgTools.printMsg("setLocation latitude=" + latitude + " longitude=" + longitude);
        Location loc = new Location("");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        ATSDK.setLocation(loc);
    }

    public void setDeviceInformationData(ReadableMap data) {
        MsgTools.printMsg("setDeviceInformationData data=" + RNMapUtil.readableMapToJsonString(data));
        try {
            Map<String, Object> performanceData = RNMapUtil.deviceInformationDataFromReadableMap(data);
            MsgTools.printMsg("setDeviceInformationData normalized=" + performanceData);
            ATSDK.setDeviceInformationData(performanceData);
        } catch (Throwable e) {
            MsgTools.printMsg("setDeviceInformationData error: " + e.getMessage());
        }
    }

    public void integrationChecking() {
        MsgTools.printMsg("integrationChecking");
        Context ctx = appContext();
        if (ctx != null) {
            ATSDK.integrationChecking(ctx);
        }
    }

    public void isNetworkLogDebug(Promise promise) {
        promise.resolve(ATSDK.isNetworkLogDebug());
    }

    public void setChannelSource(int channelFrom) {
        MsgTools.printMsg("setChannelSource " + channelFrom);
        ATSDK.setChannelSource(channelFrom);
    }

    public void setLocalStrategyAssetPath(String path) {
        MsgTools.printMsg("setLocalStrategyAssetPath " + path);
        Context ctx = appContext();
        if (ctx != null) {
            ATSDK.setLocalStrategyAssetPath(ctx, path);
        }
    }

    public void setSharedPlacementConfig(ReadableMap config) {
        MsgTools.printMsg("setSharedPlacementConfig config=" + RNMapUtil.readableMapToJsonString(config));
        try {
            if (config == null) {
                return;
            }
            ATSharedPlacementConfig sharedConfig = BridgeJsonMapUtil.sharedPlacementConfigFromJson(
                    RNMapUtil.readableMapToJsonString(config));
            if (sharedConfig != null) {
                ATSDK.setSharedPlacementConfig(sharedConfig);
                MsgTools.printMsg("setSharedPlacementConfig: " + sharedConfig);
            } else {
                MsgTools.printMsg("setSharedPlacementConfig: parsed config is null");
            }
        } catch (Throwable e) {
            MsgTools.printMsg("setSharedPlacementConfig error: " + e.getMessage());
        }
    }
}
