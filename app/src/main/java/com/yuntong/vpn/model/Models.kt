package com.yuntong.vpn.model

import com.google.gson.annotations.SerializedName

// ========== V2Board API Models ==========

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val data: AuthData?
) {
    data class AuthData(
        val token: String,
        val auth_data: String? = null
    )
}

data class UserInfo(
    val id: Int,
    val email: String,
    val u: Long,
    val d: Long,
    @SerializedName("transfer_enable") val transferEnable: Long,
    @SerializedName("expired_at") val expiredAt: Long?,
    val balance: Float,
    @SerializedName("commission_balance") val commissionBalance: Float,
    val avatar_url: String?,
    val plan_id: Int?,
    val group_id: Int?
) {
    val usedBytes: Long get() = u + d
    val remainingBytes: Long get() = transferEnable - usedBytes
    val usedGB: Double get() = usedBytes / 1073741824.0
    val totalGB: Double get() = transferEnable / 1073741824.0
    val remainingGB: Double get() = remainingBytes / 1073741824.0
    val usagePercent: Int get() = if (transferEnable > 0) ((usedBytes * 100) / transferEnable).toInt() else 0
    val isExpired: Boolean get() = expiredAt != null && expiredAt < System.currentTimeMillis() / 1000
}

data class Plan(
    val id: Int,
    val name: String,
    @SerializedName("transfer_enable") val transferEnable: Long,
    val price: Float?,
    @SerializedName("month_price") val monthPrice: Float?,
    @SerializedName("quarter_price") val quarterPrice: Float?,
    @SerializedName("half_year_price") val halfYearPrice: Float?,
    @SerializedName("year_price") val yearPrice: Float?,
    val content: String?
)

data class Server(
    val id: Int,
    val name: String,
    val type: String, // shadowsocksr, anytls, vless, hysteria2
    val host: String,
    val port: Int,
    @SerializedName("server_port") val serverPort: Int?,
    val tags: List<String>?,
    val show: Int,
    val sort: Int
)

data class SubscriptionInfo(
    val subscribe_url: String,
    val token: String
)

// ========== VPN Config Models ==========

data class VpnProfile(
    val id: Long = 0,
    val name: String,
    val type: String, // anytls, vless, hysteria2, shadowsocksr
    val host: String,
    val port: Int,
    val serverPort: Int = port,
    val uuid: String = "",
    val encryption: String = "",
    val network: String = "tcp",
    val tls: Int = 0,
    val sni: String = "",
    val path: String = "",
    val headers: Map<String, String> = emptyMap(),
    val cipher: String = "",
    val protocol: String = "",
    val protocolParam: String = "",
    val obfs: String = "",
    val obfsParam: String = "",
    val groupName: String = "",
    val groupId: String = "",
    val isActive: Boolean = false,
    val latency: Int = -1, // ms, -1 = not tested
    val uploadSpeed: Long = 0,
    val downloadSpeed: Long = 0
)

// ========== Connection State ==========

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

data class ConnectionStats(
    val uploadSpeed: Long = 0,     // bytes/s
    val downloadSpeed: Long = 0,   // bytes/s
    val totalUpload: Long = 0,     // bytes
    val totalDownload: Long = 0,   // bytes
    val duration: Long = 0,        // seconds
    val connectedServer: VpnProfile? = null
)

// ========== App State ==========

data class AppState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val connectionStats: ConnectionStats = ConnectionStats(),
    val currentServer: VpnProfile? = null,
    val serverList: List<VpnProfile> = emptyList(),
    val userInfo: UserInfo? = null,
    val isLoggedIn: Boolean = false,
    val isSubscribed: Boolean = false,
    val error: String? = null
)
