package org.p9.spear.constant

const val VPN_ACTION_DONE = 21092
const val VPN_ACTION_FAIL = VPN_ACTION_DONE + 1

const val VPN_PROFILE_REQUEST = VPN_ACTION_FAIL + 1
const val VPN_START_REQUEST = VPN_PROFILE_REQUEST + 1
const val VPN_END_REQUEST = VPN_START_REQUEST + 1

const val VPN_START_ACTION = "org.p9.spear.vpn.start"
const val VPN_END_ACTION = "org.p9.spear.vpn.end"
