package com.justemir84.justremote.remote

import android.util.Log
import com.justemir84.justremote.pairing.PairingManager

class KeySender(private val pairingManager: PairingManager) {

    private val TAG = "KeySender"

    private fun socket() = pairingManager.let { null } // placeholder access

    fun sendKey(key: RemoteKey) {
        if (!pairingManager.isConnected()) {
            Log.w(TAG, "Not connected")
            return
        }
        Log.d(TAG, "Sending key: ${key.name} (${key.code})")
        // TODO: Gerçek protokol framing ile gönder.
        // Örnek: ProtocolFramer.writeFrame(out, byteArrayOf(key.code.toByte()))
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
        Log.d(TAG, "Mouse move: dx=$dx dy=$dy")
        // TODO: hareket paketi gönder
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
}