# Defying The Heavens

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

### Windows launcher

Double-click **Play Defying The Heavens.exe** to build and play this checkout.
Keep the EXE beside `gradlew.bat` and the entire `launcher` folder. Java downloads,
Gradle caches, and launcher logs stay in the ignored `.launcher` folder.

To rebuild the EXE, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\launcher\Build-Launcher.ps1
```

See [launcher/README.md](launcher/README.md) for setup and troubleshooting.

## Cultivation fruit

The Blockbench fruit is available as an edible item and a block hanging directly below leaves.
Right-click the attached fruit to harvest it into your inventory. A full inventory leaves it on the tree.
Breaking the fruit or removing its supporting leaves drops the fruit with its age preserved.

- Starts at 1 year, gains **10 years per Minecraft day** (one year per 2,400 server ticks), and caps at 10,000 years.
- Uses elapsed server game time, including time while its chunk is unloaded; it does not age while the server is stopped. Sleeping and `/time` changes do not accelerate the clock.
- Harvested fruit stores its age permanently, cannot be placed back, and can be eaten even at full hunger. Eating it
  sometimes (1 in 8) leaves a Peach Pit that grows a Spirit Peach Tree (see below).
- Eating grants up to **10 cultivation per year**. Existing suppression, stage caps, and tribulation rules still apply.
- The creative Food & Drinks tab has a one-year fruit.

### Wild fruit

Fruit generates on its own, hanging under the leaves of naturally generated trees in new chunks:
on average one per **64** tree-bearing chunks in the Overworld, and one per **24** in the Upper Realm.
Player-placed leaves never get fruit. Wild fruit starts aging from the moment it is generated.

| Starting age | Chance |
| --- | --- |
| 10–99 years (mostly around 40) | ~82% |
| 100–999 years | 15% |
| 1,000–4,999 years | 2.6% |
| 5,000–10,000 years | 0.4% |

Within each band the younger end is more common, so a 10,000-year fruit is the rarest of all.
Spawn rates are `FRUIT_CHUNKS_*` in `ModPlacedFeatures.java` (re-run `runDatagen` after changing them);
age chances are `NATURAL_*` in `FruitAge.java`.

### Age auras

An attached fruit shows its age (`FruitAura.java` for particles, sounds and timing, `CultivationFruitAuraRenderer.java`
for the glow). Each tier has its own colour and its own way of moving, and every fruit keeps its own time, so fruit
side by side never pulse in unison. The glow fades out as your camera gets right up to the fruit.

- **100+ years (jade):** a small glow that breathes slowly in and out; a few jade motes.
- **500+ years (aqua):** the same size, still like water, with a soft ripple spreading now and then and enchanting
  glyphs swirling in.
- **1,000+ years (white, blue tinge):** a steady, crisp glow that glints like ice catching the light; a frost-white
  ring of qi, a glyph swirl and the odd rising wisp.
- **5,000+ years (white-gold):** radiant. Short beams of light burst out like the Ender Dragon's death, golden and
  fruit-sized, each breathing on its own; counter-rotating golden rings and a two-armed glyph swirl.
- **10,000 years:** a heartbeat. On each slow "lub-dub" the halo swells, the beams surge, the pillar of light flares
  and a golden ripple leaves the fruit; every fourth beat it chimes on both notes. Rings of qi circle on two planes
  and light falls slowly around it.

Preview each tier with `/cultivationfruit attach 100`, `500`, `1000`, `5000` and `10000`.

With cheats/operator permissions, look at a leaves block with empty space underneath and run:

```text
/cultivationfruit attach 1
```

The command accepts any initial age from 1 to 10,000. To test eating an ancient fruit immediately:

```text
/cultivationfruit give 10000
```

Balance constants are in `FruitAge.java`. The fruit is a peach: cream-gold skin with a red blush on its sun side,
a cleft down the front, a stem and two leaves, about half a block across and hanging from the leaves above. The game
uses copies of the model and textures from `Models/CultivationPeach` (`models/block|item/cultivation_fruit.json`,
`textures/item/cultivation_fruit/`); the item model carries display settings sized for the hand and inventory. The
original small fruit is still in `Models/CultivationFruit`.

Run `gradlew.bat runGameTest` for server-side fruit integration tests, or `gradlew.bat build` for tests and a distributable JAR.
Run `gradlew.bat runDatagen` separately before a build when updating generated translations.

## Baubles

Rings are worn in the bauble panel. In the survival inventory, click the small ring button at the top left of the
player preview to open a panel on the left of the inventory with the bauble slots (two ring slots); click it again to
close it. Shift-clicking a ring puts it on even while the panel is closed. The panel closes while the recipe book is
open. In the creative inventory tab the ring slots show right of the armor, as before. Code: `client/BaublePanel.java`,
`client/mixin/InventoryScreenMixin.java`, `RingSlot.java`.

## Meditation mats

A **Meditation Mat** (a flat, round mat of coiled straw) and a **Red Silk Meditation Mat** (the same mat with a square of
red silk laid across it) can be placed on any block. Each is drawn one and a half blocks across, overhanging its block by
a quarter block on every side; only the middle block is solid, so leave a block between mats so they don't overlap. Right-click a mat to sit in its centre and start meditating: the same
checks, messages and cultivation gain as the meditation key. Right-click again, or move, to stop. Sitting on a mat speeds
meditation (see below).

- Meditation Mat: three wheat over three Blue Spirit Planks (three wheat alone would clash with bread).
- Red Silk Meditation Mat: a Meditation Mat, red carpet and a Jade (shapeless).

Source models are in `Models/MeditationMat` and `Models/RedMeditationMat` (Blockbench project, model JSON and textures);
the game uses copies under `assets/defying-the-heavens` (`meditation_mat*`, `red_meditation_mat*`).

## Ginseng

**Ginseng** and the rarer **Spirit Ginseng** grow wild on the ground and age like Cultivation Fruit: they start at a
natural age and gain 10 years per Minecraft day, up to 10,000 years, with the same age auras as fruit. They are
ingredients for alchemy (see below): a harvested root keeps its age, which carries into the pills brewed from it. A root can
also be set on a Spirit Pedestal, where it counts like a fruit of the same age.

- Right-click a plant to dig it up into your inventory (a full inventory leaves it in the ground). Breaking it, or the
  soil under it, drops it too. Harvested ginseng keeps its age, cannot be replanted and is not food.
- From its 10th year a plant is full grown: digging it up gives 1-3 **Ginseng Seeds** (or Spirit Ginseng Seeds) and
  sometimes extra roots (a second root 25% of the time, a third 10%). Younger plants give one root and no seeds.
- Seeds planted in soil grow a 1-year seedling, a young plant from year 4 and the full bush from year 10
  (`YOUNG_YEARS`, `MATURE_YEARS`, `EXTRA_ROOT_CHANCE`, `THIRD_ROOT_CHANCE` in `GinsengBlock.java`).
- A cultivator with a Golden Core (Core Formation or higher) can sneak and right-click a plant with an empty hand to
  pour qi into it, ageing it at the same rising cost as a Spirit Peach Tree (see `QiFeeding.java`).
- Ginseng grows only on soil (grass, dirt, podzol, moss...). Wild plants appear on grass or podzol, never in caves.
- Spawning, in small patches (Ginseng 2-4 plants, Spirit Ginseng 1-3, each with its own age), on average one patch per
  this many chunks: Ginseng 80 in Overworld forests, taiga and jungles and 32 in the
  Upper Realm; Spirit Ginseng 640 and 160. These are `GINSENG_CHUNKS_*` and `SPIRIT_GINSENG_CHUNKS_*` in
  `ModPlacedFeatures.java` (and the matching `placed_feature/*ginseng*.json`; re-run `runDatagen` after changing them).
- The plants are drawn like vanilla's sweet berry bush, whose leaves they reuse: Ginseng in a warm green with one cluster
  of red berries, Spirit Ginseng in pale jade with golden berries; seedlings and young plants use the bush's earlier
  stages. The harvested items are the forked roots; the seeds are red (Spirit: gold).
- Code: `GinsengBlock`, `GinsengBlockEntity`, `GinsengItem`, `GinsengSeedsItem`, `GinsengFeature`,
  `client/GinsengAuraRenderer.java`.

With cheats/operator permissions:

```text
/ginseng plant 1000          plants 1,000-year Ginseng on the soil you look at
/ginseng plant 10000 spirit  the same, Spirit Ginseng
/ginseng give 500 [spirit]   a harvested root
```

## Spirit Peach Trees

One Cultivation Fruit in 8 leaves its **Peach Pit** when eaten (`PIT_CHANCE` in `CultivationFruitItem.java`). Plant the pit in soil and it grows into a **Spirit Peach Sapling**,
which grows (in light over time, or faster with bone meal, like any sapling) into a small **Spirit Peach Tree**: a
cherry-wood trunk with green leaves and peach blossoms, about 6 blocks tall.

- The tree's base is its **heart** (it looks like cherry wood). The heart keeps the tree's age, which starts at 1 year
  and grows 10 years a day like a fruit, and grows fruit under the tree's leaves: up to 3 at a time, one about every
  5 minutes while there is room. New fruit start at 1 year and age normally, so a tree's fruit is never older than the tree.
- **Pouring qi into the tree:** sneak and right-click the heart with an empty hand. Only a cultivator with a Golden Core
  (Core Formation or higher) can do it. It spends as much of your qi as buys whole years, and ages the tree and every
  fruit on it by that many years. Each year costs more the older the tree is: about 20 qi for a young tree, 180 at 750
  years, 800 at 5,000 years and 1,300 at 9,000. As a guide, taking a tree from nothing to 1,000 years takes about 4 hours
  of feeding at Core Formation, 1 hour at Nascent Soul and 25 minutes at Heavenly Being; 10,000 years takes about 9 hours
  at Four Axis (natural ageing: 33 and 333 hours). The cost curve is `COST_*` in `SpiritPeachHeartBlockEntity.java`.
- Breaking the heart gives an ordinary cherry log, and the tree no longer bears fruit. Spirit Peach leaves drop only to shears.
- Code: `SpiritPeachTree` (the shape), `SpiritPeachSaplingBlock`, `SpiritPeachHeartBlock(Entity)`, `PeachPitItem`.
  Textures: `block/spirit_peach_leaves.png`, `block/spirit_peach_sapling.png`, `item/peach_pit.png`; the wood is vanilla
  cherry. Run `gradlew.bat runDatagen` once so the heart is cut quickly with an axe.

## Spirit Pedestals and meditation boosts

A **Spirit Pedestal** (carved grey stone with jade inlays, about 1.25 blocks tall: a base with a jade
meander band, a column with corner pilasters and a panel bearing a jade medallion, and a flared top with jade nubs, a
row of jade beads and a polished jade disc under the fruit) displays one Cultivation Fruit or ginseng. Right-click it with one to set it
down, and right-click again to take it back. Breaking the pedestal gives back both the pedestal and its fruit, in
creative too. The fruit floats over the
pedestal, turning slowly, and shows the same age aura it had on the tree. Harvested fruit doesn't age, so a fruit's
age stays the same on a pedestal.

Several things speed up meditation. Their shares add together, and the total multiplies the normal rate:

| Source | Boost |
| --- | --- |
| Sitting on a Meditation Mat / Red Silk Meditation Mat | +10% / +20% |
| Each fruit on a pedestal within 4 blocks (2 up or down); only the 4 oldest count | 1 year +2%, 10 years +14%, 100 years +26%, 1,000 years +38%, 10,000 years +50% (+12% for every tenfold age) |
| Height, under open sky: from y 120 up to y 250 | up to +15% |
| Tranquil places: cherry grove, Peach Blossom Sanctuary, snowy slopes, frozen and jagged peaks | +10% |

The best setup today, a red mat and four 10,000-year fruit on a snowy peak, gathers about 3.45 times as fast. When you
sit down, and whenever the boost changes, the action bar shows the total and where it comes from. The rate in the
cultivation menu includes the boost and turns jade when it is boosted. A fruit's tooltip says what it would add on a
pedestal.

While someone meditates, each feeding fruit's qi arcs up off its pedestal and sweeps into its own orbit around them,
in the fruit's aura colour, with beads of light and glyphs flowing along it. The orbits sit at different heights and
widths, lean slightly and turn opposite ways like the rings of an armillary sphere, and stay below eye level and an
arm's length away. Fruit 1,000 years and older also lift wisps of light off their orbit.

- Recipe: polished deepslate in the top and bottom rows, with a Jade in the middle.
- Balance numbers are in `CultivationBoost.java`. Pedestal code is in `SpiritPedestalBlock(Entity).java`, and the
  visuals are in `client/SpiritPedestalRenderer.java` and `client/MeditationFormation.java`.
- Model: `models/block/spirit_pedestal.json`; textures `textures/block/spirit_pedestal_{column,trim,top}.png` (32x32).

### Under Qi Sense

Spirit treasures are vessels of qi, so in the grey plane of Qi they keep their colour:

- The auras of Cultivation Fruit, ginseng and whatever a pedestal holds, and a pedestal's streams and orbits around a
  meditator, are drawn on the Qi Sense layer instead of into the world, so they stay vivid while everything else is grey.
- Each one breathes out wood-qi motes that rise off it, more of them the older it is, with white-gold metal qi mixed in
  from 5,000 years.
- A pedestal feeding a meditator sends its qi into them as motes (in place of the grey particles).
- Code: `client/QiSenseTreasures.java` (and `QiSenseClientHandler#emit`, `SenseOverlay`).

## The mortal path and alchemy

Every new player starts as a **mortal**: no cultivation, no qi and no abilities, and meditation does nothing. First the
body is tempered through four stages, **Mortal Low, Mid, High and Peak**, by fighting, mining, sprinting and eating:

| Tempering from | Points |
|---|---|
| A fully charged melee hit / killing a hostile mob / killing anything else | 0.5 / 4 / 1 |
| Mining stone or deepslate / an ore | 0.2 / 3 |
| Sprinting | 0.1 per second |
| Food | 0.5 per food point (bread 2.5, steak 4) |
| Spirit Dew / a raw herb (right-click to eat) | 12 / 10 to 20 by age, doubled for rare herbs |

Low to Mid takes 120, Mid to High 200, High to Peak 300. Each stage grows the body toward Qi Refining Early's bonuses
(a third at Mid, two thirds at High, all of it at Peak), so awakening never weakens you. Only at **Mortal Peak** can a
**Marrow Cleansing Elixir** open the meridians and start the player at Qi Refining Early. Players who were already
cultivating in an existing world stay cultivators. Code: `MortalStage.java`, `Tempering.java` (all the amounts).

Pills are brewed in an **Alchemy Cauldron** (shapeless: a cauldron, two copper ingots and a gold ingot). Fill it with a
water bucket, put a fire under it (fire, soul fire, a lit campfire, lava or a magma block), then right-click the
ingredients in one at a time. Anything that doesn't fit a recipe with what's already in is refused, so nothing is lost.
When the ingredients make a recipe the water turns jade and the brew begins; it pauses while the fire is out. When it's
done, the pill pops out and the water is used up. Right-click with an empty hand to see what's inside (sneak to tip the
ingredients back out); an empty bucket takes the water back.

| Pill | Ingredients | Brew | Effect |
|---|---|---|---|
| Marrow Cleansing Elixir | Ginseng, Honey Bottle, 2 Bone Meal | 20 s | Mortal to Qi Refining Early |
| Qi Gathering Pill | Ginseng, Spirit Dew, Glowstone Dust | 20 s | More qi gathered per second for 5 minutes |
| Cultivation Pill | Ginseng, Huangjing, Glistering Melon Slice | 30 s | A share of the current stage's cultivation |
| Foundation Pill | Ginseng, Lingzhi, Spirit Ginseng, Jade | 45 s | Needed to break into Foundation Building |
| Core Pill | Spirit Ginseng, Spirit Lotus, Cultivation Fruit, Blaze Powder | 60 s | Needed to break into Core Formation |

- **Grade.** Each pill comes out Low, Mid, High, Supreme or Immortal grade (white, green, aqua, purple, gold). The odds
  depend on the average age of the aged ingredients (the herbs, Cultivation Fruit): fresh ones give mostly Low; Immortal
  is only possible from 1,000 years and still rare (the table is `ODDS` in `PillGrade.java`).
- **Potency** = grade multiplier (1, 1.5, 2, 3, 5) x age factor (1 for fresh ingredients up to 2 at 10,000 years), so 1 to
  10. It scales every effect: Qi Gathering Pill +25% qi gathering per point; Cultivation Pill 15% of a stage per point;
  breakthrough pills start the new realm 5% in per point above 1 (up to 50%). The numbers are constants at the top of
  `PillItem.java`.
- **Breakthrough pills.** At Qi Refining Grand Perfection the breakthrough waits for a Foundation Pill, and at Foundation
  Building Grand Perfection for a Core Pill. Eating it opens the way; the tribulation still has to be survived, and dying
  in it spends the pill. Later realms need no pill.
- **Pill resistance.** Each Cultivation Pill makes the next one 25% weaker (down to 10%); it wears off at one pill's worth
  every ten minutes. A pill that would do nothing right now (a cultivation pill at a bottleneck, a breakthrough pill at the
  wrong stage, anything but the elixir for a mortal) isn't eaten.
- **Rare herbs.** Purple Lingzhi and Ochre Huangjing stand in for Lingzhi and Huangjing in any recipe and count as twice
  their age, so they push the grade up.
- Recipes are in `AlchemyRecipes.java`; the cauldron is `AlchemyCauldronBlock(Entity).java`; the Qi Gathering effect is
  `ModEffects.java`. Pill tooltips list their ingredients, and the cultivation menu names the pill a breakthrough needs.

```
/cultivation mortal                                   back to Mortal Low
/cultivation addtempering <amount>                    temper a mortal's body (500+ reaches Peak from Low)
/give @s defying-the-heavens:core_pill{PillGrade:4,PillAge:10000}   an Immortal-grade pill (grades 0-4)
```

## Refining medicine and qi surges

- **Unrefined qi.** Cultivation Fruit and Cultivation Pills no longer give cultivation at once: their qi goes into an
  unrefined pool (a pale stretch past the menu's cultivation bar). Meditating refines it at 2x the meditation rate, on top
  of the meditation itself (`REFINE_RATE`). Nothing is refined at a bottleneck, so nothing is lost there either.
- **Medicinal toxicity.** Every fruit or Cultivation Pill makes the next one 25% weaker (down to 10%), fading by one dose
  every 10 minutes. Shown in the menu and on pill and fruit tooltips.
- **Qi surges.** Nothing sits on screen while you meditate. About a minute in, and every 50-80 seconds after, qi surges:
  a chime, and a short minigame appears round the crosshair (or, in the Inner Realm, on the island). Play it well for
  **Qi Harmony**: meditation (and refining) x1.5, x2 after two in a row, for 2 minutes. Fail or ignore it and it just passes.
  All screen games use **R** (Circulate Qi); three wrong taps in the Circuit or the Elements is a **qi deviation** (a
  quarter of your qi, Nausea, a little damage, and the meditation ends).
  - **Small Heavenly Circuit:** a bead of qi circles a ring of acupoints; tap as it meets each lit point, all the way round.
    More points and a faster bead at higher realms (6 points / 4 s at Qi Refining, up to 10 / 2 s).
  - **Five Elements:** the cycle (wood, fire, earth, metal, water) rings the crosshair; elements flash in the centre. Tap
    when the element the current one generates appears, five times round. Faster flashes at higher realms.
  - **Breath rhythm:** a ring swells and shrinks; hold R as it swells, let go as it shrinks, three breaths, 70% in rhythm.
  - Code: `QiSurges.java` (timing, the Inner Realm games, `FIRST_SURGE`, `INTERVAL_*`), `client/QiSurgeHud.java` (the
    screen games), `MeditationManager#harmony` / `#onCirculation` (`CIRCULATION_BONUS`, `CIRCULATION_LASTS`, `DEVIATION_*`).

## The Inner Realm

After **10 seconds of meditation** the screen fades to black and your soul turns inward: you open your eyes on your own
island in a void, while your body stays sitting where you were (other players see it, in your skin). Meditation inside is
**x1.5** as fruitful, and still gets the mats, pedestals, height and biome bonuses from around your body; suppression and
the Upper Realm's x10 also follow the body.

- **Walk freely.** The island is yours to walk; walking off the edge sets you back on the seat. No digging or building.
- **Leaving:** press the meditate key (**H**) to stop meditating; the eyes open back in your body.
- **Your body:** anything that hits it pulls you back to take the blow; being pushed or moved does too. Hostile mobs near
  it notice it, and the chunks around it stay loaded while you're away. Logging out inside puts you back at your body.
- It's an ability (on by default) that can be switched off on the Abilities tab. No tribulations from inside.

**Soul Crystal.** The island is frosted crystal tiles, see-through so the leylines and the core show beneath. A tile lights
up under your feet and fades as you move on. Its colour follows your cultivation: smoky slate at Qi Refining, through
blue, to pale white-gold at Four Axis. Unbreakable, no item. Code: `SoulCrystalBlock.java`, `client/SoulCrystalColours.java`;
textures `block/soul_crystal(_lit).png`.

**Surges on the island** (with the screen games too, see above):

- **Qi wisps:** six wisps of stray qi drift onto the island one after another; walk into them before they fade. Each is
  worth 6 seconds of meditation; gather half for Qi Harmony.
- **Leyline sequence:** tiles light gold one by one (3 at Qi Refining, up to 6); then walk onto them in the same order
  within 20 seconds. Other tiles don't count. Worth 30 seconds of meditation and Qi Harmony.
- **Heart demon** (Nascent Soul and up, at most once every 5 minutes): your own shadow rises at the island's edge and
  attacks, with 80% of your max health and blows of 10% of it. Slay it for 2 minutes of meditation and Qi Harmony. If it
  would kill you, it wins instead: you're thrown back to your body with 30% of your qi gone and Nausea. It fades after a
  minute. Fights there never break the meditation. Code: `HeartDemonEntity.java`, `client/HeartDemonRenderer.java`.

The sky is your cultivation made visible (`InnerRealmSkyRenderer.java`), drawn in place around your island:

- **Leylines** of qi 26 blocks below the island, with qi pulsing inward along them. Every stage grows more lines and
  branches, and each player's web is their own (seeded by their UUID), so it grows rather than changes.
- **Qi Refining**: almost black, a few faint threads. **Foundation Building**: a web, the first stars.
- **Core Formation**: a solid, faceted golden core beneath the island, larger each stage; gold shards orbit and fuse into
  it (8, 5, 3, then none) and qi motes spiral in.
- **Nascent Soul**: the core becomes you, in your own skin, see-through and meditating, wrapped in black mist that
  thins each stage and is gone at Grand Perfection. Nebula colours and a faint aurora appear.
- **Heavenly Being**: brighter aurora, pillars of light rising from the lines. **Four Axis**: a celestial sky, and the
  lines rise up and meet far overhead.
- The island grows with the realm (radius 3 to 8).

Code: `InnerRealm.java` (entering, leaving, the island, `ENTER_AFTER_TICKS`, `FADE_TICKS`, `CULTIVATION_BONUS`),
`InnerBodyEntity.java`, `client/InnerRealmFade.java` (and `client/mixin/ReceivingLevelScreenMixin.java`),
`client/InnerRealmSkyRenderer.java`, `client/InnerBodyRenderer.java`, `client/LotusPose.java`. The dimension is
`inner_realm` (each player's island is 4096 blocks apart along X at Y 128).

## More herbs

Four more spirit herbs, for alchemy. The first three age exactly like ginseng (10 years per day up to 10,000, the same age
auras, Golden Core qi feeding, dug up into items that keep their age, seeds from a full-grown plant, can sit on a Spirit
Pedestal). Code: `GinsengBlock` (shared), `SpiritLotusBlock`, `SpiritDewGrassBlock`, `SpiritDewItem`, `GinsengFeature`
(wild placement, on soil or water).

| Herb | Grows | Rare variant | Seeds |
|---|---|---|---|
| **Lingzhi** (lacquered red shelf fungus) | Soil, in dark and old-growth forests | Purple Lingzhi | Lingzhi Spores |
| **Huangjing** (Solomon's seal; the item is its yellow root) | Soil, in forests, taiga and jungle | Ochre Huangjing | Huangjing Seeds |
| **Spirit Lotus** (golden, five-coloured petal tips) | Still water, in swamps and rivers | none | Spirit Lotus Seeds (place on water) |
| **Spirit Dew Grass** | Soil, in meadows, flower forests, cherry groves | none | shears take the grass |

- Wild herbs grow in small patches, each plant with its own age: Lingzhi, Huangjing and Spirit Lotus 2-4 plants, their
  rare variants 1-3, Spirit Dew Grass 3-6 (the sizes are in `ModFeatures.java`).
- All of them grow more often everywhere in the Upper Realm. Spawn rates (one patch per this many chunks) are the
  `*_CHUNKS_*` constants in `ModPlacedFeatures.java`: Lingzhi and Huangjing 100 / 40 (Upper Realm), their rare variants
  1000 / 250, Spirit Lotus 160 / 64, Spirit Dew Grass 40 / 16. If you change them, also change the matching
  `placed_feature/*.json` (or re-run `runDatagen`).
- The Spirit Lotus is a lily pad as a seedling, a bud from year 4 and in flower from year 10.
- Spirit Dew Grass gathers a bead of **Spirit Dew** every few minutes (at once in the Upper Realm). Right-click to collect
  it; the grass stays. Drinking Spirit Dew refills a quarter of your qi; it is also an ingredient.

## Jade Ore

Jade Ore and Deepslate Jade Ore drop **Jade** (more with Fortune, the ore itself with Silk Touch) and a little experience,
like emerald ore, and need an iron pickaxe. Jade Ore spawns in Overworld mountain biomes from Y -48 to 160 (deepslate
jade below Y 0) and through all the Upper Realm's islands, including the Dense Qi Peaks' jade stone, at about the same
rarity as diamonds (`JADE_VEINS_*` in `ModPlacedFeatures.java`). Jade goes into the Foundation Pill, the Spirit
Pedestal and the Red Silk Meditation Mat.

The Dense Qi Peaks themselves are mostly plain stone, with broad patches and veins of Jade Stone running through it
(the patches are a surface rule in `ModNoiseSettings.java`, the veins `ORE_JADE_STONE` / `JADE_STONE_BLOBS`).

## Blue Spirit Wood

Spruce-dark logs with blue qi flowing up their veins (an animated texture); the planks show faint blue grain. Log, wood
and planks work like vanilla wood.

It grows wild as the **Blue Spirit Tree**, a spruce (vanilla spruce shape and leaves) with a Blue Spirit Log trunk, in every
kind of taiga (plain, snowy, old-growth) and anywhere in the Upper Realm: one attempt per **2** chunks in each, which in a
generated taiga works out to about one Blue Spirit Tree per 4 forested chunks, or one tree in 16
(`BLUE_SPIRIT_TREE_CHUNKS_*` in `ModPlacedFeatures.java`, `blue_spirit_tree` in `ModConfiguredFeatures.java`). There is
no sapling: its leaves drop ordinary spruce saplings, so every Blue Spirit Log comes from a tree you find. In the
Overworld it takes root on the ground beneath the taiga's canopy and grows up through it.

## Sects and cultivators

### Sect grounds

**Sects** are walled compounds laid out along one axis like a Chinese temple: a gate between two watchtowers, a front
court with lanterns and trees, the **Formation plaza** with the Formation Core at the centre, a **Meditation Hall** of
mats on one side and the **Treasury** on the other, the **main hall** at the back with the Sect Master's dais, herb
gardens (ginseng, Spirit Ginseng, fruit trees) beside it, a **pagoda**, and a row of **dwellings** down each side, one for
every member. Every building is grey brick and dark oak under tiled roofs with upturned eaves, red pillars and gold trim.

- Three sizes, for 10-12, 13-16 and 17-20 members. They only generate on fairly level, mostly dry land (at most 20
  blocks between the highest and lowest ground under the compound, 24 in the Upper Realm; the terrace levels the rest and
  fills in a pond or a stream), never in the Nether or the End:
  - **Overworld** (`defying-the-heavens:sect`): plains, meadows, forests, taiga, snowy plains, savanna, cherry groves,
    bamboo and sparse jungle. One attempt per 32x32 chunks, at least 16 chunks apart, never within 6 chunks of a village
    (`worldgen/structure_set/sects.json`).
  - **Upper Realm** (`defying-the-heavens:sect_upper_realm`): islands of Spirit Forest, Peach Blossom Sanctuary, Dense Qi
    Peaks and Thunder Peaks wide enough to hold one. One attempt per 14x14 chunks, since few islands are
    (`worldgen/structure_set/sects_upper_realm.json`). Their masters are Heavenly Beings or Four Axis cultivators.
  - Tuning: `MAX_RELIEF`, `MAX_RELIEF_UPPER` and `MAX_WET` in `SectStructure.java`, `spacing` in the structure sets.
- Worldgen that runs after structures can't spoil the grounds: no lake (vanilla lava lakes, the Upper Realm's spring
  basins) forms where it would reach into a sect (`mixin/LakeFeatureMixin`), and the sects build with nothing a water
  or lava spring can open in (the plaster is smooth quartz, not calcite; checked by a game test).
- The layout is planned in code (`SectStructure`, `SectPiece`), not from jigsaw templates, so it keeps its symmetry, fits
  its population, and each building adapts its height to the ground.
- The Treasury holds Spirit Pedestals bearing old Cultivation Fruit and ginseng and Spirit Ginseng, and loot chests;
  elders' dwellings and the library have their own (`loot_tables/chests/sect_*.json`). Taking from a sect is theft: it
  brings the sect down on you.
- Players can use the mats in the Meditation Hall.

### Protective formations

A **Formation Core** (an altar of ritual bronze, lacquer and jade with an azure crystal hovering over it) raises a
spherical **barrier** around itself. The barrier is invisible like a vanilla barrier, but lets the core's owner (or its
sect) walk through, and can be broken only by a cultivator at least as strong as the formation: at an equal rank a block
takes 5 seconds, half that for every rank stronger (`Formations.breakTicks`). Breaking the core, or the core running out
of qi, takes the whole barrier down at once.

- **Power.** The core holds a battery of 2,000 qi. The barrier costs `0.5 + radius² / 80` qi per second (1.3 at radius 8,
  13 at 32, 52 at 64); each **Qi Vein** joined to the core gathers 8 qi per second. A raised barrier stays up while the
  veins and battery can carry it. Each barrier block broken drains 40 qi and each one mended costs 5, so a sustained
  assault can empty the battery and shatter the barrier, which then can't rise for 2 minutes.
- **Qi Veins** are an ore about as rare as diamond (1-4 to a vein; many more in the Upper Realm), mined with a diamond
  pickaxe. With Qi Sense you can see them breathing qi.
- **Seals.** The **Inscription Brush** paints seals of qi ink on floors and walls (sneak to wipe one away). Seals that
  touch, like redstone dust, form one line; a line from the core to a vein, within 64 blocks of the core, carries its
  qi. The ink is invisible without Qi Sense, which shows it as glowing azure lines.
- **The core's screen** (right-click your own core): switch the barrier on or off and set its radius (4-64), and see the
  battery, upkeep and supply. The core is as strong as whoever placed it. Sect cores answer only to their sect; a sect's
  barrier is as strong as its master.
- Recipes: Formation Core (gold ingot, amethyst shard, gold ingot / lapis block, diamond, lapis block / 3 polished
  deepslate); Inscription Brush (string top right, lapis in the middle, bamboo bottom left).
- Code: `Formation`, `Formations`, `FormationData`, `FormationCoreBlock(Entity)`, `SectBarrierBlock`, `SealBlock(Entity)`,
  `InscriptionBrushItem`, `QiVeinBlock`, `QiVeinFeature`; client `FormationCoreScreen`, `FormationCoreRenderer`,
  `SealRenderer`, `ClientFormations` (under Qi Sense a barrier shows as a continuous purple membrane, brighter at its
  edges, with soft light drifting over it, and tints everything strongly purple while you stand inside it; a
  Concealment Barrier looks the same in green).

### Sect members

A sect is founded with 10-20 members the first time its core ticks (`SectManager.found`). Titles go by strength and are
recalculated whenever someone dies, leaves or breaks through: the strongest is **Sect Master**, then about 10% **Grand
Elders** (from 6 members), 20% **Elders**, 30% **Inner Disciples**, and the rest **Outer Disciples**. In the lower realms
the master is between Core Formation and Nascent Soul Grand Perfection; in the Upper Realm, a Heavenly Being or Four Axis.

| Title | Duty |
| --- | --- |
| Outer Disciple | Patrols the grounds and up to 30 blocks outside the barrier |
| Inner Disciple | Meditates in the hall, trains in the courtyards |
| Elder, Grand Elder | Guards the Treasury and the Formation Core; fights only when disciples nearby are in a fight or someone breaks into the Treasury or at the core |
| Sect Master | Meditates on the dais; fights only when the barrier is shattered or an elder is killed |

- Members below Heavenly Being stay within the barrier plus 30 blocks, leaving only to chase someone.
- A member killed by a player is never replaced, and the sect is smaller for good. One who dies otherwise, or leaves, is
  replaced after 2-4 days by a new Outer Disciple at Qi Refining Early. When the last member dies, the sect is extinct
  and never comes back.
- Cultivators cultivate ten times slower than players, and break through without tribulations (they never pass Heavenly
  Being Grand Perfection in the lower realms).
- **Leaving the sect.** A lower-realm member who reaches Heavenly Being leaves: flies 200-1,000 blocks away, finds a cave
  or a mountainside, sets down a mat and goes into seclusion behind a **Concealment Barrier** that hides their presence
  from Qi Sense and pushes back (and slows) anyone who comes close. They fight only if attacked or the barrier is struck.
  At Heavenly Being Grand Perfection they set out for the rift at 0, 0 and try to ascend to the Upper Realm (7 in 10
  succeed). Journeys through unloaded land go on off-screen (`NpcTravel`).
- **Rogue cultivators** turn up now and then 40-80 blocks from a player, out of sight. Each attempt (every 30 seconds,
  35% of the time) rolls a rank: a mortal 10% of the time, falling evenly with every stage to 0.1% for Heavenly Being Grand
  Perfection; in the Upper Realm the curve runs from Qi Refining Early to Four Axis Grand Perfection
  (`NpcSpawner`). At most 3 near any one player and 40 in a level.
- **In a fight** cultivators use what their realm gives them: a Consciousness Domain (rogues always, sect members only
  in combat), Realm Suppression and Qi Flight, but only while their qi can sustain them. They flee below a quarter of their
  health or from a domain four or more stages stronger.
- Every cultivator has a nameplate: title, a red health bar with numbers, and alignment, for example "Righteous (+120)".
  Under Qi Sense, meditating cultivators draw qi motes into themselves.

### Alignment

Alignment runs from -200 to +200: **Demonic** below -50 (red on black), **Neutral** from -50 to 50 (blue), **Righteous**
above 50 (white). A sect shares one alignment (40% righteous, 30% neutral, 30% demonic); rogues are random.

- Neutral cultivators are peaceful unless provoked. Righteous and demonic cultivators attack each other on sight, and a
  sect treats players the same way by alignment.
- Killing a cultivator who hadn't provoked you costs 10 alignment, plus 2 for each realm of theirs; killing a demonic one
  gains 6, plus 2 per realm (`Alignment.killShift`).

### Cultivator clothing

Robes, guan (hair crowns), trousers and boots, worn in place of armour, in five grades: **Mortal, Spirit, Earth, Heaven,
Immortal**. Protection matches vanilla armour grade for grade (leather, iron, diamond, netherite, netherite) because the
tribulations are balanced against vanilla gear. What sets clothing apart is what it does for cultivation:

| Grade | Qi gathering / piece | Meditation / piece | Boots speed |
| --- | --- | --- | --- |
| Mortal | +3% | +2% | +3% |
| Spirit | +5% | +4% | +5% |
| Earth | +8% | +6% | +7% |
| Heaven | +11% | +9% | +9% |
| Immortal | +15% | +12% | +12% |

- Crafted from wool and string at Mortal grade. **Upgrade** a piece one grade in the crafting grid, keeping its colours,
  enchantments, name and wear: Mortal to Spirit 2 Spirit Dew + a gold ingot; Spirit to Earth a Qi Vein + a diamond;
  Earth to Heaven 2 Qi Veins + netherite scrap; Heaven to Immortal 2 Qi Veins + a netherite ingot + Spirit Ginseng.
- **Dye** a piece: white dye for righteous white and blue-grey, blue or light blue for neutral blue, red and black dye
  together for demonic black and crimson.
- Cultivators wear it too, better grades for higher titles, and drop each piece 12% of the time; sect chests hold it.
- Code: `ClothingTier`, `ClothingItem`, `ClothingStyle`, `ClothingRecipes`; client `ClothingModels` (the robe's skirt,
  sleeves and the guan's crown), `ClothingArmorRenderer`.

### Commands (op)

```
/locate structure defying-the-heavens:sect        the nearest sect (sect_upper_realm in the Upper Realm)
/place structure defying-the-heavens:sect         a sect where you stand (on level enough ground)
/sect info                                        the nearest sect: path, barrier, members with titles and realms
/cultivator spawn <realm 0-6> <stage 1-4> <alignment>   a rogue where you stand (realm 0: a mortal)
/alignment get [player]
/alignment set <player> <value>
```

`/place` builds into land that is already generated, so it knocks loose some grass and flowers there (vanilla
structures do the same); sects that generate with the world don't.

Sects and cultivators' off-screen journeys are saved in the Overworld's `data/defying-the-heavens_sects.dat` and
`defying-the-heavens_npc_travel.dat`; formations in each dimension's `defying-the-heavens_formations.dat`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
