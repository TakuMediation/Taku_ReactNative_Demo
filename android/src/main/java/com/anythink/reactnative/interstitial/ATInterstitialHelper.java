package com.anythink.reactnative.interstitial;

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
import com.anythink.interstitial.api.ATInterstitial;
import com.anythink.interstitial.api.ATInterstitialExListener;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.listener.AdListenerFactory;
import com.anythink.reactnative.utils.BridgeJsonMapUtil;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单 placementId 的插屏 Helper。
 * Listener 回调统一经 {@link ATReactNativeEventEmitter} 转发 InterstitialCall。
 */
public class ATInterstitialHelper {

    private final Context mAppContext;
    private ATInterstitial mInterstitialAd;
    private String mPlacementId;
    private Map<String, Object> mLocalExtra;
    private Map<String, Object> mTkExtra;

    public ATInterstitialHelper(Context appContext) {
        mAppContext = appContext;
    }

    public void setLocalExtra(Map<String, Object> map) {
        mLocalExtra = map;
        if (mInterstitialAd != null && map != null) {
            mInterstitialAd.setLocalExtra(map);
        }
    }

    public void setTKExtra(Map<String, Object> map) {
        mTkExtra = map;
        if (mInterstitialAd != null && map != null) {
            mInterstitialAd.setTKExtra(map);
        }
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

    private void initInterstitial(final String placementId, final Activity activity) {
        mPlacementId = placementId;
        Context ctx = activity != null ? activity : mAppContext;
        mInterstitialAd = new ATInterstitial(ctx, placementId);
        if (mLocalExtra != null) {
            mInterstitialAd.setLocalExtra(mLocalExtra);
        }
        if (mTkExtra != null) {
            mInterstitialAd.setTKExtra(mTkExtra);
        }

        mInterstitialAd.setAdListener(new ATInterstitialExListener() {
            @Override
            public void onInterstitialAdLoaded() {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onInterstitialAdLoaded",
                        "placementId=" + mPlacementId);
                emit(Const.InterstitialCallback.LoadedCallbackKey, mPlacementId, null, null, null);
            }

            @Override
            public void onInterstitialAdLoadFail(AdError adError) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onInterstitialAdLoadFail",
                        "placementId=" + mPlacementId + "--error=" + adError.getFullErrorInfo());
                emit(Const.InterstitialCallback.LoadFailCallbackKey, mPlacementId, null, adError.getFullErrorInfo(), null);
            }

            @Override
            public void onInterstitialAdClicked(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onInterstitialAdClicked",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.InterstitialCallback.ClickCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onInterstitialAdShow(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onInterstitialAdShow",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.InterstitialCallback.ShowCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onInterstitialAdClose(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onInterstitialAdClose",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.InterstitialCallback.CloseCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onInterstitialAdVideoStart(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onInterstitialAdVideoStart",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.InterstitialCallback.PlayStartCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onInterstitialAdVideoEnd(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onInterstitialAdVideoEnd",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.InterstitialCallback.PlayEndCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onInterstitialAdVideoError(AdError adError) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onInterstitialAdVideoError",
                        "placementId=" + mPlacementId + "--error=" + adError.getFullErrorInfo());
                emit(Const.InterstitialCallback.PlayFailCallbackKey, mPlacementId, null, adError.getFullErrorInfo(), null);
            }

            @Override
            public void onDeeplinkCallback(ATAdInfo atAdInfo, boolean isSuccess) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onDeeplinkCallback",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo + "--isSuccess=" + isSuccess);
                Map<String, Object> extra = new HashMap<>();
                extra.put(Const.CallbackKey.isDeeplinkSuccess, isSuccess);
                emit(Const.InterstitialCallback.DeeplinkCallbackKey, mPlacementId, atAdInfo, null, extra);
            }

            @Override
            public void onDownloadConfirm(Context context, ATAdInfo atAdInfo, ATNetworkConfirmInfo atNetworkConfirmInfo) {
                MsgTools.printCallback("Interstitial", "ATInterstitialHelper", "onDownloadConfirm",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.InterstitialCallback.DownloadConfirmCallbackKey, mPlacementId, atAdInfo, null, null);
            }
        });

        mInterstitialAd.setAdRevenueListener(AdListenerFactory.revenueListener());
        mInterstitialAd.setAdMultipleLoadedListener(AdListenerFactory.multipleLoaded(
                Const.CallbackMethodCall.InterstitialCall,
                Const.InterstitialCallback.MultipleLoadedCallbackKey, mPlacementId));
        mInterstitialAd.setAdDownloadListener(AdListenerFactory.downloadListener());

        mInterstitialAd.setAdSourceStatusListener(new ATAdSourceStatusListener() {
            @Override
            public void onAdSourceBiddingAttempt(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--Bidding--Attempt", "ATInterstitialHelper", "onAdSourceBiddingAttempt",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.InterstitialCallback.AdSourceBiddingAttemptCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceBiddingFilled(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--Bidding--Filled", "ATInterstitialHelper", "onAdSourceBiddingFilled",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.InterstitialCallback.AdSourceBiddingFilledCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceBiddingFail(ATAdInfo adInfo, AdError adError) {
                MsgTools.printAdSource("ADSource--Bidding--Fail", "ATInterstitialHelper", "onAdSourceBiddingFail",
                        MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                emit(Const.InterstitialCallback.AdSourceBiddingFailCallbackKey, mPlacementId, adInfo,
                        adError != null ? adError.getFullErrorInfo() : null, null);
            }

            @Override
            public void onAdSourceAttempt(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--AD--Start", "ATInterstitialHelper", "onAdSourceAttempt",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.InterstitialCallback.AdSourceAttemptCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceLoadFilled(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--AD--Load--Success", "ATInterstitialHelper", "onAdSourceLoadFilled",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.InterstitialCallback.AdSourceLoadFilledCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceLoadFail(ATAdInfo adInfo, AdError adError) {
                MsgTools.printAdSource("ADSource--AD--Load--Fail", "ATInterstitialHelper", "onAdSourceLoadFail",
                        MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                emit(Const.InterstitialCallback.AdSourceLoadFailCallbackKey, mPlacementId, adInfo,
                        adError != null ? adError.getFullErrorInfo() : null, null);
            }
        });
    }

    public void loadInterstitial(final String placementId, Map<String, Object> settings, final Activity activity) {
        MsgTools.printMsg("loadInterstitial: " + placementId);
        if (settings == null) {
            settings = new HashMap<>();
        }
        if (mInterstitialAd == null) {
            initInterstitial(placementId, activity);
        }
        if (mInterstitialAd == null) {
            return;
        }

        // localExtra 经 setLocalExtra 设置，load 只解析 atAdRequest
        ATAdRequest atAdRequest = null;
        try {
            atAdRequest = BridgeJsonMapUtil.atAdRequestFromLoadExtraDic(settings);
        } catch (Throwable ignore) {
        }

        if (atAdRequest != null && activity != null) {
            mInterstitialAd.load(activity, atAdRequest);
        } else {
            mInterstitialAd.load();
        }
    }

    public void show(final Activity activity, final String scenario) {
        MsgTools.printMsg("showInterstitial: " + mPlacementId + ", scenario: " + scenario);
        if (mInterstitialAd == null) {
            return;
        }
        if (!TextUtils.isEmpty(scenario)) {
            mInterstitialAd.show(activity, scenario);
        } else {
            mInterstitialAd.show(activity);
        }
    }

    public void showConfig(final Activity activity, final String scenario, final String showCustomExt,
                           final Object atCustomContentResultArg) {
        MsgTools.printMsg("showConfigInterstitial: " + mPlacementId);
        if (mInterstitialAd == null) {
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
        mInterstitialAd.show(activity, builder.build());
    }

    public boolean isAdReady() {
        boolean isReady = mInterstitialAd != null && mInterstitialAd.isAdReady();
        MsgTools.printMsg("interstitial isAdReady: " + mPlacementId + ", " + isReady);
        return isReady;
    }

    public Map<String, Object> checkAdStatus() {
        Map<String, Object> map = new HashMap<>(5);
        if (mInterstitialAd != null) {
            ATAdStatusInfo info = mInterstitialAd.checkAdStatus();
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
        if (mInterstitialAd != null) {
            return mInterstitialAd.checkValidAdCaches();
        }
        return null;
    }
}
