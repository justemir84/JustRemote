package com.justemir84.justremote.remote

import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Basit length-prefixed framing (iskelet).
 * Gerçek Android TV Remote protokolü farklı bir binary format kullanır;
 * bu sınıf referans/prototip amaçlıdır.
 */
object ProtocolFramer {
    fun writeFrame(out: OutputStream, payload: ByteArray) {
        val len = payload.size
        val header = ByteBuffer.allocate(4).putInt(len).array()
        out.write(header)
        out.write(payload)
        out.flush()
    }

    fun readFrame(inp: java.io.InputStream): ByteArray? {
        val header = ByteArray(4)
        if (inp.read(header) != 4) return null
        val len = ByteBuffer.wrap(header).int
        if (len <= 0 || len > 65536) return null
        val payload = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = inp.read(payload, read, len - read)
            if (n < 0) return null
            read += n
        }
        return payload
    }
}