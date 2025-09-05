package com.example.projectkas.Screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.projectkas.Network.RetrofitInstance
import com.example.projectkas.Network.Student
import com.example.projectkas.ViewModel.AuthState
import com.example.projectkas.ViewModel.AuthViewModel

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun Settings(onLogout : () -> Unit,navController: NavController){

    val parentEntry = remember(navController) { navController.getBackStackEntry("main") }
    val authViewModel: AuthViewModel = hiltViewModel(parentEntry)

    val authState = authViewModel.authState.observeAsState()
    val currentUserEmail = authViewModel.auth.currentUser?.email ?: ""

    // State for API call
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val debugMode by authViewModel.debugMode.collectAsState()

    // Fetch students when authenticated
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> onLogout()
            is AuthState.Authenticated -> {
                isLoading = true
                try {
                    val response = RetrofitInstance.api.listStudents(currentUserEmail)
                    if (response.isSuccessful) {
                        students = response.body()?.students ?: emptyList()
                    } else {
                        errorMessage = "Error: ${response.errorBody()?.string()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Exception: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
            }
            else -> Unit
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color(24, 23, 23))
            .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(
            text = "Settings",
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Divider(
            color = Color(0xFF468A9A), // teal accent
            thickness = 2.dp,
            modifier = Modifier.width(180.dp)
        )
        Spacer(modifier = Modifier.height(25.dp))

        if (authState.value is AuthState.Authenticated) {
            // Profile card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Logged in as", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Text(currentUserEmail, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Debug mode",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = debugMode,
                    onCheckedChange = { authViewModel.setDebugMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF468A9A),
                        uncheckedThumbColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Student table
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else if (errorMessage != null) {
                Text(errorMessage ?: "", color = Color.Red)
            } else {
                if (students.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "Registered Students",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Divider(
                                color = Color.Gray,
                                thickness = 1.dp
                            )

                            LazyColumn {
                                items(students) { student ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = student.roll_no,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = student.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Divider(
                                        color = Color.DarkGray,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("No students registered.", color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Card(
            modifier = Modifier
                .height(55.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { authViewModel.signout() },
            shape = RoundedCornerShape(25.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8B1A1A)) // deep red
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign out",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Sign out",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

    }
}



