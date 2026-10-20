package com.boost.pubg

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager

val PUBG_PACKAGES = listOf("com.tencent.ig", "com.pubg.krmobile", "com.pubg.imobile", "com.vng.pubgmobile", "com.rekoo.pubgm")

data class Snapshot(
    val device: String, val hz: Float, val supportedHz: List<Float>, val hdr: List<String>,
    val ramFreeMb: Long, val ramTotalMb: Long, val battery: Int, val tempC: Float,
    val powerSave: Boolean, val width: Int, val height: Int, val dpi: Int
)

object Tweaks {
    @Suppress("DEPRECATION")
    fun snapshot(c: Context): Snapshot {
        val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d: Display = wm.defaultDisplay
        val dm = DisplayMetrics().also { d.getRealMetrics(it) }
        val hz = d.supportedModes.map { Math.round(it.refreshRate).toFloat() }.distinct().sorted()
        val hdr = d.hdrCapabilities?.supportedHdrTypes?.map {
            when (it) { Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"; else -> "HDR($it)" } } ?: emptyList()
        val mi = ActivityManager.MemoryInfo().also { (c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it) }
        val b: Intent? = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val lvl = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temp = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val ps = (c.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
        return Snapshot("${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})", d.refreshRate, hz, hdr,
            mi.availMem shr 20, mi.totalMem shr 20, if (lvl >= 0) lvl * 100 / scale else -1, temp, ps, dm.widthPixels, dm.heightPixels, dm.densityDpi)
    }

    fun installedPubg(c: Context) = PUBG_PACKAGES.firstOrNull { c.packageManager.getLaunchIntentForPackage(it) != null }

    fun launchPubg(c: Context): Boolean {
        val i = installedPubg(c)?.let { c.packageManager.getLaunchIntentForPackage(it) } ?: return false
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true
    }

    private fun gameMode(mode: String) = installedPubg0?.let { ShizukuShell.run("cmd game mode $mode $it") }
    var installedPubg0: String? = null

    // ---- 120 FPS (display refresh rate) ----
    fun enable120(): String {
        val a = ShizukuShell.run("settings put system peak_refresh_rate 120.0")
        val b = ShizukuShell.run("settings put system min_refresh_rate 120.0")
        gameMode("performance")
        return if (a.first == 0 && b.first == 0) "تم طلب 120Hz من النظام. تحقق من المعدل المعروض؛ إن لم يتغير فالجهاز لا يدعمه أو ROM يتجاهل الإعداد."
        else "فشل: ${a.second} ${b.second}"
    }
    fun reset120() { ShizukuShell.run("settings delete system peak_refresh_rate"); ShizukuShell.run("settings delete system min_refresh_rate") }

    // ---- iPad view: 4:3 resolution (letterbox-free wide-ish layout) ----
    fun ipadPreview(s: Snapshot): Triple<Int, Int, Int> {
        val shortSide = minOf(s.width, s.height); val longSide = maxOf(s.width, s.height)
        val newLong = shortSide * 4 / 3
        return Triple(shortSide, minOf(newLong, longSide), (s.dpi * 0.85f).toInt())
    }
    fun applyIpad(w: Int, h: Int, dpi: Int): String {
        val a = ShizukuShell.run("wm size ${w}x$h"); val b = ShizukuShell.run("wm density $dpi")
        return if (a.first == 0 && b.first == 0) "تم التطبيق. للاستعادة اضغط RESTORE." else "فشل: ${a.second} ${b.second}"
    }
    fun restoreDisplay() { ShizukuShell.run("wm size reset"); ShizukuShell.run("wm density reset") }

    // ---- Anti lag: safe actions only ----
    fun antiLag(): String {
        val k = ShizukuShell.run("cmd activity kill-all")          // kills only cached background processes
        ShizukuShell.run("settings put global low_power 0")        // turn off battery saver
        gameMode("performance")
        return "تم إغلاق عمليات الخلفية المخزنة وتعطيل توفير الطاقة. ${k.second.take(80)}"
    }

    // ---- Modes ----
    fun balanced(): String { reset120(); ShizukuShell.run("settings put global low_power 0"); gameMode("standard"); return "Balanced: إعدادات افتراضية للنظام." }
    fun extreme(): String { antiLag(); ShizukuShell.run("settings put system peak_refresh_rate 120.0"); return "Extreme: تنظيف الخلفية + وضع أداء + أعلى معدل تحديث متاح." }
    fun mode120(): String = enable120()
}
