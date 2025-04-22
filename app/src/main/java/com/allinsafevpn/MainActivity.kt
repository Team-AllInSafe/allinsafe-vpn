package com.allinsafevpn

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val success = NativeVpnBridge.connect(applicationContext, "52.78.xxx.xxx", "sua", "1234")
        if (success) {
            Log.d("VPN", "VPN 연결 시도됨")
        }

    }
}