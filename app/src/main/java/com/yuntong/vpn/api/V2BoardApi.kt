package com.yuntong.vpn.api

import com.yuntong.vpn.model.*
import retrofit2.http.*

interface V2BoardApi {

    @FormUrlEncoded
    @POST("passport/auth/login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): ApiResponse<LoginResponse.AuthData>

    @GET("passport/auth/register")
    suspend fun register(
        @Query("email") email: String,
        @Query("password") password: String,
        @Query("invite_code") inviteCode: String? = null
    ): ApiResponse<LoginResponse.AuthData>

    @GET("user/info")
    suspend fun getUserInfo(): ApiResponse<UserInfo>

    @GET("user/server/fetch")
    suspend fun fetchServers(): ApiResponse<ServerListResponse>

    @GET("user/order/fetch")
    suspend fun fetchOrders(): ApiResponse<List<Any>>

    @GET("user/plan/fetch")
    suspend fun fetchPlans(): ApiResponse<List<Plan>>

    @FormUrlEncoded
    @POST("user/ticket/create")
    suspend fun createTicket(
        @Field("subject") subject: String,
        @Field("level") level: Int = 1,
        @Field("message") message: String
    ): ApiResponse<Any>

    @GET("user/subscribe")
    suspend fun getSubscription(): ApiResponse<SubscriptionInfo>

    companion object {
        const val BASE_URL = "https://yuntong.org/api/v1/client/"
    }
}

// Generic API response wrapper
data class ApiResponse<T>(
    val data: T?,
    val message: String? = null,
    val status: String? = null
) {
    val isSuccess: Boolean get() = status == "success" || data != null
}

// Server list response (v2board returns different structures per protocol)
data class ServerListResponse(
    val shadowsocksr: List<Server>? = null,
    val anytls: List<Server>? = null,
    val vless: List<Server>? = null,
    val hysteria2: List<Server>? = null
) {
    val allServers: List<Server> get() = listOfNotNull(
        shadowsocksr, anytls, vless, hysteria2
    ).flatten()
}
