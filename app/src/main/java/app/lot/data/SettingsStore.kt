package app.lot.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.lot.domain.Wish
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** Lives drawn as a witness: fate makes every choice, and you only read. */
    val witness: Boolean = false,
    /** The era, area and sex the reader keeps asking of the lot; any by default. */
    val wish: Wish = Wish(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(onboarded = this[ONBOARDED] ?: false, witness = this[WITNESS] ?: false, wish = Wish.decode(this[WISH]))

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[WITNESS] = s.witness
            it[WISH] = s.wish.encode()
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val WITNESS = booleanPreferencesKey("witness")
        val WISH = stringPreferencesKey("wish")
    }
}
