package com.records.pesa.workers

import android.content.Context

object BudgetAlertTracker {
    private const val PREFS_NAME = "budget_alert_prefs"
    private const val KEY_FIRED = "fired_alerts"

    fun hasFired(context: Context, budgetId: Int, threshold: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fired = prefs.getStringSet(KEY_FIRED, emptySet()) ?: emptySet()
        return fired.contains("${budgetId}_$threshold")
    }

    fun markFired(context: Context, budgetId: Int, threshold: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fired = prefs.getStringSet(KEY_FIRED, emptySet())?.toMutableSet() ?: mutableSetOf()
        fired.add("${budgetId}_$threshold")
        prefs.edit().putStringSet(KEY_FIRED, fired).apply()
    }

    fun clearForBudget(context: Context, budgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fired = prefs.getStringSet(KEY_FIRED, emptySet())?.toMutableSet() ?: mutableSetOf()
        fired.removeAll { it.startsWith("${budgetId}_") }
        prefs.edit().putStringSet(KEY_FIRED, fired).apply()
    }
}
