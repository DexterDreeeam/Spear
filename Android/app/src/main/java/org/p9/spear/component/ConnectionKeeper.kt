package org.p9.spear.component

import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket

class ConnectionKeeper {

    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private lateinit var thread: Thread
    private var running = false

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

        Thread {
            try {
                socket = createSocketByEndpoint(endpoint)
                socket?.soTimeout = 5000 // set time-out for shake hand
                if (!shakeHand(auth)) {
                    onContinue(false)
                }

                socket?.soTimeout = 0
                input = socket?.getInputStream()
                output = socket?.getOutputStream()
                running = true
                thread = Thread {
                    running = true
                    loop(onBroken)
                }
                thread.start()
                onContinue(true)
            } catch (ex: ConnectException) {
                // connection failed
                onContinue(false)
            }
        }.start()
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

            input?.close()
            output?.close()
            socket?.close()
            thread?.join()

            input = null
            output = null
            socket = null
        }
    }

    private fun shakeHand(auth: String): Boolean {
        return try {
            socket?.getOutputStream()?.write(auth.toByteArray())
            val data = ByteArray(65535)
            val rLen = socket?.getInputStream()?.read(data) ?: 0
            val j = JSONObject(String(data, 0, rLen))
            Log.i(javaClass.name, j.toString())

            vpnTunAddr = j.getString("VpnTunAddr")
            vpnDns = j.getString("VpnDns")
            transportPort = j.getString("TransportPort")
            encryption = j.optString("Encryption")
            encryptionToken = j.optString("EncryptionToken")
            true
        } catch (ex: Exception) {
            Log.e(javaClass.name, ex.toString())
            false
        }
    }

    private fun loop(onBroken: () -> Unit) {
        try {
            while (running) {
                if (!replyCommand()) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "loop() Exception: $e")
        } finally {
            onBroken()
        }
    }

    private fun replyCommand(): Boolean {
        try {
            val data = ByteArray(65535)
            val rLen = input?.read(data) ?: 0
            if (rLen < 0) {
                // error
                return false
            } else if (rLen == 0) {
                // empty
            } else {
                // try reply
            }
            return true
        } catch (ex: Exception) {
            // error
            return false
        }
    }

    private fun createSocketByEndpoint(endpoint: String): Socket {
        val ipPort = endpoint.split(":")
        val ipAddress = InetAddress.getByName(ipPort[0])
        val port = Integer.parseInt(ipPort[1])
        return Socket(ipAddress, port)
    }
}
