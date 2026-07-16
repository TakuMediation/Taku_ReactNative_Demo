#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef void(^ATSendSignalResultBlock)(id _Nullable result);

/// 兼容 Flutter 旧 ATSendSignalManager 宏名。所有现存 Manager / Delegate 直接用
/// `[SendEventManager sendMethod:arguments:result:]` 即可，内部转发到 ATReactNativeEventEmitter。
#define SendEventManager [ATSendSignalManager sharedManager]

@interface ATSendSignalManager : NSObject

+ (instancetype)sharedManager;

- (void)sendMethod:(NSString *)methodName
         arguments:(nullable id)arguments
            result:(ATSendSignalResultBlock _Nullable)resultBlock;

@end

NS_ASSUME_NONNULL_END
