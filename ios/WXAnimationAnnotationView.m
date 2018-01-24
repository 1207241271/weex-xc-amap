//
//  WXAnimationAnnotationView.m
//  AFNetworking
//
//  Created by yangxu on 2018/1/24.
//

#import "WXAnimationAnnotationView.h"

@implementation WXAnimationAnnotationView
- (void)willMoveToWindow:(nullable UIWindow *)newWindow
{
    [super willMoveToWindow:newWindow];
    if (_growAnimationAnnotation) {
        [self showMarkerAnimation];
    }
}

-(void)showMarkerAnimation{
    CGRect finalFrame =self.frame;
    self.frame = CGRectMake(finalFrame.origin.x+finalFrame.size.width/2, finalFrame.origin.y+finalFrame.size.height/2, 0, 0);
    [UIView animateWithDuration:1 animations:^{
        self.frame = finalFrame;
    }];
}

@end
