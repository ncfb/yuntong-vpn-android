package com.yuntong.vpn.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuntong.vpn.api.V2BoardApi
import com.yuntong.vpn.model.*
import com.yuntong.vpn.service.XrayVpnService
import com.yuntong.vpn.util.XrayConfigGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VpnViewModel @Inject constructor(
    private val api: V2BoardApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private var authToken: String = ""

    init {
        // Observe VPN service state
        viewModelScope.launch {
            XrayVpnService.instance?.connectionState?.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }

    fun connect(profile: VpnProfile) {
        _uiState.update { it.copy(connectionState = ConnectionState.CONNECTING, currentServer = profile) }

        val intent = Intent(XrayConfigGenerator.profileToJson(profile), null)
        // This will be called from UI with proper context
    }

    fun startVpnService(context: android.content.Context, profile: VpnProfile) {
        val intent = Intent(context, XrayVpnService::class.java).apply {
            action = XrayVpnService.ACTION_CONNECT
            putExtra(XrayVpnService.EXTRA_PROFILE_JSON, XrayConfigGenerator.profileToJson(profile))
        }
        context.startService(intent)
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.CONNECTING,
                currentServer = profile
            )
        }
    }

    fun disconnect() {
        // Send disconnect to service
        XrayVpnService.instance?.let { service ->
            val intent = Intent(service, XrayVpnService::class.java).apply {
                action = XrayVpnService.ACTION_DISCONNECT
            }
            service.startService(intent)
        }
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.DISCONNECTED,
                currentServer = null,
                connectionStats = ConnectionStats()
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = api.login(email, password)
                if (response.isSuccess && response.data != null) {
                    authToken = response.data.token
                    _uiState.update { it.copy(isLoggedIn = true) }
                    fetchUserInfo()
                    fetchServers()
                } else {
                    _uiState.update { it.copy(error = response.message ?: "Login failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Network error") }
            }
        }
    }

    private suspend fun fetchUserInfo() {
        try {
            val response = api.getUserInfo()
            if (response.isSuccess) {
                _uiState.update { it.copy(userInfo = response.data) }
            }
        } catch (_: Exception) {}
    }

    private suspend fun fetchServers() {
        try {
            val response = api.fetchServers()
            if (response.isSuccess && response.data != null) {
                val profiles = response.data.allServers.map { server ->
                    VpnProfile(
                        name = server.name,
                        type = server.type,
                        host = server.host,
                        port = server.port,
                        serverPort = server.serverPort ?: server.port
                    )
                }
                _uiState.update {
                    it.copy(
                        serverList = profiles,
                        currentServer = profiles.firstOrNull()
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun selectServer(profile: VpnProfile) {
        _uiState.update { it.copy(currentServer = profile) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
