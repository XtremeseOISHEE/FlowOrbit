package com.example.floworbit.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Toggles Android's accessibility daltonizer in "simulate monochromacy" mode to make the
 * WHOLE DEVICE render grayscale ("Boring Mode" -- the same underlying setting used by Digital
 * Wellbeing's Bedtime mode). Writing it requires WRITE_SECURE_SETTINGS, which is not grantable
 * through a runtime permission dialog -- it must be granted once per device via
 * `adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS`. If that grant hasn't
 * been done, every call here is a safe no-op instead of a crash.
 *
 * The setting is system-wide and outlives our process, so the value in place before we touch it
 * is persisted and restored exactly -- if the device already had a real color-correction mode
 * enabled for accessibility reasons, we hand it back unchanged rather than just forcing it off.
 *
 * This class only knows the on/off mechanism. Whether a given "off" request should actually turn
 * it off (e.g. a focus session ending shouldn't fight a user who pinned it on manually) is a
 * policy decision made by callers via [isManuallyPinned]/[setManualPin] -- kept separate here.
 */
class BoringModeManager(private val context: Context) {

    companion object {
        private const val TAG = "BoringModeManager"
        private const val PREFS_NAME = "flow_orbit_boring_mode"
        private const val KEY_ACTIVE = "active"
        private const val KEY_MANUAL_PIN = "manual_pin"
        private const val KEY_PRIOR_ENABLED = "prior_enabled"
        private const val KEY_PRIOR_DALTONIZER = "prior_daltonizer"

        private const val SETTING_ENABLED = "accessibility_display_daltonizer_enabled"
        private const val SETTING_DALTONIZER = "accessibility_display_daltonizer"
        private const val DALTONIZER_SIMULATE_MONOCHROMACY = 0

        /**
         * Call once at app startup. If a previous session crashed or was killed mid-focus-session
         * with grayscale still on, this restores the device to whatever it was before we touched it.
         */
        fun recoverIfStuck(context: Context) {
            BoringModeManager(context).let {
                if (it.prefs.getBoolean(KEY_ACTIVE, false)) {
                    Log.w(TAG, "Boring Mode was left active from a previous session -- reverting.")
                    it.setManualPin(false)
                    it.disable()
                }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasAccess(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isActive(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)

    /** True while the user has explicitly pinned Boring Mode on, independent of any focus session. */
    fun isManuallyPinned(): Boolean = prefs.getBoolean(KEY_MANUAL_PIN, false)

    fun setManualPin(pinned: Boolean) {
        prefs.edit().putBoolean(KEY_MANUAL_PIN, pinned).apply()
    }

    fun enable() {
        if (!hasAccess() || isActive()) return

        try {
            val priorEnabled = Settings.Secure.getInt(context.contentResolver, SETTING_ENABLED, 0)
            val priorDaltonizer = Settings.Secure.getInt(context.contentResolver, SETTING_DALTONIZER, 0)

            Settings.Secure.putInt(context.contentResolver, SETTING_DALTONIZER, DALTONIZER_SIMULATE_MONOCHROMACY)
            Settings.Secure.putInt(context.contentResolver, SETTING_ENABLED, 1)

            prefs.edit()
                .putBoolean(KEY_ACTIVE, true)
                .putInt(KEY_PRIOR_ENABLED, priorEnabled)
                .putInt(KEY_PRIOR_DALTONIZER, priorDaltonizer)
                .apply()
        } catch (e: SecurityException) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted on this device; skipping Boring Mode.", e)
        }
    }

    fun disable() {
        if (!isActive()) return

        try {
            if (hasAccess()) {
                val priorEnabled = prefs.getInt(KEY_PRIOR_ENABLED, 0)
                val priorDaltonizer = prefs.getInt(KEY_PRIOR_DALTONIZER, 0)
                Settings.Secure.putInt(context.contentResolver, SETTING_ENABLED, priorEnabled)
                Settings.Secure.putInt(context.contentResolver, SETTING_DALTONIZER, priorDaltonizer)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted on this device; could not revert Boring Mode.", e)
        } finally {
            // Always clear our local "active" flag even if the revert write failed, so we don't
            // get permanently wedged thinking Boring Mode is on when we can no longer control it.
            prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
        }
    }
}
