package net.mada.lumea.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.mada.lumea.data.prefs.Settings
import net.mada.lumea.ui.advisor.AdvisorScreen
import net.mada.lumea.ui.agenda.AgendaScreen
import net.mada.lumea.ui.ai.AiProfileScreen
import net.mada.lumea.ui.ai.openAiTab
import net.mada.lumea.ui.ai.AiTransparencyScreen
import net.mada.lumea.ui.agenda.EventEditorScreen
import net.mada.lumea.ui.components.FloatingNavBar
import net.mada.lumea.ui.components.NavBarItem
import net.mada.lumea.ui.cycle.CycleScreen
import net.mada.lumea.ui.cycle.CycleSetupScreen
import net.mada.lumea.ui.cycle.DayLogScreen
import net.mada.lumea.ui.learn.LessonReaderScreen
import net.mada.lumea.ui.learn.LessonsScreen
import net.mada.lumea.ui.journal.HabitsScreen
import net.mada.lumea.ui.journal.DayReaderScreen
import net.mada.lumea.ui.journal.JournalScreen
import net.mada.lumea.ui.notes.NoteEditorScreen
import net.mada.lumea.ui.notes.NoteReaderScreen
import net.mada.lumea.ui.notes.NotesScreen
import net.mada.lumea.ui.pregnancy.EmergencyScreen
import net.mada.lumea.ui.pregnancy.PregnancyScreen
import net.mada.lumea.ui.settings.SettingsScreen
import net.mada.lumea.ui.theme.LocalLumeaStyle
import net.mada.lumea.ui.theme.screenGlow
import net.mada.lumea.ui.today.TodayScreen
import java.time.LocalDate

object Routes {
    const val TODAY = "today"
    const val NOTES = "notes"
    const val AGENDA = "agenda"
    const val CYCLE = "cycle"
    const val JOURNAL = "journal"
    const val SETTINGS = "settings"
    const val HABITS = "habits"
    const val CYCLE_SETUP = "cycle-setup"

    const val NOTE_EDITOR = "note/{noteId}/edit"
    fun noteEditor(noteId: Long) = "note/$noteId/edit"

    const val NOTE_READER = "note/{noteId}"
    fun noteReader(noteId: Long) = "note/$noteId"

    const val EVENT_EDITOR = "event/{eventId}?date={date}"
    fun eventEditor(eventId: Long, date: LocalDate? = null) =
        "event/$eventId?date=${date?.toEpochDay() ?: -1L}"

    const val DAY_LOG = "log/{date}/edit"
    fun dayLog(date: LocalDate) = "log/${date.toEpochDay()}/edit"

    const val DAY_READER = "log/{date}"
    fun dayReader(date: LocalDate) = "log/${date.toEpochDay()}"

    const val LESSONS = "lessons"

    const val PREGNANCY = "pregnancy"
    const val EMERGENCY = "emergency"
    const val ADVISOR = "advisor"
    const val AI = "ai"
    const val AI_PROFILE = "ai/profile"


    const val LESSON = "lesson/{lessonId}"
    fun lesson(id: String) = "lesson/$id"
}

private data class Tab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

@Composable
fun LumeaNavHost(settings: Settings) {
    val navController = rememberNavController()

    val tabs = buildList {
        add(Tab(Routes.TODAY, "Aujourd'hui", Icons.Rounded.WbSunny, Icons.Outlined.WbSunny))
        add(Tab(Routes.NOTES, "Notes", Icons.Rounded.StickyNote2, Icons.Outlined.StickyNote2))
        add(Tab(Routes.AGENDA, "Agenda", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth))
        if (settings.cycleTabVisible) {
            add(Tab(Routes.CYCLE, "Cycle", Icons.Rounded.Favorite, Icons.Outlined.Favorite))
        }
        add(Tab(Routes.JOURNAL, "Journal", Icons.Rounded.MenuBook, Icons.Outlined.MenuBook))
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = tabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    val scheme = MaterialTheme.colorScheme
    val glow = LocalLumeaStyle.current.showGlow

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .then(if (glow) Modifier.screenGlow(scheme.primary, scheme.tertiary) else Modifier)
        ) {
            // Transitions maison : glissement court et discret, désactivable dans les
            // réglages (certaines personnes préfèrent, d'autres en ont besoin).
            val animate = LocalLumeaStyle.current.animations
            val slide = tween<Float>(260)

            NavHost(
                navController = navController,
                startDestination = Routes.TODAY,
                enterTransition = {
                    if (animate) fadeIn(slide) + slideInHorizontally(tween(260)) { it / 12 }
                    else fadeIn(tween(0))
                },
                exitTransition = { if (animate) fadeOut(slide) else fadeOut(tween(0)) },
                popEnterTransition = {
                    if (animate) fadeIn(slide) + slideInHorizontally(tween(260)) { -it / 12 }
                    else fadeIn(tween(0))
                },
                popExitTransition = { if (animate) fadeOut(slide) else fadeOut(tween(0)) },
            ) {
                composable(Routes.TODAY) {
                    TodayScreen(
                        settings = settings,
                        onOpenNote = { navController.navigate(Routes.noteReader(it)) },
                        onOpenEvent = { navController.navigate(Routes.eventEditor(it)) },
                        onOpenDayLog = { navController.navigate(Routes.dayLog(it)) },
                        onReadDayLog = { navController.navigate(Routes.dayReader(it)) },
                        onOpenTab = { navController.navigateToTab(it) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenLessons = { navController.navigate(Routes.LESSONS) },
                        onOpenPregnancy = { navController.navigate(Routes.PREGNANCY) },
                        onOpenAdvisor = { navController.navigate(Routes.ADVISOR) },
                        onOpenAi = { navController.navigate(Routes.AI) },
                        onSetPin = { navController.navigate(Routes.SETTINGS) },
                    )
                }

                composable(Routes.NOTES) {
                    NotesScreen(
                        onOpenNote = { navController.navigate(Routes.noteReader(it)) },
                        onNewNote = { navController.navigate(Routes.noteEditor(0L)) },
                    )
                }

                composable(
                    Routes.NOTE_READER,
                    arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("noteId") ?: 0L
                    NoteReaderScreen(
                        noteId = id,
                        settings = settings,
                        onEdit = { navController.navigate(Routes.noteEditor(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    Routes.NOTE_EDITOR,
                    arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                ) { entry ->
                    NoteEditorScreen(
                        noteId = entry.arguments?.getLong("noteId") ?: 0L,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.AGENDA) {
                    AgendaScreen(
                        onOpenEvent = { navController.navigate(Routes.eventEditor(it)) },
                        onNewEvent = { date -> navController.navigate(Routes.eventEditor(0L, date)) },
                    )
                }

                composable(
                    Routes.EVENT_EDITOR,
                    arguments = listOf(
                        navArgument("eventId") { type = NavType.LongType },
                        navArgument("date") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) { entry ->
                    val epochDay = entry.arguments?.getLong("date") ?: -1L
                    EventEditorScreen(
                        eventId = entry.arguments?.getLong("eventId") ?: 0L,
                        initialDate = epochDay.takeIf { it >= 0 }?.let(LocalDate::ofEpochDay),
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.CYCLE) {
                    CycleScreen(
                        onOpenDayLog = { navController.navigate(Routes.dayLog(it)) },
                        onOpenSetup = { navController.navigate(Routes.CYCLE_SETUP) },
                        onOpenLessons = { navController.navigate(Routes.LESSONS) },
                        onOpenPregnancy = { navController.navigate(Routes.PREGNANCY) },
                        onOpenAdvisor = { navController.navigate(Routes.ADVISOR) },
                    )
                }

                composable(Routes.PREGNANCY) {
                    PregnancyScreen(
                        onBack = { navController.popBackStack() },
                        onOpenEmergency = { navController.navigate(Routes.EMERGENCY) },
                    )
                }

                composable(Routes.EMERGENCY) {
                    EmergencyScreen(
                        onBack = { navController.popBackStack() },
                        onExportPdf = { navController.popBackStack() },
                    )
                }

                composable(Routes.ADVISOR) {
                    AdvisorScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPregnancy = { navController.navigate(Routes.PREGNANCY) },
                        onOpenLessons = { navController.navigate(Routes.LESSONS) },
                        onWriteLog = { navController.navigate(Routes.dayLog(LocalDate.now())) },
                        onOpenAi = { navController.navigate(Routes.AI) },
                    )
                }

                composable(Routes.AI_PROFILE) {
                    AiProfileScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAssistant = { navController.navigate(Routes.AI) },
                    )
                }

                composable(Routes.AI) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val toolbar = MaterialTheme.colorScheme.surface.toArgb()
                    AiTransparencyScreen(
                        onBack = { navController.popBackStack() },
                        // L'onglet s'ouvre par-dessus l'app : plus de destination
                        // interne, donc plus d'écran à maintenir ni de page blanche.
                        onAccept = { service -> openAiTab(context, service.url, toolbar) },
                        onPersonalise = { navController.navigate(Routes.AI_PROFILE) },
                    )
                }


                composable(Routes.CYCLE_SETUP) {
                    CycleSetupScreen(onDone = { navController.popBackStack() })
                }

                composable(
                    Routes.DAY_LOG,
                    arguments = listOf(navArgument("date") { type = NavType.LongType }),
                ) { entry ->
                    DayLogScreen(
                        date = LocalDate.ofEpochDay(entry.arguments?.getLong("date") ?: LocalDate.now().toEpochDay()),
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.JOURNAL) {
                    JournalScreen(
                        onOpenDayLog = { navController.navigate(Routes.dayReader(it)) },
                        onWriteToday = { navController.navigate(Routes.dayLog(it)) },
                        onManageHabits = { navController.navigate(Routes.HABITS) },
                    )
                }

                composable(
                    Routes.DAY_READER,
                    arguments = listOf(navArgument("date") { type = NavType.LongType }),
                ) { entry ->
                    val date = LocalDate.ofEpochDay(
                        entry.arguments?.getLong("date") ?: LocalDate.now().toEpochDay()
                    )
                    DayReaderScreen(
                        date = date,
                        settings = settings,
                        onEdit = { navController.navigate(Routes.dayLog(date)) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.HABITS) {
                    HabitsScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCycleSetup = { navController.navigate(Routes.CYCLE_SETUP) },
                        onOpenAiProfile = { navController.navigate(Routes.AI_PROFILE) },
                    )
                }

                composable(Routes.LESSONS) {
                    LessonsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenLesson = { navController.navigate(Routes.lesson(it)) },
                    )
                }

                composable(
                    Routes.LESSON,
                    arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
                ) { entry ->
                    LessonReaderScreen(
                        lessonId = entry.arguments?.getString("lessonId").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenLesson = { navController.navigate(Routes.lesson(it)) },
                    )
                }
            }
        }

        // La barre flotte par-dessus le contenu : c'est ce qui rend l'effet de verre
        // crédible, puisqu'on voit les cartes défiler derrière elle.
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
        ) {
            FloatingNavBar(
                items = tabs.map {
                    NavBarItem(it.route, it.label, it.selectedIcon, it.icon)
                },
                selectedKey = tabs.firstOrNull { tab ->
                    currentDestination?.hierarchy?.any { it.route == tab.route } == true
                }?.route,
                onSelect = { navController.navigateToTab(it) },
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

