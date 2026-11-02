package com.justemir84.justremote.remote

import android.util.Log
import com.justemir84.justremote.pairing.PairingManager
import java.io.OutputStream

/**
 * Kumanda tuş gönderimi (iskelet).
 * Gerçek protokolde tuş kodları binary framing ile gönderilir.
 */
class KeySender(private val pairingManager: PairingManager) {

    private val TAG = "KeySender"

    fun sendKey(key: RemoteKey) {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        // TODO: Gerçek protokol framing'i uygula.
        // Örnek placeholder: tuş kodunu gönder.
        Log.d(TAG, "Sending key: ${key.name} (${key.code})")
        // pairingManager.socket?.outputStream?.write(...)
    }

    fun sendText(text: String) {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        Log.d(TAG, "Sending text: $text")
        // TODO: IME injection protokolü
    }
}