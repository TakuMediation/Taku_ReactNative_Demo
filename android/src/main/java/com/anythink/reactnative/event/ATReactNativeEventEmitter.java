package com.anythink.reactnative.event;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Map;

/**
 * Native → JS 唯一事件通道。
 *
 * 所有上行回调经此发出，按 8 个固定 callName + 统一 payload；**一律在主线程 emit**
 * （RN 的 JSI / 事件分发要求主线程；这是 iOS `dispatch_async(main)` 守护在 Android 的对应）。
 * 禁止第二套 EventBus。
 */
public class ATReactNativeEventEmitter {

    private static volatile ATReactNativeEventEmitter sInstance;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private volatile ReactApplicationContext mReactContext;

    private ATReactNativeEventEmitter() {
    }

    public static ATReactNativeEventEmitter getInstance() {
        if (sInstance == null) {
            synchronized (ATReactNativeEventEmitter.class) {
                if (sInstance == null) {
                    sInstance = new ATReactNativeEventEmitter();
                }
            }
        }
        return sInstance;
    }

    public void setReactContext(ReactApplicationContext reactContext) {
        mReactContext = reactContext;
    }

    /** 简化版：无 extraDic / errorMsg 的事件。 */
    public void sendCallback(String callName, String callbackName, String placementId) {
        sendCallback(callName, callbackName, placementId, null, null, null);
    }

    /**
     * 发送一条回调到 JS。
     *
     * @param callName     8 个固定 callName 之一（Const.CallbackMethodCall.*）
     * @param callbackName 具体回调名（Const.*Callback.*）
     * @param placementId  广告位 ID；Init 类可为空串
     * @param extraDic     展平的 ATAdInfo（Map）；可空
     * @param requestMessage AdError 全文；成功时传 null（emit 时补 ""）
     * @param extra        附加布尔字段（isDeeplinkSuccess / isTimeout）；可空
     */
    public void sendCallback(final String callName, String callbackName, String placementId,
                             Map<String, Object> extraDic, String requestMessage, Map<String, Object> extra) {
        try {
            final WritableMap payload = Arguments.createMap();
            payload.putString(Const.CallbackKey.callbackName, callbackName);
            payload.putString(Const.CallbackKey.placementID, placementId != null ? placementId : "");

            if (extraDic != null) {
                payload.putMap(Const.CallbackKey.extraDic, Arguments.makeNativeMap(extraDic));
            }

            payload.putString(Const.CallbackKey.requestMessage, requestMessage != null ? requestMessage : "");

            if (extra != null) {
                for (Map.Entry<String, Object> entry : extra.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof Boolean
                            && (TextUtils.equals(key, Const.CallbackKey.isDeeplinkSuccess)
                            || TextUtils.equals(key, Const.CallbackKey.isTimeout))) {
                        payload.putBoolean(key, (Boolean) value);
                    } else if (value instanceof String && TextUtils.equals(key, Const.CallbackKey.adId)) {
                        // native 列表多广告：adId 顶层透出，JS 按 placementID+adId 分流到对应 cell
                        payload.putString(key, (String) value);
                    }
                }
            }

            emitOnMain(callName, callbackName, payload);
        } catch (Throwable e) {
            MsgTools.printMsg("sendCallback error: " + callbackName + ", " + e.getMessage());
        }
    }

    private void emitOnMain(final String callName, final String callbackName, final WritableMap payload) {
        Runnable emit = new Runnable() {
            @Override
            public void run() {
                try {
                    ReactApplicationContext ctx = mReactContext;
                    if (ctx != null && ctx.hasActiveReactInstance()) {
                        ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit(callName, payload);
                        MsgTools.printMsg("emit " + callName + "/" + callbackName);
                    } else {
                        MsgTools.printMsg("emit skipped: react context not active, " + callName);
                    }
                } catch (Throwable e) {
                    MsgTools.printMsg("emit error: " + callName + ", " + e.getMessage());
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            emit.run();
        } else {
            mMainHandler.post(emit);
        }
    }
}
