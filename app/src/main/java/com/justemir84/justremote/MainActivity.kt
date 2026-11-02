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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PairingScreen()
                }
            }
        }
    }
}

@Composable
fun PairingScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { PairingManager(context) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("6466") }
    var code by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<PairingState>(PairingState.Idle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("JustRemote — TV Eşleştirme", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("TV IP Adresi") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Eşleştirme Kodu (TV'de gösterilen)") })
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            manager.connect(host, port.toIntOrNull() ?: 6466) { s -> state = s }
        }) {
            Text("Bağlan")
        }
        Spacer(Modifier.height(16.dp))

        when (state) {
            is PairingState.Connecting -> Text("Bağlanıyor...")
            is PairingState.PairingRequired -> Text("Eşleştirme kodu girin ve onaylayın.")
            is PairingState.Paired -> Text("Eşleştirildi ✓")
            is PairingState.Error -> Text("Hata: ${(state as PairingState.Error).message}")
            else -> Text("Hazır")
        }
    }
}