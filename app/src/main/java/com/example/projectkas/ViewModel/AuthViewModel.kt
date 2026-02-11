package com.example.projectkas.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectkas.Network.RetrofitInstance.getApi
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

import com.example.projectkas.Module.ThemeMode
import com.example.projectkas.Module.ThemeRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn


@HiltViewModel
class AuthViewModel @Inject constructor( private val themeRepository: ThemeRepository) : ViewModel()  {

    val auth = FirebaseAuth.getInstance()

    // LiveData style
    private val _debugMode = MutableStateFlow(false)
    val debugMode = _debugMode.asStateFlow()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    fun setDebugMode(enabled: Boolean) {
        _debugMode.value = enabled
    }

    fun checkAuthStatus(){
        if(auth.currentUser==null){
            _authState.value = AuthState.Unauthenticated
        }else{
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email : String,password : String){

        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener{task->
                if (task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                }else{
                    _authState.value = AuthState.Error(task.exception?.message?:"Something went wrong")
                }
            }
    }

    fun signup(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Firebase signup successful → now create user DB on your server
                    viewModelScope.launch(Dispatchers.IO) {

                        withContext(NonCancellable) {
                            try {
                                val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())
                                val response = getApi().signup(emailPart)
//                            val response = api.healthCheck()
                                Log.d("AuthVM", "Server signup response: ${response.body()?.message}")
                            } catch (e: Exception) {
                                Log.e("AuthVM", "Server signup failed: ${e.message}")
                            }
                        }
                    }
                    _authState.postValue(AuthState.Authenticated)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
    val currentTheme: StateFlow<ThemeMode> = themeRepository.getThemeFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.saveTheme(mode)
        }
    }


}

sealed class AuthState{
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message : String) : AuthState()
}