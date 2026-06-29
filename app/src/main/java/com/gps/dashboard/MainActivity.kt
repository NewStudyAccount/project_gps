package com.gps.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gps.dashboard.ui.screen.BacktrackScreen
import com.gps.dashboard.ui.screen.DashboardScreen
import com.gps.dashboard.ui.screen.TrackListScreen
import com.gps.dashboard.ui.screen.TrackReplayScreen
import com.gps.dashboard.ui.theme.GpsDashboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GpsDashboardTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToTrackList = { navController.navigate("track_list") },
            )
        }

        composable("track_list") {
            TrackListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToReplay = { trackId ->
                    navController.navigate("track_replay/$trackId")
                },
                onNavigateToBacktrack = { trackId ->
                    navController.navigate("backtrack/$trackId")
                },
            )
        }

        composable(
            "track_replay/{trackId}",
            arguments = listOf(navArgument("trackId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val trackId = backStackEntry.arguments?.getLong("trackId") ?: return@composable
            TrackReplayScreen(
                trackId = trackId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            "backtrack/{trackId}",
            arguments = listOf(navArgument("trackId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val trackId = backStackEntry.arguments?.getLong("trackId") ?: return@composable
            BacktrackScreen(
                trackId = trackId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
