# Wormhole Portals — техническая документация

Документ для разработчика: как устроен код, какие данные где лежат, как проходят основные сценарии. Геймдизайн (зачем всё это) описан в [GDD.md](GDD.md).

| | |
|---|---|
| Minecraft / NeoForge | 1.21.1 / 21.1.x, Java 21, ModDevGradle |
| modid | `eveportalnether` (не менять: от него зависят миры, конфиги и ресурспаки) |
| Отображаемое имя | Wormhole Portals |
| Базовый пакет | `com.eveportalnether` |
| Лицензия | MIT |

---

## 1. Сборка и запуск

```bash
./gradlew build          # jar в build/libs/wormhole-portals-<версия>.jar
./gradlew runClient      # клиент для разработки (папка run/)
./gradlew runServer      # dev-сервер; нужен run/eula.txt с eula=true
```

- Версия, имя и лицензия задаются в `gradle.properties` (`mod_version`, `mod_name`, `mod_license`) и подставляются в `src/main/templates/META-INF/neoforge.mods.toml`. Автор и описание записаны прямо в этом шаблоне.
- Имя jar задано в `build.gradle` (`base.archivesName = "wormhole-portals"`).
- Старые сборки сохраняются в `releases/`.
- Create в `build.gradle` не подключён: интеграция идёт только через data-рецепты с условием `neoforge:mod_loaded`, поэтому зависимости во время компиляции нет.

---

## 2. Карта пакетов

```
com.eveportalnether
├── EvePortalNether            точка входа (общая): регистрации, сеть, конфиг
├── EvePortalNetherClient      точка входа (клиент): рендер, частицы, HUD, тряска, нейм-теги
├── Config                     ModConfigSpec (COMMON)
├── block/
│   ├── WormholeBlock          блок рамки и окна: blockstate, проход, форма, анимация
│   └── entity/WormholeBlockEntity   блок-сущность (нужна рендереру BER)
├── world/
│   ├── PortalTier             классы U/S/M: вес, время, входы
│   ├── PortalColor            9 цветов, tint по стадии
│   ├── PortalShape            геометрия рамки, проверка места, карман посадки
│   ├── PortalSpawner          естественный спавн и спавн командой
│   ├── PortalEffects          все серверные эффекты: частицы, звуки, тряска, осколок
│   └── data/
│       ├── PortalPair         одна пара порталов (модель + NBT)
│       ├── PortalManager      SavedData со всеми парами; тик, распад, схлопывание, имплозия
│       └── PortalRecord       устаревший формат, нужен только для чтения старых сохранений
├── item/
│   ├── PortalRadarItem        радар
│   ├── PortalDetectorItem     детектор
│   └── DetectorDecryption     серверные сессии дешифровки
├── particle/SpiralParticleOptions   параметры частицы-спирали (общие для сервера и клиента)
├── network/                   payload'ы и регистрация (ModNetwork)
├── client/                    только клиент: ScanPanel, TransitScreen, SpiralParticle,
│                              WormholePulseRenderer, ClientPayloadHandler
├── events/                    подписчики на игровой шине NeoForge
├── advancement/               WormholeTrigger (критерий достижений), WormholeEvents (имена моментов)
├── command/ModCommands        /eveportal …
└── registry/                  DeferredRegister: блоки, предметы, BE, звуки, эффекты, частицы, триггеры
```

**Правило разделения сторон.** Всё из `client/` и `EvePortalNetherClient` загружается только на клиенте (`@Mod(dist = Dist.CLIENT)`). Серверный код не обращается к клиентским классам напрямую: обработчики пакетов вызывают `ClientPayloadHandler` внутри лямбды, которая на выделенном сервере не выполняется.

---

## 3. Модель данных

### 3.1 `PortalPair`
Одна запись — одна пара порталов (Верхний мир + Незер).

| Поле | Тип | Смысл |
|---|---|---|
| `id` | UUID | идентификатор пары |
| `overworldOrigin`, `netherOrigin` | BlockPos | нижний угол рамки в каждом измерении |
| `axis` | Direction.Axis | `X`: окно в плоскости Z/Y; `Z`: окно в плоскости X/Y |
| `width`, `height` | int | внешние размеры рамки |
| `tier` | PortalTier | класс |
| `initialTicks`, `initialJumps` | int | стартовые значения, нужны для расчёта долей |
| `ticksRemaining`, `jumpsRemaining` | int | остаток |
| `formingTicks` | int | больше 0, пока портал формируется |
| `color` | PortalColor | цвет |
| `code` | int 0–999 | цифры сигнатуры |

Производные значения:
- `getSignature()` → `"%c%c%c-%03d"` из букв класса, цвета и стадии и кода.
- `getDecay()` → 0..3:
  - 3, если портал формируется, в фазе предупреждения или у него остался 1 вход;
  - иначе по минимальной из долей время/входы: больше 0.5 → 0, больше 0.2 → 1, иначе 2.
- `getStageLetter()` → `N / F / W / C / D`.
- `isWarning()` — осталось не больше `warningDurationTicks` и портал не формируется.
- `isLastEntry()` — `jumpsRemaining == 1`.

NBT-ключи: `Id, OverworldOrigin, NetherOrigin, Axis, Width, Height, Tier, InitialTicks, InitialJumps, TicksRemaining, JumpsRemaining, FormingTicks, Color, Code`.

### 3.2 `PortalManager` (SavedData)
- Хранится в Верхнем мире под ключом `eveportalnether_portals`, доступ через `PortalManager.get(level)`.
- Старые сохранения с отдельными `PortalRecord` читаются и склеиваются в пары.
- Поиск: `findPairAt(dim, pos)` (какой паре принадлежит блок), `findNearest`, `isTooClose` (проверка дистанции при спавне), `isVisibleToRadar`.

### 3.3 Blockstate `eveportalnether:wormhole`

| Свойство | Значения | Смысл |
|---|---|---|
| `frame` | bool | `true` — рамка «Древний обсидиан», `false` — окно |
| `axis` | x / z | ориентация |
| `color` | 9 цветов | цвет окна (tint) и свечения |
| `decay` | 0–3 | стадия: tint сереет, свет 11/9/6/4 |

Модель — `multipart`: рамка — полный куб, окно — полупрозрачная плоскость с `tintindex 0`. Цвет считает `RegisterColorHandlersEvent.Block` через `PortalColor.tint(decay)`.

### 3.4 Данные на игроке и предметах
- Защита от возврата: эффект `spatial_trace` + UUID пары в persistent data игрока (ключ `EvePortalLastPair`).
- Радар: `CUSTOM_DATA.RadarTicks` — оставшееся время работы. Обновляется раз в секунду: если писать каждый тик, предмет в руке дёргается анимацией переэкипировки.

---

## 4. Серверный цикл

`events/PortalTickHandler` на `ServerTickEvent.Post` вызывает:

1. `PortalManager.tick(server)`:
   - **формирующиеся пары** уменьшают `formingTicks` и вызывают `PortalEffects.formingTick` (спираль-частицы, тон, тряска). Когда счётчик доходит до нуля, вызывается `PortalEffects.opened`;
   - **открытые пары** уменьшают `ticksRemaining` на 1;
   - раз в 10 тиков в фазе предупреждения — `playWarning`; раз в 40 тиков при последнем входе — предупреждение игрокам в радиусе 12;
   - раз в 3 тика — `PortalEffects.spiral` на обеих сторонах (спираль к центру, плотность по стадии);
   - истёкшие пары → `collapse(..., implode = true)`;
   - раз в 20 тиков — `syncDecay` (переписывает `color`/`decay` в блоках, если стадия сменилась) и `discardGhosts` (удаляет записи, у которых блоки исчезли).
2. `PortalSpawner.tick(server, manager)` — естественный спавн.
3. `DetectorDecryption.tick(server)` — активные дешифровки.

Время порталов идёт независимо от прогрузки чанков. Эффекты проверяют `level.hasChunkAt` и в выгруженных чанках не выполняются.

---

## 5. Сценарии

### 5.1 Спавн (`PortalSpawner`)
1. `effectiveCap(online) = clamp(floor(online × ratio), min, max)`. Если пар уже столько или больше, ничего не происходит.
2. Раз в `portalCheckIntervalTicks` бросается `spawnChancePerCheck`. Выбирается случайный игрок в Верхнем мире или Незере.
3. `trySpawnNear` делает до N попыток. На каждой:
   - случайная точка на дистанции 500–2000 от игрока;
   - новый бросок класса и размера (`PortalShape.rollWidth/rollHeight`);
   - проверка `isTooClose`;
   - поиск опоры `findGroundOrigin` или, с шансом 5%, `findAirOrigin`;
   - на второй стороне `findLinkedOrigin` в тех же координатах X/Z;
   - `isValid`: объём заменяем, есть опора.
4. `placePair` бросает цвет и код, ставит рамки на обеих сторонах (`WormholeBlock.placeStructure`), выставляет `formingTicks`, вызывает `syncDecay` и `PortalEffects.formingStarted`.

`forceSpawnLooking(player, tier)` (команда): луч взгляда до 48 блоков. Если он ни во что не попал — точка в 10 блоках впереди, но не ближе 4. Портал разворачивается лицом к игроку и опускается на землю (`dropToGround`).

### 5.2 Проход (`WormholeBlock.entityInside`)
1. Только `ServerPlayer`, окно (`frame=false`), без кулдауна портала у игрока.
2. Пара не найдена или ещё формируется → выход.
3. `isReturnLocked`: у игрока есть эффект и сохранённый UUID совпадает с этой парой → сообщение и звук отказа (раз в секунду), выход.
4. `consumeJump()`, вычисление точки выхода (`getStandPos` второй стороны), `PortalShape.carveLandingPocket`.
5. `PortalEffects.travelled`:
   - отправляет игроку **`TransitPayload(color, signature)`** — сигнал для экрана перехода;
   - частицы и звуки на входе и выходе, тряска.
6. `changeDimension(DimensionTransition)`, затем `applyReturnLock` (не в творческом режиме и не у наблюдателя).
7. Если входы кончились → `collapse(..., implode = true, exempt = игрок)`. Иначе `syncDecay` на обеих сторонах.

### 5.3 Схлопывание и имплозия (`PortalManager`)
- `collapse(server, pair, skipLevel, skipPos, implode, exempt)`:
  - удаляет пару из менеджера;
  - на каждой прогруженной стороне: при `implode` вызывает `implode` + `imploded` + `maybeDropShard`, иначе `closedQuietly`;
  - затем `WormholeBlock.removeStructure`.
- `implode(level, pair, exempt)`:
  - все живые сущности в радиусе `implosionRadius` получают урон `implosionDamage × 2 × (1.2 − 0.4 × falloff)` HP и отбрасывание от центра;
  - тип урона — **`eveportalnether:implosion`** (`PortalManager.IMPLOSION`), источник строится из реестра `Registries.DAMAGE_TYPE`.
- Датапак урона:
  - `data/eveportalnether/damage_type/implosion.json`: `scaling: never`, `exhaustion: 0`;
  - теги `minecraft:bypasses_armor`, `bypasses_enchantments`, `bypasses_resistance`, `bypasses_shield` в `data/minecraft/tags/damage_type/`;
  - `bypasses_invulnerability` намеренно не добавлен: творческий режим и тотем бессмертия продолжают защищать.
  - Сообщения о смерти: `death.attack.eveportalnether.implosion(.player)`.
- **Ломание блока.** `WormholeBlock.onRemove` — если блок принадлежит живой паре, пара закрывается. Флаг `REMOVING_STRUCTURE` (ThreadLocal) защищает от рекурсии, когда мод сам снимает рамку.

### 5.4 Радар (`PortalRadarItem`)
- **Зарядка** (Shift+ПКМ или ПКМ разряженным): тратит `radarBlazePowderCost` огненного порошка и добавляет `radarDurationTicks`.
- **Пассивный режим:** `inventoryTick` раз в секунду уменьшает заряд. В руке ищет ближайший видимый сигнал и пишет над хотбаром направление и дистанцию, выпускает частицу-указатель.
- **Полный скан** (ПКМ заряженным): список `radarScanCount` сигналов в чат, кулдаун `radarCooldownTicks`, `hurtAndBreak(1)`.
- Прочность `ModItems.RADAR_DURABILITY = 16`, починка осколком (`isValidRepairItem`).

### 5.5 Детектор и окно анализа
1. `PortalDetectorItem.useOn` по блоку портала → `DetectorDecryption.start(player, pair, pos)` + кулдаун.
2. `start` отправляет клиенту `PortalScanPayload.of(pair, decryptTicks)` — сразу все данные. Раскрывает их клиент, анимацией.
3. `DetectorDecryption.tick`: частицы вокруг игрока, щелчки по мере раскрытия символов.
   - Обрыв (дальше 7 блоков, детектор убран, пара исчезла) → `ScanLostPayload` + сообщение над хотбаром.
   - Успех → звук, `hurtAndBreak(1)` детектора (прочность 16).
4. Клиент: `ScanPanel.show(payload)`. Панель рисуется GUI-слоем поверх всего. Время на ней тикает локально от `ticksRemaining`.

### 5.6 Экран перехода (`client/TransitScreen`)
1. `TransitPayload` приходит **до** смены измерения, клиент запоминает цвет, сигнатуру и время.
2. Пока открыт `ReceivingLevelScreen` (стандартная «Загрузка местности»), обработчик `ScreenEvent.Render.Pre` **отменяет** его отрисовку и рисует свою:
   - как в ванили: спрайт окна `eveportalnether:block/wormhole_center` из атласа блоков, растянутый на весь экран и подкрашенный цветом портала. Атлас анимируется и во время загрузки, поэтому вихрь крутится. Перед спрайтом рисуется чёрная подложка и вызывается `g.flush()`: `fill` копится в буфере, а блит спрайта рисуется сразу, и без flush подложка легла бы поверх;
   - виньетку;
   - заголовок «Проход через червоточину», сигнатуру и «Стабилизация выхода…».
3. После закрытия экрана GUI-слой `renderArrival` ещё 1.4 с рисует тот же вихрь с затухающей альфой. Так переход виден, даже если загрузка длилась один кадр.
4. Состояние сбрасывается по таймеру, при выходе из мира и через 30 с как страховка.

### 5.7 Анонимность (`events/AnonymityHandler`)
- При входе в Незер игроку выдаётся ID `??_NNN`. `PlayerEvent.NameFormat` и `TabListNameFormat` подменяют имя. `refreshDisplayName` и `refreshTabListName` сразу применяют изменение.
- Выдача ID обновляется на логине, смене измерения и респауне и очищается на выходе.
- Клиенту уходит `AnonymityPayload(enabled)`. `RenderNameTagEvent` запрещает рисовать нейм-теги игроков, пока клиент в Незере.

### 5.8 Достижения (`advancement/`)
- **Триггер.** Один критерий `eveportalnether:wormhole` (`WormholeTrigger`, регистрируется в `ModTriggers` через `Registries.TRIGGER_TYPE`). В JSON момент выбирается условием `"conditions": {"event": "<имя>"}`. Без `event` критерий срабатывает на любой момент.
- **Имена моментов** — константы в `WormholeEvents`, вызов через `WormholeEvents.fire(player, событие)`:

| Событие | Где вызывается |
|---|---|
| `rumble` | `PortalEffects.announce` — игрок услышал далёкое открытие |
| `witness_opening` | `PortalEffects.opened` — игрок в радиусе 48 блоков |
| `travel`, `travel_<цвет>` | `WormholeBlock.entityInside` после прохода |
| `last_entry` | там же, если проход был последним |
| `return_locked` | там же, когда портал отказал из-за «Пространственного следа» |
| `decrypt`, `decrypt_massive` | `DetectorDecryption.tick` при завершении дешифровки |
| `survive_implosion` | `PortalManager.implode` — урон прошёл, а игрок жив (тотем или маленький `implosionDamage`) |
| `anonymous` | `AnonymityHandler.refresh`, когда игрок стал анонимным в Незере |

- **Файлы:** `data/eveportalnether/advancement/*.json` (14 штук), тексты — `advancements.eveportalnether.<id>.title/description`.
  - Корень `root` выдаётся триггером `minecraft:tick`, без тоста; фон вкладки — `textures/block/wormhole_frame.png`.
  - Предметные достижения (осколок, радар, детектор) используют ванильный `inventory_changed`.
  - «Спектр пустоты» требует все 9 критериев `travel_<цвет>`.
- **Новое достижение:** при необходимости добавить константу в `WormholeEvents` и вызов `fire`, затем JSON и строки в lang.

---

## 6. Визуал (клиент)

### 6.1 Частица `eveportalnether:wormhole_spiral`
- **Тип:** `ModParticles.WORMHOLE_SPIRAL`, параметры `SpiralParticleOptions(rgb)`. Для сохранения в команде используется `MapCodec`, для сети `StreamCodec`.
- **Соглашение о спавне:** `sendParticles(options, center, count = 0, xd = радиус, yd = угол, zd = плоскость, speed = 1)`. При `count = 0` ванильный пакет передаёт `xd/yd/zd × speed` в конструктор частицы как «скорость», а мы используем эти слоты как параметры орбиты. `zd = 1` — окно в плоскости X/Y, `0` — в плоскости Z/Y.
- **Поведение (`client/SpiralParticle`):**
  - частица живёт 34–52 тика;
  - радиус сжимается к центру, угловая скорость растёт в 3 раза к концу;
  - вертикальная ось орбиты вытянута в 1.35 раза под пропорции окна;
  - альфа плавно появляется и гаснет, размер уменьшается;
  - светится полной яркостью (`0xF000F0`), `PARTICLE_SHEET_TRANSLUCENT`.
- **Ресурсы:** `particles/wormhole_spiral.json` → 4 кадра `textures/particle/wormhole_spiral_0..3.png` (кадр выбирается по возрасту).
- **Кто спавнит:**
  - `PortalEffects.spiral` каждые 3 тика: 3/2/1/4 штуки по стадиям;
  - `formingTick` — три рукава каждый тик.

### 6.2 BER-пульсация (`client/WormholePulseRenderer`)
- Зарегистрирован для `WormholeBlockEntity` через `EntityRenderersEvent.RegisterRenderers`. Рамку пропускает.
- Для каждого блока окна рисует два квада (с обеих сторон плоскости, смещение ±0.03 от модели, чтобы не мерцали) с `RenderType.entityTranslucentEmissive(wormhole_pulse.png)` и полной яркостью.
- UV берётся по мировым координатам из текстуры 64×64, разбитой на 4×4 плитки, поэтому узор непрерывен по всему окну.
- **Альфа:**
  - стадии 0–2 — волна `sin(t·0.16 − y·0.65 − across·0.25)`, база 0.55 / 0.40 / 0.28;
  - на стадии 2 случайные «провалы»;
  - стадия 3 — стробоскоп: 2 тика вспышка, 5 тиков темно, со сдвигом по колонкам.
- Цвет — `COLOR.tint(DECAY)`, то же, что у модели окна.

### 6.3 Прочее
- **Тряска камеры:** `ShakePayload` задаёт силу и длительность, `ViewportEvent.ComputeCameraAngles` добавляет шум к yaw/pitch/roll. Амплитуда затухает квадратично.
- **Окно анализа:** `client/ScanPanel` (подробно в разделе 5.5 и в ГДД).
- **Анимация окна:** `textures/block/wormhole_center.png` — полоса 16×512 (32 кадра), `.mcmeta` с `frametime 2, interpolate`.

---

## 7. Сеть (`network/`)

Протокол `"4"` (`ModNetwork.PROTOCOL_VERSION`). Все пакеты идут от сервера к клиенту. Клиентская часть всегда выполняется через `context.enqueueWork`.

| Payload | Когда | Данные | Клиент |
|---|---|---|---|
| `AnonymityPayload` | логин, смена измерения | `enabled` | флаг скрытия нейм-тегов |
| `ShakePayload` | открытие, проход, предупреждение, имплозия | сила, длительность | тряска камеры |
| `PortalScanPayload` | старт дешифровки | сигнатура, класс, цвет, размеры, стадия, стабильность, тики, входы, длительность дешифровки | `ScanPanel.show` |
| `ScanLostPayload` | обрыв дешифровки | — | `ScanPanel.lost` |
| `TransitPayload` | перед проходом | цвет, сигнатура | `TransitScreen.begin` |

При добавлении или изменении пакета нужно повысить `PROTOCOL_VERSION`: клиент со старой версией тогда получит понятную ошибку при подключении.

---

## 8. Блок `WormholeBlock`: неочевидные места

- **Прочность** `-1 / 3600000`, без лута, `PushReaction.BLOCK`. Теги `wither_immune`, `dragon_immune`, `features_cannot_replace`, `create:non_movable` защищают от визеров, дракона, генерации мира и контрапций Create.
- **`getShape`:**
  - рамка — полный куб;
  - окно для обычного игрока пустое, чтобы прицел проходил сквозь него к соседним блокам;
  - творческому игроку (`EntityCollisionContext` → `Player.isCreative()`) окно показывает контур, чтобы его можно было сломать.
- **`getCollisionShape`:** рамка — полный куб, окно всегда пустое.
- **`getStateForPlacement`:** блок из инвентаря всегда ставится как рамка (`frame = true`), ось по взгляду. До 0.5 он ставился окном, которое нельзя было ни выбрать, ни сломать.
- **`animateTick`:** клиентские частицы и звуки по `decay` (гул, треск, дым, искры). Работает независимо от серверной спирали.
- **`lightLevel`:** 11/9/6/4 по `decay` у окна.

---

## 9. Ресурсы и данные

```
assets/eveportalnether/
├── blockstates/wormhole.json        multipart: frame / axis
├── models/block/wormhole_*.json     рамка и окно (translucent, tintindex 0)
├── models/item/*.json
├── textures/block/                  wormhole_frame, wormhole_center (+mcmeta), wormhole_pulse
├── textures/item/                   радар, детектор, осколок, незавершённые
├── textures/particle/               wormhole_spiral_0..3
├── textures/mob_effect/spatial_trace.png
├── particles/wormhole_spiral.json
├── sounds.json                      звуковые события мода → ванильные звуки
└── lang/en_us.json, ru_ru.json
data/
├── eveportalnether/recipe/          ванильные рецепты (без Create) и *_sequenced (с Create)
├── eveportalnether/damage_type/implosion.json
├── minecraft/tags/block/            защита блока портала
├── minecraft/tags/damage_type/      обход брони, зачарований, сопротивления и щита для имплозии
└── create/tags/block/non_movable.json
```

**Рецепты.**
- `portal_radar.json` и `portal_detector.json` имеют условие `neoforge:not → mod_loaded create`.
- `*_sequenced.json` — `create:sequenced_assembly` с условием `mod_loaded create`. Формат Create 6:
  - результаты `{"id": …}`;
  - шаги `create:deploying` с `[transitional, item]` и `create:pressing`;
  - обязателен `transitional_item`.
- Верстачных рецептов из деталей Create больше нет.

---

## 10. Команды (`/eveportal`, уровень прав 2)

| Команда | Действие |
|---|---|
| `spawn [unstable\|standard\|massive]` | пара там, куда смотрит игрок |
| `stage <fresh\|worn\|critical\|dying>` | перевести ближайший портал в стадию (подгоняет время и входы) |
| `skip` | сдвинуть ближайший портал на следующую стадию |
| `collapse` | схлопнуть ближайший портал |

---

## 11. Как расширять

- **Новый цвет:** добавить константу в `PortalColor` (буква, RGB), строки lang не нужны. Blockstate подхватит его автоматически: свойство `color` строится из enum.
- **Новый класс:** константа в `PortalTier` + `TierSpec` в `Config` + строки `tier.*` в lang.
- **Новый эффект:** метод в `PortalEffects`. Всегда проверять `isLoaded`, частицы отправлять через `level.sendParticles`.
- **Новый пакет:** record + `TYPE` + `STREAM_CODEC`, регистрация в `ModNetwork.register`, обработчик в `ClientPayloadHandler`, повысить `PROTOCOL_VERSION`.
- **Совместимость сохранений:** новые поля в `PortalPair` читать с значением по умолчанию, если ключа нет. Так сделано с `FormingTicks`, `Color` и `Code`.
