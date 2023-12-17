package org.p9.spear

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log
import android.widget.RemoteViews
import org.p9.spear.constant.VPN_STATUS_ACTION
import org.p9.spear.constant.VPN_TOGGLE_ACTION
import org.p9.spear.entity.ConnectStatus


class ToggleWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val intent = Intent(context, SpearVpn::class.java)
            intent.action = VPN_TOGGLE_ACTION
            val pendingIntent: PendingIntent = PendingIntent.getService(
                context,
                R.id.toggle_widget_button,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(
                context.packageName,
                R.layout.toggle_widget
            ).apply {
                setOnClickPendingIntent(R.id.toggle_widget_button, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.e("Check", "Inside On Receiver !!!!!!!!!!!!!!!!!!")
    }
}
