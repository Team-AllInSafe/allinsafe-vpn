package com.allinsafevpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.app.ActivityCompat.startActivityForResult
import java.io.File
import java.io.InputStream

object NativeVpnBridge {
    init {
        System.loadLibrary("charon-bridge")
    }
    external fun registerCredentials(server: String, id: String, pw: String, certPath: String): Boolean

    fun connect(context: Context, server: String, id: String, pw: String): Boolean {
//
        val certFile = File(context.filesDir, "ca-cert.pem")
        copyAssetToFile(context,"ca-cert.pem",certFile)
        val certPath = certFile.absolutePath
//        Log.d("JNI-Bridge","certPath certBytes $certBytes certFile")

        return registerCredentials(server, id, pw, certPath)
    }
    fun copyAssetToFile(context: Context, assetName: String, destFile: File) {
        context.assets.open(assetName).use { inputStream ->
            destFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }



}