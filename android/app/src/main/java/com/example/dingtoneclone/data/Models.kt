package com.example.dingtoneclone.data

import kotlinx.serialization.Serializable

// --- API Models ---

@Serializable
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val credits: Int = 0,
    val numbers: List<String> = emptyList(),
    val createdAt: String = ""
)

@Serializable
data class VirtualNumber(
    val sid: String = "",
    val phoneNumber: String = "",
    val friendlyName: String = "",
    val userId: String = "",
    val assignedAt: String = "",
    val active: Boolean = true,
    val smsCount: Int = 0
)

@Serializable
data class SmsMessage(
    val id: String = "",
    val from: String = "",
    val to: String = "",
    val body: String = "",
    val userId: String = "",
    val receivedAt: String = "",
    val read: Boolean = false
)

@Serializable
data class AvailableNumber(
    val phoneNumber: String = "",
    val friendlyName: String = "",
    val region: String = ""
)

// --- API Request Bodies ---

@Serializable
data class RegisterRequest(val displayName: String)

@Serializable
data class BuyNumberRequest(val phoneNumber: String)

@Serializable
data class SendSmsRequest(val to: String, val from: String, val body: String)

@Serializable
data class MakeCallRequest(val to: String, val from: String)

// --- API Response Wrappers ---

@Serializable
data class ProfileResponse(val user: UserProfile)

@Serializable
data class NumbersResponse(val numbers: List<VirtualNumber>)

@Serializable
data class AvailableNumbersResponse(val numbers: List<AvailableNumber>)

@Serializable
data class MessagesResponse(val messages: List<SmsMessage>)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class BuyNumberResponse(val message: String, val number: VirtualNumber)

@Serializable
data class CallResponse(val message: String, val sid: String = "", val status: String = "")

@Serializable
data class VoiceTokenResponse(val token: String, val identity: String)
