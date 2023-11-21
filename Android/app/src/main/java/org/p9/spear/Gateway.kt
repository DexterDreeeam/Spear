package org.p9.spear

import android.content.pm.PackageManager
import android.net.ProxyInfo
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.Log
import org.p9.spear.entity.VpnConfiguration
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.concurrent.TimeUnit

class Gateway(
    private val vpnService: VpnService, private val configuration: VpnConfiguration) {

    var running: Boolean = false

    fun start(fd: ParcelFileDescriptor): Boolean {
        running = true
        var connected = false
        try {
            DatagramChannel.open().use { tunnel ->
                tunnel.connect(server)
                tunnel.configureBlocking(false)
                iface = handshake(tunnel)
                connected = true
                val `in` = FileInputStream(iface!!.fileDescriptor)
                val out = FileOutputStream(iface!!.fileDescriptor)
                val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)
                var lastSendTime = System.currentTimeMillis()
                var lastReceiveTime = System.currentTimeMillis()
                while (true) {
                    var idle = true
                    var length = `in`.read(packet.array())
                    if (length > 0) {
                        packet.limit(length)
                        tunnel.write(packet)
                        packet.clear()
                        idle = false
                        lastReceiveTime = System.currentTimeMillis()
                    }
                    length = tunnel.read(packet)
                    if (length > 0) {
                        if (packet[0].toInt() != 0) {
                            out.write(packet.array(), 0, length)
                        }
                        packet.clear()
                        idle = false
                        lastSendTime = System.currentTimeMillis()
                    }
                    if (idle) {
                        Thread.sleep(IDLE_INTERVAL_MS)
                        val timeNow = System.currentTimeMillis()
                        if (lastSendTime + KEEPALIVE_INTERVAL_MS <= timeNow) {
                            packet.put(0.toByte()).limit(1)
                            for (i in 0..2) {
                                packet.position(0)
                                tunnel.write(packet)
                            }
                            packet.clear()
                            lastSendTime = timeNow
                        } else check(lastReceiveTime + RECEIVE_TIMEOUT_MS > timeNow) {
                            "Timed out"
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            Log.e(tag, "Cannot use socket", e)
        } finally {
            if (iface != null) {
                try {
                    iface!!.close()
                } catch (e: IOException) {
                    Log.e(tag, "Unable to close interface", e)
                }
            }
        }
        running = false
    }

    private fun handshake(tunnel: DatagramChannel): ParcelFileDescriptor? {
        val packet = ByteBuffer.allocate(1024)
        packet.put(0.toByte()).put(mSharedSecret).flip()
        for (i in 0..2) {
            packet.position(0)
            tunnel.write(packet)
        }
        packet.clear()
        for (i in 0 until MAX_HANDSHAKE_ATTEMPTS) {
            Thread.sleep(IDLE_INTERVAL_MS)
            val length = tunnel.read(packet)
            if (length > 0 && packet[0].toInt() == 0) {
                return configure(
                    kotlin.String(packet.array(), 1, length - 1, US_ASCII).trim { it <= ' ' })
            }
        }
        throw IOException("Timed out")
    }

    @Throws(IllegalArgumentException::class)
    private fun configure(parameters: String): ParcelFileDescriptor? {
        // Configure a builder while parsing the parameters.
        val builder = mService.Builder()
        for (parameter in parameters.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()) {
            val fields = parameter.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            try {
                when (fields[0][0]) {
                    'm' -> builder.setMtu(fields[1].toShort().toInt())
                    'a' -> builder.addAddress(fields[1], fields[2].toInt())
                    'r' -> builder.addRoute(fields[1], fields[2].toInt())
                    'd' -> builder.addDnsServer(fields[1])
                    's' -> builder.addSearchDomain(fields[1])
                }
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Bad parameter: $parameter")
            }
        }
        // Create a new interface using the builder and save the parameters.
        val vpnInterface: ParcelFileDescriptor?
        for (packageName in mPackages) {
            try {
                if (mAllow) {
                    builder.addAllowedApplication(packageName)
                } else {
                    builder.addDisallowedApplication(packageName)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(tag, "Package not available: $packageName", e)
            }
        }
        builder.setSession(mServerName).setConfigureIntent(mConfigureIntent!!)
        if (!TextUtils.isEmpty(mProxyHostName)) {
            builder.setHttpProxy(ProxyInfo.buildDirectProxy(mProxyHostName, mProxyHostPort))
        }
        synchronized(mService) {
            vpnInterface = builder.establish()
            if (mOnEstablishListener != null) {
                mOnEstablishListener!!.onEstablish(vpnInterface)
            }
        }
        Log.i(tag, "New interface: $vpnInterface ($parameters)")
        return vpnInterface
    }

    private val tag: String
        private get() = Gateway::class.java.simpleName + "[" + mConnectionId + "]"

    companion object {
        private const val MAX_PACKET_SIZE = Short.MAX_VALUE.toInt()

        private val RECONNECT_WAIT_MS: Long = TimeUnit.SECONDS.toMillis(3)

        private val KEEPALIVE_INTERVAL_MS: Long = TimeUnit.SECONDS.toMillis(15)

        private val RECEIVE_TIMEOUT_MS: Long = TimeUnit.SECONDS.toMillis(20)

        private val IDLE_INTERVAL_MS: Long = TimeUnit.MILLISECONDS.toMillis(100)

        private const val MAX_HANDSHAKE_ATTEMPTS = 50
    }
}
