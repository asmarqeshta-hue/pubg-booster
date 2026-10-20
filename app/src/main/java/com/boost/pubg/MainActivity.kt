package com.boost.pubg

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

val Neon = Color(0xFFFF1744)
val Dark = Color(0xFF121212)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Neon, background = Color.Black, surface = Dark)) { Home() } }
    }
}

@Composable
fun Home() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var snap by remember { mutableStateOf(Tweaks.snapshot(ctx)) }
    var shz by remember { mutableStateOf(ShizukuShell.state()) }
    var msg by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Triple<String, String, () -> String>?>(null) }
    Tweaks.installedPubg0 = Tweaks.installedPubg(ctx)

    LaunchedEffect(Unit) { while (true) { snap = Tweaks.snapshot(ctx); shz = ShizukuShell.state(); delay(2000) } }

    fun act(block: () -> String) {
        if (shz != SState.READY) { msg = "Shizuku غير جاهز"; return }
        scope.launch { msg = withContext(Dispatchers.IO) { block() }; snap = Tweaks.snapshot(ctx) }
    }

    val glow by rememberInfiniteTransition(label = "g").animateFloat(0.55f, 1f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")

    Column(Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(16.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("PUBG 120FPS HDR BOOSTER", color = Neon, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.alpha(glow))

        Card("SHIZUKU") {
            val (t, c) = when (shz) { SState.READY -> "متصل ✔" to Color(0xFF00E676); SState.NO_PERMISSION -> "بحاجة لإذن" to Color.Yellow; else -> "غير مشغّل" to Neon }
            Text(t, color = c, fontWeight = FontWeight.Bold)
            if (shz == SState.NO_PERMISSION) RedButton("منح الإذن") { ShizukuShell.requestPermission() }
            if (shz == SState.NOT_RUNNING) Text("شغّل تطبيق Shizuku أولاً (Wireless debugging).", color = Color.Gray, fontSize = 12.sp)
        }
        Card("DEVICE") {
            Text(snap.device, color = Color.White)
            Text("الشاشة: ${snap.width}x${snap.height} @ ${snap.dpi}dpi", color = Color.Gray)
            Text("معدل التحديث الفعلي: ${"%.0f".format(snap.hz)} Hz  (المدعوم: ${snap.supportedHz.joinToString { "${it.toInt()}" }})", color = Neon)
            Text("HDR: ${if (snap.hdr.isEmpty()) "غير مدعوم" else snap.hdr.joinToString()}", color = Color.White)
            Text("RAM: ${snap.ramFreeMb}/${snap.ramTotalMb} MB حرة", color = Color.White)
            Text("البطارية: ${snap.battery}% • الحرارة: ${snap.tempC}°C • توفير الطاقة: ${if (snap.powerSave) "مفعّل" else "معطّل"}", color = Color.White)
        }
        Card("MODES") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { RedButton("BALANCED") { act { Tweaks.balanced() } } }
                Box(Modifier.weight(1f)) { RedButton("EXTREME") { act { Tweaks.extreme() } } }
            }
            RedButton("120 FPS MODE") { act { Tweaks.mode120() } }
        }
        RedButton("ENABLE 120 FPS") {
            if (!snap.supportedHz.any { it >= 120f }) msg = "شاشتك لا تدعم 120Hz (المدعوم: ${snap.supportedHz.joinToString { "${it.toInt()}" }}). سأحاول أعلى معدل ممكن."
            act { Tweaks.enable120() }
        }
        RedButton("ENABLE HDR") {
            msg = if (snap.hdr.isEmpty()) "الشاشة لا تدعم HDR."
            else "الشاشة تدعم: ${snap.hdr.joinToString()}. Android لا يوفر تبديلاً عاماً لـ HDR؛ فعّله من إعدادات الشاشة، وداخل اللعبة يعتمد على دعمها. "
            ctx.startActivity(android.content.Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        RedButton("IPAD VIEW") {
            val (w, h, d) = Tweaks.ipadPreview(snap)
            confirm = Triple("IPAD VIEW", "سيتم ضبط الدقة إلى ${w}x$h (نسبة 4:3) وكثافة $d dpi.\nيغيّر حجم كل واجهة النظام وليس اللعبة فقط. يمكنك الاستعادة بزر RESTORE.") { Tweaks.applyIpad(w, h, d) }
        }
        OutlinedButton({ act { Tweaks.restoreDisplay(); "تمت استعادة الدقة والكثافة الأصلية." } }, Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Neon)) { Text("RESTORE DISPLAY", color = Neon) }
        RedButton("ANTI LAG BOOST") {
            confirm = Triple("ANTI LAG BOOST", "سيغلق عمليات الخلفية المخزنة، يعطّل توفير الطاقة، ويضبط وضع اللعبة على Performance. لا أوامر خطرة.") { Tweaks.antiLag() }
        }
        RedButton("LAUNCH PUBG") { if (!Tweaks.launchPubg(ctx)) msg = "PUBG Mobile غير مثبتة." }

        if (msg.isNotEmpty()) Text(msg, color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
    }

    confirm?.let { (title, body, action) ->
        AlertDialog(onDismissRequest = { confirm = null }, containerColor = Dark,
            title = { Text(title, color = Neon) }, text = { Text(body, color = Color.White) },
            confirmButton = { TextButton({ confirm = null; act(action) }) { Text("تطبيق", color = Neon) } },
            dismissButton = { TextButton({ confirm = null }) { Text("إلغاء", color = Color.Gray) } })
    }
}

@Composable
fun Card(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Dark, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Neon.copy(alpha = .6f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Neon, fontSize = 12.sp, fontWeight = FontWeight.Bold); content()
        }
    }
}

@Composable
fun RedButton(text: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.White)) { Text(text, fontWeight = FontWeight.Bold) }
}
