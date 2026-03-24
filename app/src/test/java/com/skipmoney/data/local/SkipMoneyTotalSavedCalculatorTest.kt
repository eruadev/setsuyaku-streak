package com.skipmoney.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class SkipMoneyTotalSavedCalculatorTest {

    @Test
    fun checkedAddTotalSaved_returnsSummedValue_whenWithinRange() {
        val result = checkedAddTotalSaved(
            currentTotalSaved = 1_000L,
            amountToAdd = 2_500L,
        )

        assertEquals(3_500L, result)
    }

    @Test
    fun checkedAddTotalSaved_returnsLongMaxValue_atBoundary() {
        val result = checkedAddTotalSaved(
            currentTotalSaved = Long.MAX_VALUE - 1L,
            amountToAdd = 1L,
        )

        assertEquals(Long.MAX_VALUE, result)
    }

    @Test(expected = TotalSavedOverflowException::class)
    fun checkedAddTotalSaved_throws_whenSumExceedsLongMaxValue() {
        checkedAddTotalSaved(
            currentTotalSaved = Long.MAX_VALUE,
            amountToAdd = 1L,
        )
    }
}
