package app.lot

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lot.data.Settings
import app.lot.ui.LotRoot
import app.lot.ui.theme.LotTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(t, t), navigationBarStyle = SystemBarStyle.light(t, t))
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            // Wait for settings so the welcome pages don't flash for someone already here.
            settings?.let { s -> LotTheme { LotRoot(s) } }
        }
    }
}
