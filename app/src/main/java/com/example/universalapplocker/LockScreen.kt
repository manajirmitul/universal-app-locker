package com.example.universalapplocker

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

/**
 * লক স্ক্রিনের ভিউ। LockActivity এবং Accessibility ওভারলে, দুই জায়গাতেই এটা ব্যবহার হয়।
 */
class LockScreen(
    private val ctx: Context,
    private val store: Store,
    private val pkg: String,
    private val onUnlocked: () -> Unit,
    private val onHome: () -> Unit
) {
    val root: ScrollView = ScrollView(ctx)

    init {
        root.isFillViewport = true
        root.setBackgroundColor(Ui.BG)
        Ui.applyInsets(root)
        showPin()
    }

    private fun dp(v: Int): Int = Ui.dp(ctx, v)

    private fun appLabel(): String {
        return try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun appIcon(): Drawable? {
        return try {
            ctx.packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }
    }

    private fun setContent(content: LinearLayout) {
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER
        content.setPadding(dp(24), dp(24), dp(24), dp(24))
        root.removeAllViews()
        root.addView(content)
    }

    private fun header(content: LinearLayout, title: String, sub: String) {
        val icon = ImageView(ctx)
        icon.setImageDrawable(appIcon())
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        content.addView(icon, LinearLayout.LayoutParams(dp(72), dp(72)))

        val t = Ui.tv(ctx, title, 26f, Ui.TEXT, true)
        t.gravity = Gravity.CENTER
        val tlp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tlp.topMargin = dp(14)
        content.addView(t, tlp)

        val s = Ui.tv(ctx, sub, 14f, Ui.SUB)
        s.gravity = Gravity.CENTER
        val slp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        slp.topMargin = dp(6)
        slp.bottomMargin = dp(18)
        content.addView(s, slp)
    }

    private fun showPin() {
        val content = LinearLayout(ctx)
        val limit = store.getLimit(pkg)
        header(
            content,
            ctx.getString(R.string.lock_title),
            ctx.getString(R.string.lock_sub, appLabel(), Ui.fmtMin(ctx, limit.toLong()))
        )

        if (store.hasPin()) {
            val pad = PinPadView(ctx)
            pad.showInfo(ctx.getString(R.string.lock_pin_info))
            pad.onSubmit = { entered ->
                if (store.tryPin(entered)) {
                    showOptions()
                } else {
                    pad.clear()
                    pad.showError(Ui.pinFailMessage(ctx, store))
                }
            }
            content.addView(pad)
        } else {
            val note = Ui.tv(ctx, ctx.getString(R.string.lock_no_pin), 14f, Ui.TEXT)
            note.gravity = Gravity.CENTER
            content.addView(note)
        }

        val home = Ui.btn(ctx, ctx.getString(R.string.go_home), false) { onHome() }
        val hlp = LinearLayout.LayoutParams(dp(290), ViewGroup.LayoutParams.WRAP_CONTENT)
        hlp.topMargin = dp(14)
        content.addView(home, hlp)

        setContent(content)
    }

    private fun showOptions() {
        val content = LinearLayout(ctx)
        header(
            content,
            ctx.getString(R.string.unlocked_title),
            ctx.getString(R.string.unlocked_sub, appLabel())
        )

        val minutesList = listOf(5, 15, 30)
        for (m in minutesList) {
            val b = Ui.btn(ctx, ctx.getString(R.string.plus_minutes, m), false) {
                store.grantMore(pkg, m * 60L)
                Toast.makeText(ctx, ctx.getString(R.string.granted_toast), Toast.LENGTH_SHORT).show()
                onUnlocked()
            }
            val lp = LinearLayout.LayoutParams(dp(290), ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10)
            content.addView(b, lp)
        }

        val all = Ui.btn(ctx, ctx.getString(R.string.unlock_today), true) {
            store.setFree(pkg)
            Toast.makeText(ctx, ctx.getString(R.string.unlock_today_toast), Toast.LENGTH_SHORT).show()
            onUnlocked()
        }
        val alp = LinearLayout.LayoutParams(dp(290), ViewGroup.LayoutParams.WRAP_CONTENT)
        alp.topMargin = dp(10)
        content.addView(all, alp)

        val home = Ui.btn(ctx, ctx.getString(R.string.skip_home), false, true) { onHome() }
        val hlp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        hlp.topMargin = dp(16)
        content.addView(home, hlp)

        setContent(content)
    }
}
