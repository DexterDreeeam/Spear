package org.p9.spear

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.p9.spear.entity.Package

enum class ConnectStatus {
    Loading,
    Disconnect,
    Connecting,
    Connected,
    Disconnecting,
}

enum class ProxyMode {
    Global,
    Package;

    companion object {
        fun fromString(modeStr: String): ProxyMode {
            return when (modeStr) {
                Global.toString() -> Global
                Package.toString() -> Package
                else -> Global
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    private val TAG = "SPEAR_MAIN_ACTIVITY"

    private lateinit var centerAddress: EditText
    private lateinit var proxyToken: EditText
    private lateinit var actionButton: Button
    private lateinit var modeButton: Button
    private lateinit var packages: RecyclerView

    private lateinit var sharedPreferences: SharedPreferences
    private var connectStatus: ConnectStatus = ConnectStatus.Disconnect
    private var proxyMode: ProxyMode = ProxyMode.Global

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("SpearSharedPreferences", MODE_PRIVATE)

        setContentView(R.layout.activity_main)
        centerAddress = findViewById(R.id.center_address_editor)
        proxyToken = findViewById(R.id.proxy_token_editor)
        actionButton = findViewById(R.id.action_button)
        modeButton = findViewById(R.id.mode_button)
        packages = findViewById(R.id.package_list)
        packages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        updateStatus(ConnectStatus.Loading)

        centerAddress.setText(getStorage("center_address", getString(R.string.center_address_hint)))
        centerAddress.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                setStorage("center_address", s.toString())
            }
        })

        proxyToken.setText(getStorage("proxy_token", ""))
        proxyToken.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                setStorage("proxy_token", s.toString())
            }
        })

        actionButton.setOnClickListener {
            when (connectStatus) {
                ConnectStatus.Disconnect -> {
                    updateStatus(ConnectStatus.Connected)
                }
                ConnectStatus.Connected -> {
                    updateStatus(ConnectStatus.Disconnect)
                }
                else -> {}
            }
        }

        modeButton.setOnClickListener {
            updateMode(nextMode())
        }

        var modeStr = getStorage("proxy_mode", ProxyMode.Global.toString())
        updateMode(ProxyMode.fromString(modeStr))
        updateStatus(ConnectStatus.Disconnect)
    }

    private fun updateStatus(status: ConnectStatus) {
        var buttonStr: String
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
            }
            ConnectStatus.Connecting -> {
                buttonStr = "Connecting"
                buttonClickable = false
            }
            ConnectStatus.Connected -> {
                buttonStr = "Connected"
                buttonColor = Color.GREEN
            }
            ConnectStatus.Disconnecting -> {
                buttonStr = "Disconnecting"
                buttonClickable = false
            }
        }
        actionButton.text = buttonStr
        actionButton.isClickable = buttonClickable
        actionButton.setBackgroundColor(buttonColor)
        connectStatus = status
    }

    private fun nextMode(): ProxyMode {
        return when (proxyMode) {
            ProxyMode.Global -> ProxyMode.Package
            ProxyMode.Package -> ProxyMode.Global
        }
    }

    private fun updateMode(mode: ProxyMode) {
        var modeButtonStr: String
        when (mode) {
            ProxyMode.Global -> {
                packages.visibility = View.GONE
                modeButtonStr = "Global"
            }
            ProxyMode.Package -> {
                loadPackages()
                packages.visibility = View.VISIBLE
                modeButtonStr = "Apps"
            }
        }
        modeButton.text = modeButtonStr
        proxyMode = mode
        setStorage("proxy_mode", mode.toString())
    }
    
    private fun loadPackages() {
        val checklistSet = mutableSetOf<String>()
        val checklist = getStorage("package_checklist", "")
        for (packageName in checklist.split(",")) {
            checklistSet.add(packageName)
        }
        val pm = packageManager
        val activePackages = mutableListOf<Package>()
        val inactivePackages = mutableListOf<Package>()
        for (packageInfo in pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
            val name = packageInfo.packageName
            val icon = pm.getApplicationIcon(packageName)
            if (checklistSet.contains(name)) {
                activePackages.add(Package(name, icon))
            } else {
                inactivePackages.add(Package(name, icon))
            }
        }

        val packageListAdapter = PackageListAdapter(checklist) {
            name, checked -> updatePackageChecklist(name, checked)
        }
        packages.adapter = packageListAdapter
        packageListAdapter.setPackageList(activePackages + inactivePackages)
    }

    private fun updatePackageChecklist(name: String, checked: Boolean) {
        val checklist = getStorage("package_checklist", "")
        val packageSet = mutableSetOf<String>()
        for (packageName in checklist.split(",")) {
            packageSet.add(packageName)
        }
        if (checked) {
            packageSet.add(name)
        } else {
            packageSet.remove(name)
        }
        val newChecklist = packageSet.joinToString(",")
        setStorage("package_checklist", newChecklist)
    }

    private fun setStorage(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun getStorage(key: String, default: String): String {
        return sharedPreferences.getString(key, null) ?: default
    }
}
