package com.example.universalapplocker

import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private val prefs by lazy { getSharedPreferences("locker", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) buildUi()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "Universal App Locker"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "প্রতিটি অ্যাপের দৈনিক সীমা আলাদাভাবে সেট করো। সীমা মিনিটে লিখে Save চাপো।"
            textSize = 16f
            setPadding(0, 12, 0, 16)
        })

        val usageBtn = Button(this).apply {
            text = "১) Usage Access চালু করো"
            setOnClickListener { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        }
        root.addView(usageBtn)
        val accessibilityBtn = Button(this).apply {
            text = "২) Accessibility Service চালু করো"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        root.addView(accessibilityBtn)

        val pinBtn = Button(this).apply {
            text = "PIN সেট / পরিবর্তন"
            setOnClickListener { askForPin() }
        }
        root.addView(pinBtn)

        val status = TextView(this).apply {
            text = "সার্ভিস চালু করতে উপরের দুইটি সেটিংস থেকে এই অ্যাপের অনুমতি দাও।"
            textSize = 14f
            setPadding(0, 8, 0, 12)
        }
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "অ্যাপগুলোর দৈনিক সীমা"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 8)
        })

        val apps = getLaunchableApps()
        if (apps.isEmpty()) {
            root.addView(TextView(this).apply { text = "অ্যাপের তালিকা পাওয়া যায়নি।"; textSize = 16f })
        }
        for (app in apps) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 10)
            }
            row.addView(TextView(this).apply {
                text = app.first
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
            })
            row.addView(TextView(this).apply {
                text = app.second
                textSize = 12f
            })
            val controls = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val limitInput = EditText(this).apply {
                hint = "মিনিট (0=বন্ধ)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setSingleLine(true)
                val saved = prefs.getInt(limitKey(app.second), 0)
                if (saved > 0) setText(saved.toString())
                else setText("")
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            controls.addView(limitInput)
            val save = Button(this).apply {
                text = "Save"
                setOnClickListener {
                    val mins = limitInput.text.toString().toIntOrNull() ?: 0
                    prefs.edit().putInt(limitKey(app.second), mins.coerceAtLeast(0)).apply()
                    Toast.makeText(this@MainActivity,
                        if (mins > 0) "${app.first}: দৈনিক ${mins} মিনিট সেট হয়েছে" else "${app.first}: সীমা বন্ধ",
                        Toast.LENGTH_SHORT).show()
                }
            }
            controls.addView(save)
            row.addView(controls)
            root.addView(row)
            root.addView(View(this).apply {
                setBackgroundColor(0xFFE0E0E0.toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
            })
        }
        root.addView(TextView(this).apply {
            text = "\nব্যবহার: 0 মিনিট = ওই অ্যাপের লক বন্ধ। অন্য অ্যাপের সীমা সেট করতে তালিকায় তার নাম খুঁজে মিনিট লিখে Save করো।"
            textSize = 14f
        })
    }

    private fun getLaunchableApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolved.mapNotNull {
            val pkg = it.activityInfo?.packageName ?: return@mapNotNull null
            val label = it.loadLabel(packageManager)?.toString() ?: pkg
            if (pkg == packageName) null else Pair(label, pkg)
        }.distinctBy { it.second }.sortedBy { it.first.lowercase(Locale.getDefault()) }
    }

    private fun limitKey(pkg: String) = "limit_$pkg"

    private fun askForPin() {
        val input = EditText(this).apply {
            hint = "নতুন PIN (কমপক্ষে 4 অঙ্ক)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("PIN সেট করো")
            .setMessage("PIN ভুলে গেলে অ্যাপের ডেটা মুছলে সেটিংস রিসেট হতে পারে।")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val pin = input.text.toString()
                if (pin.length >= 4) {
                    prefs.edit().putString("pin", pin).apply()
                    Toast.makeText(this, "PIN সেট হয়েছে", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this, "কমপক্ষে ৪ অঙ্কের PIN দাও", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
