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
import com.justemir84.justremote.atv.AtvRemoteClient
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("") }
    var client by remember { mutableStateOf<AtvRemoteClient?>(null) }
    var paired by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Hazır") }
    var showRemote by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("JustRemote", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (!showRemote) {
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("TV IP Adresi") })
            Spacer(Modifier.height(8.dp))
            if (client == null) {
                Button(onClick = {
                    client = AtvRemoteClient(context, host, "JustRemote")
                    status = "İstemci oluşturuldu"
                }) { Text("İstemci Oluştur") }
            } else {
                Button(onClick = {
                    scope.launch {
                        status = "Eşleştirme başlatılıyor..."
                        runCatching { client?.startPairing() }
                            .onSuccess { status = "TV'de kod gösterildi. Kodu girin." }
                            .onFailure { status = "Hata: ${it.message}" }
                    }
                }) { Text("Eşleştirmeyi Başlat") }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("6 haneli PIN") })
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        status = "Eşleştiriliyor..."
                        runCatching { client?.finishPairing(pin) }
                            .onSuccess { paired = true; status = "Eşleştirildi ✓" }
                            .onFailure { status = "Hata: ${it.message}" }
                    }
                }) { Text("Eşleştirmeyi Tamamla") }
            }
            Spacer(Modifier.height(16.dp))
            Text(status)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { showRemote = true }) { Text("Kumanda Ekranı") }
        } else {
            RemoteControlPanel(client, keySender = null) { showRemote = false }
        }
    }
}

@Composable
fun RemoteControlPanel(client: AtvRemoteClient?, keySender: Any?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Kumanda", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            Button(onClick = { scope.launch { client?.sendKeyCode("UP") } }) { Text("▲") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { client?.sendKeyCode("LEFT") } }) { Text("◀") }
            Button(onClick = { scope.launch { client?.sendKeyCode("ENTER") } }) { Text("OK") }
            Button(onClick = { scope.launch { client?.sendKeyCode("RIGHT") } }) { Text("▶") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            Button(onClick = { scope.launch { client?.sendKeyCode("DOWN") } }) { Text("▼") }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { client?.sendKeyCode("BACK") } }) { Text("Geri") }
            Button(onClick = { scope.launch { client?.sendKeyCode("HOME") } }) { Text("Home") }
            Button(onClick = { scope.launch { client?.sendKeyCode("MENU") } }) { Text("Menü") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { client?.sendKeyCode("VOL_UP") } }) { Text("Ses +") }
            Button(onClick = { scope.launch { client?.sendKeyCode("VOL_DOWN") } }) { Text("Ses -") }
            Button(onClick = { scope.launch { client?.sendKeyCode("PLAY_PAUSE") } }) { Text("Play/Pause") }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Metin (klavye)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { scope.launch { client?.sendText("test") } }) { Text("Metin Gönder") }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Geri Dön") }
    }
}