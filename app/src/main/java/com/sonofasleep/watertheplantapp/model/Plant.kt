package com.sonofasleep.watertheplantapp.model

data class Plant(
    val image: Int,
    val name: String,
    val wateringType: Int,
    val description: String,
    val notifications: Boolean
)