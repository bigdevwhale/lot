package app.lot.ui

import app.lot.domain.Birth
import app.lot.domain.Sex
import app.lot.domain.Tx
import app.lot.domain.World
import app.lot.domain.Years
import app.lot.ui.art.TagText
import java.util.Locale

/** Words that come from data rather than resources, in both languages. */
class Words(val ru: Boolean) {
    fun t(r: String, e: String) = if (ru) r else e
    fun x(tx: Tx) = tx.of(ru)

    fun n(n: Int, one: String, few: String, many: String, en1: String, enN: String) = if (ru) "$n ${Years.plural(n, one, few, many)}" else "$n ${if (n == 1) en1 else enN}"

    fun year(y: Int) = x(Years.of(y))

    fun span(from: Int, to: Int) = "${year(from)} – ${year(to)}"

    /** «2,1%», "<0.01%". */
    fun pct(p: Double): String {
        val v = p * 100
        val s = when {
            v >= 10 -> String.format(Locale.US, "%.0f", v)
            v >= 1 -> String.format(Locale.US, "%.1f", v)
            v >= .01 -> String.format(Locale.US, "%.2f", v)
            else -> return t("<0,01%", "<0.01%")
        }
        return (if (ru) s.replace('.', ',') else s) + "%"
    }

    /** People, from a share of the 117 billion: «1,2 млрд», "430 million". */
    fun people(share: Double): String {
        val b = share * World.TOTAL
        return if (b >= 1) {
            val s = String.format(Locale.US, "%.1f", b)
            if (ru) "${s.replace('.', ',')} млрд" else "$s billion"
        } else {
            val m = (b * 1000).toInt().coerceAtLeast(1)
            if (ru) "$m млн" else "$m million"
        }
    }

    fun tag(b: Birth) = TagText(
        year(b.year),
        x(b.region.name),
        t("в семье ${b.estate.of.ru}", "born to ${b.estate.of.en}"),
        if (b.sex == Sex.M) t("мальчик", "a boy") else t("девочка", "a girl"),
    )

    fun born(b: Birth): String {
        val y = Years.inYear(b.year)
        return x(Tx("{Родился|Родилась} ${Years.stop(y.ru)} ${b.region.name.ru}, в семье ${b.estate.of.ru}.", "Born ${Years.stop(y.en)} ${b.region.name.en}, to ${b.estate.of.en}.").g(b.sex))
    }

    fun gen(g: Int) = if (ru) "$g-е поколение рода" else "generation $g of the line"

    fun lived(age: Int) = if (age < 1) t("меньше года", "under a year") else n(age, "год", "года", "лет", "year", "years")
}
