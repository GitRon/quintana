package io.github.gitron.quintana

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.gitron.quintana.ui.QuintanaApp
import io.github.gitron.quintana.ui.QuintanaTheme
import io.github.gitron.quintana.ui.QuintanaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: QuintanaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so the system bars always get light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            QuintanaTheme {
                QuintanaApp(viewModel)
            }
        }
    }
}
