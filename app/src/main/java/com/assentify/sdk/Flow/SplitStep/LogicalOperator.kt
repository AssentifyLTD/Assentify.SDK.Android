package com.assentify.sdk.Flow.SplitStep

enum class LogicalOperator(val value: Int) {
    AND(1),
    OR(2);

    companion object {
        fun from(value: Int?): LogicalOperator =
            if (value == 2) OR else AND // default AND
    }
}

