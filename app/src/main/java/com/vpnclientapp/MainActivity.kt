package com.vpnclientapp

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat



class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //디버깅 중 25.04.30
        //prepareVpn()
        //startVpnService()

    }
    private val VPN_REQUEST_CODE = 0x0F

    @RequiresApi(Build.VERSION_CODES.O)
    fun prepareVpn() {
        // 1) 이미 권한이 승인됐는지 확인
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // 사용자에게 권한 다이얼로그 띄우기
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            // 이미 권한이 있으므로 바로 onActivityResult 호출 처리
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null)
        }
    }

    // 2) onActivityResult 에서 결과 처리
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // 권한 획득 → 서비스 시작
                startVpnService()
            } else {
                // 권한 거부 처리 (토스트나 UI 업데이트)
                Toast.makeText(this, "VPN 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // VPN 시작
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startVpnService() {
        Intent(this, MyVpnService::class.java).also { intent ->
            // PSK나 서버 호스트네임 같은 인자를 넘겨야 한다면 여기에 추가
            intent.putExtra("SERVER_HOSTNAME", "allinsafe.vpn.com")
            intent.putExtra("PRE_SHARED_KEY", "ltgogo1897$$")
            // 포그라운드 서비스로 시작 (API 26+ 권장)
            startForegroundService(intent)
        }
    }

    // VPN 중지
    private fun stopVpnService() {
        Intent(this, MyVpnService::class.java).also { intent ->
            stopService(intent)
        }
    }


}