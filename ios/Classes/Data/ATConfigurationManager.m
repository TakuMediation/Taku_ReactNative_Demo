//
//  ATDataMode.m
//  Pods-Runner
//
//  Created by GUO PENG on 2021/6/26.
//

#import "ATConfigurationManager.h"
#import "ATConfiguration.h"

@implementation ATConfigurationManager

+(instancetype) sharedManager {
    static ATConfigurationManager *sharedManager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedManager = [[ATConfigurationManager alloc] init];
    });
    return sharedManager;
}


- (void)setValue:(id)value forUndefinedKey:(NSString *)key{
    ATLog(@"Undefined key——%@",key);
}

- (NSString *)description
{
    return [NSString stringWithFormat:@"-----:appIdStr:%@\nappKeyStr:%@\n logEnabled:%d\n customDataDic:%@\n exludeAppleIdArray:%@\n channelStr:%@\n subchannelStr:%@\n placementCustomDataDic:%@\n PlacementIDStr:%@", self.appIdStr,self.appKeyStr,self.isLogEnabled,self.customDataDic,self.exludeBundleIDArray,self.channelStr,self.subchannelStr,self.placementCustomDataDic,self.placementIDStr];
}
@end
