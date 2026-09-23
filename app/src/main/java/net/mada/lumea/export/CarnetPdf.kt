package net.mada.lumea.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mada.lumea.data.db.PregnancyEntity
import net.mada.lumea.data.db.PrenatalCareEntity
import net.mada.lumea.domain.pregnancy.ALL_CARE_ACTS
import net.mada.lumea.domain.pregnancy.CareAct
import net.mada.lumea.domain.pregnancy.INFANT_VACCINES
import net.mada.lumea.domain.pregnancy.POSTNATAL_VISITS
import net.mada.lumea.domain.pregnancy.RECORDED_RESULTS
import net.mada.lumea.domain.pregnancy.SYMPTOM_TRIAGE
import net.mada.lumea.domain.pregnancy.Urgency
import net.mada.lumea.domain.pregnancy.label
import net.mada.lumea.domain.pregnancy.postpartumProgress
import net.mada.lumea.domain.pregnancy.pregnancyProgress
import net.mada.lumea.domain.pregnancy.riskFactor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Le carnet de suivi, en PDF, à montrer en consultation.
 *
 * Le carnet de santé mère-enfant se perd, s'abîme, s'oublie à la maison. Ce PDF
 * n'a pas vocation à le remplacer — il n'a aucune valeur officielle — mais à
 * répondre à la question que pose le soignant quand le carnet n'est pas là :
 * « qu'est-ce qui a déjà été fait, et quand ? »
 *
 * **Sur la mise en page.** Une première version alignait du texte brut : lisible
 * pour qui prend le temps, inutilisable pour un soignant qui a trois minutes.
 * Celle-ci hiérarchise — bandeau d'en-tête, encadrés de couleur, cases à cocher
 * dessinées, tableau des actes. Un soignant doit pouvoir trouver le groupe
 * sanguin en une seconde, sans lire.
 *
 * Deux partis pris tenus.
 *
 * **Rien d'intime.** Le journal, l'humeur, la gratitude et les notes n'y figurent
 * pas. Un document qu'on tend à quelqu'un ne doit contenir que ce qu'on accepte
 * de montrer. Les sections restent décochables une par une.
 *
 * **Aucune interprétation.** Le document rapporte ce qui a été coché et ce qui a
 * été noté. Il ne conclut rien, et rappelle que seule l'échographie fait foi.
 */
object CarnetPdf {

    private const val PAGE_WIDTH = 595   // A4 à 72 ppp
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    // Palette sobre : lisible en noir et blanc, et à l'impression laser.
    private const val INK = 0xFF1A1A1A.toInt()
    private const val MUTED = 0xFF6B6B6B.toInt()
    private const val ACCENT = 0xFFB4004E.toInt()
    private const val ACCENT_SOFT = 0xFFFFE4EC.toInt()
    private const val ALERT = 0xFFC62828.toInt()
    private const val ALERT_SOFT = 0xFFFFE8E6.toInt()
    private const val RULE = 0xFFE0E0E0.toInt()
    private const val BOX = 0xFFF5F5F5.toInt()

    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
    private val shortDate = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.FRENCH)

    /** Ce qu'on accepte de faire figurer dans le document. */
    data class Options(
        val identity: Boolean = true,
        val results: Boolean = true,
        val checklist: Boolean = true,
        val riskFactors: Boolean = true,
        val warningSigns: Boolean = true,
        /** Le prénom : certaines préfèrent un document anonyme. */
        val includeName: Boolean = true,
    )

    suspend fun write(
        context: Context,
        uri: Uri,
        pregnancy: PregnancyEntity,
        care: Map<String, PrenatalCareEntity>,
        displayName: String,
        options: Options = Options(),
        qr: Bitmap? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val document = PdfDocument()
            val writer = PageWriter(document)

            writer.banner(pregnancy, if (options.includeName) displayName else "")
            if (options.identity) writer.identity(pregnancy, care)
            if (options.results) writer.results(care)
            if (options.riskFactors) writer.risks(pregnancy)
            if (options.checklist) writer.checklist(pregnancy, care)
            if (options.warningSigns) writer.warnings()
            writer.disclaimer(qr)
            writer.finish()

            context.contentResolver.openOutputStream(uri, "wt")?.use { document.writeTo(it) }
                ?: error("Impossible d'écrire dans ce fichier")
            document.close()
        }
    }

    fun suggestedFileName(): String = "carnet-lumea-${LocalDate.now()}.pdf"

    /**
     * Dessine page après page, en ouvrant une feuille dès que le bas est atteint.
     * Le PDF Android n'a aucune mise en page automatique : tout est positionné à
     * la main, y compris les retours à la ligne.
     */
    private class PageWriter(private val document: PdfDocument) {

        private var page: PdfDocument.Page = newPage(1)
        private var pageNumber = 1
        private var y = 0f

        private val width = PAGE_WIDTH - 2 * MARGIN

        private val h1 = paint(22f, bold = true, color = 0xFFFFFFFF.toInt())
        private val h2 = paint(13f, bold = true, color = ACCENT)
        private val h3 = paint(10.5f, bold = true)
        private val body = paint(9.5f)
        private val small = paint(8f, color = MUTED)
        private val label = paint(7.5f, bold = true, color = MUTED)
        private val value = paint(13f, bold = true)

        private fun newPage(number: Int) = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()
        )

        private fun paint(size: Float, bold: Boolean = false, color: Int = INK) =
            Paint().apply {
                textSize = size
                this.color = color
                typeface = Typeface.create(
                    Typeface.SANS_SERIF,
                    if (bold) Typeface.BOLD else Typeface.NORMAL,
                )
                isAntiAlias = true
            }

        private fun fill(color: Int) = Paint().apply {
            this.color = color
            isAntiAlias = true
        }

        private fun ensure(space: Float) {
            if (y + space < PAGE_HEIGHT - MARGIN - 20f) return
            footer()
            document.finishPage(page)
            pageNumber++
            page = newPage(pageNumber)
            y = MARGIN
        }

        private fun footer() {
            page.canvas.drawText(
                "Lumea · document d'information, sans valeur officielle",
                MARGIN,
                PAGE_HEIGHT - MARGIN + 8f,
                small,
            )
            page.canvas.drawText(
                "$pageNumber",
                PAGE_WIDTH - MARGIN - 6f,
                PAGE_HEIGHT - MARGIN + 8f,
                small,
            )
        }

        /** Texte coupé à la largeur utile, avec retour à la ligne aux espaces. */
        private fun paragraph(text: String, paint: Paint, indent: Float = 0f, extra: Float = 3f) {
            val maxWidth = width - indent
            var remaining = text
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth, null)
                var cut = count
                if (cut < remaining.length) {
                    val space = remaining.lastIndexOf(' ', cut)
                    if (space > 0) cut = space
                }
                ensure(paint.textSize + extra)
                y += paint.textSize
                page.canvas.drawText(remaining.take(cut).trim(), MARGIN + indent, y, paint)
                y += extra
                remaining = remaining.drop(cut).trimStart()
            }
        }

        private fun section(title: String) {
            ensure(34f)
            y += 16f
            page.canvas.drawText(title.uppercase(), MARGIN, y, h2)
            y += 5f
            page.canvas.drawRect(MARGIN, y, MARGIN + 34f, y + 2f, fill(ACCENT))
            y += 12f
        }

        // ------------------------------------------------------------ En-tête

        fun banner(pregnancy: PregnancyEntity, displayName: String) {
            val height = 92f
            page.canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), height, fill(ACCENT))

            page.canvas.drawText("Carnet de suivi", MARGIN, 40f, h1)

            val isPostpartum = pregnancy.status == "POSTPARTUM"
            val headline = when {
                isPostpartum -> pregnancy.birthDate?.let {
                    val after = postpartumProgress(it)
                    "Après la naissance · ${after.weeksSince} semaines"
                }
                else -> pregnancy.lastPeriodStart?.let {
                    val p = pregnancyProgress(it)
                    "Grossesse · ${p.label} · ${p.trimester}ᵉ trimestre"
                }
            } ?: "Suivi en cours"

            page.canvas.drawText(
                headline,
                MARGIN,
                62f,
                paint(11f, bold = true, color = 0xFFFFFFFF.toInt()),
            )

            val right = buildString {
                if (displayName.isNotBlank()) append("$displayName · ")
                append("édité le ${LocalDate.now().format(shortDate)}")
            }
            val rightPaint = paint(8.5f, color = 0xCCFFFFFF.toInt())
            page.canvas.drawText(
                right,
                PAGE_WIDTH - MARGIN - rightPaint.measureText(right),
                80f,
                rightPaint,
            )

            y = height + 10f
        }

        // ----------------------------------------------- Les quatre essentiels

        fun identity(pregnancy: PregnancyEntity, care: Map<String, PrenatalCareEntity>) {
            section("L'essentiel")

            /*
             * Quatre cases côte à côte, en gros caractères. Ce sont les questions
             * posées en premier aux urgences : elles doivent se lire sans chercher.
             */
            val cells = listOf(
                "Groupe sanguin" to care["GROUPE_SANGUIN"]?.note,
                "Allergies" to care["ALLERGIES"]?.note,
                "Traitements" to care["TRAITEMENTS"]?.note,
                "Hémoglobine" to care["HEMOGLOBINE"]?.note,
            )

            val cellWidth = (width - 3 * 8f) / 4f
            val cellHeight = 48f
            ensure(cellHeight + 8f)

            cells.forEachIndexed { index, (title, content) ->
                val left = MARGIN + index * (cellWidth + 8f)
                page.canvas.drawRoundRect(
                    RectF(left, y, left + cellWidth, y + cellHeight),
                    6f, 6f, fill(BOX),
                )
                page.canvas.drawText(title.uppercase(), left + 8f, y + 15f, label)

                val text = content?.takeIf { it.isNotBlank() } ?: "—"
                val p = if (content.isNullOrBlank()) paint(13f, bold = true, color = MUTED) else value
                // Une valeur longue rétrécit plutôt que de déborder de sa case.
                var size = 13f
                while (p.measureText(text) > cellWidth - 16f && size > 7f) {
                    size -= 0.5f
                    p.textSize = size
                }
                page.canvas.drawText(text, left + 8f, y + 36f, p)
            }
            y += cellHeight + 6f

            // Dates et soignant, sur une ligne chacun.
            pregnancy.lastPeriodStart?.let {
                paragraph("Dernières règles : ${it.format(longDate)}", body)
            }
            pregnancy.birthDate?.let {
                paragraph("Naissance : ${it.format(longDate)}", body)
            }
            if (pregnancy.status != "POSTPARTUM") {
                pregnancy.lastPeriodStart?.let {
                    paragraph(
                        "Terme prévu : ${pregnancyProgress(it).dueDate.format(longDate)}",
                        body,
                    )
                }
            }

            val who = listOfNotNull(
                pregnancy.caregiverName.takeIf { it.isNotBlank() },
                pregnancy.caregiverRole.takeIf { it.isNotBlank() },
                pregnancy.facility.takeIf { it.isNotBlank() },
                pregnancy.caregiverPhone.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (who.isNotEmpty()) paragraph("Suivie par : $who", body)

            care["CONTACT_URGENCE"]?.note?.takeIf { it.isNotBlank() }?.let {
                paragraph("Personne à prévenir : $it", body)
            }
        }

        // ------------------------------------------------------- Les résultats

        fun results(care: Map<String, PrenatalCareEntity>) {
            val noted = RECORDED_RESULTS.filter {
                it.code !in setOf("GROUPE_SANGUIN", "ALLERGIES", "TRAITEMENTS", "HEMOGLOBINE") &&
                    care[it.code]?.note?.isNotBlank() == true
            }
            if (noted.isEmpty()) return

            section("Examens")
            noted.forEach { act ->
                val entry = care.getValue(act.code)
                ensure(24f)
                paragraph(act.title, h3, extra = 1f)
                paragraph(entry.note, body, indent = 10f, extra = 1f)
                paragraph("noté le ${entry.doneOn.format(shortDate)}", small, indent = 10f)
            }
        }

        // ---------------------------------------------------------- Le terrain

        fun risks(pregnancy: PregnancyEntity) {
            val risks = pregnancy.riskFactors
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { riskFactor(it)?.label }
            if (risks.isEmpty()) return

            section("Signalé par la patiente")
            ensure(risks.size * 14f + 16f)

            val boxTop = y - 4f
            val boxHeight = risks.size * 14f + 12f
            page.canvas.drawRoundRect(
                RectF(MARGIN, boxTop, MARGIN + width, boxTop + boxHeight),
                6f, 6f, fill(ACCENT_SOFT),
            )
            y += 8f
            risks.forEach {
                page.canvas.drawText("•  $it", MARGIN + 10f, y, body)
                y += 14f
            }
            y += 6f
        }

        // ---------------------------------------------------------- Les actes

        fun checklist(pregnancy: PregnancyEntity, care: Map<String, PrenatalCareEntity>) {
            val acts = if (pregnancy.status == "POSTPARTUM") {
                POSTNATAL_VISITS + INFANT_VACCINES
            } else {
                ALL_CARE_ACTS
            }

            section("Actes de suivi")
            paragraph(
                "Coché = confirmé par la patiente dans l'application.",
                small,
                extra = 6f,
            )

            acts.groupBy { it.category }.forEach { (category, group) ->
                ensure(30f)
                y += 8f
                page.canvas.drawText(category.label, MARGIN, y, h3)
                y += 6f
                page.canvas.drawRect(MARGIN, y, MARGIN + width, y + 0.7f, fill(RULE))
                y += 10f
                group.forEach { act -> actLine(act, care[act.code]) }
            }
        }

        private fun actLine(act: CareAct, entry: PrenatalCareEntity?) {
            ensure(20f)
            val done = entry != null
            val boxSize = 9f
            val top = y - 1f

            // Case à cocher dessinée : elle se voit d'un coup d'œil, contrairement
            // à un « [x] » qui se lit.
            page.canvas.drawRoundRect(
                RectF(MARGIN, top, MARGIN + boxSize, top + boxSize),
                2f, 2f,
                if (done) fill(ACCENT) else fill(0xFFFFFFFF.toInt()),
            )
            if (!done) {
                page.canvas.drawRoundRect(
                    RectF(MARGIN, top, MARGIN + boxSize, top + boxSize),
                    2f, 2f,
                    Paint().apply {
                        color = 0xFFBDBDBD.toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = 0.8f
                        isAntiAlias = true
                    },
                )
            } else {
                val tick = Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    strokeWidth = 1.4f
                    isAntiAlias = true
                }
                page.canvas.drawLine(MARGIN + 2f, top + 4.5f, MARGIN + 3.8f, top + 6.5f, tick)
                page.canvas.drawLine(MARGIN + 3.8f, top + 6.5f, MARGIN + 7f, top + 2.5f, tick)
            }

            val titlePaint = if (done) body else paint(9.5f, color = MUTED)
            page.canvas.drawText(act.title, MARGIN + boxSize + 8f, y + 6f, titlePaint)

            if (entry != null) {
                val date = entry.doneOn.format(shortDate)
                val datePaint = paint(8.5f, bold = true, color = ACCENT)
                page.canvas.drawText(
                    date,
                    PAGE_WIDTH - MARGIN - datePaint.measureText(date),
                    y + 6f,
                    datePaint,
                )
            }
            y += 15f

            if (entry?.note?.isNotBlank() == true) {
                paragraph(entry.note, small, indent = boxSize + 8f, extra = 4f)
            }
        }

        // --------------------------------------------------------- Les alertes

        fun warnings() {
            val signs = SYMPTOM_TRIAGE.filter { it.urgency == Urgency.PARTIR_MAINTENANT }

            section("Partir au CSB sans attendre")
            ensure(signs.size * 13f + 20f)

            val boxTop = y - 4f
            val boxHeight = signs.size * 13f + 14f
            page.canvas.drawRoundRect(
                RectF(MARGIN, boxTop, MARGIN + width, boxTop + boxHeight),
                6f, 6f, fill(ALERT_SOFT),
            )
            y += 9f
            signs.forEach {
                page.canvas.drawText("•  ${it.label}", MARGIN + 10f, y, paint(9.5f, color = ALERT))
                y += 13f
            }
            y += 8f
        }

        // --------------------------------------------------------- Le pied

        fun disclaimer(qr: Bitmap?) {
            ensure(80f)
            y += 10f
            page.canvas.drawRect(MARGIN, y, MARGIN + width, y + 0.7f, fill(RULE))
            y += 12f

            val textWidth = if (qr != null) width - 80f else width
            val saved = y
            var remaining =
                "Document produit par l'application Lumea à partir de ce que la patiente " +
                    "a enregistré elle-même. Il n'a aucune valeur officielle et ne remplace " +
                    "ni le carnet de santé mère-enfant, ni un compte rendu médical. Les " +
                    "semaines d'aménorrhée sont calculées depuis la date des dernières règles " +
                    "déclarée ; seule l'échographie du premier trimestre date la grossesse de " +
                    "façon fiable."
            while (remaining.isNotEmpty()) {
                val count = small.breakText(remaining, true, textWidth, null)
                var cut = count
                if (cut < remaining.length) {
                    val space = remaining.lastIndexOf(' ', cut)
                    if (space > 0) cut = space
                }
                y += small.textSize
                page.canvas.drawText(remaining.take(cut).trim(), MARGIN, y, small)
                y += 3f
                remaining = remaining.drop(cut).trimStart()
            }

            if (qr != null) {
                val size = 70f
                val left = MARGIN + width - size
                page.canvas.drawBitmap(
                    qr,
                    null,
                    RectF(left, saved, left + size, saved + size),
                    null,
                )
                page.canvas.drawText(
                    "Scanner",
                    left + size / 2f - small.measureText("Scanner") / 2f,
                    saved + size + 9f,
                    small,
                )
            }
        }

        fun finish() {
            footer()
            document.finishPage(page)
        }
    }
}
