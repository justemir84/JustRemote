package com.justemir84.justremote.pairing

import android.content.Context
import android.util.Log
import com.justemir84.justremote.storage.SecureStorage
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Android TV Remote protokolü için temel TLS bağlantı yöneticisi.
 * NOT: Bu iskelet, gerçek protokol handshake'ini içermez.
 * Üretimde sertifika pinning ve tam protokol implementasyonu gerekir.
 */
class PairingManager(private val context: Context) {

    private val secureStorage = SecureStorage(context)
    private var socket: SSLSocket? = null
    private val TAG = "PairingManager"

    // Geliştirme için geçici trust manager (ÜRETİMDE KULLANMA)
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    fun connect(host: String, port: Int = 6466, onState: (PairingState) -> Unit) {
        onState(PairingState.Connecting)
        Thread {
            try {
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                val factory = sslContext.socketFactory
                val s = factory.createSocket() as SSLSocket
                s.connect(InetSocketAddress(host, port), 5000)
                s.startHandshake()
                socket = s
                Log.d(TAG, "Connected to $host:$port")
                // Gerçek protokolde burada pairing handshake yapılır.
                onState(PairingState.PairingRequired())
            } catch (e: Exception) {
                Log.e(TAG, "Connect failed", e)
                onState(PairingState.Error(e.message ?: "Bağlantı hatası"))
            }
        }.start()
    }

    fun disconnect() {
        try { socket?.close() } catch (_: IOException) {}
        socket = null
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    /**
     * Pairing handshake (iskelet).
     * Gerçek protokolde burada TV'ye pairing kodu gönderilir ve
     * TV'den dönen sertifika/token alınır.
     */
    fun pair(code: String, onResult: (Boolean, String?) -> Unit) {
        val s = socket ?: run {
            onResult(false, "Bağlantı yok")
            return
        }
        Thread {
            try {
                val out = s.outputStream
                val inp = s.inputStream
                // Örnek: kodu gönder (gerçek protokolde binary framing gerekir)
                out.write(code.toByteArray(Charsets.UTF_8))
                out.flush()
                val buf = ByteArray(256)
                val n = inp.read(buf)
                val resp = String(buf, 0, n, Charsets.UTF_8)
                Log.d(TAG, "Pair response: $resp")
                // TODO: token'ı EncryptedSharedPreferences'a kaydet
                onResult(true, resp)
            } catch (e: Exception) {
                Log.e(TAG, "Pair failed", e)
                onResult(false, e.message)
            }
        }.start()
    }

    /**
     * Token saklama — EncryptedSharedPreferences ile şifreli.
     */
    fun saveToken(token: String) {
        secureStorage.saveToken(token)
        Log.d(TAG, "Token saved (encrypted)")
    }

    fun getToken(): String? = secureStorage.getToken()

    fun hasToken(): Boolean = secureStorage.hasToken()

    fun clearToken() = secureStorage.clearToken()
}