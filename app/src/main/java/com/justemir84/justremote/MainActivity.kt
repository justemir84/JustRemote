package com.justemir84.justremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { PairingManager(context) }
    val keySender = remember { KeySender(manager) }
    var screen by remember { mutableStateOf("pairing") } // pairing | remote | touchpad | keyboard

    when (screen) {
        "pairing" -> PairingScreen(manager) { screen = "remote" }
        "remote" -> RemoteScreen(keySender) { screen = it }
        "touchpad" -> TouchpadScreen(keySender) { screen = "remote" }
        "keyboard" -> KeyboardScreen(keySender) { screen = "remote" }
    }
}

@Composable
fun PairingScreen(manager: PairingManager, onPaired: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("6466") }
    var code by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<PairingState>(PairingState.Idle) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("JustRemote — Eşleştirme", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("TV IP Adresi") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Eşleştirme Kodu") })
        Spacer(Modifier.height(16.dp))
        Button(onClick = { manager.connect(host, port.toIntOrNull() ?: 6466) { s -> state = s } }) { Text("Bağlan") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onPaired) { Text("Kumanda Ekranı") }
        Spacer(Modifier.height(16.dp))
        when (state) {
            is PairingState.Connecting -> Text("Bağlanıyor...")
            is PairingState.PairingRequired -> Text("Eşleştirme kodu girin.")
            is PairingState.Paired -> Text("Eşleştirildi ✓")
            is PairingState.Error -> Text("Hata: ${(state as PairingState.Error).message}")
            else -> Text("Hazır")
        }
    }
}

@Composable
fun RemoteScreen(keySender: KeySender, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kumanda", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
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
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { keySender.sendKey(RemoteKey.VOLUME_UP) }) { Text("Ses +") }
            Button(onClick = { keySender.sendKey(RemoteKey.VOLUME_DOWN) }) { Text("Ses -") }
            Button(onClick = { keySender.sendKey(RemoteKey.PLAY_PAUSE) }) { Text("Play/Pause") }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onNavigate("touchpad") }) { Text("Touchpad") }
            Button(onClick = { onNavigate("keyboard") }) { Text("Klavye") }
        }
    }
}

@Composable
fun TouchpadScreen(keySender: KeySender, onBack: () -> Unit) {
    var lastX by remember { mutableStateOf(0f) }
    var lastY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Touchpad (Kaydırarak Kullan)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastX = offset.x
                            lastY = offset.y
                        },
                        onDrag = { change, dragAmount ->
                            keySender.sendMouseMove(dragAmount.x, dragAmount.y)
                            change.consume()
                        }
                    )
                }
        ) {
            Text("Buraya dokun ve kaydır", modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { keySender.sendClick() }) { Text("Tıkla") }
            Button(onClick = { keySender.sendScroll(-50f) }) { Text("Yukarı Kaydır") }
            Button(onClick = { keySender.sendScroll(50f) }) { Text("Aşağı Kaydır") }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Geri") }
    }
}

@Composable
fun KeyboardScreen(keySender: KeySender, onBack: () -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Klavye Girişi", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Metin yazın") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { keySender.sendText(text.text) }) { Text("Gönder") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Geri") }
    }
}