package com.minimaltv.files

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.minimaltv.files.ui.GlassBackground
import com.minimaltv.files.ui.StorageScreen
import com.minimaltv.files.ui.theme.FilesTheme
import java.io.File

/**
 * The whole app: one file manager screen. Another app can open it at a folder by passing the folder's path
 * in the [EXTRA_PATH] extra (for example "Show in folder" in a receiver app).
 */
class MainActivity : ComponentActivity() {

    private var startAt by mutableStateOf<File?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startAt = folderFrom(intent)
        setContent {
            FilesTheme {
                GlassBackground {
                    // gives Text its colour; the photo behind stays visible
                    Surface(color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) {
                        StorageScreen(startAt = startAt, onStartConsumed = { startAt = null })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        folderFrom(intent)?.let { startAt = it }
    }

    private fun folderFrom(intent: Intent?): File? =
        intent?.getStringExtra(EXTRA_PATH)?.let(::File)?.takeIf { it.isDirectory }

    companion object {
        const val EXTRA_PATH = "com.minimaltv.files.PATH"
    }
}
