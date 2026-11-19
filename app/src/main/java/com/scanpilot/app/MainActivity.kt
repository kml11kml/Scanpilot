package com.scanpilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scanpilot.app.data.SettingsStore
import com.scanpilot.app.ui.screens.HomeScreen
import com.scanpilot.app.ui.screens.HomeTool
import com.scanpilot.app.ui.screens.LibraryScreen
import com.scanpilot.app.ui.screens.OnboardingScreen
import com.scanpilot.app.ui.screens.ScanScreen
import com.scanpilot.app.ui.screens.SettingsScreen
import com.scanpilot.app.ui.screens.SupportScreen
import com.scanpilot.app.ui.screens.ToolsScreen
import com.scanpilot.app.ui.screens.ViewerScreen
import com.scanpilot.app.ui.theme.ScanPilotTheme
import com.scanpilot.app.viewmodel.LibraryViewModel
import com.scanpilot.app.viewmodel.ScanViewModel
import com.scanpilot.app.viewmodel.SettingsViewModel
import com.scanpilot.app.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val VIEWER = "viewer/{docId}?edit={edit}"
    const val SCAN = "scan"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    const val SUPPORT = "support"

    fun viewer(docId: Long, edit: Boolean = false) = "viewer/$docId?edit=$edit"
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val BOTTOM_TABS = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Default.Home),
    BottomTab(Routes.LIBRARY, "Library", Icons.Default.Folder),
    BottomTab(Routes.TOOLS, "Tools", Icons.Default.Build),
    BottomTab(Routes.SETTINGS, "Settings", Icons.Default.Settings)
)

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private val viewerViewModel: ViewerViewModel by viewModels()
    private val scanViewModel: ScanViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Home screen 'Scan' shortcut — comes from shortcuts.xml
        val openScanFromShortcut = intent?.getBooleanExtra("open_scan", false) == true
        setContent {
            ScanPilotTheme {
                // Left-to-right for the whole English UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        val store = remember { SettingsStore(this) }
                        val onboardingSeen by store.onboardingSeen.collectAsState(initial = null)
                        val isPro by settingsViewModel.isPro.collectAsState()
                        if (onboardingSeen == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val startDestination = when {
                                onboardingSeen != true -> Routes.ONBOARDING
                                openScanFromShortcut -> Routes.SCAN
                                else -> Routes.HOME
                            }
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            val showBottomBar = currentRoute in
                                listOf(Routes.HOME, Routes.LIBRARY, Routes.TOOLS, Routes.SETTINGS)

                            // Routing tools to the right screen
                            val onToolClick: (HomeTool) -> Unit = { tool ->
                                when (tool) {
                                    HomeTool.SCAN -> navController.navigate(Routes.SCAN)
                                    HomeTool.READ -> navController.navigate(Routes.LIBRARY)
                                    HomeTool.EDIT -> navController.navigate(Routes.LIBRARY)
                                    HomeTool.MERGE -> navController.navigate(Routes.LIBRARY)
                                    HomeTool.SPLIT -> navController.navigate(Routes.TOOLS)
                                    HomeTool.IMAGE_TO_PDF -> navController.navigate(Routes.SCAN)
                                    HomeTool.PDF_TO_IMAGE -> navController.navigate(Routes.TOOLS)
                                    HomeTool.LOCK -> navController.navigate(Routes.TOOLS)
                                    HomeTool.SIGN -> navController.navigate(Routes.LIBRARY)
                                    HomeTool.SUMMARY -> navController.navigate(Routes.LIBRARY)
                                }
                            }
                            val navigateToTab: (String) -> Unit = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }

                            Scaffold(
                                floatingActionButton = {
                                    if (showBottomBar) {
                                        // Large Scan button in the middle of the bottom navigation
                                        FloatingActionButton(
                                            onClick = { navigateToTab(Routes.SCAN) },
                                            shape = CircleShape,
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CameraAlt,
                                                contentDescription = "Scan",
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                },
                                bottomBar = {
                                    if (showBottomBar) {
                                        NavigationBar {
                                            // Home, Library | (Scan button slot) | Tools, Settings
                                            NavigationBarItem(
                                                selected = currentRoute == Routes.HOME,
                                                onClick = { navigateToTab(Routes.HOME) },
                                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                                label = { Text("Home") }
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == Routes.LIBRARY,
                                                onClick = { navigateToTab(Routes.LIBRARY) },
                                                icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                                label = { Text("Library") }
                                            )
                                            NavigationBarItem(
                                                selected = false,
                                                onClick = { navigateToTab(Routes.SCAN) },
                                                icon = { Box(modifier = Modifier.size(24.dp)) },
                                                label = { Text("Scan") },
                                                enabled = false
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == Routes.TOOLS,
                                                onClick = { navigateToTab(Routes.TOOLS) },
                                                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                                                label = { Text("Tools") }
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == Routes.SETTINGS,
                                                onClick = { navigateToTab(Routes.SETTINGS) },
                                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                label = { Text("Settings") }
                                            )
                                        }
                                    }
                                }
                            ) { padding ->
                                NavHost(
                                    navController = navController,
                                    startDestination = startDestination,
                                    modifier = Modifier.padding(padding)
                                ) {
                                    composable(Routes.ONBOARDING) {
                                        OnboardingScreen(
                                            onFinish = {
                                                lifecycleScope.launch { store.setOnboardingSeen() }
                                                navController.navigate(Routes.HOME) {
                                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                    composable(Routes.HOME) {
                                        HomeScreen(
                                            libraryViewModel = libraryViewModel,
                                            scanViewModel = scanViewModel,
                                            isPro = isPro,
                                            onOpenDocument = { docId ->
                                                navController.navigate(Routes.viewer(docId))
                                            },
                                            onOpenScan = { navController.navigate(Routes.SCAN) },
                                            onOpenLibrary = { navigateToTab(Routes.LIBRARY) },
                                            onOpenTools = { navigateToTab(Routes.TOOLS) },
                                            onToolClick = onToolClick,
                                            onOpenPro = { navigateToTab(Routes.SETTINGS) }
                                        )
                                    }
                                    composable(Routes.LIBRARY) {
                                        LibraryScreen(
                                            viewModel = libraryViewModel,
                                            onOpenDocument = { docId ->
                                                navController.navigate(Routes.viewer(docId))
                                            },
                                            onOpenScan = { navController.navigate(Routes.SCAN) },
                                            onOpenSettings = { navigateToTab(Routes.SETTINGS) },
                                            onOpenPro = { navigateToTab(Routes.SETTINGS) }
                                        )
                                    }
                                    composable(
                                        route = Routes.VIEWER,
                                        arguments = listOf(
                                            navArgument("docId") { type = NavType.LongType },
                                            navArgument("edit") {
                                                type = NavType.BoolType
                                                defaultValue = false
                                            }
                                        )
                                    ) { backStackEntry ->
                                        val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
                                        val edit = backStackEntry.arguments?.getBoolean("edit") ?: false
                                        ViewerScreen(
                                            docId = docId,
                                            viewModel = viewerViewModel,
                                            onBack = { navController.popBackStack() },
                                            editModeInitial = edit
                                        )
                                    }
                                    composable(Routes.SCAN) {
                                        ScanScreen(
                                            viewModel = scanViewModel,
                                            onBack = { navController.popBackStack() },
                                            onSaved = { navigateToTab(Routes.LIBRARY) },
                                            onOpenPro = { navigateToTab(Routes.SETTINGS) }
                                        )
                                    }
                                    composable(Routes.TOOLS) {
                                        ToolsScreen(
                                            viewModel = libraryViewModel,
                                            onToolClick = onToolClick
                                        )
                                    }
                                    composable(Routes.SETTINGS) {
                                        SettingsScreen(
                                            viewModel = settingsViewModel,
                                            onBack = { navController.popBackStack() },
                                            onOpenSupport = { navController.navigate(Routes.SUPPORT) }
                                        )
                                    }
                                    composable(Routes.SUPPORT) {
                                        SupportScreen(
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
