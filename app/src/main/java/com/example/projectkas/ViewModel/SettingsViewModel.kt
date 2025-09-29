package com.example.projectkas.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectkas.Module.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: LanguageRepository
) : ViewModel() {

    private val _currentLang = MutableStateFlow<String?>(null)
    val currentLang: StateFlow<String?> = _currentLang.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getLanguageFlow().collect { lang ->
                _currentLang.value = lang
            }
        }
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            repo.saveLanguage(langCode)
        }
    }
}
