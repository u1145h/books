package com.u1145h.books.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.u1145h.books.feature.home.HomeScreen
import com.u1145h.books.feature.library.LibraryScreen
import com.u1145h.books.feature.reader.ReaderScreen
import com.u1145h.books.feature.search.SearchScreen
import com.u1145h.books.feature.series.SeriesDetailScreen
import com.u1145h.books.feature.settings.SettingsScreen
import com.u1145h.books.feature.setup.SetupScreen
import com.u1145h.books.ui.theme.KavitaTheme

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Home : Screen("home")
    object Library : Screen("library/{libraryId}/{libraryName}") {
        fun go(id: Int, name: String) = "library/$id/${name.encode()}"
    }
    object Series : Screen("series/{seriesId}") {
        fun go(id: Int) = "series/$id"
    }
    object Reader : Screen("reader/{chapterId}/{seriesId}") {
        fun go(chapterId: Int, seriesId: Int) = "reader/$chapterId/$seriesId"
    }
    object Search : Screen("search")
    object Settings : Screen("settings")
}

private fun String.encode() = java.net.URLEncoder.encode(this, "UTF-8")

@Composable
fun KavitaApp() {
    val rootViewModel: RootViewModel = hiltViewModel()
    val settings by rootViewModel.settings.collectAsStateWithLifecycle()
    val isLoggedIn by rootViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isReady by rootViewModel.isReady.collectAsStateWithLifecycle()

    val darkTheme = when (settings.themeMode) {
        com.u1145h.books.domain.model.ThemeMode.LIGHT -> false
        com.u1145h.books.domain.model.ThemeMode.DARK -> true
        else -> true // default dark for media app
    }

    KavitaTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
        if (isReady) {
            val navController = rememberNavController()
            val startDest = if (isLoggedIn) Screen.Home.route else Screen.Setup.route

            NavHost(navController = navController, startDestination = startDest) {
                composable(Screen.Setup.route) {
                    SetupScreen(onSetupComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Home.route) {
                    HomeScreen(
                        onLibraryClick = { id, name ->
                            navController.navigate(Screen.Library.go(id, name))
                        },
                        onSeriesClick = { id ->
                            navController.navigate(Screen.Series.go(id))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onLogout = {
                            navController.navigate(Screen.Setup.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    Screen.Library.route,
                    arguments = listOf(
                        navArgument("libraryId") { type = NavType.IntType },
                        navArgument("libraryName") { type = NavType.StringType },
                    ),
                ) { back ->
                    val libId = back.arguments?.getInt("libraryId") ?: 0
                    val libName = java.net.URLDecoder.decode(
                        back.arguments?.getString("libraryName") ?: "",
                        "UTF-8",
                    )
                    LibraryScreen(
                        libraryId = libId,
                        libraryName = libName,
                        onSeriesClick = { navController.navigate(Screen.Series.go(it)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    Screen.Series.route,
                    arguments = listOf(navArgument("seriesId") { type = NavType.IntType }),
                ) { back ->
                    val sid = back.arguments?.getInt("seriesId") ?: 0
                    SeriesDetailScreen(
                        seriesId = sid,
                        onChapterClick = { chapterId ->
                            navController.navigate(Screen.Reader.go(chapterId, sid))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    Screen.Reader.route,
                    arguments = listOf(
                        navArgument("chapterId") { type = NavType.IntType },
                        navArgument("seriesId") { type = NavType.IntType },
                    ),
                ) { back ->
                    val chapterId = back.arguments?.getInt("chapterId") ?: 0
                    val seriesId = back.arguments?.getInt("seriesId") ?: 0
                    ReaderScreen(
                        chapterId = chapterId,
                        seriesId = seriesId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        onSeriesClick = { navController.navigate(Screen.Series.go(it)) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
