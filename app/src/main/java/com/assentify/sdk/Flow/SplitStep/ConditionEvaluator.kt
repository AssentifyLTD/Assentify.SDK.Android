package com.assentify.sdk.Flow.SplitStep

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.assentify.sdk.Flow.FlowController.FlowController
import com.assentify.sdk.RemoteClient.Models.Branch
import com.assentify.sdk.RemoteClient.Models.Condition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ConditionEvaluator {

    private const val TAG = "ConditionEvaluator"

    @RequiresApi(Build.VERSION_CODES.O)
    fun evaluateBranch(
        branch: Branch,
    ): Boolean {

        Log.d(TAG, "==================== evaluateBranch START ====================")
        Log.d(TAG, "Branch received: $branch")

        val conditions = branch.conditions
        if (conditions == null) {
            Log.d(TAG, "Branch has no conditions -> returning true")
            Log.d(TAG, "==================== evaluateBranch END ======================")
            return true
        }

        Log.d(TAG, "Conditions count = ${conditions.size}")

        var result: Boolean? = null

        for ((index, condition) in conditions.withIndex()) {
            Log.d(TAG, "-------------------------------------------------------------")
            Log.d(TAG, "Evaluating condition #$index")
            Log.d(TAG, "Condition object = $condition")
            Log.d(TAG, "Condition operator = ${condition.operator}")
            Log.d(TAG, "Condition conditionOperator = ${condition.conditionOperator}")
            Log.d(TAG, "Condition compare value = ${condition.value}")

            var inputValue = ""

            val doneList = FlowController.getAllDoneSteps()
            Log.d(TAG, "Done steps count = ${doneList.size}")

            doneList.forEachIndexed { stepIndex, step ->


                val outputProperties = step.stepDefinition?.customization?.outputProperties.orEmpty()

                for ((outputIndex, outputProperty) in outputProperties.withIndex()) {
                    if (outputProperty.keyIdentifier == condition.inputPropertyKey) {
                        val extractedInfo = step.submitRequestModel?.extractedInformation
                        inputValue = extractedInfo?.get(outputProperty.key)?.toString() ?: ""
                        Log.d(TAG, "${outputProperty.key} = $inputValue")

                    }
                }
            }


            val ruleResult = evaluateCondition(condition, inputValue)
            Log.d(TAG, "ruleResult for condition #$index = $ruleResult")

            val logicalOperator = LogicalOperator.from(condition.conditionOperator)
            Log.d(TAG, "Resolved logical operator = $logicalOperator")
            Log.d(TAG, "Previous accumulated result = $result")

            result = when {
                result == null -> ruleResult
                logicalOperator == LogicalOperator.AND -> result && ruleResult
                else -> result || ruleResult
            }

            Log.d(TAG, "New accumulated result after condition #$index = $result")
        }

        val finalResult = result ?: false
        Log.d(TAG, "Final branch result = $finalResult")
        Log.d(TAG, "==================== evaluateBranch END ======================")

        return finalResult
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun evaluateCondition(condition: Condition, inputValue: String?): Boolean {
        Log.d(TAG, "******************** evaluateCondition START *****************")
        Log.d(TAG, "Condition = $condition")
        Log.d(TAG, "Raw inputValue = '$inputValue'")

        val compareValue = condition.value?.trim()
        Log.d(TAG, "Trimmed compareValue = '$compareValue'")

        if (compareValue == null) {
            Log.d(TAG, "compareValue is null -> returning false")
            Log.d(TAG, "******************** evaluateCondition END *******************")
            return false
        }

        val actualValue = inputValue?.trim()
        Log.d(TAG, "Trimmed actualValue = '$actualValue'")

        if (actualValue == null) {
            Log.d(TAG, "actualValue is null -> returning false")
            Log.d(TAG, "******************** evaluateCondition END *******************")
            return false
        }

        val a = Parsed(ValueKind.TEXT, actualValue)
        val b = Parsed(ValueKind.TEXT, compareValue)

        Log.d(TAG, "Parsed actualValue -> bool=${a.bool}, num=${a.num}, date=${a.date}")
        Log.d(TAG, "Parsed compareValue -> bool=${b.bool}, num=${b.num}, date=${b.date}")

        val op = ComparisonOperator.from(condition.operator)
        Log.d(TAG, "Resolved comparison operator = $op")

        if (op == ComparisonOperator.CONTAINS) {
            val result = actualValue.contains(compareValue, ignoreCase = true)
            Log.d(TAG, "Text CONTAINS result = $result")
            Log.d(TAG, "******************** evaluateCondition END *******************")
            return result
        }

        if (op == ComparisonOperator.STARTS_WITH) {
            val result = actualValue.startsWith(compareValue, ignoreCase = true)
            Log.d(TAG, "Text STARTS_WITH result = $result")
            Log.d(TAG, "******************** evaluateCondition END *******************")
            return result
        }

        if (op == ComparisonOperator.ENDS_WITH) {
            val result = actualValue.endsWith(compareValue, ignoreCase = true)
            Log.d(TAG, "Text ENDS_WITH result = $result")
            Log.d(TAG, "******************** evaluateCondition END *******************")
            return result
        }

        val kind = detectKind(a, b)
        Log.d(TAG, "Detected ValueKind = $kind")

        val result = when (op) {
            ComparisonOperator.EQUAL -> when (kind) {
                ValueKind.BOOLEAN -> {
                    val r = a.bool == b.bool
                    Log.d(TAG, "BOOLEAN EQUAL -> ${a.bool} == ${b.bool} => $r")
                    r
                }
                ValueKind.NUMBER -> {
                    val r = a.num == b.num
                    Log.d(TAG, "NUMBER EQUAL -> ${a.num} == ${b.num} => $r")
                    r
                }
                ValueKind.DATE -> {
                    val r = a.date == b.date
                    Log.d(TAG, "DATE EQUAL -> ${a.date} == ${b.date} => $r")
                    r
                }
                ValueKind.TEXT -> {
                    val r = actualValue.equals(compareValue, ignoreCase = true)
                    Log.d(TAG, "TEXT EQUAL -> '$actualValue' == '$compareValue' => $r")
                    r
                }
            }

            ComparisonOperator.NOT_EQUAL -> when (kind) {
                ValueKind.BOOLEAN -> {
                    val r = a.bool != b.bool
                    Log.d(TAG, "BOOLEAN NOT_EQUAL -> ${a.bool} != ${b.bool} => $r")
                    r
                }
                ValueKind.NUMBER -> {
                    val r = a.num != b.num
                    Log.d(TAG, "NUMBER NOT_EQUAL -> ${a.num} != ${b.num} => $r")
                    r
                }
                ValueKind.DATE -> {
                    val r = a.date != b.date
                    Log.d(TAG, "DATE NOT_EQUAL -> ${a.date} != ${b.date} => $r")
                    r
                }
                ValueKind.TEXT -> {
                    val r = !actualValue.equals(compareValue, ignoreCase = true)
                    Log.d(TAG, "TEXT NOT_EQUAL -> '$actualValue' != '$compareValue' => $r")
                    r
                }
            }

            ComparisonOperator.GREATER_THAN,
            ComparisonOperator.GREATER_THAN_OR_EQUAL,
            ComparisonOperator.LESS_THAN,
            ComparisonOperator.LESS_THAN_OR_EQUAL -> {
                when (kind) {
                    ValueKind.NUMBER -> {
                        val x = a.num
                        val y = b.num
                        Log.d(TAG, "NUMBER comparison values -> x=$x, y=$y")

                        if (x == null || y == null) {
                            Log.d(TAG, "One of number values is null -> returning false")
                            false
                        } else {
                            when (op) {
                                ComparisonOperator.GREATER_THAN -> {
                                    val r = x > y
                                    Log.d(TAG, "NUMBER GREATER_THAN -> $x > $y => $r")
                                    r
                                }
                                ComparisonOperator.GREATER_THAN_OR_EQUAL -> {
                                    val r = x >= y
                                    Log.d(TAG, "NUMBER GREATER_THAN_OR_EQUAL -> $x >= $y => $r")
                                    r
                                }
                                ComparisonOperator.LESS_THAN -> {
                                    val r = x < y
                                    Log.d(TAG, "NUMBER LESS_THAN -> $x < $y => $r")
                                    r
                                }
                                ComparisonOperator.LESS_THAN_OR_EQUAL -> {
                                    val r = x <= y
                                    Log.d(TAG, "NUMBER LESS_THAN_OR_EQUAL -> $x <= $y => $r")
                                    r
                                }
                                else -> false
                            }
                        }
                    }

                    ValueKind.DATE -> {
                        val x = a.date
                        val y = b.date
                        Log.d(TAG, "DATE comparison values -> x=$x, y=$y")

                        if (x == null || y == null) {
                            Log.d(TAG, "One of date values is null -> returning false")
                            false
                        } else {
                            when (op) {
                                ComparisonOperator.GREATER_THAN -> {
                                    val r = x.isAfter(y)
                                    Log.d(TAG, "DATE GREATER_THAN -> $x > $y => $r")
                                    r
                                }
                                ComparisonOperator.GREATER_THAN_OR_EQUAL -> {
                                    val r = x.isAfter(y) || x.isEqual(y)
                                    Log.d(TAG, "DATE GREATER_THAN_OR_EQUAL -> $x >= $y => $r")
                                    r
                                }
                                ComparisonOperator.LESS_THAN -> {
                                    val r = x.isBefore(y)
                                    Log.d(TAG, "DATE LESS_THAN -> $x < $y => $r")
                                    r
                                }
                                ComparisonOperator.LESS_THAN_OR_EQUAL -> {
                                    val r = x.isBefore(y) || x.isEqual(y)
                                    Log.d(TAG, "DATE LESS_THAN_OR_EQUAL -> $x <= $y => $r")
                                    r
                                }
                                else -> false
                            }
                        }
                    }

                    else -> {
                        Log.d(TAG, "Kind is $kind, cannot safely use > < operators -> returning false")
                        false
                    }
                }
            }

            ComparisonOperator.CONTAINS,
            ComparisonOperator.STARTS_WITH,
            ComparisonOperator.ENDS_WITH -> {
                Log.d(TAG, "Operator already handled above -> returning false")
                false
            }
        }

        Log.d(TAG, "evaluateCondition final result = $result")
        Log.d(TAG, "******************** evaluateCondition END *******************")
        return result
    }

    private enum class ValueKind { BOOLEAN, NUMBER, DATE, TEXT }

    private data class Parsed(val kind: ValueKind, val raw: String) {
        val bool: Boolean?
            get() = raw.toBooleanStrictOrNull()

        val num: Double?
            get() = raw.toDoubleOrNull()

        val date: LocalDate?
            @RequiresApi(Build.VERSION_CODES.O)
            get() = parseDateOrNull(raw)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDateOrNull(s: String): LocalDate? {
        val formats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy"
        )

        Log.d(TAG, "Trying to parse date from '$s'")

        for (p in formats) {
            try {
                val parsed = LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern(p))
                Log.d(TAG, "Date parsed successfully with format '$p' -> $parsed")
                return parsed
            } catch (_: DateTimeParseException) {
                Log.d(TAG, "Failed parsing '$s' with format '$p'")
            }
        }

        Log.d(TAG, "Could not parse date from '$s'")
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun detectKind(a: Parsed, b: Parsed): ValueKind {
        Log.d(TAG, "detectKind() -> a.raw='${a.raw}', b.raw='${b.raw}'")
        Log.d(TAG, "a.bool=${a.bool}, b.bool=${b.bool}")
        Log.d(TAG, "a.num=${a.num}, b.num=${b.num}")
        Log.d(TAG, "a.date=${a.date}, b.date=${b.date}")

        if (a.bool != null && b.bool != null) {
            Log.d(TAG, "Detected BOOLEAN")
            return ValueKind.BOOLEAN
        }

        if (a.num != null && b.num != null) {
            Log.d(TAG, "Detected NUMBER")
            return ValueKind.NUMBER
        }

        if (a.date != null && b.date != null) {
            Log.d(TAG, "Detected DATE")
            return ValueKind.DATE
        }

        Log.d(TAG, "Falling back to TEXT")
        return ValueKind.TEXT
    }
}