#import "ATReactNativeEventEmitter.h"

#ifdef RCT_NEW_ARCH_ENABLED
#import <RNATBridgeSpec/RNATBridgeSpec.h>
@interface ATReactNativeBridge : NSObject <NativeATBridgeSpec, ATReactNativeEventSink>
#else
#import <React/RCTBridgeModule.h>
@interface ATReactNativeBridge : NSObject <RCTBridgeModule, ATReactNativeEventSink>
#endif

@end
