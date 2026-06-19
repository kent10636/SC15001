package com.walkietalkie.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.walkietalkie.app.data.ApiClient
import kotlinx.coroutines.launch

@Composable
fun GroupsScreen(
    nickname: String,
    onCreateGroup: (code: String, name: String) -> Unit,
    onJoinGroup: (code: String, name: String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf("我的对讲群") }
    var joinCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun createGroup() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val resp = ApiClient.api.createGroup(mapOf("name" to groupName))
                isLoading = false
                onCreateGroup(resp.code, resp.name)
            } catch (e: Exception) {
                isLoading = false
                error = "创建失败: ${e.message}"
            }
        }
    }

    fun joinGroup() {
        if (joinCode.length != 6) {
            error = "请输入6位加入码"
            return
        }
        scope.launch {
            isLoading = true
            error = null
            try {
                val resp = ApiClient.api.joinGroup(mapOf("code" to joinCode.uppercase()))
                isLoading = false
                onJoinGroup(resp.code, resp.name)
            } catch (e: Exception) {
                isLoading = false
                error = "加入失败: ${e.message}（请确认后端已启动）"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("你好，$nickname", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        // Create Group Card (Material You style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("创建新群组", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("群组名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { createGroup() },
                    enabled = !isLoading && groupName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    else Text("创建群组 (自动生成6位码)")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Join Group
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("加入群组", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase().take(6) },
                    label = { Text("6位加入码 (如 AB12CD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { joinGroup() },
                    enabled = !isLoading && joinCode.length == 6,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    else Text("加入群组")
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "使用同一个6位码即可让多台设备进入同一个房间",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}