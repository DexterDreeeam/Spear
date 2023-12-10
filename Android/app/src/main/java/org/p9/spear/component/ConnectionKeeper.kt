package org.p9.spear.component

import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket

class ConnectionKeeper {

    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private lateinit var thread: Thread
    private var running = false

    private val helloBytes = "hello".toByteArray()

    private var vpnTunAddr = ""
    private var vpnDns = ""
    private var transportPort = ""

    private var encryption = ""
    private var encryptionToken = ""

    fun initialize(
        endpoint: String,
        auth: String,
        onBroken: () -> Unit,
        onContinue: (Boolean) -> Unit) {
        try {
            Thread {
                socket = createSocketByEndpoint(endpoint)
                socket?.soTimeout = 5000

                input = socket?.getInputStream()
                output = socket?.getOutputStream()
                if (shakeHand(auth)) {
                    running = true
                    thread = Thread {
                        running = true
                        loop(onBroken)
                    }
                    thread?.start()
                    onContinue(true)
                }
                onContinue(false)
            }.start()
        } catch (e: Exception) {
            Log.e(javaClass.name, "Exception when initialize()")
        }
    }

    fun vpnTunAddr(): String {
        return vpnTunAddr
    }

    fun vpnDns(): String {
        return vpnDns
    }

    fun vpnTransportPort(): String {
        return transportPort
    }

    fun stop() {
        if (running) {
            running = false

            thread?.join()
            input?.close()
            output?.close()
            socket?.close()

            input = null
            output = null
            socket = null
        }
    }

    private fun shakeHand(auth: String): Boolean {
        output?.write(auth.toByteArray())
        val data = ByteArray(65535)
        val rLen = input?.read(data) ?: 0
        val j = JSONObject(String(data, 0, rLen))
        Log.i(javaClass.name, j.toString())

        vpnTunAddr = j.getString("VpnTunAddr")
        vpnDns = j.getString("VpnDns")
        transportPort = j.getString("TransportPort")

        encryption = j.optString("Encryption")
        encryptionToken = j.optString("EncryptionToken")
        return true
    }

    private fun loop(onBroken: () -> Unit) {
        try {
            while (running) {
                output?.write(helloBytes)
                Thread.sleep(1000)
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "Exception when loop()")
            onBroken()
        }
    }

    private fun createSocketByEndpoint(endpoint: String): Socket {
        val ipPort = endpoint.split(":")
        val ipAddress = InetAddress.getByName(ipPort[0])
        val port = Integer.parseInt(ipPort[1])
        return Socket(ipAddress, port)
    }
}
