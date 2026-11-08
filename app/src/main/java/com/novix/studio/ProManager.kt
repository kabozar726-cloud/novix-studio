package com.novix.studio

import android.content.Context
import android.content.SharedPreferences

object ProManager {
    const val UNLOCK_CODE = "NOVIX2026"
    private const val PREFS = "novix_prefs"
    private const val KEY_PRO = "pro_unlocked"

    fun isPro(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PRO, false)
    }

    fun unlock(context: Context, code: String): Boolean {
        val ok = code.trim().equals(UNLOCK_CODE, ignoreCase = true)
        if (ok) prefs(context).edit().putBoolean(KEY_PRO, true).apply()
        return ok
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}
