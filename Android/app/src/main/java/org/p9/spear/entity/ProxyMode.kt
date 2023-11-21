package org.p9.spear.entity

enum class ProxyMode {
    Global,
    Package;

    companion object {
        fun fromString(modeStr: String): ProxyMode {
            return when (modeStr) {
                Global.toString() -> Global
                Package.toString() -> Package
                else -> Global
            }
        }
    }
}
