package com.gramaurja.app

data class PowerStatus(
    val isOn: Boolean = false,
    val timestamp: Long = 0L,
    val zoneId: String = ""
)