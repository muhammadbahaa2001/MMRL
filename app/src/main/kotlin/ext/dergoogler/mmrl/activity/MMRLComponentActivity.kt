package ext.dergoogler.mmrl.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dergoogler.mmrl.R
import com.dergoogler.mmrl.repository.LocalRepository
import com.dergoogler.mmrl.repository.UserPreferencesRepository
import com.dergoogler.mmrl.ui.activity.CrashHandlerActivity
import com.dergoogler.mmrl.ui.activity.InstallActivity
import com.dergoogler.mmrl.ui.activity.ModConfActivity
import com.dergoogler.mmrl.ui.providable.LocalUserPreferences
import com.dergoogler.mmrl.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
open class MMRLComponentActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var localRepository: LocalRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            startCrashActivity(thread, throwable)
        }
    }

    private fun startCrashActivity(thread: Thread, throwable: Throwable) {
        val intent = Intent(this, CrashHandlerActivity::class.java).apply {
            putExtra("message", throwable.message)
            putExtra("stacktrace", formatStackTrace(throwable))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()

        exitProcess(0)
    }

    private fun formatStackTrace(throwable: Throwable, numberOfLines: Int = 88): String {
        val stackTrace = throwable.stackTrace
        val stackTraceElements = stackTrace.joinToString("\n") { it.toString() }

        return if (stackTrace.size > numberOfLines) {
            val trimmedStackTrace =
                stackTraceElements.lines().take(numberOfLines).joinToString("\n")
            val moreCount = stackTrace.size - numberOfLines

            getString(R.string.stack_trace_truncated, trimmedStackTrace, moreCount)
        } else {
            stackTraceElements
        }
    }

    companion object {
        fun startModConfActivity(context: Context, modId: String) {
            val intent = Intent(context, ModConfActivity::class.java)
                .apply {
                    putExtra("MOD_ID", modId)
                }

            context.startActivity(intent)
        }

        fun startInstallActivity(context: Context, uri: Uri) {
            val intent = Intent(context, InstallActivity::class.java)
                .apply {
                    data = uri
                }

            context.startActivity(intent)
        }
    }
}

fun MMRLComponentActivity.setBaseContent(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit
) {
    this.setContent(
        parent = parent,
    ) {
        val userPreferences by userPreferencesRepository.data.collectAsStateWithLifecycle(
            initialValue = null
        )

        val preferences = if (userPreferences == null) {
            return@setContent
        } else {
            checkNotNull(userPreferences)
        }

        CompositionLocalProvider(
            LocalUserPreferences provides preferences
        ) {
            AppTheme(
                darkMode = preferences.isDarkMode(), themeColor = preferences.themeColor
            ) {
                content()
            }
        }
    }
}