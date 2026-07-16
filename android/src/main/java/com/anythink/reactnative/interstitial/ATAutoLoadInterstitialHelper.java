package com.anythink.reactnative.interstitial;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATNetworkConfirmInfo;
import com.anythink.core.api.ATShowConfig;
import com.anythink.core.api.AdError;
import com.anythink.interstitial.api.ATInterstitialAutoAd;
import com.anythink.interstitial.api.ATInterstitialAutoEventListener;
import com.anythink.interstitial.api.ATInterstitialAutoLoadListener;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.listener.AdListenerFactory;
import com.anythink.reactnative.utils.BridgeJsonMapUtil;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 插屏 AutoLoad。callbackName 与手动一致。
 */
public class ATAutoLoadInterstitialHelper {

    public final Set<String> placementIDs = Collections.synchronizedSet(new HashSet<>());

    private static volatile ATAutoLoadInterstitialHelper instance;

    private ATAutoLoadInterstitialHelper() {
    }

    public static ATAutoLoadInterstitialHelper getInstance() {
        if (instance == null) {
            synchronized (ATAutoLoadInterstitialHelper.class) {
                if (instance == null) {
                    instance = new ATAutoLoadInterstitialHelper();
                }
            }
        }
        return instance;
    }

    private void emit(String callbackKey, String placementId, ATAdInfo adInfo, String errMsg,
                      Map<String, Object> extra) {
        Map<String, Object> info = null;
        if (adInfo != null) {
            try {
                info = Utils.jsonStrToMap(adInfo.toString());
            } catch (Throwable ignore) {
            }
        }
        ATReactNativeEventEmitter.getInstance().sendCallback(
                Const.CallbackMethodCall.InterstitialCall, callbackKey, placementId, info, errMsg, extra);
    }

    private final ATInterstitialAutoLoadListener autoLoadListener = new ATInterstitialAutoLoadListener() {
        @Override
        public void onInterstitialAutoLoaded(String placementId) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onInterstitialAutoLoaded",
                    "placementId=" + placementId);
            emit(Const.InterstitialCallback.LoadedCallbackKey, placementId, null, null, null);
        }

        @Override
        public void onInterstitialAutoLoadFail(String placementId, AdError adError) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onInterstitialAutoLoadFail",
                    "placementId=" + placementId + "--error=" + adError.getFullErrorInfo());
            emit(Const.InterstitialCallback.LoadFailCallbackKey, placementId, null, adError.getFullErrorInfo(), null);
        }
    };

    private final ATInterstitialAutoEventListener autoEventListener = new ATInterstitialAutoEventListener() {
        @Override
        public void onInterstitialAdClicked(ATAdInfo adInfo) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onInterstitialAdClicked",
                    "placementId=" + adInfo.getPlacementId() + "--adInfo=" + adInfo);
            emit(Const.InterstitialCallback.ClickCallbackKey, adInfo.getPlacementId(), adInfo, null, null);
        }

        @Override
        public void onInterstitialAdShow(ATAdInfo adInfo) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onInterstitialAdShow",
                    "placementId=" + adInfo.getPlacementId() + "--adInfo=" + adInfo);
            emit(Const.InterstitialCallback.ShowCallbackKey, adInfo.getPlacementId(), adInfo, null, null);
        }

        @Override
        public void onInterstitialAdClose(ATAdInfo adInfo) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onInterstitialAdClose",
                    "placementId=" + adInfo.getPlacementId() + "--adInfo=" + adInfo);
            emit(Const.InterstitialCallback.CloseCallbackKey, adInfo.getPlacementId(), adInfo, null, null);
        }

        @Override
        public void onInterstitialAdVideoStart(ATAdInfo adInfo) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onInterstitialAdVideoStart",
                    "placementId=" + adInfo.getPlacementId() + "--adInfo=" + adInfo);
            emit(Const.InterstitialCallback.PlayStartCallbackKey, adInfo.getPlacementId(), adInfo, null, null);
        }

        @Override
        public void onInterstitialAdVideoEnd(ATAdInfo adInfo) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onInterstitialAdVideoEnd",
                    "placementId=" + adInfo.getPlacementId() + "--adInfo=" + adInfo);
            emit(Const.InterstitialCallback.PlayEndCallbackKey, adInfo.getPlacementId(), adInfo, null, null);
        }

        @Override
        public void onInterstitialAdVideoError(AdError adError) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onInterstitialAdVideoError",
                    "error=" + adError.getFullErrorInfo());
            emit(Const.InterstitialCallback.PlayFailCallbackKey, "", null, adError.getFullErrorInfo(), null);
        }

        @Override
        public void onDeeplinkCallback(ATAdInfo adInfo, boolean isSuccess) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onDeeplinkCallback",
                    "placementId=" + adInfo.getPlacementId() + "--adInfo=" + adInfo + "--isSuccess=" + isSuccess);
            Map<String, Object> extra = new HashMap<>();
            extra.put(Const.CallbackKey.isDeeplinkSuccess, isSuccess);
            emit(Const.InterstitialCallback.DeeplinkCallbackKey, adInfo.getPlacementId(), adInfo, null, extra);
        }

        @Override
        public void onDownloadConfirm(Context context, ATAdInfo adInfo, ATNetworkConfirmInfo networkConfirmInfo) {
            MsgTools.printCallback("Interstitial", "ATAutoLoadInterstitialHelper", "onDownloadConfirm",
                    "placementId=" + adInfo.getPlacementId() + "--adInfo=" + adInfo);
            emit(Const.InterstitialCallback.DownloadConfirmCallbackKey, adInfo.getPlacementId(), adInfo, null, null);
        }
    };

    public boolean containsPlacementID(String placementID) {
        return placementIDs.contains(placementID);
    }

    public void autoLoad(String placementId, Map<String, Object> settings, Activity activity) {
        if (TextUtils.isEmpty(placementId) || activity == null) {
            MsgTools.printMsg("interstitial autoLoad: empty placementId or no activity");
            return;
        }
        placementIDs.add(placementId);
        String[] arr = new String[]{placementId};
        ATInterstitialAutoAd.init(activity, arr, autoLoadListener);
        if (settings != null) {
            ATInterstitialAutoAd.setLocalExtra(placementId, settings);
        }
        ATInterstitialAutoAd.addPlacementId(arr);
    }

    public void removePlacement(String placementId) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        placementIDs.remove(placementId);
        ATInterstitialAutoAd.removePlacementId(placementId);
    }

    public boolean isAdReady(String placementId) {
        return !TextUtils.isEmpty(placementId) && ATInterstitialAutoAd.isAdReady(placementId);
    }

    public void show(String placementId, String scenario, Activity activity) {
        Map<String, Object> cfg = new HashMap<>();
        if (!TextUtils.isEmpty(scenario)) {
            cfg.put("scenarioId", scenario);
        }
        showWithConfigMap(placementId, cfg, activity);
    }

    public void showWithConfigMap(String placementId, Map<String, Object> cfg, Activity activity) {
        if (activity == null) {
            return;
        }
        ATShowConfig.Builder builder = new ATShowConfig.Builder();
        if (cfg != null) {
            String scenario = cfg.containsKey("scenarioId")
                    ? String.valueOf(cfg.get("scenarioId"))
                    : (cfg.containsKey(Const.SCENE_ID) ? String.valueOf(cfg.get(Const.SCENE_ID)) : "");
            String showCustomExt = cfg.containsKey(Const.SHOW_CUSTOM_EXT)
                    ? String.valueOf(cfg.get(Const.SHOW_CUSTOM_EXT)) : null;
            if (!TextUtils.isEmpty(scenario)) {
                builder.scenarioId(scenario);
            }
            if (!TextUtils.isEmpty(showCustomExt)) {
                builder.showCustomExt(showCustomExt);
            }
            BridgeJsonMapUtil.applyAtCustomContentToShowConfigBuilder(
                    builder, cfg.get(Const.AT_CUSTOM_CONTENT_RESULT));
        }
        ATInterstitialAutoAd.show(activity, placementId, builder.build(), autoEventListener,
                AdListenerFactory.revenueListener());
    }
}
