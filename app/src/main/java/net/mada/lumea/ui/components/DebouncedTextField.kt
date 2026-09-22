package net.mada.lumea.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Un champ de texte dont la frappe reste locale.
 *
 * Les champs adossés aux réglages écrivaient dans DataStore à chaque lettre. Or
 * l'écriture est asynchrone : la valeur revenait par le flux avec un temps de
 * retard, remplaçait ce qui était déjà tapé entre-temps, et le curseur repartait
 * en arrière. À l'usage, ça donnait des lettres avalées et des caractères remis
 * dans le désordre — d'autant plus avec un clavier à correction automatique.
 *
 * Ici, la frappe ne touche qu'un état local et instantané. L'enregistrement part
 * une fois la frappe retombée ([DEBOUNCE_MS]), ou quand la valeur d'origine change
 * pour une autre raison. Le champ n'est réalimenté depuis la source qu'au premier
 * chargement : ensuite, c'est la personne qui écrit qui a raison, pas le flux.
 */
private const val DEBOUNCE_MS = 400L

@Composable
fun DebouncedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    /**
     * À changer pour forcer le champ à reprendre [value].
     *
     * Sert quand la valeur est modifiée ailleurs qu'au clavier — une puce de
     * suggestion, par exemple. Sans ce signal, le champ resterait sur ce qu'il
     * affichait, puisqu'il ignore volontairement les retours du flux.
     */
    resetKey: Any? = null,
) {
    var text by rememberSaveable { mutableStateOf(value) }
    var seeded by rememberSaveable { mutableStateOf(value.isNotEmpty()) }

    // Premier chargement seulement : la valeur arrive de DataStore après le premier
    // rendu, et il faut bien l'afficher. Au-delà, on ne réécrit plus par-dessus.
    LaunchedEffect(value) {
        if (!seeded && value.isNotEmpty()) {
            text = value
            seeded = true
        }
    }

    LaunchedEffect(resetKey) {
        if (resetKey != null) text = value
    }

    LaunchedEffect(text) {
        if (text != value) {
            delay(DEBOUNCE_MS)
            onValueChange(text)
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { androidx.compose.material3.Text(label) },
        placeholder = placeholder?.let { { androidx.compose.material3.Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    )
}
