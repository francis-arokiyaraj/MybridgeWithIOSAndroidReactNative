package com.mybridgewithiosreactnative


import android.content.Context
import android.util.Log
import com.facebook.react.bridge.*
import okhttp3.*
import okhttp3.Callback
import java.io.*
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.*

class SecureHttpSDownloaderModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "SecureDownloader"

    @ReactMethod
    fun downloadSecureFile(url: String, filename: String, promise: Promise) {
        try {
            val client = getPinnedHttpClient(reactApplicationContext)
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    promise.reject("DOWNLOAD_FAILED", e.message, e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val dir = reactApplicationContext.getExternalFilesDir(null)
                    val destFile = File(dir, filename)
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(destFile).use { output -> input.copyTo(output) }
                    }
                    promise.resolve(destFile.absolutePath)
                }
            })
        } catch (e: Exception) {
            promise.reject("INIT_FAILED", e.message, e)
        }
    }

    private fun getPinnedHttpClient(context: Context): OkHttpClient {
        val cf = CertificateFactory.getInstance("X.509")
        val caInput = context.resources.openRawResource(R.raw.globalca) // server.pem → res/raw/server.pem
        val ca = cf.generateCertificate(caInput)
        Log.d("TLS", "Using CA: $ca")
        caInput.close()

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("ca", ca)

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, tmf.trustManagers, null)

        val trustManager = tmf.trustManagers[0] as X509TrustManager
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }
}


//class SecureHttpSDownloaderModule {
//}