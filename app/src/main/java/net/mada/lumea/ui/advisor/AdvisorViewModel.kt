package net.mada.lumea.ui.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import net.mada.lumea.data.prefs.SettingsRepository
import net.mada.lumea.data.repo.CycleRepository
import net.mada.lumea.data.repo.PregnancyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.mada.lumea.domain.advisor.Advice
import net.mada.lumea.domain.advisor.AdvisorContext
import net.mada.lumea.domain.advisor.Topic
import net.mada.lumea.domain.advisor.advise
import net.mada.lumea.domain.advisor.adviseOn
import net.mada.lumea.domain.cycle.CycleEngine
import net.mada.lumea.domain.pregnancy.ALL_CARE_ACTS
import net.mada.lumea.domain.pregnancy.INFANT_VACCINES
import net.mada.lumea.domain.pregnancy.MamaCondition
import net.mada.lumea.domain.pregnancy.POSTNATAL_VISITS
import net.mada.lumea.domain.pregnancy.headline
import net.mada.lumea.domain.pregnancy.CareStatus
import net.mada.lumea.domain.pregnancy.mamaStatus
import net.mada.lumea.domain.pregnancy.nextCareAction
import net.mada.lumea.domain.pregnancy.postpartumProgress
import net.mada.lumea.domain.pregnancy.pregnancyProgress

/**
 * Rassemble tout ce que le conseiller doit savoir pour répondre.
 *
 * Les trois sources — cycle, grossesse, carnet de suivi — sont déjà en base ;
 * ce ViewModel ne fait que les réunir. Rien n'est appelé sur le réseau, et
 * l'objet construit ici ne quitte jamais le processus de l'application.
 */
/** Une ligne de la conversation : la question posée, ou la réponse donnée. */
sealed interface Turn {
    data class Question(val text: String) : Turn
    data class Answer(val advice: Advice) : Turn
}

/**
 * L'historique de la conversation, gardé en mémoire pour toute la session.
 *
 * Il vit dans le conteneur de l'application et non dans le ViewModel : celui-ci
 * est détruit dès qu'on quitte l'écran, ce qui effaçait la conversation au moindre
 * aller-retour vers le suivi ou les leçons.
 *
 * Il n'est volontairement **pas** écrit sur disque. Les questions posées ici
 * touchent à des sujets intimes ; les garder en mémoire le temps de la session
 * suffit à l'usage, et elles disparaissent quand l'app se ferme.
 */
class AdvisorConversation {
    private val _turns = MutableStateFlow<List<Turn>>(emptyList())
    val turns: StateFlow<List<Turn>> = _turns.asStateFlow()

    fun add(vararg items: Turn) { _turns.value = _turns.value + items }
    fun clear() { _turns.value = emptyList() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AdvisorViewModel(
    private val cycleRepo: CycleRepository,
    private val settingsRepo: SettingsRepository,
    private val pregnancyRepo: PregnancyRepository,
    private val conversation: AdvisorConversation,
) : ViewModel() {

    val turns: StateFlow<List<Turn>> = conversation.turns

    /** Pose une question et enregistre la réponse dans l'historique de la session. */
    fun ask(text: String, topic: Topic? = null) {
        if (text.isBlank()) return
        val advice = if (topic != null) adviseOn(topic, context.value)
        else advise(text, context.value)
        conversation.add(Turn.Question(text), Turn.Answer(advice))
    }

    fun clearConversation() = conversation.clear()

    val context: StateFlow<AdvisorContext> = pregnancyRepo.observeOngoing()
        .flatMapLatest { ongoing ->
            val careFlow = if (ongoing != null) pregnancyRepo.observeCare(ongoing.id)
            else flowOf(emptyList())

            combine(
                cycleRepo.observePeriods(),
                settingsRepo.settings,
                careFlow,
            ) { periods, settings, care ->
                val engine = CycleEngine(
                    settings.cycleLength,
                    settings.periodLength,
                    settings.lutealLength,
                )
                val doneCodes = care.map { it.code }.toSet()
                val isPostpartum = ongoing?.status == "POSTPARTUM"
                val progress = ongoing?.lastPeriodStart
                    ?.takeIf { !isPostpartum }
                    ?.let { pregnancyProgress(it) }
                val after = ongoing?.birthDate
                    ?.takeIf { isPostpartum }
                    ?.let { postpartumProgress(it) }

                val next = when {
                    after != null -> nextCareAction(
                        anchor = after.birthDate,
                        acts = POSTNATAL_VISITS + INFANT_VACCINES,
                        doneCodes = doneCodes,
                        currentWeek = after.weeksSince,
                    )
                    progress != null && ongoing.lastPeriodStart != null -> nextCareAction(
                        anchor = ongoing.lastPeriodStart,
                        acts = ALL_CARE_ACTS,
                        doneCodes = doneCodes,
                        currentWeek = progress.weeks,
                    )
                    else -> null
                }

                AdvisorContext(
                    insight = engine.insight(periods),
                    pregnancy = progress,
                    postpartum = after,
                    mamaProtected = mamaStatus(
                        MamaCondition.entries.map { it.code }.filter { it in doneCodes }.toSet()
                    ).protected,
                    caregiverName = ongoing?.caregiverName.orEmpty(),
                    caregiverPhone = ongoing?.caregiverPhone.orEmpty(),
                    nextCareTitle = next?.headline,
                    nextCareLate = next?.status == CareStatus.EN_RETARD,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvisorContext())
}
