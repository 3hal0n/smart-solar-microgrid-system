// ============================================================
// File: OnboardingPreferences.kt
// Purpose: Tiny SharedPreferences wrapper tracking whether the
//          first-run onboarding flow has already been shown, so
//          SplashActivity/OnboardingActivity don't each duplicate
//          the prefs name/key as string literals.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.onboarding

import android.content.Context

private const val PREFS_NAME = "onboarding_prefs"
private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"

object OnboardingPreferences {

    // Returns true once the user has completed (or skipped) onboarding at least once before.
    fun hasSeenOnboarding(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
    }

    // Persists that onboarding has been shown so future launches skip straight past it.
    fun markOnboardingShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, true).apply()
    }
}
