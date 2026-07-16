package com.anythink.reactnative.utils;

import android.util.Log;

import com.anythink.core.api.AdError;

/**
 * 桥接层日志。
 * TAG 固定 `ATRNBridge`；随 init 时与 SDK 日志双开（见 ATInitManager）。
 */
public class MsgTools {
    public static final String TAG = "ATRNBridge";
    public static boolean isDebug = false;

    public static void printMsg(String msg) {
        if (isDebug) {
            Log.i(TAG, msg != null ? msg : "null");
        }
    }

    /** 与 iOS ATLog 对齐：AdType:ClassName::methodName:params */
    public static void printCallback(String adType, String className, String methodName, String params) {
        if (!isDebug) {
            return;
        }
        String msg = adType + ":" + className + "::" + methodName;
        if (params != null && params.length() > 0) {
            msg += ":" + params;
        }
        printMsg(msg);
    }

    /** 广告源回调（与 iOS ADSource--AD--Start--Class::method 对齐）。 */
    public static void printAdSource(String phase, String className, String methodName, String params) {
        if (!isDebug) {
            return;
        }
        String msg = phase + "--" + className + "::" + methodName;
        if (params != null && params.length() > 0) {
            msg += ":" + params;
        }
        printMsg(msg);
    }

    public static String adSourceParams(String placementId, Object adInfo) {
        return "placementId=" + placementId + "--adInfo=" + adInfo;
    }

    public static String adSourceErrorParams(String placementId, Object adInfo, AdError adError) {
        String err = adError != null ? adError.getFullErrorInfo() : "";
        return "placementId=" + placementId + "--adInfo=" + adInfo + "--error=" + err;
    }

    public static void setLogDebug(boolean debug) {
        isDebug = debug;
    }
}
