package com.yuntong.vpn.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuntong.vpn.model.ConnectionState
import com.yuntong.vpn.model.VpnProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    servers: List<VpnProfile>,
    currentServer: VpnProfile?,
    connectionState: ConnectionState,
    onServerSelect: (VpnProfile) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text("Servers", color = Color.White, fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Server type filter tabs
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("All", "AnyTLS", "VLESS", "HY2", "SSR")

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) Color(0xFF42A5F5)
                            else Color.White.copy(alpha = 0.5f)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filtered server list
        val filteredServers = when (selectedTab) {
            1 -> servers.filter { it.type == "anytls" }
            2 -> servers.filter { it.type == "vless" }
            3 -> servers.filter { it.type == "hysteria2" }
            4 -> servers.filter { it.type == "shadowsocksr" }
            else -> servers
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filteredServers) { index, server ->
                ServerItem(
                    server = server,
                    isSelected = server.id == currentServer?.id,
                    isConnecting = connectionState == ConnectionState.CONNECTING,
                    onClick = { onServerSelect(server) }
                )
            }
        }
    }
}

@Composable
fun ServerItem(
    server: VpnProfile,
    isSelected: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF42A5F5).copy(alpha = 0.15f)
        else Color.White.copy(alpha = 0.05f),
        label = "bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF42A5F5).copy(alpha = 0.5f)
        else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Protocol badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (server.type) {
                            "anytls" -> Color(0xFF7C4DFF)
                            "vless" -> Color(0xFF00BFA5)
                            "hysteria2" -> Color(0xFFFF6D00)
                            "shadowsocksr" -> Color(0xFF1565C0)
                            else -> Color.Gray
                        }.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (server.type) {
                        "anytls" -> "AT"
                        "vless" -> "VL"
                        "hysteria2" -> "H2"
                        "shadowsocksr" -> "SR"
                        else -> "??"
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${server.host} · ${server.port}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }

            // Latency indicator
            if (server.latency >= 0) {
                val latencyColor = when {
                    server.latency < 100 -> Color(0xFF00E676)
                    server.latency < 200 -> Color(0xFFFFEB3B)
                    else -> Color(0xFFFF5252)
                }
                Text(
                    text = "${server.latency}ms",
                    color = latencyColor,
                    fontSize = 12.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
