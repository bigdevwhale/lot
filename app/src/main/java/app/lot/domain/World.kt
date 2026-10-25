package app.lot.domain

/** A line in both languages. Russian and English may carry {he|she} marks until a life's sex is known. */
data class Tx(val ru: String, val en: String) {
    fun of(ru: Boolean) = if (ru) this.ru else en
    fun g(sex: Sex) = Tx(mark(ru, sex), mark(en, sex))
    operator fun plus(o: Tx) = Tx(ru + o.ru, en + o.en)

    companion object {
        private val MARK = Regex("\\{([^|}]*)\\|([^}]*)\\}")
        fun mark(s: String, sex: Sex) = MARK.replace(s) { m -> if (sex == Sex.M) m.groupValues[1] else m.groupValues[2] }
    }
}

enum class Sex { M, F }

/** Eras share their everyday lines by group. */
enum class Group { STONE, ANC, MED, EARLY, IND, MOD }

class Era(
    val i: Int,
    val group: Group,
    val name: Tx,
    val span: Tx,
    val from: Int,
    val to: Int,
    /** People born in it, in billions. */
    val births: Double,
    /** The chance of dying before five. */
    val q0: Double,
    /** Age at death for those who pass five: low, most likely, high. */
    val die: Triple<Int, Int, Int>,
    val lit: Double,
    /** Children born to a family. */
    val fert: Double,
    val color: Long,
    val regions: List<Region>,
    val estates: List<Estate>,
)

class Region(
    val key: String,
    val name: Tx,
    val w: Double,
    val lon: Float,
    val lat: Float,
    val cult: String,
    val tags: Set<String>,
    val sea: Boolean,
)

/** The family one is born into: «в семье крестьян», "born to peasants". */
class Estate(
    val key: String,
    val of: Tx,
    val w: Double,
    /** 0 unfree, 1 common, 2 craft and trade, 3 the top. */
    val rank: Int,
    val work: Tx,
    val only: Set<String>? = null,
)

private fun r(key: String, ru: String, en: String, w: Double, lon: Int, lat: Int, cult: String, tags: String, sea: Boolean = false) =
    Region(key, Tx(ru, en), w, lon.toFloat(), lat.toFloat(), cult, tags.split(' ').toSet(), sea)

private fun e(key: String, ru: String, en: String, w: Double, rank: Int, workRu: String, workEn: String, only: String? = null) =
    Estate(key, Tx(ru, en), w, rank, Tx(workRu, workEn), only?.split(' ')?.toSet())

private val FIELD = Tx("{Пашешь с отцом.|Прядёшь, жнёшь и смотришь за младшими.}", "{You plough with your father.|You spin, reap and mind the little ones.}")

object World {
    /** Everyone ever born, in billions: the Population Reference Bureau's estimate, rounded. */
    const val TOTAL = 117.0

    val ERAS: List<Era> = listOf(
        Era(0, Group.STONE, Tx("Каменный век", "Stone Age"), Tx("до 8000 до н.э.", "before 8000 BC"), -50000, -8000, 9.0, .45, Triple(8, 30, 70), 0.0, 4.0, 0xFF6B4A2B,
            listOf(
                r("eaf", "Восточная Африка", "East Africa", 30.0, 36, 0, "stone", "af"),
                r("me", "Ближний Восток", "Near East", 13.0, 40, 32, "stone", "me"),
                r("eur", "Европа, у ледника", "Europe, by the ice", 8.0, 15, 48, "stone", "eu med"),
                r("sas", "Южная Азия", "South Asia", 22.0, 78, 22, "stone", "in", true),
                r("eas", "Восточная Азия", "East Asia", 17.0, 112, 34, "stone", "cn", true),
                r("am", "Америка", "The Americas", 6.0, -100, 38, "stone", "am meso"),
                r("au", "Австралия", "Australia", 4.0, 135, -25, "au", "au", true),
            ),
            listOf(e("band", "охотников и собирателей", "hunter-gatherers", 100.0, 1,
                "{Ходишь со взрослыми на зверя: тебя уже берут в загон.|Ходишь с женщинами за кореньями и учишься находить съедобное.}",
                "{You go after game with the grown-ups: they let you drive the herd.|You go with the women for roots and learn what can be eaten.}"))),
        Era(1, Group.ANC, Tx("Древний мир", "Ancient world"), Tx("8000 до н.э. – 1 н.э.", "8000 BC – AD 1"), -8000, 1, 37.0, .42, Triple(8, 38, 80), .02, 6.0, 0xFF8F6A2F,
            listOf(
                r("cn", "Китай, Жёлтая река", "China, the Yellow River", 24.0, 112, 35, "cn", "cn"),
                r("in", "Индия, Ганг", "India, the Ganges", 24.0, 82, 25, "in", "in"),
                r("mes", "Месопотамия", "Mesopotamia", 9.0, 45, 32, "mes", "me"),
                r("eg", "Египет, Нил", "Egypt, the Nile", 7.0, 31, 27, "eg", "eg me", true),
                r("med", "Средиземноморье", "The Mediterranean", 12.0, 18, 40, "gr", "med it eu", true),
                r("waf", "Западная Африка", "West Africa", 9.0, 0, 11, "af", "af"),
                r("meso", "Мезоамерика", "Mesoamerica", 4.0, -98, 19, "am", "meso am"),
                r("andes", "Анды", "The Andes", 2.0, -73, -13, "am", "andes am"),
                r("au", "Австралия", "Australia", .5, 135, -25, "au", "au", true),
            ),
            listOf(
                e("farm", "земледельцев", "farmers", 70.0, 1, "Работаешь в поле с отцом и у очага с матерью.", "You work the field with your father and the hearth with your mother."),
                e("slave", "рабов", "slaves", 12.0, 0, "Работаешь, как прикажут. Тебя могут продать.", "You work as you are told. You can be sold."),
                e("craft", "ремесленников", "craftsmen", 9.0, 2, "Помогаешь в мастерской: месишь глину, раздуваешь горн.", "You help in the workshop: kneading clay, pumping the bellows."),
                e("scribe", "писцов при храме", "temple scribes", 4.0, 3, "Носишь таблички за писцами.", "You carry the tablets behind the scribes."),
                e("war", "воинов", "warriors", 4.0, 2, "{Учишься держать копьё.|Мелешь зерно для всей дружины.}", "{You learn to hold a spear.|You grind grain for the whole war band.}"),
                e("noble", "знатного рода", "a noble house", 1.0, 3, "Учишься приказывать слугам.", "You learn to give orders to servants."),
            )),
        Era(2, Group.MED, Tx("Раннее средневековье", "Early Middle Ages"), Tx("1 – 1200", "1 – 1200"), 1, 1200, 27.0, .40, Triple(8, 42, 85), .05, 6.0, 0xFF5C6E3A,
            listOf(
                r("cn", "Китай", "China", 26.0, 112, 33, "cn", "cn"),
                r("in", "Индия", "India", 24.0, 79, 22, "in", "in"),
                r("me", "Ближний Восток", "The Middle East", 10.0, 42, 33, "me", "me eg"),
                r("fr", "Франция", "France", 5.0, 2, 47, "fr", "eu fr"),
                r("it", "Италия", "Italy", 4.0, 12, 43, "it", "eu it med", true),
                r("de", "Германия", "Germany", 3.0, 10, 51, "de", "eu de"),
                r("ru", "Русь", "Rus'", 3.0, 37, 55, "ru", "ru"),
                r("waf", "Западная Африка", "West Africa", 11.0, 0, 11, "af", "af"),
                r("jp", "Япония", "Japan", 3.0, 138, 36, "jp", "jp", true),
                r("sea", "Юго-Восточная Азия", "Southeast Asia", 5.0, 104, 13, "sea", "sea", true),
                r("meso", "Мезоамерика", "Mesoamerica", 4.0, -98, 19, "am", "meso am"),
                r("andes", "Анды", "The Andes", 2.0, -73, -13, "am", "andes am"),
                r("au", "Австралия", "Australia", .5, 135, -25, "au", "au", true),
            ),
            medEstates(.04)),
        Era(3, Group.MED, Tx("Позднее средневековье", "Late Middle Ages"), Tx("1200 – 1650", "1200 – 1650"), 1200, 1650, 12.0, .40, Triple(8, 44, 85), .10, 6.0, 0xFF3F6A6A,
            listOf(
                r("cn", "Китай", "China", 24.0, 112, 33, "cn", "cn"),
                r("in", "Индия", "India", 22.0, 79, 22, "in", "in"),
                r("sea", "Юго-Восточная Азия", "Southeast Asia", 6.0, 104, 13, "sea", "sea", true),
                r("jp", "Япония", "Japan", 4.0, 138, 36, "jp", "jp", true),
                r("me", "Ближний Восток", "The Middle East", 7.0, 42, 33, "me", "me eg"),
                r("fr", "Франция", "France", 5.0, 2, 47, "fr", "eu fr"),
                r("de", "Германия", "Germany", 4.0, 10, 51, "de", "eu de"),
                r("it", "Италия", "Italy", 4.0, 12, 43, "it", "eu it med", true),
                r("en", "Англия", "England", 2.0, -1, 52, "en", "eu en", true),
                r("ru", "Русь", "Rus'", 3.0, 37, 55, "ru", "ru"),
                r("waf", "Западная Африка", "West Africa", 12.0, 0, 11, "af", "af"),
                r("meso", "Мезоамерика", "Mesoamerica", 4.0, -98, 19, "am", "meso am"),
                r("andes", "Анды", "The Andes", 3.0, -73, -13, "am", "andes am"),
                r("au", "Австралия", "Australia", .4, 135, -25, "au", "au", true),
            ),
            medEstates(.02)),
        Era(4, Group.EARLY, Tx("Новое время", "Early modern"), Tx("1650 – 1850", "1650 – 1850"), 1650, 1850, 9.0, .37, Triple(8, 48, 90), .25, 6.0, 0xFF3D4F7A,
            listOf(
                r("cn", "Китай", "China", 30.0, 112, 33, "cn", "cn"),
                r("in", "Индия", "India", 20.0, 79, 22, "in", "in"),
                r("sea", "Юго-Восточная Азия", "Southeast Asia", 6.0, 104, 13, "sea", "sea", true),
                r("jp", "Япония", "Japan", 4.0, 138, 36, "jp", "jp", true),
                r("me", "Ближний Восток", "The Middle East", 5.0, 42, 33, "me", "me eg"),
                r("fr", "Франция", "France", 4.0, 2, 47, "fr", "eu fr"),
                r("de", "Германия", "Germany", 4.0, 10, 51, "de", "eu de"),
                r("en", "Англия", "England", 3.0, -1, 52, "en", "eu en", true),
                r("it", "Италия", "Italy", 3.0, 12, 43, "it", "eu it med", true),
                r("ru", "Россия", "Russia", 6.0, 38, 55, "ru", "ru"),
                r("waf", "Западная Африка", "West Africa", 10.0, 0, 11, "af", "af"),
                r("latam", "Латинская Америка", "Latin America", 4.0, -60, -12, "la", "latam meso andes am"),
                r("us", "Северная Америка", "North America", 1.5, -78, 40, "us", "us", true),
                r("au", "Австралия", "Australia", .2, 135, -25, "au", "au", true),
            ),
            listOf(
                e("farm", "крестьян", "peasants", 66.0, 1, FIELD.ru, FIELD.en),
                e("serf", "крепостных", "serfs", 14.0, 0, "Работаешь три дня в неделю на барина, остальное — на себя.", "Three days a week you work for the master, the rest for yourselves.", "ru"),
                e("slave", "невольников", "enslaved people", 6.0, 0, "Работаешь на плантации от рассвета до темноты.", "You work on the plantation from dawn to dark.", "latam us af"),
                e("craft", "ремесленников", "craftsmen", 9.0, 2, "{Стоишь у горна.|Шьёшь на заказ.}", "{You stand at the forge.|You sew to order.}"),
                e("trade", "торговцев", "merchants", 6.0, 2, "Стоишь за прилавком.", "You stand behind the counter."),
                e("clergy", "духовенства", "clergy", 2.0, 3, "Помогаешь отцу в храме: свечи, молитвы, похороны.", "You help your father at the temple: candles, prayers, funerals."),
                e("noble", "дворян", "gentry", 2.0, 3, "Учишься письму, верховой езде и манерам.", "You learn letters, riding and manners."),
            )),
        Era(5, Group.IND, Tx("Век машин", "Age of machines"), Tx("1850 – 1950", "1850 – 1950"), 1850, 1950, 8.0, .28, Triple(8, 58, 95), .5, 5.0, 0xFF6A3F6E,
            listOf(
                r("cn", "Китай", "China", 22.0, 112, 33, "cn", "cn"),
                r("in", "Индия", "India", 18.0, 79, 22, "in", "in"),
                r("sea", "Юго-Восточная Азия", "Southeast Asia", 7.0, 104, 13, "sea", "sea", true),
                r("jp", "Япония", "Japan", 4.0, 138, 36, "jp", "jp", true),
                r("me", "Ближний Восток", "The Middle East", 5.0, 42, 33, "me", "me eg"),
                r("fr", "Франция", "France", 4.0, 2, 47, "fr", "eu fr"),
                r("de", "Германия", "Germany", 4.0, 10, 51, "de", "eu de"),
                r("en", "Англия", "England", 4.0, -1, 52, "en", "eu en", true),
                r("it", "Италия", "Italy", 4.0, 12, 43, "it", "eu it med", true),
                r("ru", "Россия", "Russia", 9.0, 38, 55, "ru", "ru"),
                r("af", "Африка", "Africa", 10.0, 15, 5, "af", "af"),
                r("latam", "Латинская Америка", "Latin America", 6.0, -60, -12, "la", "latam meso andes am"),
                r("us", "США", "The United States", 5.0, -90, 40, "us", "us", true),
                r("au", "Австралия", "Australia", .5, 145, -32, "au", "au", true),
            ),
            listOf(
                e("farm", "крестьян", "peasants", 52.0, 1, FIELD.ru, FIELD.en),
                e("mill", "фабричных рабочих", "mill hands", 20.0, 1, "Идёшь на фабрику: двенадцать часов у станка.", "You go to the mill: twelve hours at the machine."),
                e("craft", "ремесленников", "craftsmen", 10.0, 2, "Учишься ремеслу отца.", "You learn your father's trade."),
                e("shop", "лавочников", "shopkeepers", 8.0, 2, "Стоишь за прилавком.", "You stand behind the counter."),
                e("clerk", "служащих", "clerks", 6.0, 2, "Переписываешь бумаги в конторе.", "You copy papers in an office."),
                e("rich", "богатых", "the wealthy", 1.5, 3, "Учишься музыке и языкам.", "You learn music and languages."),
            )),
        Era(6, Group.MOD, Tx("Наше время", "Our time"), Tx("1950 – 2026", "1950 – 2026"), 1950, 2026, 15.0, .07, Triple(10, 74, 100), .85, 2.6, 0xFF8F2F22,
            listOf(
                r("cn", "Китай", "China", 18.0, 112, 33, "cn", "cn"),
                r("in", "Индия", "India", 19.0, 79, 22, "in", "in"),
                r("sea", "Юго-Восточная Азия", "Southeast Asia", 9.0, 104, 13, "sea", "sea", true),
                r("jp", "Япония", "Japan", 2.0, 138, 36, "jp", "jp", true),
                r("me", "Ближний Восток", "The Middle East", 7.0, 42, 33, "me", "me eg"),
                r("eu", "Европа", "Europe", 6.0, 10, 49, "de", "eu de fr it en"),
                r("ru", "Россия", "Russia", 3.0, 38, 55, "ru", "ru"),
                r("af", "Африка", "Africa", 22.0, 15, 5, "af", "af"),
                r("latam", "Латинская Америка", "Latin America", 9.0, -60, -12, "la", "latam meso andes am"),
                r("us", "США", "The United States", 5.0, -90, 40, "us", "us", true),
                r("au", "Австралия", "Australia", .4, 145, -32, "au", "au", true),
            ),
            listOf(
                e("farm", "крестьян", "smallholders", 33.0, 1, "Работаешь в поле до и после школы.", "You work the field before and after school."),
                e("work", "рабочих", "workers", 25.0, 1, "Подрабатываешь на рынке.", "You earn a little at the market."),
                e("poor", "городской бедноты", "the urban poor", 15.0, 0, "Продаёшь воду на перекрёстке.", "You sell water at a crossroads."),
                e("office", "служащих", "office workers", 19.0, 2, "Делаешь уроки за кухонным столом.", "You do your homework at the kitchen table."),
                e("well", "обеспеченных", "the well-off", 6.0, 3, "Ходишь в кружок робототехники.", "You go to a robotics club."),
                e("rich", "очень богатых", "the very rich", 1.0, 3, "На каникулы летаешь в другие страны.", "You fly abroad for the holidays."),
            )),
    )

    val ALL_REGIONS: List<Region> get() = ERAS.flatMap { it.regions }

    fun region(key: String, eraI: Int): Region = ERAS[eraI].regions.firstOrNull { it.key == key } ?: ALL_REGIONS.first { it.key == key }

    fun eraOf(year: Int): Era = ERAS.lastOrNull { year >= it.from } ?: ERAS[0]

    /** Cultures change hands: the Americas speak Spanish after the conquest, Australia English after the ships. */
    fun culture(r: Region, year: Int): String = when {
        r.cult == "am" && year > 1540 -> "la"
        r.cult == "au" && year > 1850 -> "en"
        else -> r.cult
    }
}

private fun medEstates(serfs: Double) = listOf(
    e("farm", "крестьян", "peasants", 78.0, 1, FIELD.ru, FIELD.en),
    e("bond", "холопов", "bondsmen", serfs * 100, 0, "Работаешь на господском дворе без платы.", "You work in the lord's yard for no pay."),
    e("craft", "ремесленников", "craftsmen", 8.0, 2, "{Стоишь у горна.|Помогаешь в лавке.}", "{You stand at the forge.|You help in the shop.}"),
    e("trade", "торговцев", "merchants", 4.0, 2, "Считаешь мешки и учишь цены.", "You count sacks and learn the prices."),
    e("temple", "служителей храма", "temple servants", 2.5, 3, "Подметаешь храм и учишь молитвы.", "You sweep the temple and learn the prayers."),
    e("war", "воинов", "warriors", 2.0, 2, "{Чистишь коня и оружие.|Шьёшь и стряпаешь на всю дружину.}", "{You groom the horse and clean the arms.|You sew and cook for the whole band.}"),
    e("noble", "знатного рода", "a noble house", 1.5, 3, "Учишься держаться в седле и при гостях.", "You learn to sit a horse and to sit with guests."),
)
