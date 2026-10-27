package com.example.universalapplocker

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class LockActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("locker", Context.MODE_PRIVATE) }
    private var lockedPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        lockedPackage = intent.getStringExtra("locked_package") ?: ""
        showLockUi()
    }

    private fun showLockUi() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
        }
        layout.addView(TextView(this).apply {
            text = "দৈনিক সময় শেষ!"
            textSize = 26f
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = "এই অ্যাপের জন্য আজকের নির্ধারিত সময় শেষ হয়েছে। PIN দিলে অ্যাপটি আজকের জন্য খুলবে।"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 18)
        })
        val pin = EditText(this).apply {
            hint = "PIN লিখো"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        layout.addView(pin)
        layout.addView(Button(this).apply {
            text = "আজকের জন্য আনলক"
            setOnClickListener {
                val savedPin = prefs.getString("pin", null)
                if (savedPin != null && pin.text.toString() == savedPin) {
                    prefs.edit().putBoolean("bypass_${today()}_$lockedPackage", true).apply()
                    Toast.makeText(this@LockActivity, "আজকের জন্য আনলক হয়েছে", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@LockActivity, "PIN ভুল অথবা আগে PIN সেট করা হয়নি", Toast.LENGTH_SHORT).show()
                }
            }
        })
        layout.addView(Button(this).apply {
            text = "হোম স্ক্রিনে যাও"
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
        })
        setContentView(layout)
    }

    private fun today() = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
}
