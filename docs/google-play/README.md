# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Lot-v<версия>.aab`.
  Первая версия для Play — **v1.3.0**: AAB добавляется к уже вышедшему релизу повторным запуском
  Actions → Release → Run workflow → `1.3.0`, без нового релиза и поста в Telegram. Секреты `SIGNING_*` в репозитории заданы;
  без них сборка подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.lot`.
  2. **Select key** → ключ с SHA-256 `1fc2412b70cbb7b2e129af70b0ef24f1c5e4c7404984b5b48cb4fef538eaf8f1`
     (сертификат CN=Lot, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке оставьте вариант «Google создаст ключ подписи».
  Ваш keystore станет ключом загрузки (upload key) — храните его.
  ⚠️ Из-за этого APK из GitHub и версия из Play будут подписаны разными ключами: поверх друг друга они
  не обновятся. Если нужно, чтобы совпадали, выберите «Использовать свой ключ» и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге).
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/lot/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Lot: The Lottery of Birth` |
| Default language | English (United States) – en-US |
| App or game | **Game** (см. «Категория» в разделе 3) |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…7.png` | `graphics/screenshots-ru/1…7.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из рендеров README (`docs/screenshots/<язык>`):
Play не берёт картинки длиннее двух ширин, а рендеры 1080×2341, поэтому экран уходит за нижний край
под подписью. Как пересобрать:

```bash
./gradlew testDebugUnitTest -Prender --tests '*RenderArtScreens*' && python tools/shots.py app/build/art
python tools/play.py   # draw, book, end, atlas, chest, thing, welcome
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Game → **Simulation** |
| Tags (до 5) | Life simulation, Simulation, Educational, Single player, Offline (берите ближайшие из списка Play) |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/lot/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** «Жребий» — симулятор жизни (как BitLife), и в Play такие лежат в Game → Simulation:
там их и ищут. Альтернатива — Application → **Education**, если хочется подчеркнуть историю; тогда
в разделе 1 выберите App, а в анкете рейтинга — All Other App Types.

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **Game** (для Education — All Other App Types).
- В тексте жизней есть смерть (в том числе детей), войны, чума, рабство, побои, казни и упоминания
  пива, вина и табака — только словами, без изображений и без поощрения. Где анкета спрашивает
  о **текстовых упоминаниях** насилия, смерти или алкоголя — отвечайте **Yes** (без крови, без
  реалистичного изображения, не поощряется); где об изображённом насилии, крови, сексуальном контенте,
  ненормативной лексике, наркотиках, азартных играх — **No**. Ожидаемый рейтинг — примерно 12+/Teen.
- **Users interact / share content** — **No**: сервера, чата и профилей нет.
- Цифровые покупки, передача местоположения, браузер — **No**.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over** (политика: приложение не для детей младше 13).
- *Could your store listing unintentionally appeal to children?* → **No**: метрическая книга,
  историческая тематика, без детских героев. Если Play всё же отнесёт к детской аудитории, понадобится
  соответствие политике Families — тогда пишите, разберём.

**News apps** — **No.**

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**
- *Does your app collect or share any of the required user data types?* → **No.**
  У приложения нет разрешения `INTERNET`: ничего не может покинуть телефон. Ссылки на сообщество
  и GitHub в настройках открывают Telegram или браузер по нажатию — это не сбор данных разработчиком.
- *Is all of the user data collected by your app encrypted in transit?* и вопрос об удалении данных
  после ответа «No» не появятся.
- Системный бэкап Android (`allowBackup="true"`) в Data safety не декларируется.
- В карточке будет «No data collected».

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — No.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Permissions** — деклараций не нужно: релизный APK не запрашивает ни одного разрешения
(единственное в манифесте — `app.lot.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, служебное,
его добавляет androidx.core).

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Lot-v1.3.0.aab` из GitHub Release.
3. Release name: `1.3.0`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу; жребий тянется, книга листается, сундук и атлас открываются.

## Следующие версии

- `release_notes.txt` переписать под новую версию (для первой там «First release on Google Play»).
- Скриншоты пересобирать, только если поменялись показанные экраны.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.lot`.
