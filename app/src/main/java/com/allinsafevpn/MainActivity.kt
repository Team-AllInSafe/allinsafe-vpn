package com.allinsafevpn

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.app.ActivityCompat
import com.allinsafevpn.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    val REQUEST_CODE_PREPARE_VPN = 1001
    lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startVpn.setOnClickListener{
            val intent = VpnService.prepare(this)
            if (intent != null) {//vpn 허용해줘
                startActivityForResult(intent, REQUEST_CODE_PREPARE_VPN)
            } else {
                // 이미 허용됨! 바로 VPN 연결 가능
                ConnectVPN()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PREPARE_VPN && resultCode == RESULT_OK) {
            ConnectVPN()
        }
    }

    private fun ConnectVPN(){
        val success = NativeVpnBridge.connect(applicationContext, "3.36.105.39", "sua", "1234")
        if (success) {
            Log.d("VPN", "VPN 연결 시도됨")
        }
        val certExists = applicationContext.assets.list("")?.contains("ca-cert.pem") == true
        Log.d("JNI-Bridge", "ca-cert.pem exists? $certExists")

    }

}