//
//  ATNativeDelegate.h
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import <Foundation/Foundation.h>
#import <AnyThinkSDK/AnyThinkSDK.h>

NS_ASSUME_NONNULL_BEGIN

@interface ATNativeDelegate : NSObject<ATNativeADDelegate, ATAdMultipleLoadingDelegate>

/// 加载成功回调（供 Fabric ComponentView 在 loaded 时 getOffer + 渲染 ATNativeADView + attach）
@property (nonatomic, copy, nullable) void (^onNativeLoaded)(NSString *placementID);
/// 加载失败回调
@property (nonatomic, copy, nullable) void (^onNativeLoadFail)(NSString *placementID, NSError * _Nullable error);

@end

NS_ASSUME_NONNULL_END
