package net.mada.lumea.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.mada.lumea.di.AppContainer
import net.mada.lumea.di.container

/** Raccourci : crée un ViewModel en lui passant le conteneur de dépendances de l'app. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val appContainer = LocalContext.current.applicationContext.container()
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(appContainer) } },
    )
}
