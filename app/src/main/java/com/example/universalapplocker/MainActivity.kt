package com.example.universalapplocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

data class AppItem(val label: String, val pkg: String)

class MainActivity : Activity() {
    companion object {
        @Volatile
        var authedUntil = 0L
    }

    private lateinit var store: Store
    private val handler = Handler(Looper.getMainLooper())

    private var apps: List<AppItem> = emptyList()
    private var shown: List<AppItem> = emptyList()
    private var query = ""
    private var mainRoot: View? = null
    private var showingMain = false
    private lateinit var adapter: AppAdapter

    private lateinit var heroCard: LinearLayout
    private lateinit var heroTitle: TextView
    private lateinit var heroSub: TextView
    private lateinit var a11yMark: TextView
    private lateinit var pinMark: TextView
    private lateinit var batMark: TextView
    private lateinit var usageMark: TextView
    private lateinit var pinBtn: TextView
    private lateinit var countText: TextView

    private val refresher = object : Runnable {
        override fun run() {
            if (showingMain) {
                refreshStatus()
                adapter.notifyDataSetChanged()
                handler.postDelayed(this, 5000L)
            }
        }
    }

    private fun dp(v: Int): Int = Ui.dp(this, v)

    private fun lp(w: Int, h: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        p.setMargins(dp(l), dp(t), dp(r), dp(b))
        return p
    }

    // ---------- জীবনচক্র ----------
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        loadAppsAsync()
    }

    override fun onResume() {
        super.onResume()
        val needAuth = store.hasPin() && System.currentTimeMillis() > authedUntil
        if (needAuth) {
            showingMain = false
            handler.removeCallbacksAndMessages(null)
            setContentView(buildGate())
        } else {
            showMain()
        }
    }

    override fun onPause() {
        if (showingMain) authedUntil = System.currentTimeMillis() + 120_000L
        handler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    private fun showMain() {
        showingMain = true
        if (mainRoot == null) mainRoot = buildMain()
        val root = mainRoot!!
        if (root.parent == null) setContentView(root)
        refreshStatus()
        adapter.notifyDataSetChanged()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(refresher, 5000L)
    }

    // ---------- অ্যাপের তালিকা ----------
    private fun loadAppsAsync() {
        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(intent, 0)
            val seen = HashSet<String>()
            val out = ArrayList<AppItem>()
            for (ri in list) {
                val p = ri.activityInfo.packageName
                if (p == packageName || !seen.add(p)) continue
                out.add(AppItem(ri.loadLabel(pm).toString(), p))
            }
            runOnUiThread {
                apps = out
                applyFilter()
            }
        }.start()
    }

    private fun applyFilter() {
        val q = query.trim().lowercase(Locale.getDefault())
        val filtered = apps.filter { q.isEmpty() || it.label.lowercase(Locale.getDefault()).contains(q) }
        shown = filtered.sortedWith(
            compareByDescending<AppItem> { store.getLimit(it.pkg) > 0 }
                .thenBy { it.label.lowercase(Locale.getDefault()) }
        )
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }

    // ---------- PIN গেট ----------
    private fun buildGate(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Ui.BG)
        root.setPadding(dp(24), dp(24), dp(24), dp(24))
        Ui.applyInsets(root)

        val lock = Ui.tv(this, "🔒", 44f)
        lock.gravity = Gravity.CENTER
        root.addView(lock)
        val title = Ui.tv(this, "Universal App Locker", 22f, Ui.TEXT, true)
        title.gravity = Gravity.CENTER
        root.addView(title, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 8))
        val sub = Ui.tv(this, getString(R.string.gate_sub), 14f, Ui.SUB)
        sub.gravity = Gravity.CENTER
        root.addView(sub, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 4, b = 18))

        val pad = PinPadView(this)
        pad.onSubmit = { pin ->
            if (store.tryPin(pin)) {
                authedUntil = System.currentTimeMillis() + 120_000L
                showMain()
            } else {
                pad.clear()
                pad.showError(Ui.pinFailMessage(this, store))
            }
        }
        root.addView(pad)
        return root
    }

    // ---------- মূল স্ক্রিন ----------
    private fun buildMain(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Ui.BG)
        Ui.applyInsets(root)

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(20), dp(16), dp(20), dp(8))
        val titles = LinearLayout(this)
        titles.orientation = LinearLayout.VERTICAL
        titles.addView(Ui.tv(this, "Universal App Locker", 22f, Ui.TEXT, true))
        titles.addView(Ui.tv(this, getString(R.string.main_subtitle), 13f, Ui.SUB))
        top.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val langChip = Ui.tv(this, "🌐 " + getString(R.string.lang_code), 13f, Ui.TEXT, true)
        langChip.setPadding(dp(12), dp(8), dp(12), dp(8))
        langChip.background = Ui.ripple(this, Ui.rounded(this, Ui.CARD2, 14), 14)
        langChip.setOnClickListener { showLanguageDialog() }
        top.addView(langChip, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, l = 10))
        root.addView(top)

        val search = EditText(this)
        search.hint = getString(R.string.search_hint)
        search.setHintTextColor(Ui.SUB)
        search.setTextColor(Ui.TEXT)
        search.textSize = 15f
        search.inputType = InputType.TYPE_CLASS_TEXT
        search.setSingleLine(true)
        search.background = Ui.rounded(this, Ui.CARD, 14)
        search.setPadding(dp(16), dp(12), dp(16), dp(12))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString() ?: ""
                applyFilter()
            }
        })
        root.addView(search, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 16, 4, 16, 8))

        val list = ListView(this)
        list.divider = null
        list.dividerHeight = 0
        list.selector = ColorDrawable(Color.TRANSPARENT)
        list.overScrollMode = View.OVER_SCROLL_NEVER
        list.isVerticalScrollBarEnabled = false
        list.addHeaderView(buildHeader(), null, false)
        adapter = AppAdapter()
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val idx = position - list.headerViewsCount
            if (idx >= 0 && idx < shown.size) showLimitDialog(shown[idx])
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun mark(): TextView {
        val t = Ui.tv(this, "!", 14f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        return t
    }

    private fun setMark(t: TextView, ok: Boolean) {
        t.text = if (ok) "✓" else "!"
        t.background = Ui.rounded(this, if (ok) Ui.OK else Ui.WARN, 13)
        t.setTextColor(if (ok) Color.parseColor("#06251A") else Color.parseColor("#3B2A00"))
    }

    private fun setupRow(mark: TextView, title: String, desc: String, action: View?): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(8), 0, dp(8))
        row.addView(mark, LinearLayout.LayoutParams(dp(26), dp(26)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), 0, dp(8), 0)
        col.addView(Ui.tv(this, title, 14f, Ui.TEXT, true))
        col.addView(Ui.tv(this, desc, 12f, Ui.SUB))
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (action != null) row.addView(action)
        return row
    }

    private fun buildHeader(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(16), dp(4), dp(16), dp(4))

        // স্ট্যাটাস কার্ড
        heroCard = LinearLayout(this)
        heroCard.orientation = LinearLayout.VERTICAL
        heroCard.setPadding(dp(20), dp(18), dp(20), dp(18))
        heroTitle = Ui.tv(this, "", 19f, Color.WHITE, true)
        heroSub = Ui.tv(this, "", 13f, Color.parseColor("#E6E3FF"))
        heroCard.addView(heroTitle)
        heroCard.addView(heroSub, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 4))
        box.addView(heroCard, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 12))

        // সেটআপ কার্ড
        val setup = LinearLayout(this)
        setup.orientation = LinearLayout.VERTICAL
        setup.background = Ui.rounded(this, Ui.CARD, 20)
        setup.setPadding(dp(18), dp(14), dp(18), dp(10))
        setup.addView(Ui.tv(this, getString(R.string.setup_title), 15f, Ui.TEXT, true))

        a11yMark = mark()
        pinMark = mark()
        batMark = mark()
        usageMark = mark()
        pinBtn = Ui.btn(this, getString(R.string.btn_set), true, true) { showPinSetup() }

        setup.addView(
            setupRow(
                a11yMark, getString(R.string.step1_title), getString(R.string.step1_desc),
                Ui.btn(this, getString(R.string.btn_open), true, true) {
                    startSafe(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        )
        setup.addView(setupRow(pinMark, getString(R.string.step2_title), getString(R.string.step2_desc), pinBtn))
        setup.addView(
            setupRow(
                batMark, getString(R.string.step3_title), getString(R.string.step3_desc),
                Ui.btn(this, getString(R.string.btn_open), true, true) { openBattery() }
            )
        )
        setup.addView(
            setupRow(
                usageMark, getString(R.string.step4_title), getString(R.string.step4_desc),
                Ui.btn(this, getString(R.string.btn_open), true, true) {
                    startSafe(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
        )

        val help = Ui.tv(this, getString(R.string.restricted_help), 12f, Ui.SUB)
        setup.addView(help, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 6, b = 8))
        setup.addView(
            Ui.btn(this, getString(R.string.open_app_info), false, true) {
                startSafe(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            },
            lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 6)
        )
        box.addView(setup, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 16))

        // তালিকার শিরোনাম
        val titleRow = LinearLayout(this)
        titleRow.orientation = LinearLayout.HORIZONTAL
        titleRow.gravity = Gravity.CENTER_VERTICAL
        titleRow.addView(
            Ui.tv(this, getString(R.string.list_title), 17f, Ui.TEXT, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        countText = Ui.tv(this, "", 12f, Ui.SUB)
        titleRow.addView(countText)
        box.addView(titleRow, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 4, 0, 4, 4))
        box.addView(
            Ui.tv(this, getString(R.string.list_hint), 12f, Ui.SUB),
            lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, l = 4, b = 4)
        )
        return box
    }

    private fun refreshStatus() {
        if (mainRoot == null) return
        val a11y = Sys.isA11yEnabled(this)
        val count = store.limitedCount()
        if (a11y) {
            heroCard.background = Ui.gradient(this, Ui.ACCENT_DARK, Ui.ACCENT, 22)
            heroTitle.text = getString(R.string.hero_on)
            heroSub.text = if (count > 0) {
                getString(R.string.hero_on_count, count)
            } else {
                getString(R.string.hero_on_none)
            }
        } else {
            heroCard.background = Ui.gradient(this, Color.parseColor("#7F1D1D"), Color.parseColor("#DC2626"), 22)
            heroTitle.text = getString(R.string.hero_off)
            heroSub.text = getString(R.string.hero_off_sub)
        }
        setMark(a11yMark, a11y)
        setMark(pinMark, store.hasPin())
        setMark(batMark, Sys.ignoringBattery(this))
        setMark(usageMark, Sys.hasUsageAccess(this))
        pinBtn.text = if (store.hasPin()) getString(R.string.btn_change) else getString(R.string.btn_set)
        countText.text = if (count > 0) getString(R.string.active_count, count) else ""
    }

    private fun startSafe(i: Intent) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.settings_open_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBattery() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            startSafe(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    // ---------- ভাষা বাছাই ----------
    private fun showLanguageDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = Ui.rounded(this, Ui.CARD, 26)
        box.setPadding(dp(20), dp(20), dp(20), dp(14))
        box.addView(
            Ui.tv(this, getString(R.string.lang_title), 19f, Ui.TEXT, true),
            lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 14)
        )
        val dialog = Ui.sheet(this, box)
        val current = LocaleHelper.get(this)
        val options = listOf(
            Pair("auto", getString(R.string.lang_auto)),
            Pair("en", "English"),
            Pair("bn", "বাংলা")
        )
        for (o in options) {
            val selected = o.first == current
            val label = if (selected) "✓  " + o.second else o.second
            val row = Ui.tv(this, label, 16f, if (selected) Color.WHITE else Ui.SUB, selected)
            row.setPadding(dp(16), dp(14), dp(16), dp(14))
            val bg = Ui.rounded(this, if (selected) Ui.ACCENT_DARK else Ui.CARD2, 14)
            row.background = Ui.ripple(this, bg, 14)
            row.setOnClickListener {
                dialog.dismiss()
                if (o.first != current) {
                    LocaleHelper.set(this, o.first)
                    recreate()
                }
            }
            box.addView(row, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 8))
        }
        dialog.show()
    }

    // ---------- PIN সেট ----------
    private fun showPinSetup() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER_HORIZONTAL
        box.background = Ui.rounded(this, Ui.CARD, 26)
        box.setPadding(dp(18), dp(22), dp(18), dp(18))

        val title = Ui.tv(this, getString(R.string.pin_new_title), 19f, Ui.TEXT, true)
        title.gravity = Gravity.CENTER
        box.addView(title, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val sub = Ui.tv(this, getString(R.string.pin_new_sub), 13f, Ui.SUB)
        sub.gravity = Gravity.CENTER
        box.addView(sub, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 4, b = 14))

        val pad = PinPadView(this)
        box.addView(pad)
        val dialog = Ui.sheet(this, box)
        var first: String? = null
        pad.onSubmit = { pin ->
            val f = first
            if (f == null) {
                first = pin
                title.text = getString(R.string.pin_again_title)
                pad.clear()
                pad.showInfo(getString(R.string.pin_confirm_info))
            } else if (f == pin) {
                store.setPin(pin)
                authedUntil = System.currentTimeMillis() + 120_000L
                dialog.dismiss()
                Toast.makeText(this, getString(R.string.pin_saved), Toast.LENGTH_SHORT).show()
                refreshStatus()
            } else {
                first = null
                title.text = getString(R.string.pin_new_title)
                pad.clear()
                pad.showError(getString(R.string.pin_mismatch))
            }
        }
        dialog.show()
    }

    // ---------- সীমা ঠিক করার ডায়ালগ ----------
    private fun showLimitDialog(item: AppItem) {
        if (!store.hasPin()) {
            Toast.makeText(this, getString(R.string.need_pin_first), Toast.LENGTH_LONG).show()
            showPinSetup()
            return
        }
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.background = Ui.rounded(this, Ui.CARD, 26)
        box.setPadding(dp(20), dp(20), dp(20), dp(18))

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val icon = ImageView(this)
        try {
            icon.setImageDrawable(packageManager.getApplicationIcon(item.pkg))
        } catch (e: Exception) {
        }
        head.addView(icon, lp(dp(44), dp(44), r = 12))
        head.addView(
            Ui.tv(this, item.label, 17f, Ui.TEXT, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        box.addView(head)

        box.addView(
            Ui.tv(this, getString(R.string.limit_label), 13f, Ui.SUB),
            lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 16, b = 6)
        )

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setSingleLine(true)
        input.hint = getString(R.string.limit_hint)
        input.setHintTextColor(Ui.SUB)
        input.setTextColor(Ui.TEXT)
        input.textSize = 20f
        input.background = Ui.rounded(this, Ui.CARD2, 14)
        input.setPadding(dp(16), dp(12), dp(16), dp(12))
        val cur = store.getLimit(item.pkg)
        if (cur > 0) input.setText(cur.toString())
        box.addView(input, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 10))

        val presets = listOf(
            Pair(getString(R.string.preset_off), 0),
            Pair(Ui.fmtMin(this, 15L), 15),
            Pair(Ui.fmtMin(this, 30L), 30),
            Pair(Ui.fmtMin(this, 60L), 60),
            Pair(Ui.fmtMin(this, 120L), 120),
            Pair(Ui.fmtMin(this, 180L), 180)
        )
        val chips = LinearLayout(this)
        chips.orientation = LinearLayout.HORIZONTAL
        for (p in presets) {
            val c = Ui.tv(this, p.first, 13f, Ui.TEXT, true)
            c.setPadding(dp(14), dp(8), dp(14), dp(8))
            c.background = Ui.ripple(this, Ui.rounded(this, Ui.CARD2, 14), 14)
            c.setOnClickListener {
                input.setText(p.second.toString())
                input.setSelection(input.text.length)
            }
            chips.addView(c, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, r = 8))
        }
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.addView(chips)
        box.addView(hs, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, b = 18))

        val dialog = Ui.sheet(this, box)
        val buttons = LinearLayout(this)
        buttons.orientation = LinearLayout.HORIZONTAL
        buttons.addView(
            Ui.btn(this, getString(R.string.btn_cancel), false) { dialog.dismiss() },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        val saveLp = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, l = 10)
        saveLp.weight = 1f
        buttons.addView(
            Ui.btn(this, getString(R.string.btn_save), true) {
                val mins = (input.text.toString().trim().toIntOrNull() ?: 0).coerceIn(0, 1440)
                store.setLimit(item.pkg, mins)
                dialog.dismiss()
                val msg = if (mins > 0) {
                    getString(R.string.limit_set_toast, item.label, Ui.fmtMin(this, mins.toLong()))
                } else {
                    getString(R.string.limit_off_toast, item.label)
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                applyFilter()
                refreshStatus()
            },
            saveLp
        )
        box.addView(buttons)
        dialog.show()
    }

    // ---------- তালিকার অ্যাডাপ্টার ----------
    private inner class AppAdapter : BaseAdapter() {
        override fun getCount(): Int = shown.size
        override fun getItem(position: Int): Any = shown[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row = (convertView as? Row) ?: Row(this@MainActivity)
            row.bind(shown[position])
            return row
        }
    }

    private inner class Row(ctx: Context) : FrameLayout(ctx) {
        private val icon = ImageView(ctx)
        private val name = Ui.tv(ctx, "", 15f, Ui.TEXT, true)
        private val sub = Ui.tv(ctx, "", 12f, Ui.SUB)
        private val chip = Ui.tv(ctx, "", 12f, Ui.TEXT, true)
        private val fill = View(ctx)
        private val rest = View(ctx)
        private val bar = LinearLayout(ctx)

        init {
            setPadding(dp(16), dp(5), dp(16), dp(5))
            val card = LinearLayout(ctx)
            card.orientation = LinearLayout.HORIZONTAL
            card.gravity = Gravity.CENTER_VERTICAL
            card.background = Ui.rounded(ctx, Ui.CARD, 18)
            card.setPadding(dp(14), dp(12), dp(14), dp(12))

            icon.scaleType = ImageView.ScaleType.FIT_CENTER
            card.addView(icon, lp(dp(44), dp(44), r = 12))

            val col = LinearLayout(ctx)
            col.orientation = LinearLayout.VERTICAL
            name.setSingleLine(true)
            name.ellipsize = TextUtils.TruncateAt.END
            sub.setSingleLine(true)
            sub.ellipsize = TextUtils.TruncateAt.END
            col.addView(name)
            col.addView(sub)
            bar.orientation = LinearLayout.HORIZONTAL
            bar.weightSum = 100f
            bar.background = Ui.rounded(ctx, Ui.LINE, 3)
            bar.addView(fill)
            bar.addView(rest)
            col.addView(bar, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(6), t = 7))
            card.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            chip.setPadding(dp(12), dp(6), dp(12), dp(6))
            card.addView(chip, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, l = 10))

            addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        fun bind(item: AppItem) {
            try {
                icon.setImageDrawable(context.packageManager.getApplicationIcon(item.pkg))
            } catch (e: Exception) {
                icon.setImageDrawable(null)
            }
            name.text = item.label
            val limit = store.getLimit(item.pkg)
            if (limit <= 0) {
                sub.text = getString(R.string.row_no_limit)
                sub.setTextColor(Ui.SUB)
                bar.visibility = View.GONE
                chip.text = getString(R.string.row_set)
                chip.setTextColor(Ui.SUB)
                chip.background = Ui.rounded(context, Ui.CARD2, 14)
                return
            }
            val used = store.getUsed(item.pkg)
            val total = limit * 60L + store.getExtra(item.pkg)
            val free = store.isFree(item.pkg)
            val pct = if (free) 0 else minOf(100L, used * 100L / maxOf(1L, total)).toInt()
            val color = when {
                free -> Ui.OK
                pct >= 100 -> Ui.BAD
                pct >= 80 -> Ui.WARN
                else -> Ui.ACCENT
            }
            sub.text = if (free) {
                getString(R.string.row_used_free, Ui.fmtMin(context, used / 60))
            } else {
                getString(R.string.row_used, Ui.fmtMin(context, used / 60), Ui.fmtMin(context, total / 60))
            }
            sub.setTextColor(if (pct >= 100 && !free) Ui.BAD else Ui.SUB)
            bar.visibility = View.VISIBLE
            fill.background = Ui.rounded(context, color, 3)
            fill.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct.toFloat())
            rest.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (100 - pct).toFloat())
            chip.text = Ui.fmtMin(context, limit.toLong())
            chip.setTextColor(Color.WHITE)
            chip.background = Ui.rounded(context, Ui.ACCENT_DARK, 14)
        }
    }
}
