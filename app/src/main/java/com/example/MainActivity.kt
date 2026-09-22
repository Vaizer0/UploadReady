package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import com.vaizero.uploadready.ui.home.HomeScreen
import com.vaizero.uploadready.ui.home.HomeViewModel
import com.vaizero.uploadready.ui.navigation.Screen
import com.vaizero.uploadready.ui.profiles.ProfilesScreen
import com.vaizero.uploadready.ui.profiles.ProfilesViewModel
import com.vaizero.uploadready.ui.settings.SettingsScreen
import com.vaizero.uploadready.ui.tools.ToolsScreen
import com.vaizero.uploadready.ui.tools.pdf.PdfToolScreen
import com.vaizero.uploadready.ui.tools.pdf.PdfToolViewModel
import com.vaizero.uploadready.ui.tools.photo.PhotoToolScreen
import com.vaizero.uploadready.ui.tools.photo.PhotoToolViewModel
import com.vaizero.uploadready.ui.tools.signature.SignatureToolScreen
import com.vaizero.uploadready.ui.tools.signature.SignatureToolViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val photoToolViewModel: PhotoToolViewModel by viewModels()
    private val signatureToolViewModel: SignatureToolViewModel by viewModels()
    private val pdfToolViewModel: PdfToolViewModel by viewModels()
    private val profilesViewModel: ProfilesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                UploadReadyApp(
                    homeViewModel = homeViewModel,
                    photoToolViewModel = photoToolViewModel,
                    signatureToolViewModel = signatureToolViewModel,
                    pdfToolViewModel = pdfToolViewModel,
                    profilesViewModel = profilesViewModel
                )
            }
        }
    }
}

@Composable
fun UploadReadyApp(
    homeViewModel: HomeViewModel,
    photoToolViewModel: PhotoToolViewModel,
    signatureToolViewModel: SignatureToolViewModel,
    pdfToolViewModel: PdfToolViewModel,
    profilesViewModel: ProfilesViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Tools,
        Screen.Profiles,
        Screen.Settings
    )

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            modifier = Modifier.testTag("nav_item_${screen.route}"),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToPhoto = { navController.navigate(Screen.ROUTE_PHOTO) },
                    onNavigateToSignature = { navController.navigate(Screen.ROUTE_SIGNATURE) },
                    onNavigateToPdf = { navController.navigate(Screen.ROUTE_PDF) },
                    onNavigateToProfiles = { navController.navigate(Screen.Profiles.route) }
                )
            }

            // Tools Overview
            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigateToPhoto = { navController.navigate(Screen.ROUTE_PHOTO) },
                    onNavigateToSignature = { navController.navigate(Screen.ROUTE_SIGNATURE) },
                    onNavigateToPdf = { navController.navigate(Screen.ROUTE_PDF) }
                )
            }

            // Profiles
            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    viewModel = profilesViewModel,
                    onApplyPhotoProfile = { profile ->
                        photoToolViewModel.applyProfile(profile)
                        navController.navigate(Screen.ROUTE_PHOTO)
                    },
                    onApplySignatureProfile = { profile ->
                        signatureToolViewModel.applyProfile(profile)
                        navController.navigate(Screen.ROUTE_SIGNATURE)
                    }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // Sub-routes for each Tool
            composable(Screen.ROUTE_PHOTO) {
                PhotoToolScreen(
                    viewModel = photoToolViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ROUTE_SIGNATURE) {
                SignatureToolScreen(
                    viewModel = signatureToolViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ROUTE_PDF) {
                PdfToolScreen(
                    viewModel = pdfToolViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
