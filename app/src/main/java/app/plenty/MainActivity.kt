package app.plenty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.plenty.ui.PlentyApp
import app.plenty.ui.theme.PlentyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlentyTheme {
                PlentyApp()
            }
        }
    }
}
