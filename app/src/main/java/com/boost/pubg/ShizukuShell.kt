package com.boost.pubg

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

enum class SState { NOT_RUNNING, NO_PERMISSION, READY }

/** Runs shell commands through Shizuku (adb-level, no root). */
object ShizukuShell {
    const val REQ = 1001

    fun state(): SState = try {
        when {
            !Shizuku.pingBinder() -> SState.NOT_RUNNING
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> SState.NO_PERMISSION
            else -> SState.READY
        }
    } catch (e: Throwable) { SState.NOT_RUNNING }

    fun requestPermission() = try {
        if (!Shizuku.shouldShowRequestPermissionRationale()) Shizuku.requestPermission(REQ)
    } catch (_: Throwable) {}

    /** Returns Pair(exitCode, output). Never throws. */
    fun run(cmd: String): Pair<Int, String> = try {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
        m.isAccessible = true
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        p.waitFor() to out.trim()
    } catch (e: Throwable) { -1 to (e.message ?: "error") }
}
