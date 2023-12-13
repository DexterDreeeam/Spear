package org.p9.spear

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import org.p9.spear.component.ConnectionKeeper
import org.p9.spear.component.Gateway
import org.p9.spear.component.IGateway
import org.p9.spear.constant.VPN_END_ACTION
import org.p9.spear.constant.VPN_START_ACTION
import org.p9.spear.entity.ProxyMode
import java.net.InetAddress
import java.net.Socket

class SpearVpn : VpnService() {

    private val configureIntent: PendingIntent by lazy {
        var activityFlag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activityFlag += PendingIntent.FLAG_MUTABLE
        }
        PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), activityFlag)
    }

    private lateinit var sharedPreferences: SharedPreferences

    private var connectionKeeper: ConnectionKeeper? = null
    private var vpnInterface: ParcelFileDescriptor? = null
    private var ipAddr: String? = null
    private var port: String? = null
    // private var endpoint: String? = null
    private var proxyMode: ProxyMode? = null
    private var appsList: List<String>? = null
    private var gateway: IGateway? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var act = intent?.action
        return when (act) {
            VPN_START_ACTION -> {
                if (!preConnect()) {
                    notifyActivity(VPN_START_ACTION, false)
                }
                START_STICKY
            }
            VPN_END_ACTION -> {
                disconnect()
                notifyActivity(VPN_END_ACTION, true)
                START_NOT_STICKY
            }
            else -> {
                START_NOT_STICKY
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    private fun loadConfigs(): Boolean {
        sharedPreferences = getSharedPreferences("SpearSharedPreferences", MODE_PRIVATE)
        val token = getStorage("connect_proxy_token", "")
        val parts = token.split(":")
        if (parts.size < 2 || parts[0].length < 8 || parts[1].isEmpty()) {
            return false;
        }

        ipAddr = parts[0]
        port = parts[1]
        proxyMode = ProxyMode.fromString(sharedPreferences.getString("proxy_mode", "").toString())
        appsList = listOf()

        if (proxyMode == ProxyMode.Package) {
            val checklistStr = sharedPreferences.getString("package_checklist", "").toString()
            var checklist = mutableListOf<String>()
            checklistStr.split(",").map {
                app -> checklist.add(app)
            }
            appsList = checklist
        }

        return true
    }

    private fun preConnect(): Boolean {
        if (!loadConfigs()) {
            return false
        }

        var ept = ""
        if (ipAddr != null && port != null) {
            ept = "$ipAddr:$port"
        } else {
            return false
        }

        val keeper = ConnectionKeeper()
        keeper.initialize(
            ept,
            "password",
            onBroken = { disconnect() },
            onContinue = { isSuccess ->
                postConnect(isSuccess)
            })

        connectionKeeper = keeper
        return true
    }

    private fun postConnect(preSuccess: Boolean) {
        if (!preSuccess) {
            notifyActivity(VPN_START_ACTION, false)
            return
        }

        val eptPort: String = connectionKeeper?.vpnTransportPort() ?: ""
        if (eptPort == "") {
            notifyActivity(VPN_START_ACTION, false)
            return
        }

        vpnInterface = createVpnInterface() ?: null
        if (vpnInterface == null) {
            notifyActivity(VPN_START_ACTION, false)
            return
        }

        if (vpnInterface?.fileDescriptor == null) {
            notifyActivity(VPN_START_ACTION, false)
            return
        }

        val fd = vpnInterface?.fileDescriptor!!
        gateway = Gateway(this, "$ipAddr:$eptPort")
        gateway?.start(fd)
        notifyActivity(VPN_START_ACTION, true)
    }

    private fun disconnect() {
        gateway?.stop()
        vpnInterface?.close()
        connectionKeeper?.stop()

        gateway = null
        vpnInterface = null
        connectionKeeper = null
        System.gc()
    }

    private fun createVpnInterface(): ParcelFileDescriptor? {
        val builder = Builder()
        appsList?.forEach {
            app ->
                if (app != "") {
                    Log.i(javaClass.name, "Route for package: $app")
                    builder.addAllowedApplication(app)
                }
        }

        val vpnTun: String = connectionKeeper?.vpnTunAddr() ?: ""
        val vpnDns: String = connectionKeeper?.vpnDns() ?: ""
        if (vpnTun == "" || vpnDns == "") {
            return null
        }

        Log.i(javaClass.name, "VPN Tun Address: $vpnTun")
        Log.i(javaClass.name, "VPN Dns Address: $vpnDns")

        return builder
            .addAddress(vpnTun, 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(vpnDns)
            .setSession("SpearVpnSession")
            .setBlocking(true)
            .setConfigureIntent(configureIntent)
            .also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.setMetered(false)
                }
            }
            .establish() ?: throw IllegalStateException("Init vpnInterface failed")
    }

    private fun notifyActivity(action: String, result: Boolean) {
        val intent = Intent(action)
        intent.putExtra("result", result)
        sendBroadcast(intent)
    }

    private fun getStorage(key: String, default: String): String {
        return sharedPreferences.getString(key, null) ?: default
    }
}
