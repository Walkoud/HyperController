package com.walkoud.hypercontroller.core.model

data class AppRestriction(
    val bgControl: String,
    val bgDelayMin: Int,
    val lastConfigured: Long
)
