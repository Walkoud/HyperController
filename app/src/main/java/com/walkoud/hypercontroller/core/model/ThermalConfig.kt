package com.walkoud.hypercontroller.core.model

data class ThermalConfig(
    val id: Int = 0,
    val userId: Int = 0,
    val iecKillBatteryTemp: Int = 0,
    val allowedIECBatteryTemp: Int = 0,
    val iecScreenOffTimePeriod: Int = 0,
    val iecScreenOnTimePeriod: Int = 0,
    val iecChargingTimePeriod: Int = 0,
    val iecPowerKillPolicy: Int = 0,
    val powerSaveMode: Int = 0
)
