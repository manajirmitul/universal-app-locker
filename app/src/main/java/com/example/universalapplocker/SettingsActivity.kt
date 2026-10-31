package com.example.universalapplocker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView

/** ভাষা, থিম ও অ্যাপের তথ্য এখানে। */
class SettingsActivity : Activity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.applyTheme(this)
        setContentView(build())
        Ui.styleWindow(this)
    }

    private fun dp(v: Int): Int = Ui.dp(this, v)

    private fun lp(w: Int, h: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        p.setMargins(dp(l), dp(t), dp(r), dp(b))
        return p
    }

    private fun build(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Ui.BG)
        Ui.applyInsets(root)

        // উপরের বার
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(12), dp(14), dp(20), dp(8))
        val back = Ui.tv(this, "←", 24f, Ui.TEXT, true)
        back.gravity = Gravity.CENTER
        back.setPadding(dp(12), dp(4), dp(12), dp(4))
        back.background = Ui.ripple(this, Ui.rounded(this, Color.TRANSPARENT, 14), 14)
        back.setOnClickListener { finish() }
        bar.addView(back)
        bar.addView(
            Ui.tv(this, getString(R.string.settings_title), 22f, Ui.TEXT, true),
            lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, l = 4)
        )
        root.addView(bar)

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(4), dp(16), dp(24))

        val langOptions = listOf(
            Pair("auto", getString(R.string.lang_auto)),
            Pair("en", "English"),
            Pair("bn", "বাংলা")
        )
        content.addView(
            optionCard(getString(R.string.lang_title), langOptions, LocaleHelper.get(this)) { code ->
                LocaleHelper.set(this, code)
                recreate()
            },
            lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 14)
        )

        val themeOptions = listOf(
            Pair("system", getString(R.string.theme_system)),
            Pair("dark", getString(R.string.theme_dark)),
            Pair("light", getString(R.string.theme_light))
        )
        content.addView(
            optionCard(getString(R.string.theme_title), themeOptions, Ui.themeMode(this)) { code ->
                Ui.setThemeMode(this, code)
                recreate()
            },
            lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 14)
        )

        content.addView(
            aboutCard(),
            lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        sv.addView(content)
        root.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun optionCard(
        title: String,
        options: List<Pair<String, String>>,
        current: String,
        onPick: (String) -> Unit
    ): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.rounded(this, Ui.CARD, 20)
        card.setPadding(dp(18), dp(16), dp(18), dp(10))
        card.addView(
            Ui.tv(this, title, 15f, Ui.TEXT, true),
            lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 10)
        )
        for (o in options) {
            val selected = o.first == current
            val label = if (selected) "✓  " + o.second else o.second
            val row = Ui.tv(this, label, 16f, if (selected) Color.WHITE else Ui.TEXT, selected)
            row.setPadding(dp(16), dp(13), dp(16), dp(13))
            val bg = Ui.rounded(this, if (selected) Ui.ACCENT_DARK else Ui.CARD2, 14)
            row.background = Ui.ripple(this, bg, 14)
            row.setOnClickListener {
                if (o.first != current) onPick(o.first)
            }
            card.addView(row, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 8))
        }
        return card
    }

    private fun aboutCard(): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.rounded(this, Ui.CARD, 20)
        card.setPadding(dp(18), dp(16), dp(18), dp(16))
        card.addView(Ui.tv(this, getString(R.string.about_title), 15f, Ui.TEXT, true))
        card.addView(
            Ui.tv(this, "Universal App Locker", 17f, Ui.TEXT, true),
            lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 10)
        )
        val ver = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        card.addView(Ui.tv(this, getString(R.string.about_version, ver), 13f, Ui.SUB))
        card.addView(
            Ui.tv(this, getString(R.string.about_text), 13f, Ui.SUB),
            lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 8)
        )
        return card
    }
}
