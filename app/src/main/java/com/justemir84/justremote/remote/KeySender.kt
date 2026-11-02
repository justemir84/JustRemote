package com.justemir84.justremote.remote

import android.util.Log
import com.justemir84.justremote.pairing.PairingManager
import com.justemir84.justremote.protocol.PairingProtocol

class KeySender(private val pairingManager: PairingManager) {

    private val TAG = "KeySender"
    private var sequence: Long = 0

    fun sendKey(key: RemoteKey) {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        val msg = PairingProtocol.buildKeyEvent(sequence++, 0, key.code) // 0 = ACTION_DOWN
        pairingManager.sendBytes(msg)
        // ACTION_UP
        val upMsg = PairingProtocol.buildKeyEvent(sequence++, 1, key.code)
        pairingManager.sendBytes(upMsg)
        Log.d(TAG, "Sent key: ${key.name}")
    }

    fun sendText(text: String) {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        Log.d(TAG, "Sending text: $text")
        // TODO: IME injection protokolü
    }

    fun sendMouseMove(dx: Float, dy: Float) {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        // TODO: Gerçek protokolde mouse hareketi paketi gönder.
        // Örnek placeholder: dx, dy'yi byte'a çevir ve gönder.
        val payload = byteArrayOf(
            (dx.toInt() and 0xFF).toByte(),
            (dy.toInt() and 0xFF).toByte()
        )
        pairingManager.sendBytes(payload)
        Log.d(TAG, "Mouse move: dx=$dx dy=$dy")
    }

    fun sendClick() {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        Log.d(TAG, "Mouse click")
        // TODO: click paketi gönder
    }

    fun sendScroll(dy: Float) {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        Log.d(TAG, "Scroll: dy=$dy")
        // TODO: scroll paketi gönder
    }

    fun sendText(text: String) {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        // TODO: IME injection protokolü
        Log.d(TAG, "Sending text: $text")
    }
}