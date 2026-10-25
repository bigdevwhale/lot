package app.lot.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** Lives drawn as a witness: fate makes every choice, and you only read. */
    val witness: Boolean = false,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(onboarded = this[ONBOARDED] ?: false, witness = this[WITNESS] ?: false)

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[WITNESS] = s.witness
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val WITNESS = booleanPreferencesKey("witness")
    }
}
