package com.example.universalapplocker

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** ডট দেখানো PIN ডায়াল-প্যাড (৪ থেকে ৮ অঙ্ক)। */
class PinPadView(context: Context) : LinearLayout(context) {
    var onSubmit: ((String) -> Unit)? = null

    private val pin = StringBuilder()
    private val dots: TextView = Ui.tv(context, "", 30f, Ui.TEXT, true)
    private val msg: TextView = Ui.tv(context, "", 13f, Ui.SUB)
    private val maxLen = 8
    private val minLen = 4

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        dots.gravity = Gravity.CENTER
        dots.letterSpacing = 0.15f
        msg.gravity = Gravity.CENTER
        updateDots()

        addView(
            dots,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        val msgLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        msgLp.topMargin = Ui.dp(context, 4)
        msgLp.bottomMargin = Ui.dp(context, 10)
        addView(msg, msgLp)

        val grid = LinearLayout(context)
        grid.orientation = LinearLayout.VERTICAL
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("←", "0", "OK")
        )
        val m = Ui.dp(context, 5)
        for (r in rows) {
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            for (label in r) {
                val k = key(label)
                val lp = LinearLayout.LayoutParams(0, Ui.dp(context, 58), 1f)
                lp.setMargins(m, m, m, m)
                row.addView(k, lp)
            }
            grid.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        addView(
            grid,
            LinearLayout.LayoutParams(Ui.dp(context, 290), ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    private fun key(label: String): TextView {
        val isOk = label == "OK"
        val t = Ui.tv(context, label, if (isOk) 17f else 22f, Ui.TEXT, true)
        t.gravity = Gravity.CENTER
        val base = if (isOk) Ui.rounded(context, Ui.ACCENT_DARK, 18) else Ui.rounded(context, Ui.CARD2, 18)
        t.background = Ui.ripple(context, base, 18)
        t.setOnClickListener { press(label) }
        return t
    }

    private fun press(label: String) {
        when (label) {
            "←" -> {
                if (pin.isNotEmpty()) pin.deleteCharAt(pin.length - 1)
                updateDots()
            }
            "OK" -> {
                if (pin.length < minLen) {
                    showError("কমপক্ষে $minLen অঙ্ক দিন")
                } else {
                    onSubmit?.invoke(pin.toString())
                }
            }
            else -> {
                if (pin.length < maxLen) pin.append(label)
                msg.text = ""
                updateDots()
            }
        }
    }

    private fun updateDots() {
        dots.text = if (pin.isEmpty()) {
            "○ ○ ○ ○"
        } else {
            (0 until pin.length).joinToString(" ") { "●" }
        }
    }

    fun clear() {
        pin.setLength(0)
        updateDots()
    }

    fun showError(m: String) {
        msg.text = m
        msg.setTextColor(Ui.BAD)
    }

    fun showInfo(m: String) {
        msg.text = m
        msg.setTextColor(Ui.SUB)
    }
}
