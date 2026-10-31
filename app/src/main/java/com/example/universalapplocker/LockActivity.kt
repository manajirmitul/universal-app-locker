package com.example.universalapplocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** ওভারলে দেখানো না গেলে বিকল্প হিসেবে এই স্ক্রিন খোলা হয়। */
class LockActivity : Activity() {
    companion object {
        @Volatile
        var visible = false
    }

    private lateinit var store: Store
    private var pkg: String = ""

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.applyTheme(this)
        store = Store(this)
        pkg = intent.getStringExtra("locked_package") ?: ""
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val p = intent.getStringExtra("locked_package") ?: ""
        if (p != pkg) {
            pkg = p
            render()
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

    private fun goHome() {
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_HOME)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
        finish()
    }

    private fun render() {
        val screen = LockScreen(this, store, pkg, { finish() }, { goHome() })
        setContentView(screen.root)
        Ui.styleWindow(this)
    }
}
