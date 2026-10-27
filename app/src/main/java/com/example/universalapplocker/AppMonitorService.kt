package com.example.universalapplocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppMonitorService : AccessibilityService() {
    private val prefs by lazy { getSharedPreferences("locker", MODE_PRIVATE) }
    private var currentPackage: String? = null
    private var lastTick = 0L
    private var lastLockedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        lastTick = System.currentTimeMillis()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val now = System.currentTimeMillis()
        accrueUsage(now)
        currentPackage = pkg
        lastTick = now
        checkLimit(pkg)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        accrueUsage(System.currentTimeMillis())
        super.onDestroy()
    }

    private fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun ensureNewDay() {
        val today = today()
        if (prefs.getString("usage_day", "") != today) {
            val editor = prefs.edit()
            for ((key, _) in prefs.all) {
                if (key.startsWith("used_")) editor.remove(key)
            }
            editor.putString("usage_day", today).apply()
        }
    }

    private fun accrueUsage(now: Long) {
        ensureNewDay()
        val pkg = currentPackage ?: return
        val previous = lastTick
        if (previous <= 0L || now <= previous) return
        val delta = (now - previous).coerceAtMost(5000L) / 1000L
        if (delta <= 0) return
        val key = "used_${pkg}"
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + delta).apply()
        checkLimit(pkg)
    }

    private fun checkLimit(pkg: String) {
        if (pkg == packageName || pkg == LockActivity::class.java.name) return
        val limitMinutes = prefs.getInt("limit_$pkg", 0)
        if (limitMinutes <= 0) {
            lastLockedPackage = null
            return
        }
        val todayKey = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        if (prefs.getBoolean("bypass_${todayKey}_$pkg", false)) return
        val usedSeconds = prefs.getLong("used_$pkg", 0L)
        if (usedSeconds >= limitMinutes * 60L && lastLockedPackage != pkg) {
            lastLockedPackage = pkg
            val intent = Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("locked_package", pkg)
            }
            startActivity(intent)
        }
    }
}
