package com.womensafety.app.data.model

data class Invitation(
    val id: String = "",
    val invitedBy: String = "",
    val inviteePhone: String = "",
    val status: String = "pending", // pending, accepted, declined
    val createdAt: Long = System.currentTimeMillis()
)
