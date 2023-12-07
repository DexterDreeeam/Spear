package org.p9.spear.component

import android.net.VpnService
import android.util.Log
import org.p9.spear.entity.Packet
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

fun getSocketAddressByEndpoint(endpoint: String): SocketAddress {
    val ipPort = endpoint.split(":")
    val ipAddress = InetAddress.getByName(ipPort[0])
    val port = Integer.parseInt(ipPort[1])
    return InetSocketAddress(ipAddress, port)
}

abstract class IPort (protected val vpnService: VpnService) {

    protected var connected: Boolean? = false

    abstract fun connect(): Boolean

    abstract fun stop(): Boolean

    abstract fun send(packet: Packet): Boolean

    var onReceive: ((Packet, Int) -> Boolean)? = null
}

class TestPort(vpn: VpnService, private val endpoint: String) : IPort(vpn) {

    private var tunnel: DatagramChannel? = null
    private var thread: Thread? = null

    override fun connect(): Boolean {
        if (connected != false) {
            return false
        }
        connected = null
        try {
            val t = DatagramChannel.open()
            if (!vpnService.protect(t.socket())) {
                throw IllegalStateException("Cannot protect the tunnel");
            }
            t.connect(getSocketAddressByEndpoint(endpoint))
            t.configureBlocking(true)
            tunnel = t
            thread = Thread {
                loop()
            }
            thread?.start()
        }
        catch (ex: Exception) {
            Log.e(javaClass.name, "connect exception", ex)
            connected = false
            return false
        }

        connected = true
        return true
    }

    override fun stop(): Boolean {
        if (connected != true) {
            return false
        }
        connected = null
        thread?.interrupt()
        thread = null
        tunnel = null
        connected = false
        return true
    }

    override fun send(packet: Packet): Boolean {
        if (connected != true || tunnel == null) {
            return false
        }
        tunnel?.write(packet.buffer)
        return true
    }

    private fun loop() {
        while(connected == true && tunnel != null) {
            val packet = Packet(ByteBuffer.allocate(UShort.MAX_VALUE.toInt()))
            val len = tunnel?.read(packet.buffer) ?: 0
            if (len > 0) {
                if (onReceive?.invoke(packet, len) == false) {
                    break
                }
            }
        }
    }

}
