//
//  WifiConnector.m
//  MybridgeWithIOSReactNative
//
//  Created by Safran App Developer on 18/06/25.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(WifiConnector, NSObject)
RCT_EXTERN_METHOD(connectToWifi:(NSString *)ssid
                  password:(NSString *)password
//                  isWEP:(BOOL)isWEP
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
@end
