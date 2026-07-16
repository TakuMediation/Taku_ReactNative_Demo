#import "ATSendSignalManager.h"
#import "ATReactNativeEventEmitter.h"
#import "ATConfiguration.h"

@implementation ATSendSignalManager

+ (instancetype)sharedManager {
    static ATSendSignalManager *_inst = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{ _inst = [[ATSendSignalManager alloc] init]; });
    return _inst;
}

- (void)sendMethod:(NSString *)methodName
         arguments:(id)arguments
            result:(ATSendSignalResultBlock)resultBlock {
    NSDictionary *dict = [arguments isKindOfClass:[NSDictionary class]] ? arguments : nil;
    NSString *callbackName = dict[@"callbackName"];
    NSString *placementID = dict[@"placementID"];
    id rawExtra = dict[@"extraDic"];
    NSDictionary *extraDic = [rawExtra isKindOfClass:[NSDictionary class]] ? rawExtra : dict;
    NSString *requestMessage = dict[@"requestMessage"];
  
    
  ATLog(@"methodName = %@; arguments = %@", methodName, arguments);
    [[ATReactNativeEventEmitter sharedInstance] sendCallbackWithCallName:methodName
                                                            callbackName:callbackName
                                                             placementID:placementID
                                                                extraDic:extraDic
                                                          requestMessage:requestMessage];

    if (resultBlock) { resultBlock(nil); }
}

@end
