package org.p9.spear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver(notify: (String, Boolean) -> Unit) : BroadcastReceiver() {

    private val onNotify = notify
    override fun onReceive(context: Context?, intent: Intent?) {
        onNotify(intent?.action ?: "", intent?.getBooleanExtra("result", false) ?: false)
    }
}
