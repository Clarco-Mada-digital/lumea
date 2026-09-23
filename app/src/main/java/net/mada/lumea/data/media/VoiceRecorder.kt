package net.mada.lumea.data.media

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Le journal à la voix.
 *
 * Écrire suppose de savoir écrire, d'avoir le temps, et d'en avoir la force. Ces
 * trois conditions ne sont pas toujours réunies — ni pour quelqu'un dont le
 * français n'est pas la première langue écrite, ni pour une femme épuisée après
 * un accouchement, ni pour une adolescente qui préfère parler que rédiger.
 *
 * L'enregistrement ne remplace pas le texte : les deux coexistent dans la même
 * journée. On peut dicter trois phrases et en écrire une, ou l'inverse.
 *
 * **Format.** AAC dans un conteneur MP4 (`.m4a`) : environ un mégaoctet par
 * minute, lisible partout, et assez léger pour tenir dans une sauvegarde. La
 * durée est plafonnée — non par avarice, mais parce qu'un fichier de vingt
 * minutes rend la sauvegarde intransportable en données mobiles.
 */
class VoiceRecorder(private val context: Context) {

    /** Au-delà, l'enregistrement s'arrête tout seul. */
    val maxDurationMs = 5 * 60_000

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null

    val isRecording: Boolean get() = recorder != null
    val isPlaying: Boolean get() = player?.isPlaying == true

    /**
     * Démarre un enregistrement et renvoie le nom du fichier.
     *
     * Renvoie `null` si le micro est indisponible — occupé par un appel, refusé
     * par le système. L'écran doit le dire plutôt que de laisser croire que
     * l'enregistrement tourne.
     */
    fun start(store: MediaStore): String? {
        stopRecording()
        val name = store.newAudioName()
        val target = store.file(name)

        return runCatching {
            val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            instance.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // 64 kbit/s mono : largement suffisant pour la voix, et quatre
                // fois plus léger que le réglage par défaut.
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(44_100)
                setAudioChannels(1)
                setMaxDuration(maxDurationMs)
                setOutputFile(target.absolutePath)
                prepare()
                start()
            }
            recorder = instance
            currentFile = target
            name
        }.getOrElse {
            runCatching { target.delete() }
            recorder = null
            currentFile = null
            null
        }
    }

    /**
     * Arrête l'enregistrement.
     *
     * Renvoie `false` si le fichier est inutilisable — un enregistrement d'une
     * fraction de seconde produit un conteneur vide que rien ne sait lire. Mieux
     * vaut l'effacer que de laisser une ligne muette dans le journal.
     */
    fun stopRecording(): Boolean {
        val instance = recorder ?: return false
        recorder = null

        val ok = runCatching {
            instance.stop()
            true
        }.getOrDefault(false)
        runCatching { instance.release() }

        val file = currentFile
        currentFile = null
        if (!ok || file == null || file.length() < 1_000) {
            runCatching { file?.delete() }
            return false
        }
        return true
    }

    fun play(file: File, onFinished: () -> Unit = {}) {
        stopPlaying()
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlaying()
                    onFinished()
                }
                prepare()
                start()
            }
        }
    }

    fun stopPlaying() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    /** À appeler quand l'écran disparaît : un micro qui reste ouvert est un problème. */
    fun release() {
        stopRecording()
        stopPlaying()
    }
}

/** Durée d'un enregistrement, en secondes, sans le lire entièrement. */
fun audioDurationSeconds(file: File): Int = runCatching {
    android.media.MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(file.absolutePath)
        val ms = retriever
            .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        (ms / 1000).toInt()
    }
}.getOrDefault(0)
