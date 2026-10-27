package com.example.universalapplocker

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * সামনে কোন অ্যাপ খোলা আছে তা ধরে, প্রতি সেকেন্ডে সময় গোনে
 * এবং সীমা পেরোলে LockActivity দেখায়।
 */
class AppMonitorService : AccessibilityService() {
    private lateinit var store: Store
    private val handler = Handler(Looper.getMainLooper())
    private var currentPackage: String? = null
    private var lastTick = 0L
    private var carryMs = 0L
    private var lastLaunch = 0L

    // কিবোর্ড ও নোটিফিকেশন শেড খুললে অ্যাপের সময় গোনা থামবে না
    private val ignored = setOf("com.android.systemui", "android")

    private val ticker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (isScreenOn()) {
                accrue(now)
                val pkg = currentPackage
                if (pkg != null) enforce(pkg)
            } else {
                lastTick = now
            }
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = Store(this)
        lastTick = System.currentTimeMillis()
        handler.removeCallbacksAndMessages(null)
        handler.post(ticker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored || isIme(pkg)) return

        val now = System.currentTimeMillis()
        if (pkg != currentPackage) {
            accrue(now)
            currentPackage = pkg
            carryMs = 0L
            syncFromUsageStats(pkg)
        }
        enforce(pkg)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun isScreenOn(): Boolean {
        return try {
            (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        } catch (e: Exception) {
            true
        }
    }

    private fun isIme(pkg: String): Boolean {
        return try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.any { it.packageName == pkg }
        } catch (e: Exception) {
            false
        }
    }

    /** আগের মুহূর্ত থেকে এখন পর্যন্ত চলতি অ্যাপের সময় যোগ করে। */
    private fun accrue(now: Long) {
        val pkg = currentPackage
        val previous = lastTick
        lastTick = now
        if (pkg == null || previous <= 0L || now <= previous) return
        if (store.getLimit(pkg) <= 0) return
        carryMs += (now - previous).coerceAtMost(5000L)
        val secs = carryMs / 1000L
        carryMs %= 1000L
        if (secs > 0L) store.addUsed(pkg, secs)
    }

    /** Usage Access দেওয়া থাকলে আজকের আগের ব্যবহারও হিসাবে ধরে। */
    private fun syncFromUsageStats(pkg: String) {
        if (store.getLimit(pkg) <= 0) return
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val map = usm.queryAndAggregateUsageStats(
                store.startOfDayMillis(), System.currentTimeMillis()
            )
            val sec = (map[pkg]?.totalTimeInForeground ?: 0L) / 1000L
            if (sec > store.getUsed(pkg)) store.setUsed(pkg, sec)
        } catch (e: Exception) {
            // অনুমতি না থাকলে নিজের হিসাবই চলবে
        }
    }

    private fun enforce(pkg: String) {
        if (pkg == packageName) return
        if (store.getLimit(pkg) <= 0) return
        if (!store.isOverLimit(pkg)) return
        if (LockActivity.visible) return
        val now = System.currentTimeMillis()
        if (now - lastLaunch < 1500L) return
        lastLaunch = now
        try {
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.putExtra("locked_package", pkg)
            startActivity(intent)
        } catch (e: Exception) {
            // পরের সেকেন্ডে আবার চেষ্টা হবে
        }
    }
}
