package com.example.universalapplocker

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.widget.TextView

/** ছোট ছোট UI সহায়ক ফাংশন ও রঙ। */
object Ui {
    val BG: Int = Color.parseColor("#0E1015")
    val CARD: Int = Color.parseColor("#181B22")
    val CARD2: Int = Color.parseColor("#232733")
    val ACCENT: Int = Color.parseColor("#7C6CFF")
    val ACCENT_DARK: Int = Color.parseColor("#5A49E8")
    val OK: Int = Color.parseColor("#34D399")
    val WARN: Int = Color.parseColor("#FBBF24")
    val BAD: Int = Color.parseColor("#F87171")
    val TEXT: Int = Color.parseColor("#F1F3F8")
    val SUB: Int = Color.parseColor("#98A2B3")
    val LINE: Int = Color.parseColor("#2A2F3B")

    fun dp(ctx: Context, v: Int): Int =
        (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    fun rounded(
        ctx: Context, color: Int, radiusDp: Int, strokeColor: Int = 0, strokeDp: Int = 0
    ): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.setColor(color)
        d.cornerRadius = dp(ctx, radiusDp).toFloat()
        if (strokeDp > 0) d.setStroke(dp(ctx, strokeDp), strokeColor)
        return d
    }

    fun gradient(ctx: Context, c1: Int, c2: Int, radiusDp: Int): GradientDrawable {
        val d = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = dp(ctx, radiusDp).toFloat()
        return d
    }

    fun ripple(ctx: Context, content: Drawable, radiusDp: Int): Drawable {
        val mask = rounded(ctx, Color.WHITE, radiusDp)
        return RippleDrawable(
            ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), content, mask
        )
    }

    fun tv(
        ctx: Context, s: String, sp: Float, color: Int = TEXT, bold: Boolean = false
    ): TextView {
        val t = TextView(ctx)
        t.text = s
        t.textSize = sp
        t.setTextColor(color)
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        return t
    }

    fun btn(
        ctx: Context,
        label: String,
        filled: Boolean = true,
        small: Boolean = false,
        onClick: () -> Unit
    ): TextView {
        val t = tv(ctx, label, if (small) 13f else 15f, if (filled) Color.WHITE else ACCENT, true)
        t.gravity = Gravity.CENTER
        val ph = dp(ctx, if (small) 14 else 18)
        val pv = dp(ctx, if (small) 8 else 13)
        t.setPadding(ph, pv, ph, pv)
        val base = if (filled) {
            rounded(ctx, ACCENT_DARK, 14)
        } else {
            rounded(ctx, Color.TRANSPARENT, 14, ACCENT, 1)
        }
        t.background = ripple(ctx, base, 14)
        t.setOnClickListener { onClick() }
        return t
    }

    fun fmtMin(ctx: Context, totalMinutes: Long): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> ctx.getString(R.string.fmt_h_m, h, m)
            h > 0 -> ctx.getString(R.string.fmt_h, h)
            else -> ctx.getString(R.string.fmt_m, m)
        }
    }

    fun pinFailMessage(ctx: Context, store: Store): String {
        val b = store.blockedSeconds()
        return if (b > 0) ctx.getString(R.string.pin_blocked, b) else ctx.getString(R.string.pin_wrong)
    }

    /** স্ট্যাটাস বার / নেভিগেশন বার / কিবোর্ডের জায়গা ছেড়ে কনটেন্ট সাজায়। */
    fun applyInsets(v: View) {
        v.setOnApplyWindowInsetsListener { view, insets ->
            var left = 0
            var top = 0
            var right = 0
            var bottom = 0
            if (Build.VERSION.SDK_INT >= 30) {
                val b = insets.getInsets(
                    WindowInsets.Type.systemBars() or
                        WindowInsets.Type.ime() or
                        WindowInsets.Type.displayCutout()
                )
                left = b.left
                top = b.top
                right = b.right
                bottom = b.bottom
            } else {
                left = insets.systemWindowInsetLeft
                top = insets.systemWindowInsetTop
                right = insets.systemWindowInsetRight
                bottom = insets.systemWindowInsetBottom
            }
            view.setPadding(left, top, right, bottom)
            insets
        }
        v.requestApplyInsets()
    }

    /** মাঝখানে ভাসমান কার্ড-ডায়ালগ। */
    fun sheet(activity: Activity, content: View): Dialog {
        val d = Dialog(activity)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(content)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.setOnShowListener {
            val w = (activity.resources.displayMetrics.widthPixels * 0.92f).toInt()
            d.window?.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return d
    }
}
