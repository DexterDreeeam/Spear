package org.p9.spear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ApplicationInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import org.p9.spear.component.ConnectionKeeper
import org.p9.spear.component.Gateway
import org.p9.spear.component.IGateway
import org.p9.spear.constant.VPN_END_ACTION
import org.p9.spear.constant.VPN_START_ACTION
import org.p9.spear.constant.VPN_STATUS_ACTION
import org.p9.spear.constant.VPN_TOGGLE_ACTION
import org.p9.spear.entity.ConnectStatus
import org.p9.spear.entity.ProxyMode


class SpearVpn : VpnService() {

    private var debugLogger: DebugLogger? = null

    companion object {
        const val spearChannelId = "P9SpearChannel"
        const val spearChannelTitle = "P9SpearChannel"
    }

    private val configureIntent: PendingIntent by lazy {
        val activityFlag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT or FLAG_ACTIVITY_NEW_TASK
        val intent = Intent(this, SpearVpn::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this, 0, intent, activityFlag)
        } else {
            PendingIntent.getService(
                this, 0, intent, activityFlag)
        }
    }

    private lateinit var configureManager: ConfigureManager
    private lateinit var appWidgetManager: AppWidgetManager
    private var connectStatus: ConnectStatus = ConnectStatus.Disconnect

    private var connectionKeeper: ConnectionKeeper? = null
    private var vpnInterface: ParcelFileDescriptor? = null
    private var ipAddr: String? = null
    private var port: String? = null
    private var proxyMode: ProxyMode? = null
    private var appsList: List<String>? = null
    private var gateway: IGateway? = null

    override fun onCreate() {
        super.onCreate()
        initDebugLogger()
        responseToStartService()
        appWidgetManager = AppWidgetManager.getInstance(this)
    }

    override fun onRevoke() {
        debugLogger?.append("${javaClass.name} onRevoke")

        disconnect()
        super.onRevoke()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debugLogger?.append("${javaClass.name} onStartCommand")

        initDebugLogger()
        responseToStartService()
        return when (intent?.action) {
            VPN_STATUS_ACTION -> {
                notifyStatus()
                if (connectStatus == ConnectStatus.Disconnect) {
                    START_NOT_STICKY
                } else {
                    START_STICKY
                }
            }
            VPN_START_ACTION -> {
                debugLogger?.reset()
                onStartFlow()
            }
            VPN_END_ACTION -> {
                onEndFlow()
            }
            VPN_TOGGLE_ACTION -> {
                if (connectStatus == ConnectStatus.Disconnect) {
                    onStartFlow()
                } else {
                    onEndFlow()
                }
            }
            else -> {
                START_NOT_STICKY
            }
        }
    }

    override fun onDestroy() {
        debugLogger?.append("${javaClass.name} onDestroy")

        disconnect()
        super.onDestroy()
    }

    private fun loadConfigs(): Boolean {
        configureManager = ConfigureManager(this)

        val token = configureManager.getConnectToken() ?: ":"
        val parts = token.split(":")
        if (parts.size < 2 || parts[0].length < 8 || parts[1].isEmpty()) {
            return false
        }

        ipAddr = parts[0]
        port = parts[1]
        proxyMode = ProxyMode.fromString(configureManager.getMode() ?: "")
        appsList = listOf()

        if (proxyMode == ProxyMode.Package) {
            appsList = configureManager.getPackageChecklist()
        }

        return true
    }

    private fun onStartFlow(): Int {
        changeStatus(ConnectStatus.Disconnect, ConnectStatus.Connecting)
        if (prepare(this) != null || !preConnect()) {
            changeStatus(ConnectStatus.Disconnect)
            // requestVpnGrant() // ask grant vpn
        }
        return START_STICKY
    }

    private fun onEndFlow(): Int {
        disconnect()
        changeStatus(ConnectStatus.Disconnect)
        return START_NOT_STICKY
    }

    private fun changeStatus(fromStatus: ConnectStatus, toStatus: ConnectStatus): Boolean {
        return if (connectStatus == fromStatus) {
            connectStatus = toStatus
            notifyStatus()
            true
        } else {
            false
        }
    }

    private fun changeStatus(toStatus: ConnectStatus): Boolean {
        return if (connectStatus != toStatus) {
            connectStatus = toStatus
            notifyStatus()
            true
        } else {
            false
        }
    }

    private fun preConnect(): Boolean {
        if (!loadConfigs()) {
            return false
        }

        val ept: String
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
            changeStatus(ConnectStatus.Disconnect)
            return
        }

        val eptPort: String = connectionKeeper?.vpnTransportPort() ?: ""
        if (eptPort == "") {
            changeStatus(ConnectStatus.Disconnect)
            return
        }

        vpnInterface = createVpnInterface()
        if (vpnInterface == null) {
            changeStatus(ConnectStatus.Disconnect)
            return
        }

        if (vpnInterface?.fileDescriptor == null) {
            changeStatus(ConnectStatus.Disconnect)
            return
        }

        val fd = vpnInterface?.fileDescriptor!!
        gateway = Gateway(this, "$ipAddr:$eptPort")
        gateway?.start(fd)

        changeStatus(ConnectStatus.Connecting, ConnectStatus.Connected)
    }

    private fun disconnect() {
        changeStatus(ConnectStatus.Connected, ConnectStatus.Disconnecting)

        gateway?.stop()
        vpnInterface?.close()
        connectionKeeper?.stop()

        gateway = null
        vpnInterface = null
        connectionKeeper = null
        changeStatus(ConnectStatus.Disconnect)
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

    private fun requestVpnGrant() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun notifyStatus() {
        val intent = Intent()
        intent.action = VPN_STATUS_ACTION
        intent.putExtra("result", connectStatus.name)
        sendBroadcast(intent)

        appWidgetManager
            .getAppWidgetIds(ComponentName(this, ToggleWidget::class.java))
            .forEach {
                val views = RemoteViews(
                    packageName,
                    R.layout.toggle_widget
                )
                views.setInt(
                    R.id.toggle_widget_button,
                    "setImageResource",
                    if (connectStatus == ConnectStatus.Connected) {
                        R.drawable.active_widget_icon
                    } else  {
                        R.drawable.normal_widget_icon
                    })
                appWidgetManager.updateAppWidget(it, views)
            }
    }

    private fun responseToStartService() {
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                spearChannelId,
                spearChannelTitle,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, spearChannelId)
            .setContentTitle("")
            .setContentText("").build()

        startForeground(1, notification)
    }

    private fun initDebugLogger() {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            debugLogger = DebugLogger(this)
        }
    }
}
