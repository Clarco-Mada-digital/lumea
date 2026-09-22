package net.mada.lumea.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Un dossier de notes (Perso, Travail, Idées…). */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "📁",
    val colorIndex: Int = 0,
    val position: Int = 0,
)

/**
 * Une note. Le corps est du texte libre ; si [isChecklist] est vrai, chaque ligne
 * du corps préfixée par "[x] " ou "[ ] " est rendue comme une case à cocher.
 */
@Entity(
    tableName = "notes",
    indices = [Index("folderId"), Index("updatedAt"), Index("isArchived")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val folderId: Long? = null,
    val tags: String = "",
    val colorIndex: Int = 0,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isChecklist: Boolean = false,
    val isArchived: Boolean = false,
    /** Note protégée : son contenu n'apparaît qu'après saisie du code. */
    val isLocked: Boolean = false,
    val linkedDate: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Un événement d'agenda. Les instants sont des epoch-millis en heure locale de l'appareil. */
@Entity(tableName = "events", indices = [Index("startAt")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val location: String = "",
    val startAt: Long,
    val endAt: Long,
    val allDay: Boolean = false,
    val colorIndex: Int = 0,
    /** Minutes avant le début ; null = aucun rappel. */
    val reminderMinutes: Int? = 15,
    /** NONE, DAILY, WEEKLY, MONTHLY, YEARLY */
    val repeat: String = "NONE",
    val isDone: Boolean = false,
    val linkedNoteId: Long? = null,
)

/** Une période de règles réellement enregistrée par l'utilisatrice. */
@Entity(tableName = "periods", indices = [Index(value = ["startDate"], unique = true)])
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    /** null tant que les règles sont en cours. */
    val endDate: LocalDate? = null,
)

/**
 * Un test de grossesse, et ce qu'il est devenu.
 *
 * On garde les tests négatifs autant que les positifs : « j'ai déjà testé il y a
 * trois jours » est l'information qui dit s'il faut retester ou consulter. Une
 * seule ligne peut être `ongoing` à la fois — c'est la grossesse en cours.
 */
@Entity(tableName = "pregnancies")
data class PregnancyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Jour où le test a été fait. */
    val testedOn: LocalDate,
    /** "POSITIVE", "NEGATIVE" ou "UNCLEAR". */
    val result: String,
    /**
     * Premier jour des dernières règles au moment du test : c'est de cette date
     * que découlent les semaines d'aménorrhée et le terme. Figée ici pour que le
     * calcul ne bouge plus si l'historique des règles est modifié plus tard.
     */
    val lastPeriodStart: LocalDate? = null,
    /** "ONGOING", "ENDED" ou "NONE" (test négatif ou non suivi). */
    val status: String = "NONE",
    /** Date de fin du suivi, quelle qu'en soit la raison. Rien n'est demandé ici. */
    val endedOn: LocalDate? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),

    // --- Le soignant qui suit la grossesse -------------------------------
    /** Nom de la sage-femme, du médecin ou de l'agent du CSB. */
    val caregiverName: String = "",
    /** « Sage-femme », « Médecin », « Agent communautaire »… */
    val caregiverRole: String = "",
    val caregiverPhone: String = "",
    /** Le CSB ou la maternité où se fait le suivi. */
    val facility: String = "",

    /**
     * Facteurs de risque cochés, séparés par des virgules.
     *
     * Ils ne servent pas à poser un diagnostic — l'app n'en est pas capable — mais
     * à adapter les conseils affichés et à rappeler ce qu'il faut signaler.
     */
    val riskFactors: String = "",

    /**
     * Date de naissance de l'enfant.
     *
     * Elle fait basculer le suivi en « après la naissance » : le statut passe à
     * POSTPARTUM et l'app continue d'accompagner jusqu'au retour des règles, au
     * lieu de s'arrêter à l'accouchement comme le font la plupart des applis.
     */
    val birthDate: LocalDate? = null,
)

/**
 * Un acte de suivi confirmé : une CPN faite, un vaccin reçu, une dose de TPIg avalée.
 *
 * C'est la différence entre une liste de conseils et un vrai carnet : ce qui est
 * coché ici est ce qui a réellement eu lieu, avec sa date et, le cas échéant, le
 * résultat noté (tension, poids, compte rendu d'échographie).
 */
@Entity(
    tableName = "prenatal_care",
    indices = [Index(value = ["pregnancyId", "code"], unique = true)],
)
data class PrenatalCareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pregnancyId: Long,
    /** Code stable venant de `MadagascarCare` : "CPN1", "VAT2", "TPI1"… */
    val code: String,
    val doneOn: LocalDate,
    /** Ce qui a été dit ou mesuré : résultat d'échographie, tension, remarque. */
    val note: String = "",
)

/**
 * Le carnet du jour : il sert à la fois au suivi de cycle (flux, symptômes)
 * et au journal intime (texte, humeur, gratitude).
 */
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val date: LocalDate,
    /** 0 = aucun, 1 = léger, 2 = moyen, 3 = abondant, 4 = très abondant. */
    val flow: Int = 0,
    val symptoms: String = "",
    /** 1 (très bas) à 5 (rayonnant) ; 0 = non renseigné. */
    val mood: Int = 0,
    /** 1 à 5 ; 0 = non renseigné. */
    val energy: Int = 0,
    val journal: String = "",
    val gratitude: String = "",
    val waterGlasses: Int = 0,
    val sleepHours: Float = 0f,
    /** Journée protégée : son texte n'apparaît qu'après saisie du code. */
    val isLocked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Une habitude à cocher chaque jour. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "✨",
    val colorIndex: Int = 0,
    val isActive: Boolean = true,
    val position: Int = 0,
)

/** Une case cochée : l'habitude [habitId] a été faite le jour [date]. */
@Entity(tableName = "habit_checks", primaryKeys = ["habitId", "date"], indices = [Index("date")])
data class HabitCheckEntity(
    val habitId: Long,
    val date: LocalDate,
)
