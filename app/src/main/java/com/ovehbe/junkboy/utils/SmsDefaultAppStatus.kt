package com.ovehbe.junkboy.utils

object SmsDefaultAppStatus {

    fun isDefinitelyDefaultSmsApp(
        currentPackage: String,
        defaultSmsPackage: String?,
        roleHeld: Boolean
    ): Boolean {
        return defaultSmsPackage == currentPackage || roleHeld
    }
}
