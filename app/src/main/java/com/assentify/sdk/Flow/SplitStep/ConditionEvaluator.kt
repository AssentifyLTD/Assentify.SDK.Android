package com.assentify.sdk.Flow.SplitStep

import android.os.Build
import androidx.annotation.RequiresApi
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.RemoteClient.Models.Branch
import com.assentify.sdk.RemoteClient.Models.Condition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ConditionEvaluator {

    @RequiresApi(Build.VERSION_CODES.O)
    fun evaluateBranch(
        branch: Branch,
    ): Boolean {

        val conditions = branch.conditions
            ?: return true

        var result: Boolean? = null
        for (condition in conditions) {
            var inputValue = "";
            val doneList = FlowController.getAllDoneSteps();
            doneList.forEach { step ->
                for (outputProperty in step.stepDefinition!!.customization.outputProperties) {
                    if (outputProperty.keyIdentifier == condition.inputPropertyKey) {
                        inputValue =
                            step.submitRequestModel!!.extractedInformation.getValue(
                                outputProperty.key
                            )
                    }
                }
            }
            val ruleResult = evaluateCondition(condition, inputValue)

            result = when {
                result == null -> ruleResult
                LogicalOperator.from(condition.conditionOperator) == LogicalOperator.AND ->
                    result && ruleResult

                else ->
                    result || ruleResult
            }
        }

        return result ?: false
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun evaluateCondition(condition: Condition, inputValue: String?): Boolean {
        val compareValue = condition.value?.trim() ?: return false
        val actualValue = inputValue?.trim() ?: return false

        val a = Parsed(ValueKind.TEXT, actualValue)
        val b = Parsed(ValueKind.TEXT, compareValue)

        val op = ComparisonOperator.from(condition.operator)

        // Operators that should always be text-based
        if (op == ComparisonOperator.CONTAINS)
            return actualValue.contains(compareValue, ignoreCase = true)
        if (op == ComparisonOperator.STARTS_WITH)
            return actualValue.startsWith(compareValue, ignoreCase = true)
        if (op == ComparisonOperator.ENDS_WITH)
            return actualValue.endsWith(compareValue, ignoreCase = true)

        val kind = detectKind(a, b)

        return when (op) {
            ComparisonOperator.EQUAL -> when (kind) {
                ValueKind.BOOLEAN -> a.bool == b.bool
                ValueKind.NUMBER -> a.num == b.num
                ValueKind.DATE -> a.date == b.date
                ValueKind.TEXT -> actualValue.equals(compareValue, ignoreCase = true)
            }

            ComparisonOperator.NOT_EQUAL -> when (kind) {
                ValueKind.BOOLEAN -> a.bool != b.bool
                ValueKind.NUMBER -> a.num != b.num
                ValueKind.DATE -> a.date != b.date
                ValueKind.TEXT -> !actualValue.equals(compareValue, ignoreCase = true)
            }

            ComparisonOperator.GREATER_THAN,
            ComparisonOperator.GREATER_THAN_OR_EQUAL,
            ComparisonOperator.LESS_THAN,
            ComparisonOperator.LESS_THAN_OR_EQUAL -> {
                when (kind) {
                    ValueKind.NUMBER -> {
                        val x = a.num ?: return false
                        val y = b.num ?: return false
                        when (op) {
                            ComparisonOperator.GREATER_THAN -> x > y
                            ComparisonOperator.GREATER_THAN_OR_EQUAL -> x >= y
                            ComparisonOperator.LESS_THAN -> x < y
                            ComparisonOperator.LESS_THAN_OR_EQUAL -> x <= y
                            else -> false
                        }
                    }
                    ValueKind.DATE -> {
                        val x = a.date ?: return false
                        val y = b.date ?: return false
                        when (op) {
                            ComparisonOperator.GREATER_THAN -> x.isAfter(y)
                            ComparisonOperator.GREATER_THAN_OR_EQUAL -> x.isAfter(y) || x.isEqual(y)
                            ComparisonOperator.LESS_THAN -> x.isBefore(y)
                            ComparisonOperator.LESS_THAN_OR_EQUAL -> x.isBefore(y) || x.isEqual(y)
                            else -> false
                        }
                    }
                    else -> false // if not number/date, cannot do > < safely
                }
            }

            ComparisonOperator.CONTAINS,
            ComparisonOperator.STARTS_WITH,
            ComparisonOperator.ENDS_WITH -> false // handled above
        }
    }


    /** Dynamic detecting **/
    private enum class ValueKind { BOOLEAN, NUMBER, DATE, TEXT }

    private data class Parsed(val kind: ValueKind, val raw: String) {
        val bool: Boolean? get() = raw.toBooleanStrictOrNull()
        val num: Double? get() = raw.toDoubleOrNull()
        val date: LocalDate? @RequiresApi(Build.VERSION_CODES.O)
        get() = parseDateOrNull(raw)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDateOrNull(s: String): LocalDate? {
        // Add/remove formats as needed
        val formats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy"
        )
        for (p in formats) {
            try {
                return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern(p))
            } catch (_: DateTimeParseException) {}
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun detectKind(a: Parsed, b: Parsed): ValueKind {
        // If both can be parsed as boolean -> boolean
        if (a.bool != null && b.bool != null) return ValueKind.BOOLEAN
        // If both can be parsed as number -> number
        if (a.num != null && b.num != null) return ValueKind.NUMBER
        // If both can be parsed as date -> date
        if (a.date != null && b.date != null) return ValueKind.DATE
        // fallback
        return ValueKind.TEXT
    }
}
