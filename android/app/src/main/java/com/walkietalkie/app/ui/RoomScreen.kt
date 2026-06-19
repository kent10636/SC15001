package com.walkietalkie.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walkietalkie.app.data.ApiClient
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.launch

/**
 * Core PTT Room - Minimal verifiable demo.
 * Fetches token from backend, connects to LiveKit, controls mic on button hold.
 */
@Composable
fun RoomScreen(
    nickname: String,
    groupCode: String,
    groupName: String,
    onLeave: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isTalking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在连接房间...") }
    var room by remember { mutableStateOf<Room?>(null) }
    val participants = remember { mutableStateListOf(nickname) }

    // Connect to LiveKit when screen enters
    LaunchedEffect(groupCode) {
        try {
            val tokenResp = ApiClient.api.getToken(groupCode, nickname)
            val livekitRoom = LiveKit.create(context)
            livekitRoom.connect(tokenResp.livekitUrl, tokenResp.token)
            room = livekitRoom

            // Listen for participants (basic)
            livekitRoom.remoteParticipants.forEach { p ->
                if (p.identity.value !in participants) participants.add(p.identity.value)
            }

            status = "已连接 • 按住下方按钮说话"
        } catch (e: Exception) {
            status = "连接失败: ${e.message}"
        }
    }

    fun startTalking() {
        room?.localParticipant?.setMicrophoneEnabled(true)
        isTalking = true
        status = "正在说话..."
    }

    fun stopTalking() {
        room?.localParticipant?.setMicrophoneEnabled(false)
        isTalking = false
        status = "已连接 • 松开结束"
    }

    // Clean up on leave
    DisposableEffect(Unit) {
        onDispose {
            room?.disconnect()
            room = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(groupName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("加入码：$groupCode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("当前成员 (${participants.size})", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                participants.forEach { p ->
                    Text("• $p${if (p == nickname) " (你)" else ""}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Real PTT Button
        Box(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.any { it.pressed }
                            if (pressed && !isTalking) {
                                startTalking()
                            } else if (!pressed && isTalking) {
                                stopTalking()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {},
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTalking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isTalking) "松开结束" else "按住说话",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("PTT", fontSize = 16.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(status, style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.weight(1f))

        OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
            Text("离开群组")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "提示：启动后端 + 两台设备用相同码即可真实对讲",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}