package com.example.universalapplocker

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class LockActivity : Activity() {
    companion object {
        @Volatile
        var visible = false
    }

    private lateinit var store: Store
    private var pkg: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        pkg = intent.getStringExtra("locked_package") ?: ""
        showPin()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val p = intent.getStringExtra("locked_package") ?: ""
        if (p != pkg) {
            pkg = p
            showPin()
        }
    }

    override fun onResume() {
        super.onResume()
        visible = true
    }

    override fun onPause() {
        visible = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    override fun onBackPressed() {
        goHome()
    }

    private fun dp(v: Int): Int = Ui.dp(this, v)

    private fun goHome() {
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_HOME)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
        finish()
    }

    private fun appLabel(): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun appIcon(): Drawable? {
        return try {
            packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }
    }

    private fun frame(content: LinearLayout): View {
        val sv = ScrollView(this)
        sv.isFillViewport = true
        sv.setBackgroundColor(Ui.BG)
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER
        content.setPadding(dp(24), dp(24), dp(24), dp(24))
        sv.addView(content)
        Ui.applyInsets(sv)
        return sv
    }

    private fun header(content: LinearLayout, title: String, sub: String) {
        val icon = ImageView(this)
        icon.setImageDrawable(appIcon())
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        content.addView(icon, LinearLayout.LayoutParams(dp(72), dp(72)))

        val t = Ui.tv(this, title, 26f, Ui.TEXT, true)
        t.gravity = Gravity.CENTER
        val tlp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tlp.topMargin = dp(14)
        content.addView(t, tlp)

        val s = Ui.tv(this, sub, 14f, Ui.SUB)
        s.gravity = Gravity.CENTER
        val slp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        slp.topMargin = dp(6)
        slp.bottomMargin = dp(18)
        content.addView(s, slp)
    }

    private fun showPin() {
        val content = LinearLayout(this)
        val limit = store.getLimit(pkg)
        header(
            content,
            "সময় শেষ!",
            "${appLabel()} অ্যাপের আজকের সীমা (${Ui.fmtMin(limit.toLong())}) শেষ হয়েছে।"
        )

        if (store.hasPin()) {
            val pad = PinPadView(this)
            pad.showInfo("আরও সময় পেতে PIN দিন")
            pad.onSubmit = { entered ->
                if (store.tryPin(entered)) {
                    showOptions()
                } else {
                    pad.clear()
                    pad.showError(Ui.pinFailMessage(store))
                }
            }
            content.addView(pad)
        } else {
            val note = Ui.tv(
                this,
                "কোনো PIN সেট করা নেই। Universal App Locker খুলে একটি PIN সেট করুন।",
                14f, Ui.WARN
            )
            note.gravity = Gravity.CENTER
            content.addView(note)
        }

        val home = Ui.btn(this, "হোম স্ক্রিনে যান", false) { goHome() }
        val hlp = LinearLayout.LayoutParams(dp(290), ViewGroup.LayoutParams.WRAP_CONTENT)
        hlp.topMargin = dp(14)
        content.addView(home, hlp)

        setContentView(frame(content))
    }

    private fun showOptions() {
        val content = LinearLayout(this)
        header(content, "আনলক হয়েছে", "${appLabel()} কতক্ষণের জন্য খুলতে চান?")

        val options = listOf(
            Pair("আরও ৫ মিনিট", 5),
            Pair("আরও ১৫ মিনিট", 15),
            Pair("আরও ৩০ মিনিট", 30)
        )
        for (o in options) {
            val b = Ui.btn(this, o.first, false) {
                store.grantMore(pkg, o.second * 60L)
                Toast.makeText(this, "${o.first} দেওয়া হয়েছে", Toast.LENGTH_SHORT).show()
                finish()
            }
            val lp = LinearLayout.LayoutParams(dp(290), ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10)
            content.addView(b, lp)
        }

        val all = Ui.btn(this, "আজকের জন্য সম্পূর্ণ আনলক", true) {
            store.setFree(pkg)
            Toast.makeText(this, "আজকের জন্য আনলক হয়েছে", Toast.LENGTH_SHORT).show()
            finish()
        }
        val alp = LinearLayout.LayoutParams(dp(290), ViewGroup.LayoutParams.WRAP_CONTENT)
        alp.topMargin = dp(10)
        content.addView(all, alp)

        val home = Ui.btn(this, "বাদ দিন, হোমে যান", false, true) { goHome() }
        val hlp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        hlp.topMargin = dp(16)
        content.addView(home, hlp)

        setContentView(frame(content))
    }
}
