package com.example.projectkas

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.projectkas.Module.LanguageRepository
import com.example.projectkas.Screens.Home
import com.example.projectkas.Screens.Login
import com.example.projectkas.Screens.ProfileScreen
import com.example.projectkas.Screens.Register
import com.example.projectkas.Screens.Settings
import com.example.projectkas.Screens.SignUp
import com.example.projectkas.Screens.Splash
import com.example.projectkas.Screens.StudentsList
import com.example.projectkas.ui.theme.LocaleHelper
import com.example.projectkas.ui.theme.ProjectKASTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import androidx.compose.runtime.collectAsState
import com.example.projectkas.Module.ThemeMode
import com.example.projectkas.ViewModel.AuthViewModel
import com.example.projectkas.ui.theme.navBarIndicatorIconColor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Construct a lightweight LanguageRepository manually,
        // since Hilt isn't available yet during attachBaseContext.
        val repo = LanguageRepository(newBase)

        val language = runBlocking {
            repo.getLanguageFlow().first() ?: Locale.getDefault().language
        }

        val context = LocaleHelper.wrap(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasApp()
        }
    }
}

@Composable
fun KasApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val currentTheme by authViewModel.currentTheme.collectAsState()
    val isDarkTheme = when (currentTheme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Screens that should show Scaffold (main graph)
    val bottomBarScreens = listOf(Screen.Home, Screen.Register, Screen.Settings)

    val optionalScreenRoute = "profile/{rollNo}/{studentName}/{id}"
    val optionalScreenRoute2 = "student/{classID}"

    val showTopBars = bottomBarScreens.any {
        it.route == currentRoute ||
                optionalScreenRoute == currentRoute ||
                optionalScreenRoute2 == currentRoute
    }
    val activity = LocalContext.current.findActivity()

    ProjectKASTheme(darkTheme = isDarkTheme) {
        Scaffold(
            // ✅ FIXED: Use theme color instead of hardcoded Color(0xFF181717)
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (showTopBars) {
                    CustomTopBar()
                }
            },
            bottomBar = {
                if (showTopBars) {
                    // ✅ FIXED: Use theme color instead of Color(30, 28, 28)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        bottomBarScreens.forEach { screen ->
                            NavigationBarItem(
                                // ✅ FIXED: All navigation colors now use theme
                                colors = NavigationBarItemDefaults.colors(
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    selectedIconColor = navBarIndicatorIconColor,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Text(
                                        text = screen.icon,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(innerPadding)
            ) {

                // Splash decides where to go
                composable("splash") {
                    Splash(
                        onAuthCheckComplete = { isLoggedIn ->
                            if (isLoggedIn) {
                                navController.navigate("main") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else {
                                navController.navigate("auth") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // Auth graph
                navigation(startDestination = Screen.Login.route, route = "auth") {
                    composable(Screen.Login.route) {
                        Login(
                            onLoginSuccess = {
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            },
                            onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
                        )
                    }
                    composable(Screen.SignUp.route) {
                        SignUp(
                            onSignUpSuccess = {
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            },
                            onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                        )
                    }
                }

                // Main graph
                navigation(startDestination = Screen.Home.route, route = "main") {
                    composable(Screen.Home.route) { Home(navController) }
                    composable(Screen.Register.route) { Register(navController) }
                    composable(Screen.Settings.route) {
                        Settings(
                            onLocaleApplied = { langCode ->
                                // apply new locale and restart activity to pick up resources
                                activity?.let { act ->
                                    // wrap the base context (optional but helpful)
                                    LocaleHelper.wrap(act, langCode)
                                    // restart activity to reload resources / recomposition
                                    act.recreate()
                                }
                            },
                            onLogout = {
                                navController.navigate("auth") {
                                    popUpTo("main") { inclusive = true }
                                }
                            },
                            navController = navController
                        )
                    }
                    composable(
                        route = "${Screen.Profile.route}/{rollNo}/{studentName}/{id}/{classID}",
                        arguments = listOf(
                            navArgument("rollNo") { type = NavType.StringType },
                            navArgument("studentName") { type = NavType.StringType },
                            navArgument("id") { type = NavType.StringType },
                            navArgument("classID") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val rollNo = backStackEntry.arguments?.getString("rollNo")
                        val classID = backStackEntry.arguments?.getString("classID")
                        val studentName = backStackEntry.arguments?.getString("studentName")
                        val id = backStackEntry.arguments?.getString("id")
                        ProfileScreen(
                            navController = navController,
                            rollNo = rollNo,
                            studentName = studentName,
                            id = id,
                            classID = classID
                        )
                    }

                    composable(
                        route = "${Screen.Student.route}/{classID}",
                        arguments = listOf(
                            navArgument("classID") { type = NavType.StringType },
                        )
                    ) { backStackEntry ->
                        val classID = backStackEntry.arguments?.getString("classID")
                        StudentsList(navController = navController, classID)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(currentRoute: String? = "⌘") {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = currentRoute ?: "",
                style = MaterialTheme.typography.headlineLarge,
                // ✅ FIXED: Use theme color instead of Color(238, 238, 238)
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        // ✅ FIXED: Use theme colors instead of hardcoded colors
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

sealed class Screen(val route: String, val title: String, val icon : String) {
    object Login : Screen(route = "login", title = "Login", icon = "❖")
    object SignUp : Screen(route = "signup", title = "SignUp", icon = "❖")
    object Home : Screen(route = "⌘", title = "Home", icon = "⌘")
    object Register : Screen(route = "⌆", title = "Register", icon = "⌆")
    object Settings : Screen(route = "⚙︎", title = "Settings", icon = "⚙︎")
    object Profile : Screen("profile", "Profile", "👤")
    object Student : Screen("student", "Students", "")
}

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}