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
import com.justemir84.justremote.atv.AtvRemoteClient
import com.justemir84.justremote.discovery.NsdDiscovery
import com.justemir84.justremote.update.UpdateManager
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
    var pin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Hazır") }
    var showRemote by remember { mutableStateOf(false) }
    var showTouchpad by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }
    var discovered by remember { mutableStateOf<List<com.justemir84.justremote.discovery.DiscoveredTv>>(emptyList()) }
    var discovering by remember { mutableStateOf(false) }
    val updateManager = remember { UpdateManager(context) }
    var updateInfo by remember { mutableStateOf<UpdateManager.ReleaseInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    // Auto-check update on launch
    LaunchedEffect(Unit) {
        checkingUpdate = true
        val info = updateManager.checkLatestRelease()
        if (info != null && info.version != updateManager.getCurrentVersion()) {
            updateInfo = info
        }
        checkingUpdate = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("JustRemote", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (!showRemote && !showTouchpad && !showKeyboard) {
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("TV IP Adresi") })
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                discovering = true
                discovered = emptyList()
                scope.launch {
                    val nsd = NsdDiscovery(context)
                    nsd.discover(8000).collect { list ->
                        discovered = list
                    }
                    discovering = false
                }
            }) { Text(if (discovering) "Taranıyor..." else "Cihaz Tara") }
            if (discovered.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                discovered.forEach { tv ->
                    Button(onClick = { host = tv.host }) {
                        Text("${tv.name} (${tv.host})")
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
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
                            .onSuccess { status = "Eşleştirildi ✓" }
                            .onFailure { status = "Hata: ${it.message}" }
                    }
                }) { Text("Eşleştirmeyi Tamamla") }
            }
            Spacer(Modifier.height(16.dp))
            Text(status)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    checkingUpdate = true
                    val info = updateManager.checkLatestRelease()
                    if (info != null && info.version != updateManager.getCurrentVersion()) {
                        updateInfo = info
                    } else {
                        status = "Güncel sürüm"
                    }
                    checkingUpdate = false
                }
            }) { Text(if (checkingUpdate) "Kontrol ediliyor..." else "Güncelleme Kontrol Et") }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { showRemote = true }) { Text("Kumanda Ekranı") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showTouchpad = true }) { Text("Touchpad") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showKeyboard = true }) { Text("Klavye") }
        } else if (showRemote) {
            RemoteControlPanel(client) { showRemote = false }
        } else if (showTouchpad) {
            TouchpadScreen(client) { showTouchpad = false }
        } else if (showKeyboard) {
            KeyboardScreen(client) { showKeyboard = false }
        }
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Güncelleme Mevcut") },
            text = { Text("Yeni sürüm: ${updateInfo!!.version}. İndirip kurmak ister misiniz?") },
            confirmButton = {
                Button(onClick = {
                    val info = updateInfo!!
                    if (info.downloadUrl != null) {
                        downloading = true
                        scope.launch {
                            val ok = updateManager.downloadAndInstall(info.downloadUrl) { p -> progress = p }
                            downloading = false
                            if (!ok) status = "Güncelleme başarısız"
                        }
                    }
                    updateInfo = null
                }) { Text(if (downloading) "%$progress" else "İndir") }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) { Text("Sonra") }
            }
        )
    }
}

@Composable
fun RemoteControlPanel(client: AtvRemoteClient?, onBack: () -> Unit) {
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
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Geri Dön") }
    }
}

@Composable
fun TouchpadScreen(client: AtvRemoteClient?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Touchpad (Kaydırarak Gezin)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(androidx.compose.ui.graphics.Color.DarkGray)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            // Swipe yönüne göre D-pad tuşu gönder
                            if (dragAmount.x > 30) { scope.launch { client?.sendKeyCode("RIGHT") } }
                            else if (dragAmount.x < -30) { scope.launch { client?.sendKeyCode("LEFT") } }
                            if (dragAmount.y > 30) { scope.launch { client?.sendKeyCode("DOWN") } }
                            else if (dragAmount.y < -30) { scope.launch { client?.sendKeyCode("UP") } }
                            change.consume()
                        }
                    )
                }
        ) {
            Text("Buraya dokun ve kaydır", modifier = Modifier.align(Alignment.Center), color = androidx.compose.ui.graphics.Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { client?.sendKeyCode("ENTER") } }) { Text("Tıkla (OK)") }
            Button(onClick = { scope.launch { client?.sendKeyCode("BACK") } }) { Text("Geri") }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Geri Dön") }
    }
}

@Composable
fun KeyboardScreen(client: AtvRemoteClient?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

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
        Button(onClick = { scope.launch { client?.sendText(text.text) } }) { Text("Gönder") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Geri Dön") }
    }
}