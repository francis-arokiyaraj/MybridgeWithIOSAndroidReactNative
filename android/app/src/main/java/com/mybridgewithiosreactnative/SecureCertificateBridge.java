package com.mybridgewithiosreactnative;


import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class SecureCertificateBridge extends ReactContextBaseJavaModule {

    private static final String KEY_ALIAS = "MySecureCertKey";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    public SecureCertificateBridge(ReactApplicationContext reactContext) {
        super(reactContext);
        generateKeyIfNeeded();
    }

    @Override
    public String getName() {
        return "SecureCertificate";
    }

    private void generateKeyIfNeeded() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
                keyGenerator.init(new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build());
                keyGenerator.generateKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        return ((SecretKey) keyStore.getKey(KEY_ALIAS, null));
    }

    @ReactMethod
    public void storeCertificate(String filePath, String certName, Promise promise) {
        Log.d("TAG","init success");
        try {
            byte[] fileData = readFile(new File(filePath));
            SecretKey key = getSecretKey();

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            byte[] iv = new byte[16];
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] encrypted = cipher.doFinal(fileData);
            File file = new File(getReactApplicationContext().getFilesDir(), certName);
            file.createNewFile();
            file.setWritable(true);
            file.setReadable(true);
            file.setExecutable(true);

            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(encrypted);
            outputStream.close();
            Log.d("TAG","Saved success");
            promise.resolve("Saved successfully");
        } catch (Exception e) {
            promise.reject("STORE_ERROR", e);
        }
    }

    @ReactMethod
    public void readCertificate(String certName, Promise promise) {
        try {
            File file = new File(getReactApplicationContext().getFilesDir(), certName);
            byte[] encrypted = readFile(file);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            byte[] iv = new byte[16];
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            promise.resolve(Base64.encodeToString(decrypted, Base64.DEFAULT));
        } catch (Exception e) {
            promise.reject("READ_ERROR", e);
        }
    }

    private byte[] readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] content = new byte[(int) file.length()];
        fis.read(content);
        fis.close();
        return content;
    }
}


//
//import android.os.Build;
//import android.security.keystore.KeyGenParameterSpec;
//import android.security.keystore.KeyProperties;
//import android.util.Base64;
//
//import com.facebook.react.bridge.*;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.FileInputStream;
//import java.security.KeyStore;
//import java.security.SecureRandom;
//
//import javax.crypto.*;
//import javax.crypto.spec.IvParameterSpec;
//
//public class SecureCertificateBridge extends ReactContextBaseJavaModule {
//    private static final String KEY_ALIAS = "CertSecureKey";
//    private final File storageDir;
//
//    public SecureCertificateBridge(ReactApplicationContext reactContext) {
//        super(reactContext);
//        storageDir = new File(reactContext.getFilesDir(), "certs");
//        if (!storageDir.exists()) storageDir.mkdirs();
//        generateKeyIfNeeded();
//    }
//
//    @Override
//    public String getName() {
//        return "SecureCertificateBridge";
//    }
//
//    private void generateKeyIfNeeded() {
//        try {
//            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
//            ks.load(null);
//            if (!ks.containsAlias(KEY_ALIAS)) {
//                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
//                        KEY_ALIAS,
//                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
//                )
//                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
//                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
//                        .setKeySize(256)
//                        .build();
//
//                KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
//                keyGen.init(spec);
//                keyGen.generateKey();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private SecretKey getSecretKey() throws Exception {
//        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
//        ks.load(null);
//        return ((SecretKey) ks.getKey(KEY_ALIAS, null));
//    }
//
//    @ReactMethod
//    public void storeCertificate(String name, String base64Content, Promise promise) {
//        try {
//            byte[] data = Base64.decode(base64Content, Base64.DEFAULT);
//            SecretKey key = getSecretKey();
//
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
//            byte[] iv = new byte[16];
//            new SecureRandom().nextBytes(iv);
//            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
//
//            byte[] encrypted = cipher.doFinal(data);
//            File file = new File(storageDir, name);
//            FileOutputStream fos = new FileOutputStream(file);
//            fos.write(iv);
//            fos.write(encrypted);
//            fos.close();
//
//            promise.resolve("Stored securely");
//        } catch (Exception e) {
//            promise.reject("STORE_ERROR", e);
//        }
//    }
//
//    @ReactMethod
//    public void getCertificate(String name, Promise promise) {
//        try {
//            File file = new File(storageDir, name);
//            FileInputStream fis = new FileInputStream(file);
//            byte[] iv = new byte[16];
//            fis.read(iv);
//            byte[] encrypted = null;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                encrypted = fis.readAllBytes();
//            }
//            fis.close();
//
//            SecretKey key = getSecretKey();
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
//            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
//
//            byte[] decrypted = cipher.doFinal(encrypted);
//            String base64 = Base64.encodeToString(decrypted, Base64.NO_WRAP);
//            promise.resolve(base64);
//        } catch (Exception e) {
//            promise.reject("READ_ERROR", e);
//        }
//    }
//
//    @ReactMethod
//    public void deleteCertificate(String name, Promise promise) {
//        File file = new File(storageDir, name);
//        if (file.delete()) {
//            promise.resolve("Deleted");
//        } else {
//            promise.reject("DELETE_ERROR", "File not found or could not be deleted.");
//        }
//    }
//}
