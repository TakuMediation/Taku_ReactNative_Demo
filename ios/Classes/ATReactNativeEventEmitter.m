#import "ATReactNativeEventEmitter.h"
#import "ATConfiguration.h"

@interface ATReactNativeEventEmitter ()
@property (nonatomic, weak) id<ATReactNativeEventSink> sink;
@end

@implementation ATReactNativeEventEmitter

+ (instancetype)sharedInstance {
    static ATReactNativeEventEmitter *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[ATReactNativeEventEmitter alloc] init];
    });
    return instance;
}

- (void)setSink:(id<ATReactNativeEventSink>)sink {
    _sink = sink;
}

- (void)sendCallbackWithCallName:(NSString *)callName
                    callbackName:(NSString *)callbackName
                     placementID:(NSString *)placementID
                        extraDic:(NSDictionary *)extraDic
                  requestMessage:(NSString *)requestMessage {

    NSMutableDictionary *payload = [NSMutableDictionary dictionary];
    payload[@"callbackName"]    = callbackName ?: @"";
    payload[@"placementID"]     = placementID  ?: @"";
    payload[@"extraDic"]        = extraDic     ?: @{};
    payload[@"requestMessage"]  = requestMessage ?: @"";

    __weak id<ATReactNativeEventSink> sink = self.sink;
    NSString *name = callName ?: @"";

    void (^emit)(void) = ^{
        id<ATReactNativeEventSink> strongSink = sink;
        if (strongSink == nil) {
            // 桥还没建好（JS bundle 未加载完）或已销毁；丢弃事件而不是 crash。
            ATLog(@"[ATRNBridge] sink not ready, drop event=%@/%@ placementID=%@",
                  name, payload[@"callbackName"], payload[@"placementID"]);
            return;
        }
      ATLog(@"[ATRNBridge] sendevent=%@/%@ placementID=%@",
            name, payload[@"callbackName"], payload[@"placementID"]);
        [strongSink emitEventName:name body:payload];
    };

    // 🔴 线程命门：原生回调可能在子线程，必须切回主线程
    if ([NSThread isMainThread]) {
        emit();
    } else {
        dispatch_async(dispatch_get_main_queue(), emit);
    }
}

@end
