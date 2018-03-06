//
//  WXMapViewModule.m
//  Pods
//
//  Created by yangshengtao on 17/1/23.
//
//

#import "WXMapViewModule.h"
#import "WXMapViewComponent.h"
#import "WXConvert+AMapKit.h"
#import "LocateViewController.h"
#import <AMapSearchKit/AMapSearchKit.h>

@interface WXMapViewModule()<AMapSearchDelegate>
@property(nonatomic,strong) AMapSearchAPI *search;
@property(nonatomic,strong) NSMutableArray *reGeoRequestArray;
@property(nonatomic,strong) NSMutableArray *reGeoResultArray;
@property(nonatomic,copy)   WXModuleCallback reGeoCallBack;
@end
@implementation WXMapViewModule

@synthesize weexInstance;

WX_EXPORT_METHOD(@selector(getUserLocation:callback:))
WX_EXPORT_METHOD(@selector(getCenterLocation:callback:))
WX_EXPORT_METHOD(@selector(choosePosition:))
WX_EXPORT_METHOD(@selector(getLineDistance:marker:callback:))
WX_EXPORT_METHOD(@selector(reGeoPositions:callback:))
WX_EXPORT_METHOD(@selector(addMarkerMoveAnimation:points:speed:callback:))
WX_EXPORT_METHOD_SYNC(@selector(polygonContainsMarker:ref:callback:))

- (void)getUserLocation:(NSString *)elemRef callback:(WXModuleCallback)callback
{
    [self performBlockWithRef:elemRef block:^(WXComponent *component) {
        callback([(WXMapViewComponent *)component getUserLocation] ? : nil);
    }];
}

-(void)getCenterLocation:(NSString *)elemRef callback:(WXModuleCallback)callback{
    [self performBlockWithRef:elemRef block:^(WXComponent *component) {
        callback([(WXMapViewComponent *)component getCenterLocation] ? : nil);
    }];
}

-(void)includePoints:(NSString *)elemRef points:(NSArray*)points{
    [self performBlockWithRef:elemRef block:^(WXComponent *component) {
        [(WXMapViewComponent *)component includePoints:points];
    }];
}

-(void)addMarkerMoveAnimation:(NSString *)elemRef points:(NSArray *)points speed:(NSInteger)speed callback:(WXModuleCallback)callback{
    [self performBlockWithRef:elemRef block:^(WXComponent *component) {
        [(WXMapViewComponent *)component addPointAnimation:points speed:speed callback:callback];
    }];
}

-(void)choosePosition:(WXModuleCallback)callback {
    LocateViewController* locateView = [[LocateViewController alloc] init];
    locateView.modalTransitionStyle = UIModalTransitionStyleCoverVertical;
    locateView.callback = callback;
    [weexInstance.viewController presentViewController:locateView animated:YES completion:nil];
}
-(void)reGeoPositions:(NSArray *)postions callback:(WXModuleCallback)callback{
    self.reGeoCallBack = callback;
    if (!self.search) {
        self.search = [[AMapSearchAPI alloc] init];
        self.search.delegate = self;
    }
    self.reGeoRequestArray = [[NSMutableArray alloc] init];
    dispatch_queue_t queue = dispatch_queue_create("com.xunce.positions",DISPATCH_QUEUE_CONCURRENT);
    for (int index = 0; index<postions.count; index++) {
        AMapReGeocodeSearchRequest *regeo = [[AMapReGeocodeSearchRequest alloc] init];
        regeo.location = [AMapGeoPoint locationWithLatitude:[postions[index][1] floatValue] longitude:[postions[index][0] floatValue]];
        [self.reGeoRequestArray addObject:regeo];
        dispatch_async(queue, ^{
            [self.search AMapReGoecodeSearch:regeo];
        });
    }
    self.reGeoResultArray = [[NSMutableArray alloc]initWithArray:self.reGeoRequestArray];
}
-(void)AMapSearchRequest:(id)request didFailWithError:(NSError *)error{
    Boolean isFinished = true;
    for (int index = 0 ; index < self.reGeoRequestArray.count; index++) {
        if ([self.reGeoResultArray[index] isKindOfClass:[NSDictionary class]]) {
            continue;
        }
        if ([self.reGeoRequestArray[index] isEqual:request]) {
            [self.reGeoResultArray replaceObjectAtIndex:index withObject:@{@"error":error.description}];
            continue;
        }
        isFinished = false;
    }
    if (isFinished && self.reGeoCallBack != nil) {
        self.reGeoCallBack(@{@"result":@"fail",@"data":self.reGeoResultArray});
    }
}

-(void)onReGeocodeSearchDone:(AMapReGeocodeSearchRequest *)request response:(AMapReGeocodeSearchResponse *)response{
    Boolean isFinished = true;
    for (int index = 0 ; index < self.reGeoRequestArray.count; index++) {
        if ([self.reGeoResultArray[index] isKindOfClass:[NSDictionary class]]) {
            continue;
        }
        if ([self.reGeoRequestArray[index] isEqual:request]) {
            [self.reGeoResultArray replaceObjectAtIndex:index withObject:@{@"formattedAddress":response.regeocode.formattedAddress,@"addressComponent":        [WXConvert convertAddressComponent:response.regeocode.addressComponent]}];
            continue;
        }
        isFinished = false;
    }
    if (isFinished && self.reGeoCallBack != nil) {
        self.reGeoCallBack(@{@"result":@"success",@"data":self.reGeoResultArray});
    }
}

- (void)getLineDistance:(NSArray *)marker marker:(NSArray *)anotherMarker callback:(WXModuleCallback)callback
{
    CLLocationCoordinate2D location1 = [WXConvert CLLocationCoordinate2D:marker];
    CLLocationCoordinate2D location2 = [WXConvert CLLocationCoordinate2D:anotherMarker];
    MAMapPoint p1 = MAMapPointForCoordinate(location1);
    MAMapPoint p2 = MAMapPointForCoordinate(location2);
    CLLocationDistance distance =  MAMetersBetweenMapPoints(p1, p2);
    NSDictionary *userDic;
    if (distance > 0) {
        userDic = @{@"result":@"success",@"data":@{@"distance":[NSNumber numberWithDouble:distance]}};
    }else {
        userDic = @{@"result":@"false",@"data":@""};
    }
    callback(userDic);
}

- (void)polygonContainsMarker:(NSArray *)position ref:(NSString *)elemRef callback:(WXModuleCallback)callback
{
    [self performBlockWithRef:elemRef block:^(WXComponent *WXMapRenderer) {
        CLLocationCoordinate2D loc1 = [WXConvert CLLocationCoordinate2D:position];
        MAMapPoint p1 = MAMapPointForCoordinate(loc1);
        NSDictionary *userDic;

        if (![WXMapRenderer.shape isKindOfClass:[MAMultiPoint class]]) {
            userDic = @{@"result":@"false",@"data":[NSNumber numberWithBool:NO]};
            return;
        }
        MAMapPoint *points = ((MAMultiPoint *)WXMapRenderer.shape).points;
        NSUInteger pointCount = ((MAMultiPoint *)WXMapRenderer.shape).pointCount;
        
        if(MAPolygonContainsPoint(p1, points, pointCount)) {
             userDic = @{@"result":@"success",@"data":[NSNumber numberWithBool:YES]};
        } else {
            userDic = @{@"result":@"false",@"data":[NSNumber numberWithBool:NO]};
        }
        callback(userDic);
    }];
}

- (void)performBlockWithRef:(NSString *)elemRef block:(void (^)(WXComponent *))block {
    if (!elemRef) {
        return;
    }
    
    __weak typeof(self) weakSelf = self;
    
    WXPerformBlockOnComponentThread(^{
        WXComponent *component = (WXComponent *)[weakSelf.weexInstance componentForRef:elemRef];
        if (!component) {
            return;
        }
        
        [weakSelf performSelectorOnMainThread:@selector(doBlock:) withObject:^() {
            block(component);
        } waitUntilDone:NO];
    });
}

- (void)doBlock:(void (^)())block {
    block();
}
@end
