package org.p9.spear.component

import android.net.VpnService
import android.util.Log
import org.p9.spear.entity.Packet
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.StandardSocketOptions
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

    var onReceive: ((Packet) -> Boolean)? = null
}

class TestPort(vpn: VpnService, private val endpoint: String) : IPort(vpn) {

    private var tunnel: DatagramChannel? = null
    private var thread: Thread? = null
    private val packet = Packet(
        ByteBuffer.allocate(UShort.MAX_VALUE.toInt()))

    override fun connect(): Boolean {
        if (connected != false) {
            return false
        }
        connected = null
        try {
            val t = DatagramChannel.open()
            if (!vpnService.protect(t.socket())) {
                throw IllegalStateException("Cannot protect the tunnel")
            }
            t.configureBlocking(true)
            t.setOption(StandardSocketOptions.SO_SNDBUF, 16 * 1024 * 1024)
            t.setOption(StandardSocketOptions.SO_RCVBUF, 16 * 1024 * 1024)
            t.connect(getSocketAddressByEndpoint(endpoint))
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
            packet.len = tunnel?.read(packet.buffer) ?: 0
            if (packet.len > 0) {
                if (onReceive?.invoke(packet) == false) {
                    break
                }
                packet.buffer.clear()
            } else {
                Log.e(javaClass.name, "loop but no data!!!!")
            }
        }
    }
}
