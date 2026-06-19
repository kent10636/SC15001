package com.walkietalkie.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Group Room Screen (PTT focus)
 *
 * TODO (Phase 3):
 * - Connect to LiveKit Room using token from backend
 * - Observe remote participants
 * - Big press-and-hold button that calls:
 *     room.localParticipant.setMicrophoneEnabled(true)
 *     ... on release: false
 * - Show speaking indicators
 */
@Composable
fun GroupRoomScreen(
    groupName: String = "Demo Group",
    onLeave: () -> Unit = {}
) {
    var isTalking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Group: $groupName", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        // Participants placeholder
        Text("Participants (TODO: LiveKit remoteParticipants)", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))

        // The all-important PTT button
        Button(
            onClick = { /* hold logic in real impl with pointerInput or long-press */ },
            modifier = Modifier.size(180.dp)
        ) {
            Text(if (isTalking) "RELEASE TO STOP" else "HOLD TO TALK (PTT)", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "In final version this will publish your mic only while pressed\n" +
            "using LiveKit: localParticipant.setMicrophoneEnabled(true/false)",
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLeave) {
            Text("Leave Group")
        }
    }
}
