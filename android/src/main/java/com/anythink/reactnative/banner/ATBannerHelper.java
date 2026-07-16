package com.anythink.reactnative.banner;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.anythink.banner.api.ATBannerExListener;
import com.anythink.banner.api.ATBannerView;
import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATAdSourceStatusListener;
import com.anythink.core.api.ATAdStatusInfo;
import com.anythink.core.api.ATNetworkConfirmInfo;
import com.anythink.core.api.ATAdRequest;
import com.anythink.core.api.AdError;
import com.anythink.core.api.ATShowConfig;
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
 * 单 placementId 的 Banner Helper。
 * 持原生 ATBannerView + Listener；回调经 {@link ATReactNativeEventEmitter} 转发 BannerCall。
 * 视图与命令式共享同一 Helper/ATBannerView（按 placementId 池化）。
 */
public class ATBannerHelper {

    private final Context mAppContext;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private ATBannerView mBannerView;
    private String mPlacementId;
    private Map<String, Object> mLocalExtra;
    private Map<String, Object> mTkExtra;
    private String mShowScenarioId;
    private String mShowCustomExt;

    /** 在主线程执行（已在主线程则直接跑）。view 操作必须主线程，否则 "Only the original thread..." 崩。 */
    private void runOnMain(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            mMain.post(r);
        }
    }

    public ATBannerHelper(Context appContext, String placementId) {
        mAppContext = appContext;
        mPlacementId = placementId;
    }

    private void emit(String callbackKey, ATAdInfo adInfo, String errMsg, Map<String, Object> extra) {
        Map<String, Object> info = null;
        if (adInfo != null) {
            try {
                info = Utils.jsonStrToMap(adInfo.toString());
            } catch (Throwable ignore) {
            }
        }
        ATReactNativeEventEmitter.getInstance().sendCallback(
                Const.CallbackMethodCall.BannerCall, callbackKey, mPlacementId, info, errMsg, extra);
    }

    public void setLocalExtra(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        if (mLocalExtra == null) {
            mLocalExtra = new HashMap<>();
        }
        mLocalExtra.putAll(map);
        if (mBannerView != null) {
            mBannerView.setLocalExtra(mLocalExtra);
        }
    }

    public void setShowConfig(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            mShowScenarioId = "";
            mShowCustomExt = "";
            applyShowConfig();
            return;
        }
        Object scenario = map.get("scenarioId");
        Object ext = map.get("showCustomExt");
        mShowScenarioId = scenario != null ? String.valueOf(scenario) : "";
        mShowCustomExt = ext != null ? String.valueOf(ext) : "";
        applyShowConfig();
    }

    private void applyShowConfig() {
        if (mBannerView == null) {
            return;
        }
        ATShowConfig.Builder builder = new ATShowConfig.Builder();
        if (mShowScenarioId != null && mShowScenarioId.length() > 0) {
            builder.scenarioId(mShowScenarioId);
        }
        if (mShowCustomExt != null && mShowCustomExt.length() > 0) {
            builder.showCustomExt(mShowCustomExt);
        }
        mBannerView.setShowConfig(builder.build());
    }

    public void setTKExtra(Map<String, Object> map) {
        mTkExtra = map;
        if (mBannerView != null && map != null) {
            mBannerView.setTKExtra(map);
        }
    }

    /** 取/建原生 banner view（Fabric ViewManager 与命令式共用）。 */
    public ATBannerView getOrCreateBannerView(Activity activity) {
        if (mBannerView == null) {
            initBanner(activity);
        }
        return mBannerView;
    }

    private void initBanner(Activity activity) {
        Context ctx = activity != null ? activity : mAppContext;
        mBannerView = new ATBannerView(ctx);
        mBannerView.setPlacementId(mPlacementId);
        if (mLocalExtra != null) {
            mBannerView.setLocalExtra(mLocalExtra);
        }
        if (mTkExtra != null) {
            mBannerView.setTKExtra(mTkExtra);
        }
        applyShowConfig();

        mBannerView.setBannerAdListener(new ATBannerExListener() {
            @Override
            public void onBannerLoaded() {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onBannerLoaded",
                        "placementId=" + mPlacementId);
                emit(Const.BannerCallback.LoadedCallbackKey, null, null, null);
            }

            @Override
            public void onBannerFailed(AdError adError) {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onBannerFailed",
                        "placementId=" + mPlacementId + "--error=" + adError.getFullErrorInfo());
                emit(Const.BannerCallback.LoadFailCallbackKey, null, adError.getFullErrorInfo(), null);
            }

            @Override
            public void onBannerClicked(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onBannerClicked",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.BannerCallback.ClickCallbackKey, atAdInfo, null, null);
            }

            @Override
            public void onBannerShow(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onBannerShow",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.BannerCallback.ShowCallbackKey, atAdInfo, null, null);
            }

            @Override
            public void onBannerClose(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onBannerClose",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.BannerCallback.CloseCallbackKey, atAdInfo, null, null);
            }

            @Override
            public void onBannerAutoRefreshed(ATAdInfo atAdInfo) {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onBannerAutoRefreshed",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.BannerCallback.RefreshCallbackKey, atAdInfo, null, null);
            }

            @Override
            public void onBannerAutoRefreshFail(AdError adError) {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onBannerAutoRefreshFail",
                        "placementId=" + mPlacementId + "--error=" + adError.getFullErrorInfo());
                emit(Const.BannerCallback.RefreshFailCallbackKey, null, adError.getFullErrorInfo(), null);
            }

            @Override
            public void onDeeplinkCallback(boolean isRefresh, ATAdInfo atAdInfo, boolean isSuccess) {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onDeeplinkCallback",
                        "placementId=" + mPlacementId + "--isRefresh=" + isRefresh + "--adInfo=" + atAdInfo
                                + "--isSuccess=" + isSuccess);
                Map<String, Object> extra = new HashMap<>();
                extra.put(Const.CallbackKey.isDeeplinkSuccess, isSuccess);
                emit(Const.BannerCallback.DeeplinkCallbackKey, atAdInfo, null, extra);
            }

            @Override
            public void onDownloadConfirm(Context context, ATAdInfo atAdInfo, ATNetworkConfirmInfo atNetworkConfirmInfo) {
                MsgTools.printCallback("Banner", "ATBannerHelper", "onDownloadConfirm",
                        "placementId=" + mPlacementId + "--adInfo=" + atAdInfo);
                emit(Const.BannerCallback.DownloadConfirmCallbackKey, atAdInfo, null, null);
            }
        });

        mBannerView.setAdRevenueListener(AdListenerFactory.revenueListener());
        mBannerView.setAdMultipleLoadedListener(AdListenerFactory.multipleLoaded(
                Const.CallbackMethodCall.BannerCall,
                Const.BannerCallback.MultipleLoadedCallbackKey, mPlacementId));
        mBannerView.setAdDownloadListener(AdListenerFactory.downloadListener());

        mBannerView.setAdSourceStatusListener(new ATAdSourceStatusListener() {
            @Override
            public void onAdSourceBiddingAttempt(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--Bidding--Attempt", "ATBannerHelper", "onAdSourceBiddingAttempt",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.BannerCallback.AdSourceBiddingAttemptCallbackKey, adInfo, null, null);
            }

            @Override
            public void onAdSourceBiddingFilled(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--Bidding--Filled", "ATBannerHelper", "onAdSourceBiddingFilled",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.BannerCallback.AdSourceBiddingFilledCallbackKey, adInfo, null, null);
            }

            @Override
            public void onAdSourceBiddingFail(ATAdInfo adInfo, AdError adError) {
                MsgTools.printAdSource("ADSource--Bidding--Fail", "ATBannerHelper", "onAdSourceBiddingFail",
                        MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                emit(Const.BannerCallback.AdSourceBiddingFailCallbackKey, adInfo,
                        adError != null ? adError.getFullErrorInfo() : null, null);
            }

            @Override
            public void onAdSourceAttempt(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--AD--Start", "ATBannerHelper", "onAdSourceAttempt",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.BannerCallback.AdSourceAttemptCallbackKey, adInfo, null, null);
            }

            @Override
            public void onAdSourceLoadFilled(ATAdInfo adInfo) {
                MsgTools.printAdSource("ADSource--AD--Load--Success", "ATBannerHelper", "onAdSourceLoadFilled",
                        MsgTools.adSourceParams(mPlacementId, adInfo));
                emit(Const.BannerCallback.AdSourceLoadFilledCallbackKey, adInfo, null, null);
            }

            @Override
            public void onAdSourceLoadFail(ATAdInfo adInfo, AdError adError) {
                MsgTools.printAdSource("ADSource--AD--Load--Fail", "ATBannerHelper", "onAdSourceLoadFail",
                        MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                emit(Const.BannerCallback.AdSourceLoadFailCallbackKey, adInfo,
                        adError != null ? adError.getFullErrorInfo() : null, null);
            }
        });
    }

    public void load(Map<String, Object> settings, Activity activity) {
        getOrCreateBannerView(activity);
        if (mBannerView == null) {
            return;
        }
        ATAdRequest atAdRequest = null;
        try {
            atAdRequest = BridgeJsonMapUtil.atAdRequestFromLoadExtraDic(settings);
        } catch (Throwable ignore) {
        }
        if (atAdRequest != null) {
            mBannerView.loadAd(atAdRequest);
        } else {
            mBannerView.loadAd();
        }
    }

    public Map<String, Object> checkAdStatus() {
        Map<String, Object> map = new HashMap<>(5);
        if (mBannerView != null) {
            ATAdStatusInfo info = mBannerView.checkAdStatus();
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
        return mBannerView != null ? mBannerView.checkValidAdCaches() : null;
    }

    /** Fabric 卸载：从容器摘 view，保留 Helper 池内已加载广告（完整销毁走 destroyBanner 桥）。 */
    public void detachFromContainer() {
        MsgTools.printMsg("banner detachFromContainer: " + mPlacementId);
        final ATBannerView view = mBannerView;
        if (view == null) {
            return;
        }
        // removeView 触碰 view 层级，必须主线程（JS 经桥可能在 RN bridge 线程调到这）。
        runOnMain(new Runnable() {
            @Override
            public void run() {
                if (view.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) view.getParent()).removeView(view);
                }
            }
        });
    }

    /** unmount/销毁：彻底释放（停刷新 + 释放广告 + 摘 view）。 */
    public void destroy() {
        MsgTools.printMsg("banner destroy: " + mPlacementId + ", hasView=" + (mBannerView != null));
        final ATBannerView view = mBannerView;
        mBannerView = null;
        if (view == null) {
            return;
        }
        // removeView/destroy 触碰 view 层级，必须主线程，否则 "Only the original thread..." 崩。
        runOnMain(new Runnable() {
            @Override
            public void run() {
                if (view.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) view.getParent()).removeView(view);
                }
                view.destroy();
            }
        });
    }
}
