package org.p9.spear.entity

import java.nio.ByteBuffer

class Packet(val buffer: ByteBuffer) {
    var len: Int = 0
}
