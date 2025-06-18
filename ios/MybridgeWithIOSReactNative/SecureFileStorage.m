//
//  SecureFileStorage.m
//  MybridgeWithIOSReactNative
//
//  Created by Safran App Developer on 09/06/25.
//

#import <Foundation/Foundation.h>

#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(SecureFileStorage, NSObject)

RCT_EXTERN_METHOD(saveFile:(NSString *)filename
                  content:(NSString *)content
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(readFile:(NSString *)filename
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)


RCT_EXTERN_METHOD(storeFileFromPath:(NSString *)filePath
                  destFileName:(NSString *)destFileName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getDecryptedFileContent:(NSString *)destFileName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getDecryptedFileToDownloads:(NSString *)destFileName
                  outputFileName:(NSString *)outputFileName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end
