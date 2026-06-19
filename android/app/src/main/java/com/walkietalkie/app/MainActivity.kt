package com.walkietalkie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.walkietalkie.app.ui.NicknameScreen
import com.walkietalkie.app.ui.GroupsScreen
import com.walkietalkie.app.ui.RoomScreen
import com.walkietalkie.app.ui.theme.WalkieTalkieTheme

/**
 * Minimal verifiable core demo:
 * Nickname (anonymous) → Create/Join group by 6-char code → Room with real PTT
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WalkieTalkieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WalkieTalkieApp()
                }
            }
        }
    }
}

@Composable
fun WalkieTalkieApp() {
    val navController = rememberNavController()
    var nickname by remember { mutableStateOf("匿名用户") }
    var currentGroupCode by remember { mutableStateOf<String?>(null) }
    var currentGroupName by remember { mutableStateOf<String?>(null) }

    NavHost(navController = navController, startDestination = "nickname") {
        composable("nickname") {
            NicknameScreen(
                initialNickname = nickname,
                onContinue = { newName ->
                    nickname = newName
                    navController.navigate("groups")
                }
            )
        }
        composable("groups") {
            GroupsScreen(
                nickname = nickname,
                onCreateGroup = { code, name ->
                    currentGroupCode = code
                    currentGroupName = name
                    navController.navigate("room")
                },
                onJoinGroup = { code, name ->
                    currentGroupCode = code
                    currentGroupName = name
                    navController.navigate("room")
                }
            )
        }
        composable("room") {
            val code = currentGroupCode
            val name = currentGroupName
            if (code != null && name != null) {
                RoomScreen(
                    nickname = nickname,
                    groupCode = code,
                    groupName = name,
                    onLeave = {
                        currentGroupCode = null
                        currentGroupName = null
                        navController.popBackStack("groups", inclusive = false)
                    }
                )
            } else {
                // Fallback
                GroupsScreen(
                    nickname = nickname,
                    onCreateGroup = { c, n -> /* ignore */ },
                    onJoinGroup = { c, n -> /* ignore */ }
                )
            }
        }
    }
}
