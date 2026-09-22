package net.mada.lumea.export

import android.content.Context
import android.graphics.Paint
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
 * Deux partis pris.
 *
 * **Rien d'intime.** Le journal, l'humeur, la gratitude et les notes n'y figurent
 * pas. Un document qu'on tend à quelqu'un ne doit contenir que ce qu'on accepte
 * de montrer : dates, actes, résultats saisis.
 *
 * **Aucune interprétation.** Le document rapporte ce qui a été coché et ce qui a
 * été noté. Il ne conclut rien, ne calcule aucun risque, et rappelle en pied de
 * page que seule l'échographie fait foi pour dater la grossesse.
 */
object CarnetPdf {

    private const val PAGE_WIDTH = 595   // A4 à 72 ppp
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f

    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
    private val shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH)

    suspend fun write(
        context: Context,
        uri: Uri,
        pregnancy: PregnancyEntity,
        care: Map<String, PrenatalCareEntity>,
        displayName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val document = PdfDocument()
            val writer = PageWriter(document)

            writer.header(pregnancy, displayName)
            writer.identity(pregnancy)
            writer.results(care)
            writer.checklist(pregnancy, care)
            writer.footer()
            writer.finish()

            context.contentResolver.openOutputStream(uri, "wt")?.use { document.writeTo(it) }
                ?: error("Impossible d'écrire dans ce fichier")
            document.close()
        }
    }

    fun suggestedFileName(): String = "carnet-lumea-${LocalDate.now()}.pdf"

    /**
     * Dessine page après page, en ouvrant une nouvelle feuille dès que le bas est
     * atteint. Le PDF Android n'a pas de mise en page automatique : chaque ligne
     * est positionnée à la main.
     */
    private class PageWriter(private val document: PdfDocument) {

        private var page: PdfDocument.Page = newPage(1)
        private var pageNumber = 1
        private var y = MARGIN + 20f

        private val title = paint(20f, bold = true)
        private val heading = paint(13f, bold = true)
        private val body = paint(10f)
        private val small = paint(8.5f, grey = true)
        private val line = Paint().apply { color = 0xFFDDDDDD.toInt() }

        private fun newPage(number: Int) = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()
        )

        private fun paint(size: Float, bold: Boolean = false, grey: Boolean = false) =
            Paint().apply {
                textSize = size
                color = if (grey) 0xFF666666.toInt() else 0xFF111111.toInt()
                typeface = Typeface.create(
                    Typeface.SANS_SERIF,
                    if (bold) Typeface.BOLD else Typeface.NORMAL,
                )
                isAntiAlias = true
            }

        private fun ensure(space: Float) {
            if (y + space < PAGE_HEIGHT - MARGIN - 24f) return
            pageFooter()
            document.finishPage(page)
            pageNumber++
            page = newPage(pageNumber)
            y = MARGIN + 20f
        }

        private fun pageFooter() {
            page.canvas.drawText(
                "Lumea — document d'information, sans valeur officielle — page $pageNumber",
                MARGIN,
                PAGE_HEIGHT - MARGIN + 10f,
                small,
            )
        }

        /** Écrit un texte en le coupant à la largeur utile. */
        private fun paragraph(text: String, paint: Paint, indent: Float = 0f) {
            val maxWidth = PAGE_WIDTH - 2 * MARGIN - indent
            var remaining = text
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth, null)
                var cut = count
                if (cut < remaining.length) {
                    val space = remaining.lastIndexOf(' ', cut)
                    if (space > 0) cut = space
                }
                ensure(paint.textSize + 4f)
                page.canvas.drawText(remaining.take(cut).trim(), MARGIN + indent, y, paint)
                y += paint.textSize + 4f
                remaining = remaining.drop(cut).trimStart()
            }
        }

        fun header(pregnancy: PregnancyEntity, displayName: String) {
            page.canvas.drawText("Carnet de suivi", MARGIN, y, title)
            y += 26f

            val isPostpartum = pregnancy.status == "POSTPARTUM"
            val summary = when {
                isPostpartum -> pregnancy.birthDate?.let {
                    val after = postpartumProgress(it)
                    "Après la naissance du ${it.format(dayMonthYear)} — " +
                        "${after.weeksSince} semaines"
                }
                else -> pregnancy.lastPeriodStart?.let {
                    val p = pregnancyProgress(it)
                    "Grossesse en cours — ${p.label}, ${p.trimester}ᵉ trimestre · " +
                        "terme prévu le ${p.dueDate.format(dayMonthYear)}"
                }
            } ?: "Suivi en cours"

            paragraph(summary, heading)
            if (displayName.isNotBlank()) paragraph(displayName, body)
            paragraph("Édité le ${LocalDate.now().format(dayMonthYear)}", small)
            y += 8f
            separator()
        }

        private fun separator() {
            ensure(14f)
            page.canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line)
            y += 16f
        }

        fun identity(pregnancy: PregnancyEntity) {
            if (pregnancy.lastPeriodStart != null) {
                paragraph(
                    "Dernières règles : ${pregnancy.lastPeriodStart.format(dayMonthYear)}",
                    body,
                )
            }
            if (pregnancy.caregiverName.isNotBlank() || pregnancy.facility.isNotBlank()) {
                val who = listOfNotNull(
                    pregnancy.caregiverName.takeIf { it.isNotBlank() },
                    pregnancy.caregiverRole.takeIf { it.isNotBlank() },
                    pregnancy.facility.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                paragraph("Suivie par : $who", body)
            }

            val risks = pregnancy.riskFactors
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { riskFactor(it)?.label }
            if (risks.isNotEmpty()) {
                y += 6f
                paragraph("Situations signalées par la patiente :", heading)
                risks.forEach { paragraph("— $it", body, indent = 10f) }
            }
            y += 8f
            separator()
        }

        fun results(care: Map<String, PrenatalCareEntity>) {
            val noted = RECORDED_RESULTS.filter { care[it.code]?.note?.isNotBlank() == true }
            if (noted.isEmpty()) return

            paragraph("Résultats notés", heading)
            y += 4f
            noted.forEach { act ->
                val entry = care.getValue(act.code)
                paragraph("${act.title} : ${entry.note}", body)
                paragraph("noté le ${entry.doneOn.format(shortDate)}", small, indent = 10f)
            }
            y += 8f
            separator()
        }

        fun checklist(pregnancy: PregnancyEntity, care: Map<String, PrenatalCareEntity>) {
            val acts = if (pregnancy.status == "POSTPARTUM") {
                POSTNATAL_VISITS + INFANT_VACCINES
            } else {
                ALL_CARE_ACTS
            }

            paragraph("Actes de suivi", heading)
            y += 4f

            acts.groupBy { it.category }.forEach { (category, group) ->
                ensure(30f)
                y += 6f
                paragraph(category.label, paint(11f, bold = true))
                group.forEach { act -> actLine(act, care[act.code]) }
            }
        }

        private fun actLine(act: CareAct, entry: PrenatalCareEntity?) {
            val mark = if (entry != null) "[x]" else "[ ]"
            val done = entry?.let { "  —  fait le ${it.doneOn.format(shortDate)}" } ?: ""
            paragraph("$mark ${act.title}$done", body)
            if (entry?.note?.isNotBlank() == true) {
                paragraph(entry.note, small, indent = 22f)
            }
        }

        fun footer() {
            y += 12f
            separator()
            paragraph(
                "Document généré par l'application Lumea à partir de ce que la patiente " +
                    "a enregistré elle-même. Il n'a aucune valeur officielle et ne remplace " +
                    "ni le carnet de santé mère-enfant, ni un compte rendu médical. " +
                    "Les semaines d'aménorrhée sont calculées depuis la date des dernières " +
                    "règles déclarée ; seule l'échographie du premier trimestre date la " +
                    "grossesse de façon fiable.",
                small,
            )
        }

        fun finish() {
            pageFooter()
            document.finishPage(page)
        }
    }
}
