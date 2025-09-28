package com.example.projectkas.Module

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val DATASTORE_NAME = "user_prefs"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

@Singleton
class LanguageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val LANG_KEY = stringPreferencesKey("language_code") // "en", "hi", etc.

    fun getLanguageFlow(): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[LANG_KEY] }

    suspend fun saveLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[LANG_KEY] = languageCode
        }
    }
}