package com.mybridgewithiosreactnative

import android.content.Context
import android.util.Base64
import com.facebook.react.bridge.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class SecureFileBridgeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val secretKey: SecretKey by lazy {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        keyGen.generateKey()
    }

    override fun getName(): String = "SecureFileBridge"

    @ReactMethod
    fun storeFileFromPath(sourcePath: String, destFileName: String, promise: Promise) {
        try {
            val file = File(sourcePath)
            val input = FileInputStream(file).readBytes()

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16)
            iv.indices.forEach { iv[it] = it.toByte() }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            val encrypted = cipher.doFinal(input)
            val outputFile = File(reactApplicationContext.filesDir, destFileName)
            FileOutputStream(outputFile).use { it.write(encrypted) }

            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("ERROR", e)
        }
    }

    @ReactMethod
    fun getDecryptedFileContent(destFileName: String, promise: Promise) {
        try {
            val inputFile = File(reactApplicationContext.filesDir, destFileName)
            val encrypted = FileInputStream(inputFile).readBytes()

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16)
            iv.indices.forEach { iv[it] = it.toByte() }
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            val decrypted = cipher.doFinal(encrypted)
            promise.resolve(String(decrypted))
        } catch (e: Exception) {
            promise.reject("ERROR", e)
        }
    }
}

//class SecureFileBridgeModule {
//}