package net.mada.lumea.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Les fichiers joints aux notes et au journal : photos et enregistrements.
 *
 * **Le problème corrigé ici.** Les photos étaient référencées par leur chemin
 * absolu, du genre `/data/user/0/net.mada.lumea/files/media/img_123.jpg`, et ce
 * chemin partait dans la sauvegarde — mais pas le fichier. Restaurer sur un autre
 * téléphone, ou simplement réinstaller l'application, laissait des notes pleines
 * d'images introuvables. Les souvenirs étaient perdus sans que personne ne soit
 * prévenu.
 *
 * Deux changements. D'abord, seul le **nom du fichier** est écrit dans la note :
 * il se résout au moment de l'affichage, donc il survit à un changement de
 * chemin. Ensuite les fichiers eux-mêmes voyagent dans la sauvegarde.
 *
 * **Pourquoi les photos sont réduites.** Une photo de téléphone pèse trois à cinq
 * mégaoctets. Vingt photos, et la sauvegarde devient intransportable — surtout
 * quand on l'envoie en données mobiles. Réduites à 1600 pixels de large, elles
 * tiennent en quelques centaines de kilooctets sans perte visible à l'écran.
 */
class MediaStore(private val context: Context) {

    /** Largeur maximale d'une photo enregistrée. Au-delà, aucun écran n'y gagne. */
    private val maxWidth = 1600

    private val folder: File
        get() = File(context.filesDir, "media").apply { if (!exists()) mkdirs() }

    fun file(name: String): File = File(folder, name)

    /**
     * Résout une référence écrite dans une note.
     *
     * Accepte aussi les anciens chemins absolus : des notes existantes en
     * contiennent, et il n'est pas question de les casser en corrigeant le format.
     */
    fun resolve(reference: String): File =
        if (reference.contains('/')) File(reference) else file(reference)

    fun exists(reference: String): Boolean = resolve(reference).exists()

    /**
     * Copie une image choisie, en la réduisant.
     *
     * Renvoie le nom du fichier — jamais son chemin — ou `null` si la lecture
     * échoue.
     */
    suspend fun saveImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0) return@runCatching null

            var sample = 1
            while (bounds.outWidth / sample > maxWidth) sample *= 2

            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(
                    it, null,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            } ?: return@runCatching null

            val name = "img_${System.currentTimeMillis()}.jpg"
            file(name).outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            name
        }.getOrNull()
    }

    /** Un nom de fichier pour un nouvel enregistrement audio. */
    fun newAudioName(): String = "aud_${System.currentTimeMillis()}.m4a"

    fun delete(reference: String) {
        runCatching { resolve(reference).delete() }
    }

    /** Tous les fichiers joints, pour les emporter dans une sauvegarde. */
    fun all(): List<File> = folder.listFiles()?.filter { it.isFile }.orEmpty()

    /** Réécrit un fichier reçu d'une sauvegarde. N'écrase jamais un fichier existant. */
    fun restore(name: String, bytes: ByteArray) {
        val target = file(name)
        if (target.exists()) return
        runCatching { target.writeBytes(bytes) }
    }

    /** Poids total des fichiers joints, pour l'afficher avant un export. */
    fun totalBytes(): Long = all().sumOf { it.length() }
}
