//
//  MAURLocation.h
//  BackgroundGeolocation
//
//  Created by Marian Hello on 10/06/16.
//

#ifndef MAURLocation_h
#define MAURLocation_h

#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>
#import "MAURConfig.h"

@class MAURLocation;

typedef MAURLocation * _Nullable (^ MAURLocationTransform)(MAURLocation * _Nonnull location);

typedef NS_ENUM(NSInteger, MAURLocationStatus) {
    MAURLocationDeleted = 0,
    MAURLocationPostPending = 1,
    MAURLocationSyncPending = 2,
};
@interface MAURLocation : NSObject <NSCopying>{
BOOL ongoing;
BOOL paused;
}
@property (nonatomic, retain) NSNumber *locationId;
@property (nonatomic, retain) NSDate *time;
@property (nonatomic, retain) NSNumber *accuracy;
@property (nonatomic, retain) NSNumber *altitudeAccuracy;
@property (nonatomic, retain) NSNumber *speed;
@property (nonatomic, retain) NSNumber *heading;
@property (nonatomic, retain) NSNumber *altitude;
@property (nonatomic, retain) NSNumber *latitude;
@property (nonatomic, retain) NSNumber *longitude;
@property (nonatomic, retain) NSString *provider;
@property (nonatomic, retain) NSNumber *locationProvider;
@property (nonatomic, retain) NSNumber *radius; //only for stationary locations
@property (nonatomic) BOOL isValid;
@property (nonatomic) BOOL ongoing;
@property (nonatomic) BOOL paused;
@property (nonatomic, retain) NSNumber *userId;
@property (nonatomic, retain) NSNumber *orderId;
@property (nonatomic, retain) NSDate *recordedAt;
- (MAURConfig*) getConfig;
+ (instancetype) fromCLLocation:(CLLocation*)location;
+ (NSTimeInterval) locationAge:(CLLocation*)location;
+ (NSMutableDictionary*) toDictionary:(CLLocation*)location;
- (NSTimeInterval) locationAge;
- (NSDictionary*) toDictionary;
- (NSDictionary*) toDictionaryWithId;
- (id) toResultFromTemplate:(id)locationTemplate;
- (CLLocationCoordinate2D) coordinate;
- (BOOL) hasAccuracy;
- (BOOL) hasTime;
- (double) distanceFromLocation:(MAURLocation*)location;
- (BOOL) isBetterLocation:(MAURLocation*)location;
- (BOOL) isBeyond:(MAURLocation*)location radius:(NSInteger)radius;
- (id) copyWithZone: (NSZone *)zone;
- (id) getValueForKey:(id)key;

@end

#endif /* MAURLocation_h */
