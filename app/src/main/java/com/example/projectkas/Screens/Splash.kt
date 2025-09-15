package com.example.projectkas.Screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.projectkas.ViewModel.AuthState
import com.example.projectkas.ViewModel.AuthViewModel

@Composable
fun Splash(
    onAuthCheckComplete: (Boolean) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.observeAsState(AuthState.Loading)

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                onAuthCheckComplete(true)
            }
            is AuthState.Unauthenticated -> {
                onAuthCheckComplete(false)
            }
            else -> Unit
        }
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (authState) {
            is AuthState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthState.Error -> {
                val errorMessage = (authState as AuthState.Error).message
                Text(text = "Error: $errorMessage")
            }
            else -> Unit
        }
    }
}
