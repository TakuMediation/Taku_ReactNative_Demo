package com.anythink.reactnative.listener;

import com.anythink.china.api.ATAppDownloadListener;
import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATAdMultipleLoadedListener;
import com.anythink.core.api.ATAdRevenueListener;
import com.anythink.core.api.ATEventInterface;
import com.anythink.core.api.ATRequestingInfo;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.utils.BridgeJsonMapUtil;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 跨广告类型共享 Listener 工厂：Revenue / MultipleLoaded / AppDownload。
 * 各 Helper init 时挂到对应 SDK 广告对象上。
 */
public final class AdListenerFactory {

    private AdListenerFactory() {
    }

    private static final ATAdRevenueListener REVENUE_LISTENER = new ATAdRevenueListener() {
        @Override
        public void onAdRevenuePaid(ATAdInfo adInfo) {
            String placementId = adInfo != null && adInfo.getPlacementId() != null
                    ? adInfo.getPlacementId() : "";
            MsgTools.printCallback("Common", "AdListenerFactory", "onAdRevenuePaid",
                    "placementId=" + placementId + "--adInfo=" + adInfo);
            ATReactNativeEventEmitter.getInstance().sendCallback(
                    Const.CallbackMethodCall.CommonADCall,
                    Const.CommonADCallBack.AdShowRevenueCallbackKey,
                    placementId, adInfoToMap(adInfo), null, null);
        }
    };

    private static final ATEventInterface DOWNLOAD_LISTENER = new ATAppDownloadListener() {
        @Override
        public void onDownloadStart(ATAdInfo adInfo, long totalBytes, long currBytes,
                                    String fileName, String appName) {
            emitDownload(Const.DownloadCallCallback.DownloadStartKey, adInfo,
                    totalBytes, currBytes, fileName, appName, null);
        }

        @Override
        public void onDownloadUpdate(ATAdInfo adInfo, long totalBytes, long currBytes,
                                     String fileName, String appName) {
            emitDownload(Const.DownloadCallCallback.DownloadUpdateKey, adInfo,
                    totalBytes, currBytes, fileName, appName, null);
        }

        @Override
        public void onDownloadPause(ATAdInfo adInfo, long totalBytes, long currBytes,
                                    String fileName, String appName) {
            emitDownload(Const.DownloadCallCallback.DownloadPauseKey, adInfo,
                    totalBytes, currBytes, fileName, appName, null);
        }

        @Override
        public void onDownloadFinish(ATAdInfo adInfo, long totalBytes, String fileName, String appName) {
            emitDownload(Const.DownloadCallCallback.DownloadFinishedKey, adInfo,
                    totalBytes, totalBytes, fileName, appName, null);
        }

        @Override
        public void onDownloadFail(ATAdInfo adInfo, long totalBytes, long currBytes,
                                   String fileName, String appName) {
            emitDownload(Const.DownloadCallCallback.DownloadFailedKey, adInfo,
                    totalBytes, currBytes, fileName, appName, null);
        }

        @Override
        public void onInstalled(ATAdInfo adInfo, String fileName, String appName) {
            emitDownload(Const.DownloadCallCallback.DownloadInstalledKey, adInfo,
                    0, 0, fileName, appName, null);
        }
    };

    public static ATAdRevenueListener revenueListener() {
        return REVENUE_LISTENER;
    }

    public static ATEventInterface downloadListener() {
        return DOWNLOAD_LISTENER;
    }

    public static ATAdMultipleLoadedListener multipleLoaded(final String callName,
                                                            final String callbackKey,
                                                            final String placementId) {
        return new ATAdMultipleLoadedListener() {
            @Override
            public void onAdMultipleLoaded(ATRequestingInfo requestingInfo) {
                MsgTools.printCallback("Common", "AdListenerFactory", "onAdMultipleLoaded",
                        "placementId=" + placementId + "--requestingInfo=" + requestingInfo);
                Map<String, Object> extraDic = BridgeJsonMapUtil.requestingInfoToExtraDic(requestingInfo);
                ATReactNativeEventEmitter.getInstance().sendCallback(
                        callName, callbackKey, placementId, extraDic, null, null);
            }
        };
    }

    private static Map<String, Object> adInfoToMap(ATAdInfo adInfo) {
        if (adInfo == null) {
            return null;
        }
        try {
            return Utils.jsonStrToMap(adInfo.toString());
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static void emitDownload(String callbackKey, ATAdInfo adInfo, long totalBytes, long currBytes,
                                     String fileName, String appName, String errMsg) {
        String placementId = adInfo != null && adInfo.getPlacementId() != null
                ? adInfo.getPlacementId() : "";
        Map<String, Object> extraDic = adInfoToMap(adInfo);
        if (extraDic == null) {
            extraDic = new HashMap<>();
        }
        extraDic.put("totalBytes", totalBytes);
        extraDic.put("currBytes", currBytes);
        if (fileName != null) {
            extraDic.put("fileName", fileName);
        }
        if (appName != null) {
            extraDic.put("appName", appName);
        }
        MsgTools.printCallback("Download", "AdListenerFactory", callbackKey,
                "placementId=" + placementId + "--totalBytes=" + totalBytes + "--currBytes=" + currBytes
                        + "--fileName=" + fileName + "--appName=" + appName + "--adInfo=" + adInfo);
        ATReactNativeEventEmitter.getInstance().sendCallback(
                Const.CallbackMethodCall.DownloadCall, callbackKey, placementId, extraDic, errMsg, null);
    }
}
