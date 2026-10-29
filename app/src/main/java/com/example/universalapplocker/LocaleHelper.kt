package com.example.universalapplocker

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** ব্যবহারকারীর বেছে নেওয়া ভাষা মনে রাখে ও অ্যাপে প্রয়োগ করে। "auto" মানে ফোনের ভাষা। */
object LocaleHelper {
    private const val KEY = "lang"

    fun get(ctx: Context): String {
        return ctx.getSharedPreferences("locker", Context.MODE_PRIVATE)
            .getString(KEY, "auto") ?: "auto"
    }

    fun set(ctx: Context, code: String) {
        ctx.getSharedPreferences("locker", Context.MODE_PRIVATE)
            .edit().putString(KEY, code).apply()
    }

    fun wrap(base: Context): Context {
        val lang = get(base)
        if (lang == "auto") {
            Locale.setDefault(base.resources.configuration.locales.get(0))
            return base
        }
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
