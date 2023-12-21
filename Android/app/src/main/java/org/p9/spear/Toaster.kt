package org.p9.spear

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Toast

class Toaster(private val context: Context) {
    fun notify(msg: String) {
        val duration = Toast.LENGTH_LONG
        Toast.makeText(context, msg, duration).show()
    }

    fun dialog(msg: String) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Spear")
            .setMessage(msg)
            .setIcon(R.mipmap.ic_spear_foreground)
            .setPositiveButton("OK") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }
}