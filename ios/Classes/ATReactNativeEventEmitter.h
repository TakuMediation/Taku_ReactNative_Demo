#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * 下行事件接收者协议。由 ATReactNativeBridge 实现，
 * EventEmitter 通过 sink 转发到 RCTDeviceEventEmitter，
 * 避免 EventEmitter.m 直接 import 含 C++ 的 RNATBridgeSpec.h。
 */
@protocol ATReactNativeEventSink <NSObject>
- (void)emitEventName:(NSString *)name body:(nullable id)body;
@end

/**
 * Native → JS 唯一事件通道（对应 Flutter `ATSendSignalManager` / Android `ATReactNativeEventEmitter`）。
 *
 * 业务侧的 5 个 Delegate 通过 [SendEventManager sendMethod:arguments:result:]
 * （ATSendSignalManager 垫片）调到这里，再由本类按 8 callName + 统一 payload 结构
 * 转给 sink（即 ATReactNativeBridge），最终通过 RCTCallableJSModules.invokeModule
 * 调用 JS 端 RCTDeviceEventEmitter.emit，由 NativeEventEmitter 订阅者收到。
 *
 * 🔴 线程命门：原生回调可能在子线程，必须切回主线程再 emit（RN 0.85 严格主线程要求）。
 */
@interface ATReactNativeEventEmitter : NSObject

+ (instancetype)sharedInstance;

/** 由 ATReactNativeBridge 在 init 时调用 setSink:self 注册自己作为下行通道。 */
- (void)setSink:(nullable id<ATReactNativeEventSink>)sink;

/** 发送回调到 JS（8 callName + 统一 payload）。一律主线程执行。 */
- (void)sendCallbackWithCallName:(NSString *)callName
                    callbackName:(NSString *)callbackName
                     placementID:(nullable NSString *)placementID
                        extraDic:(nullable NSDictionary *)extraDic
                  requestMessage:(nullable NSString *)requestMessage;

@end

NS_ASSUME_NONNULL_END
