package com.sonofasleep.watertheplantapp.model

data class Plant(
    val name: String,
    val wateringType: Int,
    val description: String,
    val notifications: Boolean
)