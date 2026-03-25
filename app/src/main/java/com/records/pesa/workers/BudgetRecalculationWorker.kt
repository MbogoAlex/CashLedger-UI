package com.records.pesa.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import com.records.pesa.db.models.BudgetRecalcLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

/**
 * Periodically recalculates budget expenditure for all active budgets.
 * Also triggered on-demand after each new SMS transaction is parsed.
 */
class BudgetRecalculationWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as CashLedger).container
            val budgets = container.dbRepository.getActiveBudgets().first()
            val today = LocalDate.now()

            for (budget in budgets) {
                val memberNames = container.dbRepository.getBudgetMembersOnce(budget.id).map { it.memberName }
                val categoryId = budget.categoryId ?: continue

                val mpesaOutflow = if (memberNames.isEmpty()) {
                    container.dbRepository
                        .getOutflowForCategory(categoryId, budget.startDate, minOf(today, budget.limitDate))
                        .first()
                } else {
                    container.dbRepository.getOutflowForCategoryAndMembers(
                        categoryId, budget.startDate, minOf(today, budget.limitDate), memberNames
                    )
                }
                val manualOutflow = if (memberNames.isEmpty()) {
                    container.dbRepository.sumManualOutflowForCategoryInPeriod(
                        categoryId, budget.startDate, minOf(today, budget.limitDate)
                    )
                } else {
                    container.dbRepository.sumManualOutflowForCategoryAndMembers(
                        categoryId, budget.startDate, minOf(today, budget.limitDate), memberNames
                    )
                }
                val newExpenditure = mpesaOutflow + manualOutflow

                val limitReached = newExpenditure >= budget.budgetLimit
                val exceededBy = max(0.0, newExpenditure - budget.budgetLimit)
                val pct = if (budget.budgetLimit > 0) newExpenditure / budget.budgetLimit * 100.0 else 0.0
                val thresholdPct = budget.alertThreshold.toDouble()

                // Determine threshold crossed BEFORE firing alerts (to capture correctly)
                val thresholdCrossed = when {
                    pct >= 100.0 && !BudgetAlertTracker.hasFired(context, budget.id, "100_recalc") -> "100%"
                    pct >= thresholdPct && !BudgetAlertTracker.hasFired(context, budget.id, "threshold_recalc") -> "${budget.alertThreshold}%"
                    else -> null
                }

                // Update DB fields
                container.dbRepository.updateBudgetExpenditure(
                    id = budget.id,
                    expenditure = newExpenditure,
                    limitReached = limitReached,
                    exceededBy = exceededBy
                )

                // Budget newly crossed custom threshold → alert
                if (pct >= thresholdPct && !BudgetAlertTracker.hasFired(context, budget.id, "threshold_recalc")) {
                    NotificationHelper.notify(
                        context = context,
                        id = budget.id * 10 + 4,
                        budgetId = budget.id,
                        title = "⚠️ Budget Alert: ${budget.name}",
                        body = "${budget.name} is ${pct.toInt()}% used — KES ${newExpenditure.toLong()} of KES ${budget.budgetLimit.toLong()}."
                    )
                    BudgetAlertTracker.markFired(context, budget.id, "threshold_recalc")
                }

                // Budget exceeded 100% → alert
                if (pct >= 100.0 && !BudgetAlertTracker.hasFired(context, budget.id, "100_recalc")) {
                    NotificationHelper.notify(
                        context = context,
                        id = budget.id * 10 + 5,
                        budgetId = budget.id,
                        title = "🔴 Budget Exceeded: ${budget.name}",
                        body = "${budget.name} is over limit by KES ${exceededBy.toLong()}."
                    )
                    BudgetAlertTracker.markFired(context, budget.id, "100_recalc")
                }

                // Insert audit log for every recalculation
                container.dbRepository.insertBudgetRecalcLog(
                    BudgetRecalcLog(
                        budgetId = budget.id,
                        budgetName = budget.name,
                        timestamp = LocalDateTime.now(),
                        oldExpenditure = budget.expenditure,
                        newExpenditure = newExpenditure,
                        thresholdCrossed = thresholdCrossed
                    )
                )
            }
            Log.d("BudgetRecalcWorker", "Recalculated ${budgets.size} budgets")
            Result.success()
        } catch (e: Exception) {
            Log.e("BudgetRecalcWorker", "doWork failed: $e")
            Result.failure()
        }
    }
}
