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
stone, ores, qi veins, Blue Spirit Wood, leaves), the clothing (every path's colours, grade and piece), and the
Cultivator Spawn Egg. An item added later that the list doesn't name still shows, at the end. The items stay in
vanilla's tabs too. The mod's other creatures (tribulation clouds, heart demons, the bodies left behind by the Inner
Realm and Soul Projection) only arise from what makes them, so they have no eggs.

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
leaves the body, then the first light of qi). Players who were already
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
and sect cultivators perform it too (see Everyday life). Anyone already past Qi Refining Mid already has it.

1. Build a **Heaven Altar** (smooth stone, jade, a Low spirit stone and copper) under open sky, and build round it.
2. Right-click it to open its screen: nine **offering** slots in a ring (anything at all may be laid there) round a dial
   showing the **chance**, which moves as you add or take items; beside it the offering's tier and worth (and what the
   next tier needs), the altar's grandeur and its points by part, the height bonus, the reward (permission to cultivate, and
   any Heaven's Favour), and whether you may begin now (`HeavenAltarMenu`, `HeavenAltarScreen`).
3. **Sneak and right-click it with empty hands** to begin. The offerings rise as smoke and are taken, whatever happens.
4. **Kneel still for three minutes.** Turning your head is fine; moving a third of a block (being knocked back too),
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

**Icons:** every ability has its own 16x16 icon (`textures/gui/ability/<id>.png`), shown on the wheel, in the editor and
beside its name in the ability list: Qi Sustenance a red rice bowl with qi rising like steam, Qi Flight the golden core
borne up on azure qi wings (it was a sword on a cloud until Sword Flight came), Qi Sense an eye ringed by the five
elements' motes, Consciousness Domain a lattice sphere around a bright soul, Realm Suppress a mountain pressing down on
red chevrons, the Inner Realm a floating island with a blossom tree, Sword Flight a sword with streaks of light behind it.
A new ability needs a PNG of its own there.

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
soul is near, no outline, no name.

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
bamboo grow on the open grass between. Buildings stand on stone bases, with plain plastered walls between their pillars, under tiled roofs whose
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
  Heaven (one at a time; a sect from before the altar existed kneels by its Formation Core), kneels three minutes on an
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

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
