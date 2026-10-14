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

## Creative tab

Everything the mod adds has its own creative tab, **Defying The Heavens** (`ModCreativeTab`, shown with a Cultivation
Fruit; with vanilla's tabs full it sits on the second page, behind the arrows at the top). In order: what cultivation is
done with (meditation mats, Spirit Pedestal, Heaven Altar, Alchemy Cauldron, Formation Core, Inscription Brush, the
rings), what is taken (a one-year Cultivation Fruit, every pill at every grade from Low to Immortal, Spirit Dew), the
herbs (a year old) and their seeds, spores and pit, spirit stones of every grade and jade, the world's blocks (jade
stone, ores, qi veins, Blue Spirit Wood, leaves), the clothing (every path's colours, grade and piece), the
Netherworld's things (Yang Fire Talisman, Ghost-Quelling Talisman, Peach Wood Sword, Red Spider Lily, the Other-Shore Flower and Netherworld Lotus
a year old with their bulbs and seeds, Yin Jade with its ore and block, the Yang Preservation and Yin Gathering Pills at
every grade, a Ghost Jade Slip of each method), and the spawn eggs: Cultivator, Ghost Cultivator and Hungry Ghost. An
item added later that the list doesn't name still shows, at the end. The items stay in vanilla's tabs too. The mod's
other creatures (tribulation clouds, heart demons, the bodies left behind by the Inner Realm and Soul Projection) only
arise from what makes them, so they have no eggs.

## Recipe book

Every crafting recipe the mod adds has its own tab in the recipe book, last among the crafting table's tabs (and the
inventory's), shown with a Cultivation Fruit: meditation mats, the Spirit Pedestal, Heaven Altar, Alchemy Cauldron and
Formation Core, the Inscription Brush and the talismans painted with it, spirit stones up and down the grades, Blue
Spirit Wood, Yin Jade, the Peach Wood Sword, the Mortal-grade clothing. The search tab finds them too. You don't have to
come by an ingredient first: every player knows all of the mod's recipes from the moment they join (a recipe added in a
later version is learned at the next join), so the first join shows the "New Recipes Unlocked!" toast once.

Not in the book: dyeing and upgrading clothing (they work on any piece, keeping its colours, enchantments and wear, so
like vanilla's own fireworks and banner recipes the book can't show them; see Cultivator clothing), and the Alchemy
Cauldron's brews, which aren't crafting-table recipes (see The mortal path and alchemy).

Code: `ModRecipeBook` (teaching the recipes on join), client `CultivationRecipeTab`, client mixins
`RecipeBookCategoriesMixin` (vanilla's tabs are a fixed enum: the tab is added to it as it loads, and to the crafting
book's tabs and search) and `ClientRecipeBookMixin` (the mod's crafting recipes are sorted into it); tests in
`NetherworldInhabitantsGameTests` (`everyRecipeOfTheModIsInTheRecipeBook`). A new crafting recipe under
`data/defying-the-heavens/recipes/` goes in the tab by itself.

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
**Marrow Cleansing Elixir** open the meridians and start the player at Qi Refining Early (a dusky steam of impurities
leaves the body, then the first light of qi). The ghost path waits on it too: a mortal, alive or dead, takes up and
cultivates no ghost method (see **The two paths**). Players who were already
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

## The Ritual to the Heavens

Leaving **Qi Refining Mid** takes the heavens' **permission to cultivate**, not a tribulation: at Mid the cultivation bar stops full (pills,
fruit and meditation add nothing more) until the **Ritual to the Heavens** is done. Every path performs the same ritual,
and sect cultivators perform it too (see Everyday life). Anyone already past Qi Refining Mid already has it, and an op's
`/cultivation set` (or `mortal`) back to Qi Refining Mid or below takes it away again, Heaven's Favour with it, so the ritual
is asked once more.

1. Build a **Heaven Altar** (smooth stone, jade, a Low spirit stone and copper) under open sky, and build round it.
2. Right-click it to open its screen: nine **offering** slots in a ring (anything at all may be laid there) round a dial
   showing the **chance**, which moves as you add or take items; beside it the offering's tier and worth (and what the
   next tier needs), the altar's grandeur and its points by part, the height bonus, the reward (permission to cultivate, and
   any Heaven's Favour), and whether you may begin now (`HeavenAltarMenu`, `HeavenAltarScreen`).
3. **Sneak and right-click it with empty hands** to begin. The offerings rise as smoke and are taken, whatever happens.
4. **Kneel still for fifteen seconds.** Turning your head is fine; moving a third of a block (being knocked back too),
   leaving the game, dying or losing the altar breaks the ritual, and the offering is gone.
5. The heavens answer: **permission to cultivate** (and a full Mid steps straight into Late), or **silence**, and no new ritual until
   the next dawn.

**Offerings** are valued as cultivators value things (`TalkSystem.worth`, in Low spirit stones: a Mid stone is 9, a High
81, a Supreme 729, an Immortal 6,561; herbs, pills and fruit by their age and grade; ordinary items nothing):

| Offering | Worth | Reward | Base chance |
|---|---|---|---|
| Unworthy | under 9 | counts as no offering | 0.5% (2% at most) |
| Proper | 9 - 80 | permission to cultivate | 40% |
| Generous | 81 - 728 | permission + Heaven's Favour I | 55% |
| Lavish | 729 - 6,560 | permission + Heaven's Favour II | 70% |
| Heavenly | 6,561+ | permission + Heaven's Favour III | 80% |

**Heaven's Favour** raises every cultivation bonus (health, damage, speed, armour, the qi pool and its gathering) and
cultivation speed by 5% a step, for good; the cultivation menu shows it.

**The altar** adds up to +30% and **height** up to +15% (a point for every 10 blocks above sea level); the chance never
passes 95%. The altar is judged freely, by what is built round the Heaven Altar block (`HeavenAltar#judge`), and must see
open sky:

| What | Points |
|---|---|
| A raised platform it stands in the middle of: 3x3 / 5x5 / 7x7 / 9x9 | 2 / 5 / 8 / 10 |
| Levels (terraces) stepping down from it (square or round, up to 3) | 3 each |
| Noble stone in the platform (polished, carved, tiled or bricked stone, quartz, jade, calcite, precious blocks) | up to 6 |
| Pillars 3 blocks tall on the platform | 1 each, up to 4 |
| Lit candles, lit fires, lanterns | 1 each, up to 5 |
| Treasures on Spirit Pedestals | 2 each, up to 4 |

Humble (0-9 points) +0%, Modest (10-19) +10%, Grand (20-29) +20%, Magnificent (30+) +30%. Code: `HeavenRitual`,
`HeavenAltar`, `HeavenAltarBlock(Entity)`; the gate is `PlayerCultivation#needsHeavensLeave`.

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

## Heaven methods

The heaven methods are the living's techniques, cultivated on the qi of the five elements (wood, water, fire, earth and
metal, the motes Qi Sense shows). Your cultivation is **its own bar**, as it always was: the methods speed it, bring spells
and passives as it climbs, and open the way into new realms. There are **thirty**: one of each element for each realm, Qi
Refining to Four Axis. Each has its own layers, a passive that grows with them, and **two spells**. The dead can't take one
up (their meridians belong to the ghost path now), nor a mortal.

**Layers.** A method of a realm spans **Qi Refining up to that realm**, nine layers a realm (`MethodLayers`, the same layout as
the ghost foundations): a Qi Refining method has 9 layers, a Four Axis method 54. Within each realm's nine, layers 1-2 stand
for Early, 3-5 Mid, 6-8 Late and 9 Grand Perfection.
- **Cultivation needs no method.** Meditation fills the bar, the stages step on by themselves, and the gates are the realm's:
  the Ritual to the Heavens at Qi Refining Mid, a tribulation at every Grand Perfection (and every stage from Nascent Soul),
  Foundation and Core Pills, the lower realm's cap. Without a method you cultivate at **three quarters' pace**.
- **A method is understood as far as your cultivation has come** (`HeavenMethods#understoodTo`): learned, its layers up to
  your stage are yours at once. Each time you reach a new stage, **its layers settle in** while you meditate
  (`HeavenMethods#settle`), on half the stage's own cultivation, gathered by the method's element (so half as long as the
  stage takes at a plain pace: under a minute at Qi Refining, several minutes in the highest realms), each told as it does
  ("layer 3 settles in", with the spell it brings). Every method you know settles at once, each by its own element. A
  method's layers never run ahead of your realm.
- **Methods open the way into realms**, and nothing else waits on them: breaking into a realm wants a heaven method **of that
  realm or deeper**, of any element (`HeavenMethods#opensWay`). A Qi Refining method takes you no further than Qi Refining
  Grand Perfection; for Foundation Building you need a Foundation Building method (or deeper). Minor stages, and the stage
  tribulations from Nascent Soul, never wait on one.
- **A fall takes the layers past it**: a failed tribulation (a stage down, its cultivation lost), the nascent soul lost (back to
  Core Formation Grand Perfection), the Chief Judge's gaze (back to Qi Refining Early) leave every method understood no deeper
  than the stage you fall to (`HeavenMethods#clampTo`); they settle in again as you climb back.
- **The Cultivation tab's bar** is the cultivation bar, in the colour of the **element** of the method you practise (wood
  green, water blue, fire red, earth ochre, metal silver; the old cyan with none), the stage's cultivation written on it,
  unrefined pill qi a pale stretch beyond the fill. It is framed in amber while the realm is held (a tribulation's
  bottleneck, the ritual's gate, a breakthrough waiting on a method) and in gold at Four Axis Grand Perfection. Under it, the
  method you practise, its layer, and what meditating where you are adds a second; a layer still settling is among the
  details below ("Layer 4 settling: 40%").

Pills and fruit are refused at a bottleneck, at the very peak, or while the realm is suppressed, as they always were. Cultivator
NPCs advance by their own reckoning. (A save from before keeps its cultivation, and every method it knows is understood as far
as its realm on loading. `/cultivation set` leaves every method you know understood exactly as far as the stage set;
`/heavenmethod learn <id> <layer>` sets a method's layer, past the realm too, though its spells and passive still wait on it.)

**The rate.** What meditation gathers, every boost to it included (Rings of Power, mats and the other surroundings, Qi
Harmony, a held spirit stone, a teacher's Insight, Heaven's Favour, the Upper Realm's dense qi, the Inner Realm), goes onto the
bar x `HeavenMethods#cultivationFactor`, the method you practise's:
- the **element's share** of the qi where you sit (`HeavenMethods#elementFactor`: x0.75 where there is none of it, x1 at an
  even fifth, up to x1.5 where it runs thick (three fifths or more): forests for wood, seas for water, deserts and the Nether
  for fire, mountains and caves for earth and metal);
- x1.25 within 8 blocks of a **Dao Stele** of the same element (`STELE_BONUS`);
- and a **Qi Gathering Pill**'s and cultivator **clothing**'s boost to qi gathering.

With no method, x0.75 (and the pill's and clothing's boosts). Refined pill and fruit qi goes onto the bar as it is, whatever
the method. The layers settle in by the same factor, each method by its own element. While the realm is suppressed (Four Axis
in the lower realms), nothing gains. Choose which method to practise on the Methods tab's **Heaven Path** page (the Ghost Path
and Heaven Path buttons at its top switch pages): the deepest realm your methods open the way into, each method's layer, its
realms as pips, its next layer settling (how far, and how long here) or the stage it opens at, the element's share here, and a
**Practise** button.

**Learning one.**
- **Dao Steles** stand on small worn plinths, their heart-stone of their element (moss, prismarine, red nether brick,
  granite, quartz): about one in every 60 chunks across the Overworld and one in 32 in the Upper Realm. The land picks the
  element; rarity picks the realm (`DaoSteleFeature#WEIGHTS`):

  | Where | Qi Refining | Foundation Building | Core Formation | Nascent Soul | Heavenly Being | Four Axis |
  |---|---|---|---|---|---|---|
  | The Overworld | 600 | 250 | 60 | 8 | 0 | 0 |
  | Its rare lands (cherry groves, mushroom isles, ice spikes, eroded badlands, the deep jungles) | 300 | 250 | 150 | 40 | 3 | 0 |
  | The Upper Realm | 0 | 40 | 150 | 100 | 40 | 10 |
  | Its wondrous lands (Dense Qi Peaks, Thunder Peaks, Ancient Sword Graveyard, Spirit Forest, Peach Blossom Sanctuary) | 0 | 0 | 100 | 100 | 60 | 25 |

  Meditate within 4 blocks of one for 15 seconds and you learn its method; touch it to read what it holds. They can't be
  broken.
- **Jade Slips**, read (right-click), teach their method at once and aren't used up. Every living cultivator cultivates a
  heaven method of its sect's element (or its own, for a rogue), of its own realm (a sect's elders and master always, others
  four times in ten) or one below it. One struck down leaves its slip a quarter of the time (a tenth for a sect member).
  Elders and the master sell their sect's slip to one they are Warm to, rogues to one they are Courteous to: 60 stones for a
  Qi Refining method, three times as much for each realm deeper (180, 540, 1,620, 4,860, 14,580).
- **Taught**: a sect's elders and master, and rogues, answer "Will you teach me your method?" for one they are Warm to,
  once a day: the method, or, if you know it, its layers still settling settled at once and ten minutes of your stage's
  plain meditation on the bar.

The first method learned is practised, and a deeper one of the same element takes over the practice. Methods of one element
don't stack (only the deepest counts); methods of different elements do.

**Passives.** Every layer of an element's deepest method strengthens its passive, by the realm the layer stands in
(`HeavenPassives#PER_LAYER`), once **your cultivation has reached the stage that layer stands for** and the layer has settled in
(a layer an op set ahead of your realm adds nothing until the realm catches up):

| Element | Passive | Per layer: QR / FB / CF / NS / HB / FA | At a full Four Axis method |
|---|---|---|---|
| Wood | Evergreen Vitality: health regained each second | 0.02 / 0.04 / 0.08 / 0.15 / 0.25 / 0.35 | 8.01 a second |
| Water | Ceaseless Flow: qi gathered each second | 0.02 / 0.06 / 0.15 / 0.4 / 1.1 / 3 | +42.57 a second |
| Fire | Fury of the Fanned Flame: attack speed | 0.25% / 0.3% / 0.35% / 0.4% / 0.5% / 0.6% | +21.6% |
| Earth | Unshaken Mountain Body: less harm taken | 0.2% / 0.25% / 0.3% / 0.35% / 0.4% / 0.5% | 18% less (never the void's, the soul harms', or a tribulation's) |
| Metal | Whetted Edge: melee harm | 0.05 / 0.1 / 0.25 / 0.5 / 1 / 2 | +35.1 |

**Spells.** Each method brings two spells, at the **3rd and 7th layers of its own realm** (a Four Axis method's 48th and
52nd), and only once **your cultivation has reached the stage that layer stands for** too (`Spell#realmNeeded`: the 3rd
layer's Mid, the 7th's Late, of the method's realm). The Spells tab shows a spell still locked with both. They go on the
spell wheel (V) as they come, whether the layer or the realm comes last, like the ghost spells (never the ability wheel),
cost heaven qi and have cooldowns. A
spell that needs something to take hold of (a being looked at, the ground) isn't cast without it, and nothing is paid. Their
qi harm is the element's own and passes armour. None touches its caster, spectators or its tamed creatures; harm to another
player needs PvP on, and the spells that hold, carry, swap or lull only slow or harm the great beasts (the Ender Dragon, the
Wither, the Warden, an Elder Guardian, anything of 300 health or more). Blocks a spell sets down (the rampart) are always
taken up again, and nothing is broken.

| Element | Method (realm) | Spells |
|---|---|---|
| Wood | Green Sprout Breathing Manual (QR) | **Grasping Roots** (roots bind the being looked at, hauling it down from the air); **Bramble Mantle** (melee blows on you softened, their strikers torn) |
| Wood | Coiling Vine Scripture (FB) | **Wisteria Ascent** (a vine hauls you to the block looked at, where you cling); **Twin Serpent Vines** (two beings bound together, hauled back when they stray) |
| Wood | Ancient Cypress Art (CF) | **Bursting Thorn Pods** (hidden pods burst under foes); **Heartwood of Nine Rings** (rooted, ring on ring warding and mending you; the uprooting lends a blow) |
| Wood | Primeval Forest Canon (NS) | **Pollen of Long Dreams** (a cloud that puts foes to sleep); **Myriad Thorn Thicket** (whatever moves in it is torn) |
| Wood | Eternal Spring Sutra (HB) | **Strangler Fig Seed** (its host's healing comes to you); **Rite of Returning Spring** (friends mended, their hurts growing back) |
| Wood | Heaven-Bearing World Tree Classic (FA) | **Heaven-Shading Canopy** (missiles caught in its leaves give you qi); **The World Tree Rises** (allies mended beneath it, foes struck by its roots) |
| Water | Morning Dew Gathering Art (QR) | **Hoarfrost Needle** (frost stacks; three freeze); **Morning Mist Shroud** (creatures lose track of those inside; you unseen) |
| Water | Clear Fount Cleansing Manual (FB) | **Returning Current** (the current carries you back to a spring, half your lost health with you); **Renewing Mountain Rain** (friends mended, their cooldowns running twice as fast) |
| Water | Nine-Bend River Record (CF) | **Coiling Stream Lash** (a being swung over your head and dashed down behind you); **Nine-Bend Whirlpool** (swept round and held, then flung out) |
| Water | Moon-Mirror Lake Sutra (NS) | **Moon in the Water** (you and the being looked at change places); **Still Lake Mirror** (blows halved, then mirrored back) |
| Water | Heavenly River Scripture (HB) | **Abyssal Water Prison** (a globe of water drowns its prisoner); **Overturned Heavenly River** (a wall of water carries all before it and crashes) |
| Water | Boundless Four Seas Canon (FA) | **Ten Thousand Reflections** (foe creatures turn on one another); **Eternal Winter Descends** (all frozen still, the harm held in the ice until it shatters) |
| Fire | Ember Kindling Art (QR) | **Sleeping Ember** (an ember lodged in a being bursts later, or when you strike it); **Flash of Struck Flint** (foes looking at you blinded) |
| Fire | Furnace Heart Manual (FB) | **Fire Horse Gallop** (swift, leaving burning hoofprints); **Sealed Furnace Refinement** (harm cut, fire's whole, the held-back harm refined into qi) |
| Fire | Crimson Crucible Classic (CF) | **Prairie-Burning Spark** (crimson fire leaping from being to being); **Crimson Lotus Blooms** (a lotus of fire bursts and scatters twelve petals) |
| Fire | Vermilion Bird Sutra (NS) | **Ash-Searing Talon** (the wound can't heal); **Vermilion Bird Plunge** (soar, glide, and strike where you land) |
| Fire | Golden Crow Great Sun Scripture (HB) | **Light of First Dawn** (friends' ills burned away and barred a while); **Noonday Golden Crow** (a sun overhead sears all it can see) |
| Fire | Heaven-Burning Phoenix Nirvana Canon (FA) | **Three Heaven-Scorching Firestorms** (three storms hunt a foe each); **Undying Phoenix** (death turned to ash and a rebirth) |
| Earth | Rammed Earth Manual (QR) | **Rolling Millstone** (runs over and shoves what it meets); **Rammed Earth Rampart** (a wall raised row by row, crumbling after) |
| Earth | Thousand-Layer Stone Record (FB) | **Stone Scale Armour** (three scales, each stopping or blunting a blow); **Swallowing Sands** (quicksand that holds and sinks) |
| Earth | Five Sacred Peaks Classic (CF) | **Earth-Splitting Fissure** (a line of earth erupts; a wall two high stops it); **Mount Tai Presses Down** (a being held down and crushed on the ground) |
| Earth | Yellow Dragon Earth-Vein Canon (NS) | **Earth Dragon Turns Over** (a dragon hunts its quarry from below); **Earth-Vein Guardian** (a stone head bites the nearest foe again and again) |
| Earth | Sovereign Earth Myriad-Bearing Sutra (HB) | **Bedrock Bears the Blows** (part of every hurt sinks away, the rest comes slowly); **Shoulder the Myriad Burdens** (you bear half your allies' hurts) |
| Earth | Four Pillars Upholding Heaven Scripture (FA) | **Riding Ten Thousand Dunes** (a river of sand carries you and your allies, trampling foes); **Fall of Buzhou** (a mountain falls on all in its shadow) |
| Metal | Whetstone Edge Manual (QR) | **Swallow-Return Thread** (pierces going out and coming home); **Iron Bell Cover** (each blow two the less) |
| Metal | Hundred-Forged Lodestone Art (FB) | **Lodestone Brand** (every missile near bends into the branded); **Hundred Weapons Kneel** (enemies numbed, creatures disarmed, players' items seized) |
| Metal | Golden Thunder Core Sutra (CF) | **Armour-Splitting Finger** (a crack in the armour that every blow widens); **Leaping Golden Lightning** (leaps from being to being) |
| Metal | Nine Bells Sword-Soul Classic (NS) | **Nine Circling Blades** (each darts at a foe that comes near); **Thousand-Echo Toll** (those who hear it share one another's wounds) |
| Metal | Heaven-Severing Sword Scripture (HB) | **Fate-Severing Edge** (every blessing severed, none taking hold after); **Ten Thousand Swords Descend** (a sword a second on all beneath, roofs sheltering) |
| Metal | Four Swords Immortal-Slaying Canon (FA) | **Indestructible Golden Body** (no blow takes more than a tenth of your health; nothing moves you); **Four Gates Immortal-Slaying Array** (enemies sealed in a great square and struck) |

Each spell's own numbers (harm, reach, how long) are its description in the Spells tab and the constants at the top of its
element's class. Two notes on the calls made where the designs left room: Hundred Weapons Kneel takes players only when they
are at odds with you (the same rule as a spell's foes), and a creature's weapon falls to the ground if it picked it up (it
keeps those for good) but shatters if it spawned holding it, so none can be farmed.

**Commands (op):** `/heavenmethod learn <id> [layer]`, `/heavenmethod progress <amount>` (that much settling on the practised
method, no further than the realm), `/heavenmethod forget <id>`.

Code: `HeavenMethod` (the thirty, their ids and realms), `HeavenState` (what you know, saved), `HeavenMethods` (who, how far
understood, settling, the gate, the rate, learning), `HeavenPassives`, `MethodLayers`, `HeavenSpells` (the kit: who is spared, targets, what
lingers, wards, temporary blocks, the elements' qi harm) and `WoodSpells`, `WaterSpells`, `FireSpells`, `EarthSpells`,
`MetalSpells`; `Spell` (the sixty); `DaoSteleBlock`, `DaoSteleFeature` (`DAO_STELE_CHUNKS`), `JadeSlipItem`,
`CultivatorNpc#heavenMethod`, `TalkSystem#teachHeaven`, `CultivatorTrades#heavenSlipPrice`, `MeditationManager#heavenMethods`,
`NetherworldCommand#registerHeaven`; client `CultivationScreen` (the Methods tab's heaven page), `ClientMethods`,
`HeavenLangText` (the methods' and spells' English). Tests: `HeavenMethodsGameTests` and one `Heaven<Element>SpellsGameTests`
per element (each spell in a batch of its own).

## The Inner Realm

After **10 seconds of meditation** the screen fades to black and your soul turns inward: you open your eyes on your own
island in a void, while your body stays sitting where you were (other players see it, in your skin). Meditation inside is
**x1.5** as fruitful, and still gets the mats, pedestals, height and biome bonuses from around your body; suppression and
the Upper Realm's x10 also follow the body.

- **Walk freely.** The island is yours to walk, and you can step off it: a step below its top an unseen floor (barrier
  blocks) runs 48 blocks out from its middle, walled 3 blocks high round its edge, so there's room to walk off into the
  distance and to fight a heart demon, and no falling off (should you get past the wall anyway, you're set back on the
  seat). No digging or building; what a creative player breaks of the island or its floor is whole again 2 seconds
  later.
- **Leaving:** press the meditate key (**H**) to stop meditating; the eyes open back in your body.
- **Your body:** anything that hits it pulls you back to take the blow; being pushed or moved does too. Hostile mobs near
  it notice it, and the chunks around it stay loaded while you're away. Logging out inside puts you back at your body.
- It's an ability (on by default) that can be switched off on the Abilities tab or the ability wheel. No tribulations from inside.

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
`inner_realm` (at Y 128, each player's island in a slot of its own, 4096 blocks apart in a grid from the origin, handed
out the first time they need one and kept for good, so no two players share an island: `InnerRealm.Islands`).

## The ability wheel

Abilities can be switched on and off without opening the menu: **hold Z** (rebindable: "Ability Wheel (hold)") and an
eight-slot wheel opens in the middle of the screen. Point toward a slot (the direction is enough, the pointer needn't be
on it) and **click** to switch that ability on or off; click as many as you like, then let go of the key to close it.
The middle names the ability under the pointer and whether it's On or Off. A slot's rim is gold with a green lamp when
its ability is on, grey and dimmed when it's off.

You keep moving while it's open: walk, jump (double-tap for Qi Flight), sneak and sprint all work, toggled Sneak/Sprint
included. Only the view can't turn, since the mouse is pointing at the wheel. The wheel only opens once you've awakened
an ability; a mortal pressing the key just sees "No abilities awakened yet." above the hotbar.

**Arranging it:** cultivation menu, Abilities tab, **Arrange Wheel** (the button switches back to the **Ability List**).
The wheel is on the left, your awakened abilities on the right (gold-framed if already on the wheel):

- drag an ability onto a slot to put it there; if it was already elsewhere on the wheel, it trades places with what was
  in that slot
- drag a slot onto another slot to swap them, or off the wheel to empty it (right-click a slot also empties it)

A new cultivator's wheel starts with the abilities in menu order, and saves from before the wheel get that default too.
It's saved with the rest of your cultivation and kept through death. An ability placed on the wheel that you haven't
awakened (or can't use under a lower realm's suppression) shows as an empty slot.

**Icons:** every ability has its own 32x32 icon (`textures/gui/ability/<id>.png`, drawn whole at whatever size is asked,
so the art can grow without code changes), shown on the wheel, in the editor and
beside its name in the ability list: Qi Sustenance a red rice bowl with qi rising like steam, Qi Flight the golden core
borne up on azure qi wings (it was a sword on a cloud until Sword Flight came), Qi Sense an eye ringed by the five
elements' motes, Consciousness Domain a lattice sphere around a bright soul, Realm Suppress a mountain pressing down on
red chevrons, the Inner Realm a floating island with a blossom tree, Sword Flight a sword with streaks of light behind it;
the ghost abilities' are under The Netherworld. A new ability needs a PNG of its own there.

Spells don't go on this wheel: they have their own (hold V, see The Netherworld).

Code: `client/AbilityWheelScreen.java` (the held wheel), `client/AbilityWheel.java` (icons, slot layout and drawing),
`client/CultivationScreen.java` (the editor), `PlayerCultivation` (`WHEEL_SLOTS`, `setWheelSlot`; NBT "AbilityWheel"),
`CultivationManager#setWheelSlot` (packet `set_wheel_slot`); tests in `AbilityWheelGameTests`.

## Sword Flight

From **Foundation Building Early**, a cultivator can ride a sword. Hold any sword in your main hand (vanilla or modded:
anything that is a sword or in the `swords` item tag) and **double-tap jump**, as for Qi Flight. The sword leaves your
hand and lies flat under your feet, point first the way you look; you stand on it sideways, the leading arm raised
toward the point and the other held out behind, and a glowing azure trail streams out behind you with a few glints of
sword light left hanging in the air. Look down in first person to see the blade under you.

- **Cheaper, slower.** It costs less qi than Qi Flight and flies at **75%** of its speed (sprinting doubles both as
  usual). That speed is fixed from the day it awakens: no realm, stage or gear makes the sword faster (NPCs' swords
  neither), so Qi Flight stays the way to fly fast. Qi per second by realm: Foundation Building 7, Core Formation 18 (Qi Flight 28), Nascent Soul 45 (55),
  Heavenly Being 48 (60), Four Axis 110 (150). A full Early pool rides for about a minute at Foundation Building and Core
  Formation. Like Qi Flight it never runs out in the Upper Realm or from Heavenly Being on, and it stays finite in the
  lower realms through Core Formation even in the full Immortal set; in that set Nascent Soul Late and Grand Perfection
  ride forever there.
- **The sword takes no wear** from being ridden.
- **One flight at a time.** Only one of Sword Flight and Qi Flight can be switched on; switching one on (Abilities tab or
  the ability wheel) switches the other off, and both can be off. With Sword Flight on you need a sword in hand to fly,
  and putting it away mid-air drops you; with Qi Flight on a sword in hand is just held. When your golden core forms,
  Qi Flight comes on and Sword Flight goes off (a chat line says so); a save from before this rule with both on keeps
  Qi Flight the same way. NPCs aren't bound by this: they pick whichever suits them as they go (below).
- Running dry, falls, the Spatial Gap and Realm Suppress work as for Qi Flight: out of qi you drop (flight returns at 5%),
  falls hurt, no flight in the Gap, and pressure that holds you below Foundation Building grounds you.
- A chat line marks the day it awakens; the ability is on by default. It's appended to new wheels (slot 7); a wheel saved
  before it existed has to have it dragged on.

**NPC cultivators** ride their swords too: every one from Foundation Building up carries one. They ride wherever they fly
(journeys, seclusion, returning to their grounds, chasing a flier, fleeing), except that one able to fly on qi switches
to Qi Flight while in a fight or running from danger, where speed matters more than qi.

Code: `SwordFlight.java` (rules, speed, syncing riders to nearby players: packet `sword_flight`), `QiFlight.java` (runs
both flights for players), `Realm` (the sword/sec column), `CultivatorNpc` (`isSwordFlying`, mode choice in `tickQi` /
`setQiFlying`), `client/SwordFlightClient.java` (the sword underfoot and the trail), `client/SwordStance.java` and
`client/mixin/LivingEntityRendererMixin` (the sideways stance), `client/mixin/ItemInHandLayerMixin` /
`ItemInHandRendererMixin` (the hand left empty); tests in `SwordFlightGameTests`.

## Soul Projection

From **Nascent Soul Early**, a cultivator can send their nascent soul out of the body. Switch **Soul Projection** on (ability
wheel or Abilities tab) while standing on steady, dry ground with at least 10% of your qi: you sit in meditation for
**5 seconds** while the soul gathers itself (moving or being hurt breaks it off), then it rises out. Switch it again to
call it home: it is drawn back along its tether for **5 seconds** and you stand up where your body sat. A chat line marks
the day the soul forms, and the ability goes into the first empty slot of your wheel.

- **The body** stays sitting cross-legged where you left it, in your skin and armor, with your health and your armor's
  protection. You're told whenever it is hurt (a warning, and its health on screen), monsters nearby go for it, and if it
  is slain your soul dies with it. Players can only hurt it where they could hurt you (PvP).
- **The soul** is you, flying free of gravity through stone, wood and earth: it moves the way you look (look up and press
  forward to rise), jump and sneak lift and lower it, sprint doubles its pace, and it carries its momentum. It has its own
  hearts, **1 at Nascent Soul Early and 3 more every stage** (10 at Grand Perfection, 13 at Heavenly Being Early, 34 at
  Four Axis Grand Perfection), and they never heal; coming home you have the body's health, not the soul's.
- **The tether**, a thread of bright blue light from your body's chest to the soul, lets it go **32 / 48 / 64 / 96** blocks
  through Nascent Soul's stages, 128-176 through Heavenly Being, 192-256 through Four Axis; it stretches against you as you
  near its end.
- **Qi**: half your realm's gathering a second at the body, rising to twice your gathering at the tether's end, and that
  much again while possessing (so clothing, pills and the Upper Realm's dense qi all help; there it costs nothing net).
  A Nascent Soul Early pool lasts about a minute at the tether's end. **Run dry and the soul snaps home at once** (no 5 s,
  no penalty). Taking hold of a body costs 10% of your pool on top.
- **What a soul can't do**: touch anything. No blocks, chests, items (it picks nothing up and drops nothing), plates or
  tripwires; no meditating, no Inner Realm, no breakthroughs or tribulations (and you can't send the soul out during one),
  no portals, rift or Spatial Gap; no Qi Flight, Sword Flight or Realm Suppress. Qi Sense is forced open: it is how a nascent
  soul sees. Blasts, fire, lava, falls, drowning, suffocation, arrows and monsters can't harm it.
- **Barriers**: a formation's barrier is qi, as the soul is. One that doesn't let you through and is stronger than you stops
  the soul at its shell (and a soul forced across anyway is set back and loses 5% of its pool); one you could break, you may
  pass, but it drains 2% of your pool a second while you're inside.

**Who sees a soul.** Only Nascent Soul cultivators and above, and only with **Qi Sense** open: to them it is your skin as
violet light, the colour shifting through deeper and paler violets, brightest at its edges (Fresnel glow), floating upright
with its legs together trailing behind like a ghost's. To a Nascent Soul Early it is a faint violet haze; it clears with
each stage, whole at Grand Perfection and above. Heavenly Beings and above also see the tether. Everyone else's client is
never even sent the soul, so nothing on it can reveal one. A **Core Formation Grand Perfection** (or stronger) cultivator
whose Consciousness Domain is open senses a soul that comes inside the domain without seeing it: a warning that a nascent
soul is near, no outline, no name. A cultivator with the **Ghost Eye** open (a ghost method's ability, see The Netherworld)
sees souls as a Nascent Soul with Qi Sense open would, whatever their realm.

**Where it can't go.** The heavens can't see the Netherworld, and a nascent soul can't be sent out there or on the Soul
River ("The heavens cannot see you here."); a soul already out can't take a rift up to it. The dead can't send out souls
at all.

**Harming a soul.** Only those who see it, by their own hand: a Nascent Soul cultivator (Qi Sense open) lands a quarter of
a blow at Early, 40% at Mid, 55% at Late, 70% at Grand Perfection (it's a haze to them); a Heavenly Being or above lands it
all, and can strike blind without Qi Sense. A Heavenly Being or above with Qi Sense open can also **strike the tether**:
swing at the thread itself. It holds 20 plus the soul's own health, mends if left alone for 10 seconds, and when it is cut
the soul is lost.

**Losing the soul.** A cut tether, a slain body, a soul struck down, or **logging out within 10 seconds of harm to the soul
or the body** (fleeing a fight) kills you where your body sat (your things fall there) and costs you the nascent soul: you
fall to **Core Formation Grand Perfection** with no cultivation, and must form it again through the Nascent Soul
tribulation. Logging out at peace, the server stopping, or a crash simply bring the soul home.

**Possession.** Right-click a being at least **three realms below** your own (a Nascent Soul takes Qi Refining
cultivators, mortals and beasts; a Heavenly Being, Foundation Building and below; Four Axis, Core Formation and below;
monsters and animals count as mortals) to take it over: you look out through its eyes and move it with its own speed and
strength (it runs as fast as it would run at you, no faster, and its melee is your attack), until you **sneak** to let
go, your qi runs out, you call the soul home, or it pulls too far along the tether. Inside a body your soul can't be
struck; if the body dies, the soul is thrown out unharmed (and
exposed again). **A flier flies** (a bee, parrot, allay, bat, ghast, phantom, vex or blaze): forward goes where you
look, so look up to climb and down to dive, and **jump** rises straight up; let go of the keys and it hangs in the air.
Each flies at its own pace (a bee or parrot gently, a vex fast; a phantom gathers speed while it holds its line, up to
far faster than anything else, and loses it when it turns), slower in water and slower still in lava. It takes no fall
damage while you fly it, and a roosting bat is roused. (A flier you hold is weightless, or a server without flight
allowed would kick you for floating a mount; it gets its weight back when you let go. A vex can't pass through walls
while you hold it: the server checks a ridden body against the blocks it moves through.)
Not bosses, Wardens, mounts, named creatures, or another player's pets. Whatever dies in your hands dies by
your hand (a possessed sect member walked into lava counts as murdered, not replaced). A possessed cultivator talks to no
one, and breaks off any talk it was in.
**A player's body is seized** instead (only where PvP is on): they die, and a body in their likeness takes their place with
their realm's strength and all they carried (unless items are kept on death). You control that body until you let it go,
when it collapses and everything it carried falls where it stands.

**NPCs.** Cultivators of Nascent Soul and above sense souls as players do; below that a soul isn't there for them at all.
Sect members who sense one go for a soul that comes onto their grounds, Nascent Souls striking for less, as players do.
NPCs don't send out souls of their own yet.

Code: `SoulProjection.java` (the rules: going out and back, the tether, qi, harm, severing, possession, who is sent a soul),
`SoulBodyEntity.java` (the body left behind), `SoulVesselEntity.java` (a seized player's body), `SoulBarriers.java`
(formation barriers), mixins `SoulPlayerMixin`, `SoulEntityMixin`, `SoulLivingEntityMixin`, `SoulMobMixin`,
`SoulServerPlayerMixin` (only perceivers are sent it), `SoulProjectileMixin`; client `ClientSoul.java` (its flight, the
tether's pull, striking tethers), `SoulRenderer.java` with core shader `shaders/core/soul.*` (drawn on the sense layer so it
keeps its colour under Qi Sense), `SoulHud.java`, `SoulBodyRenderer` / `SoulVesselRenderer`, client mixins `SoulFlightMixin`,
`SoulInputMixin`, `SoulRenderDispatcherMixin`; tests in `SoulProjectionGameTests`. Shader packs that replace core shaders
(Iris) fall back to a plain violet translucent soul.

## The Netherworld

Above the Nether lies the **Netherworld**, the realm of the dead: misty plateaus of soul soil and blackstone between about
Y 40 and 200 over a bottomless abyss, dim (the ground is seen without a torch, barely), under a sky of its own. It is a
high realm, as the Upper Realm is above the Overworld: Realm Suppression doesn't reach it, portals don't work there, and
NPC cultivators come up into it from below. But its riches are another kind of qi. Ordinary qi is as thin there as
anywhere, and breakthroughs into Four Axis stay the Upper Realm's; what the Netherworld has is **ghostly qi**, which the
dead and those who learn the ghost methods cultivate on. And the heavens can't see it: no tribulation, Ritual to the
Heavens, Inner Realm or Soul Projection there or on the river to it ("The heavens cannot see you here.").

There are two ways in: **die** (you wake there as a ghost), or **go up alive** from the Nether.

Code: `HighRealm` (what the two high realms share), `ModDimensions` (`netherworld`, `soul_river`, `NETHERWORLD_MIN_Y`,
`NETHERWORLD_AMBIENT_LIGHT`), `ModBiomes`, `ModNoiseSettings#netherworld`, `NetherworldManager` (the per-second tick);
client `NetherworldClient`, `NetherworldSkyRenderer`, `UpperRealmClient` (its fog). Tests in `NetherworldGameTests`,
`NetherworldQiGameTests`, `NetherworldDeadGameTests`, `NetherworldMethodsGameTests`, `NetherworldInhabitantsGameTests`.

### Getting there: the Soul River

The Nether's rift, the **Gate of the Yellow Springs**, stands at Nether 0, 0: placed on server start in an open pocket of
that column between Y 32 and 110 clear of lava (or, failing that, a chamber carved at Y 70), never on the bedrock roof,
with a small shrine of blackstone, soul soil and soul lanterns round it. Walk into it, at any cultivation, and you're
taken onto the **Soul River**: a glowing cyan river of streams, sparkles and wisps in a black void, with the souls of
villagers and beasts drifting down it beside you, which carries you for **60 seconds** up to the Ghost Gate. Going down
is the same river the other way: walk into the **way down** on the Ghost Gate's plaza, or fall into the abyss (below
Y 0; this takes anyone, cooldown or not), and it sets you down on safe ground in the Nether: from the way down, by the
Nether's rift, at its own height (ground near it first, never a pocket under the bedrock roof while there is any);
from a fall, under where you fell.

- The river does no harm, but it bends and sways, and its current pushes straight on: **steer** (look and move) to keep
  to its flow. Flying and gliding stop on it.
- Out of the flow for more than **¾ of a second**, or more than 3 blocks past its edge, and **your soul scatters**. Going
  up, the ascent fails: you're thrown back by the Nether's rift, hurt (60% of your max health, never your last heart,
  through armour) and dazed for 12 seconds, and the rift turns you away for a minute. Going down, it spits you out early.
- After any journey down the rifts let you be for 10 seconds, so nobody is swept straight back up.
- A relog mid-river starts the journey over; dying ends it.

**The Eye.** Off to one side of the river, square to its flow so that you must turn to see it, hangs a huge white eye in
the void: the eye of the **Chief Judge of the Netherworld**, a True Immortal. It watches the dead drift by, now and then
looks straight at the living, and blinks. **Don't meet its gaze.** Hold it within 22 degrees of where you look and the
whispers start, a heartbeat quickens, the view rolls and breathes and tears, and a crimson dark closes in; after
**10 seconds** the gaze unmakes you: you die, and your cultivation is stripped back to **Qi Refining Early**, whatever
your realm. Look away and it ebbs (from the brink to nothing in 4 seconds). Nothing below a True Immortal is spared;
creative and spectator players are untouched.

| Soul River | Value | Constant |
|---|---|---|
| Journey, either way | 60 s (480 blocks) | `SoulRiverCourse.TICKS`, `FLOW` |
| The flow's reach from its middle | 4 blocks at the start, narrowing to 3 | `SoulRiverCourse.RADIUS_START` / `RADIUS_END` |
| Grace out of the flow | 15 ticks, or 3 blocks past the edge at once | `SoulRiver.GRACE_TICKS`, `HARD_MARGIN` |
| Scattering | 60% of max health, 12 s dazed, 60 s cooldown | `SCATTER_HARM`, `DAZE_TICKS`, `SCATTER_COOLDOWN_TICKS` |
| After a journey down | 10 s before a rift takes you | `SoulRiver.COOLDOWN_TICKS` |
| The Eye's gaze | 22° cone; death at 10 s; ebbs in 4 s | `JudgeEye.LOOK_CONE`, `GAZE_SECONDS`, `RECOVER_SECONDS` |

Code: `NetherRift` (the Nether's rift and landings), `SpatialRiftBlock#leads` (a rift leads by the realm it stands in),
`SoulRiver` (the server's side: time, straying, scattering, where you come out), `SoulRiverCourse` (the river's path, a
pure function both sides share), `JudgeEye` (where it looks, when it blinks, the gaze's toll; `isImmune` is where True
Immortals will come in); client `ClientSoulRiver` (steering), `SoulRiverRenderer`, `SoulRiverSky`, `RiverSouls`,
`ClientJudgeGaze` with `mixin/GameRendererMixin` (the warping), `mixin/SoulRiverMixin`. Damage types `soul_scatter` and
`judge_gaze` (re-run `runDatagen` after changing them).

### The land and the Ghost Gate

| Biome | What it is |
|---|---|
| **Yellow Springs** | low golden riverlands of pools (held in by banks of mud and soul soil where the land beyond lies lower or a chasm opens, so none spills) and springs; the Netherworld Lotus floats on its water |
| **Fields of the Other Shore** | soul-soil meadows red with spider lilies, a dead tree here and there; the Other-Shore Flower's home |
| **Bone Wastes** | ash and bone, dead trees, soul fire burning on the ground; Hungry Ghosts gather here most |
| **Nine Nether Peaks** | dark crags of blackstone and basalt, where ghostly qi is densest and Yin Jade runs thickest |

The **Ghost Gate** is the Netherworld's hub, at 0, 0, built the first time the server starts with the Netherworld.
North to south:

- the **arrival plaza**, 21 x 21 of blackstone bricks with braziers of soul fire and soul lanterns, where the river sets
  the living down and the dead wake. The **way down** (a rift) is on its north side, and a **Method Stele** of the
  Netherworld Breathing Art faces the arrival;
- the **Gate** itself, two towering pillars of blackstone striped with bone, chains and lanterns hanging from the lintel,
  **Ox-Head** and **Horse-Face** standing guard before it;
- the **Naihe Bridge** over a channel of the Wangchuan, the river of forgetting, with **Meng Po's hall** and her pot by
  its near end, on the bank west of the Gate;
- the **Wheel of Reincarnation**, turning slowly on its dais on the far bank;
- and west of the plaza, the tall **Terrace for Gazing Homeward**, its ramp rising from the plaza's edge.

Within 32 blocks of the Gate's middle, survival players can't break or place blocks and blasts break nothing; Hungry
Ghosts and wandering ghost cultivators never spawn on its ground. The way down, the Wheel's hub and the plaza's stele
are put back on every start if something took them.

Code: `GhostGate` (the build, `GATE_RADIUS`, the fixtures' positions, its keepers), `WheelOfReincarnationBlock`(+`Entity`)
and client `WheelOfReincarnationRenderer`, `SpiderLilyBlock`, `DeadTreeFeature`, `YellowSpringsPoolFeature`. The biomes'
features are in `ModBiomes#netherworldFeatures` and `ModPlacedFeatures` (the `NW_*` features); if you change them, re-run
`runDatagen`.

### Yin corruption

A living body in the Netherworld fills with its yin. The **Yin Corruption** bar (under the qi bar, top left) climbs from
0 to 100%, its percentage beside it with an arrow (up while rising, down while ebbing, a dash while a pill holds it).
Past a quarter a cold cyan-black frost creeps in from the edges of the screen, thicker as it climbs; there are warnings
at 50, 75 and 90%. **At 100% your body gives way: you become a ghost where you stand, keeping everything you carry** for
now; none of it goes back with you when the Wheel returns you to life (see The dead, below). Outside the Netherworld it ebbs, from full to nothing in **5 minutes**; on the Soul River it holds
still. Ghosts, and players in creative or spectator, take none.

How long a stay fills it, by true realm:

| Realm | Full in | Constant (`YinCorruption`) |
|---|---|---|
| Below Nascent Soul (mortals too) | 1 minute: they have no business there | `STAY_BELOW_NASCENT_SOUL` |
| Nascent Soul | 5 minutes | `STAY_NASCENT_SOUL` |
| Heavenly Being | 10 minutes | `STAY_HEAVENLY_BEING` |
| Four Axis | 20 minutes | `STAY_FOUR_AXIS` |

times the stage's power (Early 1.0 up to Grand Perfection 1.3), times 1 + 10% for each layer of your deepest foundation
(up to 18: `BREATHING_STAY_PER_LAYER`, `STAY_LAYERS_MAX`): a Four Axis Grand Perfection stays 26 minutes, about 49 with the
ghost path at Qi Refining's Grand Perfection (9 layers deep), about 73 from Foundation Building's (18) on.

| What changes it | How | Constant |
|---|---|---|
| **Netherworld Veil** (ability, below) | yin comes in at half the rate | `YinCorruption.VEIL_FACTOR` |
| **Yang Preservation Pill** | holds it still: 3 minutes per point of potency, 15 at most | `PillItem.PRESERVATION_TICKS_PER_POTENCY`, `MAX_PRESERVATION_TICKS` |
| **Yang Fire Talisman** | burnt (right-click), drives a third of it out at once; not spent on a body with no yin | `YinCorruption.TALISMAN_CLEANSE` |
| A blow from the dead | each drives 5% more in | `YinCorruption.GHOSTLY_BLOW`, `HungryGhost.YIN_PER_BLOW` |

The **Yang Fire Talisman** is crafted from paper, blaze powder and a gold nugget with the Inscription Brush (shapeless;
the brush comes back a use more worn). It stacks to 16, and ghost cultivators sell them.

Code: `YinCorruption` (the stay, the rate, the fade, the warnings), `YangFireTalismanItem`, `NetherworldState` (saved in
the "Netherworld" compound of a player's cultivation, only when it differs from the defaults); client `NetherworldHud`
(the bars and the frost), `ClientNetherworldData`.

### Ghostly qi

The second kind of qi. Ordinary qi is the cultivator's own; ghostly qi belongs to places, and is what the ghost methods
are cultivated on and the **Ghost Qi** pool fills from. Under Qi Sense it is a sixth element, yin, its motes thick in the
Netherworld.

| Where | Density (a Soul Sand Valley = 1) | The pool fills, a second | Ghost methods go |
|---|---|---|---|
| The Netherworld | 10 (`GhostQi.NETHERWORLD`) | 2 | x10 |
| The Nine Nether Peaks | 12 (x `PEAKS_FACTOR`) | 2.4 | x12 |
| A Soul Sand Valley | 1 (`SOUL_SAND_VALLEY`) | 0.2 | x1 (the baseline) |
| The rest of the Nether | 0.25 (`NETHER`) | 0.05 | x0.25 |
| Everywhere else | 0 | nothing | not at all |

The pool fills at density x 0.2 a second (`FILL_PER_DENSITY`); spirit stones and Spirit Dew don't fill it. Everyone who
has one sees it as a cyan **Ghost Qi** bar: every ghost, and every living cultivator who knows a ghost method. It holds
30 (`BASE_POOL`), plus 10 for every layer known (`POOL_PER_LAYER`: your deepest foundation's, and the fire's and the
steps'; `GhostMethods#layersKnown`), plus 20 more for every layer of your deepest foundation (`BREATHING_POOL_PER_LAYER`).
Ghost abilities and spells are paid from it, never from your qi.

Code: `GhostQi` (density, fill, pool size), `QiElement.YIN`, client `QiSenseClientHandler`, `NetherworldHud`.

### The dead

While the gamerule **`netherworldAfterDeath`** is on (the default), a player who dies wakes at the Ghost Gate as a
**ghost** instead of at their bed. The death itself is vanilla's (its message, its drops, keepInventory if set); only
the respawn is the Gate's. A living visitor overtaken by yin becomes a ghost where they stand instead, and keeps all
they carry, as a ghost. With the gamerule off, death is vanilla's and there are no ghosts.

A ghost:

- is drawn as cyan light in its own skin, brightest at its edges, with its name above it, and is seen by everyone in the
  Netherworld. It walks with gravity and walls, and takes no fall, drowning, fire, lava, suffocation, freezing or hunger.
  Its tread makes no sound: no footsteps on any block, no splashing, no thud when it lands (on its own client as on
  everyone else's). Ghost cultivators and Hungry Ghosts walk as silently, and none of the dead is slowed by soul sand or
  soul soil (`SoulEntityMixin`). In first person its own hand is the same cyan
  light; it swings its arm when it strikes, as the living do, and when it meditates it sits cross-legged, floating a
  quarter of a block off the ground.
- has **half its living hearts**, plus a heart for every layer of its deepest foundation, and heals a heart every two seconds.
- **flies**: the dead need no ground. Double-tap jump to fly, at a drift (60% of Qi Flight's speed, `QiFlight.GHOST_SPEED`),
  or as fast as Qi Flight with Ghost Flight on (below); not on the Soul River, nor while a Ghost-Quelling Talisman binds it.
  Holding jump while falling, it drifts down gently.
- **touches nothing of the living world.** It can't break or place, use items or blocks (but it can use the dead's own:
  steles, Meng Po's pot, the Wheel, a ghost palace's treasury chests; and read a Ghost Jade Slip or a Jade Slip, though it
  takes up no heaven method), drop or pick up anything (but the experience that comes to it, which it takes as the living do,
  and the dead's money and writings: spirit stones, Yin Jade, the slips, a Marrow Cleansing Elixir: `Incorporeal#mayCarry`),
  ride, take a rift or portal, meditate on the main track, break
  through, fly on qi or a sword (it has its own flight), Realm Suppress, turn inward or project its soul. Arrows, blasts and ordinary mobs pass
  through it. It cultivates only its ghost method, half again as fast as the living do (below).
- **is struck** only by what touches the dead: another ghost's or a ghostly creature's blows, ghost fire, a Peach Wood
  Sword (in full, and twice over), a Ghost-Quelling Talisman (below), and the living who can see it, as they would a projected soul (a Nascent Soul with
  Qi Sense open lands a quarter to 70% of a blow by stage, a Heavenly Being or above all of it). A ghost's own blow on
  the living is a ghostly one, light (half a heart) but driving 5% of the Netherworld's yin into them.
- at no health **disperses** in cyan sparks and reforms at the Gate **5 seconds** later with half its health and half
  its ghostly qi. Falling into the abyss does the same (a Ghost King, below, rides the Soul River down instead). The
  scattering ends the fight: whatever fought it lets it go (`CultivatorNpc#letGoOf`), so the Gate's wardens, say, don't
  cut it down again the moment it reforms before them. A ghost that logs out scattered reforms at the Gate when it
  returns; the scattered body is marked in its own saved data (a tag), so even with the ghost's record gone (a
  development client's new random name each launch is a new player) it comes back seen and touchable, never
  untouchable for good.

**Coming back.** Use the **Wheel of Reincarnation**'s hub, on the far bank: it turns, and you return to life at your own
respawn point (your bed or anchor, as vanilla would choose it). In hardcore the Wheel refuses. **Nothing you carry goes back
with you**: whatever a ghost still holds (only one overtaken by yin holds anything, since a death drops its things and the
dead pick nothing up) falls where it stands at the Wheel, inventory, armour and rings alike, and what bears the Curse of
Vanishing is gone, just as at a death; you'll have to come back up the Soul River for it. With keepInventory on, as at a
death, you keep it all (`GhostSystem#leaveBelongings`; the same goes for `/netherworld revive`). Before that:

- **The Terrace for Gazing Homeward**: climb it, and standing at its top a ghost sees where it died (the dimension, and
  the place to within 10 blocks); a journal note keeps it.
- **Meng Po's soup**: drink from her pot or ask her for it, and when the Wheel returns you to life your medicinal
  toxicity is gone (the pills' diminishing returns start over). The living are turned away.

A creative ghost keeps the look and none of the rules.

| The dead | Value | Constant (`GhostSystem` unless named) |
|---|---|---|
| Hearts | half the living's, +1 per layer of the deepest foundation | `HEALTH_SHARE`, `HEALTH_PER_BREATHING_LAYER` |
| Healing | a heart every 2 s | `REGEN_PER_SECOND` |
| Dispersal | reforms after 5 s with half health and qi | `DISPERSE_TICKS` |
| A ghost's blow on the living | half a heart, +5% yin | `GHOSTLY_BLOW`, `YinCorruption.GHOSTLY_BLOW` |
| The Wheel's turning | 2 s | `WHEEL_TICKS` |
| The Terrace | within 3.5 blocks of its top stone; the place to 10 blocks | `GhostGate.TERRACE_REACH`, `TERRACE_ROUNDING` |

Code: `GhostSystem` (becoming a ghost, the rules, dispersal, the Wheel), `Incorporeal` (what a ghost shares with a
projected soul), `ModGameRules`, `GhostGate` (the Terrace, Meng Po's pot), mixins `SoulEntityMixin`, `SoulPlayerMixin`,
`SoulLivingEntityMixin`, `SoulServerPlayerMixin`, `SoulProjectileMixin`, `ServerGamePacketListenerImplMixin` (a hardcore
ghost isn't made a spectator); client `SoulRenderer` (ghosts drawn with the soul shader, `shaders/core/soul.*`, turned to
the dead's cyan), `ClientGhosts`, `mixin/GhostDriftMixin`, `mixin/SoulRenderDispatcherMixin`.

### Ghost methods

The ghost methods are cultivated on ghostly qi, by the dead and the living alike, layer by layer. Each layer brings
something, and some bring abilities and spells.

**The ghost path.** The ghost methods are a path of their own beside ordinary cultivation, and **neither builds on the
other**: the foundations carry the **ghost path** through the realms, never your ordinary cultivation, and your ordinary
realm neither opens a ghost method nor speeds it. A Four Axis cultivator who comes down starts the ghost path at Qi
Refining like anyone else. But walking both paths makes you stronger than walking either (see **The two paths** below).
The Methods tab shows where your ghost path stands (`GhostMethods#ghostRank`).

**The foundations** carry the ghost path **from Qi Refining up to their own realm**, nine layers to each realm they span
(`MethodLayers`): the Netherworld Breathing Art has 9 layers (Qi Refining alone), the Yellow Springs Marrow Art 18 (Qi
Refining, then Foundation Building), and so on to the Six Paths Reincarnation Canon's 54 (Qi Refining all the way through
Four Axis). The deeper the foundation, the further it goes and the rarer it is; whichever you find, you can practise it
from the start. Each foundation's spells come in its own realm (its last nine layers):

| Foundation | Carries the ghost path | Layers | Spells (layer) | Where it is found |
|---|---|---|---|---|
| **Netherworld Breathing Art** | Qi Refining | 9 | Ghostly Wail (5); abilities Ghost Eye (3), Netherworld Veil (6) | steles everywhere (the most common), one on the Gate's plaza; slips from ghost cultivators |
| **Yellow Springs Marrow Art** | to Foundation Building | 18 | Yellow Springs Tide (12), Marrow Ward (16) | steles everywhere (uncommon); Foundation Building ghosts teach it, sell its slip (180 stones) and now and then drop it |
| **Ghost Core Canon** | to Core Formation | 27 | Ghost Core Burst (21), King's Decree (25) | Core Formation Ghost Kings (most palaces); now and then a stele anywhere, more often on the Nine Nether Peaks |
| **Nine Nether Soul Sutra** | to Nascent Soul | 36 | Soul Rend (30), Nine Nether Step (34) | Nascent Soul kings (about one palace in four); Nine Nether Peaks steles |
| **Yama's Judgement Scripture** | to Heavenly Being | 45 | Yama's Verdict (39), Chains of Hell (43) | Heavenly Being kings (about one palace in eight); Nine Nether Peaks steles, rare |
| **Six Paths Reincarnation Canon** | to Four Axis | 54 | Wheel of Samsara (48), Six Paths Annihilation (52) | Four Axis kings (one palace in twenty); Nine Nether Peaks steles, about three in 1,371 |

A king teaches its foundation to one it is Warm to, sells its slip to one it is Warm to (60 stones for the Breathing Art,
three times as much for each foundation deeper: 180, 540, 1,620, 4,860, and 14,580 for the Six Paths Reincarnation Canon),
and always leaves the slip when it falls.

Every layer of your deepest foundation adds +20 ghostly qi to the pool and a heart as a ghost, and (up to 18 layers) +10%
to the living's stay among the yin. Foundations don't stack: they all lay out the realms the same way, so only the deepest
counts (`GhostMethods#foundationDepth`). **A foundation found later is taken up where your ghost path already stands**
(`GhostMethods#startLayer`): with the Breathing Art perfected, the Marrow Art starts at its ninth layer, progress and all,
and goes on into Foundation Building; a lesser foundation you've outgrown is learned whole, perfected at once, its spells
in reach. Learning a foundation that goes further than the one you practise makes it the one you practise.

Besides the foundations, two methods of their own:

| Method | What its layers bring | Abilities | Spells |
|---|---|---|---|
| **Ghost Fire Scripture** (offence) | a ghost's blows +10% | | Ghost Fire (1), Ghost Fire Bolt (4); the fire leaps to a second foe (7) |
| **Wandering Soul Steps** (movement) | a ghost +3% swifter | Yin Wind Steps (1), Ghost Flight (4, ghosts only) | Soul Dash (2) |

**Learning one.** First your meridians must be open (a Marrow Cleansing Elixir): a mortal, alive or dead, can't take up or
cultivate a ghost method (`GhostMethods#refusal`); anyone else can take up any of them. Meditate within 4 blocks of a
**Method Stele** for 15 seconds and you learn its method. Steles
stand on worn plinths across the Netherworld, one in about every 24 chunks: the Breathing Art's most often, then the
fire's, the steps', the Marrow Art's, and now and then the Ghost Core Canon's (weights 600 / 300 / 200 / 150 / 10); on the
**Nine Nether Peaks** the deeper foundations are carved too (the Marrow Art 200, the Ghost Core Canon 40, the Nine Nether
Soul Sutra 20, Yama's Judgement Scripture 8, the Six Paths Reincarnation Canon 3, of 1,371; `GhostMethod#steleWeight`,
`#peakWeight`). They're unbreakable; touch one to read what it teaches (the dead can too), and the Ghost Eye shows them
through walls. A **Ghost Jade Slip**, read (right-click), teaches its method at once and isn't used up: ghost cultivators
drop them now and then, and Ghost Elders sell their own. A ghost cultivator knows its own realm's foundation five times
in ten (a Ghost King always), otherwise one of the first three methods, and one struck down leaves its foundation's slip
15% of the time. One who is Warm to you will **teach** you its method once a day: the method, or ten minutes' progress
if you know it.

**Cultivating.** Meditate as usual: while ghostly qi flows where you sit, the method you practise gains progress on top
of your ordinary cultivation (a ghost cultivates only the method). Choose which one in the cultivation menu's
**Methods** tab: where your ghost path stands, every method you know, its layer and progress, how fast it's going here
and why, and a **Practise** button. Progress is counted in seconds of baseline meditation, so a layer costs its minutes x
60 (`GhostMethods#layerCost`):

| Layer | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | All nine |
|---|---|---|---|---|---|---|---|---|---|---|
| The fire, the steps | 5 | 8 | 12 | 20 | 30 | 45 | 60 | 90 | 120 | 6.5 h |
| A foundation, each realm's nine, before the realm's factor | 3 | 4 | 5 | 6 | 8 | 10 | 12 | 15 | 20 | 83 min |

A foundation's layers cost the same whichever foundation it is: each realm's nine are the row above times the realm's
factor (`MethodLayers.REALM_FACTOR`): x1 Qi Refining (83 minutes), x2 Foundation Building (2.8 hours), x4 Core Formation
(5.5 hours), x7 Nascent Soul (9.7 hours), x11 Heavenly Being (15 hours) and x16 Four Axis (22 hours). So the Breathing Art
takes 83 minutes, the Marrow Art those and 2.8 hours more, and the Six Paths Reincarnation Canon all of them, about 57
hours, all at the baseline: a living cultivator whose ghost path is at Nascent Soul, in a **Soul Sand Valley**. The rate is
`BASE_RATE` x the density (a Soul Sand Valley's 1 the baseline: `GhostQi.SOUL_SAND_VALLEY`) x the ghost path's factor
(`realmFactor`: -10% a realm below Nascent Soul, +10% a realm above; a ghost path not yet begun counts as Qi Refining) x Qi
Harmony x a Yin Gathering Pill's boost x 1.5 for the dead (`GHOST_FACTOR`) x 1.25 beside a stele of the method you're
practising (`STELE_BONUS`, within 8 blocks). Your ordinary realm doesn't come into it.

Every layer can be cultivated **wherever ghostly qi flows**, the Nether included; the Netherworld is simply the best place
for it. Its qi is ten times as dense, so methods go **ten times as fast** there (twelve on the Nine Nether Peaks): the
Breathing Art in about 8 minutes, the Six Paths Reincarnation Canon in about 5.7 hours (the dead half as fast again).
In a Soul Sand Valley they go at the baseline, and in the rest of the Nether at a quarter of it.

**The ghost path's realms.** A foundation carries the ghost path through each realm it spans (`GhostMethods#floor`; the
deepest layer of any foundation you know counts), for the living and the dead alike. Within each realm's nine:

| Layer within the realm | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| Ghost path, in that realm | Early | Early | Mid | Mid | Mid | Late | Late | Late | Grand Perfection |

So the Six Paths Reincarnation Canon's layer 10 is Foundation Building Early, its 21 Core Formation Mid, its 54 Four Axis
Grand Perfection. There's no tribulation or ritual on the ghost path. A ghost whose ghost path reaches Core Formation (the
Ghost Core Canon's 19th layer, or any deeper foundation's) is a Ghost King (below), whatever its ordinary cultivation. The
Methods tab draws a foundation's layers realm by realm, names the next layer that carries the ghost path on, and once a
foundation is perfected, says a deeper one must be sought out. Saves from before the foundations spanned the realms below
their own are moved over once (`NetherworldState`, "MethodLayout"): a foundation's layer stands for the same stage as before.

**The ghost path's gate.** The fire and the steps go only as far as your ghost path carries them (`GhostMethods#layerCap`):
three layers while it is at Qi Refining (or not begun), up to the sixth at Foundation Building, all nine from Core
Formation, the living and the dead alike. At the gate the layer's bar stays full and the Methods tab says what your ghost
path must reach.

Code: `GhostMethod` (the methods, their realms, layers and rarity; ids saved, add new ones at the end), `MethodLayers`
(the realm-by-realm layout, shared with the heaven methods), `GhostMethods` (the ghost path, rate, layers, learning, what
each layer grants, the gate), `MethodSteleBlock`, `MethodSteleFeature` (`NW_STELE_CHUNKS`
in `ModPlacedFeatures`), `GhostJadeSlipItem`, `MeditationManager#ghostMethods`, `TalkSystem#teach`,
`GhostCultivatorNpc#method`; client `ClientMethods`, `CultivationScreen` (the Methods tab).

### Ghost Kings

One of the dead whose cultivation **or ghost path** is **Core Formation or above** is a **Ghost King**
(`GhostSystem#isKing`): one who died there or later, or whose ghost path rose to it (taking up the Ghost Core Canon); never
a mortal's ghost, whose ghost path lies dormant (below). Whichever path makes it one, the dead are always drawn as the dead:
a king crowned, the rest as common ghosts. It is told so (the Journal keeps a note), and:

- **its hands are its own again.** It breaks and places, uses items and blocks, picks things up and drops them, and strikes
  the living world as the living do (its blows are ordinary ones, no longer ghostly).
- **its way is its own.** It takes rifts and portals: down the Soul River from the Netherworld's way down (or by falling
  into the abyss) to the Nether, and through a Nether portal to the Overworld.
- **it is still one of the dead** in what the living world can't do to it. Arrows, blasts, falls, lava, water and
  ordinary mobs pass through it, and only what reaches the dead strikes it. It still doesn't break through, Realm Suppress,
  fly on qi or turn inward, and its method is still its only cultivation.
- **out of the Netherworld it is seen only by those who perceive souls**: a Heavenly Being or above, a Nascent Soul with
  Qi Sense open, anyone with the Ghost Eye open, and the dead (`SoulProjection#seenOnlyBySeers`). Below that it isn't
  sent to their game at all.
- **it is crowned**: a crown of bone with ten points tipped with soul fire, cold flames rising off them, its cyan
  deeper, and its edges and halo violet. A player king keeps a man's size so it still fits through doors; its name has the
  title "[Ghost King]" above it. (A living cultivator whose ghost path has reached Core Formation can take this form
  too: see **The two paths**.)

Brought below Core Formation (the Chief Judge's gaze), it is one of the common dead again and told so. Born again at the
Wheel, it is simply alive, its cultivation kept.

Code: `GhostSystem` (`isKing`, `KING_RANK`, `watchKing`, `attack`), `Incorporeal` (`is` leaves kings out, `isGhost` and
`untouchable` keep them in), `SoulProjection#broadcastsTo`, `ModPackets#sendGhost` (the king flag); client `ClientGhosts`,
`SoulRenderer` (`drawCrown`, the shader's blue switch), `GhostKingCrown` (model layer
`defying-the-heavens:ghost_king_crown`, texture `textures/entity/ghost/king_crown.png`), `shaders/core/soul.fsh`.

### The two paths

Ordinary cultivation is the **heaven path**; the ghost methods' is the **ghost path**. Neither builds on the other, but a
cultivator who walks both is stronger than one who walks either (`TwoPaths`):

- **The body follows the higher path.** Health, attack damage, speed, armour, toughness and knockback resistance come from
  the realm and stage of whichever path stands higher (the same table either way, `TwoPaths#body`), so a living cultivator
  whose ghost path is Nascent Soul and heaven path Core Formation has a Nascent Soul body. Realm Suppression and Realm
  Suppress hold the ghost path down as they do the heaven path. Qi and its pool are still the heaven path's.
- **Harmony.** The two paths together add to that body, the more the closer they stand: **+30%** with both in the same
  realm, **+20%** a realm apart, **+10%** two apart, nothing further (`TwoPaths#HARMONY`, by realm, not stage), on top of
  Heaven's Favour. The Stats tab names the path the body follows and the harmony; on the Cultivation tab the two paths
  stand one under the other, each with its rank and its bar: the heaven path's cultivation bar (above) and the ghost path's,
  which fills through the stage it stands at, its deepest foundation's layer written on it (`GhostMethods#stageProgress`),
  amber once that foundation is perfected (the foundation to seek named on it) and gold at the last. A method's name too long
  for its line is written short ("…, 4 of 54"). The tab's details (pills, path, harmony, favour, the Upper Realm's qi, ghostly qi, yin)
  share rows as they fit, and the lines draw closer when there is much to say, so everything fits.
- **Effective cultivation.** When the harmony (or Heaven's Favour) carries the body past its own rank, the Cultivation and
  Stats tabs say what it comes to on a single path, in gold (`TwoPaths#effective`): the rank a one-path cultivator with no
  Favour would need for a body as mighty. A body that equals a stage is named as it ("Foundation Building - Late"); one past a
  stage and short of the next is **Quasi-** the next ("Foundation Building - Quasi-Late"); one past a realm's Grand Perfection
  is Quasi- the next realm ("Quasi-Core Formation"); past Four Axis Grand Perfection, beyond it. A Quasi- name carries how far
  toward it the body has come ("Quasi-Core Formation (11%)"), since past Grand Perfection the next realm is a long way off
  (`TwoPaths#effectiveLabel`). Bodies are weighed by their
  **might** (`TwoPaths#might`, the geometric mean of the health and harm bonuses). Within a realm every bonus grows alike, x1.0
  / 1.1 / 1.2 / 1.3 from Early to Grand Perfection, so the harmony is worth whole stages there; a realm's Early is 2 to 4 times
  the realm before's, which no harmony reaches:

  | Paths | Harmony | Early body | Mid | Late | Grand Perfection |
  |---|---|---|---|---|---|
  | in one realm | x1.3 | Grand Perfection | Quasi- next realm | Quasi- next realm | Quasi- next realm |
  | a realm apart | x1.2 | Late | Quasi- next realm | Quasi- next realm | Quasi- next realm |
  | two apart | x1.1 | Mid | Quasi-Grand Perfection | Quasi- next realm | Quasi- next realm |

  (The body is the higher path's, so "Mid" means the higher path at Mid.) Mid on both paths in one realm comes 5% (Qi Refining)
  to 18% (Heavenly Being) of the way to the next realm. Even Grand Perfection on both paths with full Favour (x1.3 x1.15) stops
  short of the next realm's Early: Foundation Building's comes about half way, Heavenly Being's most of it.
- **Spells of both paths**, one for each realm from Foundation Building, need **both paths at that realm** and cost **qi
  as well as ghostly qi** (below). When both paths reach a realm you're told, and its spell goes on the spell wheel.
- **Two forms, while you live.** Once your ghost path reaches Core Formation (a Ghost King's might) you have a **Ghost King
  form** beside your human one: crowned, in the dead's deeper light, as the Ghost Kings are drawn. Your **true form** is the
  form of your higher path: Four Axis on the heaven path and Nascent Soul on the ghost path, you look human; the ghost path
  higher, you look a Ghost King; level, human. The **Form Shift** ability (Abilities tab, or the ability wheel) takes the
  other form while it's on. In the Ghost King's form you are **seen as the dead are**: outside the Netherworld only by those
  who perceive souls (a Heavenly Being or above, a Nascent Soul with Qi Sense open, anyone with the Ghost Eye open, and the
  dead; `SoulProjection#seenOnlyBySeers`), so to anyone else's game you aren't there at all; in the Netherworld, by everyone.
  In all else you are still alive: you eat, mobs hunt you, arrows strike you. Below a Core Formation ghost path the living
  look human. You're told when your true form changes.
- **The dead are the dead.** Whatever the realms of their two paths, the dead are drawn as the dead: a Ghost King crowned
  (either path at Core Formation), the rest as common ghosts. They have no human form and no Form Shift; born again, their
  forms are theirs again.
- **A mortal has no ghost path.** Its meridians are closed, for the ghost path as for the heaven path, until a **Marrow
  Cleansing Elixir** opens them: alive or dead, it can't take up a ghost method (steles, slips and teachers say so) or
  cultivate one, and any it knows lie dormant (no ghost path, spells, abilities or harmony). Only the living can drink the
  elixir, so a mortal's ghost must be born again at the Wheel first.

| Spell of both paths | Both paths at | Qi | Ghostly qi | Cooldown | What it does |
|---|---|---|---|---|---|
| **Yin-Yang Palm** | Foundation Building | 20 | 15 | 6 s | a palm of light and shadow strikes the first being within 16 blocks: 5 hearts of harm and thrown back; a quarter of the harm mends you |
| **Taiji Shield** | Core Formation | 60 | 25 | 30 s | light and dark turn round you for 10 s: arrows, fireballs and anything else shot at you from within 3.5 blocks are turned back, the shooter's no longer; Resistance I |
| **Life and Death Seal** | Nascent Soul | 150 | 40 | 20 s | the being you look at (32 blocks) is sealed for 8 s: it loses 2 hearts a second and half of it mends you; slowed while sealed |
| **Heaven and Hell Pillar** | Heavenly Being | 350 | 60 | 25 s | where you look (32 blocks) heaven's light falls and hell's fire rises: everything within 5 blocks takes 15 hearts of harm, is thrown up and set burning with ghost fire |
| **Primordial Chaos** | Four Axis | 800 | 120 | 2 min | the two paths made one: everything within 16 blocks is dragged in and takes 25 hearts of harm; a tenth of all of it mends you, and Strength II for 10 s |

Their harm is the dead's kind (ghostly), so it reaches the living and the dead alike. Icons: the Palm an open hand, half
ivory and half cyan; the Shield a taiji in a gold rim; the Seal a red seal crossed light and dark; the Pillar a gold beam
falling into cold fire; Chaos two arms, light and dark, wound round a black heart; Form Shift a face half a man's and half
a crowned ghost's.

Code: `TwoPaths` (the paths, `body`, `harmony`, `bothReach`, the forms and `look`, and the once-a-second watch that tells
of new forms and spells), `CultivationStats#apply`, `Spell` (`isDual`, `dualRealm`, `qiCost`; appended), `Spells` (`palm`,
`taiji` and `turnBack`, `seal` and `drawOff`, `pillar`, `chaos`, `tick`; `PALM_*`, `TAIJI_*`, `SEAL_*`, `PILLAR_*`,
`CHAOS_*`), `Ability#FORM_SHIFT`, `ModPackets#sendGhost` (the look); client `ClientGhosts#drawnAsLight`/`crowned`,
`SoulRenderer`, `mixin/SoulRenderDispatcherMixin`, `mixin/PlayerRendererMixin`, `CultivationScreen` (Stats, Spells).

### Ghost abilities and spells

**Abilities** are switched on and left on, from the Abilities tab or the ability wheel (hold Z), like any other; these
four are paid from the ghostly qi pool, a little each second.

| Ability | From | What it does | Ghostly qi a second |
|---|---|---|---|
| **Ghost Eye** | Breathing Art 3 | ghosts, ghost cultivators, Hungry Ghosts and Method Steles glow through walls within 32 blocks; outside the Netherworld, projected souls are seen as a Nascent Soul with Qi Sense open sees them | 0.5 (`GHOST_EYE_COST`) |
| **Netherworld Veil** | Breathing Art 6 | the living take the Netherworld's yin at half the rate | 1 (`VEIL_COST`) |
| **Yin Wind Steps** | Soul Steps 1 | 20% swifter, and falls do no harm | 1 (`YIN_WIND_COST`, `YIN_WIND_SPEED`) |
| **Ghost Flight** | Soul Steps 4, ghosts only | a ghost flies as fast as on qi instead of at a drift, never running dry in the Netherworld | 2 (`GHOST_FLIGHT_COST`) |

**Spells** are cast at will, and live on a **spell wheel** of their own, never the ability wheel. **Hold V** (rebindable:
"Spell Wheel (hold)") and an eight-slot wheel opens: **click** a slot to ready its spell, **right-click** to cast it at
once. **C** ("Cast Readied Spell") casts the readied one; its name, and the seconds left while it cools, sit under the
bars top left. Each costs ghostly qi (the spells of both paths, above, qi too) and has a cooldown; none can be cast on the
Soul River or while dispersed. A
spell you learn goes into the first empty slot. The cultivation menu's **Spells** tab lists every spell (cost, cooldown,
the layer it needs, whether it's ready), and its **Arrange Wheel** button lays the wheel out as the ability wheel's
editor does. A mortal, alive or dead, can't shape one: its meridians are closed.

| Spell | From | Ghostly qi | Cooldown | What it does |
|---|---|---|---|---|
| **Ghost Fire** | Fire Scripture 1 | 3, then 3 a burning blow | 2 s | the hands take cold fire: the next 3 blows within 20 s set what they strike burning for 4 s (half a heart a second, three times that on the dead); from layer 7 the fire leaps to a second foe within 4 blocks |
| **Ghost Fire Bolt** | Fire Scripture 4 | 8 | 1.5 s | a bolt of it, thrown up to 24 blocks: 3 hearts of ghostly harm, and it sets the struck burning |
| **Soul Dash** | Soul Steps 2 | 6 | 3 s | a rush forward on the yin wind, about 8 blocks; no fall harm for 2 s after |
| **Ghostly Wail** | Breathing Art 5 | 15 | 12 s | a cry of the dead: everything within 8 blocks is driven back, slowed and weakened for 5 s, and the dead among them (ghosts, ghost cultivators, Hungry Ghosts) take 2 hearts |
| **Yellow Springs Tide** | Marrow Art 12 | 12 | 8 s | a surge of the springs' yellow water 7 blocks ahead: all it reaches is driven back and slowed (Slowness II, 4 s), and the dead take 3 hearts |
| **Marrow Ward** | Marrow Art 16 | 20 | 30 s | your bones hold like iron for 10 s: Resistance II and 4 hearts of absorption |
| **Ghost Core Burst** | Ghost Core Canon 21 | 25 | 10 s | the core flares: everything within 5 blocks takes 4 hearts of ghostly harm, is thrown back and set burning with ghost fire |
| **King's Decree** | Ghost Core Canon 25 | 30 | 40 s | every foe within 16 blocks (monsters, and whatever has you as its target) is weakened, slowed and glowing for 8 s |
| **Soul Rend** | Nine Nether Soul Sutra 30 | 35 | 6 s | a beam 32 blocks long: every being in its line takes 8 hearts of ghostly harm |
| **Nine Nether Step** | Nine Nether Soul Sutra 34 | 30 | 8 s | a step through the dark to where you look, up to 24 blocks, short of any wall; no fall harm for 5 s after |
| **Yama's Verdict** | Yama's Judgement 39 | 50 | 12 s | the being you look at (32 blocks) takes 10 hearts of ghostly harm, 20 if it is below half its health |
| **Chains of Hell** | Yama's Judgement 43 | 60 | 30 s | every foe within 12 blocks is dragged to you and held fast (Slowness VII, Weakness II) for 3 s |
| **Wheel of Samsara** | Six Paths Canon 48 | 80 | 2 min | your health made whole, harmful effects gone, and half your yin corruption with them |
| **Six Paths Annihilation** | Six Paths Canon 52 | 150 | 90 s | six pillars of cold fire rise round you: everything within 12 blocks takes 20 hearts of ghostly harm and is hurled away |

None of a caster's spells touch the caster, spectators or the caster's own tamed creatures; harm to another player needs
PvP on. The numbers are the `Spell` enum (layer, cost, cooldown) and `Spells` (`GHOST_FIRE_*`, `BOLT_*`, `DASH_*`, `WAIL_*`,
`TIDE_*`, `WARD_TICKS`, `BURST_*`, `DECREE_*`, `REND_*`, `STEP_*`, `VERDICT_*`, `CHAINS_*`, `ANNIHILATION_*`).

**Icons** (32x32, `textures/gui/ability/<id>.png` and `textures/gui/spell/<id>.png`): Ghost Eye a slit-pupilled cyan
eye, Netherworld Veil a dark hood with two cold eyes, Yin Wind Steps a pale footprint on curling wind, Ghost Flight a
ghost rising on trails of light; Ghost Fire a cold flame, Ghost Fire Bolt a ball of it streaking up and right, Soul Dash
a ghost darting with speed lines behind, Ghostly Wail a ghost's open-mouthed cry ringing out both sides.

Code: `Ability` (the four, appended), `Spell`, `Spells` (casting, the effects, packets), `GhostFireEffect`,
`NetherworldState` (`setSpellSlot`, `addToWheel`; NBT "Netherworld"); client `SpellWheelScreen`, `GhostEye`,
`CultivationScreen` (the Spells tab and its editor), `ModKeybinds` (`SPELL_WHEEL`, `CAST_SPELL`), `NetherworldHud`.

### Who lives there

**Ghost cultivators** are the Netherworld's rogues: cultivators in every way that matters (rank, path, regard, talk,
spars, daily life) but dead, drawn as cyan light in their cultivator skins. They turn up there instead of living rogues,
Qi Refining Early to Foundation Building Grand Perfection (`WANDERER_CAP`; the stronger dead keep palaces, below), never
mortals; they join no sect and make no journeys. They speak as the
dead do, tell you how they died if you ask about them, and from Foundation Building on know a ghost method they can teach
(their realm's foundation four times in ten, else one of the first three methods); **Ghost Elders** (Foundation Building
and up) sell its Ghost Jade Slip (60 low-grade stones, 180 for the Yellow Springs Marrow Art), and one struck down leaves a
foundation's slip 15% of the time. The dead fight back against whatever strikes them, Hungry Ghosts included (a king's
court as one). A ghost talks with cultivators living and dead (in the Netherworld everyone sees the dead, living NPCs too:
`CultivatorNpc#senses`) and, with the spirit stones and slips it carries, **trades and gives gifts** as the living do, by the
same standing (one far above it greets, hears and takes a gift, but doesn't trade); it makes no demands. The dead sell Yin
Jade, Other-Shore Flowers and Yang Fire Talismans, to the living and the dead alike. Blows reach
them as they reach any of the dead. Struck down they scatter in sparks, now and then leaving a jade slip (8%), Yin Jade
(40%) or an Other-Shore Flower (15%). An NPC cultivator who ascends from the Nether goes to its rift and comes out at
the Ghost Gate as one of them (a Ghost King, if it was Core Formation or above).

**Ghost Kings and their palaces.** The Netherworld's kings (Core Formation, Nascent Soul, Heavenly Being, now and then Four
Axis) rule from **ghost
palaces** in the Fields of the Other Shore and the Bone Wastes: one try in every 24 x 24 chunks, about one in five of which
finds land level enough, over no chasm and at least 256 blocks from the Gate. No lake, spring, pool, dead tree, bone pile,
soul fire or stele is placed on or beside one (`NoGhostPalaceFilter`). A palace is a platform of black stone 41 blocks square,
levelled into the ground (cut away above, filled below, on pillars of basalt and bone over a deep drop), walled round, with
bone towers at its corners and a gate of bone pillars hung with soul lanterns. Inside: braziers of soul fire and beds of red
spider lilies either side of the way in, and at the back the hall, on a raised floor, red columns under gold capitals,
black walls barred with iron and a double roof of deepslate tiles with upturned eaves. A red carpet runs up the middle to a
dais and the throne set in the back wall. The king stands before its throne, half again a man's height and crowned, and
keeps its realm's foundation: a Core Formation king (most palaces) the Ghost Core Canon, a Nascent Soul king (27%) the Nine
Nether Soul Sutra, a Heavenly Being king (13%) Yama's Judgement Scripture, a Four Axis king (5%: `GhostPalacePiece`
`FOUR_AXIS_KING` and the rest, decided by where the palace stands) the Six Paths Reincarnation Canon. It teaches it and
sells its slip to one it is Warm to, and leaves its slip when it falls. Six
**Ghost Retainers** (Foundation Building) keep the hall and the gate, all of one path with their king, and allies: a court
never turns on itself, and no blow of one of its own lands on it. The king greets the dead and warns the living out of its
palace. A court leaves passers-by alone, but strike the king or any retainer and **the whole court** (everyone within 40
blocks) turns on you. Slay the king and its throne stands empty, until a ghost cultivator who rises to Core
Formation (`GhostCultivatorNpc#rise`) goes to claim the nearest empty one within 768 blocks (`GhostPalaces`). With none in
reach, it roams, a king without a court.

**A palace's treasury** (`PalaceTreasury`, `GhostPalacePiece#treasury`) stands in the courtyard's back corner beside the hall:
a vault of black stone with red columns at its corners, iron-barred windows, a carved lintel over its door and a low tiled
roof, four chests inside of the dead's wealth (`chests/ghost_palace_treasury`: middling and high spirit stones, Yin Jade,
old Other-Shore Flowers and Netherworld Lotus, Yin Gathering Pills, gold, and Ghost Jade Slips, the first three methods
most, now and then the Yellow Springs Marrow Art or the Ghost Core Canon). **The king's seal** holds the chests shut and
unbreakable while the king lives and stands in its palace, as a sect's elders keep theirs: one a full realm above the king
(by the stronger of their two paths) breaks it with a touch and opens it with the next, and it stays broken five minutes
(`SEAL_RECAST`; a Four Axis king's can't be broken); a king slain, or its throne empty, leaves it open. The dead may open it
too. Laying hands on it is **theft**, open or not: the king and its retainers turn on you, and think 60 worse of you once a
day.

**The Gate's keepers**, three ghost cultivators who never leave their posts, each **three times a man's height** (5.4
blocks tall and 1.8 across, their eyes, reach and stride to match: `GhostCultivatorNpc.KEEPER_SCALE`):

- **Ox-Head** and **Horse-Face**, Four Axis Grand Perfection, guard the Gate: they greet the dead, warn the living about
  the yin, and fight whoever attacks them. One slain comes back to its post three days later
  (`GhostGate.OFFICIAL_RETURNS_AFTER`). They wear their beasts' heads in place of a man's (below): Ox-Head a bull's,
  broad and dark, glaring under a heavy brow, a gold ring through the muzzle and great horns sweeping out, up and
  forward; Horse-Face a horse's, its long face sloping to a broad nose, a white blaze down it, ears pricked and a black
  mane down the neck. Besides the Yin Jade and herbs every ghost sells, they sell the living Yang Preservation Pills, a
  Peach Wood Sword and Ghost-Quelling Talismans (two for 12), the means to keep the restless dead in order.
- **Meng Po**, Keeper of the Naihe Bridge, stands in her hall with her pot at her side and never fights; ask her about
  the soup. Her hall, built to her height on the bank west of the Gate, is raised a step off the ground (stairs up along
  its east side from the plaza and the Gate's way, and from the north), railed over the water, with six red columns
  under gold capitals and red beams, a screen at its back, and a double roof of deepslate tiles 17 blocks high, its
  eaves turned up at the corners and a lantern on the ridge. A Gate built with one of the old pavilions (small, against
  the Gate's west pillar) has it replaced by the hall the next time the server starts: the pavilion taken away, the
  pillar made whole, and Meng Po set down at her new post.

Their plates (name, title, health and path) are 1.7 times the size of a man's, to be read from as far off as they stand,
and float clear over the beasts' horns and ears (`CultivatorNpcRenderer#plateSize`, `GhostlyRenderers#plateHeight`).

**Hungry Ghosts** are the dead who starved in life: gaunt, translucent cyan, about Qi Refining strength (8 hearts),
in packs of two to four, in any light, most of all in the Bone Wastes; never on the Gate's ground, nor on Peaceful. They go for the
living first (half a heart a blow, but each blow drives 5% yin in), then for ghosts (1½ hearts, ghostly), and with no
player about for the Netherworld's cultivators (ghost cultivators, a king's court, a cultivator newly come up the Soul
River; never the Gate's keepers), who fight back. A ghost who
disperses one takes 5 ghostly qi from it. They drop bones, and Yin Jade one time in ten.

| Hungry Ghosts | Value | Constant (`HungryGhost`) |
|---|---|---|
| Health | 16 | `HEALTH` |
| Blows | 1 on the living (+5% yin), 3 on the dead | `ON_THE_LIVING`, `YIN_PER_BLOW`, `ON_THE_DEAD` |
| Against cultivator NPCs | its blows divided by 1 + the cultivator's realm (Qi Refining 0, Foundation Building 1...); theirs on it times 1 + half that, so a Foundation Building ghost fends off a pack of three | `realmOver` |
| A ghost's reward | 5 ghostly qi | `QI_REWARD` |
| Spawning | weight 10 in every Netherworld biome; 30% of tries go ahead in the Bone Wastes, 6% elsewhere; at most 8 within 48 blocks | `ModBiomes#hungryGhosts`, `SPAWN_CHANCE_BONE_WASTES`, `SPAWN_CHANCE`, `CROWD_CAP`, `CROWD` |

Code: `GhostCultivatorNpc` (the dead as NPCs, the keepers' and the courts' roles: `becomeKing`, `becomeCourtier`, `rise`,
`rallyCourt`), `GhostPalaceStructure`, `GhostPalacePiece` (the palace, built in code; data `worldgen/structure/ghost_palace.json`,
`worldgen/structure_set/ghost_palaces.json`, biome tag `has_structure/ghost_palace`), `GhostPalaces` (the thrones, saved
data), `Ghostly` (what the dead's kind share), `HungryGhost`,
`NpcSpawner` (the dead in the Netherworld), `NpcTravel` (ascending from the Nether), `NpcLines` (the ghost voice and how
they died), `TalkSystem` (the soup, teaching), `CultivatorTrades#ghostGoods`, `CultivatorLoot`; client `GhostlyRenderers`,
`SoulRenderer#drawDead`. Loot tables `entities/ghost_cultivator.json`, `entities/hungry_ghost.json`,
`chests/ghost_cultivator.json` are hand-written in `src/main/resources`.

### Beasts' heads

A beast's head worn on a man's body is its own little model, built from cubes as vanilla's models are, and drawn in
place of the human head and its hat layer: it turns and nods with the head it replaces (its pivot is the neck), so any
humanoid body can wear one. Ox-Head and Horse-Face are the first; the same pieces are there for whatever comes to wear
one later (a demon race's tiger or snake head, say).

- **The list** is `BeastHead` (shared by both sides, so the server can name one and sync it): `OX`, `HORSE`. Saved and
  synced by id; add new heads at the end.
- **The models** are in client `BeastHeadModels`: each a model layer, `defying-the-heavens:beast_head/<id>`, with its mesh
  (in the model's pixels: the human head is the box from (-4, -8, -4) to (4, 0, 4) about the neck, the face toward -z, -x
  its right), and a 64 x 64 texture, `textures/entity/beast_head/<id>.png`. Each mesh's comment lists its boxes' texture
  corners. A head also names its **brow**, where a talisman sealed to it hangs (on the long face of a horse it lies along
  the top of the face).
- **Wearing one**: hide the body model's `head` and `hat`, call the head's `follow(model.head)` after the body's
  `setupAnim`, and draw it with the same transforms (and the same render type) as the body, in its own texture.
  `SoulRenderer#drawBody` does so for the dead (`drawDead` picks the head from `GhostCultivatorNpc.Role#beastHead`); a living
  wearer would do the same from a render layer with an ordinary entity render type.
- **A new head**: an entry in `BeastHead`, its mesh in `BeastHeadModels#definition` and its brow in `#brow`, and its texture.
  The textures here were drawn by a small generator, box by box (fur fill, then eyes, nose, blaze and the like).

### Herbs, Yin Jade and the exorcist's tools

| Thing | Where / how | What for |
|---|---|---|
| **Other-Shore Flower** (bulbs) | an aging herb, like ginseng, glowing faintly; roots in soul soil and soul sand (planted, any soil). A patch in about every 6 chunks of the Fields of the Other Shore, every 40 of the Yellow Springs and Bone Wastes, every 160 of a Soul Sand Valley | the Yin Gathering Pill |
| **Netherworld Lotus** (seeds) | an aging herb on still water, like the Spirit Lotus; a patch in about every 8 chunks of the Yellow Springs | the Yin Gathering Pill |
| **Yin Jade Ore** | veins in the Netherworld's stone, 16 a chunk on the Nine Nether Peaks and 4 in its other biomes, and one vein in about every 4 chunks of a Soul Sand Valley; an iron pickaxe, more with Fortune, the ore with Silk Touch | **Yin Jade**: the Ghost-Quelling Talisman; a trade good the dead value; 9 make a Block of Yin Jade |
| **Red Spider Lily** | an ordinary flower carpeting the Fields of the Other Shore | decoration |
| **Peach Wood Sword** | 2 cherry planks, a stick and a Peach Pit (shapeless) | a stone sword to the living; on the dead every blow lands in full, whoever swings it, and twice as hard (`PeachWoodSwordItem.ON_THE_DEAD`) |
| **Ghost-Quelling Talisman** | 2 paper, redstone (the cinnabar) and Yin Jade with the Inscription Brush (shapeless; makes 2, the brush comes back a use more worn); the Gate's wardens sell them | thrown (right-click, like a snowball): see below |
| **Yang Preservation Pill** | Alchemy Cauldron, 30 s: Ginseng, blaze powder, glowstone dust, a honey bottle | holds yin corruption still (above) |
| **Yin Gathering Pill** | Alchemy Cauldron, 30 s: an Other-Shore Flower, a Netherworld Lotus, a ghast tear | the ghost methods go +25% faster per point of potency for 5 minutes (`PillItem.YIN_BOOST_PER_POTENCY`); only for one who knows a method |

**The Ghost-Quelling Talisman.** Thrown, it flies past the living (and through a projected soul) and strikes only the
dead: a player's ghost, a ghost cultivator, a Hungry Ghost. It deals **6** of the dead's own harm, which lands in full, and
binds the one it strikes where it stands for **5 seconds** (the Quelled effect): no walking, jumping, Ghost Flight or Soul
Dash, and a bound creature is held still, only falling; it can still strike what is in reach. The talisman shows sealed
to its brow, yellow paper in its own colours over the dead's cyan light (on a beast's head too), and gold sparks circle
it. A ghost can't throw one (the dead use no items), and it hurts another player's ghost only where PvP is on. Its
numbers are `GhostQuellingTalismanItem.DAMAGE` and `BIND_TICKS`.

Code: `GhostQuellingTalismanItem` (the throw, the strike, `quell`), `GhostQuellingTalismanEntity` (only the dead stop it),
`QuelledEffect` / `ModEffects.QUELLED`, `SoulProjectileMixin` (lets it reach a ghost, as no other projectile does),
`SoulLivingEntityMixin` (no jumping), `QiFlight` and `Spells` (no flight or dash); packet `quelled`; client `ClientQuelled`,
`SoulRenderer#drawTalisman` (the paper on the brow is the item's own picture); recipe `recipes/ghost_quelling_talisman.json`.

Rates are the `*_CHUNKS_*` and `YIN_JADE_*` constants in `ModPlacedFeatures` (re-run `runDatagen` after changing them).
Code: `OtherShoreFlowerBlock`, `ModBlocks#NETHERWORLD_LOTUS`, `GinsengFeature` (`Ground.SOUL`), `PeachWoodSwordItem`,
`PillItem` (`YANG_PRESERVATION`, `YIN_GATHERING`), `AlchemyRecipes`, `ModEffects` (`YIN_GATHERING`, `YANG_PRESERVATION`);
the herbs' blockstates and models are hand-written in `src/main/resources`.

### Commands (op)

All act on yourself.

```
/netherworld enter          arrive alive at the Ghost Gate (skipping the river)
/netherworld gate           the same, from anywhere
/netherworld leave          down the Soul River to the Nether
/netherworld up             onto the Soul River from its start, going up (clears the rift's cooldown)
/netherworld ghost          become a ghost where you stand
/netherworld revive         return to life, as at the Wheel
/netherworld corruption <percent>   set your yin corruption
/method learn <id> [layer]  a ghost method at that layer (to 54; without one, as a stele teaches it), progress reset
/method progress <seconds>  that many seconds of baseline meditation on the practised method (the outside cap ignored)
/method forget <id>         as if it had never been learned
/gamerule netherworldAfterDeath false   death as vanilla's, no ghosts
```

Method ids: `netherworld_breathing`, `ghost_fire_scripture`, `wandering_soul_steps`. Code: `NetherworldCommand`.

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

**Sects** are open villages spread over wide lawns, with no wall: the barrier closes round the buildings and is their
wall, while the grounds, the ways and the gates reach on beyond it. A paifang gate stands across the paved way in, well
outside the barrier, its **gate guards** (the strongest Outer Disciples, "[Gate Guard]" on their nameplates) standing on
the road before it; the way, lined with stone lanterns, runs up the middle to the round **Formation plaza** with the
Formation Core at its centre, and on to the steps of the **main hall**; a second way crosses the plaza and leaves on
either side through a torii, guarded too. The **main hall** stands on a terrace with flag poles at its corners, stone
guardians at the foot of its steps and two great braziers sending up smoke; inside, banners hang on the pillars over a
bordered carpet to the Sect Master's mat on its dais, between lanterns. The **Meditation Hall** of mats,
where the disciples live and meditate, and the **Treasury** face the way in across lawns from either side, a **pagoda**
stands beside the main hall (every storey but the top is the sect's library, shelves and candles and lecterns, with the
library chest on the ground floor; a ladder climbs to a meditation chamber at the top) with herb gardens (ginseng,
Spirit Ginseng, fruit trees) by it, and the **elders' houses** (pavilions with two eaves, one per elder) stand apart
round the grounds, each with an earthen footpath to its door. Out on the lawns rises the **Altar of Heaven**, round
terraces of worked stone after Beijing's Circular Mound (one, two or three by the sect's size, built Modest, Grand and
Magnificent), with a Heaven Altar on top where the members perform the Ritual to the Heavens, and stairs up the front
a step at a time. Not far off lies the **training ground**: a paved yard with a sparring ring set into the floor, straw
training dummies across the back with a drill spot before each, sharpening stones and barrels down the sides, stone
lanterns at the corners and banners at the way in; the disciples spar and practise their sword forms there. Cherry trees and clumps of
bamboo grow on the open grass between. Buildings stand on stone bases, with plain plastered walls between their pillars
(set evenly and mirrored about the door, which always stands in the middle of a bay), under tiled roofs whose
ridges, eaves and upturned corners are picked out in the trim colour; the colours go by the sect's path: grey stone, red
pillars and slate roofs trimmed in bronze for the righteous, Blue Spirit Wood and teal roofs for the neutral, black stone,
crimson pillars and dark nether-brick roofs trimmed in red with soul lanterns for the demonic.

- Three sizes, for 10-12, 13-16 and 17-20 members. They only generate on fairly level, mostly dry land (at most 20
  blocks between the highest and lowest ground under the grounds, 24 in the Upper Realm; the grounds are levelled, a
  pond or a stream filled in), never in the Nether or the End. The levelled ground keeps the land's own floor, column by
  column (podzol and coarse dirt in an old taiga, snow on the snowy slopes, sand, stone, grass on the plains), so a sect
  sits in its land rather than on a lawn; only a filled-in pond is grassed over. (Chunks generated before this keep the
  old grass.)
  - **Overworld** (`defying-the-heavens:sect`): plains, meadows, forests, taiga, snowy plains, savanna, cherry groves,
    bamboo and sparse jungle. One attempt per 32x32 chunks, at least 16 chunks apart, never within 10 chunks of a village
    (`worldgen/structure_set/sects.json`).
  - **Upper Realm** (`defying-the-heavens:sect_upper_realm`): islands of Spirit Forest, Peach Blossom Sanctuary, Dense Qi
    Peaks and Thunder Peaks wide enough to hold one. The realm is mostly open sky, so a sect doesn't need its island right
    at its site: there is one site in every 256 x 256-block cell (`worldgen/structure_set/sects_upper_realm.json`: spacing
    16, separation 15, a start chunk every 16 chunks exactly), and the sect stands wherever within 48 blocks of the
    cell's middle an island will hold it, taking a smaller size if the island is too small for the one rolled. Its
    grounds never leave its cell, so two never overlap. To keep that cheap, the island masks of the terrain
    (`ModNoiseSettings.islandMask`, four 2D noise samples) pick the two most promising spots per size before the
    generator is asked the heights (~10 ms a column, most of it empty sky): about 85 ms a site on the worldgen threads.
    Surveyed on two seeds, within 2,048 blocks of the rift: 21 and 15 sects, against 1 and 6 when a sect could only
    stand at its site. Their masters are Heavenly Beings or Four Axis cultivators.
  - The grounds are 133, 145 and 153 blocks across; the buildings stand within 52, 58 and 62 blocks of the plaza (the
    barrier reaches 2 past them, never more than 64), the gates 8 further out (`Size` in `SectStructure.java`). Halls
    stand 8 blocks back from the ways, and the elders' houses have 7-9 blocks of lawn between them.
  - The land just beyond the gates and at the far corners of the grounds may rise at most 8 blocks above them
    (`MAX_EDGE_RISE`): the grounds are levelled out to there, so higher land would stand as a cliff across the way out.
  - Tuning: `MAX_RELIEF`, `MAX_RELIEF_UPPER`, `MAX_EDGE_RISE`, `MAX_WET`, and for the Upper Realm `ISLAND_SEARCH`,
    `ISLAND_MIN` and `ISLAND_TRIES` in `SectStructure.java`; `spacing` in the structure sets (the Upper Realm's must keep a
    start chunk every 16 chunks exactly, or `ISLAND_SEARCH` could make two sects overlap: a game test checks it).
- Worldgen that runs after structures can't spoil the grounds: no lake (vanilla lava lakes, the Upper Realm's spring
  basins) forms where it would reach into a sect (`mixin/LakeFeatureMixin`), no wild tree, bamboo, huge mushroom, mossy
  boulder or jungle vine grows on the grounds or within 4 blocks of them (`mixin/SectClearingMixin`; saplings planted
  later grow as usual), and the sects build with nothing a water or lava spring can open in (the plaster is smooth quartz, not
  calcite; checked by a game test).
- The layout is planned in code (`SectStructure`, `SectLayout`, `SectPiece`), not from jigsaw templates, so it fits its
  population, every door gets a footpath, and each building adapts its height to the ground. A game test lays out
  sects of every size and facing and checks there is a house for every elder, a home (an elder's house or a meditation
  mat) for every member but the master, no two buildings overlap, all of them are inside the barrier, and the gates and
  the six gate-guard posts are outside it.
- **Gate guards** (`Sect#guardPost`): the strongest Outer Disciples, in seniority order, take the posts two at the gate,
  then one at each torii, then a second at each; the rest patrol as before. They stand on the road outside their gate
  watching it, outside the barrier, where visitors meet them (a future pass into the sect would be bought from them). One
  raised above the Outer Disciples (its seniors fallen, say, while it was fighting) is a gate guard no longer: its nameplate
  shows its new title at once, and it leaves the gate for its new station.
- The earlier layouts are in the git history: the first walled, fully paved compounds (up to 56206f9) and another open
  village design (ab0301b).
- The Treasury holds Spirit Pedestals bearing old Cultivation Fruit and ginseng and Spirit Ginseng, and loot chests;
  elders' houses and the library have their own (`loot_tables/chests/sect_*.json`). Taking from a sect is theft: it
  brings the sect down on you.
- Players can use the mats in the Meditation Hall.

### Protective formations

A **Formation Core** (an altar of ritual bronze, lacquer and jade with an azure crystal hovering over it) raises a
spherical **barrier** around itself. The barrier is invisible like a vanilla barrier, but lets the core's owner (or its
sect) walk through, and can be broken only by a cultivator at least as strong as the formation: at an equal rank a block
takes 5 seconds, half that for every rank stronger (`Formations.breakTicks`). Breaking the core, or the core running out
of qi, takes the whole barrier down at once.

No monster spawns inside a raised barrier, a sect's or a secluded cultivator's Concealment Barrier: zombies, skeletons,
spiders, creepers, pillager patrols and the rest pass over it (`mixin/NaturalSpawnerMixin`, `mixin/PatrolSpawnerMixin`).
Any that get in another way (a phantom, an enderman teleporting, one from a monster spawner) are swept out every half
second: a wild one vanishes in a puff of smoke, and one that has to stay in
the world (carrying something it picked up, say) is set down on the ground just outside. A monster that has been given a
name with a name tag stays where it is, so a zombie villager can be cured inside your own barrier. Bosses are left alone,
and animals and other peaceful mobs are never touched (`Formations.purge`).

The barrier's shell has no openings (`Formations.fillFor`):
- **Water and lava.** Still water or lava on the shell is closed by barrier blocks that hold it: the water still looks and
  flows as before, and comes back when the barrier comes down (`SectBarrierBlock.FLUID`). Flowing water or lava is shut out.
- **Anything that leaves a gap** (torches, fences, walls, panes, doors, trapdoors, carpets, rails, chests, flowers, sugar
  cane, kelp, crops and so on) is replaced by the barrier. Whatever was built or placed drops as an item (whole: a door, a
  bed, a chest with its contents); wild plants and snow simply give way. Solid blocks (stone, logs, leaves, stairs, slabs,
  anything filling its whole footprint at least half a block high) are kept as part of the wall, and weaker cultivators
  can't dig them, as before. A sect's own buildings are never touched where they reach the dome (a tall pagoda's spire).
- **Seals** are kept, so an array line can cross the shell, and a seal on the shell is solid to anyone the barrier keeps out.

Only chorus fruit, a Nether portal that opens inside, or a respawn point inside can still put someone past the shell. A
player too weak to break the barrier who ends up inside that way is set down just outside within half a second
(`Formations.guard`). The owner, the players they trust, anyone strong enough to break it, and players in creative or
spectator stay where they are. Rogue cultivators never spawn inside a barrier either.

A barrier is raised, mended and watched over only while its land ticks (a player near enough, or the land held for a
moment: `NpcLedger`). Land that is merely loaded is left alone, so it can unload: reading its blocks every tick used to
hold it loaded for good, freezing the cultivators in it.

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
- **Trusted cultivators.** The screen's **Trusted...** page lists up to 16 players who pass the barrier as
  its owner does (and may dig the ground it runs through). Type a name and press **Trust** (or Enter) to add one: anyone
  online, or anyone who has played on the server before. Press the cross beside a name to strike them off.
- **A fallen sect's core.** When a sect's last member dies, its core answers to no one: the first player to right-click
  it claims it, as if they had placed it. Its barrier stands until then, as strong as the last Sect Master.
- Recipes: Formation Core (gold ingot, amethyst shard, gold ingot / lapis block, diamond, lapis block / 3 polished
  deepslate); Inscription Brush (string top right, lapis in the middle, bamboo bottom left).
- Code: `Formation`, `Formations`, `FormationData`, `FormationCoreBlock(Entity)`, `SectBarrierBlock`, `SealBlock(Entity)`,
  `InscriptionBrushItem`, `QiVeinBlock`, `QiVeinFeature`; client `FormationCoreScreen`, `FormationCoreRenderer`,
  `SealRenderer`, `ClientFormations` (under Qi Sense a barrier shows as a continuous purple membrane, brighter at its
  edges, with soft light drifting over it, and tints everything strongly purple while you stand inside it; a
  Concealment Barrier looks the same in green).

### Sect members

A sect is founded with 10-20 members the first time its core ticks (`SectManager.found`), and it needn't wait for a
player to come by (see **Sects come into being unseen** below). Titles go by strength and are
recalculated whenever someone dies, leaves or breaks through: the strongest is **Sect Master**, then about 10% **Grand
Elders** (from 6 members), 20% **Elders**, 30% **Inner Disciples**, and the rest **Outer Disciples**. In the lower realms
the master is between Core Formation and Nascent Soul Grand Perfection and most members sit near Qi Refining; in the Upper
Realm, rich in qi, the master is a Heavenly Being or Four Axis and the members run from Core Formation up, leaning toward
the top.

| Title | Duty |
| --- | --- |
| Outer Disciple | The strongest (up to six) keep the gates as gate guards (by day; the next strongest by night, where there are enough); the rest patrol the grounds and up to 30 blocks outside the barrier |
| Inner Disciple | Meditates in the hall, walks the courtyards (not in the rain) |
| Elder, Grand Elder | Guards the Treasury and the Formation Core; otherwise fights only when disciples nearby are in a fight or someone breaks into the Treasury or at the core |
| Sect Master | Meditates on the dais; otherwise fights only when the barrier is shattered or an elder is killed |

- **The treasury is sealed while its elders keep it.** As long as an Elder or a Grand Elder stands anywhere in the
  territory, the treasury's chests and pedestals won't open, nothing in it can be broken (a chest broken would spill all
  it holds), and no blast breaks into it ("A seal of Core Formation holds it shut..."). Trying still raises the cry of
  thief, and the whole sect comes. Its keepers must be beaten, or drawn out of the territory, first; then it's anyone's
  (`SectManager.treasurySeal`). Or it can be broken: the seal is of its strongest keeper's realm, and a cultivator a full
  realm above that (a Nascent Soul against Core Formation keepers, say; never a mortal) right-clicks a chest or pedestal
  to shatter it, and right-clicks again to open it. A broken seal stays broken for 5 minutes, then its keepers, if any
  are still about, cast it anew.
- A barrier block broken (or landed on) shows pale shards of light-blue glass, not the red barrier sign.
- **A struck barrier ripples and rings.** What stops you is a shell of invisible barrier blocks round the core, but
  what's struck is the true sphere they stand for, as Qi Sense shows it: a blow, an arrow or anything else thrown at
  it (a snowball, a trident, a fireball), or a cell broken, and for a moment the whole sphere shows faintly while bright
  rings run out over its surface from where it was hit and fade (a core's barrier purple, a concealment green), seen
  by everyone within 160 blocks, Qi Sense or not, with a deep crystal resonance and a chime (a breach the louder and
  wider) (`Formations.struck`, `BarrierRipples` on the client).

- **The whole sect turns out** (every member in its territory, the elders and the Sect Master too) when a player or a
  cultivator attacks any member anywhere in the territory (the barrier plus 30 blocks, so the gate guards too), or when
  someone breaks a block of the barrier to force a way in. Striking the barrier without breaking it, trespassing, and
  monsters only rouse the members near by, as above (`SectManager.alert`).
- Members below Heavenly Being stay within the barrier plus 30 blocks, leaving only to chase someone (anywhere in the
  territory and up to 48 blocks past it). One you strike keeps after you just as far (not only while you are within 32
  blocks or in its sight). Chasing a flier, a member takes off only under the open sky (from inside a hall, or under
  thick leaves, it walks out first), climbs over whatever it flies into, and drops out of the air when its qi runs dry;
  it can't take off again until its qi is back over 40%.
- A member killed by a player is never replaced, and the sect is smaller for good. One who dies otherwise, or leaves, is
  replaced after 2-4 days by a new Outer Disciple at the realm's lowest rank (Qi Refining Early below, Core Formation
  Early in the Upper Realm), unless a newcomer from below has taken the slot (see **Coming up**). When the last member
  dies, the sect is extinct and never comes back.
- Cultivators cultivate ten times slower than players, and break through without tribulations (they never pass Heavenly
  Being Grand Perfection in the lower realms).
- **Leaving the sect.** A lower-realm member who reaches Heavenly Being leaves: flies 200-1,000 blocks away, finds a cave
  or a mountainside, sets down a mat and goes into seclusion behind a **Concealment Barrier** that hides their presence
  from Qi Sense and pushes back (and slows) anyone who comes close. They fight only if attacked or the barrier is struck.
  At Heavenly Being Grand Perfection they set out for the rift at 0, 0 and try to ascend to the Upper Realm (7 in 10
  succeed).
- **Journeys follow a real way** (`NpcTravel`). While the land round a traveller runs (within a player's simulation
  distance), it flies in the world. Out of every player's reach (or its land no longer running: a fast flight, a
  teleport or a logout left it behind) it goes on unseen, in a straight line from where it was to where it's bound at
  about flying speed (8 blocks a second), so at any moment it is somewhere on that line. It can be found there: as soon
  as a player's running land reaches where it has got to, it is back in the world right there, in the air on its way,
  as far off as it can be and still move: about 164 blocks at simulation distance 10 (tested), beyond the 160 blocks a
  game shows cultivators from, so it is first seen flying nearer rather than appearing; at a lower simulation distance,
  at its edge (100 blocks at 6), the nearest anything moves. It can be followed, talked to, traded with or fought (a
  fight or a talk stops it; it goes on after). Lower realms and
  the Upper Realm alike: a sect member leaving, a rogue let go, a Heavenly Being on its way to the rift (from the Nether
  or the End it rises out of sight and goes on from the matching place in the Overworld), a newcomer coming out on the
  Upper Realm side of the rift and flying on to the sect it chose. Nobody pops out of thin air where they're bound:
  with a player waiting there, a traveller is put back along its way to fly in in their sight. What a journey comes to
  is settled at its end: there an ascension is tried (unseen, 7 in 10 come through, out near the rift on the Upper
  Realm side), a newcomer joins its sect, one leaving becomes a rogue or seeks seclusion. A destination nobody has
  loaded is loaded for a moment so the traveller can arrive. It sets down on dry ground near the spot (within 64
  blocks, not out at sea), and in the Upper Realm on the nearest island, as a player arriving does (a small platform if
  the abyss is empty there), never over the void; one flying in the world bound for open sky between islands goes on
  to the nearest island, and one held up over open sky (talking, say) hovers rather than sink into the abyss.
- **Coming up.** A cultivator who makes it through to the Upper Realm looks once for the nearest sect of its path
  (righteous, neutral or demonic) that will take it, as far as 8,192 blocks off (nearest first, so it goes further only
  when there's none nearer; that far takes it about 17 minutes unseen): one with a free slot (a member it lost, dying to
  anything but a player, whose replacement hasn't come yet), or a full one with a member weaker than the newcomer, and no
  other newcomer already on the way. Only sects already founded are known: those within 2,048 blocks of the rift are
  founded early in a world's life without anyone going up (see below), so a newcomer has a dozen or more to choose from,
  and further out those near wherever players have been. It holds its
  place at once (the disciple on the way is called off, and nobody else can take it), goes there (unseen, if nobody is
  near, and to be met on the way like any traveller) and joins as it gets there, titled by strength: the Sect Master's
  seat if it is the strongest. A full sect lets its weakest member
  go (never the master) to make room: that one sets off 200-1,000 blocks to make its own way, and no replacement is sent
  for. It goes at once even if nobody is near: every member's place is kept (each second it ticks, and as its land
  unloads, when it can't move), so the sect wakes its land there (failing that, round its home, then the whole
  territory) until it is loaded, and it sets off the moment it is, ticking or not, on its way unseen straight away with
  nobody within 160 blocks. Tested with nobody in the Upper Realm: gone 4 seconds after it was let go. Should it somehow
  be in none of those places, it still sets off the instant its land next loads, before anyone could reach it. If the
  newcomer dies on the way or hasn't come in 3 days, its place goes back to a new disciple. With no sect to
  take it, it finds a high or sheltered spot and goes into seclusion in the Upper Realm, cultivating toward Four Axis.
- **Lives go on unseen** (`NpcLedger`). A cultivator whose land is unloaded catches up when it is next loaded, however
  long it was away: half of every second counts as meditation, stage by stage, and one held at Qi Refining Mid's gate
  asks the heavens once for each day it was away (sect members from the sect's store, rogues from what they carry). The
  steps of a life don't wait for a player either: when a cultivator's land unloads, the ledger reckons the same way when
  its next step falls due (a sect member or rogue reaching Heavenly Being, a secluded one reaching Grand Perfection; one
  held at the gate every 3 days), and then loads its land for 15 seconds (keeping that dimension ticking meanwhile, even
  with no player in it: a level nobody is in stops ticking its creatures after 15 seconds). It catches up, takes the step (leaves its sect,
  goes into seclusion on the best spot to hand, or sets off to ascend), and its journey goes on off-screen. Roughly, at
  that pace: Qi Refining takes 10 days, Foundation Building 17, Core Formation 27, Nascent Soul 43, and a Heavenly Being
  needs 44 more to reach Grand Perfection. Mortals and the Upper Realm's cultivators have no such steps.
- **Sects come into being unseen** (`SectSites`). A sect used to be founded only when a player came near enough for its
  core to tick. Now the server asks the world generator where sects stand (as `/locate` does, generating nothing to find
  out) within 2,048 blocks of the world spawn, 2,048 blocks of the rift in the Upper Realm, and 1,024 blocks of every
  player in either, and loads each one's heart for a moment so it founds itself: its middle generated, its members born,
  and their lives going on from then on like any other sect's, unseen. One site at a time, nearest first, taking spawn,
  rift and players in turn, about two a second; the ground is sampled off the server thread (10-100 ms a site), so it
  doesn't stall the game. Each site is decided once (founded, or no sect there) and remembered across restarts. A new
  world works through what's round spawn and the rift in its first two or three minutes, at the cost of generating each
  sect's middle in the background; a player travelling has the sects ahead founded before they get there. Overworld
  sects are rare (they need wide, flat, dry ground away from villages), so how many stand near spawn depends on the seed
  (none within 2,048 on one test seed, 4 on another); the Upper Realm has a dozen or more round the rift (see above). A
  world made without structures has none. Rogues and hermits wait for a site near them to be settled (`unsettled`)
  instead of asking the generator themselves.
- **Hermits.** Each 1,024 x 1,024-block region of the Overworld has, 4 times in 5, a Heavenly Being (Early, Mid or
  Late) who was in seclusion before anyone came, in a cave (above the deep dark, clear of lava) or high on a mountain in
  the chunk the world's seed picks. They are seated the first time that chunk loads (`HermitSites`; worldgen itself can't
  raise a Concealment Barrier) and live like any secluded cultivator from then on, rumours included. A region with no
  cave or mountain there has none, nor one near a sect's grounds (it waits while a sect site near by is still to be
  looked at). Never where someone has been at work: no spot lit by a
  torch or lantern, or within 4 blocks of a chest, bed, furnace, sign, rails or any other block with a block entity (a
  dark corner of a mine or a cellar looks like a cave otherwise), and never with a player within 80 blocks to see one
  appear (it waits until they've gone). When a region's hermit leaves (sets off to ascend, or dies), a new one takes its
  place two weeks of game days later, seated as the land next loads; one killed by a player is never replaced, as with a
  sect.
- **Rogue cultivators** turn up now and then near a player, out of close sight: 40-80 blocks off around a player who stays
  about (an attempt every 30 seconds, 35% of the time), or 64-128 blocks ahead along the way of one travelling faster than
  7 blocks a second (flying, riding: an attempt every 5 seconds). Each attempt rolls a rank: a mortal 10% of the time,
  falling evenly with every stage to 0.1% for Heavenly Being Grand Perfection; in the Upper Realm the curve runs from Qi
  Refining Early to Four Axis Grand Perfection (`NpcSpawner`). **Cultivators never despawn**, so rogues are kept rare by
  placement instead: there is never more than one wandering rogue within 160 blocks (sideways) of a player, because a new
  one keeps 352 blocks (twice that, plus room for strolling) from every other, loaded or not. Mortal wanderers are kept
  the same way among themselves, apart from the rogues, so you may meet one of each. `RogueIndex` remembers where each
  one is, saved with the Overworld. Sect members, and those seeking seclusion, in seclusion or on a journey don't count;
  a rogue a player has named (a name tag) still does, so naming one doesn't make room for another. Neither kind turns up
  within 160 blocks of a sect's grounds, nor near a sect site not settled yet (`SectSites.unsettled`: the sites round a
  player are looked at within seconds of their coming, so this only holds a spawn back a little).
  None turns up while 40 rogues and wanderers are loaded in the level (sect members don't count). Flying over land you
  meet one every 350-650 blocks or so; over water, none.
- **In a fight** cultivators use what their realm gives them: a Consciousness Domain (rogues always, sect members only
  in combat), Realm Suppression and Qi Flight or Sword Flight, but only while their qi can sustain them. They flee below a quarter of their
  health (all but the Sect Master, who makes a last stand), and rogues and disciples also flee from a hostile domain four or more stages stronger once their own domain
  senses it. The Sect Master and the elders never flee a stronger domain: they stand and fight for their sect. A
  cultivator fleeing a stronger domain runs (or flies) to a refuge about 200 blocks away (160-240), on the far side from
  what it fears, and only then comes home; it gives up after 3 minutes. The refuge is kept within the area the world is
  simulating around players (so it doesn't freeze out there): fleeing from you, it ends up near the edge of your
  simulation distance if that is closer than 200 blocks (10-12 chunks by default, about 160-190 blocks). Badly hurt, a
  cultivator only backs away. Struck by its pursuer at close quarters, a fleeing cultivator strikes back
  (`FLEE_DISTANCE`, `FLEE_SPREAD`, `FLEE_TIMEOUT` in `CultivatorNpc.java`). A fleeing cultivator is a little slower than a
  sprinting player of its own realm, and a badly hurt one well slower, on foot or in the air, so it can be run down
  (`RUN_SPEED`, `FLY_SPEED` in `CultivatorGoals.FleeGoal`).
- Every cultivator has a nameplate: their name ("Mo Lian", or what a name tag called them), title, a red health bar with
  numbers, and alignment, for example "Righteous (+120)". Under Qi Sense, meditating cultivators draw qi motes into themselves.

### Everyday life

Around their duties, cultivators live a day (`NpcLife`), and say things aloud of their own accord: a line over their head,
above the name, in quotes, for as long as it takes to read, seen by anyone within 20 blocks (`NpcSpeech`; every line,
in each path's voice, is in `NpcLines`).

| Time of day | In a sect |
| --- | --- |
| Dawn (0) | A bell rings three times from the main hall; the disciples get up |
| Morning (1,000-4,000) | An Elder (they take turns by the day; with none, the Sect Master) teaches in the main hall, standing before the dais, the disciples seated in rows before it; a listener now and then answers or asks a question. They bow when it ends. With no room in the hall, the lecture is on the plaza |
| Day (4,000-11,000) and evening | Duties, and between them: two disciples stop to talk, two on neighbouring mats whisper, elders talk quietly at their posts; disciples practise sword forms before the training ground's dummies, or spar in pairs in its ring (lunge, parry, a clash of blades, then a bow, and the stronger usually wins), and others stop to watch; a sect laid out before it had a training ground uses the plaza |
| Dusk (12,000) | A drum sounds three times |
| Night (13,000-24,000) | Disciples sleep on their mats (from Core Formation they meditate through the night instead). With outer disciples enough for two watches the gates have a night watch, who sleep in through the next morning; the watch hands over with a word |

- **Talk** is about real things: their cultivation, their duties, the Sect Master and elders by name, the last visitor to
  the grounds (by name), and the rumours going round (`Rumours`: herbs, caves, killings). Rogues who meet on the road pass
  a few words too.
- **Juniors bow** to a passing Elder or the Sect Master, who may nod back.
- **Passing a cultivator** (within 6 blocks, 2.5 sneaking; once every few minutes each): one who likes you greets you
  by name, one who doesn't says so, one two realms beneath you bows in awe, one two realms above is dismissive, a mortal is
  warned; a gate guard welcomes a friend and halts a stranger; and anyone eyes a Supreme or Immortal spirit stone, or a
  Cultivation Fruit, in your hand.
- They remark on the rain and the thunder, dusk and dawn; grumble patrolling in the rain (nobody practises or strolls in
  it); mumble in their sleep; shout when the sect is attacked, its barrier struck or its treasury robbed; cheer a
  breakthrough (theirs, or a fellow's).
- **When a member falls**, those near cry out its name, and for half a day after its fellows go and stand where it fell.
- **The Ritual to the Heavens.** A member whose cultivation fills at Qi Refining Mid goes, by day, to the sect's Altar of
  Heaven (one at a time; a sect from before the altar existed kneels by its Formation Core), kneels fifteen seconds on an
  offering from the sect's store (the cheapest that counts, a Proper one: the store holds one for each member it can house
  and gains one a day), and the heavens answer as for a player, light or thunder; denied, it tries again the next day. One
  away while it waited, and rogues on the road, ask off-screen.
- Talk and practice start only where a player is near enough to see them; the routine (sleep, the lecture) goes on
  regardless. Anything more pressing (a fight, your talk with them) interrupts what they're doing, and they take it up
  again after. Nothing of it is saved: it is worked out again from the time of day.

### Alignment

Alignment runs from -200 to +200: **Demonic** below -50 (red on black), **Neutral** from -50 to 50 (blue), **Righteous**
above 50 (white). A sect shares one alignment (40% righteous, 30% neutral, 30% demonic); rogues are random.

- Neutral cultivators are peaceful unless provoked. Righteous and demonic cultivators attack each other on sight, and a
  sect treats players the same way by alignment.
- Killing a cultivator who hadn't provoked you costs 10 alignment, plus 2 for each realm of theirs; killing a demonic one
  gains 6, plus 2 per realm (`Alignment.killShift`).

### Talking to cultivators

Right-click a cultivator to talk, from up to 6 blocks away. The **talk wheel** opens round the crosshair with what you
can mean by it (click one, or press its number): **Greet**, **Ask**, **Spar**, **Trade**, **Gift** and **Provoke**, and
in some talks **Demand**, **Release**, **Spare** or **Leave**. Greet, Gift, Provoke and the rest are answered in a line
over the hotbar; Trade opens the trading screen; Ask and Spar open the **dialogue box**, with their portrait and name
plate (personal name, title, sect, realm), what they say, your regard, and numbered replies. The wheel folds into tabs
on the box, so you can turn to a gift without leaving; Esc goes back to the wheel, and again ends the talk. They stop and
face you while you talk; walking away (8 blocks), a fight, or being struck yourself ends it, and if they break it off the
window closes with a line over the hotbar saying why (`TalkSystem`, `TalkWheelScreen`, `DialogueScreen`).

- **Names.** Every cultivator has a personal name ("Mo Lian", "Gu Hanyu"), worked out from its UUID (`NpcNames`).
- **Standing.** Their realm against yours, as each side shows it (so Realm Suppression and concealment change it), sets
  how they speak to you and what they entertain. Their path sets the tone: the righteous are formal, the neutral terse,
  the demonic mocking. A mortal player is spoken to as a mortal (Greet, Ask, Trade, Gift).

| Their realm against yours | How they treat you | What the wheel offers |
| --- | --- | --- |
| 2+ realms above | Dismissive | Greet, Ask (the basics: no rumours, nor their elders), Gift |
| 1 realm above | A senior, courteous but distant | All but Spar, until they are Courteous to you |
| The same realm | "Fellow Daoist", and they offer tea | All six |
| 1 realm below | They answer freely: how far along their stage they are, their elders' realms | All six |
| 2+ realms below | As below; pressed down by your Realm Suppress, they kneel | All six, and Demand and Release while you press them |

- **Greeting.** You bow to a senior and cup your fist to an equal or a junior; they answer in kind (`GesturePose`).
- **Regard** (-100 to 100, five pips: Polite from 20, Courteous 40, Warm 60, Trusted 80, Sworn friend 90) is what they
  think of you: their paths' baseline (+10 between two of the same strong path, -10 when you lean the other way), plus
  their own regard, plus their sect's standing with you. At -50 or lower they attack you if they think they can win
  (`Regard`). Courteous opens rumours (and Spar with a senior), Warm a senior's guidance, Trusted the best trades; a
  Sworn friend gives you something now and then when you greet them, and comes to your aid when something attacks you
  within 24 blocks of them.

| What you do | Regard |
| --- | --- |
| Greet them | +2, once a day |
| Give them something (old herbs, pills, Cultivation Fruit, spirit stones; worth less to the stronger, and less each time the same kind of gift comes again that day) | +1 to +25 |
| Win a spar | +8; +15 over a higher realm; +25 when they didn't hold back (once a day) |
| Lose a spar | +3; -3 to someone a realm or more below you, or for walking off (once a day) |
| Spare one who begs for their life | +10 (and +2 alignment) |
| Kill someone who was attacking them | +20 (+5 to their sect) |
| Wake them from meditation (elders and the master on duty don't mind) | -2 |
| Press up against a secluded cultivator's barrier to talk | -5, once a day |
| Provoke them | -15, and they may attack: likelier on the demonic path or if they are stronger, never if a realm below you |
| Demand tribute | -20 (and -5 alignment), once a day |
| Strike them | -30, once a fight |
| Kill one of their sect | -40 to the whole sect |
| Rob their treasury | -60 to the whole sect, once a day |

- A cultivator's own regard drifts a point a day back toward zero; a sect's grudges don't fade.
- Provoking counts as starting the fight: killing them afterwards still costs alignment as murder.
- They won't talk in a fight, in seclusion, while leaving for the sky, while turned on you, while talking to someone else,
  or with another's soul inside them (Soul Projection: they just don't seem to hear you). If one of these comes about
  mid-talk, they break it off. **An enemy of their path** doesn't talk at all: its wheel offers only Provoke (it fights)
  and Leave, unless you are two realms above it, when it throws itself down and begs: Spare or Leave.

**Ask** answers from the world as it is:

| Topic | Needs | What they tell you |
| --- | --- | --- |
| Themselves | Anyone | Their name, title, sect and realm, how long they have cultivated, a line about their path |
| Their sect | A sect member | Its size, path, master (by name and realm), and the nearest sect of the enemy path |
| Its people | A sect member, not far above you | Its elders and master by name, and who keeps the gates |
| Nearby | Anyone | The nearest other sect within 2,000 blocks: its name, path, direction and rough distance |
| Rumours | Courteous, not far above you | One or two real things they have heard of (see below) |
| Guidance | Warm, a realm or more above you | Their Insight: +25% cultivation from meditation for 10 minutes, once a day from each teacher |

Topics not open yet still show, greyed, with the regard they need.

- **Rumours** (`Rumours`): old herbs (a hundred years and more), Cultivation Fruit and Spirit Peach Trees, a secluded
  Heavenly Being's cave, a fallen sect's unclaimed Formation Core, a cultivator killed in the last three days (and by
  whom). Never coordinates: a direction, a rough distance, the country it's in, and for herbs a guess at the age ("five
  hundred and fifty to six hundred and fifty years"); the stronger the teller, the closer their guesses. Each cultivator
  tells you a thing once; rogues have heard of things up to 2,500 blocks away, sect members only within 400. The index is
  fed as those things come and go (their block entities loading, a sect falling, a killing), never by scanning chunks.
  Everything you are told goes into the **Journal** tab of the cultivation menu, newest first.
- **Spar** (`SparSystem`): a duel to first blood. Pick terms in the box: **friendly**, **a wager** (hold a herb, pill,
  fruit or spirit stones: you stake one, they stake something of like worth of their own, the winner takes both; one
  wager a day with each), and, with someone stronger, **don't hold back**. Three breaths, then it begins. It ends when
  either of you has lost a quarter of your health since it began, when you walk 16 blocks off (a forfeit), or after three
  minutes (a draw). Nobody dies: a blow from the other that would go past the line stops at it. A stronger cultivator
  holds back to your realm unless told not to. Blows in a spar are no attack on their sect. Losing to a senior who is
  Warm to you can teach you their Insight.
- **Trade** (`CultivatorTrades`): barter on the trading screen, in spirit stones. They sell by station: Outer Disciples
  and commoners seeds and young herbs, Inner Disciples rarer seeds and Qi Gathering Pills, Elders pills and old herbs, the
  Sect Master the best of both; rogues odd things from far away. The stock changes each day. Regard opens better offers
  (one more at each of Polite, Courteous, Warm and Trusted) and lowers prices (x1.3 when Resentful down to x0.75 when
  Sworn). They also buy what you carry that a cultivator has use for (herbs, pills, fruit), for half to four-fifths of
  its worth by regard.
- **Pressure**: two realms below you with your Realm Suppress on them, they talk on one knee. **Demand** takes tribute
  (something of what they keep, and spirit stones); **Release** lifts your pressure off them for five minutes, or until you
  switch Realm Suppress off (switched on again, it bears down on them like anyone else).
- **Mercy**: a fleeing cultivator you outclass by two realms, run down and struck, throws itself down and begs for 15
  seconds; right-click it to answer. **Spare** it, and it owes you its life: it won't raise a hand against you for a day.

### Spirit stones

Heaven and earth's qi condensed into small, smooth oval stones with a pearl's sheen: the cultivators' money (prices,
wagers, gifts) and a store of qi. Five grades, each worth nine of the one below: **Low, Mid, High, Supreme, Immortal**.
Nine of a grade craft into one of the next, and one breaks back into nine (`SpiritStoneItem`). They glow, more with
each grade: a soft halo in the grade's colour around the stone (in slots, in hand, on the ground, in item frames), and
the stone lit from within so it still shows in the dark. A Low stone barely gleams; Supreme and Immortal stones shine
and slowly breathe (`SpiritStoneGlow`).

- **Absorb**: hold right-click for a second and a half to draw a stone's qi into your pool, 25 qi for every Low stone's
  worth (225 for a Mid, and so on); the stone is used up.
- **Meditate** with one in either hand to cultivate 50% faster while its qi lasts: 30 seconds for every Low stone's
  worth, then it is spent (how much is left shows on it).
- **Where they come from**: Spirit Stone Veins (Low stones) through Overworld stone, the deepslate kind with the odd Mid;
  Rich Spirit Stone Veins (Mid, a chance of High, rarely Supreme, very rarely Immortal) at the bottom of the Overworld
  and all through the Upper Realm (iron pickaxe for the common veins, diamond for the rich); sect chests (most in the
  treasury); slain cultivators, who carry them by realm; and selling herbs, pills and fruit to cultivators.

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

The Netherworld's own commands (`/netherworld`, `/method`) are listed under The Netherworld. Its Ghost Gate is remembered
as built in the Netherworld's `data/defying-the-heavens_ghost_gate.dat`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
