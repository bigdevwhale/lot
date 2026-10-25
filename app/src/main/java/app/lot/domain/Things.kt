package app.lot.domain

enum class Shape { NEEDLE, FLINT, BEADS, POUCH, FLUTE, TABLET, MIRROR, AMULET, COIN, CUP, SPOON, CHARM, COMB, WHISTLE, PIPE, LETTER, GLASSES, BUTTON, PHOTO, TICKET, THIMBLE, WATCH, PHONE, KEY, CASSETTE }

/** One thing a life leaves behind for the chest. */
/** [tags] keep a thing to the regions it belongs to; empty means anywhere. Keys never change: the chest stores them. */
class ThingKind(val key: String, val group: Group, val shape: Shape, val name: Tx, val tags: Set<String> = emptySet()) {
    fun fits(r: Region) = tags.isEmpty() || tags.any { it in r.tags }
}

object Things {
    private fun t(group: Group, shape: Shape, ru: String, en: String) = ThingKind("${group.name.lowercase()}.${shape.name.lowercase()}", group, shape, Tx(ru, en))

    /** A thing of one region; [id] makes its key unique next to the general thing of the same shape. */
    private fun l(group: Group, shape: Shape, id: String, tags: String, ru: String, en: String) =
        ThingKind("${group.name.lowercase()}.$id", group, shape, Tx(ru, en), tags.split(' ').filter { it.isNotBlank() }.toSet())

    val ALL = listOf(
        t(Group.STONE, Shape.NEEDLE, "костяная игла", "a bone needle"),
        t(Group.STONE, Shape.FLINT, "кремнёвый скребок", "a flint scraper"),
        t(Group.STONE, Shape.BEADS, "бусы из ракушек", "a string of shell beads"),
        t(Group.STONE, Shape.POUCH, "мешочек с охрой", "a pouch of ochre"),
        t(Group.STONE, Shape.FLUTE, "флейта из птичьей кости", "a bird-bone flute"),
        t(Group.ANC, Shape.TABLET, "глиняная табличка с долгом", "a clay tablet recording a debt"),
        t(Group.ANC, Shape.MIRROR, "бронзовое зеркальце", "a bronze hand mirror"),
        t(Group.ANC, Shape.AMULET, "амулет из синего камня", "a blue stone amulet"),
        t(Group.ANC, Shape.COIN, "медная монета", "a copper coin"),
        t(Group.ANC, Shape.CUP, "глиняная чашка", "a clay cup"),
        t(Group.MED, Shape.SPOON, "деревянная ложка", "a wooden spoon"),
        t(Group.MED, Shape.CHARM, "оловянный оберег", "a pewter charm"),
        t(Group.MED, Shape.COIN, "серебряная монетка", "a little silver coin"),
        t(Group.MED, Shape.COMB, "костяной гребень", "a bone comb"),
        t(Group.MED, Shape.WHISTLE, "ивовая свистулька", "a willow whistle"),
        t(Group.EARLY, Shape.CHARM, "медный амулет на шнурке", "a copper charm on a cord"),
        t(Group.EARLY, Shape.PIPE, "глиняная трубка", "a clay pipe"),
        t(Group.EARLY, Shape.LETTER, "письмо, которое некому было прочитать", "a letter nobody could read"),
        t(Group.EARLY, Shape.SPOON, "расписная ложка", "a painted spoon"),
        t(Group.EARLY, Shape.GLASSES, "очки в проволочной оправе", "wire-rimmed spectacles"),
        t(Group.IND, Shape.BUTTON, "пуговица от шинели", "a greatcoat button"),
        t(Group.IND, Shape.PHOTO, "фотокарточка в картонной рамке", "a photograph in a card frame"),
        t(Group.IND, Shape.TICKET, "железнодорожный билет", "a railway ticket"),
        t(Group.IND, Shape.THIMBLE, "напёрсток", "a thimble"),
        t(Group.IND, Shape.WATCH, "карманные часы", "a pocket watch"),
        t(Group.MOD, Shape.PHONE, "кнопочный телефон", "a push-button phone"),
        t(Group.MOD, Shape.LETTER, "школьный аттестат", "a school certificate"),
        t(Group.MOD, Shape.KEY, "ключ от квартиры", "a flat key"),
        t(Group.MOD, Shape.BEADS, "пластиковые бусы", "plastic beads"),
        t(Group.MOD, Shape.CASSETTE, "кассета с песнями", "a mixtape"),
        l(Group.STONE, Shape.AMULET, "bear", "eu", "зуб пещерного медведя на шнурке", "a cave bear's tooth on a cord"),
        l(Group.STONE, Shape.CHARM, "ivory", "eu", "фигурка из бивня мамонта", "a mammoth-ivory figurine"),
        l(Group.STONE, Shape.BEADS, "ostrich", "af", "бусы из скорлупы страусиных яиц", "ostrich-eggshell beads"),
        l(Group.ANC, Shape.AMULET, "jade", "cn", "нефритовая подвеска", "a jade pendant"),
        l(Group.ANC, Shape.COIN, "spade", "cn", "бронзовая монета в виде лопатки", "a bronze spade coin"),
        l(Group.ANC, Shape.AMULET, "scarab", "eg", "фаянсовый скарабей", "a faience scarab"),
        l(Group.ANC, Shape.CUP, "lamp", "med me", "глиняный светильник", "a clay oil lamp"),
        l(Group.ANC, Shape.BEADS, "quipu", "andes", "шнурок с узелками кипу", "a knotted quipu cord"),
        l(Group.ANC, Shape.BEADS, "jadeite", "meso", "бусина из жадеита", "a jadeite bead"),
        l(Group.ANC, Shape.LETTER, "palm", "in", "листья пальмы с письменами", "a palm-leaf manuscript"),
        l(Group.ANC, Shape.BEADS, "cowrie", "af in", "нитка раковин каури", "a string of cowrie shells"),
        l(Group.MED, Shape.BEADS, "rosary", "me in jp sea", "чётки", "prayer beads"),
        l(Group.MED, Shape.LETTER, "birch", "ru", "берестяная грамота", "a birch-bark letter"),
        l(Group.MED, Shape.CUP, "tea", "cn jp", "чайная чашка", "a tea bowl"),
        l(Group.MED, Shape.CHARM, "cross", "eu ru", "медный нательный крестик", "a small copper cross"),
        l(Group.MED, Shape.AMULET, "scallop", "eu", "раковина паломника", "a pilgrim's scallop shell"),
        l(Group.MED, Shape.TABLET, "salt", "af", "плитка соли", "a slab of salt"),
        l(Group.MED, Shape.TICKET, "jiaozi", "cn", "бумажная ассигнация", "a paper banknote"),
        l(Group.MED, Shape.POUCH, "cacao", "meso", "мешочек какао-бобов", "a pouch of cacao beans"),
        l(Group.MED, Shape.CUP, "ewer", "me eg", "медный кувшинчик", "a little copper ewer"),
        l(Group.EARLY, Shape.CUP, "porcelain", "cn", "фарфоровая чашка", "a porcelain cup"),
        l(Group.EARLY, Shape.POUCH, "tobacco", "eu ru us latam", "кисет", "a tobacco pouch"),
        l(Group.EARLY, Shape.NEEDLE, "quill", "eu ru en fr de it", "гусиное перо", "a goose quill"),
        l(Group.EARLY, Shape.LETTER, "freedom", "ru latam us", "вольная грамота", "a letter of freedom"),
        l(Group.EARLY, Shape.COIN, "real", "latam", "серебряный реал", "a silver real"),
        l(Group.EARLY, Shape.BEADS, "netsuke", "jp", "нэцкэ на шнурке", "a netsuke on a cord"),
        l(Group.IND, Shape.LETTER, "postcard", "eu ru it de en fr", "открытка из Америки", "a postcard from America"),
        l(Group.IND, Shape.BUTTON, "medal", "eu ru de fr en it us", "медаль за войну", "a war medal"),
        l(Group.IND, Shape.TICKET, "union", "eu en de us", "профсоюзный билет", "a union card"),
        l(Group.IND, Shape.KEY, "chest", "", "ключ от сундука", "a key to the chest"),
        l(Group.MOD, Shape.PHONE, "radio", "", "транзисторный приёмник", "a transistor radio"),
        l(Group.MOD, Shape.BUTTON, "badge", "ru", "комсомольский значок", "a Komsomol badge"),
        l(Group.MOD, Shape.TICKET, "pass", "", "пропуск на завод", "a factory pass"),
        l(Group.MOD, Shape.PHOTO, "wedding", "", "свадебная фотография", "a wedding photograph"),
    )

    /** A thing for a life in [g] born in [r]. */
    fun pick(g: Group, r: Region, rnd: kotlin.random.Random): ThingKind = ALL.filter { it.group == g && it.fits(r) }.random(rnd)

    fun of(key: String): ThingKind = ALL.firstOrNull { it.key == key } ?: ALL[0]
    fun group(g: Group) = ALL.filter { it.group == g }
}
