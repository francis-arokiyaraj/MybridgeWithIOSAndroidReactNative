//
//  SecureDownloader.m
//  MybridgeWithIOSReactNative
//
//  Created by Safran App Developer on 23/06/25.
//

#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(SecureDownloader, NSObject)

RCT_EXTERN_METHOD(downloadSecureFile:(NSString *)url
                  filename:(NSString *)filename
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end
