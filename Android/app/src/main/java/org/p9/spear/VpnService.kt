package org.p9.spear

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import org.p9.spear.component.Gateway
import org.p9.spear.component.IGateway
import org.p9.spear.constant.VPN_END_ACTION
import org.p9.spear.constant.VPN_START_ACTION
import org.p9.spear.entity.ProxyMode

class SpearVpn : VpnService() {

    private val configureIntent: PendingIntent by lazy {
        var activityFlag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activityFlag += PendingIntent.FLAG_MUTABLE
        }
        PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), activityFlag)
    }

    private lateinit var vpnInterface: ParcelFileDescriptor

    private lateinit var endpoint: String

    private lateinit var proxyMode: ProxyMode

    private lateinit var appsList: List<String>

    private lateinit var gateway: IGateway

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var act = intent?.action
        return when (act) {
            VPN_START_ACTION -> {
                connect()
                notifyActivity(act, true)
                START_STICKY
            }
            VPN_END_ACTION -> {
                disconnect()
                notifyActivity(act, true)
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

    private fun loadConfigs() {
        val sharedPreferences = getSharedPreferences("SpearSharedPreferences", MODE_PRIVATE)
        endpoint = "20.255.49.236:22333" // sharedPreferences.getString("endpoint", "").toString()
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

        gateway = Gateway(this, endpoint)
    }

    private fun connect() {
        loadConfigs()
        vpnInterface = createVpnInterface()
        gateway.start(vpnInterface.fileDescriptor)
    }

    private fun disconnect() {
        gateway.stop()
        vpnInterface.close()
        System.gc()
    }

    private fun createVpnInterface(): ParcelFileDescriptor {
        val builder = Builder()
        appsList.forEach {
            app ->
                if (app != "") {
                    Log.i(javaClass.name, "Route for package: $app")
                    builder.addAllowedApplication(app)
                }
        }

        return builder
            .addAddress("10.10.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
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
}
