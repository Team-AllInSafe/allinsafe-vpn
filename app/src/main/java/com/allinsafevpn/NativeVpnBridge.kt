package com.allinsafevpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.app.ActivityCompat.startActivityForResult
import java.io.File

object NativeVpnBridge {
    init {
        System.loadLibrary("charon-bridge")
    }
    external fun startVpn(server: String, id: String, pw: String, certBytes: ByteArray,cerPath:String): Boolean

    fun connect(context: Context, server: String, id: String, pw: String): Boolean {
        val certBytes = context.assets.open("ca-cert.pem").use { it.readBytes() }
        val certPath = File(context.filesDir, "ca-cert.pem").absolutePath

        return startVpn(server, id, pw, certBytes, certPath)
    }



}