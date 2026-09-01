package com.selfbudget.app

import com.selfbudget.app.core.ui.RetirementAccountIcon
import com.selfbudget.app.core.ui.getAccountIcon
import com.selfbudget.app.data.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AccountIconsTest {
    @Test
    fun testRetirementIconCreation() {
        val retirementIcon = RetirementAccountIcon
        assertNotNull(retirementIcon)
        assertEquals("RetirementAccount", retirementIcon.name)
    }

    @Test
    fun testGetAccountIconForAllAccountTypes() {
        for (type in AccountType.entries) {
            val icon = getAccountIcon(type)
            assertNotNull("Icon for $type should not be null", icon)
            if (type == AccountType.RETIREMENT) {
                assertEquals(RetirementAccountIcon, icon)
            }
        }
    }
}
