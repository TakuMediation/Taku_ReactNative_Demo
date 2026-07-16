//
//  ATBannerDelegate.h
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import <Foundation/Foundation.h>
#import <AnyThinkSDK/AnyThinkSDK.h>

NS_ASSUME_NONNULL_BEGIN

@interface ATBannerDelegate : NSObject<ATBannerDelegate, ATAdMultipleLoadingDelegate>

/// 加载成功回调（供 Fabric ComponentView 在 loaded 时 retrieve view + attach）
@property (nonatomic, copy, nullable) void (^onBannerLoaded)(NSString *placementID);
/// 加载失败回调
@property (nonatomic, copy, nullable) void (^onBannerLoadFail)(NSString *placementID, NSError * _Nullable error);

@end

NS_ASSUME_NONNULL_END
