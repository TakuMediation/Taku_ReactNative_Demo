package com.anythink.reactnative.reward;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATAdStatusInfo;
import com.anythink.core.api.ATNetworkConfirmInfo;
import com.anythink.core.api.ATShowConfig;
import com.anythink.core.api.AdError;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.listener.AdListenerFactory;
import com.anythink.reactnative.utils.BridgeJsonMapUtil;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.Utils;
import com.anythink.rewardvideo.api.ATRewardVideoAutoAd;
import com.anythink.rewardvideo.api.ATRewardVideoAutoEventListener;
import com.anythink.rewardvideo.api.ATRewardVideoAutoLoadListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 激励视频 AutoLoad。
 * callbackName 经唯一通道转发，与手动加载完全一致。
 */
public class ATAutoLoadRewardVideoHelper {

    public final Set<String> placementIDs = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Map<String, Object>> placementSettings = new HashMap<>();

    private static volatile ATAutoLoadRewardVideoHelper instance;

    private ATAutoLoadRewardVideoHelper() {
    }

    public static ATAutoLoadRewardVideoHelper getInstance() {
        if (instance == null) {
            synchronized (ATAutoLoadRewardVideoHelper.class) {
                if (instance == null) {
                    instance = new ATAutoLoadRewardVideoHelper();
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
                Const.CallbackMethodCall.rewardedVideoCall, callbackKey, placementId, info, errMsg, extra);
    }

    private final ATRewardVideoAutoLoadListener autoLoadListener = new ATRewardVideoAutoLoadListener() {
        @Override
        public void onRewardVideoAutoLoaded(String placementId) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardVideoAutoLoaded",
                    "placementId=" + placementId);
            emit(Const.RewardVideoCallback.LoadedCallbackKey, placementId, null, null, null);
        }

        @Override
        public void onRewardVideoAutoLoadFail(String placementId, AdError adError) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardVideoAutoLoadFail",
                    "placementId=" + placementId + "--error=" + adError.getFullErrorInfo());
            emit(Const.RewardVideoCallback.LoadFailCallbackKey, placementId, null, adError.getFullErrorInfo(), null);
        }
    };

    private final ATRewardVideoAutoEventListener autoEventListener = new ATRewardVideoAutoEventListener() {
        @Override
        public void onRewardedVideoAdPlayStart(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdPlayStart",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.PlayStartCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        @Override
        public void onRewardedVideoAdPlayEnd(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdPlayEnd",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.PlayEndCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        @Override
        public void onRewardedVideoAdPlayFailed(AdError adError, ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdPlayFailed",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo
                            + "--error=" + adError.getFullErrorInfo());
            emit(Const.RewardVideoCallback.PlayFailCallbackKey, atAdInfo.getPlacementId(), atAdInfo,
                    adError.getFullErrorInfo(), null);
        }

        @Override
        public void onRewardedVideoAdClosed(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdClosed",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.CloseCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        @Override
        public void onRewardedVideoAdPlayClicked(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdPlayClicked",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.ClickCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        @Override
        public void onReward(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onReward",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.RewardCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        @Override
        public void onRewardFailed(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardFailed",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.RewardFailedCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        public void onDeeplinkCallback(ATAdInfo atAdInfo, boolean isSuccess) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onDeeplinkCallback",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo + "--isSuccess=" + isSuccess);
            Map<String, Object> extra = new HashMap<>();
            extra.put(Const.CallbackKey.isDeeplinkSuccess, isSuccess);
            emit(Const.RewardVideoCallback.DeeplinkCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, extra);
        }

        public void onDownloadConfirm(Context context, ATAdInfo atAdInfo, ATNetworkConfirmInfo networkConfirmInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onDownloadConfirm",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.DownloadConfirmCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        public void onRewardedVideoAdAgainPlayStart(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdAgainPlayStart",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.AgainPlayStartCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        public void onRewardedVideoAdAgainPlayEnd(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdAgainPlayEnd",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.AgainPlayEndCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        public void onRewardedVideoAdAgainPlayFailed(AdError adError, ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdAgainPlayFailed",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo
                            + "--error=" + adError.getFullErrorInfo());
            emit(Const.RewardVideoCallback.AgainPlayFailCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        public void onRewardedVideoAdAgainPlayClicked(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onRewardedVideoAdAgainPlayClicked",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.AgainClickCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        public void onAgainReward(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onAgainReward",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.AgainRewardCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }

        @Override
        public void onAgainRewardFailed(ATAdInfo atAdInfo) {
            MsgTools.printCallback("RewardVideo", "ATAutoLoadRewardVideoHelper", "onAgainRewardFailed",
                    "placementId=" + atAdInfo.getPlacementId() + "--adInfo=" + atAdInfo);
            emit(Const.RewardVideoCallback.AgainRewardFailedCallbackKey, atAdInfo.getPlacementId(), atAdInfo, null, null);
        }
    };

    public boolean containsPlacementID(String placementID) {
        return placementIDs.contains(placementID);
    }

    public void autoLoad(String placementId, Map<String, Object> settings, Activity activity) {
        if (TextUtils.isEmpty(placementId) || activity == null) {
            MsgTools.printMsg("autoLoad: empty placementId or no activity");
            return;
        }
        placementIDs.add(placementId);
        if (settings != null) {
            placementSettings.put(placementId, settings);
        }
        String[] arr = new String[]{placementId};
        ATRewardVideoAutoAd.init(activity, arr, autoLoadListener);
        if (settings != null) {
            ATRewardVideoAutoAd.setLocalExtra(placementId, settings);
        }
        ATRewardVideoAutoAd.addPlacementId(arr);
    }

    public void removePlacement(String placementId) {
        if (TextUtils.isEmpty(placementId)) {
            return;
        }
        placementIDs.remove(placementId);
        placementSettings.remove(placementId);
        ATRewardVideoAutoAd.removePlacementId(new String[]{placementId});
    }

    public boolean isAdReady(String placementId) {
        return !TextUtils.isEmpty(placementId) && ATRewardVideoAutoAd.isAdReady(placementId);
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
        ATRewardVideoAutoAd.show(activity, placementId, builder.build(), autoEventListener,
                AdListenerFactory.revenueListener());
    }
}
