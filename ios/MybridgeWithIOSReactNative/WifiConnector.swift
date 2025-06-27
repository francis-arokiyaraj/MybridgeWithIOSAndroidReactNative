//
//  WifiConnector.swift
//  MybridgeWithIOSReactNative
//
//  Created by Safran App Developer on 18/06/25.
//

import Foundation
import NetworkExtension
import SystemConfiguration.CaptiveNetwork
import SystemConfiguration
import React

@objc(WifiConnector)
class WifiConnector: NSObject {

  @objc(connectToWifi:password:resolver:rejecter:)
  func connectToWifi(ssid: String, password: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {

    let config = NEHotspotConfiguration(ssid: ssid, passphrase: password, isWEP: false)
    config.joinOnce = false

    NEHotspotConfigurationManager.shared.apply(config) { error in
      if let err = error as NSError? {
        if err.code == NEHotspotConfigurationError.alreadyAssociated.rawValue {
          self.checkInternetConnectivity(ssid: ssid, resolve: resolve, reject: reject)
        } else {
          reject("WIFI_ERROR", "Failed to connect: \(err.localizedDescription)", err)
        }
      } else {
        self.checkInternetConnectivity(ssid: ssid, resolve: resolve, reject: reject)
      }
    }
  }

  private func checkInternetConnectivity(ssid: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let url = URL(string: "https://clients3.google.com/generate_204") else {
      print("Connected to \(ssid), but no valid URL for test.")
      resolve("Connected to \(ssid), but no valid URL for test.")
      return
    }

    let task = URLSession(configuration: .default).dataTask(with: url) { _, response, error in
      if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 204 {
        print("Connected to \(ssid) with Internet")
        resolve("Connected to \(ssid) with Internet")
      } else {
        print("Connected to \(ssid), but no internet access")
        resolve("Connected to \(ssid), but no internet access")
      }
    }
    task.resume()
  }

  @objc
  static func requiresMainQueueSetup() -> Bool {
    return false
  }
}
