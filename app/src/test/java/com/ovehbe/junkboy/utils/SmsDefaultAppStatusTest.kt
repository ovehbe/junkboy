package com.ovehbe.junkboy.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDefaultAppStatusTest {

    @Test
    fun identifiesDefaultAppFromTelephonyPackage() {
        assertTrue(
            SmsDefaultAppStatus.isDefinitelyDefaultSmsApp(
                currentPackage = "com.ovehbe.junkboy",
                defaultSmsPackage = "com.ovehbe.junkboy",
                roleHeld = false
            )
        )
    }

    @Test
    fun identifiesDefaultAppFromRoleManager() {
        assertTrue(
            SmsDefaultAppStatus.isDefinitelyDefaultSmsApp(
                currentPackage = "com.ovehbe.junkboy",
                defaultSmsPackage = "com.samsung.android.messaging",
                roleHeld = true
            )
        )
    }

    @Test
    fun returnsFalseWhenNeitherSignalMatches() {
        assertFalse(
            SmsDefaultAppStatus.isDefinitelyDefaultSmsApp(
                currentPackage = "com.ovehbe.junkboy",
                defaultSmsPackage = "com.samsung.android.messaging",
                roleHeld = false
            )
        )
    }
}
