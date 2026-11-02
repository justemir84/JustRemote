package com.justemir84.justremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justemir84.justremote.atv.AtvRemoteClient
import com.justemir84.justremote.discovery.DiscoveredTv
import com.justemir84.justremote.discovery.NsdDiscovery
import com.justemir84.justremote.ui.Fg
import com.justemir84.justremote.ui.Ink
import com.justemir84.justremote.ui.JustRemoteTheme
import com.justemir84.justremote.ui.Line
import com.justemir84.justremote.ui.Muted
import com.justemir84.justremote.ui.Panel as PanelColor
import com.justemir84.justremote.update.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setContent {
            JustRemoteTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    JustRemoteApp()
                }
            }
        }
    }
}

private enum class Screen { CONNECT, REMOTE, TOUCHPAD, KEYBOARD }

@Composable
private fun JustRemoteApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }

    var host by remember { mutableStateOf("") }
    var screen by remember { mutableStateOf(Screen.CONNECT) }
    var client by remember { mutableStateOf<AtvRemoteClient?>(null) }
    var paired by remember { mutableStateOf(false) }
    var awaitingPin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Hazır") }
    var discovering by remember { mutableStateOf(false) }
    var discovered by remember { mutableStateOf<List<DiscoveredTv>>(emptyList()) }
    var updateInfo by remember { mutableStateOf<UpdateManager.ReleaseInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    BackHandler(enabled = screen != Screen.CONNECT) { screen = Screen.CONNECT }

    LaunchedEffect(Unit) {
        checkingUpdate = true
        val info = updateManager.checkLatestRelease()
        if (info != null && info.version != updateManager.getCurrentVersion()) updateInfo = info
        checkingUpdate = false
    }

    val sendKey: (String) -> Unit = { key -> scope.launch { client?.sendKeyCode(key) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (screen) {
            Screen.CONNECT -> ConnectScreen(
                host = host,
                onHostChange = { host = it },
                discovered = discovered,
                discovering = discovering,
                onScan = {
                    discovering = true
                    discovered = emptyList()
                    scope.launch {
                        val nsd = NsdDiscovery(context)
                        nsd.discover(8000).collect { found -> discovered = found }
                        discovering = false
                    }
                },
                onConnect = {
                    val target = host.trim()
                    if (target.isEmpty()) {
                        status = "Önce TV'nin IP adresini gir"
                    } else {
                        scope.launch {
                            status = "TV'ye bağlanılıyor..."
                            runCatching {
                                client?.disconnect()
                                val c = AtvRemoteClient(context, target, "Just Remote")
                                client = c
                                c.startPairing()
                            }.onSuccess {
                                awaitingPin = true
                                status = "TV'de gösterilen 6 haneli kodu gir"
                            }.onFailure {
                                status = "Bağlantı hatası: ${it.message}"
                            }
                        }
                    }
                },
                status = status,
                paired = paired,
                awaitingPin = awaitingPin,
                pin = pin,
                onPinChange = { pin = it.uppercase() },
                onFinishPairing = {
                    scope.launch {
                        status = "Eşleştiriliyor..."
                        runCatching { client?.finishPairing(pin) }
                            .onSuccess {
                                paired = true
                                awaitingPin = false
                                status = "Eşleştirildi"
                                screen = Screen.REMOTE
                            }
                            .onFailure { status = "Eşleştirme hatası: ${it.message}" }
                    }
                },
                checkingUpdate = checkingUpdate,
                onCheckUpdate = {
                    scope.launch {
                        checkingUpdate = true
                        val info = updateManager.checkLatestRelease()
                        if (info != null && info.version != updateManager.getCurrentVersion()) {
                            updateInfo = info
                        } else {
                            status = "En son sürümü kullanıyorsun"
                        }
                        checkingUpdate = false
                    }
                },
                onOpen = { screen = it },
            )

            Screen.REMOTE -> RemoteScreen(
                host = host,
                client = client,
                onBack = { screen = Screen.CONNECT },
                onKey = sendKey,
                onSelect = { screen = it },
            )

            Screen.TOUCHPAD -> TouchpadScreen(
                host = host,
                client = client,
                onBack = { screen = Screen.CONNECT },
                onKey = sendKey,
                onSelect = { screen = it },
            )

            Screen.KEYBOARD -> KeyboardScreen(
                host = host,
                client = client,
                onBack = { screen = Screen.CONNECT },
                onSelect = { screen = it },
            )
        }
    }

    val info = updateInfo
    if (info != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            containerColor = PanelColor,
            titleContentColor = Fg,
            textContentColor = Muted,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Güncelleme mevcut") },
            text = { Text("Yeni sürüm ${info.version}. Şimdi indirip kurmak ister misin?") },
            confirmButton = {
                TextButton(onClick = {
                    if (info.downloadUrl != null) {
                        downloading = true
                        scope.launch {
                            val ok = updateManager.downloadAndInstall(info.downloadUrl) { p -> progress = p }
                            downloading = false
                            if (!ok) status = "Güncelleme indirilemedi"
                        }
                    }
                    updateInfo = null
                }) { Text(if (downloading) "%$progress" else "İNDİR", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) { Text("SONRA", color = Muted) }
            },
        )
    }
}

/* ------------------------------------------------------------------ */
/* Connect                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun ConnectScreen(
    host: String,
    onHostChange: (String) -> Unit,
    discovered: List<DiscoveredTv>,
    discovering: Boolean,
    onScan: () -> Unit,
    onConnect: () -> Unit,
    status: String,
    paired: Boolean,
    awaitingPin: Boolean,
    pin: String,
    onPinChange: (String) -> Unit,
    onFinishPairing: () -> Unit,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onOpen: (Screen) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Image(
            painter = painterResource(R.drawable.logo_badge),
            contentDescription = "Just Remote",
            modifier = Modifier.size(132.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "ANDROID TV KUMANDASI",
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
        )
        Spacer(Modifier.height(26.dp))

        PanelCard {
            SectionLabel("BAĞLANTI")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                label = { Text("TV IP adresi") },
                placeholder = { Text("192.168.1.20", color = Color(0xFF454545)) },
                colors = fieldColors(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton(
                    text = if (discovering) "TARANIYOR" else "TARA",
                    modifier = Modifier.weight(1f),
                    onClick = onScan,
                )
                SolidButton(
                    text = if (paired) "TEKRAR BAĞLAN" else "BAĞLAN",
                    modifier = Modifier.weight(1f),
                    onClick = onConnect,
                )
            }

            if (discovered.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                SectionLabel("BULUNAN CİHAZLAR")
                Spacer(Modifier.height(4.dp))
                discovered.forEach { tv ->
                    DeviceRow(tv) { onHostChange(tv.host) }
                }
            } else if (discovering) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 1.5.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Aynı Wi-Fi'deki TV'ler aranıyor...",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (awaitingPin) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("EŞLEŞTİRME KODU")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("6 haneli kod") },
                    placeholder = { Text("A1B2C3", color = Color(0xFF454545)) },
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                SolidButton(
                    text = "EŞLEŞTİR",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pin.length >= 6,
                    onClick = onFinishPairing,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        StatusBar(status, highlight = paired)

        if (paired) {
            Spacer(Modifier.height(18.dp))
            BottomNav(current = Screen.CONNECT, onSelect = onOpen)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onCheckUpdate, enabled = !checkingUpdate) {
            Text(
                if (checkingUpdate) "KONTROL EDİLİYOR..." else "GÜNCELLEME KONTROL ET",
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
            )
        }
        Spacer(Modifier.height(18.dp))
    }
}

/* ------------------------------------------------------------------ */
/* Remote                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun RemoteScreen(
    host: String,
    client: AtvRemoteClient?,
    onBack: () -> Unit,
    onKey: (String) -> Unit,
    onSelect: (Screen) -> Unit,
) {
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(client) {
        while (true) {
            connected = client?.isConnected == true
            delay(1200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader("KUMANDA", host.ifBlank { "TV" }, connected, onBack)
        Spacer(Modifier.height(22.dp))

        SectionLabel("GEZİNME")
        Spacer(Modifier.height(12.dp))
        KeyCircle("↑") { onKey("UP") }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KeyCircle("←") { onKey("LEFT") }
            KeyCircle("OK", size = 86.dp, filled = true) { onKey("ENTER") }
            KeyCircle("→") { onKey("RIGHT") }
        }
        KeyCircle("↓") { onKey("DOWN") }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill("GERİ", Modifier.weight(1f)) { onKey("BACK") }
            ActionPill("HOME", Modifier.weight(1f)) { onKey("HOME") }
            ActionPill("MENÜ", Modifier.weight(1f)) { onKey("MENU") }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("SES")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill("SES −", Modifier.weight(1f)) { onKey("VOL_DOWN") }
            ActionPill("SES +", Modifier.weight(1f)) { onKey("VOL_UP") }
            ActionPill("SESSİZ", Modifier.weight(1f)) { onKey("MUTE") }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("MEDYA")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill("ÖNCEKİ", Modifier.weight(1f)) { onKey("MEDIA_PREV") }
            ActionPill("OYNAT", Modifier.weight(1f)) { onKey("PLAY_PAUSE") }
            ActionPill("SONRAKİ", Modifier.weight(1f)) { onKey("MEDIA_NEXT") }
        }

        Spacer(Modifier.weight(1f))
        BottomNav(current = Screen.REMOTE, onSelect = onSelect)
        Spacer(Modifier.height(6.dp))
    }
}

/* ------------------------------------------------------------------ */
/* Touchpad                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun TouchpadScreen(
    host: String,
    client: AtvRemoteClient?,
    onBack: () -> Unit,
    onKey: (String) -> Unit,
    onSelect: (Screen) -> Unit,
) {
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(client) {
        while (true) {
            connected = client?.isConnected == true
            delay(1200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader("TOUCHPAD", host.ifBlank { "TV" }, connected, onBack)
        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Line, RoundedCornerShape(28.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onKey("ENTER") })
                }
                .pointerInput(Unit) {
                    var accX = 0f
                    var accY = 0f
                    var sent = false
                    detectDragGestures(
                        onDragStart = {
                            accX = 0f
                            accY = 0f
                            sent = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accX += dragAmount.x
                            accY += dragAmount.y
                            if (!sent) {
                                if (abs(accX) > 60f && abs(accX) >= abs(accY)) {
                                    onKey(if (accX > 0) "RIGHT" else "LEFT")
                                    sent = true
                                } else if (abs(accY) > 60f) {
                                    onKey(if (accY > 0) "DOWN" else "UP")
                                    sent = true
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("↑", color = Color(0xFF3A3A3A), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Kaydırarak gezin\nDokunarak seç",
                    color = Color(0xFF3A3A3A),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SolidButton("TIKLA (OK)", Modifier.weight(1f)) { onKey("ENTER") }
            OutlineButton("GERİ", Modifier.weight(1f)) { onKey("BACK") }
        }

        Spacer(Modifier.height(14.dp))
        BottomNav(current = Screen.TOUCHPAD, onSelect = onSelect)
        Spacer(Modifier.height(6.dp))
    }
}

/* ------------------------------------------------------------------ */
/* Keyboard                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun KeyboardScreen(
    host: String,
    client: AtvRemoteClient?,
    onBack: () -> Unit,
    onSelect: (Screen) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(client) {
        while (true) {
            connected = client?.isConnected == true
            delay(1200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader("KLAVYE", host.ifBlank { "TV" }, connected, onBack)
        Spacer(Modifier.height(18.dp))

        PanelCard {
            SectionLabel("METİN")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text("TV'ye gönderilecek metin") },
                colors = fieldColors(),
                minLines = 3,
            )
            Spacer(Modifier.height(12.dp))
            SolidButton(
                text = "GÖNDER",
                modifier = Modifier.fillMaxWidth(),
                enabled = text.text.isNotBlank(),
                onClick = {
                    scope.launch {
                        client?.sendText(text.text)
                        text = TextFieldValue("")
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Metin, TV'de açık olan arama/klavye alanına yazılır.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))
        BottomNav(current = Screen.KEYBOARD, onSelect = onSelect)
        Spacer(Modifier.height(6.dp))
    }
}

/* ------------------------------------------------------------------ */
/* Components                                                          */
/* ------------------------------------------------------------------ */

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = Muted,
    )
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(PanelColor)
            .border(1.dp, Line, RoundedCornerShape(26.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun StatusBar(text: String, highlight: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (highlight) Color.White else Color(0xFF4A4A4A)),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DeviceRow(tv: DiscoveredTv, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tv.name.ifBlank { "Android TV" },
                color = Fg,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(tv.host, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Text("SEÇ", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    subtitle: String,
    connected: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, Line, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("←", color = Fg, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Muted)
            Text(
                subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = Fg,
                maxLines = 1,
            )
        }
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (connected) Color.White else Color(0xFF454545)),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (connected) "BAĞLI" else "BEKLEMEDE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Muted,
        )
    }
}

@Composable
private fun KeyCircle(
    label: String,
    size: Dp = 74.dp,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) Color.White else Color(0xFF0A0A0A))
            .border(1.dp, if (filled) Color.White else Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (filled) Color.Black else Fg,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ActionPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Fg)
    }
}

@Composable
private fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NavChip("KUMANDA", current == Screen.REMOTE, Modifier.weight(1f)) { onSelect(Screen.REMOTE) }
        NavChip("TOUCHPAD", current == Screen.TOUCHPAD, Modifier.weight(1f)) { onSelect(Screen.TOUCHPAD) }
        NavChip("KLAVYE", current == Screen.KEYBOARD, Modifier.weight(1f)) { onSelect(Screen.KEYBOARD) }
    }
}

@Composable
private fun NavChip(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (active) Color.White else Color.Transparent)
            .border(1.dp, if (active) Color.White else Line, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = if (active) Color.Black else Muted,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SolidButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color(0xFF1A1A1A),
            disabledContentColor = Color(0xFF555555),
        ),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        border = BorderStroke(1.dp, if (enabled) Line else Color(0xFF161616)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Fg,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color(0xFF4A4A4A),
        ),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Line,
    focusedTextColor = Fg,
    unfocusedTextColor = Fg,
    cursorColor = Color.White,
    focusedLabelColor = Muted,
    unfocusedLabelColor = Muted,
)
