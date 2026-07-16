//
//  ATNativeAttributeMode.m
//  topon_flutter_plugin
//
//  Created by GUO PENG on 2021/6/30.
//

#import "ATNativeAttributeMode.h"
#import "ATConfiguration.h"

@implementation ATNativeAttributeMode

- (void)setValue:(id)value forUndefinedKey:(NSString *)key{
    ATLog(@"Undefined key——%@",key);
}

- (BOOL)unsetFrame {
    
    if (self.width == 0 || self.height == 0) {
        return YES;
    } else {
        return NO;
    }
}


@end
