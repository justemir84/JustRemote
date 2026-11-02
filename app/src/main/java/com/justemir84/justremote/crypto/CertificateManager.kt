package com.justemir84.justremote.crypto

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Date
import javax.security.auth.x500.X500Principal

class CertificateManager(private val context: Context) {

    companion object {
        const val KEY_ALIAS = "justremote_client"
        const val KEYSTORE_PASSWORD = "justremote" // production'da güvenli saklanmalı
    }

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreateKeyPair(): KeyPair {
        return try {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            if (entry != null) {
                KeyPair(entry.certificate.publicKey, entry.privateKey)
            } else {
                generateKeyPair()
            }
        } catch (e: Exception) {
            generateKeyPair()
        }
    }

    private fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val kp = kpg.generateKeyPair()

        // Self-signed certificate oluştur
        val subject = X500Name("CN=JustRemote Client")
        val serial = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date(System.currentTimeMillis() - 86400000L * 365)
        val notAfter = Date(System.currentTimeMillis() + 86400000L * 365 * 10)
        val builder = JcaX509v3CertificateBuilder(
            subject, serial, notBefore, notAfter, subject, kp.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(kp.private)
        val cert = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        // KeyStore'a kaydet
        val entry = KeyStore.PrivateKeyEntry(kp.private, arrayOf(cert))
        keyStore.setEntry(KEY_ALIAS, entry, null)

        return kp
    }

    fun getCertificateModulusHex(): String {
        val kp = getOrCreateKeyPair()
        val modulus = (kp.public as java.security.interfaces.RSAPublicKey).modulus
        return modulus.toString(16)
    }

    fun getCertificateExponentHex(): String {
        val kp = getOrCreateKeyPair()
        val exponent = (kp.public as java.security.interfaces.RSAPublicKey).publicExponent
        return exponent.toString(16).padStart(2, '0')
    }
}