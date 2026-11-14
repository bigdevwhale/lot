"""Every generated picture in the app: id -> (kind, subject). `python tools/art/gen.py` asks Codex to paint the missing ones,
`python tools/art/pack.py` turns the raw PNGs into the WebPs under app/src/main/assets/art."""

THING_STYLE = (
    "Museum catalogue illustration of one single historical object: {s}. Hand-painted gouache with fine sepia ink linework, "
    "warm muted earthy palette (umber, ochre, faded red, verdigris, bone), soft light from the upper left, three-quarter view. "
    "The object is alone, centered, filling about 75% of a square frame. Fully transparent background, no cast shadow, "
    "no ground, no text, no labels, no frame, no hands."
)
ERA_STYLE = (
    "Wide landscape vignette, 3:2. Hand-painted gouache with fine sepia ink linework in a warm muted parchment palette, "
    "like a plate in an old history book. The scene fades softly at all edges into a fully transparent background (an oval vignette). "
    "Quiet everyday life seen from a distance, small figures, historically and regionally accurate buildings, dress and landscape, "
    "no close-up faces, no text, no frame: {s}."
)
COLOR_STYLE = (
    "Hand-painted gouache illustration with fine sepia ink linework, warm muted earthy palette, soft light from the upper left. "
    "Centered, square frame, fully transparent background, no ground shadow, no text: {s}."
)
ICON_STYLE = (
    "A small emblem icon, drawn as a bold, simple engraved woodcut silhouette in one single flat colour, dark umber #2A1A10, "
    "on a fully transparent background. Thick clean lines, readable at 24 pixels, no shading gradients, no text, no frame, "
    "centered in a square: {s}."
)

THINGS = {
    "stone.needle": "a prehistoric bone sewing needle with a drilled eye, polished ivory-coloured bone",
    "stone.flint": "a knapped grey-brown flint scraper with conchoidal flake scars",
    "stone.beads": "a string of small drilled seashell beads on a thin sinew cord, laid in a loose curve",
    "stone.pouch": "a small leather pouch tied with a thong, spilling a little red ochre powder",
    "stone.flute": "a palaeolithic flute made from a hollow bird bone with four finger holes",
    "anc.tablet": "a Mesopotamian clay tablet with cuneiform wedge marks recording a debt",
    "anc.mirror": "an ancient polished bronze hand mirror, round disc with a short cast handle, green patina at the rim",
    "anc.amulet": "a plain smooth oval pendant of polished blue lapis lazuli stone, drilled and hung on a simple cord, no carving, no figure, no metal setting",
    "anc.coin": "an ancient worn copper coin with a crude abstract stamped mark, no animal, no face, no letters",
    "anc.cup": "a simple ancient hand-thrown unglazed terracotta drinking cup without a handle",
    "med.spoon": "a medieval carved wooden spoon, worn with use",
    "med.charm": "a small cast pewter charm with a simple sun-wheel pattern and a loop for a cord, no cross, no figure",
    "med.coin": "a small thin hammered silver coin, worn, with a simple abstract dotted pattern, no cross, no face, no letters",
    "med.comb": "a medieval double-sided bone comb with riveted side plates",
    "med.whistle": "a simple carved willow-bark whistle",
    "early.charm": "a small copper charm pendant on a waxed cord, 17th century",
    "early.pipe": "a long white clay tobacco pipe, 17th century, with a small bowl",
    "early.letter": "a folded 18th-century paper letter with a broken red wax seal and faded iron-gall handwriting (illegible scribbles, no real letters)",
    "early.spoon": "a carved wooden spoon painted with simple red and black bands and dots, worn with use",
    "early.glasses": "a pair of 18th-century round wire-rimmed spectacles",
    "ind.button": "a large brass military greatcoat button with a plain embossed rim and a shank, slightly tarnished, no emblem",
    "ind.photo": "a sepia studio portrait photograph of a family in an old cardboard cabinet-card frame",
    "ind.ticket": "an old printed cardboard railway ticket, early 20th century, with a punched hole (no readable text)",
    "ind.thimble": "a dimpled steel sewing thimble",
    "ind.watch": "a brass pocket watch with a white enamel dial and a short chain",
    "mod.phone": "a 2000s push-button mobile phone with a small green-grey screen",
    "mod.letter": "a school leaving certificate, a slightly creased official paper sheet with an ornamental border, an ink stamp and faint ruled lines, absolutely no letters or words",
    "mod.key": "a modern flat apartment key with a plastic tag on a small ring",
    "mod.beads": "a necklace of cheap bright plastic beads",
    "mod.cassette": "an audio cassette mixtape with a hand-written paper label (scribbles, no readable text)",
    "stone.bear": "a cave bear's canine tooth pierced and hung on a leather cord",
    "stone.ivory": "a small palaeolithic mammoth-ivory figurine of a stylised animal",
    "stone.ostrich": "a strand of small round ostrich-eggshell disc beads",
    "anc.jade": "an ancient Chinese carved green jade pendant disc (bi) with a cord",
    "anc.spade": "an ancient Chinese bronze spade-shaped coin",
    "anc.scarab": "an Egyptian blue-green faience scarab amulet",
    "anc.lamp": "an ancient Mediterranean terracotta oil lamp with a nozzle and a small handle",
    "anc.quipu": "an Inca quipu, a main cord with many hanging knotted coloured strings",
    "anc.jadeite": "a single polished green Mesoamerican jadeite bead on a string",
    "anc.palm": "an Indian palm-leaf manuscript, a bundle of long narrow leaves tied between wooden boards, with incised script lines (illegible)",
    "anc.cowrie": "a string of white cowrie shells",
    "med.rosary": "a string of wooden prayer beads with a tassel",
    "med.birch": "a medieval Novgorod birch-bark letter, a curling strip of bark with scratched letters (illegible)",
    "med.tea": "a medieval East Asian stoneware tea bowl with a brown-black glaze",
    "med.cross": "a small medieval copper neck cross on a cord",
    "med.scallop": "a pilgrim's scallop shell pierced for a cord",
    "med.salt": "a rough rectangular slab of rock salt, as traded across the Sahara",
    "med.jiaozi": "an early Chinese paper banknote printed in black and red with a woodblock pattern (no readable characters)",
    "med.cacao": "a small woven cloth pouch spilling brown cacao beans",
    "med.ewer": "a small hammered copper ewer with a long spout and a handle",
    "early.porcelain": "a Chinese blue-and-white porcelain cup",
    "early.tobacco": "a leather drawstring tobacco pouch",
    "early.quill": "a goose-feather quill pen with an inked cut nib",
    "early.freedom": "a rolled 19th-century document of manumission with a wax seal and a ribbon (no readable text)",
    "early.real": "a Spanish colonial silver real coin, an irregular hand-struck cob",
    "early.netsuke": "a small carved ivory Japanese netsuke toggle in the shape of a sleeping cat, on a silk cord",
    "ind.postcard": "an old picture postcard showing a steamship and a harbour skyline (no readable text)",
    "ind.medal": "a bronze war medal hanging from a striped ribbon",
    "ind.union": "a small folded trade union membership card booklet with a stamp (no readable text)",
    "ind.chest": "a big old iron key for a wooden chest",
    "mod.radio": "a 1960s portable transistor radio in a leather case",
    "mod.badge": "a small red enamel Soviet youth pin badge with a profile medallion and a red flag (no readable text)",
    "mod.pass": "a laminated factory entry pass with a small ID photo (no readable text)",
    "mod.wedding": "a black-and-white wedding photograph in a simple frame",
}

# A plate per era and part of the world: a life in Song China must not get a Gothic town.
# Keys are "<era index>.<world>"; the worlds are the cultures of World.kt folded together (see Pics.world).
ERAS = {
    "0.any": "Stone Age: a small band of hunter-gatherers by a fire at the mouth of a rock shelter, hide windbreaks, a wide river valley with a distant herd of grazing animals",
    "0.au": "Ancient Australia: a small family group by a fire near a waterhole, red earth, eucalyptus trees, rock outcrops with ochre paintings, kangaroos far away",
    "1.cn": "Ancient China on the Yellow River: a village of rammed-earth houses with thatched roofs, millet fields, a pottery kiln, an ox cart, loess hills",
    "1.in": "Ancient India on the Ganges: a village of mud and bamboo houses by a wide river, rice paddies, zebu cattle, a banyan tree, a small boat",
    "1.me": "Ancient Near East: a mud-brick river town with a stepped temple, date palms, farmers with oxen at irrigation channels, a reed boat on the river",
    "1.eu": "Ancient Mediterranean: a whitewashed hillside town above a bay, olive trees and vines, a small temple with columns, a galley and fishing boats",
    "1.af": "Ancient West Africa: a village of round thatched houses and granaries in the savanna, millet fields, baobab trees, an iron-smelting furnace",
    "1.am": "Ancient Americas: a village of thatched houses among maize fields and terraces, a low earthen platform temple, mountains beyond",
    "1.au": "Ancient Australia: a camp by a river with bark shelters, people fishing with spears, eucalyptus trees, red cliffs",
    "2.cn": "Tang-era China: a walled town with tiled curved roofs and a pagoda, rice paddies with water buffalo, a canal with a barge",
    "2.in": "Early medieval India: a village by a stone temple with a carved tower, a stepped pond, rice fields, palm trees, an ox cart",
    "2.me": "Early medieval Middle East: a town of flat-roofed houses with a domed mosque and a minaret, a covered market, date palms, a camel caravan arriving",
    "2.eu": "Early medieval Europe: a small village of thatched houses with a small stone chapel, strip fields, peasants with sickles, forest beyond",
    "2.ru": "Early medieval Rus: a village of log houses on a river bank with a wooden palisade, a small wooden church, birch forest, a long boat on the river",
    "2.af": "Early medieval West Africa: a trading town of mud-brick houses by a river, a market under trees, canoes, a camel caravan from the desert",
    "2.jp": "Heian-era Japan: a village of thatched farmhouses among rice paddies, a wooden shrine gate, pine-covered hills, mist",
    "2.sea": "Early medieval Southeast Asia: a village of stilt houses by a river, rice paddies, a stone temple tower in the jungle, a long boat",
    "2.am": "Classic-era Americas: a town around a stepped stone pyramid, thatched houses, maize fields, a market in the plaza, forested hills",
    "2.au": "Australia: a family group gathering shellfish on a shore, bark canoes, smoke from a camp fire, eucalyptus woodland",
    "3.cn": "Ming-era China: a busy walled city with tiled roofs, a drum tower, a stone arch bridge over a canal with junks, market stalls",
    "3.in": "Late medieval India: a town below a hill fort, a domed tomb and a temple tower, a bazaar street, elephants and ox carts",
    "3.me": "Late medieval Middle East: a city of domes and minarets, a caravanserai, a busy bazaar, a camel caravan at the gate",
    "3.eu": "Late medieval Europe: a walled market town with a stone cathedral, carts at the gate, a windmill on a hill, strip fields",
    "3.ru": "Late medieval Russia: a wooden town with a white stone kremlin and an onion-domed church on a river, log houses, a market, boats",
    "3.af": "Late medieval West Africa: a great mud-brick mosque with wooden beams, a market town, a salt caravan of camels, canoes on the river",
    "3.jp": "Muromachi-era Japan: a castle town of wooden houses below a small hilltop castle, rice paddies, a wooden bridge, pines",
    "3.sea": "Late medieval Southeast Asia: a river port with stilt houses, gilded temple spires, trading boats, rice fields and palms",
    "3.am": "Late pre-Columbian Americas: a stone city with terraces and a stepped temple, a causeway, a market, maize terraces on mountain slopes",
    "3.au": "Australia: people on a river bank with stone fish traps, bark shelters, tall gum trees, a wide dry plain beyond",
    "4.cn": "Qing-era China: a river town with tiled roofs and a pagoda, a tea terrace on the hill, junks on the river, a stone bridge",
    "4.in": "Early modern India: a town by a river with bathing steps, a domed mosque and a temple, a bazaar, bullock carts, a trading ship far off",
    "4.me": "Ottoman-era Middle East: a hillside town of domes and slender minarets, a covered bazaar, a coffee house, a harbour with sailing boats",
    "4.eu": "Early modern Europe: a harbour town with tall sailing ships, gabled houses, a church spire, merchants with barrels on the quay",
    "4.ru": "Imperial Russia: a village of log houses along a muddy road, an onion-domed church, a manor house on the hill, fields with peasants",
    "4.af": "Early modern West Africa: a coastal town of thatched and mud houses, a stone fort on the headland, canoes in the surf, a sailing ship offshore",
    "4.jp": "Edo-era Japan: a post town on a highway with wooden inns, travellers and porters, a wooden bridge, a snow-capped mountain beyond",
    "4.sea": "Early modern Southeast Asia: a spice port with stilt houses and a wooden mosque, sailing ships of several kinds, palms and rice fields",
    "4.am": "Colonial Latin America: a town around a plaza with a white baroque church, adobe houses with tiled roofs, mule trains, mountains",
    "4.us": "Early North America: a settlement of timber houses and a white meeting house, fenced fields, a water mill, forest all around",
    "4.au": "Colonial Australia: a few timber huts and a stone store by a bay, a sailing ship at anchor, gum trees, sheep on dry grass",
    "5.cn": "China around 1900: a treaty-port city with tiled roofs beside brick trading houses, a steamship and junks on the river, rickshaws",
    "5.in": "Colonial India: a railway station with a steam train, a crowded bazaar, a cotton mill chimney, temple towers and palms",
    "5.me": "Middle East around 1900: an old city of domes and minarets with a railway line and a steam train at its edge, telegraph poles, camels",
    "5.eu": "Industrial Europe: a steam train on a viaduct, factory chimneys smoking, rows of brick terraced houses, a church spire",
    "5.ru": "Russia around 1900: a provincial town with wooden houses, a brick factory with a chimney, an onion-domed church, a steam train",
    "5.af": "Colonial Africa: a railway through the savanna with a small station, a market town of tin-roofed houses, a steamboat on a river",
    "5.jp": "Meiji-era Japan: a town of tiled wooden houses with a brick factory and its chimney, a steam train, telegraph poles, a mountain beyond",
    "5.sea": "Colonial Southeast Asia: a port with steamships and sampans, rows of shophouses, a rubber plantation on the hills, palms",
    "5.am": "Latin America around 1900: a town with a plaza and a church, a steam train at a small station, a coffee plantation on the slopes",
    "5.us": "Industrial United States: a mill town with brick factories and chimneys, a steam locomotive, wooden houses, telegraph poles",
    "5.au": "Australia around 1900: a country town with verandahed shops, a steam train, a windmill pump, sheep paddocks and gum trees",
    "6.cn": "Modern China: a city of tall apartment towers behind an old lane of tiled roofs, bicycles and scooters, a high-speed train on a viaduct",
    "6.in": "Modern India: a crowded street with auto-rickshaws and buses, concrete apartment blocks, a temple tower, tangled wires overhead",
    "6.me": "Modern Middle East: a city of pale concrete apartment blocks with satellite dishes, a mosque dome and minaret, traffic, palms",
    "6.eu": "Modern Europe: a city street with a tram, old houses beside post-war apartment blocks, a church tower, bicycles",
    "6.ru": "Late Soviet and modern Russia: rows of prefabricated apartment blocks, a tram, a playground in the courtyard, birch trees",
    "6.af": "Modern Africa: a busy city market with minibuses, tin-roofed houses and new concrete towers beyond, mobile phone masts",
    "6.jp": "Modern Japan: a dense neighbourhood of small houses and apartment blocks, a commuter train on an elevated line, power lines, a mountain beyond",
    "6.sea": "Modern Southeast Asia: a street full of motorbikes, shophouses with blank signboards, a gilded temple roof, new towers beyond",
    "6.am": "Modern Latin America: a hillside of colourful self-built houses above a city of towers, a bus on a winding road, a cable car",
    "6.us": "Modern United States: a suburb of single houses with lawns and cars, a highway and a city skyline of towers beyond",
    "6.au": "Modern Australia: a suburb of bungalows with tin roofs near a beach, gum trees, a city skyline across the bay",
}

COLOR = {
    "empty_book": "an old closed leather-bound parish register with brass corners, a goose quill and an inkwell beside it",
    "empty_chest": "an old open wooden chest with iron bands, empty inside, lid raised",
    "register": "an open old parish register with ruled pages of faded handwriting (illegible), an inkwell and a quill lying on it",
}

# The draw: a sack and a blank tag; the app burns the lot's words into the tag and animates both.
STAGE = {
    "sack": "a plump hessian (burlap) sack standing upright on its base, front view, wider than it is tall, its mouth open at the top "
            "and gathered loosely by a thick rope drawstring tied in a small bow at the front, the dark inside of the open mouth clearly visible "
            "as an oval at the top, coarse woven texture, a few soft folds, warm straw and ochre tones",
    "tag": "a blank flat wooden tag seen straight from the front, not tilted: a horizontal rectangle about twice as wide as it is tall, "
           "with rounded right corners and the left end cut into a blunt point, one round drilled hole near the left point, "
           "light warm wood with fine visible grain and slightly scorched edges, a completely blank smooth face, no text, no marks, "
           "no string, no nail",
}

# The atlas: textures cut by the continents in code (the geography stays exact) and a few old-chart ornaments.
TEXTURE_STYLE = (
    "A flat seamless-looking texture, landscape 1536x1024, OPAQUE (no transparency), evenly lit, filling the whole frame edge to edge, "
    "no border, no vignette, no text, no shapes of land or coastlines: {s}."
)
MAP_TEXTURES = {
    "sea": "the sea on an antique hand-painted map: aged parchment with a faint blue-green watercolour wash, "
           "delicate engraved horizontal wave hatching, a few soft stains and foxing spots",
    "land": "the land on an antique hand-painted map: aged paper with a warm ochre and pale sienna watercolour wash, "
            "faint hand-drawn hill hatching and tiny tree marks scattered evenly, a few soft stains",
}
MAP_ORNAMENTS = {
    "compass": "an ornate antique compass rose with sixteen points, sepia ink with faded red and gold accents",
    "ship": "a small three-masted sailing ship with full sails on a few waves, as drawn on an antique map, sepia ink with light watercolour",
    "serpent": "a coiled sea serpent rising from the waves, as drawn on an antique map, sepia ink with light green watercolour",
}

# The launcher icon (tools/art/icon.py makes the mipmaps and the Play icon): painted layers of an adaptive icon.
APP = {
    "foreground": COLOR_STYLE.format(s="an app icon motif: a small plump hessian sack with its mouth open and a wooden tag on a string rising out of it, "
        "the tag tilted, with three short dark burnt lines on it instead of writing; bold, simple, chunky shapes that read at a tiny size, "
        "strong warm highlights, the whole motif compact and centered, filling about 70% of the frame"),
    "background": TEXTURE_STYLE.format(s="dark brown tooled leather of an old book binding, deep umber with a soft warm sheen in the middle, "
        "very subtle grain, no tooling patterns, no border"),
}

ICONS = {
    "tab_draw": "a cloth drawstring sack with a small wooden tag on a string rising out of it",
    "tab_book": "a thick old book, closed, with a ribbon bookmark",
    "tab_chest": "an old wooden chest with iron bands and a lock, closed",
    "tab_atlas": "a compass rose with eight points",
    "census": "three small standing human figures of different heights, side by side",
    "encounters": "a classical profile head on a round medallion",
    "chronicle": "an hourglass",
    "notebook": "an inkwell with a quill standing in it",
    "share": "a dense crowd of many small human figures",
    "cradle": "a wooden rocking cradle",
    "literacy": "an open book seen from above",
    "people": "a single human figure with arms at the sides",
    "door_open": "an arched wooden door standing half open, light beyond",
    "door_shut": "an arched wooden door, closed, with a heavy padlock",
    "candle": "a burning candle",
    "lantern": "an old hand lantern with a small flame",
    "globe": "a globe on a stand",
    "signpost": "a wooden signpost with two arms pointing different ways",
    "quill": "a goose quill pen",
    "seal": "a round wax seal with a ribbon",
    "heart": "a heart held in two cupped open hands",
    "bug": "a beetle seen from above",
    "idea": "an oil lamp with a small bright flame and rays around it",
    "bell": "a hand bell with a wooden handle, ringing, small lines around it",
    "chat": "two overlapping speech scrolls",
}


def jobs():
    """(id, prompt, outpath relative to raw dir) for every picture."""
    out = []
    for k, s in THINGS.items():
        out.append((f"thing/{k}", THING_STYLE.format(s=s)))
    for k, s in ERAS.items():
        out.append((f"era/{k}", ERA_STYLE.format(s=s)))
    for k, s in COLOR.items():
        out.append((f"pic/{k}", COLOR_STYLE.format(s=s)))
    for k, s in STAGE.items():
        out.append((f"stage/{k}", COLOR_STYLE.format(s=s)))
    for k, s in MAP_TEXTURES.items():
        out.append((f"map/{k}", TEXTURE_STYLE.format(s=s)))
    for k, s in MAP_ORNAMENTS.items():
        out.append((f"map/{k}", COLOR_STYLE.format(s=s)))
    for k, p in APP.items():
        out.append((f"app/{k}", p))
    for k, s in ICONS.items():
        out.append((f"icon/{k}", ICON_STYLE.format(s=s)))
    return out
