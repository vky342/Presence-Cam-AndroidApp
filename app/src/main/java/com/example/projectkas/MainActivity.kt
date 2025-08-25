package com.example.projectkas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.projectkas.Screens.Home
import com.example.projectkas.Screens.Login
import com.example.projectkas.Screens.Register
import com.example.projectkas.Screens.Settings
import com.example.projectkas.Screens.SignUp
import com.example.projectkas.Screens.Splash
import com.example.projectkas.ui.theme.ProjectKASTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectKASTheme {
                KasApp()
            }
        }
    }
}
@Composable
fun KasApp() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Screens that should show Scaffold (main graph)
    val bottomBarScreens = listOf(Screen.Home, Screen.Register, Screen.Settings)

    val showBars = currentRoute in bottomBarScreens.map { it.route }

    Scaffold(
        topBar = {
            if (showBars) {
                CustomTopBar()
            }
        },
        bottomBar = {
            if (showBars) {
                NavigationBar(containerColor = Color(30, 28, 28)) {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            colors = NavigationBarItemDefaults.colors(
                                selectedTextColor = Color(238, 238, 238),
                                selectedIconColor = Color.Black,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.White
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
                composable(Screen.Settings.route) { Settings(
                    onLogout = {
                        navController.navigate("auth") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                ) }
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
                color = Color(238, 238, 238) // ensures contrast
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(24, 23, 23), // dark background
            titleContentColor = Color(238, 238, 238)
        )
    )
}


sealed class Screen(val route: String, val title: String, val icon : String) {
    object Login : Screen(route = "login", title = "Login", icon = "❖")
    object SignUp : Screen(route = "signup", title = "SignUp", icon = "❖")
    object Home : Screen(route = "⌘", title = "Home", icon = "⌘")
    object Register : Screen(route = "⌆", title = "Register", icon = "⌆")
    object Settings : Screen(route = "⚙︎", title = "Settings", icon = "⚙︎")
}

