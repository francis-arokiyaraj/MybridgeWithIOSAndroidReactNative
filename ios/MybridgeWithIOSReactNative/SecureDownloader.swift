//
//  SecureDownloader.swift
//  MybridgeWithIOSReactNative
//
//  Created by Safran App Developer on 23/06/25.
//

import Foundation
import React

@objc(SecureDownloader)
class SecureDownloader: NSObject {
  private var session: URLSession?

  @objc(downloadSecureFile:filename:resolver:rejecter:)
  func downloadSecureFile(_ urlString: String,
                          filename: String,
                          resolver: @escaping RCTPromiseResolveBlock,
                          rejecter: @escaping RCTPromiseRejectBlock) {
    guard let url = URL(string: urlString) else {
      rejecter("INVALID_URL", "Invalid URL provided", nil)
      return
    }

    print("📥 Starting download from URL: \(url)")

    let config = URLSessionConfiguration.default
    session = URLSession(configuration: config, delegate: self, delegateQueue: nil)

    let task = session!.dataTask(with: url) { data, response, error in
      if let error = error {
        print("❌ Download error: \(error.localizedDescription)")
        rejecter("DOWNLOAD_FAILED", error.localizedDescription, error)
        return
      }

      guard let data = data else {
        print("❌ No data received")
        rejecter("NO_DATA", "No data received", nil)
        return
      }

      do {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let fileURL = docs.appendingPathComponent(filename)
        try data.write(to: fileURL)
        print("✅ File saved at: \(fileURL.path)")
        resolver(fileURL.path)
      } catch {
        print("❌ Failed to write file: \(error.localizedDescription)")
        rejecter("WRITE_FAILED", error.localizedDescription, error)
      }
    }

    task.resume()
  }
}

extension SecureDownloader: URLSessionDelegate {
  func urlSession(_ session: URLSession,
                  didReceive challenge: URLAuthenticationChallenge,
                  completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {

    print("🔐 Received SSL challenge for host: \(challenge.protectionSpace.host)")

    guard let serverTrust = challenge.protectionSpace.serverTrust else {
      print("❌ No server trust object provided")
      completionHandler(.cancelAuthenticationChallenge, nil)
      return
    }

    guard let certPath = Bundle.main.path(forResource: "secondserverglobalca", ofType: "der") else {
        print("❌ globalca.der not found in bundle")
        completionHandler(.cancelAuthenticationChallenge, nil)
        return
    }
    do {
      let certData = try Data(contentsOf: URL(fileURLWithPath: certPath))
      guard let pinnedCert = SecCertificateCreateWithData(nil, certData as CFData) else {
          print("❌ Unable to create pinned certificate from certData")
          completionHandler(.cancelAuthenticationChallenge, nil)
          return
      }

      SecTrustSetAnchorCertificates(serverTrust, [pinnedCert] as CFArray)
      SecTrustSetAnchorCertificatesOnly(serverTrust, true)

      var error: CFError?
      let isTrusted = SecTrustEvaluateWithError(serverTrust, &error)

      if isTrusted {
        print("✅ SSL trust evaluation succeeded")
        completionHandler(.useCredential, URLCredential(trust: serverTrust))
      } else {
        print("❌ SSL trust evaluation failed: \(error?.localizedDescription ?? "Unknown error")")
        completionHandler(.cancelAuthenticationChallenge, nil)
      }

    } catch {
      print("❌ Error loading pinned cert: \(error.localizedDescription)")
      completionHandler(.cancelAuthenticationChallenge, nil)
    }
  }
}
