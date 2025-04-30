package com.vpnclientapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.IpSecManager
import android.net.IpSecTransform
import android.net.LinkAddress
import android.net.Network
import android.net.VpnService
import android.os.Build
import androidx.annotation.RequiresApi
import android.net.ipsec.ike.*
import android.system.OsConstants.AF_INET
import android.os.Handler
import android.os.HandlerThread
import android.system.OsConstants
import java.net.InetAddress
import android.util.Log

@RequiresApi(Build.VERSION_CODES.S) // Android 12 이상


class MyVpnService : VpnService() {
    companion object {
        const val EXTRA_SERVER = "SERVER_HOSTNAME"
        const val EXTRA_PSK    = "PRE_SHARED_KEY"
    }

    private var handlerThread: HandlerThread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val server = intent?.getStringExtra(EXTRA_SERVER)
        val psk    = intent?.getStringExtra(EXTRA_PSK)
        if (server.isNullOrBlank() || psk.isNullOrBlank()) {
            Log.e("MyVpnService", "Missing SERVER_HOSTNAME or PRE_SHARED_KEY")
            stopSelf()
            return START_NOT_STICKY
        }

        // 1) VpnService.Builder 로 기본 터널 UI 설정
        val builder = Builder().apply {
            setSession("MyCustomVpn")
            addAddress("3.36.105.39", 8) //TODO :: 8 맞나??
            addRoute("0.0.0.0", 0)
            setConfigureIntent(
                PendingIntent.getActivity(
                    this@MyVpnService, 0,
                    Intent(this@MyVpnService, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        builder.establish()

        // 2) 별도 스레드에서 IKEv2 PSK 연결
        handlerThread = HandlerThread("VpnIkeThread").also { it.start() }
        val handler = Handler(handlerThread!!.looper)
        handler.post {
            connectIkeWithPsk(
                context        = this,
                serverHostname = "allinsafe.vpn.com",
                preSharedKey   = "ltgogo1897$$"
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread?.quitSafely()
    }

    private fun connectIkeWithPsk(
        context: VpnService,
        serverHostname: String,
        preSharedKey:   String
    ) {
        val ipSecManager = context.getSystemService(IpSecManager::class.java)
        val cm           = context.getSystemService(ConnectivityManager::class.java)
        var network: Network? = null
        cm.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(n: Network) {
                    super.onAvailable(n)
                    network = n
                }
            }
        )

        // IKE SA 파라미터
        val ikeParams = IkeSessionParams.Builder()
            .setServerHostname(serverHostname)
            .setAuthPsk(preSharedKey.toByteArray(Charsets.UTF_8))
            .setRemoteIdentification(IkeFqdnIdentification(serverHostname))
            .addIkeSaProposal(
                IkeSaProposal.Builder()
                    .addDhGroup(SaProposal.DH_GROUP_2048_BIT_MODP)
                    .addEncryptionAlgorithm(SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, 256)
                    .addIntegrityAlgorithm(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA2_256_128)
                    .addPseudorandomFunction(SaProposal.PSEUDORANDOM_FUNCTION_SHA2_256)
                    .build()
            )
            .build()

        // Child SA (터널) 파라미터
        val childParams = TunnelModeChildSessionParams.Builder()
            .addChildSaProposal(
                ChildSaProposal.Builder()
                    .addEncryptionAlgorithm(SaProposal.ENCRYPTION_ALGORITHM_AES_CBC, 128)
                    .addIntegrityAlgorithm(SaProposal.INTEGRITY_ALGORITHM_HMAC_SHA1_96)
                    .build()
            )
            .addInternalAddressRequest(OsConstants.AF_INET)
            .addInternalDnsServerRequest(OsConstants.AF_INET)
            .build()

        // 콜백 스레드
        val cbThread = HandlerThread("IkeCallbackThread").also { it.start() }
        val cbHandler = Handler(cbThread.looper)
        var tunnelInterface: Any? = null

        // IKE 콜백
        val ikeCallback = object : IkeSessionCallback {
            override fun onOpened(cfg: IkeSessionConfiguration) {
                val info: IkeSessionConnectionInfo = cfg.getIkeSessionConnectionInfo()
                val local  = info.localAddress
                val remote = info.remoteAddress
                Log.d("MyVpnService", "IKE opened: local=$local, remote=$remote")

                tunnelInterface = ipSecManager.javaClass
                    .getMethod(
                        "createIpSecTunnelInterface",
                        InetAddress::class.java,
                        InetAddress::class.java,
                        Network::class.java
                    )
                    .invoke(ipSecManager, local, remote, network)
            }
            override fun onClosed() {
                Log.d("MyVpnService", "IKE closed")
            }
        }

        // Child 콜백
        val childCallback = object : ChildSessionCallback {
            override fun onOpened(cfg: ChildSessionConfiguration) {
                Log.d("MyVpnService", "Child SA opened")
                @Suppress("UNCHECKED_CAST")
                val addrs = cfg.javaClass
                    .getMethod("getInternalAddresses")
                    .invoke(cfg) as List<LinkAddress>
                for (addr in addrs) {
                    Class.forName("android.net.IpSecManager\$IpSecTunnelInterface")
                        .getMethod("addAddress", InetAddress::class.java, Int::class.java)
                        .invoke(tunnelInterface, addr.address, addr.prefixLength)
                }
            }
            override fun onClosed() {
                Log.d("MyVpnService", "Child SA closed")
            }
            override fun onIpSecTransformCreated(transform: IpSecTransform, reqId: Int) {
                Log.d("MyVpnService", "Transform created reqId=$reqId")
                ipSecManager.javaClass
                    .getMethod(
                        "applyTunnelModeTransform",
                        Class.forName("android.net.IpSecManager\$IpSecTunnelInterface"),
                        Int::class.java,
                        IpSecTransform::class.java
                    )
                    .invoke(ipSecManager, tunnelInterface, reqId, transform)
            }
            override fun onIpSecTransformDeleted(transform: IpSecTransform, reqId: Int) {
                Log.d("MyVpnService", "Transform deleted reqId=$reqId")
            }
        }

        // 세션 생성
        IkeSession(
            context,
            ikeParams,
            childParams,
            { task -> cbHandler.post(task) },
            ikeCallback,
            childCallback
        )
    }
}
/*
    val ikeCallback = object : IkeSessionCallback {
        override fun onOpened(sessionConfig: IkeSessionConfiguration) {
            val connInfo = sessionConfig.getIkeSessionConnectionInfo()
            val localAddr = connInfo.localAddress
            val remoteAddr = connInfo.remoteAddress
            Log.d("VPN", "IKE opened – local=$localAddr, remote=$remoteAddr")

            tunnelInterface = ipSecManager.javaClass
                .getMethod(
                    "createIpSecTunnelInterface",
                    InetAddress::class.java,
                    InetAddress::class.java,
                    Network::class.java
                )
                .invoke(ipSecManager, localAddr, remoteAddr, network)
        }
        override fun onClosed() { Log.d("VPN", "IKE closed") }
    }

    val childCallback = object : ChildSessionCallback {
        override fun onOpened(childConfig: ChildSessionConfiguration) {
            Log.d("VPN", "Child SA opened")
            @Suppress("UNCHECKED_CAST")
            val addrs = childConfig.javaClass
                .getMethod("getInternalAddresses")
                .invoke(childConfig) as List<LinkAddress>
            for (addr in addrs) {
                Class.forName("android.net.IpSecManager\$IpSecTunnelInterface")
                    .getMethod("addAddress", InetAddress::class.java, Int::class.java)
                    .invoke(tunnelInterface, addr.address, addr.prefixLength)
            }
        }
        override fun onClosed() { Log.d("VPN", "Child SA closed") }
        override fun onIpSecTransformCreated(transform: IpSecTransform, reqId: Int) {
            Log.d("VPN", "IPSec transform created, reqId=$reqId")
            ipSecManager.javaClass
                .getMethod(
                    "applyTunnelModeTransform",
                    Class.forName("android.net.IpSecManager\$IpSecTunnelInterface"),
                    Int::class.java,
                    IpSecTransform::class.java
                )
                .invoke(ipSecManager, tunnelInterface, reqId, transform)
        }
        override fun onIpSecTransformDeleted(transform: IpSecTransform, reqId: Int) {
            Log.d("VPN", "IPSec transform deleted, reqId=$reqId")
        }
    }

// 6-2) connect 함수 안에서 IkeSession 생성 시에 사용
    ref = IkeSession(
        context,
        ikeParams,
        childParams,
        { task -> handler.post(task) },
        ikeCallback,      // 여기
        childCallback     // 여기
    )


}
*/
