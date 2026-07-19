package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.WorkflowViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Room database, Repository and ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(database.appDao())
        val viewModel = WorkflowViewModel(application)

        setContent {
            MyApplicationTheme {
                var activeTab by remember { mutableStateOf(0) } // 0: Editor, 1: Runner, 2: Workspace, 3: Library, 4: Browser, 5: Settings

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom bar only for primary views (0 to 4)
                        if (activeTab in 0..4) {
                            NavigationBar(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF00E5FF)
                            ) {
                                NavigationBarItem(
                                    selected = activeTab == 0,
                                    onClick = { activeTab = 0 },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "المحرر", modifier = Modifier.size(20.dp)) },
                                    label = { Text("المحرر", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00E5FF),
                                        unselectedIconColor = Color.Gray,
                                        selectedTextColor = Color(0xFF00E5FF),
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF0F172A)
                                    )
                                )

                                NavigationBarItem(
                                    selected = activeTab == 1,
                                    onClick = { activeTab = 1 },
                                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = "التشغيل", modifier = Modifier.size(20.dp)) },
                                    label = { Text("التشغيل", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00E5FF),
                                        unselectedIconColor = Color.Gray,
                                        selectedTextColor = Color(0xFF00E5FF),
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF0F172A)
                                    )
                                )

                                NavigationBarItem(
                                    selected = activeTab == 2,
                                    onClick = { activeTab = 2 },
                                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = "الملفات", modifier = Modifier.size(20.dp)) },
                                    label = { Text("الملفات", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00E5FF),
                                        unselectedIconColor = Color.Gray,
                                        selectedTextColor = Color(0xFF00E5FF),
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF0F172A)
                                    )
                                )

                                NavigationBarItem(
                                    selected = activeTab == 3,
                                    onClick = { activeTab = 3 },
                                    icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "المكتبة", modifier = Modifier.size(20.dp)) },
                                    label = { Text("المكتبة", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00E5FF),
                                        unselectedIconColor = Color.Gray,
                                        selectedTextColor = Color(0xFF00E5FF),
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF0F172A)
                                    )
                                )

                                NavigationBarItem(
                                    selected = activeTab == 4,
                                    onClick = { activeTab = 4 },
                                    icon = { Icon(Icons.Default.Language, contentDescription = "المتصفح", modifier = Modifier.size(20.dp)) },
                                    label = { Text("المتصفح", fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00E5FF),
                                        unselectedIconColor = Color.Gray,
                                        selectedTextColor = Color(0xFF00E5FF),
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF0F172A)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activeTab) {
                            0 -> WorkflowEditorScreen(
                                viewModel = viewModel,
                                onNavigateToRunner = { activeTab = 1 },
                                onNavigateToLibrary = { activeTab = 3 }
                            )
                            1 -> RunnerScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { activeTab = 5 },
                                onNavigateToEditor = { activeTab = 0 }
                            )
                            2 -> WorkspaceScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { activeTab = 0 }
                            )
                            3 -> LibrariesScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { activeTab = 0 }
                            )
                            4 -> BrowserScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { activeTab = 0 }
                            )
                            5 -> SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

