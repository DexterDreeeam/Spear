package org.p9.spear.component

import android.net.VpnService
import android.util.Log
import org.p9.spear.entity.Packet
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

abstract class IGateway : Runnable {

    private var running: Boolean? = false
    var subGateways = mutableListOf<IGateway>()
    protected lateinit var fd: FileDescriptor
    private lateinit var thread: Thread
    private var overrideIterate: Boolean = true

    protected open fun setup() {}

    protected open fun clean() {}

    fun start(fd: FileDescriptor) {
        if (this::thread.isInitialized && thread.isAlive) {
            throw IllegalStateException("Running already")
        }

        running = null
        setup()
        this.fd = fd
        thread = Thread(this).apply { start() }
        subGateways.forEach { g -> g.start(fd) }
    }

    fun stop() {
        running = null
        subGateways.forEach { g -> g.stop() }
        subGateways.clear()
        if (this::thread.isInitialized) {
            thread.interrupt()
        }
    }

    override fun run() {
        running = true
        var first = true
        while (!this.thread.isInterrupted && overrideIterate && iterate(first)) {
            first = false
        }
        running = false
    }

    protected open fun iterate(firstTime: Boolean): Boolean {
        overrideIterate = false
        return true
    }
}

class Gateway(vpn: VpnService, endpoint: String) : IGateway() {

    private val port: IPort = TestPort(vpn, endpoint)

    override fun setup() {
        subGateways.clear()
        subGateways.add(GatewaySend(port))
        subGateways.add(GatewayReceive(port))
        port.connect()
    }
}

class GatewaySend(private val port: IPort) : IGateway() {

    private lateinit var stream: FileInputStream
    private val buffer: ByteBuffer = ByteBuffer.allocate(UShort.MAX_VALUE.toInt())

    override fun setup() {
        stream = FileInputStream(fd)
    }

    override fun iterate(firstTime: Boolean): Boolean {
        return try {
            Log.i("Gateway Send", "+++")
            val len = stream.read(buffer.array())
            if (len > 0) {
                Log.i(javaClass.name, "iterate send buffer with len $len")
                buffer.limit(len)
                val packet = Packet(buffer)
                port.send(packet)
                buffer.clear()
            }
            Log.i("Gateway Send", "---")
            true
        } catch (e: Exception) {
            false
        }
    }
}

class GatewayReceive(port: IPort) : IGateway() {

    private lateinit var stream: FileOutputStream
    private val buffer: ByteBuffer = ByteBuffer.allocate(16384)

    init {
        port.onReceive = { packet, len ->
            stream.write(packet.buffer.array(), 0, len)
            true
        }
    }

    override fun setup() {
        stream = FileOutputStream(fd)
    }
}
