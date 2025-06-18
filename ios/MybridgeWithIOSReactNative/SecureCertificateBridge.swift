//
//  SecureCertificateBridge.swift
//  MybridgeWithIOSReactNative
//
//  Created by Safran App Developer on 12/06/25.
//
//

import Foundation
import Security
import React

@objc(SecureCertificateBridge)
class SecureCertificateBridge: NSObject {
  
  @objc
  func storeCertificate(_ filePath: String, certName: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    do {
      let data = try Data(contentsOf: URL(fileURLWithPath: filePath))
      let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrAccount as String: certName,
        kSecValueData as String: data
      ]
      SecItemDelete(query as CFDictionary) // remove existing
      let status = SecItemAdd(query as CFDictionary, nil)
      if status == errSecSuccess {
        resolver("Saved successfully")
      } else {
        rejecter("STORE_ERROR", "Unable to store certificate", nil)
      }
    } catch {
      rejecter("STORE_ERROR", error.localizedDescription, error)
    }
  }

  @objc
  func readCertificate(_ certName: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrAccount as String: certName,
      kSecReturnData as String: kCFBooleanTrue!,
      kSecMatchLimit as String: kSecMatchLimitOne
    ]
    print("reading ..........................")
    var result: AnyObject?
    let status = SecItemCopyMatching(query as CFDictionary, &result)
    if status == errSecSuccess, let data = result as? Data {
      print("reading ..........................2")
      resolver(data.base64EncodedString())
    } else {
      print("reading ..........................3")
      rejecter("READ_ERROR", "Could not find certificate", nil)
    }
  }
}


//import Foundation
//import Security
//import React
//
//@objc(SecureCertificateBridge)
//class SecureCertificateBridge: NSObject {
//
//  let service = "com.myapp.certificates"
//
//  @objc
//  func storeCertificate(_ name: String, content: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
//    guard let data = Data(base64Encoded: content) else {
//      reject("INVALID_DATA", "Content is not valid base64", nil)
//      return
//    }
//
//    let query: [String: Any] = [
//      kSecClass as String: kSecClassGenericPassword,
//      kSecAttrService as String: service,
//      kSecAttrAccount as String: name,
//      kSecValueData as String: data,
//      kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
//    ]
//
//    SecItemDelete(query as CFDictionary)
//    let status = SecItemAdd(query as CFDictionary, nil)
//
//    if status == errSecSuccess {
//      resolve("Stored securely")
//    } else {
//      reject("STORE_FAILED", "Failed to store", nil)
//    }
//  }
//
//  @objc
//  func getCertificate(_ name: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
//    let query: [String: Any] = [
//      kSecClass as String: kSecClassGenericPassword,
//      kSecAttrService as String: service,
//      kSecAttrAccount as String: name,
//      kSecReturnData as String: true
//    ]
//
//    var dataTypeRef: AnyObject?
//    let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)
//
//    if status == errSecSuccess {
//      if let data = dataTypeRef as? Data {
//        resolve(data.base64EncodedString())
//      } else {
//        reject("READ_FAILED", "Failed to decode data", nil)
//      }
//    } else {
//      reject("NOT_FOUND", "Certificate not found", nil)
//    }
//  }
//
//  @objc
//  func deleteCertificate(_ name: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
//    let query: [String: Any] = [
//      kSecClass as String: kSecClassGenericPassword,
//      kSecAttrService as String: service,
//      kSecAttrAccount as String: name
//    ]
//
//    let status = SecItemDelete(query as CFDictionary)
//
//    if status == errSecSuccess {
//      resolve("Deleted")
//    } else {
//      reject("DELETE_FAILED", "Certificate not found or failed to delete", nil)
//    }
//  }
//}
