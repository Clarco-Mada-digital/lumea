package net.mada.lumea

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.data.prefs.Settings
import net.mada.lumea.di.container
import net.mada.lumea.ui.LumeaNavHost
import net.mada.lumea.ui.lock.LockScreen
import net.mada.lumea.ui.theme.LumeaTheme

/**
 * FragmentActivity (et non ComponentActivity) : c'est ce qu'exige BiometricPrompt
 * pour afficher le dialogue d'empreinte.
 */
class MainActivity : FragmentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = container()

        /*
         * Anti-tapjacking : Android ignore les appuis reçus pendant qu'une autre
         * app superpose une fenêtre. Sans cela, une app malveillante peut couvrir
         * le pavé de code d'un calque transparent et lire ce qu'on tape.
         */
        window.decorView.filterTouchesWhenObscured = true

        // Verrouille dès que l'app quitte le premier plan.
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) app.lock.lock()
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
            val unlocked by app.lock.unlocked.collectAsStateWithLifecycle()

            SideEffect { applyScreenshotPolicy(settings.hideFromRecents) }

            LumeaTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        LumeaNavHost(settings = settings)

                        AnimatedVisibility(visible = !unlocked, enter = fadeIn(), exit = fadeOut()) {
                            LockScreen(
                                lockManager = app.lock,
                                biometricEnabled = settings.biometricEnabled,
                                activity = this@MainActivity,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Empêche les captures d'écran et masque l'aperçu dans la liste des apps récentes.
     *
     * Jamais en debug : FLAG_SECURE noircit aussi la recopie d'écran (scrcpy, Android
     * Studio), ce qui rend le développement impossible à l'aveugle.
     */
    private fun applyScreenshotPolicy(hide: Boolean) {
        if (hide && !BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
