//
//  LocateViewController.m
//  map
//
//  Created by Daisy on 2017/12/13.
//  Copyright © 2017年 Daisy. All rights reserved.
//

#import "LocateViewController.h"
#import <MAMapKit/MAMapKit.h>
#import <AMapLocationKit/AMapLocationKit.h>
#import <AMapSearchKit/AMapSearchKit.h>
#import <AMapFoundationKit/AMapFoundationKit.h>


@interface LocateViewController ()<MAMapViewDelegate,AMapLocationManagerDelegate,AMapSearchDelegate>
@property (nonatomic,strong) MAMapView* mapView;
@property (nonatomic,strong) AMapLocationManager* locationManager;
@property (nonatomic,strong) UILabel* label_title;
@property (nonatomic,strong) UILabel* label_addr;
@property (nonatomic,strong) AMapSearchAPI* search;
@property (nonatomic,strong) AMapReGeocodeSearchRequest *regeo;
@property (nonatomic,strong) UIWindow* window;

@end

@implementation LocateViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    [self.window setBackgroundColor:[UIColor whiteColor]];
    self.view.backgroundColor = [UIColor whiteColor];
    //[self.window makeKeyAndVisible];

    
    //get user location
    [self configLocationManager];
    [self locateAction];
    
    //set search to get regeo
    self.search = [[AMapSearchAPI alloc] init];
    [self.search setDelegate:self];
    self.regeo = [[AMapReGeocodeSearchRequest alloc] init];
    
    //to draw a map
    self.mapView = [[MAMapView alloc]initWithFrame:CGRectMake(0, 64, [UIScreen mainScreen].bounds.size.width, [UIScreen mainScreen].bounds.size.height-164)];
    self.mapView.delegate = self;
    [self.view addSubview:_mapView];
    self.mapView.showsUserLocation = YES;//这句就是开启定位
    self.mapView.userTrackingMode = MAUserTrackingModeFollow; // 追踪用户位置.
    
    //setting the center point of the map
    CLLocationCoordinate2D centerCoordinate;
    centerCoordinate.longitude = 114.306812;
    centerCoordinate.latitude = 30.582582;
    self.mapView.centerCoordinate = centerCoordinate;
    
    //create a bar
    UINavigationBar *navBar = [[UINavigationBar alloc]initWithFrame:CGRectMake(0, 20, [UIScreen mainScreen].bounds.size.width, 44)];
    [self.view addSubview:navBar];
    UINavigationItem* navItem = [[UINavigationItem alloc]initWithTitle:@"选择位置"];
    navBar.items=@[navItem];
    UIBarButtonItem* done = [[UIBarButtonItem alloc]initWithBarButtonSystemItem:UIBarButtonSystemItemDone target:self action:@selector(sendPosition)];
    navItem.rightBarButtonItem = done;
    
    
    //to draw location text
    [self addLocationRec];
    
}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)viewDidAppear:(BOOL)animated {
    MAPointAnnotation *pointAnnotation = [[MAPointAnnotation alloc] init];
    CGPoint mapCenter = [self.mapView convertCoordinate:self.mapView.centerCoordinate toPointToView:self.view];
    [pointAnnotation setLockedScreenPoint:CGPointMake(mapCenter.x, mapCenter.y-64)];
    [pointAnnotation setLockedToScreen:YES];
    
    [_mapView addAnnotation:pointAnnotation];
}


- (MAAnnotationView *)mapView:(MAMapView *)mapView viewForAnnotation:(id <MAAnnotation>)annotation
{
    if ([annotation isKindOfClass:[MAPointAnnotation class]])
    {
        static NSString *pointReuseIndentifier = @"pointReuseIndentifier";
        MAPinAnnotationView*annotationView = (MAPinAnnotationView*)[mapView dequeueReusableAnnotationViewWithIdentifier:pointReuseIndentifier];
        if (annotationView == nil)
        {
            annotationView = [[MAPinAnnotationView alloc] initWithAnnotation:annotation reuseIdentifier:pointReuseIndentifier];
        }
        annotationView.pinColor = MAPinAnnotationColorRed;
        return annotationView;
    }
    return nil;
}

- (void)mapView:(MAMapView *)mapView regionDidChangeAnimated:(BOOL)animated {
    MACoordinateRegion region;
    CLLocationCoordinate2D centerCoordinate = mapView.region.center;
    region.center= centerCoordinate;
    
    NSLog(@"regionDidChangeAnimated %f,%f",centerCoordinate.latitude, centerCoordinate.longitude);
    
    self.regeo.location = [AMapGeoPoint locationWithLatitude:centerCoordinate.latitude longitude:centerCoordinate.longitude];
    self.regeo.requireExtension = YES;
    //发起逆地理编码
    [self.search AMapReGoecodeSearch:self.regeo];
    
}

-(void)sendPosition {
    self.callback(@{@"result":@"success",@"data":@[@(self.mapView.centerCoordinate.longitude),@(self.mapView.centerCoordinate.latitude)]});
//    [self.delegate passCoordinate: self.mapView.centerCoordinate];
    [self dismissViewControllerAnimated:YES completion:nil];
}

-(void)addLocationRec {
    self.label_title = [[UILabel alloc] initWithFrame:CGRectMake(15, [UIScreen mainScreen].bounds.size.height-93, [UIScreen mainScreen].bounds.size.width-40, 40)];
    self.label_title.text = @"目的地位置";
    self.label_title.font = [UIFont fontWithName:@"Arial-BoldMT" size:18];
    [self.view addSubview:self.label_title];
    
    self.label_addr = [[UILabel alloc] initWithFrame:CGRectMake(15, [UIScreen mainScreen].bounds.size.height-78, [UIScreen mainScreen].bounds.size.width-30, 80)];
    self.label_addr.text = @"";
    self.label_addr.font = [UIFont fontWithName:@"Arial" size:15];
    self.label_addr.numberOfLines = 2;
    [self.view addSubview:self.label_addr];
}

- (void)configLocationManager
{
    self.locationManager = [[AMapLocationManager alloc] init];
    
    [self.locationManager setDelegate:self];
    
    [self.locationManager setDesiredAccuracy:kCLLocationAccuracyHundredMeters];
    
    [self.locationManager setLocationTimeout:6];
    
    [self.locationManager setReGeocodeTimeout:3];
}

- (void)locateAction
{
    //带逆地理的单次定位
    [self.locationManager requestLocationWithReGeocode:YES completionBlock:^(CLLocation *location, AMapLocationReGeocode *regeocode, NSError *error) {
        
        if (error)
        {
            NSLog(@"locError:{%ld - %@};", (long)error.code, error.localizedDescription);
            
            if (error.code == AMapLocationErrorLocateFailed)
            {
                return;
            }
        }
        
        //定位信息
        NSLog(@"location:%@", location);
        
        //self.mapView.centerCoordinate = location.coordinate;
        //self.regeo.location = [AMapGeoPoint locationWithLatitude:location.coordinate.latitude longitude:location.coordinate.longitude];
        //self.regeo.requireExtension = YES;
        //发起逆地理编码
        //[self.search AMapReGoecodeSearch:self.regeo];
        
        //逆地理信息
        if (regeocode)
        {
            NSLog(@"reGeocode:%@", regeocode);
            
        }
    }];
}

- (void)onReGeocodeSearchDone:(AMapReGeocodeSearchRequest *)request response:(AMapReGeocodeSearchResponse *)response
{
    if (response.regeocode != nil)
    {
        //CLLocationCoordinate2D coordinate = CLLocationCoordinate2DMake(request.location.latitude, request.location.longitude);
        NSLog(@"-----result reGeocode:%@", response.regeocode.formattedAddress);
        [self.label_addr setText:response.regeocode.formattedAddress];
    }
}

@end

