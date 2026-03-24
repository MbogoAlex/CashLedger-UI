package com.records.pesa.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import kotlinx.coroutines.flow.first
import java.time.LocalDate
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
                val spending = container.dbRepository
                    .getOutflowForCategory(budget.categoryId, budget.startDate, minOf(today, budget.limitDate))
                    .first()

                val limitReached = spending >= budget.budgetLimit
                val exceededBy = max(0.0, spending - budget.budgetLimit)
                val pct = if (budget.budgetLimit > 0) spending / budget.budgetLimit * 100.0 else 0.0

                // Update DB fields
                container.dbRepository.updateBudgetExpenditure(
                    id = budget.id,
                    expenditure = spending,
                    limitReached = limitReached,
                    exceededBy = exceededBy
                )

                // Budget newly crossed 80% → alert
                if (pct >= 80.0 && !BudgetAlertTracker.hasFired(context, budget.id, "80_recalc")) {
                    NotificationHelper.notify(
                        context = context,
                        id = budget.id * 10 + 4,
                        budgetId = budget.id,
                        title = "⚠️ Budget Alert: ${budget.name}",
                        body = "${budget.name} is ${pct.toInt()}% used — KES ${spending.toLong()} of KES ${budget.budgetLimit.toLong()}."
                    )
                    BudgetAlertTracker.markFired(context, budget.id, "80_recalc")
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
            }
            Log.d("BudgetRecalcWorker", "Recalculated ${budgets.size} budgets")
            Result.success()
        } catch (e: Exception) {
            Log.e("BudgetRecalcWorker", "doWork failed: $e")
            Result.failure()
        }
    }
}
