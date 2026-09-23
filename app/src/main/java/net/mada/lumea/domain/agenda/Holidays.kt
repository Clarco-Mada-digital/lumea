package net.mada.lumea.domain.agenda

import java.time.LocalDate
import java.time.Month

/**
 * Les jours fériés, calculés en local.
 *
 * Aucun appel réseau : les dates fixes sont connues, et les fêtes mobiles se
 * calculent à partir de Pâques. C'est ce qui permet d'afficher le calendrier
 * complet hors ligne, y compris pour une année à venir.
 *
 * Le pays se choisit dans les réglages plutôt que de se déduire de la langue du
 * téléphone : beaucoup d'appareils sont en français sans être en France, et
 * afficher le 14 juillet à Antananarivo serait aussi faux qu'oublier le 26 juin.
 */
enum class HolidayCountry(val code: String, val label: String) {
    AUCUN("NONE", "Ne pas afficher"),
    MADAGASCAR("MG", "Madagascar"),
    FRANCE("FR", "France"),
}

data class Holiday(val date: LocalDate, val name: String)

/**
 * Dimanche de Pâques, par l'algorithme de Butcher (calendrier grégorien).
 *
 * Plusieurs fériés en dépendent — lundi de Pâques, Ascension, lundi de Pentecôte.
 * Le calcul est exact pour toutes les années du calendrier grégorien.
 */
fun easterSunday(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = (h + l - 7 * m + 114) % 31 + 1
    return LocalDate.of(year, month, day)
}

/**
 * Les jours fériés d'une année, pour le pays choisi.
 *
 * Sources : loi malgache sur les jours fériés pour Madagascar, code du travail
 * pour la France. Les fêtes religieuses mobiles de l'islam ne figurent pas dans
 * la liste malgache : elles suivent le calendrier lunaire et ne se calculent pas
 * de façon fiable sans table, et elles ne sont pas fériées nationalement.
 */
fun holidays(year: Int, country: HolidayCountry): List<Holiday> = when (country) {
    HolidayCountry.AUCUN -> emptyList()
    HolidayCountry.MADAGASCAR -> madagascar(year)
    HolidayCountry.FRANCE -> france(year)
}

private fun madagascar(year: Int): List<Holiday> {
    val easter = easterSunday(year)
    return listOf(
        Holiday(LocalDate.of(year, Month.JANUARY, 1), "Nouvel An"),
        Holiday(LocalDate.of(year, Month.MARCH, 8), "Journée de la femme"),
        Holiday(LocalDate.of(year, Month.MARCH, 29), "Fête des Martyrs"),
        Holiday(easter.plusDays(1), "Lundi de Pâques"),
        Holiday(LocalDate.of(year, Month.MAY, 1), "Fête du Travail"),
        Holiday(easter.plusDays(39), "Ascension"),
        Holiday(easter.plusDays(50), "Lundi de Pentecôte"),
        Holiday(LocalDate.of(year, Month.JUNE, 26), "Fête de l'Indépendance"),
        Holiday(LocalDate.of(year, Month.AUGUST, 15), "Assomption"),
        Holiday(LocalDate.of(year, Month.NOVEMBER, 1), "Toussaint"),
        Holiday(LocalDate.of(year, Month.DECEMBER, 11), "Fête de la République"),
        Holiday(LocalDate.of(year, Month.DECEMBER, 25), "Noël"),
    ).sortedBy { it.date }
}

private fun france(year: Int): List<Holiday> {
    val easter = easterSunday(year)
    return listOf(
        Holiday(LocalDate.of(year, Month.JANUARY, 1), "Jour de l'An"),
        Holiday(easter.plusDays(1), "Lundi de Pâques"),
        Holiday(LocalDate.of(year, Month.MAY, 1), "Fête du Travail"),
        Holiday(LocalDate.of(year, Month.MAY, 8), "Victoire 1945"),
        Holiday(easter.plusDays(39), "Ascension"),
        Holiday(easter.plusDays(50), "Lundi de Pentecôte"),
        Holiday(LocalDate.of(year, Month.JULY, 14), "Fête nationale"),
        Holiday(LocalDate.of(year, Month.AUGUST, 15), "Assomption"),
        Holiday(LocalDate.of(year, Month.NOVEMBER, 1), "Toussaint"),
        Holiday(LocalDate.of(year, Month.NOVEMBER, 11), "Armistice 1918"),
        Holiday(LocalDate.of(year, Month.DECEMBER, 25), "Noël"),
    ).sortedBy { it.date }
}

/**
 * Les fériés d'un intervalle, indexés par date.
 *
 * L'intervalle peut chevaucher deux années — le calendrier affiche toujours
 * quelques jours du mois précédent et du suivant.
 */
fun holidaysBetween(
    from: LocalDate,
    to: LocalDate,
    country: HolidayCountry,
): Map<LocalDate, Holiday> {
    if (country == HolidayCountry.AUCUN) return emptyMap()
    return (from.year..to.year)
        .flatMap { holidays(it, country) }
        .filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
        .associateBy { it.date }
}
