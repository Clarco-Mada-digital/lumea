package net.mada.lumea.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mada.lumea.data.security.DatabaseKeyProvider
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * La sauvegarde qui se fait toute seule.
 *
 * L'export manuel n'existe que si on y pense — et on n'y pense jamais avant de
 * casser son téléphone. Dans un contexte où l'appareil se perd, se vole, se
 * partage ou tombe en panne, une sauvegarde qui dépend d'un geste volontaire
 * n'en est pas vraiment une.
 *
 * Celle-ci tourne avec la vérification quotidienne, une fois par semaine, et
 * écrit dans le dossier privé de l'application.
 *
 * **Ce qu'elle protège, et ce qu'elle ne protège pas.** Elle couvre le cas le
 * plus fréquent : l'app désinstallée par erreur, les données effacées, une
 * restauration ratée. Elle ne couvre pas le téléphone perdu — le fichier part
 * avec lui. D'où le rappel mensuel qui invite à en copier une ailleurs : c'est
 * le seul geste que l'application ne peut pas faire à la place de quelqu'un.
 *
 * **Chiffrement.** Le fichier est chiffré avec la clé de la base, tirée du
 * Keystore matériel. Il est donc illisible hors de ce téléphone — y compris par
 * une autre application qui viendrait fouiller. En contrepartie, il ne sert pas
 * à migrer vers un nouvel appareil : pour ça, il faut un export manuel avec une
 * phrase de passe, et l'écran le dit.
 */
class AutoBackup(private val context: Context, private val backup: BackupManager) {

    /** Combien de copies on garde. Au-delà, la plus ancienne est effacée. */
    private val keep = 4

    private val folder: File
        get() = File(context.filesDir, "sauvegardes").apply { if (!exists()) mkdirs() }

    /**
     * Écrit une sauvegarde si la dernière date de plus d'une semaine.
     *
     * Renvoie `true` si un fichier a été écrit. Toute erreur est avalée : une
     * sauvegarde qui échoue ne doit jamais empêcher l'application de démarrer ni
     * faire remonter une alerte, elle réessaiera demain.
     */
    suspend fun runIfDue(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val last = latest()
            if (!force && last != null) {
                val age = LocalDate.now().toEpochDay() - lastModifiedDate(last).toEpochDay()
                if (age < 7) return@runCatching false
            }

            // La clé de la base sert aussi ici : le fichier est donc illisible
            // hors de ce téléphone, Keystore matériel compris.
            val key = DatabaseKeyProvider.passphrase(context)
            val bytes = backup.serialize(String(key, Charsets.ISO_8859_1).toCharArray())
            val name = "lumea-${LocalDateTime.now().format(STAMP)}.lumea"
            File(folder, name).writeBytes(bytes)

            prune()
            true
        }.getOrDefault(false)
    }

    /** La sauvegarde automatique la plus récente, s'il y en a une. */
    fun latest(): File? = folder.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".lumea") }
        ?.maxByOrNull { it.lastModified() }

    /** Depuis combien de jours la dernière copie a-t-elle été écrite ? */
    fun daysSinceLatest(): Int? = latest()?.let {
        (LocalDate.now().toEpochDay() - lastModifiedDate(it).toEpochDay()).toInt()
    }

    fun all(): List<File> = folder.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".lumea") }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

    fun clear() {
        folder.listFiles()?.forEach { it.delete() }
    }

    /** Ne garde que les [keep] dernières : inutile d'accumuler des mois de copies. */
    private fun prune() {
        all().drop(keep).forEach { it.delete() }
    }

    private fun lastModifiedDate(file: File): LocalDate =
        java.time.Instant.ofEpochMilli(file.lastModified())
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()

    private companion object {
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
    }
}
