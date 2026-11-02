package com.justemir84.justremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.justemir84.justremote.pairing.PairingManager
import com.justemir84.justremote.pairing.PairingState
import com.justemir84.justremote.remote.KeySender
import com.justemir84.justremote.remote.RemoteKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { PairingManager(context) }
    val keySender = remember { KeySender(manager) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("6466") }
    var code by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<PairingState>(PairingState.Idle) }
    var showRemote by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("JustRemote", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (!showRemote) {
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("TV IP Adresi") })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Eşleştirme Kodu") })
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                manager.connect(host, port.toIntOrNull() ?: 6466) { s -> state = s }
            }) { Text("Bağlan") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showRemote = true }) { Text("Kumanda Ekranı") }
            Spacer(Modifier.height(16.dp))
            when (state) {
                is PairingState.Connecting -> Text("Bağlanıyor...")
                is PairingState.PairingRequired -> Text("Eşleştirme kodu girin.")
                is PairingState.Paired -> Text("Eşleştirildi ✓")
                is PairingState.Error -> Text("Hata: ${(state as PairingState.Error).message}")
                else -> Text("Hazır")
            }
        } else {
            RemoteControlPanel(keySender) { showRemote = false }
        }
    }
}

@Composable
fun RemoteControlPanel(keySender: KeySender, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Kumanda", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        // D-pad
        Row(horizontalArrangement = Arrangement.Center) {
            Button(onClick = { keySender.sendKey(RemoteKey.UP) }) { Text("▲") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { keySender.sendKey(RemoteKey.LEFT) }) { Text("◀") }
            Button(onClick = { keySender.sendKey(RemoteKey.OK) }) { Text("OK") }
            Button(onClick = { keySender.sendKey(RemoteKey.RIGHT) }) { Text("▶") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            Button(onClick = { keySender.sendKey(RemoteKey.DOWN) }) { Text("▼") }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { keySender.sendKey(RemoteKey.BACK) }) { Text("Geri") }
            Button(onClick = { keySender.sendKey(RemoteKey.HOME) }) { Text("Home") }
            Button(onClick = { keySender.sendKey(RemoteKey.MENU) }) { Text("Menü") }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { keySender.sendKey(RemoteKey.VOLUME_UP) }) { Text("Ses +") }
            Button(onClick = { keySender.sendKey(RemoteKey.VOLUME_DOWN) }) { Text("Ses -") }
            Button(onClick = { keySender.sendKey(RemoteKey.PLAY_PAUSE) }) { Text("Play/Pause") }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Geri Dön") }
    }
}