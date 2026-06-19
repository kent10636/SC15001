package com.walkietalkie.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun NicknameScreen(
    initialNickname: String,
    onContinue: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialNickname) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "WalkieTalkie",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "最小核心 Demo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("你的昵称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onContinue(name.ifBlank { "匿名用户" }) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("进入群组")
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "匿名使用 • 无需注册",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}