package com.justemir84.justremote.pairing

import android.content.Context
import android.util.Log
import com.justemir84.justremote.crypto.CertificateManager
import com.justemir84.justremote.crypto.SecretGenerator
import com.justemir84.justremote.storage.SecureStorage
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Android TV Remote protokolü için temel TLS bağlantı yöneticisi.
 * NOT: Bu iskelet, gerçek protokol handshake'ini içermez.
 * Üretimde sertifika pinning ve tam protokol implementasyonu gerekir.
 */
class PairingManager(private val context: Context) {

    private val secureStorage = SecureStorage(context)
    private val certManager = CertificateManager(context)
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

    fun sendPairingRequest(serviceName: String = "info.kodono.assistant", clientName: String = "JustRemote") {
        val s = socket ?: return
        Thread {
            try {
                val msg = com.justemir84.justremote.protocol.PairingProtocol.buildPairingRequest(serviceName, clientName)
                s.outputStream.write(msg)
                s.outputStream.flush()
                Log.d(TAG, "Pairing request sent")
            } catch (e: Exception) {
                Log.e(TAG, "Pairing request failed", e)
            }
        }.start()
    }

    fun sendBytes(data: ByteArray) {
        try {
            socket?.outputStream?.write(data)
            socket?.outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Send failed", e)
        }
    }

    fun getServerModulusHex(): String? {
        val cert = socket?.session?.peerCertificates?.firstOrNull() as? X509Certificate
        val pub = cert?.publicKey as? java.security.interfaces.RSAPublicKey
        return pub?.modulus?.toString(16)
    }

    fun getServerExponentHex(): String? {
        val cert = socket?.session?.peerCertificates?.firstOrNull() as? X509Certificate
        val pub = cert?.publicKey as? java.security.interfaces.RSAPublicKey
        return pub?.publicExponent?.toString(16)?.padStart(2, '0')
    }

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
                val serverMod = getServerModulusHex()
                val serverExp = getServerExponentHex()
                if (serverMod == null || serverExp == null) {
                    onResult(false, "Sunucu sertifikası alınamadı")
                    return@Thread
                }
                val clientMod = certManager.getCertificateModulusHex()
                val clientExp = certManager.getCertificateExponentHex()

                // Kontrol: ilk 2 hex karakter hash ile eşleşmeli
                val hash = SecretGenerator.computeSecret(clientMod, clientExp, serverMod, serverExp, code)
                if (!SecretGenerator.verifyCheck(code, hash)) {
                    onResult(false, "Pairing kodu geçersiz (kontrol uyuşmuyor)")
                    return@Thread
                }

                val out = s.outputStream
                // TODO: Gerçek protokolde hash'i binary framing ile gönder.
                // Örnek placeholder: hash'i gönder
                out.write(hash)
                out.flush()

                // Token'ı sakla (gerçek protokolde TV'den dönen token)
                saveToken(code) // placeholder
                onResult(true, "Eşleştirme başarılı")
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