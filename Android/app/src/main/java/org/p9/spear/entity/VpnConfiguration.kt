package org.p9.spear.entity

data class VpnConfiguration(
    val address: String,
    val protocol: String,
    val proxyMode: ProxyMode,
    val appsList: List<String>,
)
