package com.example.projectkas.Screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.projectkas.Network.RetrofitInstance
import com.example.projectkas.Network.Student
import com.example.projectkas.Screen
import com.example.projectkas.ViewModel.AuthState
import com.example.projectkas.ViewModel.AuthViewModel
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun Settings(onLogout : () -> Unit,navController: NavController){

    val parentEntry = remember(navController) { navController.getBackStackEntry("main") }
    val authViewModel: AuthViewModel = hiltViewModel(parentEntry)

    val authState = authViewModel.authState.observeAsState()
    val currentUserEmail = authViewModel.auth.currentUser?.email ?: ""

    // State for API call
    var query by rememberSaveable { mutableStateOf("") }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    val filteredStudents by remember {
        derivedStateOf {
            val q = query.trim()
            if (q.isEmpty()) students.toList()
            else students.filter { st ->
                st.roll_no.contains(q, true) ||
                        st.name.contains(q, true)
            }
        }
    }
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

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp, horizontal = 20.dp),
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

        Spacer(modifier = Modifier.height(14.dp))

        SimpleSearchBar(
            query = query,
            onQueryChange = {
                query = it
            },
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.95f),
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Student table
        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else if (errorMessage != null) {
            Text(errorMessage ?: "", color = Color.Red)
        } else {
            if (filteredStudents.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Registered Students",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                        )
                        Divider(
                            color = Color.Gray,
                            thickness = 1.dp
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredStudents) { student ->
                                StudentRow(
                                    student = student,
                                    onEdit = {
                                        navController.navigate("${Screen.Profile.route}/${student.roll_no}/${student.name}")
                                    },
                                    onDelete = { /* Handle Delete */ }
                                )
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


        Spacer(modifier = Modifier.height(20.dp))

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

@Composable
fun StudentRow(student: Student, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = student.roll_no,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        onEdit()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                )
            }
        }
    }
}



@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithHistory(
    modifier: Modifier = Modifier,
    historyInitial: List<String> = emptyList(),
    placeholderText: String = "Search student...",
    onSearch: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    // persistent-ish history for the session; swap with ViewModel/prefs for real persistence
    var history by rememberSaveable { mutableStateOf(historyInitial) }

    var query by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // compute live suggestions (case-insensitive contains)
    val suggestions = remember(query, history) {
        if (query.isBlank()) history else history.filter { it.contains(query, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxWidth()) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(color = Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        expanded = true
                    },
                    placeholder = { Text(placeholderText) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search icon") },
                    trailingIcon = {
                        AnimatedVisibility(visible = query.isNotEmpty()) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "clear",
                                modifier = Modifier
                                    .clickable {
                                        query = ""
                                        expanded = false
                                    }
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        val finalQuery = query.trim()
                        if (finalQuery.isNotEmpty()) {
                            // add to front of history but keep unique, most recent first
                            history = listOf(finalQuery) + history.filterNot { it.equals(finalQuery, true) }
                            onSearch(finalQuery)
                            focusManager.clearFocus()
                            expanded = false
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(24, 23, 23),
                        unfocusedContainerColor = Color(24, 23, 23),

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,

                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,

                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

            }


        // Suggestions dropdown
        AnimatedVisibility(visible = expanded && suggestions.isNotEmpty()) {
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    suggestions.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // choose suggestion: run search and add to history as recent
                                    val chosen = item.trim()
                                    history = listOf(chosen) + history.filterNot { it.equals(chosen, true) }
                                    onSearch(chosen)
                                    query = ""
                                    expanded = false
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item, style = MaterialTheme.typography.bodyMedium)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search student..."
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = { onQueryChange(it) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    modifier = Modifier
                        .clickable {
                            onQueryChange("")
                            focusManager.clearFocus()
                        }
                        .padding(8.dp)
                )
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            focusManager.clearFocus()
        }),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(24, 23, 23),
            unfocusedContainerColor = Color(24, 23, 23),

            focusedTextColor = Color.White,
            unfocusedTextColor = Color.LightGray,

            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.Gray,

            focusedIndicatorColor = Color.White,
            unfocusedIndicatorColor = Color.Gray
        )
    )
}


