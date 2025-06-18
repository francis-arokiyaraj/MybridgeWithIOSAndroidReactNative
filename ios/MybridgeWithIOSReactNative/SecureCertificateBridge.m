//
//  SecureCertificateBridge.m
//  MybridgeWithIOSReactNative
//
//  Created by Safran App Developer on 12/06/25.
//

#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(SecureCertificateBridge, NSObject)


RCT_EXTERN_METHOD(storeCertificate:(NSString *)filePath
                  certName:(NSString *)certName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(readCertificate:(NSString *)certName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

//RCT_EXTERN_METHOD(storeCertificate:(NSString *)name
//                  content:(NSString *)content
//                  resolver:(RCTPromiseResolveBlock)resolve
//                  rejecter:(RCTPromiseRejectBlock)reject)
//
//RCT_EXTERN_METHOD(getCertificate:(NSString *)name
//                  resolver:(RCTPromiseResolveBlock)resolve
//                  rejecter:(RCTPromiseRejectBlock)reject)
//
//RCT_EXTERN_METHOD(deleteCertificate:(NSString *)name
//                  resolver:(RCTPromiseResolveBlock)resolve
//                  rejecter:(RCTPromiseRejectBlock)reject)

@end
