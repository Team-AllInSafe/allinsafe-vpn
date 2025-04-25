package com.allinsafevpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.app.ActivityCompat.startActivityForResult
import java.io.File
import java.io.InputStream

object NativeVpnBridge {
    init {
        System.loadLibrary("charon-bridge")
    }
    external fun registerCredentials(server: String, id: String, pw: String, certBytes: InputStream, cerPath:String): Boolean

    fun connect(context: Context, server: String, id: String, pw: String): Boolean {
//        val certBytes = context.assets.open("ca-cert.pem").use { it.readBytes() }
//        val certPath = File(context.filesDir, "ca-cert.pem").absolutePath
        val certBytes = context.assets.open("ca-cert.pem")
        val certFile = File(context.filesDir, "ca-cert.pem")
        certBytes.use { input ->
            certFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val certPath = certFile.absolutePath
        return registerCredentials(server, id, pw, certBytes, certPath)
    }



}