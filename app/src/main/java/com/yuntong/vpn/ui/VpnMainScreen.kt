package com.yuntong.vpn.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yuntong.vpn.model.ConnectionState
import com.yuntong.vpn.model.ConnectionStats
import com.yuntong.vpn.model.VpnProfile
import com.yuntong.vpn.service.XrayVpnService
import kotlin.math.cos
import kotlin.math.sin

// ========== Main VPN Screen (KuaiLian style) ==========

@Composable
fun VpnMainScreen(
    viewModel: VpnViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = when (uiState.connectionState) {
                        ConnectionState.CONNECTED -> listOf(
                            Color(0xFF0D1B2A),
                            Color(0xFF1B3A4B),
                            Color(0xFF006D77)
                        )
                        ConnectionState.CONNECTING -> listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            Color(0xFF0F3460)
                        )
                        else -> listOf(
                            Color(0xFF0D1117),
                            Color(0xFF161B22),
                            Color(0xFF21262D)
                        )
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            TopBar(
                userInfo = uiState.userInfo,
                onSettingsClick = { /* TODO: navigate to settings */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Connection status text
            ConnectionStatusText(
                connectionState = uiState.connectionState,
                serverName = uiState.currentServer?.name
            )

            Spacer(modifier = Modifier.weight(0.3f))

            // Big connect button (the hero element)
            ConnectButton(
                connectionState = uiState.connectionState,
                onClick = {
                    when (uiState.connectionState) {
                        ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                            val profile = uiState.currentServer ?: return@ConnectButton
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                (context as? Activity)?.startActivityForResult(intent, 100)
                            } else {
                                viewModel.connect(profile)
                            }
                        }
                        ConnectionState.CONNECTED -> {
                            viewModel.disconnect()
                        }
                        else -> {}
                    }
                }
            )

            Spacer(modifier = Modifier.weight(0.2f))

            // Speed stats
            SpeedStats(
                connectionState = uiState.connectionState,
                stats = uiState.connectionStats
            )

            Spacer(modifier = Modifier.weight(0.3f))

            // Server selection card
            ServerSelectionCard(
                currentServer = uiState.currentServer,
                connectionState = uiState.connectionState,
                onClick = { /* TODO: open server list */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom navigation
            BottomNavBar()
        }
    }
}

// ========== Connect Button (Animated Circle) ==========

@Composable
fun ConnectButton(
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> 1.0f
            ConnectionState.CONNECTING -> 0.95f
            else -> 0.9f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val buttonColor = when (connectionState) {
        ConnectionState.CONNECTED -> Color(0xFF00C853)
        ConnectionState.CONNECTING -> Color(0xFF2979FF)
        ConnectionState.ERROR -> Color(0xFFFF1744)
        else -> Color(0xFF42A5F5)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Outer pulse ring (only when connected)
        if (connectionState == ConnectionState.CONNECTED) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 10f
                drawCircle(
                    color = buttonColor.copy(alpha = pulseAlpha * 0.3f),
                    radius = radius + 10f,
                    center = center
                )
            }
        }

        // Connecting animation ring
        if (connectionState == ConnectionState.CONNECTING) {
            Canvas(modifier = Modifier.size(190.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 12f
                val strokeWidth = 4.dp.toPx()

                // Background circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Animated arc
                val startAngle = rotation
                drawArc(
                    color = buttonColor,
                    startAngle = startAngle,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Main circle button
        Canvas(modifier = Modifier.size(170.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 4f

            // Shadow
            drawCircle(
                color = buttonColor.copy(alpha = 0.3f),
                radius = radius + 8f,
                center = center
            )

            // Main gradient circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        buttonColor.copy(alpha = 0.9f),
                        buttonColor.copy(alpha = 0.7f),
                        buttonColor.copy(alpha = 0.5f)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Inner border
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Power icon
        Icon(
            imageVector = if (connectionState == ConnectionState.CONNECTED)
                Icons.Filled.PowerSettingsNew else Icons.Filled.PowerSettingsNew,
            contentDescription = if (connectionState == ConnectionState.CONNECTED) "Disconnect" else "Connect",
            tint = Color.White,
            modifier = Modifier.size(56.dp)
        )
    }
}

// ========== Connection Status Text ==========

@Composable
fun ConnectionStatusText(
    connectionState: ConnectionState,
    serverName: String?
) {
    val statusText = when (connectionState) {
        ConnectionState.DISCONNECTED -> "Tap to Connect"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.DISCONNECTING -> "Disconnecting..."
        ConnectionState.ERROR -> "Connection Failed"
    }

    val statusColor = when (connectionState) {
        ConnectionState.CONNECTED -> Color(0xFF00E676)
        ConnectionState.CONNECTING -> Color(0xFF82B1FF)
        ConnectionState.ERROR -> Color(0xFFFF5252)
        else -> Color.White.copy(alpha = 0.7f)
    }

    Text(
        text = statusText,
        color = statusColor,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )

    if (serverName != null && connectionState == ConnectionState.CONNECTED) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = serverName,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}

// ========== Speed Stats ==========

@Composable
fun SpeedStats(
    connectionState: ConnectionState,
    stats: ConnectionStats
) {
    val isVisible = connectionState == ConnectionState.CONNECTED

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SpeedItem(
                icon = Icons.Filled.ArrowUpward,
                label = "Upload",
                speed = formatSpeed(stats.uploadSpeed)
            )
            SpeedItem(
                icon = Icons.Filled.ArrowDownward,
                label = "Download",
                speed = formatSpeed(stats.downloadSpeed)
            )
        }
    }
}

@Composable
fun SpeedItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    speed: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF64FFDA),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = speed,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

// ========== Server Selection Card ==========

@Composable
fun ServerSelectionCard(
    currentServer: VpnProfile?,
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag or icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E88E5).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Server",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentServer?.name ?: "Select Server",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (currentServer != null) {
                    Text(
                        text = "${currentServer.host}:${currentServer.port}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "More",
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

// ========== Top Bar ==========

@Composable
fun TopBar(
    userInfo: com.yuntong.vpn.model.UserInfo?,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = "YunTong",
                tint = Color(0xFF42A5F5),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "YunTong VPN",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Right side
        Row {
            // Usage indicator
            if (userInfo != null) {
                Text(
                    text = "${String.format("%.1f", userInfo.usedGB)}/${String.format("%.0f", userInfo.totalGB)} GB",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ========== Bottom Navigation ==========

@Composable
fun BottomNavBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem(Icons.Filled.Home, "Home", true)
        BottomNavItem(Icons.Outlined.Dns, "Servers", false)
        BottomNavItem(Icons.Outlined.Person, "Account", false)
        BottomNavItem(Icons.Outlined.Info, "About", false)
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { /* TODO */ }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF42A5F5) else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = if (selected) Color(0xFF42A5F5) else Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp
        )
    }
}

// ========== Helpers ==========

fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1073741824 -> String.format("%.1f GB/s", bytesPerSec / 1073741824.0)
        bytesPerSec >= 1048576 -> String.format("%.1f MB/s", bytesPerSec / 1048576.0)
        bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
        bytesPerSec > 0 -> "$bytesPerSec B/s"
        else -> "0 B/s"
    }
}
