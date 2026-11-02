package com.justemir84.justremote.crypto

import java.security.MessageDigest

/**
 * Android TV Remote pairing code hash hesaplama.
 * Format: ilk 2 hex karakter kontrol, son 4 karakter secret.
 * Hash = SHA-256(client_modulus + client_exponent + server_modulus + server_exponent + last4)
 * İlk byte hash'in ilk 2 hex karakteriyle eşleşmeli.
 */
object SecretGenerator {

    fun computeSecret(
        clientModulusHex: String,
        clientExponentHex: String,
        serverModulusHex: String,
        serverExponentHex: String,
        pairingCode: String
    ): ByteArray {
        val last4 = pairingCode.takeLast(4)
        val input = clientModulusHex +
            clientExponentHex.padStart(2, '0') +
            serverModulusHex +
            serverExponentHex.padStart(2, '0') +
            last4
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray(Charsets.UTF_8))
    }

    fun verifyCheck(pairingCode: String, hash: ByteArray): Boolean {
        val check = pairingCode.take(2)
        val firstByte = hash[0].toInt() and 0xFF
        return firstByte == check.toInt(16)
    }
}