package org.p9.spear

import android.content.Context.MODE_MULTI_PROCESS
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.pm.PackageManager
import org.p9.spear.entity.Package

class ConfigureManager(context: ContextWrapper) {
    private val sharedPreferences = context.getSharedPreferences(
        "SpearSharedPreferences", MODE_PRIVATE or MODE_MULTI_PROCESS)

    fun getToken(): String? {
        return get("proxy_token")
    }

    fun setToken(t: String) {
        set("proxy_token", t)
    }

    fun getConnectToken(): String? {
        return get("connect_proxy_token")
    }

    fun setConnectToken(t: String) {
        set("connect_proxy_token", t)
    }

    fun getMode(): String? {
        return get("proxy_mode")
    }

    fun setMode(m: String) {
        set("proxy_mode", m)
    }

    fun getPackages(context: ContextWrapper): List<Package> {
        val activePackages = mutableListOf<Package>()
        val inactivePackages = mutableListOf<Package>()
        val checklist = getPackageChecklist()
        val allPackages = loadAllPackages(context)
        for (p in allPackages) {
            if (checklist.contains(p.name)) {
                activePackages.add(p)
            } else {
                inactivePackages.add(p)
            }
        }
        return activePackages + inactivePackages
    }

    fun getPackageChecklist(): List<String> {
        val checklistStr = get("package_checklist") ?: ""
        if (checklistStr == "") {
            return listOf()
        }
        return checklistStr.split(",")
    }

    fun setPackageChecklist(name: String, checked: Boolean) {
        val checklist = getPackageChecklist()
        val contains = checklist.contains(name)
        if (checked == contains) {
            return
        }

        val newChecklist = checklist.toMutableList()
        if (checked) {
            newChecklist.add(name)
        } else {
            newChecklist.remove(name)
        }
        setPackageChecklist(newChecklist)
    }

    private fun setPackageChecklist(checklist: List<String>) {
        set("package_checklist", checklist.joinToString(","))
    }

    private fun loadAllPackages(context: ContextWrapper): List<Package> {
        val pm = context.packageManager
        val allPackages = mutableListOf<Package>()
        for (packageInfo in pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
            val name = packageInfo.packageName
            if (name == "org.p9.spear") {
                continue
            }
            pm.getLaunchIntentForPackage(name)?.component?.className ?: continue
            val icon = pm.getApplicationIcon(name)
            allPackages.add(Package(name, icon))
        }
        return allPackages
    }

    private fun set(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
        editor.commit()
    }

    private fun get(key: String): String? {
        val v = sharedPreferences.getString(key, "")
        if (v == "") {
            return null
        }
        return v
    }
}