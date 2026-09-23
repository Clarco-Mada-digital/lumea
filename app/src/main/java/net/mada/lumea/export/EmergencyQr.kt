package net.mada.lumea.export

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import net.mada.lumea.data.db.PregnancyEntity
import net.mada.lumea.data.db.PrenatalCareEntity
import net.mada.lumea.domain.pregnancy.postpartumProgress
import net.mada.lumea.domain.pregnancy.pregnancyProgress
import net.mada.lumea.domain.pregnancy.riskFactor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * La fiche d'urgence en QR code, lisible par le soignant depuis son téléphone.
 *
 * Le cas visé est concret : au CSB, le carnet papier est resté à la maison, le
 * téléphone de la patiente est à 3 % de batterie ou son écran est cassé, et il
 * n'y a pas d'imprimante. Le soignant scanne, et lit sur son propre appareil.
 *
 * **Tout est dans le code, rien sur un serveur.** Le QR contient le texte lui-même,
 * pas un lien : il fonctionne sans réseau, des deux côtés, et rien n'est déposé
 * nulle part. C'est aussi ce qui en fixe la limite — un QR tient environ 1 200
 * caractères en correction moyenne, donc on écrit l'essentiel et on s'arrête.
 *
 * **Ce qu'il ne contient pas.** Ni journal, ni humeur, ni notes. Et le nom n'y
 * figure que si on le demande : un QR se photographie, se transfère, et on ne
 * contrôle plus ce qu'il devient.
 */
object EmergencyQr {

    /** Au-delà, le code devient trop dense pour être scanné sur un écran fissuré. */
    private const val MAX_CHARS = 1_100

    private val shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH)

    /**
     * Le texte encodé.
     *
     * Écrit pour être lu tel quel par un humain dans n'importe quel lecteur de
     * QR — pas de JSON, pas de format à interpréter. Une ligne par information,
     * les plus urgentes en premier.
     */
    fun buildText(
        pregnancy: PregnancyEntity,
        care: Map<String, PrenatalCareEntity>,
        displayName: String,
        includeName: Boolean,
    ): String = buildString {
        appendLine("FICHE D'URGENCE — Lumea")
        if (includeName && displayName.isNotBlank()) appendLine("Patiente : $displayName")

        when {
            pregnancy.status == "POSTPARTUM" -> pregnancy.birthDate?.let {
                val after = postpartumProgress(it)
                appendLine("ACCOUCHEMENT le ${it.format(shortDate)} (il y a ${after.daysSince} j)")
            }
            else -> pregnancy.lastPeriodStart?.let {
                val p = pregnancyProgress(it)
                appendLine("ENCEINTE — ${p.label}, ${p.trimester}e trimestre")
                appendLine("Terme prevu : ${p.dueDate.format(shortDate)}")
                appendLine("Dernieres regles : ${it.format(shortDate)}")
            }
        }

        appendLine()
        line("Groupe sanguin", care["GROUPE_SANGUIN"]?.note)
        line("Allergies", care["ALLERGIES"]?.note)
        line("Traitements", care["TRAITEMENTS"]?.note)
        line("Hemoglobine", care["HEMOGLOBINE"]?.note)

        val risks = pregnancy.riskFactors
            .split(",").filter { it.isNotBlank() }.mapNotNull { riskFactor(it)?.label }
        if (risks.isNotEmpty()) {
            appendLine()
            appendLine("Signale : ${risks.joinToString(" ; ")}")
        }

        val who = listOfNotNull(
            pregnancy.caregiverName.takeIf { it.isNotBlank() },
            pregnancy.facility.takeIf { it.isNotBlank() },
            pregnancy.caregiverPhone.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        if (who.isNotEmpty()) {
            appendLine()
            appendLine("Suivie par : $who")
        }
        care["CONTACT_URGENCE"]?.note?.takeIf { it.isNotBlank() }?.let {
            appendLine("A prevenir : $it")
        }

        appendLine()
        appendLine("Genere le ${LocalDate.now().format(shortDate)} — declaratif,")
        appendLine("sans valeur officielle.")
    }.let { if (it.length > MAX_CHARS) it.take(MAX_CHARS) + "…" else it }

    private fun StringBuilder.line(label: String, content: String?) {
        appendLine("$label : ${content?.takeIf { it.isNotBlank() } ?: "non renseigne"}")
    }

    /**
     * Rend le QR en image noir et blanc.
     *
     * Correction d'erreur moyenne : le code reste lisible même partiellement
     * abîmé — écran rayé, reflet, photo prise de travers.
     */
    fun render(text: String, sizePx: Int = 720): Bitmap? = runCatching {
        val matrix = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(sizePx * sizePx)
        for (row in 0 until sizePx) {
            val offset = row * sizePx
            for (col in 0 until sizePx) {
                pixels[offset + col] = if (matrix[col, row]) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        bitmap
    }.getOrNull()
}
