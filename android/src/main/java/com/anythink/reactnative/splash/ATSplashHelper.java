package com.anythink.reactnative.splash;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATAdSourceStatusListener;
import com.anythink.core.api.ATNetworkConfirmInfo;
import com.anythink.core.api.ATAdRequest;
import com.anythink.core.api.AdError;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.listener.AdListenerFactory;
import com.anythink.reactnative.utils.BridgeJsonMapUtil;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.Utils;
import com.anythink.splashad.api.ATSplashAd;
import com.anythink.splashad.api.ATSplashAdExtraInfo;
import com.anythink.splashad.api.ATSplashExListener;

import java.util.HashMap;
import java.util.Map;

/**
 * 单 placementId 的开屏 Helper。
 * Listener 经 {@link ATReactNativeEventEmitter} 转发 SplashCall；show 渲染进桥内容器（高度可配），dismiss 移除防泄漏。
 */
public class ATSplashHelper {

    private final Context mAppContext;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private ATSplashAd mSplashAd;
    private String mPlacementId;
    private FrameLayout mContainer;
    private Map<String, Object> mLocalExtra;
    private Map<String, Object> mTkExtra;

    public ATSplashHelper(Context appContext) {
        mAppContext = appContext;
    }

    public void setLocalExtra(Map<String, Object> map) {
        mLocalExtra = map;
        if (mSplashAd != null && map != null) {
            mSplashAd.setLocalExtra(map);
        }
    }

    public void setTKExtra(Map<String, Object> map) {
        mTkExtra = map;
        if (mSplashAd != null && map != null) {
            mSplashAd.setTKExtra(map);
        }
    }

    public Map<String, Object> checkAdStatus() {
        Map<String, Object> map = new HashMap<>(5);
        if (mSplashAd != null) {
            com.anythink.core.api.ATAdStatusInfo info = mSplashAd.checkAdStatus();
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

    public java.util.List<ATAdInfo> validAdCaches() {
        return mSplashAd != null ? mSplashAd.checkValidAdCaches() : null;
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
                Const.CallbackMethodCall.SplashCall, callbackKey, placementId, info, errMsg, extra);
    }

    private void initSplash(final String placementId, final Activity activity, int fetchAdTimeout) {
        mPlacementId = placementId;
        Context ctx = activity != null ? activity : mAppContext;

        ATSplashExListener listener = new ATSplashExListener() {
            @Override
            public void onAdLoaded(boolean isTimeout) {
                MsgTools.printCallback("Splash", "ATSplashHelper", "onAdLoaded",
                        "placementId=" + mPlacementId + "--isTimeout=" + isTimeout);
                Map<String, Object> extra = new HashMap<>();
                extra.put(Const.CallbackKey.isTimeout, isTimeout);
                emit(Const.SplashCallback.LoadedCallbackKey, mPlacementId, null, null, extra);
            }

            @Override
            public void onAdLoadTimeout() {
                MsgTools.printCallback("Splash", "ATSplashHelper", "onAdLoadTimeout",
                        "placementId=" + mPlacementId);
                emit(Const.SplashCallback.TimeoutCallbackKey, mPlacementId, null, null, null);
            }

            @Override
            public void onNoAdError(AdError adError) {
                MsgTools.printCallback("Splash", "ATSplashHelper", "onNoAdError",
                        "placementId=" + mPlacementId + "--error=" + adError.getFullErrorInfo());
                emit(Const.SplashCallback.LoadFailCallbackKey, mPlacementId, null, adError.getFullErrorInfo(), null);
            }

            @Override
            public void onAdShow(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Splash", "ATSplashHelper", "onAdShow",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.SplashCallback.ShowCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onAdClick(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Splash", "ATSplashHelper", "onAdClick",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.SplashCallback.ClickCallbackKey, mPlacementId, atAdInfo, null, null);
            }

            @Override
            public void onAdDismiss(ATAdInfo atAdInfo, ATSplashAdExtraInfo atSplashAdExtraInfo) {
                MsgTools.printCallback("Splash", "ATSplashHelper", "onAdDismiss",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo + "--extraInfo=" + atSplashAdExtraInfo);
                emit(Const.SplashCallback.CloseCallbackKey, mPlacementId, atAdInfo, null, null);
                removeContainerOnMain();
            }

            @Override
            public void onDeeplinkCallback(ATAdInfo atAdInfo, boolean isSuccess) {
                MsgTools.printCallback("Splash", "ATSplashHelper", "onDeeplinkCallback",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo + "--isSuccess=" + isSuccess);
                Map<String, Object> extra = new HashMap<>();
                extra.put(Const.CallbackKey.isDeeplinkSuccess, isSuccess);
                emit(Const.SplashCallback.DeeplinkCallbackKey, mPlacementId, atAdInfo, null, extra);
            }

            @Override
            public void onDownloadConfirm(Context context, ATAdInfo atAdInfo, ATNetworkConfirmInfo atNetworkConfirmInfo) {
                MsgTools.printCallback("Splash", "ATSplashHelper", "onDownloadConfirm",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.SplashCallback.DownloadConfirmCallbackKey, mPlacementId, atAdInfo, null, null);
            }
        };

        if (fetchAdTimeout > 0) {
            mSplashAd = new ATSplashAd(ctx, placementId, listener, fetchAdTimeout);
        } else {
            mSplashAd = new ATSplashAd(ctx, placementId, listener);
        }
        if (mLocalExtra != null) {
            mSplashAd.setLocalExtra(mLocalExtra);
        }
        if (mTkExtra != null) {
            mSplashAd.setTKExtra(mTkExtra);
        }

        mSplashAd.setAdRevenueListener(AdListenerFactory.revenueListener());
        mSplashAd.setAdMultipleLoadedListener(AdListenerFactory.multipleLoaded(
                Const.CallbackMethodCall.SplashCall,
                Const.SplashCallback.MultipleLoadedCallbackKey, mPlacementId));
        mSplashAd.setAdDownloadListener(AdListenerFactory.downloadListener());

        mSplashAd.setAdSourceStatusListener(new ATAdSourceStatusListener() {
            @Override
            public void onAdSourceBiddingAttempt(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--Bidding--Attempt", "ATSplashHelper", "onAdSourceBiddingAttempt",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.SplashCallback.AdSourceBiddingAttemptCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceBiddingFilled(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--Bidding--Filled", "ATSplashHelper", "onAdSourceBiddingFilled",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.SplashCallback.AdSourceBiddingFilledCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceBiddingFail(ATAdInfo adInfo, AdError adError) {
                MsgTools.printAdSource("ADSource--Bidding--Fail", "ATSplashHelper", "onAdSourceBiddingFail",
                        MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                emit(Const.SplashCallback.AdSourceBiddingFailCallbackKey, mPlacementId, adInfo,
                        adError != null ? adError.getFullErrorInfo() : null, null);
            }

            @Override
            public void onAdSourceAttempt(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--AD--Start", "ATSplashHelper", "onAdSourceAttempt",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.SplashCallback.AdSourceAttemptCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceLoadFilled(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--AD--Load--Success", "ATSplashHelper", "onAdSourceLoadFilled",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.SplashCallback.AdSourceLoadFilledCallbackKey, mPlacementId, adInfo, null, null);
            }

            @Override
            public void onAdSourceLoadFail(ATAdInfo adInfo, AdError adError) {
                MsgTools.printAdSource("ADSource--AD--Load--Fail", "ATSplashHelper", "onAdSourceLoadFail",
                        MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                emit(Const.SplashCallback.AdSourceLoadFailCallbackKey, mPlacementId, adInfo,
                        adError != null ? adError.getFullErrorInfo() : null, null);
            }
        });
    }

    public void loadSplash(final String placementId, Map<String, Object> settings, final Activity activity) {
        MsgTools.printMsg("loadSplash: " + placementId);
        if (settings == null) {
            settings = new HashMap<>();
        }
        int fetchAdTimeout = -1;
        try {
            Object t = settings.get("fetchAdTimeout");
            if (t != null) {
                fetchAdTimeout = Integer.parseInt(t.toString());
            }
        } catch (Throwable ignore) {
        }

        if (mSplashAd == null) {
            initSplash(placementId, activity, fetchAdTimeout);
        }
        if (mSplashAd == null) {
            return;
        }

        // localExtra 经 setLocalExtra 设置，load 只解析 atAdRequest
        ATAdRequest atAdRequest = null;
        try {
            atAdRequest = BridgeJsonMapUtil.atAdRequestFromLoadExtraDic(settings);
        } catch (Throwable ignore) {
        }

        if (atAdRequest != null) {
            mSplashAd.loadAd(atAdRequest);
        } else {
            mSplashAd.loadAd();
        }
    }

    /** heightPx>0 容器为该像素高度（半屏）；否则全屏。showCustomExt/atCustom 经 ATShowConfig。容器加/show 在主线程。 */
    public void show(final Activity activity, final String scenario, final int heightPx,
                     final String showCustomExt, final Object atCustom) {
        MsgTools.printMsg("showSplash: " + mPlacementId + ", heightPx=" + heightPx);
        if (mSplashAd == null) {
            return;
        }
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                removeContainer();
                ViewGroup root = activity.findViewById(android.R.id.content);
                if (root == null) {
                    return;
                }
                FrameLayout container = new FrameLayout(activity);
                int h = heightPx > 0 ? heightPx : ViewGroup.LayoutParams.MATCH_PARENT;
                container.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, h));
                root.addView(container);
                mContainer = container;
                boolean useConfig = (showCustomExt != null && showCustomExt.length() > 0) || atCustom != null;
                if (useConfig) {
                    com.anythink.core.api.ATShowConfig.Builder builder = new com.anythink.core.api.ATShowConfig.Builder();
                    if (scenario != null && scenario.length() > 0) {
                        builder.scenarioId(scenario);
                    }
                    if (showCustomExt != null && showCustomExt.length() > 0) {
                        builder.showCustomExt(showCustomExt);
                    }
                    BridgeJsonMapUtil.applyAtCustomContentToShowConfigBuilder(builder, atCustom);
                    mSplashAd.show(activity, container, null, builder.build());
                } else if (scenario != null && scenario.length() > 0) {
                    mSplashAd.show(activity, container, scenario);
                } else {
                    mSplashAd.show(activity, container);
                }
            }
        });
    }

    public boolean isAdReady() {
        boolean isReady = mSplashAd != null && mSplashAd.isAdReady();
        MsgTools.printMsg("splash isAdReady: " + mPlacementId + ", " + isReady);
        return isReady;
    }

    private void removeContainerOnMain() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                removeContainer();
            }
        });
    }

    /** 主线程调用：从父布局移除容器。 */
    private void removeContainer() {
        if (mContainer != null && mContainer.getParent() instanceof ViewGroup) {
            ((ViewGroup) mContainer.getParent()).removeView(mContainer);
        }
        mContainer = null;
    }
}
