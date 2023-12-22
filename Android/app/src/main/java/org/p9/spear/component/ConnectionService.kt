package org.p9.spear.component

import android.net.VpnService
import android.util.Log
import java.io.FileDescriptor

class ConnectionService(
    private val vpn: VpnService,
    private val fd: FileDescriptor,
    private val endpoint: String) {

    private var gateway: IGateway? = null

    fun initialize(): Boolean {
        return try {
            gateway = Gateway(vpn, endpoint)
            gateway?.start(fd)
            true
        } catch (ex: Exception) {
            Log.e(javaClass.name, "ex: $ex")
            false
        }
    }

    fun close() {
        gateway?.stop()
        gateway = null
    }
}