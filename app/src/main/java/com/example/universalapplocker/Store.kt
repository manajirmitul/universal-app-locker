package com.example.universalapplocker

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** সব সেটিংস, দৈনিক ব্যবহার ও PIN এখানে জমা থাকে। */
class Store(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("locker", Context.MODE_PRIVATE)

    init {
        migrateOldPin()
    }

    // ---------- দিন ----------
    fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun startOfDayMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** নতুন দিন শুরু হলে আগের দিনের ব্যবহার, বাড়তি সময় ও আনলক মুছে ফেলে। */
    fun rolloverIfNeeded() {
        val t = today()
        if (prefs.getString("usage_day", "") == t) return
        val editor = prefs.edit()
        for (key in prefs.all.keys) {
            if (key.startsWith("used_") || key.startsWith("extra_") ||
                key.startsWith("free_") || key.startsWith("bypass_")
            ) {
                editor.remove(key)
            }
        }
        editor.putString("usage_day", t).apply()
    }

    // ---------- সীমা ----------
    fun getLimit(pkg: String): Int = prefs.getInt("limit_$pkg", 0)

    fun setLimit(pkg: String, minutes: Int) {
        val e = prefs.edit()
        if (minutes > 0) e.putInt("limit_$pkg", minutes) else e.remove("limit_$pkg")
        e.apply()
    }

    fun limitedCount(): Int = prefs.all.keys.count { it.startsWith("limit_") }

    // ---------- ব্যবহার (সেকেন্ড) ----------
    fun getUsed(pkg: String): Long {
        rolloverIfNeeded()
        return prefs.getLong("used_$pkg", 0L)
    }

    fun setUsed(pkg: String, seconds: Long) {
        rolloverIfNeeded()
        prefs.edit().putLong("used_$pkg", seconds).apply()
    }

    fun addUsed(pkg: String, seconds: Long) {
        setUsed(pkg, getUsed(pkg) + seconds)
    }

    // ---------- বাড়তি সময় / আজকের জন্য আনলক ----------
    fun getExtra(pkg: String): Long {
        rolloverIfNeeded()
        return prefs.getLong("extra_$pkg", 0L)
    }

    private fun setExtra(pkg: String, seconds: Long) {
        rolloverIfNeeded()
        prefs.edit().putLong("extra_$pkg", seconds).apply()
    }

    /** ব্যবহার করা সময়ের পর থেকে আরও [seconds] সেকেন্ড দেয়। */
    fun grantMore(pkg: String, seconds: Long) {
        val limitSec = getLimit(pkg) * 60L
        val base = limitSec + getExtra(pkg)
        val target = maxOf(getUsed(pkg), base) + seconds
        setExtra(pkg, target - limitSec)
    }

    fun isFree(pkg: String): Boolean {
        rolloverIfNeeded()
        return prefs.getBoolean("free_$pkg", false)
    }

    fun setFree(pkg: String) {
        rolloverIfNeeded()
        prefs.edit().putBoolean("free_$pkg", true).apply()
    }

    fun allowedSeconds(pkg: String): Long {
        val limit = getLimit(pkg)
        if (limit <= 0 || isFree(pkg)) return Long.MAX_VALUE
        return limit * 60L + getExtra(pkg)
    }

    fun isOverLimit(pkg: String): Boolean {
        if (getLimit(pkg) <= 0) return false
        return getUsed(pkg) >= allowedSeconds(pkg)
    }

    // ---------- PIN (হ্যাশ করে রাখা হয়) ----------
    fun hasPin(): Boolean = prefs.contains("pin_hash")

    fun setPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltHex = toHex(salt)
        prefs.edit()
            .putString("pin_salt", saltHex)
            .putString("pin_hash", hash(saltHex, pin))
            .remove("pin")
            .apply()
    }

    private fun hash(salt: String, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return toHex(md.digest((salt + pin).toByteArray(Charsets.UTF_8)))
    }

    private fun toHex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }

    private fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString("pin_salt", null) ?: return false
        val saved = prefs.getString("pin_hash", null) ?: return false
        return MessageDigest.isEqual(saved.toByteArray(), hash(salt, pin).toByteArray())
    }

    private fun migrateOldPin() {
        val old = prefs.getString("pin", null) ?: return
        if (!prefs.contains("pin_hash")) setPin(old) else prefs.edit().remove("pin").apply()
    }

    /** পরপর ৫ বার ভুল হলে ৩০ সেকেন্ড অপেক্ষা করতে হয়। */
    fun blockedSeconds(): Long {
        val r = blockedUntil - System.currentTimeMillis()
        return if (r > 0) (r + 999) / 1000 else 0L
    }

    fun tryPin(pin: String): Boolean {
        if (blockedSeconds() > 0) return false
        if (verifyPin(pin)) {
            fails = 0
            return true
        }
        fails++
        if (fails >= 5) {
            blockedUntil = System.currentTimeMillis() + 30_000L
            fails = 0
        }
        return false
    }

    companion object {
        private var fails = 0
        private var blockedUntil = 0L
    }
}

/** ফোনের সেটিংস/অনুমতির অবস্থা যাচাই। */
object Sys {
    fun isA11yEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(ctx, AppMonitorService::class.java)
        for (s in enabled.split(':')) {
            val cn = ComponentName.unflattenFromString(s)
            if (cn != null && cn == me) return true
        }
        return false
    }

    fun hasUsageAccess(ctx: Context): Boolean {
        return try {
            val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                ops.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
                )
            } else {
                ops.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun ignoringBattery(ctx: Context): Boolean {
        return try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(ctx.packageName)
        } catch (e: Exception) {
            false
        }
    }
}
