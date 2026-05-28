package com.yuntong.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.yuntong.vpn.R
import com.yuntong.vpn.model.ConnectionState
import com.yuntong.vpn.model.VpnProfile
import com.yuntong.vpn.ui.MainActivity
import com.yuntong.vpn.util.XrayConfigGenerator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class XrayVpnService : VpnService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayCore: Long = 0

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _currentProfile = MutableStateFlow<VpnProfile?>(null)
    val currentProfile: StateFlow<VpnProfile?> = _currentProfile.asStateFlow()

    companion object {
        const val CHANNEL_ID = "yuntong_vpn_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_CONNECT = "com.yuntong.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.yuntong.vpn.DISCONNECT"
        const val EXTRA_PROFILE_JSON = "profile_json"

        // Singleton access for UI to observe state
        var instance: XrayVpnService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onDestroy() {
        scope.cancel()
        disconnect()
        instance = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profileJson = intent.getStringExtra(EXTRA_PROFILE_JSON) ?: return START_NOT_STICKY
                val profile = XrayConfigGenerator.profileFromJson(profileJson)
                scope.launch { connect(profile) }
            }
            ACTION_DISCONNECT -> {
                scope.launch { disconnect() }
            }
        }
        return START_STICKY
    }

    private suspend fun connect(profile: VpnProfile) {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED) return

        _connectionState.value = ConnectionState.CONNECTING
        _currentProfile.value = profile

        try {
            // Generate Xray config from profile
            val config = XrayConfigGenerator.generate(profile)

            // Setup VPN interface
            vpnInterface = Builder()
                .setSession("YunTong VPN")
                .setMtu(1500)
                .addAddress("172.19.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .addDisallowedApplication(packageName)
                .establish()

            if (vpnInterface == null) {
                _connectionState.value = ConnectionState.ERROR
                return
            }

            // Start Xray core
            startXrayCore(config)

            // Start foreground notification
            startForeground(NOTIFICATION_ID, buildNotification(profile.name))

            _connectionState.value = ConnectionState.CONNECTED

        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            disconnect()
        }
    }

    private suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTING
        try {
            stopXrayCore()
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}
        _connectionState.value = ConnectionState.DISCONNECTED
        _currentProfile.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startXrayCore(config: String) {
        // libxray JNI interface
        xrayCore = nativeStartXray(config, vpnInterface!!.fd.toLong())
    }

    private fun stopXrayCore() {
        if (xrayCore != 0L) {
            nativeStopXray(xrayCore)
            xrayCore = 0
        }
    }

    // JNI native methods - implemented in libxray.so
    private external fun nativeStartXray(config: String, vpnFd: Long): Long
    private external fun nativeStopXray(core: Long)
    private external fun nativeGetXrayVersion(): String

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "YunTong VPN",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "VPN connection status"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(serverName: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, XrayVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPending = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("YunTong VPN")
            .setContentText("Connected to $serverName")
            .setSmallIcon(R.drawable.ic_vpn_connected)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_disconnect, "Disconnect", disconnectPending)
            .setOngoing(true)
            .build()
    }

    override fun onRevoke() {
        scope.launch { disconnect() }
    }
}
