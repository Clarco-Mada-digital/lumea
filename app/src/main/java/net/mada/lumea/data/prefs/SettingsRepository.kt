package net.mada.lumea.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.mada.lumea.domain.agenda.HolidayCountry

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("lumea_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Palette { ROSE, LAVANDE, PECHE, MENTHE, NUIT }

/** Taille du texte, appliquée à toute la typographie de l'app. */
enum class TextScale(val factor: Float, val label: String) {
    COMPACT(0.9f, "Compact"),
    NORMAL(1f, "Normal"),
    GRAND(1.15f, "Grand"),
    TRES_GRAND(1.3f, "Très grand"),
}

/** Style des coins : du plus anguleux au plus arrondi. */
enum class CornerStyle(val factor: Float, val label: String) {
    VIF(0.45f, "Vif"),
    NET(1f, "Net"),
    DOUX(1.6f, "Doux"),
}

data class Settings(
    val displayName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: Palette = Palette.ROSE,
    val dynamicColor: Boolean = false,
    val textScale: TextScale = TextScale.NORMAL,
    val cornerStyle: CornerStyle = CornerStyle.NET,
    val highContrast: Boolean = false,
    val showGlow: Boolean = true,
    val animationsEnabled: Boolean = true,
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val lutealLength: Int = 14,
    val cycleTabVisible: Boolean = true,
    val periodReminder: Boolean = true,
    /** Nombre de jours d'avance pour le rappel de règles. */
    val periodReminderDaysBefore: Int = 2,
    val fertileReminder: Boolean = false,
    val journalReminder: Boolean = false,
    /** Minutes depuis minuit pour le rappel de journal. */
    val journalReminderMinute: Int = 21 * 60,
    val lockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    /**
     * Désactivé par défaut. FLAG_SECURE noircit aussi la recopie d'écran et les
     * captures : activé d'office, il donne l'impression que l'app est cassée, et on
     * ne peut même plus atteindre le réglage pour le désactiver. La vraie protection
     * reste le code PIN ; ceci est un bonus qu'on choisit en connaissance de cause.
     */
    val hideFromRecents: Boolean = false,

    // --- Personnalisation de l'assistant IA -------------------------------
    /**
     * Le profil sert à composer une introduction que l'utilisatrice colle
     * elle-même dans la conversation. Il reste dans l'app tant qu'elle ne le fait
     * pas, et ne contient **rien de médical** — c'est une règle, pas un oubli.
     */
    val assistantName: String = "",
    val userAlias: String = "",
    val occupation: String = "",
    val interests: String = "",
    val assistantTone: AssistantTone = AssistantTone.SIMPLE,
    val assistantNotes: String = "",

    /**
     * Widget en mode discret : il n'affiche qu'un point et le nom de l'app.
     *
     * Activé par défaut. Un widget qui annonce « 12 SA » sur l'écran d'accueil
     * trahit une grossesse auprès de quiconque regarde le téléphone ; c'est à
     * l'utilisatrice de décider si elle peut se le permettre, pas à nous de le
     * supposer.
     */
    val widgetDiscreet: Boolean = true,

    /** L'invitation à poser un code a été refusée : on ne la repropose plus. */
    val pinInvitationDismissed: Boolean = false,

    /**
     * Pays dont on affiche les jours fériés dans l'agenda.
     *
     * Choisi explicitement plutôt que déduit de la langue du téléphone :
     * beaucoup d'appareils sont en français sans être en France, et afficher le
     * 14 juillet à Antananarivo serait aussi faux qu'oublier le 26 juin.
     */
    val holidayCountry: HolidayCountry = HolidayCountry.MADAGASCAR,
)

/** Comment l'assistant doit répondre. Le ton change beaucoup l'utilité perçue. */
enum class AssistantTone(val label: String, val instruction: String) {
    SIMPLE(
        "Simple et clair",
        "Explique simplement, avec des mots de tous les jours et des exemples concrets.",
    ),
    DETAILLE(
        "Détaillé",
        "Donne des réponses complètes et structurées, avec le raisonnement.",
    ),
    TECHNIQUE(
        "Technique",
        "Va droit au but, utilise le vocabulaire technique de mon domaine, sans " +
            "reformuler les bases.",
    ),
    PEDAGOGUE(
        "Comme un prof",
        "Pose-moi des questions pour vérifier que j'ai compris, et propose des " +
            "exercices ou des moyens mnémotechniques.",
    ),
}

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            displayName = p[K.displayName] ?: "",
            themeMode = p[K.themeMode]?.let(::enumOr) ?: ThemeMode.SYSTEM,
            palette = p[K.palette]?.let(::paletteOr) ?: Palette.ROSE,
            dynamicColor = p[K.dynamicColor] ?: false,
            textScale = p[K.textScale]?.let { raw ->
                runCatching { TextScale.valueOf(raw) }.getOrDefault(TextScale.NORMAL)
            } ?: TextScale.NORMAL,
            cornerStyle = p[K.cornerStyle]?.let { raw ->
                runCatching { CornerStyle.valueOf(raw) }.getOrDefault(CornerStyle.NET)
            } ?: CornerStyle.NET,
            highContrast = p[K.highContrast] ?: false,
            showGlow = p[K.showGlow] ?: true,
            animationsEnabled = p[K.animationsEnabled] ?: true,
            cycleLength = p[K.cycleLength] ?: 28,
            periodLength = p[K.periodLength] ?: 5,
            lutealLength = p[K.lutealLength] ?: 14,
            cycleTabVisible = p[K.cycleTabVisible] ?: true,
            periodReminder = p[K.periodReminder] ?: true,
            periodReminderDaysBefore = p[K.periodReminderDaysBefore] ?: 2,
            fertileReminder = p[K.fertileReminder] ?: false,
            journalReminder = p[K.journalReminder] ?: false,
            journalReminderMinute = p[K.journalReminderMinute] ?: (21 * 60),
            lockEnabled = p[K.lockEnabled] ?: false,
            biometricEnabled = p[K.biometricEnabled] ?: false,
            hideFromRecents = p[K.hideFromRecents] ?: false,
            assistantName = p[K.assistantName] ?: "",
            userAlias = p[K.userAlias] ?: "",
            occupation = p[K.occupation] ?: "",
            interests = p[K.interests] ?: "",
            assistantTone = p[K.assistantTone]?.let { raw ->
                runCatching { AssistantTone.valueOf(raw) }.getOrDefault(AssistantTone.SIMPLE)
            } ?: AssistantTone.SIMPLE,
            assistantNotes = p[K.assistantNotes] ?: "",
            widgetDiscreet = p[K.widgetDiscreet] ?: true,
            pinInvitationDismissed = p[K.pinInvitationDismissed] ?: false,
            holidayCountry = p[K.holidayCountry]?.let { raw ->
                runCatching { HolidayCountry.valueOf(raw) }
                    .getOrDefault(HolidayCountry.MADAGASCAR)
            } ?: HolidayCountry.MADAGASCAR,
        )
    }

    suspend fun setDisplayName(value: String) = put(K.displayName, value)
    suspend fun setThemeMode(value: ThemeMode) = put(K.themeMode, value.name)
    suspend fun setPalette(value: Palette) = put(K.palette, value.name)
    suspend fun setDynamicColor(value: Boolean) = put(K.dynamicColor, value)
    suspend fun setTextScale(value: TextScale) = put(K.textScale, value.name)
    suspend fun setCornerStyle(value: CornerStyle) = put(K.cornerStyle, value.name)
    suspend fun setHighContrast(value: Boolean) = put(K.highContrast, value)
    suspend fun setShowGlow(value: Boolean) = put(K.showGlow, value)
    suspend fun setAnimationsEnabled(value: Boolean) = put(K.animationsEnabled, value)
    suspend fun setCycleLength(value: Int) = put(K.cycleLength, value.coerceIn(20, 45))
    suspend fun setPeriodLength(value: Int) = put(K.periodLength, value.coerceIn(1, 12))
    suspend fun setLutealLength(value: Int) = put(K.lutealLength, value.coerceIn(9, 18))
    suspend fun setCycleTabVisible(value: Boolean) = put(K.cycleTabVisible, value)
    suspend fun setPeriodReminder(value: Boolean) = put(K.periodReminder, value)
    suspend fun setPeriodReminderDaysBefore(value: Int) = put(K.periodReminderDaysBefore, value.coerceIn(0, 7))
    suspend fun setFertileReminder(value: Boolean) = put(K.fertileReminder, value)
    suspend fun setJournalReminder(value: Boolean) = put(K.journalReminder, value)
    suspend fun setJournalReminderMinute(value: Int) = put(K.journalReminderMinute, value.coerceIn(0, 24 * 60 - 1))
    suspend fun setLockEnabled(value: Boolean) = put(K.lockEnabled, value)
    suspend fun setBiometricEnabled(value: Boolean) = put(K.biometricEnabled, value)
    suspend fun setHideFromRecents(value: Boolean) = put(K.hideFromRecents, value)

    suspend fun setAssistantName(value: String) = put(K.assistantName, value.take(40))
    suspend fun setUserAlias(value: String) = put(K.userAlias, value.take(40))
    suspend fun setOccupation(value: String) = put(K.occupation, value.take(80))
    suspend fun setInterests(value: String) = put(K.interests, value.take(200))
    suspend fun setAssistantTone(value: AssistantTone) = put(K.assistantTone, value.name)
    suspend fun setAssistantNotes(value: String) = put(K.assistantNotes, value.take(400))
    suspend fun setWidgetDiscreet(value: Boolean) = put(K.widgetDiscreet, value)
    suspend fun dismissPinInvitation() = put(K.pinInvitationDismissed, true)
    suspend fun setHolidayCountry(value: HolidayCountry) = put(K.holidayCountry, value.name)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    private fun enumOr(raw: String) = runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    private fun paletteOr(raw: String) = runCatching { Palette.valueOf(raw) }.getOrDefault(Palette.ROSE)

    private object K {
        val displayName = stringPreferencesKey("display_name")
        val themeMode = stringPreferencesKey("theme_mode")
        val palette = stringPreferencesKey("palette")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val textScale = stringPreferencesKey("text_scale")
        val cornerStyle = stringPreferencesKey("corner_style")
        val highContrast = booleanPreferencesKey("high_contrast")
        val showGlow = booleanPreferencesKey("show_glow")
        val animationsEnabled = booleanPreferencesKey("animations_enabled")
        val cycleLength = intPreferencesKey("cycle_length")
        val periodLength = intPreferencesKey("period_length")
        val lutealLength = intPreferencesKey("luteal_length")
        val cycleTabVisible = booleanPreferencesKey("cycle_tab_visible")
        val periodReminder = booleanPreferencesKey("period_reminder")
        val periodReminderDaysBefore = intPreferencesKey("period_reminder_days")
        val fertileReminder = booleanPreferencesKey("fertile_reminder")
        val journalReminder = booleanPreferencesKey("journal_reminder")
        val journalReminderMinute = intPreferencesKey("journal_reminder_minute")
        val lockEnabled = booleanPreferencesKey("lock_enabled")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
        val hideFromRecents = booleanPreferencesKey("hide_from_recents")
        val assistantName = stringPreferencesKey("assistant_name")
        val userAlias = stringPreferencesKey("assistant_user_alias")
        val occupation = stringPreferencesKey("assistant_occupation")
        val interests = stringPreferencesKey("assistant_interests")
        val assistantTone = stringPreferencesKey("assistant_tone")
        val assistantNotes = stringPreferencesKey("assistant_notes")
        val widgetDiscreet = booleanPreferencesKey("widget_discreet")
        val pinInvitationDismissed = booleanPreferencesKey("pin_invitation_dismissed")
        val holidayCountry = stringPreferencesKey("holiday_country")
    }
}
