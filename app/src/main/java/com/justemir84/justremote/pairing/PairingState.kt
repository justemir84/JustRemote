package com.justemir84.justremote.pairing

sealed class PairingState {
    object Idle : PairingState()
    object Connecting : PairingState()
    data class PairingRequired(val code: String? = null) : PairingState()
    object Paired : PairingState()
    data class Error(val message: String) : PairingState()
}