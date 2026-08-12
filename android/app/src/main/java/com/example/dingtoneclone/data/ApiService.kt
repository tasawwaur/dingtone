package com.example.dingtoneclone.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Auth
    @GET("auth/profile")
    suspend fun getProfile(): ProfileResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ProfileResponse

    // Numbers
    @GET("numbers/available")
    suspend fun getAvailableNumbers(@Query("country") country: String = "US"): AvailableNumbersResponse

    @GET("numbers/my")
    suspend fun getMyNumbers(): NumbersResponse

    @POST("numbers/buy")
    suspend fun buyNumber(@Body body: BuyNumberRequest): BuyNumberResponse

    @DELETE("numbers/{sid}")
    suspend fun releaseNumber(@Path("sid") sid: String): MessageResponse

    // SMS
    @GET("sms/inbox")
    suspend fun getInbox(@Query("number") number: String? = null): MessagesResponse

    @POST("sms/send")
    suspend fun sendSms(@Body body: SendSmsRequest): MessageResponse

    // Calls
    @POST("calls/make")
    suspend fun makeCall(@Body body: MakeCallRequest): CallResponse

    @POST("calls/token")
    suspend fun getVoiceToken(): VoiceTokenResponse
}
