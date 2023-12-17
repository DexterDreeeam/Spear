package org.p9.spear.entity

enum class ConnectStatus {
    Loading,
    Disconnect,
    Connecting,
    Connected,
    Disconnecting;

    companion object {
        fun fromString(s: String): ConnectStatus {
            return when (s) {
                Loading.name -> Loading
                Disconnect.name -> Disconnect
                Connecting.name -> Connecting
                Connected.name -> Connected
                Disconnecting.name -> Disconnecting
                else -> Loading
            }
        }
    }
}
