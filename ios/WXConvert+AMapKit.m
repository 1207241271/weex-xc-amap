//
//  WXConvert+AMapKit.m
//  Pods
//
//  Created by yangshengtao on 17/3/3.
//
//

#import "WXConvert+AMapKit.h"
#import "NSArray+WXMap.h"
#import <objc/runtime.h>

@implementation WXConvert (AMapKit)


#define WX_JSON_CONVERTER(type)           \
+ (type *)type:(id)json { return json; }

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wobjc-protocol-method-implementation"
WX_JSON_CONVERTER(NSArray)
WX_JSON_CONVERTER(NSDictionary)
#pragma clang diagnostic pop


+ (CLLocationCoordinate2D)CLLocationCoordinate2D:(id)json
{
    json = [self NSArray:json];
    return (CLLocationCoordinate2D){
        [[json wxmap_safeObjectForKey:1] doubleValue],
        [[json wxmap_safeObjectForKey:0] doubleValue]
    };
}

+ (BOOL)isLineDash:(id)json
{
    json = [self NSString:json];
    if ([json isEqualToString:@"dashed"]) {
        return YES;
    }
    return NO;
}

+ (CGSize)offsetToContainRect:(CGRect)innerRect inRect:(CGRect)outerRect
{
    CGFloat nudgeRight = fmaxf(0, CGRectGetMinX(outerRect) - (CGRectGetMinX(innerRect)));
    CGFloat nudgeLeft = fminf(0, CGRectGetMaxX(outerRect) - (CGRectGetMaxX(innerRect)));
    CGFloat nudgeTop = fmaxf(0, CGRectGetMinY(outerRect) - (CGRectGetMinY(innerRect)));
    CGFloat nudgeBottom = fminf(0, CGRectGetMaxY(outerRect) - (CGRectGetMaxY(innerRect)));
    return CGSizeMake(nudgeLeft ?: nudgeRight, nudgeTop ?: nudgeBottom);
}

+ (CGPoint)sizeToWXPixelType:(id)json withInstance:(WXSDKInstance *)instance
{
    json = [self NSArray:json];
    return CGPointMake([WXConvert WXPixelType:[json wxmap_safeObjectForKey:0] scaleFactor:instance.pixelScaleFactor],
                      [WXConvert WXPixelType:[json wxmap_safeObjectForKey:1] scaleFactor:instance.pixelScaleFactor]);
}

+ (BOOL)isValidatedArray:(id)json
{
    NSArray *convertedjson = [self NSArray:json];
    if (convertedjson && convertedjson.count > 1) {
        return YES;
    }
    return NO;
}

+ (UIColor *)UIColor:(id)value withOpacity:(id)opacity{
    if ([value isKindOfClass:[NSNull class]] || !value) {
        return nil;
    }
    float alpha = 1.0;
    if ([opacity isKindOfClass:[NSString class]]) {
        alpha = [WXConvert CGFloat:opacity];
    }
    UIColor *color = [WXConvert UIColor:value];
    const CGFloat *components = CGColorGetComponents(color.CGColor);
    
    return [UIColor colorWithRed:components[0] green:components[1] blue:components[2] alpha:alpha];
}

+(NSDictionary *)convertAddressComponent:(id)addressComponent{
    NSMutableDictionary *dic = [NSMutableDictionary dictionaryWithDictionary:[self getKeyAndValueFromIvar:addressComponent]];
    if ([[dic allKeys] containsObject:@"streetNumber"]) {
        [dic setObject: [self getKeyAndValueFromIvar:[addressComponent valueForKey:@"streetNumber"]] forKey:@"streetNumber"];
    }
    return [NSDictionary dictionaryWithDictionary:dic];
}

+(NSDictionary *)getKeyAndValueFromIvar:(id)object{
    NSMutableDictionary *result = [NSMutableDictionary new];
    unsigned int ivarCount = 0;
    Ivar *ivarList = class_copyIvarList([object class], &ivarCount);
    for(unsigned int i = 0;i < ivarCount; i++){
        Ivar ivar1 = ivarList[i];
        NSString *propertyName = [NSString stringWithUTF8String:ivar_getName(ivar1)];
        NSString *propertyType =    [NSString stringWithUTF8String:ivar_getTypeEncoding(ivar1)];
        NSString *key = [propertyName substringFromIndex:1];
        id value = [object valueForKey:propertyName];
        if (@available(iOS 8.0, *)) {
            if ([propertyType containsString:@"NSString"]||[propertyType isEqualToString:@"d"]||[propertyType isEqualToString:@"q"]||[propertyType isEqualToString:@"f"]||[propertyType isEqualToString:@"i"]) {
                [result setValue:value forKey:key];
            }else if([propertyType containsString:@"NSArray"]){
                [result setValue:[self getArrayFromIvar:value] forKey:key];
            }else{
                [result setValue:[self getKeyAndValueFromIvar:value] forKey:key];
            }
        } else {
            // Fallback on earlier versions
        }
    }
    return result;
}
+(NSArray *)getArrayFromIvar:(NSArray *)object{
    NSMutableArray *array = [NSMutableArray new];
    for(int i=0;i<object.count;i++){
        id temp = [object objectAtIndex:i];
        if ([temp isKindOfClass:[NSArray class]]) {
            [array addObject:[self getArrayFromIvar:temp]];
        }else{
            [array addObject:[self getKeyAndValueFromIvar:temp]];
        }
//        [array addObject:[self get]]
    }
    return [NSArray arrayWithArray:array];
}
@end
