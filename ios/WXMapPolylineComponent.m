//
//  WXMapPolyline.m
//  Pods
//
//  Created by yangshengtao on 17/3/3.
//
//

#import "WXMapPolylineComponent.h"
#import "NSDictionary+WXMap.h"

@implementation WXMapPolylineComponent
- (instancetype)initWithRef:(NSString *)ref
                       type:(NSString*)type
                     styles:(nullable NSDictionary *)styles
                 attributes:(nullable NSDictionary *)attributes
                     events:(nullable NSArray *)events
               weexInstance:(WXSDKInstance *)weexInstance
{
    self = [super initWithRef:ref type:type styles:styles attributes:attributes events:events weexInstance:weexInstance];
    if (self) {
        _texture = [attributes wxmap_safeObjectForKey:@"texture"];
    }
    return self;
}

- (void)updateAttributes:(NSDictionary *)attributes
{
    if ([attributes wxmap_safeObjectForKey:@"texture"]) {
        _texture = [attributes wxmap_safeObjectForKey:@"texture"];
    }
}

@end
