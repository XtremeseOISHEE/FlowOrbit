package com.example.floworbit.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Single source of truth for the "Allow 5 min" bypass, shared by both blocking paths
 * (the accessibility-service block screen and the focus-session warning overlay).
 *
 * A grant is not just a 5-minute window -- it also starts a 30-minute cooldown during which
 * the same app can't be granted again, so the bypass can't be tapped repeatedly to keep an
 * app open indefinitely through a session.
 */
object AllowanceManager {

    const val ALLOWANCE_DURATION_MS = 5 * 60 * 1000L
    const val COOLDOWN_DURATION_MS = 30 * 60 * 1000L

    private const val PREF = "floworbit_allowances"
    private const val KEY_ALLOW_PREFIX = "allow_until_"
    private const val KEY_COOLDOWN_PREFIX = "cooldown_until_"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** True while a package can still be granted a fresh 5-minute allowance (not on cooldown). */
    fun canGrantAllowance(context: Context, packageName: String): Boolean {
        val cooldownUntil = prefs(context).getLong(KEY_COOLDOWN_PREFIX + packageName, 0L)
        return System.currentTimeMillis() >= cooldownUntil
    }

    /** Milliseconds until this package can be granted again; 0 if it can be granted now. */
    fun cooldownRemainingMillis(context: Context, packageName: String): Long {
        val cooldownUntil = prefs(context).getLong(KEY_COOLDOWN_PREFIX + packageName, 0L)
        return (cooldownUntil - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * Grants a 5-minute allowance and immediately starts the 30-minute cooldown for the next
     * grant -- the cooldown is set now, not when the allowance expires, so ending the session
     * early via "Go Home" doesn't let someone dodge it.
     */
    fun grantAllowance(context: Context, packageName: String) {
        if (!canGrantAllowance(context, packageName)) {
            Log.w("ALLOW", "Ignored grant for $packageName -- still on cooldown")
            return
        }
        val now = System.currentTimeMillis()
        Log.d("ALLOW", "Granting 5 min for $packageName; cooldown starts now")
        prefs(context).edit()
            .putLong(KEY_ALLOW_PREFIX + packageName, now + ALLOWANCE_DURATION_MS)
            .putLong(KEY_COOLDOWN_PREFIX + packageName, now + ALLOWANCE_DURATION_MS + COOLDOWN_DURATION_MS)
            .apply()
    }

    /** True while an active (already-granted) allowance window is still running. */
    fun isAllowedNow(context: Context, packageName: String): Boolean {
        val until = prefs(context).getLong(KEY_ALLOW_PREFIX + packageName, 0L)
        return System.currentTimeMillis() <= until
    }

    /** Ends an active allowance early. The cooldown already set at grant time is untouched. */
    fun clearAllowance(context: Context, packageName: String) {
        Log.d("ALLOW", "Clearing active allowance for $packageName")
        prefs(context).edit()
            .remove(KEY_ALLOW_PREFIX + packageName)
            .apply()
    }
}
