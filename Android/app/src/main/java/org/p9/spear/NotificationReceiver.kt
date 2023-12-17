package org.p9.spear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver(notify: (String, String) -> Unit) : BroadcastReceiver() {

    private val onNotify = notify
    override fun onReceive(context: Context?, intent: Intent?) {
        onNotify(intent?.action ?: "", intent?.getStringExtra("result") ?: "")
    }
}
