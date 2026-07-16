//
//  ATContainerTool.h
//  topon_flutter_plugin
//
//  Created by GUO PENG on 2021/6/25.
//

#import <Foundation/Foundation.h>
#import <AnyThinkSDK/ATCheckLoadModel.h>
NS_ASSUME_NONNULL_BEGIN

#define kATStringIsEmpty(str) (([str isKindOfClass:[NSNull class]] || str == nil || [str length] < 1) ? (NSLog(@"%s - placementID empty", __PRETTY_FUNCTION__), YES) : NO)
 
@interface ATCommonTool : NSObject

+ (UIViewController *)currentViewController;

+ (UIViewController *)getRootViewController;

+ (UIViewController *)getCurrentViewController;

+ (NSString*)dictionaryToJson:(NSDictionary *)dic;

+ (NSDictionary *)dictionaryWithJsonString:(NSString *)jsonString;

+ (NSString *)toReadableJSONString:(NSArray *)dataArr;

+ (NSDictionary *)objectToJSONString:(ATCheckLoadModel *)mode;
/**
 *  十六进制字符串转颜色
 */
+ (UIColor *)colorWithHex:(uint)hex alpha:(CGFloat)alpha;

+ (UIColor *)colorWithHexString:(NSString *)HexString;

+ (UIColor *)colorWithHexString:(NSString *)HexString alpha:(CGFloat)alpha;

+ (UIColor*) colorRGBonvertToHSB:(UIColor*)color withBrighnessDelta:(CGFloat)delta;

+ (UIColor*) colorRGBonvertToHSB:(UIColor*)color withAlphaDelta:(CGFloat)delta;

+ (UIColor*) colorWithHex:(NSInteger)hexValue;

+ (NSString*) checkStrParamsEmptyAndReturn:(NSString *)str;

/// 解析 extraDic 中的 atAdRequest 字段：若含 channelSource 则更新 SDK 全局配置，
/// 解析 adxBidFloorInfo / preLoadInfo 供后续扩展；处理完后从 dic 中移除 atAdRequest，
/// 保持传给 SDK loadAD 的 extra 干净（与 Unity ATxxxWrapper::loadXxxWithPlacementID:customDataJSONString: 一致）。
+ (void)applyAtAdRequestAndRemove:(NSMutableDictionary *)extraDic;

@end

NS_ASSUME_NONNULL_END
