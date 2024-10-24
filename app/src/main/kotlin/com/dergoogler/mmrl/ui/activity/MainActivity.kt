package com.dergoogler.mmrl.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import com.dergoogler.mmrl.Compat
import com.dergoogler.mmrl.app.Const
import com.dergoogler.mmrl.database.entity.Repo.Companion.toRepo
import com.dergoogler.mmrl.datastore.UserPreferencesCompat.Companion.isRoot
import com.dergoogler.mmrl.datastore.UserPreferencesCompat.Companion.isSetup
import com.dergoogler.mmrl.datastore.WorkingMode
import com.dergoogler.mmrl.network.NetworkUtils
import com.dergoogler.mmrl.worker.ModuleUpdateWorker
import com.dergoogler.mmrl.worker.RepoUpdateWorker
import ext.dergoogler.mmrl.activity.MMRLComponentActivity
import ext.dergoogler.mmrl.activity.setBaseContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : MMRLComponentActivity() {
    private var isLoading by mutableStateOf(true)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override val requirePermissions = listOf(Manifest.permission.POST_NOTIFICATIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { isLoading }

        if (permissionsGranted) {
            lifecycleScope.launch {
                val userPreferences = userPreferencesRepository.data.first()

                startWorkTask<RepoUpdateWorker>(
                    context = this@MainActivity,
                    enabled = userPreferences.autoUpdateRepos,
                    repeatInterval = userPreferences.autoUpdateReposInterval,
                    workName = REPO_UPDATE_WORK_NAME,
                    existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
                )

                startWorkTask<ModuleUpdateWorker>(
                    context = this@MainActivity,
                    enabled = userPreferences.checkModuleUpdates,
                    repeatInterval = userPreferences.checkModuleUpdatesInterval,
                    workName = MODULE_UPDATE_WORK_NAME,
                    existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
                )

            }
        }

        setBaseContent {
            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(initialValue = null)

            val preferences = if (userPreferences == null) {
                return@setBaseContent
            } else {
                isLoading = false
                checkNotNull(userPreferences)
            }

            LaunchedEffect(userPreferences) {
                if (preferences.workingMode.isSetup) {
                    Timber.d("add default repository")
                    localRepository.insertRepo(Const.DEMO_REPO_URL.toRepo())
                }

                Compat.init(preferences.workingMode)
                NetworkUtils.setEnableDoh(preferences.useDoh)
                setInstallActivityEnabled(preferences.workingMode.isRoot)
            }

            Crossfade(
                targetState = preferences.workingMode.isSetup,
                label = "MainActivity"
            ) { isSetup ->
                if (isSetup) {
                    SetupScreen(
                        setMode = ::setWorkingMode
                    )
                } else {
                    MainScreen()
                }
            }
        }
    }

    private fun setWorkingMode(value: WorkingMode) {
        lifecycleScope.launch {
            userPreferencesRepository.setWorkingMode(value)
        }
    }

    private fun setInstallActivityEnabled(enable: Boolean) {
        val component = ComponentName(
            this, InstallActivity::class.java
        )

        val state = if (enable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        packageManager.setComponentEnabledSetting(
            component,
            state,
            PackageManager.DONT_KILL_APP
        )
    }

}