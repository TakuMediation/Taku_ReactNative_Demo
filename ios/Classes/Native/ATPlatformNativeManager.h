//
//  ATPlatformNativeManager.h
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface ATPlatformNativeManager : NSObject

/// 加载原生广告（异步；通过 ATNativeDelegate 回调）
- (void)loadNativeWith:(NSString *)placementID extraDic:(NSDictionary *)extraDic;

/// 同步尝试把已加载好的 SDK Native ATNativeADView 取出并加进 hostView。
/// 当前实现：模板（Express）模式直接渲染；自渲染模式记日志后返回 NO（待 selfRender 接 RN 元素 binding）。
- (BOOL)attachNativeToHostView:(UIView *)hostView
                   placementID:(NSString *)placementID
                       sceneID:(nullable NSString *)sceneID;



@end

NS_ASSUME_NONNULL_END
