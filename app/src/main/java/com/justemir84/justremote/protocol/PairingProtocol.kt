package com.justemir84.justremote.protocol

import java.nio.ByteBuffer

/**
 * Android TV Remote pairing protokolü — binary framing iskeleti.
 * Referans: https://github-wiki-see.page/m/Aymkdn/assistant-freebox-cloud/wiki/Google-TV-(aka-Android-TV)-Remote-Control-(v2)
 *
 * NOT: Bu sınıf, protokolün tam implementasyonunu içermez.
 * Gerçek implementasyon için resmi protobuf .proto dosyası ve tam handshake gerekir.
 */
object PairingProtocol {

    const val PROTOCOL_VERSION = 2
    const val STATUS_OK = 200

    // Message tags
    const val MSG_PAIRING_REQUEST = 82
    const val MSG_OPTIONS = 162
    const val MSG_CONFIGURATION = 242
    const val MSG_SECRET = 40

    /**
     * PairingRequest mesajı oluşturur (iskelet).
     * Format: [size] [8,2] [16,200,1] [tag] [length] [10, size, bytes...] [18, size, bytes...]
     */
    fun buildPairingRequest(serviceName: String, clientName: String): ByteArray {
        val serviceBytes = serviceName.toByteArray(Charsets.UTF_8)
        val clientBytes = clientName.toByteArray(Charsets.UTF_8)

        // Payload oluştur
        val payload = mutableListOf<Byte>()
        // protocol version
        payload.add(8); payload.add(PROTOCOL_VERSION.toByte())
        // status
        payload.add(16); payload.add(STATUS_OK.toByte()); payload.add(1)
        // message tag
        payload.add(MSG_PAIRING_REQUEST.toByte())
        // length (placeholder)
        payload.add(43.toByte())
        // service name tag
        payload.add(10)
        payload.add(serviceBytes.size.toByte())
        serviceBytes.forEach { payload.add(it) }
        // client name tag
        payload.add(18)
        payload.add(clientBytes.size.toByte())
        clientBytes.forEach { payload.add(it) }

        val payloadArr = payload.toByteArray()
        // Size header (1 byte for small messages)
        return byteArrayOf(payloadArr.size.toByte()) + payloadArr
    }

    /**
     * Key event mesajı oluşturur (iskelet).
     * Format: [5 byte header] [8 byte sequence] [4 byte action] [4 byte keycode]
     */
    fun buildKeyEvent(sequence: Long, action: Int, keycode: Int): ByteArray {
        val buf = ByteBuffer.allocate(5 + 8 + 4 + 4)
        // Header (placeholder)
        buf.put(0x08); buf.put(0x02); buf.put(0x10); buf.put(0xc8.toByte()); buf.put(0x01)
        buf.putLong(sequence)
        buf.putInt(action)
        buf.putInt(keycode)
        return buf.array()
    }
}