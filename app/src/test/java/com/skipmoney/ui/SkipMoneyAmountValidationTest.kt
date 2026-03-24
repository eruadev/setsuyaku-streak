package com.skipmoney.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipMoneyAmountValidationTest {

    @Test
    fun isSavingAmountWithinLimit_returnsTrue_belowLimit() {
        assertTrue(isSavingAmountWithinLimit(MAX_SAVING_AMOUNT - 1L))
    }

    @Test
    fun isSavingAmountWithinLimit_returnsTrue_atLimit() {
        assertTrue(isSavingAmountWithinLimit(MAX_SAVING_AMOUNT))
    }

    @Test
    fun isSavingAmountWithinLimit_returnsFalse_aboveLimit() {
        assertFalse(isSavingAmountWithinLimit(MAX_SAVING_AMOUNT + 1L))
    }
}
