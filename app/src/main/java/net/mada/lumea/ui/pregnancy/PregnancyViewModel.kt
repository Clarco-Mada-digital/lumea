package net.mada.lumea.ui.pregnancy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.mada.lumea.data.db.EventEntity
import net.mada.lumea.data.db.PregnancyEntity
import net.mada.lumea.data.db.PrenatalCareEntity
import net.mada.lumea.data.repo.CycleRepository
import net.mada.lumea.data.repo.EventRepository
import net.mada.lumea.data.repo.PregnancyRepository
import net.mada.lumea.domain.pregnancy.ALL_CARE_ACTS
import net.mada.lumea.domain.pregnancy.INFANT_VACCINES
import net.mada.lumea.domain.pregnancy.NextCare
import net.mada.lumea.domain.pregnancy.POSTNATAL_VISITS
import net.mada.lumea.domain.pregnancy.nextCareAction
import net.mada.lumea.domain.pregnancy.CareCategory
import net.mada.lumea.domain.pregnancy.MamaCondition
import net.mada.lumea.domain.pregnancy.MamaStatus
import net.mada.lumea.domain.pregnancy.PostpartumProgress
import net.mada.lumea.domain.pregnancy.PregnancyProgress
import net.mada.lumea.domain.pregnancy.mamaStatus
import net.mada.lumea.domain.pregnancy.postpartumProgress
import net.mada.lumea.domain.pregnancy.careDate
import net.mada.lumea.domain.pregnancy.pregnancyProgress
import java.time.LocalDate
import java.time.ZoneId

/** Les trois réponses possibles à « tu as fait un test ? ». */
enum class TestResult(val stored: String) {
    POSITIVE("POSITIVE"),
    NEGATIVE("NEGATIVE"),
    UNCLEAR("UNCLEAR"),
}

data class PregnancyUiState(
    val ongoing: PregnancyEntity? = null,
    val progress: PregnancyProgress? = null,
    val lastTest: PregnancyEntity? = null,
    /** Les actes déjà confirmés, par code : "CPN1" → la ligne du carnet. */
    val care: Map<String, PrenatalCareEntity> = emptyMap(),
    /** Renseigné une fois l'enfant né, jusqu'au retour des règles. */
    val postpartum: PostpartumProgress? = null,
) {
    val riskCodes: Set<String>
        get() = ongoing?.riskFactors.orEmpty()
            .split(",").filter { it.isNotBlank() }.toSet()

    /** Vrai tant que l'app suit l'après-naissance plutôt que la grossesse. */
    val isPostpartum: Boolean get() = ongoing?.status == "POSTPARTUM"

    /**
     * La prochaine chose à faire, ce qui reste après avoir retiré les cases cochées.
     *
     * C'est elle que l'accueil et l'en-tête du suivi affichent, au lieu de répéter
     * l'étape de la semaine en cours même quand elle est déjà faite.
     */
    val nextCare: NextCare?
        get() {
            val current = ongoing ?: return null
            return if (isPostpartum) {
                val birth = current.birthDate ?: return null
                nextCareAction(
                    anchor = birth,
                    acts = POSTNATAL_VISITS + INFANT_VACCINES,
                    doneCodes = care.keys,
                    currentWeek = postpartum?.weeksSince ?: 0,
                )
            } else {
                val start = current.lastPeriodStart ?: return null
                nextCareAction(
                    anchor = start,
                    acts = ALL_CARE_ACTS,
                    doneCodes = care.keys,
                    currentWeek = progress?.weeks ?: 0,
                )
            }
        }

    /** L'état de la contraception par allaitement, d'après les cases cochées. */
    val mama: MamaStatus
        get() = mamaStatus(MamaCondition.entries.map { it.code }.filter { care.containsKey(it) }.toSet())
}

@OptIn(ExperimentalCoroutinesApi::class)
class PregnancyViewModel(
    private val repo: PregnancyRepository,
    private val cycleRepo: CycleRepository,
    private val events: EventRepository? = null,
    private val settingsRepo: net.mada.lumea.data.prefs.SettingsRepository? = null,
) : ViewModel() {

    val state: StateFlow<PregnancyUiState> = repo.observeOngoing()
        .flatMapLatest { ongoing ->
            val careFlow = if (ongoing != null) repo.observeCare(ongoing.id)
            else flowOf(emptyList())
            combine(repo.observeAll(), careFlow) { all, care ->
                PregnancyUiState(
                    ongoing = ongoing,
                    progress = ongoing?.lastPeriodStart?.let { pregnancyProgress(it) },
                    lastTest = all.firstOrNull(),
                    care = care.associateBy { it.code },
                    postpartum = ongoing?.birthDate?.let { postpartumProgress(it) },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PregnancyUiState())

    // ------------------------------------------------------ Le carnet de suivi

    /** Confirme qu'un acte a bien été fait, ou revient en arrière. */
    fun setCareDone(code: String, done: Boolean, on: LocalDate = LocalDate.now()) =
        viewModelScope.launch {
            val id = state.value.ongoing?.id ?: return@launch
            repo.setCareDone(id, code, done, on)
        }

    /** Note un résultat : groupe sanguin, terme donné par l'échographie, hémoglobine. */
    fun setCareNote(code: String, note: String) = viewModelScope.launch {
        val id = state.value.ongoing?.id ?: return@launch
        repo.setCareNote(id, code, note)
    }

    fun setCaregiver(name: String, role: String, phone: String, facility: String) =
        viewModelScope.launch {
            val id = state.value.ongoing?.id ?: return@launch
            repo.setCaregiver(id, name, role, phone, facility)
        }

    fun toggleRiskFactor(code: String) = viewModelScope.launch {
        val current = state.value
        val id = current.ongoing?.id ?: return@launch
        val codes = current.riskCodes.toMutableSet()
        if (!codes.add(code)) codes.remove(code)
        repo.setRiskFactors(id, codes)
    }

    /**
     * Pose les rendez-vous du carnet dans l'agenda, avec un rappel la veille.
     *
     * On réutilise les événements de l'app plutôt que d'inventer un deuxième
     * système de notifications : les rappels, la reprogrammation au redémarrage et
     * l'affichage au mois existent déjà et sont éprouvés.
     */
    fun scheduleCareInAgenda(onScheduled: (Int) -> Unit) = viewModelScope.launch {
        val repository = events ?: return@launch
        val start = state.value.ongoing?.lastPeriodStart ?: return@launch
        val today = LocalDate.now()
        var count = 0

        ALL_CARE_ACTS
            .filter { it.category != CareCategory.SUPPLEMENT }
            .filter { state.value.care[it.code] == null }
            .forEach { act ->
                val date = careDate(act, start)
                // On ne programme rien dans le passé : ce serait une notification
                // immédiate et inutile pour un rendez-vous déjà manqué.
                if (date.isBefore(today)) return@forEach
                val at = date.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.save(
                    EventEntity(
                        title = act.title,
                        notes = act.why,
                        location = state.value.ongoing?.facility.orEmpty(),
                        startAt = at,
                        endAt = at + 3_600_000,
                        allDay = false,
                        colorIndex = 0,
                        // La veille : assez tôt pour s'organiser, assez tard pour
                        // qu'on s'en souvienne encore.
                        reminderMinutes = 24 * 60,
                        // Marqué comme venant du carnet : c'est ce qui permet
                        // de les retirer tous quand le suivi est effacé.
                        source = net.mada.lumea.data.db.EVENT_SOURCE_CARE,
                    )
                )
                count++
            }
        onScheduled(count)
    }

    private val _justRecorded = MutableStateFlow<TestResult?>(null)

    /** Le résultat qui vient d'être saisi, pour que l'écran sache quoi afficher ensuite. */
    val justRecorded: StateFlow<TestResult?> = _justRecorded.asStateFlow()
    fun clearJustRecorded() { _justRecorded.value = null }

    /**
     * Enregistre un test.
     *
     * La date des dernières règles est figée au moment du test : c'est d'elle que
     * découlent les semaines d'aménorrhée, et elle ne doit plus bouger même si
     * l'historique des règles est corrigé plus tard.
     */
    fun recordTest(result: TestResult, testedOn: LocalDate = LocalDate.now()) =
        viewModelScope.launch {
            val lastPeriod = cycleRepo.allPeriods().maxByOrNull { it.startDate }?.startDate
            repo.recordTest(result.stored, lastPeriod, testedOn)
            _justRecorded.value = result
        }

    /**
     * L'enfant est né : on bascule en suivi d'après la naissance.
     *
     * Le suivi continue au lieu de s'arrêter — c'est là que se jouent la
     * contraception par allaitement et les visites postnatales.
     */
    fun recordBirth(on: LocalDate) = viewModelScope.launch {
        val id = state.value.ongoing?.id ?: return@launch
        repo.recordBirth(id, on)
    }

    /**
     * Le retour de couches, qui referme la boucle.
     *
     * On clôt le suivi **et** on enregistre ces règles comme premier jour d'un
     * nouveau cycle : le moteur repart de là, et l'écran Cycle retrouve son anneau,
     * son calendrier et ses prévisions.
     */
    fun recordPeriodReturn(on: LocalDate) = viewModelScope.launch {
        val id = state.value.ongoing?.id ?: return@launch
        repo.endPostpartum(id, on)
        cycleRepo.startPeriod(on)
    }

    /**
     * Écrit le carnet en PDF, pour le montrer en consultation.
     *
     * Ne contient que le suivi : ni journal, ni humeur, ni notes. Un document
     * qu'on tend à quelqu'un ne doit contenir que ce qu'on accepte de montrer.
     */
    fun exportPdf(context: android.content.Context, uri: android.net.Uri, onDone: (Boolean) -> Unit) =
        viewModelScope.launch {
            val current = state.value.ongoing
            if (current == null) {
                onDone(false)
                return@launch
            }
            val name = settingsRepo?.settings?.first()?.displayName.orEmpty()
            val result = net.mada.lumea.export.CarnetPdf.write(
                context = context,
                uri = uri,
                pregnancy = current,
                care = state.value.care,
                displayName = name,
            )
            onDone(result.isSuccess)
        }

    fun suggestedPdfName() = net.mada.lumea.export.CarnetPdf.suggestedFileName()

    /** Le refus est définitif : on ne repropose pas le code à chaque ouverture. */
    fun dismissPinInvitation() = viewModelScope.launch {
        settingsRepo?.dismissPinInvitation()
    }

    /**
     * Ferme le suivi, et emporte les rendez-vous qu'il avait posés.
     *
     * Sans ça, les CPN et les vaccins programmés restaient dans l'agenda après la
     * fermeture du suivi : des rappels pour des rendez-vous qui n'existent plus,
     * sans moyen évident de comprendre d'où ils venaient.
     */
    fun endFollowUp(alsoRemoveEvents: Boolean = true) = viewModelScope.launch {
        state.value.ongoing?.let { repo.endFollowUp(it.id) }
        if (alsoRemoveEvents) events?.deleteGeneratedCare()
    }

    /** Combien de rendez-vous du carnet sont encore posés dans l'agenda. */
    suspend fun generatedEventCount(): Int = events?.countGeneratedCare() ?: 0

    /** Corrige la date de départ quand l'échographie donne un autre terme. */
    fun correctStartDate(date: LocalDate) = viewModelScope.launch {
        val current = state.value.ongoing ?: return@launch
        repo.recordTest(current.result, date, current.testedOn)
        repo.delete(current.id)
    }
}
