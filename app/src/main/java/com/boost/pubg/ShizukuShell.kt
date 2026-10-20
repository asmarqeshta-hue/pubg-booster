package com.boost.pubg

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

enum class SState { NOT_RUNNING, NO_PERMISSION, READY }

object ShizukuShell {
    const val REQ = 1001

    fun state(): SState {
        return try {
            when {
                !Shizuku.pingBinder() -> SState.NOT_RUNNING
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> SState.NO_PERMISSION
                else -> SState.READY
            }
        } catch (e: Throwable) {
            SState.NOT_RUNNING
        }
    }

    fun requestPermission() {
        try {
            if (!Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(REQ)
            }
        } catch (e: Throwable) {
        }
    }

    fun run(cmd: String): Pair<Int, String> {
        return try {
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            m.isAccessible = true
            val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
            Pair(p.waitFor(), out.trim())
        } catch (e: Throwable) {
            Pair(-1, e.message ?: "error")
        }
    }
}
