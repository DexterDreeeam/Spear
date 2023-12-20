package org.p9.spear

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.p9.spear.constant.VPN_END_ACTION
import org.p9.spear.constant.VPN_GRANT_ACTION
import org.p9.spear.constant.VPN_START_ACTION
import org.p9.spear.constant.VPN_STATUS_ACTION
import org.p9.spear.entity.ConnectStatus
import org.p9.spear.entity.ProxyMode

class MainActivity : AppCompatActivity() {

    private lateinit var appVersion: TextView
    private lateinit var appWebsite: TextView
    private lateinit var proxyToken: EditText
    private lateinit var actionButton: Button
    private lateinit var modeButton: Button
    private lateinit var packages: RecyclerView

    private lateinit var notificationReceiver: BroadcastReceiver
    private lateinit var configureManager: ConfigureManager

    private var proxyTokenStr: String = ""
    private var connectStatus: ConnectStatus = ConnectStatus.Loading
    private var proxyMode: ProxyMode = ProxyMode.Global

    private val grantVpn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationReceiver = NotificationReceiver {
            act, result -> receiveResult(act, result)
        }
        configureManager = ConfigureManager(this)

        setContentView(R.layout.activity_main)
        appVersion = findViewById(R.id.app_version)
        appWebsite = findViewById(R.id.app_website)
        proxyToken = findViewById(R.id.proxy_token_editor)
        actionButton = findViewById(R.id.action_button)
        modeButton = findViewById(R.id.mode_button)
        packages = findViewById(R.id.package_list)
        packages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        updateStatus(ConnectStatus.Loading)

        val version = packageManager.getPackageInfo(packageName, 0).versionName +
            if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                "d"
            } else {
                "r"
            }
        appVersion.text = version

        appWebsite.setOnClickListener {
            val url = getString(R.string.app_website_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        proxyTokenStr = configureManager.getConnectToken() ?: configureManager.getToken() ?: ""
        proxyToken.setText(proxyTokenStr)
        proxyToken.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                proxyTokenStr = s.toString()
                configureManager.setToken(proxyTokenStr)
            }
        })

        actionButton.setOnClickListener {
            when (connectStatus) {
                ConnectStatus.Disconnect -> {
                    onConnectClicked()
                }
                ConnectStatus.Connected -> {
                    disconnect()
                }
                else -> {}
            }
        }

        modeButton.setOnClickListener {
            updateMode(nextMode())
        }

        val modeStr = configureManager.getMode() ?: ProxyMode.Global.toString()
        updateMode(ProxyMode.fromString(modeStr))
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()

        val filter = IntentFilter()
        filter.addAction(VPN_GRANT_ACTION)
        filter.addAction(VPN_STATUS_ACTION)
        filter.addAction(VPN_START_ACTION)
        filter.addAction(VPN_END_ACTION)
        registerReceiver(notificationReceiver, filter)
        queryVpnServiceStatus()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(notificationReceiver)
    }

    private fun onConnectClicked() {
        configureManager.setConnectToken(proxyTokenStr)
        if (proxyMode == ProxyMode.Package) {
            loadPackages(false)
        }
        modeButton.isEnabled = false
        connect()
    }

    private fun connect() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            grantVpn.launch(prepareIntent)
        } else {
            startVpnService()
        }
    }

    private fun disconnect() {
        endVpnService()
    }

    private fun queryVpnServiceStatus() {
        val it = Intent(this, SpearVpn::class.java)
        it.action = VPN_STATUS_ACTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(it)
        } else {
            startService(it)
        }
    }

    private fun startVpnService() {
        updateStatus(ConnectStatus.Connecting)
        val it = Intent(this, SpearVpn::class.java)
        it.action = VPN_START_ACTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(it)
        } else {
            startService(it)
        }
    }

    private fun endVpnService() {
        updateStatus(ConnectStatus.Disconnecting)
        val it = Intent(this, SpearVpn::class.java)
        it.action = VPN_END_ACTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(it)
        } else {
            startService(it)
        }
    }

    private fun receiveResult(action: String, result: String) {
        when (action) {
            VPN_GRANT_ACTION -> {
                onConnectClicked()
            }
            VPN_STATUS_ACTION -> {
                updateStatus(ConnectStatus.fromString(result))
            }
            else -> {}
        }
    }

    private fun updateStatus(status: ConnectStatus) {
        val buttonStr: String
        var buttonClickable = true
        var buttonColor: Int = Color.GRAY
        when (status) {
            ConnectStatus.Loading -> {
                buttonStr = "Loading"
                buttonClickable = false
            }
            ConnectStatus.Disconnect -> {
                buttonStr = "Connect"
                buttonColor = Color.CYAN
                activateMode()
            }
            ConnectStatus.Connecting -> {
                buttonStr = "Connecting"
                buttonClickable = false
            }
            ConnectStatus.Connected -> {
                buttonStr = "Disconnect"
                buttonColor = Color.GREEN
            }
            ConnectStatus.Disconnecting -> {
                buttonStr = "Disconnecting"
                buttonClickable = false
            }
        }

        actionButton.text = buttonStr
        actionButton.isEnabled = buttonClickable
        actionButton.isClickable = buttonClickable
        actionButton.setBackgroundColor(buttonColor)
        connectStatus = status
    }

    private fun activateMode() {
        if (proxyMode == ProxyMode.Package) {
            loadPackages(true)
        }
        modeButton.isEnabled = true
    }

    private fun nextMode(): ProxyMode {
        return when (proxyMode) {
            ProxyMode.Global -> ProxyMode.Package
            ProxyMode.Package -> ProxyMode.Global
        }
    }

    private fun updateMode(mode: ProxyMode) {
        val modeButtonStr: String
        when (mode) {
            ProxyMode.Global -> {
                packages.visibility = View.GONE
                modeButtonStr = "Global"
            }
            ProxyMode.Package -> {
                loadPackages(true)
                packages.visibility = View.VISIBLE
                modeButtonStr = "Apps"
            }
        }
        modeButton.text = modeButtonStr
        proxyMode = mode
        configureManager.setMode(mode.toString())
    }
    
    private fun loadPackages(active: Boolean) {
        val packageListAdapter = PackageListAdapter(
            configureManager.getPackageChecklist(),
            active) {
                name, checked -> configureManager.setPackageChecklist(name, checked)
        }
        packages.adapter = packageListAdapter
        packageListAdapter.setPackageList(configureManager.getPackages(this))
    }
}
