package com.sonofasleep.watertheplantapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val PLANT_PREFERENCES = "plant_references"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PLANT_PREFERENCES
)

class DataStoreRepository(context: Context) {

    private val isSortASC = booleanPreferencesKey("is_sort_asc")
    private val numberOfPermissionTry = intPreferencesKey("number_of_try")

    val readSortType: Flow<Boolean> = context.dataStore.data
        .catch {
            if (it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[isSortASC] ?: true
        }

    val readNumberOfPermissionTry: Flow<Int> = context.dataStore.data
        .catch {
            if (it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences -> preferences[numberOfPermissionTry] ?: 0 }

    suspend fun saveSortType(isSortASC: Boolean, context: Context) {
        context.dataStore.edit { preference ->
            preference[this.isSortASC] = isSortASC
        }
    }

    suspend fun saveNumberOfPermissionTry(permissionTryNumber: Int, context: Context) {
        context.dataStore.edit { preference ->
            preference[this.numberOfPermissionTry] = permissionTryNumber
        }
    }
}