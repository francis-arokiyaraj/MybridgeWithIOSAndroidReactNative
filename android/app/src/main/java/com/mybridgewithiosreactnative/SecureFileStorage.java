package com.mybridgewithiosreactnative;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

import android.os.Environment;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;




public class SecureFileStorage extends ReactContextBaseJavaModule {

    private final Context context;
    private final String KEY_ALIAS = "my_secure_key";

    public SecureFileStorage(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "SecureFileStorage";
    }

    // --- Key setup ---
    private void generateSymmetricKeyIfNeeded() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(
                    new KeyGenParameterSpec.Builder(KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build()
            );
            keyGenerator.generateKey();
        }
    }

    private SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    // --- File utilities ---
    private byte[] readFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return fis.readAllBytes();
            } else {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int nRead;
                while ((nRead = fis.read(data)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                return buffer.toByteArray();
            }
        }
    }

    private void writeFile(File file, byte[] data) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    private byte[] readFromContentUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new FileNotFoundException("Unable to open stream for URI: " + uri);
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }
        inputStream.close();
        return buffer.toByteArray();
    }

    // --- Store file from any path ---
    @ReactMethod
    public void storeFileFromPath(String sourcePath, String destFileName, Promise promise) {
        try {
            generateSymmetricKeyIfNeeded();
            byte[] inputBytes;

            if (sourcePath.startsWith("content://")) {
                Uri uri = Uri.parse(sourcePath);
                inputBytes = readFromContentUri(context, uri);
            } else {
                File sourceFile = new File(sourcePath.replace("file://", ""));
                inputBytes = readFile(sourceFile);
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(inputBytes);

            File outputFile = new File(context.getFilesDir(), destFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(iv.length); // Write IV length
                fos.write(iv);        // Write IV
                fos.write(encrypted); // Write ciphertext
            }

            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("ENCRYPTION_ERROR", e);
        }
    }

    // --- Decrypt file to Downloads ---
    @ReactMethod
    public void getDecryptedFileToDownloads(String storedFileName, String destFileName, Promise promise) {
        try {
            File inputFile = new File(context.getFilesDir(), storedFileName);
            FileInputStream fis = new FileInputStream(inputFile);

            int ivLength = fis.read();
            byte[] iv = new byte[ivLength];
            fis.read(iv);

            byte[] encryptedData;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                encryptedData = fis.readAllBytes();
            } else {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int nRead;
                while ((nRead = fis.read(data)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                encryptedData = buffer.toByteArray();
            }

            fis.close();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, iv));
            byte[] decrypted = cipher.doFinal(encryptedData);

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File outputFile = new File(downloadsDir, destFileName);
            writeFile(outputFile, decrypted);

            promise.resolve(outputFile.getAbsolutePath());
        } catch (Exception e) {
            promise.reject("DECRYPTION_ERROR", e);
        }
    }

    // --- Save string to encrypted file ---
    @ReactMethod
    public void saveFile(String filename, String content, Promise promise) {
        Log.d("tag", "inside bridge call");
        try {
            generateSymmetricKeyIfNeeded();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

            File file = new File(context.getFilesDir(), filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(iv.length);
                fos.write(iv);
                fos.write(encrypted);
            }

            promise.resolve("Saved");
        } catch (Exception e) {
            promise.reject("SAVE_FAILED", e);
        }
    }

    // --- Read and decrypt string from file ---
    @ReactMethod
    public void readFile(String filename, Promise promise) {
        try {
            File file = new File(context.getFilesDir(), filename);
            try (FileInputStream fis = new FileInputStream(file)) {
                int ivLength = fis.read();
                byte[] iv = new byte[ivLength];
                fis.read(iv);

                byte[] encryptedData;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    encryptedData = fis.readAllBytes();
                } else {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[1024];
                    int nRead;
                    while ((nRead = fis.read(data)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    encryptedData = buffer.toByteArray();
                }

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, iv));

                byte[] decrypted = cipher.doFinal(encryptedData);
                promise.resolve(new String(decrypted, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            promise.reject("READ_FAILED", e);
        }
    }

    @ReactMethod
    public void exportFileToDownloads(String filename, Promise promise) {
        boolean success = CopyFileToDownloads.copyToDownloads(getReactApplicationContext(), filename);
        if (success) {
            promise.resolve("File exported to Downloads.");
        } else {
            promise.reject("EXPORT_FAILED", "Could not copy file to Downloads.");
        }
    }
}


//
//public class SecureFileStorage extends ReactContextBaseJavaModule {
//    private final Context context;
//    private final String KEY_ALIAS = "my_secure_key";
//    private final SecretKey secretKey;
//    private final byte[] iv;
//    public SecureFileStorage(ReactApplicationContext reactContext) {
//        super(reactContext);
//        this.context = reactContext;
//        this.secretKey = generateKey();
//        this.iv = generateIv();
//    }
//
//
//    @NonNull
//    @Override
//    public String getName() {
//        return "SecureFileStorage";
//    }
//
//    private SecretKey generateKey() {
//        try {
//            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//            keyGen.init(256); // Use 128 if 256 isn't supported
//            return keyGen.generateKey();
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to generate AES key", e);
//        }
//    }
//
//    private byte[] generateIv() {
//        byte[] iv = new byte[16];
//        for (int i = 0; i < iv.length; i++) {
//            iv[i] = (byte) i; // Fixed IV for demo only. Use SecureRandom in production.
//        }
//        return iv;
//    }
//
//    private void generateSymmetricKeyIfNeeded() throws Exception {
//        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
//        keyStore.load(null);
//
//        if (!keyStore.containsAlias(KEY_ALIAS)) {
//            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
//            keyGenerator.init(
//                    new KeyGenParameterSpec.Builder(KEY_ALIAS,
//                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
//                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
//                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
//                            .build()
//            );
//            keyGenerator.generateKey();
//        }
//    }
//
//    @ReactMethod
//    public void saveFile(String filename, String content, Promise promise) {
//        Log.d("tag","inside bridge call");
//        try {
//            generateSymmetricKeyIfNeeded();
//
//            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
//            keyStore.load(null);
//
//            SecretKey key = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
//            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//            cipher.init(Cipher.ENCRYPT_MODE, key);
//
//            byte[] iv = cipher.getIV();
//            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
//
//            File file = new File(context.getFilesDir(), filename);
//            try (FileOutputStream fos = new FileOutputStream(file)) {
//                fos.write(iv.length);
//                fos.write(iv);
//                fos.write(encrypted);
//            }
//
//            promise.resolve("Saved");
//        } catch (Exception e) {
//            promise.reject("SAVE_FAILED", e);
//        }
//    }
//
//    @ReactMethod
//    public void readFile(String filename, Promise promise) {
//        try {
//            File file = new File(context.getFilesDir(), filename);
//            try (FileInputStream fis = new FileInputStream(file)) {
//                int ivLength = fis.read();
//                byte[] iv = new byte[ivLength];
//                fis.read(iv);
//
//                byte[] encryptedData = null;
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    encryptedData = fis.readAllBytes();
//                }
//
//                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
//                keyStore.load(null);
//
//                SecretKey key = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
//                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
//
//                byte[] decrypted = cipher.doFinal(encryptedData);
//                promise.resolve(new String(decrypted, StandardCharsets.UTF_8));
//            }
//        } catch (Exception e) {
//            promise.reject("READ_FAILED", e);
//        }
//    }
//
////    @ReactMethod
////    public void storeFileFromPath(String sourcePath, String destFileName, Promise promise) {
////        try {
////            File sourceFile = new File(sourcePath);
////            byte[] inputBytes = readFile(sourceFile);
////
////            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
////            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
////
////            byte[] encrypted = cipher.doFinal(inputBytes);
////            File outputFile = new File(getReactApplicationContext().getFilesDir(), destFileName);
////            writeFile(outputFile, encrypted);
////
////            promise.resolve(null);
////        } catch (Exception e) {
////            promise.reject("ENCRYPTION_ERROR", e);
////        }
////    }
//
//    @ReactMethod
//    public void storeFileFromPath(String sourcePath, String destFileName, Promise promise) {
//        try {
//            Context context = getReactApplicationContext();
//            byte[] inputBytes;
//
//            if (sourcePath.startsWith("content://")) {
//                Uri uri = Uri.parse(sourcePath);
//                inputBytes = readFromContentUri(context, uri);
//            } else {
//                File sourceFile = new File(sourcePath.replace("file://", ""));
//                inputBytes = readFile(sourceFile);
//            }
//
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
//
//            byte[] encrypted = cipher.doFinal(inputBytes);
//            File outputFile = new File(context.getFilesDir(), destFileName);
//            writeFile(outputFile, encrypted);
//            Log.d("TAG","Saved successfully");
//            promise.resolve(null);
//        } catch (Exception e) {
//            promise.reject("ENCRYPTION_ERROR", e);
//        }
//    }
//
//    private byte[] readFromContentUri(Context context, Uri uri) throws IOException {
//        InputStream inputStream = context.getContentResolver().openInputStream(uri);
//        if (inputStream == null) {
//            throw new FileNotFoundException("Unable to open stream for URI: " + uri);
//        }
//
//        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//        byte[] data = new byte[1024];
//        int nRead;
//        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
//            buffer.write(data, 0, nRead);
//        }
//
//        inputStream.close();
//        return buffer.toByteArray();
//    }
//
//    @ReactMethod
//    public void getDecryptedFileContent(String destFileName, Promise promise) {
//        try {
//            File inputFile = new File(getReactApplicationContext().getFilesDir(), destFileName);
//            byte[] encrypted = readFile(inputFile);
//
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
//
//            byte[] decrypted = cipher.doFinal(encrypted);
//            promise.resolve(new String(decrypted));
//        } catch (Exception e) {
//            promise.reject("DECRYPTION_ERROR", e);
//        }
//    }
//
////    @ReactMethod
////    public void getDecryptedFileToDownloads(String storedFileName, String destFileName, Promise promise) {
////        Log.d("TAG", storedFileName);
////        Log.d("destination", destFileName);
////        try {
////            File inputFile = new File(getReactApplicationContext().getFilesDir(), storedFileName);
////            byte[] encrypted = readFile(inputFile);
////
////            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
////            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
////
////            byte[] decrypted = cipher.doFinal(encrypted);
////
////            // Create output file in Downloads
////            File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
////            File outputFile = new File(downloadsDir, "decrypted_" + destFileName);
////            writeFile(outputFile, decrypted);
////
////            promise.resolve(outputFile.getAbsolutePath());
////        } catch (Exception e) {
////            promise.reject("DECRYPTION_ERROR", e);
////        }
////    }
////
//
//    @ReactMethod
//    public void getDecryptedFileToDownloads(String storedFileName, String destFileName, Promise promise) {
//        try {
//            File inputFile = new File(getReactApplicationContext().getFilesDir(), storedFileName);
//            byte[] encrypted = readFile(inputFile);
//
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
//
//            byte[] decrypted = cipher.doFinal(encrypted);
//
//            // Output to Downloads/output.pem
//            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//            File outputFile = new File(downloadsDir, destFileName);
//
//            writeFile(outputFile, decrypted);
//
//            promise.resolve(outputFile.getAbsolutePath());
//        } catch (Exception e) {
//            promise.reject("DECRYPTION_ERROR", e);
//        }
//    }
//
//    private byte[] readFile(File file) throws Exception {
//        try (FileInputStream fis = new FileInputStream(file)) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                return fis.readAllBytes();
//            }
//        }
//        return new byte[0];
//    }
//
//    private void writeFile(File file, byte[] data) throws Exception {
//        try (FileOutputStream fos = new FileOutputStream(file)) {
//            fos.write(data);
//        }
//    }
//
//}
//
////public class SecureFileStore {
////}
