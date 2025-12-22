package com.assentify.sdk.Flow.SplitStep

enum class ComparisonOperator(val value: Int) {
    EQUAL(0),
    NOT_EQUAL(1),
    GREATER_THAN(2),
    GREATER_THAN_OR_EQUAL(3),
    LESS_THAN(4),
    LESS_THAN_OR_EQUAL(5),
    CONTAINS(6),
    STARTS_WITH(7),
    ENDS_WITH(8);

    companion object {
        fun from(value: Int): ComparisonOperator =
            entries.first { it.value == value }
    }
}





