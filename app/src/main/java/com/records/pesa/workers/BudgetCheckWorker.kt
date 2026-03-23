package com.records.pesa.workers

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class BudgetCheckWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CashLedger).container
        val budgets = container.dbRepository.getActiveBudgets().first()

        for (budget in budgets) {
            val today = LocalDate.now()

            // Reset fired alerts if the budget period has expired (new period started)
            if (today > budget.limitDate) {
                BudgetAlertTracker.clearForBudget(context, budget.id)
                continue
            }

            val spending = container.dbRepository
                .getOutflowForCategory(budget.categoryId, budget.startDate, today)
                .first()

            val pct = if (budget.budgetLimit > 0) spending / budget.budgetLimit * 100.0 else 0.0

            // 70% threshold (index 0)
            if (pct >= 70.0 && !BudgetAlertTracker.hasFired(context, budget.id, "70")) {
                NotificationHelper.notify(
                    context = context,
                    id = budget.id * 10 + 0,
                    title = "Heads up: ${budget.name}",
                    body = "You've used 70% of your budget. KES ${spending.toLong()} of KES ${budget.budgetLimit.toLong()} used."
                )
                BudgetAlertTracker.markFired(context, budget.id, "70")
            }

            // 85% threshold (index 1)
            if (pct >= 85.0 && !BudgetAlertTracker.hasFired(context, budget.id, "85")) {
                NotificationHelper.notify(
                    context = context,
                    id = budget.id * 10 + 1,
                    title = "⚠️ ${budget.name} Budget Warning",
                    body = "85% used. Only KES ${(budget.budgetLimit - spending).toLong()} remaining."
                )
                BudgetAlertTracker.markFired(context, budget.id, "85")
            }

            // 100% threshold — exceeded (index 2)
            if (pct >= 100.0 && !BudgetAlertTracker.hasFired(context, budget.id, "100")) {
                NotificationHelper.notify(
                    context = context,
                    id = budget.id * 10 + 2,
                    title = "🚨 ${budget.name} Budget Exceeded",
                    body = "You've exceeded your budget of KES ${budget.budgetLimit.toLong()} by KES ${(spending - budget.budgetLimit).toLong()}."
                )
                BudgetAlertTracker.markFired(context, budget.id, "100")
            }

            // Expiry: budget expires tomorrow (index 3)
            if (budget.limitDate == today.plusDays(1) && !BudgetAlertTracker.hasFired(context, budget.id, "expiry")) {
                NotificationHelper.notify(
                    context = context,
                    id = budget.id * 10 + 3,
                    title = "📅 ${budget.name} Expires Tomorrow",
                    body = "KES ${(budget.budgetLimit - spending).toLong()} remaining before your budget closes."
                )
                BudgetAlertTracker.markFired(context, budget.id, "expiry")
            }
        }

        return Result.success()
    }
}
