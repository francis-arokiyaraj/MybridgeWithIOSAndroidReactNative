//
//  SecureFileStorage.swift
//  MybridgeWithIOSReactNative
//
//  Created by Safran App Developer on 09/06/25.
//

import React
import Foundation
import CryptoKit

@objc(SecureFileStorage)
class SecureFileStorage: NSObject {

    private let keyTag = "com.secureFileStorage.symmetricKey"
    private var key = SymmetricKey(size: .bits256)
  
    // MARK: - Get or generate AES key
    private func getSymmetricKey() throws -> SymmetricKey {
        let tag = keyTag.data(using: .utf8)!

        var query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecReturnData as String: true
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)

        if status == errSecSuccess, let keyData = item as? Data {
            return SymmetricKey(data: keyData)
        }

        // Not found — generate a new key
        let newKey = SymmetricKey(size: .bits256)
        let newKeyData = newKey.withUnsafeBytes { Data($0) }

        query = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecValueData as String: newKeyData,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        let addStatus = SecItemAdd(query as CFDictionary, nil)
        guard addStatus == errSecSuccess else {
            throw NSError(domain: "KeychainError", code: -1, userInfo: nil)
        }

        return newKey
    }

    // MARK: - Save file
    @objc
    func saveFile(_ filename: String, content: String, resolver: RCTPromiseResolveBlock, rejecter: RCTPromiseRejectBlock) {
        do {
            let key = try getSymmetricKey()
            let data = Data(content.utf8)

            let sealedBox = try AES.GCM.seal(data, using: key)
            let encryptedData = sealedBox.combined!

            let url = try getFileURL(filename)
            try encryptedData.write(to: url, options: [.atomic, .completeFileProtection])

            resolver("Saved")
        } catch {
            rejecter("SAVE_FAILED", "Could not save file", error)
        }
    }

    // MARK: - Read file
    @objc
    func readFile(_ filename: String, resolver: RCTPromiseResolveBlock, rejecter: RCTPromiseRejectBlock) {
        do {
            let url = try getFileURL(filename)
            let encryptedData = try Data(contentsOf: url)

            let key = try getSymmetricKey()
            let sealedBox = try AES.GCM.SealedBox(combined: encryptedData)
            let decryptedData = try AES.GCM.open(sealedBox, using: key)

            let content = String(decoding: decryptedData, as: UTF8.self)
            resolver(content)
        } catch {
            rejecter("READ_FAILED", "Could not read file", error)
        }
    }

    // MARK: - Helpers
    private func getFileURL(_ filename: String) throws -> URL {
        guard let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            throw NSError(domain: "FileError", code: -2, userInfo: nil)
        }
        return dir.appendingPathComponent(filename)
    }
  


  @objc
  func storeFileFromPath(_ filePath: String, destFileName: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
      do {
          let key = try getSymmetricKey()
          let url = URL(fileURLWithPath: filePath)
          let data = try Data(contentsOf: url)

          let sealedBox = try AES.GCM.seal(data, using: key)
          let combined = sealedBox.combined!

          let destURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(destFileName)
          try combined.write(to: destURL)

          resolver("Stored successfully.")
      } catch {
          rejecter("ERROR", "Encryption failed", error)
      }
  }

  @objc
  func getDecryptedFileContent(_ destFileName: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
      do {
          let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(destFileName)
          let encrypted = try Data(contentsOf: url)
          let sealedBox = try AES.GCM.SealedBox(combined: encrypted)
          let decrypted = try AES.GCM.open(sealedBox, using: key)

          resolver(String(data: decrypted, encoding: .utf8))
      } catch {
          rejecter("ERROR", "Decryption failed", error)
      }
  }
  
  
  @objc
  func getDecryptedFileToDownloads(_ destFileName: String, outputFileName: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    do {
      let key = try getSymmetricKey()
      let fileURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(destFileName)

      let encrypted = try Data(contentsOf: fileURL)

      guard encrypted.count > 12 + 16 else {
          reject("DECRYPTION_ERROR", "Encrypted data is too short to be valid", nil)
          return
      }

      let sealedBox: AES.GCM.SealedBox
      do {
        sealedBox = try AES.GCM.SealedBox(combined: encrypted)
      } catch {
        reject("DECRYPTION_ERROR", "Invalid sealed box format. Possibly corrupted or wrong key.", error)
        return
      }

      let decrypted = try AES.GCM.open(sealedBox, using: key)

      let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
      let outputFile = documentsDirectory.appendingPathComponent(outputFileName)

      try decrypted.write(to: outputFile)
      resolve("Decrypted file saved to Downloads.")
    } catch {
      reject("DECRYPTION_ERROR", "Failed to decrypt file", error)
    }
  }

//  @objc
//  func getDecryptedFileToDownloads(_ destFileName: String, outputFileName: String, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
//    do {
//      let key = try getSymmetricKey()
//      let fileURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(destFileName)
//      let encrypted = try Data(contentsOf: fileURL)
//
//      let sealedBox = try AES.GCM.SealedBox(combined: encrypted)
//      let decrypted = try AES.GCM.open(sealedBox, using: key)
//
//      let downloadsDirectory = FileManager.default.urls(for: .downloadsDirectory, in: .userDomainMask).first!
//
//      let outputFile = downloadsDirectory.appendingPathComponent(outputFileName)
//
//      let folder = downloadsDirectory
//
//      if !FileManager.default.fileExists(atPath: folder.path) {
//          try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
//      }
//      try decrypted.write(to: outputFile)
//
//      resolve("Decrypted file saved to Downloads.")
//    } catch {
//      reject("DECRYPTION_ERROR", "Failed to decrypt file", error)
//    }
//  }



    @objc
    static func requiresMainQueueSetup() -> Bool {
        return false
    }
}
