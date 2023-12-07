package org.p9.spear.component

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket

class ConnectionKeeper {

    private lateinit var socket: Socket
    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private lateinit var thread: Thread
    private var running = false

    fun initialize(
        endpoint: String,
        auth: String,
        onBroken: () -> Unit): Boolean {
        try {
            val networkSetupThread = Thread {
                socket = createSocketByEndpoint(endpoint)
                input = socket.getInputStream()
                output = socket.getOutputStream()
            }
            networkSetupThread.start()
            networkSetupThread.join()
            if (shakeHand(auth)) {
                running = true
                thread = Thread {
                    running = true
                    loop(onBroken)
                }
                return true
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "Exception when initialize()")
            onBroken()
        }
        return false
    }

    fun vpnTunAddress(): String {
        return "10.10.0.2"
    }

    fun vpnDns(): String {
        return "8.8.8.8"
    }

    fun vpnTransportEndpoint(): String {
        return "20.255.49.236:22334"
    }

    fun stop() {
        if (running) {
            running = false
            thread.join()
        }
    }

    private fun shakeHand(auth: String): Boolean {
        output.write(auth.toByteArray())
        return true
    }

    private fun loop(onBroken: () -> Unit) {
        try {
            while (running) {
                output.write("hello".toByteArray())
                Thread.sleep(100)
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
