package com.example.projectkas.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.projectkas.ViewModel.AuthState
import com.example.projectkas.ViewModel.AuthViewModel

@Composable
fun Settings(onLogout : () -> Unit,authViewModel: AuthViewModel = hiltViewModel()){

    val authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        when(authState.value){
            is AuthState.Unauthenticated -> onLogout()
            else -> Unit
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color(24, 23, 23)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("VisualAttendanceApp is a streamlined tool designed to simplify attendance tracking using computer vision. Built with Jetpack Compose, this app lets users upload or capture three clear face photos for quick registration, and then accurately records attendance. With a clean Material design and smooth navigation, it offers an intuitive experience for educators and administrators alike.",
            textAlign = TextAlign.Center,modifier = Modifier.fillMaxWidth(0.8f),style = MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic
            ), color = Color(89, 88, 88, 255)
        )

        Spacer(modifier = Modifier.fillMaxWidth().height(40.dp))

        TextButton(onClick = {
            authViewModel.signout()
        }) {
            Text(text = "Sign out")
        }

    }

}



