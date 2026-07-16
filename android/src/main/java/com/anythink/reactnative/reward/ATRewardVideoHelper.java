package com.anythink.reactnative.reward;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATAdSourceStatusListener;
import com.anythink.core.api.ATAdStatusInfo;
import com.anythink.core.api.ATNetworkConfirmInfo;
import com.anythink.core.api.ATAdRequest;
import com.anythink.core.api.ATShowConfig;
import com.anythink.core.api.AdError;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.listener.AdListenerFactory;
import com.anythink.reactnative.utils.BridgeJsonMapUtil;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.Utils;
import com.anythink.rewardvideo.api.ATRewardVideoAd;
import com.anythink.rewardvideo.api.ATRewardVideoExListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单 placementId 的激励视频 Helper。
 * Listener 回调统一经 {@link ATReactNativeEventEmitter} 转发 RewardedVideoCall。
 */
public class ATRewardVideoHelper {

    private final Context mAppContext;
    private ATRewardVideoAd mRewardVideoAd;
    private String mPlacementId;
    private Map<String, Object> mLocalExtra;
    private Map<String, Object> mTkExtra;

    public ATRewardVideoHelper(Context appContext) {
        mAppContext = appContext;
    }

    /** 缓存并应用 localExtra（ad 未创建时缓存，initRewardVideo 时应用）。 */
    public void setLocalExtra(Map<String, Object> map) {
        mLocalExtra = map;
        if (mRewardVideoAd != null && map != null) {
            mRewardVideoAd.setLocalExtra(map);
        }
    }

    public void setTKExtra(Map<String, Object> map) {
        mTkExtra = map;
        if (mRewardVideoAd != null && map != null) {
            mRewardVideoAd.setTKExtra(map);
        }
    }

    /** adInfo→Map 后经唯一通道发出。adInfo / errMsg 可空。 */
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

    private void initRewardVideo(final String placementId, final Activity activity) {
        mPlacementId = placementId;
        Context ctx = activity != null ? activity : mAppContext;
        mRewardVideoAd = new ATRewardVideoAd(ctx, placementId);
        if (mLocalExtra != null) {
            mRewardVideoAd.setLocalExtra(mLocalExtra);
        }
        if (mTkExtra != null) {
            mRewardVideoAd.setTKExtra(mTkExtra);
        }

        mRewardVideoAd.setAdListener(new ATRewardVideoExListener() {
            @Override
            public void onRewardedVideoAdLoaded() {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdLoaded",
                        "placementId=" + mPlacementId);
                emit(Const.RewardVideoCallback.LoadedCallbackKey, mPlacementId, null, null, null);
            }

            @Override
            public void onRewardedVideoAdFailed(AdError adError) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdFailed",
                        "placementId=" + mPlacementId + "--error=" + adError.getFullErrorInfo());
                emit(Const.RewardVideoCallback.LoadFailCallbackKey, mPlacementId, null, adError.getFullErrorInfo(), null);
            }

            @Override
            public void onRewardedVideoAdPlayStart(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdPlayStart",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.PlayStartCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onRewardedVideoAdPlayEnd(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdPlayEnd",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.PlayEndCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onRewardedVideoAdPlayFailed(AdError adError, ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdPlayFailed",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo + "--error=" + adError.getFullErrorInfo());
                emit(Const.RewardVideoCallback.PlayFailCallbackKey, mPlacementId, atAdInfo, adError.getFullErrorInfo(), null);
            }

            @Override
            public void onRewardedVideoAdClosed(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdClosed",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.CloseCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onRewardedVideoAdPlayClicked(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdPlayClicked",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.ClickCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onReward(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onReward",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.RewardCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onRewardFailed(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardFailed",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.RewardFailedCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onDeeplinkCallback(ATAdInfo atAdInfo, boolean isSuccess) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onDeeplinkCallback",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo + "--isSuccess=" + isSuccess);
                Map<String, Object> extra = new HashMap<>();
                extra.put(Const.CallbackKey.isDeeplinkSuccess, isSuccess);
                emit(Const.RewardVideoCallback.DeeplinkCallbackKey, mPlacementId, atAdInfo, null, extra);
            }

            @Override
            public void onDownloadConfirm(Context context, ATAdInfo atAdInfo, ATNetworkConfirmInfo atNetworkConfirmInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onDownloadConfirm",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.DownloadConfirmCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onRewardedVideoAdAgainPlayStart(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdAgainPlayStart",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.AgainPlayStartCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onRewardedVideoAdAgainPlayEnd(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdAgainPlayEnd",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.AgainPlayEndCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onRewardedVideoAdAgainPlayFailed(AdError adError, ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdAgainPlayFailed",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo + "--error=" + adError.getFullErrorInfo());
                emit(Const.RewardVideoCallback.AgainPlayFailCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onRewardedVideoAdAgainPlayClicked(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onRewardedVideoAdAgainPlayClicked",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.AgainClickCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onAgainReward(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onAgainReward",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.AgainRewardCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onAgainRewardFailed(ATAdInfo atAdInfo) {
                MsgTools.printCallback("RewardVideo", "ATRewardVideoHelper", "onAgainRewardFailed",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.RewardVideoCallback.AgainRewardFailedCallbackKey, mPlacementId, atAdInfo, null, null);
            }
        });

        mRewardVideoAd.setAdRevenueListener(AdListenerFactory.revenueListener());
        mRewardVideoAd.setAdMultipleLoadedListener(AdListenerFactory.multipleLoaded(
                Const.CallbackMethodCall.rewardedVideoCall,
                Const.RewardVideoCallback.MultipleLoadedCallbackKey, mPlacementId));
        mRewardVideoAd.setAdDownloadListener(AdListenerFactory.downloadListener());

        mRewardVideoAd.setAdSourceStatusListener(new ATAdSourceStatusListener() {
            @Override
            public void onAdSourceBiddingAttempt(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--Bidding--Attempt", "ATRewardVideoHelper", "onAdSourceBiddingAttempt",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.RewardVideoCallback.AdSourceBiddingAttemptCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceBiddingFilled(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--Bidding--Filled", "ATRewardVideoHelper", "onAdSourceBiddingFilled",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.RewardVideoCallback.AdSourceBiddingFilledCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceBiddingFail(ATAdInfo adInfo, AdError adError) {
                MsgTools.printAdSource("ADSource--Bidding--Fail", "ATRewardVideoHelper", "onAdSourceBiddingFail",
                        MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                emit(Const.RewardVideoCallback.AdSourceBiddingFailCallbackKey, mPlacementId, adInfo,
                        adError != null ? adError.getFullErrorInfo() : null, null);
            }

            @Override
            public void onAdSourceAttempt(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--AD--Start", "ATRewardVideoHelper", "onAdSourceAttempt",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.RewardVideoCallback.AdSourceAttemptCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceLoadFilled(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--AD--Load--Success", "ATRewardVideoHelper", "onAdSourceLoadFilled",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.RewardVideoCallback.AdSourceLoadFilledCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceLoadFail(ATAdInfo adInfo, AdError adError) {
                MsgTools.printAdSource("ADSource--AD--Load--Fail", "ATRewardVideoHelper", "onAdSourceLoadFail",
                        MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                emit(Const.RewardVideoCallback.AdSourceLoadFailCallbackKey, mPlacementId, adInfo,
                        adError != null ? adError.getFullErrorInfo() : null, null);
            }
        });
    }

    public void loadRewardedVideo(final String placementId, Map<String, Object> settings, final Activity activity) {
        MsgTools.printMsg("loadRewardedVideo: " + placementId);
        if (settings == null) {
            settings = new HashMap<>();
        }
        if (mRewardVideoAd == null) {
            initRewardVideo(placementId, activity);
        }
        if (mRewardVideoAd == null) {
            MsgTools.printMsg("loadRewardedVideo: mRewardVideoAd null after init, placementId=" + placementId);
            return;
        }

        // localExtra（user_id/userData 等）现经 setLocalExtra 设置，load 只解析 atAdRequest
        ATAdRequest atAdRequest = null;
        try {
            atAdRequest = BridgeJsonMapUtil.atAdRequestFromLoadExtraDic(settings);
        } catch (Throwable ignore) {
        }

        if (atAdRequest != null && activity != null) {
            mRewardVideoAd.load(activity, atAdRequest);
        } else {
            mRewardVideoAd.load();
        }
    }

    public void show(final Activity activity, final String scenario) {
        MsgTools.printMsg("showRewardedVideo: " + mPlacementId + ", scenario: " + scenario);
        if (mRewardVideoAd == null) {
            return;
        }
        if (!TextUtils.isEmpty(scenario)) {
            mRewardVideoAd.show(activity, scenario);
        } else {
            mRewardVideoAd.show(activity);
        }
    }

    public void showConfig(final Activity activity, final String scenario, final String showCustomExt,
                           final Object atCustomContentResultArg) {
        MsgTools.printMsg("showConfigRewardedVideo: " + mPlacementId);
        if (mRewardVideoAd == null) {
            return;
        }
        ATShowConfig.Builder builder = new ATShowConfig.Builder();
        if (!TextUtils.isEmpty(scenario)) {
            builder.scenarioId(scenario);
        }
        if (!TextUtils.isEmpty(showCustomExt)) {
            builder.showCustomExt(showCustomExt);
        }
        BridgeJsonMapUtil.applyAtCustomContentToShowConfigBuilder(builder, atCustomContentResultArg);
        mRewardVideoAd.show(activity, builder.build());
    }

    public boolean isAdReady() {
        boolean isReady = mRewardVideoAd != null && mRewardVideoAd.isAdReady();
        MsgTools.printMsg("video isAdReady: " + mPlacementId + ", " + isReady);
        return isReady;
    }

    public Map<String, Object> checkAdStatus() {
        Map<String, Object> map = new HashMap<>(5);
        if (mRewardVideoAd != null) {
            ATAdStatusInfo info = mRewardVideoAd.checkAdStatus();
            map.put("isLoading", info.isLoading());
            map.put("isReady", info.isReady());
            ATAdInfo top = info.getATTopAdInfo();
            if (top != null) {
                map.put("adInfo", top.toString());
            }
            return map;
        }
        map.put("isLoading", false);
        map.put("isReady", false);
        return map;
    }

    public List<ATAdInfo> validAdCaches() {
        if (mRewardVideoAd != null) {
            return mRewardVideoAd.checkValidAdCaches();
        }
        return null;
    }
}
