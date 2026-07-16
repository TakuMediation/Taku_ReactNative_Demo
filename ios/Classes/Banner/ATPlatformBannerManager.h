//
//  ATPlatformBannerManager.h
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface ATPlatformBannerManager : NSObject

/// 加载横幅广告（异步；通过 ATBannerDelegate 回调）
- (void)loadBannerWith:(NSString *)placementID extraDic:(NSDictionary *)extraDic;

/// 同步尝试把已加载好的 SDK Banner UIView 取出并加进 hostView。
/// 加载未完成时返回 NO（调用方应 polling 重试）；成功返回 YES 且 SDK view 已 addSubview 到 hostView。
- (BOOL)attachBannerToHostView:(UIView *)hostView
                   placementID:(NSString *)placementID
                       sceneID:(nullable NSString *)sceneID;



@end

NS_ASSUME_NONNULL_END
