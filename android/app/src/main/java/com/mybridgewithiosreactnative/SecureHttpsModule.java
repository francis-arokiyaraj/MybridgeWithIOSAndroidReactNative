package com.mybridgewithiosreactnative;

//public class SecureHttpsModule {
//}

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.*;

        import java.io.*;
        import java.net.URL;
import java.security.*;
        import java.security.cert.*;
import java.security.cert.Certificate;

import javax.net.ssl.*;

        import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SecureHttpsModule extends ReactContextBaseJavaModule {
    private static final String TAG = "SecureHttpsModule";
    private final ReactApplicationContext reactContext;

    public SecureHttpsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "SecureHttps";
    }

    @ReactMethod
    public void downloadFile(String filename, Promise promise) {
        try {
            OkHttpClient client = getSecureOkHttpClient();

            String url = "https://192.168.0.185:8443/api/data?file=" + filename;

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                promise.reject("DOWNLOAD_FAILED", "Server responded with code: " + response.code());
                return;
            }

            // Save file to app internal storage
            File file = new File(reactContext.getFilesDir(), filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(response.body().bytes());
            fos.close();

            promise.resolve("File saved to: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error downloading file", e);
            promise.reject("ERROR", e.getMessage(), e);
        }
    }

    private OkHttpClient getSecureOkHttpClient() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        InputStream caInput = reactContext.getResources().openRawResource(
                reactContext.getResources().getIdentifier("server_iotec_pem", "raw", reactContext.getPackageName()));
        Certificate ca = cf.generateCertificate(caInput);
        caInput.close();

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        return new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                .hostnameVerifier((hostname, session) -> hostname.equals("192.168.0.185"))
                .build();
    }
}
