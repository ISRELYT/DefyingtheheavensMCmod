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
  leaves a Peach Pit that grows a Spirit Peach Tree (see below).
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

- Meditation Mat: three wheat in a row.
- Red Silk Meditation Mat: a Meditation Mat and red carpet (shapeless).

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
- Spawning, on average one plant per this many chunks: Ginseng 80 in Overworld forests, taiga and jungles and 32 in the
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

Eating a Cultivation Fruit leaves its **Peach Pit**. Plant the pit in soil and it grows into a **Spirit Peach Sapling**,
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

- Recipe: polished deepslate in the top and bottom rows, with jade stone or an emerald in the middle.
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

Every new player starts as a **mortal**: no cultivation, no qi, no realm bonuses and no abilities, and meditation does
nothing. Drinking a **Marrow Cleansing Elixir** opens the meridians and starts the player at Qi Refining Early. Players
who were already cultivating in an existing world stay cultivators.

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
| Foundation Pill | Ginseng, Lingzhi, Spirit Ginseng, Amethyst Shard | 45 s | Needed to break into Foundation Building |
| Core Pill | Spirit Ginseng, Huangjing, Cultivation Fruit, Blaze Powder | 60 s | Needed to break into Core Formation |

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
/cultivation mortal                                   back to mortal, to try the elixir
/give @s defying-the-heavens:core_pill{PillGrade:4,PillAge:10000}   an Immortal-grade pill (grades 0-4)
```

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

- All of them grow more often everywhere in the Upper Realm. Spawn rates (one per this many chunks) are the
  `*_CHUNKS_*` constants in `ModPlacedFeatures.java`: Lingzhi and Huangjing 100 / 40 (Upper Realm), their rare variants
  1000 / 250, Spirit Lotus 160 / 64, Spirit Dew Grass 40 / 16. If you change them, also change the matching
  `placed_feature/*.json` (or re-run `runDatagen`).
- The Spirit Lotus is a lily pad as a seedling, a bud from year 4 and in flower from year 10.
- Spirit Dew Grass gathers a bead of **Spirit Dew** every few minutes (at once in the Upper Realm). Right-click to collect
  it; the grass stays. Drinking Spirit Dew refills a quarter of your qi; it is also an ingredient.

## Blue Spirit Wood

Spruce-dark logs with blue qi flowing up their veins (an animated texture); the planks show faint blue grain. Log, wood
and planks work like vanilla wood.

It grows wild as the **Blue Spirit Tree**, a rare spruce (vanilla spruce shape and leaves) with a Blue Spirit Log trunk: on
average one per **24** chunks of Overworld taiga and one per **16** chunks anywhere in the Upper Realm
(`BLUE_SPIRIT_TREE_CHUNKS_*` in `ModPlacedFeatures.java`, `blue_spirit_tree` in `ModConfiguredFeatures.java`). There is
no sapling: its leaves drop ordinary spruce saplings, so every Blue Spirit Log comes from a tree you find. In the
Overworld it takes root on the ground beneath the taiga's canopy and grows up through it.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
