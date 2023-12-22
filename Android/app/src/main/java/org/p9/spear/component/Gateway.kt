package org.p9.spear.component

import android.net.VpnService
import android.util.Log
import org.p9.spear.entity.Packet
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer

abstract class IGateway : Runnable {

    private var running: Boolean? = false
    var subGateways = mutableListOf<IGateway>()
    protected lateinit var fd: FileDescriptor
    private lateinit var thread: Thread

    fun start(fd: FileDescriptor) {
        if (this::thread.isInitialized && thread.isAlive) {
            throw IllegalStateException("Running already")
        }

        this.fd = fd
        running = null
        setup()
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

    protected open fun setup() {}

    protected open fun clean() {}

    protected open fun firstIterate(): Boolean {
        return true
    }

    abstract fun iterate(): Boolean

    final override fun run() {
        running = true
        if (firstIterate()) {
            while (!this.thread.isInterrupted) {
                if (!iterate()) {
                    break
                }
            }
        }
        clean()
        running = false
    }
}

class Gateway(vpn: VpnService, endpoint: String) : IGateway() {

    private val port: IPort = TestPort(vpn, endpoint)

    override fun setup() {
        subGateways.clear()
        subGateways.add(GatewaySend(port))
        subGateways.add(GatewayReceive(port))
    }

    override fun firstIterate(): Boolean {
        port.connect()
        return true
    }

    override fun iterate(): Boolean {
        return false
    }
}

class GatewaySend(private val port: IPort) : IGateway() {

    private lateinit var stream: FileInputStream
    private val packet = Packet(
        ByteBuffer.allocate(UShort.MAX_VALUE.toInt()))

    override fun setup() {
        stream = FileInputStream(fd)
    }

    override fun iterate(): Boolean {
        return try {
            val len = stream.read(packet.buffer.array())
            if (len <= 0) {
                Log.e(javaClass.name, "loop but no data!!!!")
                true
            } else if (packet.buffer.get(0).toInt() == 0) {
                // Control Message, ignore
                true
            } else {
                // Log.i(javaClass.name, "+++ $len")
                packet.buffer.limit(len)
                port.send(packet)
                packet.buffer.clear()
                true
            }
        } catch (ex: IOException) {
            // there will be lots of IOException in Huawei and Xiaomi
            // Log.e(javaClass.name, "iterate() IOException: $ex")
            true
        } catch (ex: Exception) {
            // Unknown Exception
            Log.e(javaClass.name, "iterate() Exception: $ex")
            false
        }
    }
}

class GatewayReceive(port: IPort) : IGateway() {

    private lateinit var stream: FileOutputStream

    init {
        port.onReceive = { packet ->
            // Log.i(javaClass.name, "--- ${packet.len}")
            try {
                stream.write(packet.buffer.array(), 0, packet.len)
                true
            } catch (ex: IOException) {
                // Log.e(javaClass.name, "Exception $ex when port.onReceive.")
                false
            }
        }
    }

    override fun setup() {
        stream = FileOutputStream(fd)
    }

    override fun iterate(): Boolean {
        return false
    }
}
