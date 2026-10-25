package app.lot.domain

enum class Shape { NEEDLE, FLINT, BEADS, POUCH, FLUTE, TABLET, MIRROR, AMULET, COIN, CUP, SPOON, CHARM, COMB, WHISTLE, PIPE, LETTER, GLASSES, BUTTON, PHOTO, TICKET, THIMBLE, WATCH, PHONE, KEY, CASSETTE }

/** One thing a life leaves behind for the chest. */
class ThingKind(val key: String, val group: Group, val shape: Shape, val name: Tx)

object Things {
    private fun t(group: Group, shape: Shape, ru: String, en: String) = ThingKind("${group.name.lowercase()}.${shape.name.lowercase()}", group, shape, Tx(ru, en))

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
    )

    fun of(key: String): ThingKind = ALL.firstOrNull { it.key == key } ?: ALL[0]
    fun group(g: Group) = ALL.filter { it.group == g }
}
