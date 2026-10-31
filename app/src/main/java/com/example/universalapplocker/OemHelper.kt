package com.example.universalapplocker

import android.os.Build
import java.util.Locale

/** ফোনের ব্র্যান্ড অনুযায়ী সঠিক টিপসের লেখা বেছে দেয়। */
object OemHelper {
    fun tipsRes(): Int {
        val m = Build.MANUFACTURER.lowercase(Locale.US)
        return when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> R.string.tips_xiaomi
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") -> R.string.tips_oppo
            m.contains("vivo") || m.contains("iqoo") -> R.string.tips_vivo
            m.contains("samsung") -> R.string.tips_samsung
            m.contains("huawei") || m.contains("honor") -> R.string.tips_huawei
            m.contains("tecno") || m.contains("infinix") || m.contains("itel") ||
                m.contains("transsion") -> R.string.tips_transsion
            else -> R.string.tips_generic
        }
    }
}
