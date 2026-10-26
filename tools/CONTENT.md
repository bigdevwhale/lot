# Lot content guide

Everything a life is made of lives in `app/src/main/assets`. Run `python tools/validate.py` after every edit; it must print `OK`.

## Voice

- The book is a parish register written about **you**: Russian in the 2nd person singular («Пашешь с отцом», «Впервые увидел{|а} море»), English in the 2nd person present ("You plough with your father").
- **Russian must read as if written by a Russian author, not translated.** Short, concrete, plain sentences. Natural word order. No calques from English, no bureaucratic words («осуществлять», «является», «данный»), no doubled genitives, no «который» chains. Read every line aloud. When in doubt, say less.
- English is equally plain: short sentences, concrete nouns, no purple prose.
- One or two sentences, at most ~160 characters per language. A detail beats a summary: «Отец привёз из города кусок сахара. Делили на всех», not «Жизнь была трудной, но были и радости».
- Past tense verbs that depend on the person's sex carry marks `{male|female}`: `{увидел|увидела}`, `{пошёл|пошла}`, `{Женился|Вышла замуж}`, `сам{|а}`. Use marks in English only where English really differs (`{wife|husband}`, `{son|daughter}`). Present tense needs no marks.
- History arrives from the side: rumours, prices, an army passing, a new crop, a sermon, a rule from far away. Never lecture, never moralise, never explain the inequality — show it.
- Joys matter as much as hardship. Weddings, food, songs, a good harvest, a first time for something.
- No anachronisms: no potatoes in Europe before 1570, no tea in medieval Europe, no paper money in Europe before 1660, no clocks before 1300, no rice in the Andes, no horses in the Americas before 1500, no Christianity in Japan unless tagged and dated, no church in China, etc. If a detail belongs to one culture, tag it.

## lines/<group>.tsv

Tab-separated, one line per row, 6 columns; lines starting with `#` are comments:

`group  kind  tags  ru  en  need`

- **group**: the file's group: `stone` (before 8000 BC), `anc` (8000 BC – AD 1), `med` (1 – 1650, both Middle Ages), `early` (1650 – 1850), `ind` (1850 – 1950), `mod` (1950 – 2026).
- **kind**:
  - `c` childhood (ages 2–10);
  - `y` youth (14–17);
  - `a` grown-up years (21–55), everyday life, work, family, neighbours;
  - `j` a small joy (any adult age);
  - `o` old age (58+);
  - `x` hard times (drought, war, plague, famine, fire, a cruel master): one line that opens a crisis.
- **tags**: empty = anywhere. Otherwise space-separated region tags, the line fits a region carrying ANY of them:
  `af` Africa · `me` Near/Middle East · `eg` Egypt · `med` Mediterranean (Greece, Rome) · `eu` Europe · `fr` France · `de` Germany/central Europe · `it` Italy · `en` England/Britain · `ru` Rus'/Russia · `in` India · `cn` China · `jp` Japan · `sea` Southeast Asia · `meso` Mesoamerica · `andes` Andes · `am` the Americas before 1500 · `latam` Latin America after 1500 · `us` North America/USA · `au` Australia.
  In every era but stone `sea` is Southeast Asia and nothing else. Only in `stone` lines, where no region is Southeast Asia, does `sea` mean "a coastal region". A line about the sea elsewhere uses `+sea` in need, not the tag.
- **need**: empty = anyone. Otherwise space-separated conditions, ALL must hold:
  `m` / `f` (male / female) · `r0` unfree (slave, serf, bondsman, urban poor today) · `r1` common (peasant, worker) · `r2` craft and trade · `r3` the top (noble, clergy, scribe, rich). Several `r` tokens mean any of them (`r2 r3`) ·
  `lit` / `illit` (can read / can't) · `married` · `single` · `kids` (has living children) · `monk` · `town` (went to town to work or learn).
  `after:YEAR` / `before:YEAR` keep a line to the years it is true (`after:1930` for the kolkhoz, `before:-500` for stone tools after the bronze came; negative = BC). The year is the one the life has reached at that page, not the birth year.
  `!TOKEN` negates (`!town` = has not moved to town). Marks a line can leave: `+town`, `+widowed` (lines about a living spouse carry `!widowed`), `+abroad` (after it the life gets only untagged lines, forks and events). `+sea` marks a line where the life sees the sea (a line tagged `sea` counts too); a "first time at the sea" line also asks `!sea`. After the monastery door a life gets only lines and forks whose need names `monk`. Lines about children of school age, grown children and grandchildren need `kids school`, `kids grown` or `kids grand`: 8, 16 and 36 years after the first child. `+town` is not a condition: a line carrying it makes the life a town life once written (a new flat in the city, the factory hostel), so later lines and forks about fields, harvests, cattle or rent (which carry `!town`) stop coming.
  Use need for lines that only make sense for some people: a slave's line, a noblewoman's line, a line about your children (`kids`), about reading (`lit`).
- Column text must not contain tabs.

Which regions exist in each era (and so which tags make sense):
- stone: af, me, eu, in, cn, am, au
- anc: cn, in, me (Mesopotamia), eg, med, af (West Africa), meso, andes, au
- med: cn, in, me, eg, fr, it, de, en, ru, af, jp, sea, meso, andes, au (eras 1–1200 and 1200–1650; the European regions also carry `eu`; in `med` the Americas are tagged `meso`/`andes`, pre-conquest life — the conquest itself arrives through history.tsv)
- early: cn, in, sea, jp, me, fr, de, en, it, ru, af, latam, us, au
- ind: cn, in, sea, jp, me, fr, de, en, it, ru, af, latam, us, au
- mod: cn, in, sea, jp, me, eu (incl. de fr it en), ru, af, latam, us, au

Families per era (for `need` r-tokens): stone — one band (r1). anc — farmers r1, slaves r0, craftsmen r2, temple scribes r3, warriors r2, nobles r3. med — peasants r1, bondsmen r0, craftsmen r2, merchants r2, temple servants r3, warriors r2, nobles r3. early — peasants r1, serfs r0 (ru only), enslaved r0 (latam us af), craftsmen r2, merchants r2, clergy r3, gentry r3. ind — peasants r1, mill hands r1, craftsmen r2, shopkeepers r2, clerks r2, the wealthy r3. mod — smallholders r1, workers r1, urban poor r0, office workers r2, well-off r3, very rich r3.

Most people who ever lived were peasants: most lines should fit a common rural life, with a good share tagged to a region so that China feels like China and the Andes like the Andes.

## history.tsv

`tags  from  to  meet  ru  en` — real events reaching a life from the side. `tags` are region tags as above, or `*` for anywhere from 1850 on. `from`/`to` are years (negative = BC). `meet` = 1 for a passing encounter with a person later written about (rare, gentle, plausible: Confucius passing with pupils, a young Marco Polo asking about paper money), otherwise 0. **Never reorder or edit existing rows: an event's row number is its id. Only append.**

## dilemmas*.txt

Every `dilemmas*.txt` file is read. Everyday forks, and (with `slot:`) the fixed crisis, legacy and last wish. Work at 12 and marriage stay in code. Blocks separated by a blank line:

```
id: well_dispute
groups: anc med early
tags: *
need:
ages: 20-55
weight: 1
q: Соседи спорят о колодце. | The neighbours quarrel over the well.
door: Встать на сторону родни | Side with your kin
out: 70 | Родня не забудет. Соседи тоже. | Your kin won't forget. Nor will the neighbours. | wealth+1
out: 30 | Дошло до драки. Тебе разбили бровь. | It comes to blows. Your brow is split. |
door: Помирить их | Make peace between them
shut: r0 | Тебя никто не станет слушать. | Nobody will listen to you.
out: 100 | Колодец теперь общий. Пьют все. | The well is shared now. Everyone drinks. | kind
ep: {Мирил|Мирила} соседей. | Made peace between neighbours.
```

- `groups`: one or more of stone anc med early ind mod. `tags`: region tags or `*`. `need`: as in lines (may be empty). `ages`: the range the fork can come at (forks come at 19, 27, 39, 51, 64). `weight`: 1 normal, 2 common, 0.5 rare.
- `q`: the question, `ru | en`.
- 2 or 3 `door` blocks. A door: `door: ru | en`, then optional `shut:` lines `cond | why ru | why en` (the door is shut when ALL conds hold; conds: `m f r0 r1 r2 r3 lit !lit poor rich married !married kids !kids monk town`; `poor` = nothing to spare, `rich` = well off), then one or more `out:` lines `weight | ru | en | effects`, then optionally `ep: ru | en` (a line for the epitaph if this door is taken) and `ask: ru | en` (a quiet question for the reader of today, about their own life, with no gendered verbs in Russian: «Кому ты в последний раз уступил…» is wrong, «Когда в последний раз пришлось уступить?» is right).
- **At least one door must be open for everyone the dilemma's `need` allows.** Shut doors are the point: show who could not take that door and why, in one plain line.
- Effects (space-separated, may be empty): `wealth+1 wealth-1 wealth+2 wealth-2` · `riskN` (N% chance of dying within three years: risk5…risk60) · `lit` (learns to read) · `sea` (sees the sea) · `kind` · `love` · `town` · `moved` · `free` (an unfree person becomes free) · `faith` · `brave` · `kids+1` · `kids-1` (a child dies) · `married` (marries now if single).
- Outcomes are not judgements. A kind choice can end badly, a selfish one well. That is life.

### Outcomes that know the century

An `out:` line may carry a fifth column, a need: `weight | ru | en | effects | need`. The outcome can only come when the need holds, so one fork can play out differently in different centuries and places. Besides the usual need tokens (`after:1930` and `before:1917` included, to keep an outcome to its years) it takes:

- `@stone @anc @med @early @ind @mod`: the era group;
- `#cn #eu #ru …`: a region tag;
- any token negated with `!`: `!@mod`, `!#cn`.

Among a door's outcomes, those whose need holds are drawn by weight. Every door keeps at least one outcome with no need, which is the fallback. A dilemma that spans several groups should give each group its own outcomes: in 1100 the stranger leaves a knife, in 1900 he leaves a newspaper and the address of a factory.

The same `@group` and `#tag` tokens work in `need:` and in `shut:` conditions, so a door can be shut only in some times: `shut: r0 !@mod | …`.

### The fixed forks: slot

A block with `slot: crisis`, `slot: legacy` or `slot: wish` is not an everyday fork. It fills one of the fixed places of every life:

- `crisis`: hard times, once in a life between 30 and 42 (`ages: 28-44`). A concrete calamity of its era and place: famine, plague, a raid, a flood, locusts, a war, a crash. Usually three doors: sit it out, leave, help others.
- `legacy`: what to leave behind, once at 45 or 48 (`ages: 43-50`). Those with children (`need: kids`) and those without (`need: !kids`) both need blocks.
- `wish`: the last wish at 70 (`ages: 68-72`).

Each slot draws one block that fits the life (era, region, need) with every check an everyday fork gets; a monk gets only blocks whose need names `monk`. So every era, every region and both monks and the unfree must have blocks in each slot.

Effects for these blocks besides the usual ones: `taught` (taught the children or novices to read: the epitaph says so), `tree` (planted a tree by the house), `wishsea` (sees the sea at last, in old age), `story` (tells a story; the outcome text holds `%story%` where the story goes, such as «Вечерами рассказываешь детям %story%.»; the story is "how great-grandfather outwitted a wolf", or the one the great-grandparent told).

## Checking

`python tools/validate.py` checks the columns, kinds, tags, needs, the gender marks, the dilemma syntax and that every dilemma has a door open to its audience. It prints counts per group and kind.
