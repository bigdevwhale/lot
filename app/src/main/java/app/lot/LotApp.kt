package app.lot

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import app.lot.data.DemoSeed
import app.lot.data.LotDb
import app.lot.data.Repo
import app.lot.data.SettingsStore
import app.lot.domain.Content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = LotDb.create(app)
    val content: Content = Content.read(
        app.assets.list("lines").orEmpty().sorted().map { app.assets.open("lines/$it") },
        app.assets.open("history.tsv"),
        app.assets.list("").orEmpty().filter { it.startsWith("dilemmas") && it.endsWith(".txt") }.sorted().map { app.assets.open(it) },
    )
    val repo = Repo(db, content)
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** False while the demo build is still living its first thirty lives; the screens wait for it. */
    val ready = MutableStateFlow(!BuildConfig.DEMO)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class LotApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
            val lang = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: resources.configuration.locales[0].language
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container, lang == "ru")
            container.repo.refresh()
            container.ready.value = true
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as LotApp).container

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
