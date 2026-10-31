package com.example.universalapplocker

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * সামনে কোন অ্যাপ খোলা আছে তা ধরে, প্রতি সেকেন্ডে সময় গোনে
 * এবং সীমা পেরোলে লক স্ক্রিন দেখায়। লক স্ক্রিন প্রথমে Accessibility ওভারলে হিসেবে
 * (এতে আলাদা অনুমতি লাগে না, সব ব্র্যান্ডে চলে), না হলে LockActivity দিয়ে।
 */
class AppMonitorService : AccessibilityService() {
    companion object {
        @Volatile
        var running = false
    }

    private lateinit var store: Store
    private val handler = Handler(Looper.getMainLooper())
    private var currentPackage: String? = null
    private var lastTick = 0L
    private var carryMs = 0L
    private var lastLaunch = 0L
    private var overlay: View? = null
    private var overlayPkg: String? = null

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
        running = true
        lastTick = System.currentTimeMillis()
        handler.removeCallbacksAndMessages(null)
        handler.post(ticker)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored || isIme(pkg)) return

        // ব্যবহারকারী লক করা অ্যাপ ছেড়ে অন্য কোথাও গেলে ওভারলে সরিয়ে ফেলি
        if (overlay != null && pkg != overlayPkg && pkg != packageName) hideOverlay()

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

    override fun onUnbind(intent: Intent?): Boolean {
        running = false
        hideOverlay()
        handler.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        running = false
        hideOverlay()
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
        if (overlay != null) return
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
        if (overlay != null && overlayPkg == pkg) return
        if (LockActivity.visible) return
        val now = System.currentTimeMillis()
        if (now - lastLaunch < 1500L) return
        lastLaunch = now
        if (!showOverlay(pkg)) startLockActivity(pkg)
    }

    // ---------- ওভারলে লক ----------
    private fun showOverlay(pkg: String): Boolean {
        return try {
            hideOverlay()
            val ctx = LocaleHelper.wrap(this)
            Ui.applyTheme(ctx)
            val screen = LockScreen(ctx, store, pkg, { unlockedFromOverlay() }, { homeFromOverlay() })
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE
            )
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.addView(screen.root, params)
            overlay = screen.root
            overlayPkg = pkg
            true
        } catch (e: Exception) {
            overlay = null
            overlayPkg = null
            false
        }
    }

    private fun hideOverlay() {
        val v = overlay
        overlay = null
        overlayPkg = null
        if (v != null) {
            try {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(v)
            } catch (e: Exception) {
                // আগেই সরে গেছে
            }
        }
    }

    private fun unlockedFromOverlay() {
        val p = overlayPkg
        hideOverlay()
        currentPackage = p
        lastTick = System.currentTimeMillis()
        carryMs = 0L
    }

    private fun homeFromOverlay() {
        hideOverlay()
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    private fun startLockActivity(pkg: String) {
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
