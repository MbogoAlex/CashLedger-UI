package com.records.pesa.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import com.records.pesa.db.models.BudgetCycleLog
import com.records.pesa.db.models.BudgetRecalcLog
import com.records.pesa.functions.RecurrenceHelper
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

/**
 * Periodically recalculates budget expenditure for all active budgets.
 * Also triggered on-demand after each new SMS transaction is parsed.
 *
 * For recurring budgets: when today passes the limitDate, the completed cycle is
 * archived to BudgetCycleLog, and the budget is reset with new start/end dates.
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
                val categoryId = budget.categoryId ?: continue

                if (budget.isRecurring && !budget.recurrenceType.isNullOrBlank() && today > budget.limitDate) {
                    // ── Cycle rollover for recurring budgets ───────────────────
                    // Archive the completed cycle
                    container.dbRepository.insertBudgetCycleLog(
                        BudgetCycleLog(
                            budgetId = budget.id,
                            budgetName = budget.name,
                            cycleNumber = budget.cycleNumber,
                            cycleStartDate = budget.startDate,
                            cycleEndDate = budget.limitDate,
                            budgetLimit = budget.budgetLimit,
                            finalExpenditure = budget.expenditure,
                            limitReached = budget.limitReached,
                            exceededBy = budget.exceededBy,
                            closedAt = LocalDateTime.now(),
                        )
                    )

                    // Calculate next cycle dates — advance until we reach the current active cycle
                    var nextStart = RecurrenceHelper.nextCycleStartDate(budget.limitDate)
                    var nextEnd = RecurrenceHelper.nextCycleEndDate(
                        nextStart,
                        budget.recurrenceType,
                        budget.recurrenceIntervalDays,
                    )
                    var nextCycleNum = budget.cycleNumber + 1

                    // Keep advancing if we've still passed the end (multiple missed cycles)
                    while (today > nextEnd) {
                        container.dbRepository.insertBudgetCycleLog(
                            BudgetCycleLog(
                                budgetId = budget.id,
                                budgetName = budget.name,
                                cycleNumber = nextCycleNum,
                                cycleStartDate = nextStart,
                                cycleEndDate = nextEnd,
                                budgetLimit = budget.budgetLimit,
                                finalExpenditure = 0.0,
                                limitReached = false,
                                exceededBy = 0.0,
                                closedAt = LocalDateTime.now(),
                            )
                        )
                        nextStart = RecurrenceHelper.nextCycleStartDate(nextEnd)
                        nextEnd = RecurrenceHelper.nextCycleEndDate(
                            nextStart,
                            budget.recurrenceType,
                            budget.recurrenceIntervalDays,
                        )
                        nextCycleNum++
                    }

                    // Reset budget for new cycle
                    container.dbRepository.updateBudget(
                        budget.copy(
                            startDate = nextStart,
                            limitDate = nextEnd,
                            expenditure = 0.0,
                            limitReached = false,
                            limitReachedAt = null,
                            exceededBy = 0.0,
                            cycleNumber = nextCycleNum,
                        )
                    )

                    // Clear alert tracker so the new cycle can fire alerts fresh
                    BudgetAlertTracker.clearForBudget(context, budget.id)

                    // Notify user that the cycle has completed
                    if (!BudgetAlertTracker.hasFired(context, budget.id, "cycle_end")) {
                        val completedCycle = budget.cycleNumber
                        val spent = budget.expenditure.toLong()
                        val limit = budget.budgetLimit.toLong()
                        val summary = if (budget.expenditure >= budget.budgetLimit)
                            "Limit of KES $limit was exceeded — spent KES $spent."
                        else
                            "Spent KES $spent of KES $limit."
                        NotificationHelper.notify(
                            context = context,
                            id = budget.id * 10 + 6,
                            budgetId = budget.id,
                            title = "🔄 Cycle $completedCycle Complete: ${budget.name}",
                            body = "$summary New cycle starts $nextStart."
                        )
                    }

                    Log.d("BudgetRecalcWorker", "Rolled budget ${budget.id} into cycle $nextCycleNum ($nextStart → $nextEnd)")

                    // Immediately recalculate expenditure for the new cycle so the UI shows real figures
                    val memberNamesNew = container.dbRepository.getBudgetMembersOnce(budget.id).map { it.memberName }
                    val mpesaNew = if (memberNamesNew.isEmpty())
                        container.dbRepository.getOutflowForCategory(categoryId, nextStart, minOf(today, nextEnd)).first()
                    else
                        container.dbRepository.getOutflowForCategoryAndMembers(categoryId, nextStart, minOf(today, nextEnd), memberNamesNew)
                    val manualNew = if (memberNamesNew.isEmpty())
                        container.dbRepository.sumManualOutflowForCategoryInPeriod(categoryId, nextStart, minOf(today, nextEnd))
                    else
                        container.dbRepository.sumManualOutflowForCategoryAndMembers(categoryId, nextStart, minOf(today, nextEnd), memberNamesNew)
                    val newCycleExpenditure = mpesaNew + manualNew
                    container.dbRepository.updateBudgetExpenditure(
                        id = budget.id,
                        expenditure = newCycleExpenditure,
                        limitReached = newCycleExpenditure >= budget.budgetLimit,
                        exceededBy = max(0.0, newCycleExpenditure - budget.budgetLimit)
                    )

                } else {
                    // ── Normal expenditure recalculation ──────────────────────
                    val memberNames = container.dbRepository.getBudgetMembersOnce(budget.id).map { it.memberName }

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

                    val thresholdCrossed = when {
                        pct >= 100.0 && !BudgetAlertTracker.hasFired(context, budget.id, "100_recalc") -> "100%"
                        pct >= thresholdPct && !BudgetAlertTracker.hasFired(context, budget.id, "threshold_recalc") -> "${budget.alertThreshold}%"
                        else -> null
                    }

                    container.dbRepository.updateBudgetExpenditure(
                        id = budget.id,
                        expenditure = newExpenditure,
                        limitReached = limitReached,
                        exceededBy = exceededBy
                    )

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
            }
            Log.d("BudgetRecalcWorker", "Recalculated ${budgets.size} budgets")
            Result.success()
        } catch (e: Exception) {
            Log.e("BudgetRecalcWorker", "doWork failed: $e")
            Result.failure()
        }
    }
}
