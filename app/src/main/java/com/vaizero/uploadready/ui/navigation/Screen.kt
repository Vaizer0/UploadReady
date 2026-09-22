package com.vaizero.uploadready.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Tools : Screen("tools", "Tools", Icons.Filled.Tune, Icons.Outlined.Tune)
    object Profiles : Screen("profiles", "Profiles", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    // Sub-routes
    companion object {
        const val ROUTE_PHOTO = "tool_photo"
        const val ROUTE_SIGNATURE = "tool_signature"
        const val ROUTE_PDF = "tool_pdf"
    }
}
