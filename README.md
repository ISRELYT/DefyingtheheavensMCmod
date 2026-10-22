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

## Storage Rings

A **Storage Ring** folds a space into a ring: crafted from four gold ingots, four jade and an ender pearl (gold at the corners,
jade at the sides, the pearl in the middle). Right-click to open it; sneak-right-click to wear it in a ring slot, and the
**Storage Ring key (B)** opens the one worn (or else the first one carried). Its screen, in lacquer and gold: what it holds
as a grid (one well a kind, how many beneath it, shortened to k/M/B), a search line, how full it is, and the pack below.
Click a kind to take a stack (right-click: half; shift-click: straight into the pack); click with something carried to put
it in (right-click: one); shift-click the pack to put a stack in. Rings can't go inside rings, and the open ring can't be moved.
A ring lying on the ground never despawns and is proof against fire, cactus and blasts (the void still takes it).

What it holds lives with the world, keyed by the ring: the ring goes where it goes (a fallen cultivator's ring is the
victor's), and a ring lost is a space lost. **Refine** (top right of its screen) raises its grade:

| Grade | Holds | Refining into it takes | Realm |
|---|---|---|---|
| Mortal | 27 stacks | crafted | - |
| Spirit | 54 stacks | 16 Low Spirit Stones, 4 ender pearls | Foundation Building |
| Earth | 108 stacks | 16 Mid Spirit Stones, 8 jade | Core Formation |
| Heaven | 216 stacks | 16 High Spirit Stones, 2 shulker shells | Nascent Soul |
| Boundless (Qiankun Ring) | **without end**: any number of any number of kinds | 8 Supreme Spirit Stones, 1 Immortal Spirit Stone, a nether star | Heavenly Being |

Code: `StorageRingItem`, `StorageRings` (the spaces, saved data, clicks, refining), `StorageRingMenu`; client
`StorageRingScreen`, `ClientStorageRing`; tests `StorageRingGameTests`.

## Meditation mats

A **Meditation Mat** (a flat, round mat of coiled straw) and a **Red Silk Meditation Mat** (the same mat with a square of
red silk laid across it) can be placed on any block. Each is drawn one and a half blocks across, overhanging its block by
a quarter block on every side; only the middle block is solid, so leave a block between mats so they don't overlap. Right-click a mat to sit in its centre and start meditating: the same
checks, messages and cultivation gain as the meditation key. Right-click again, or move, to stop. Sitting on a mat speeds
meditation (see below).

**Meditating in the air** (the user's ask, 2026-10-22): one may sit in meditation while flying (Qi Flight, Sword Flight, the dead's
free flight), not only on the ground. The flight goes on costing its qi all the while, so only where gathering keeps up with it
(from Heavenly Being everywhere, anyone in the Upper Realm) can one stay up there and cultivate indefinitely; a pool run dry ends
the flight and drops one out of the meditation ("Your qi can no longer hold you up"). Sneaking, drifting or rising end it as on the
ground, and the soul turns inward to the Inner Realm only from firm ground (`MeditationManager#steady`).

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

A meditating cultivator draws the land's qi into itself as motes, as many as it truly gathers (`QiSenseClientHandler#inflow`,
by its realm, stage and boosts): in the Upper Realm some 2 a tick at Qi Refining, 5 at Heavenly Being and 6 at Four Axis, and
past that the more for every realm (the user's ask, 2026-10-22, when the realms after Four Axis all drew in Four Axis's 6): Dao
Integration 7 to 8, Void Shattering 9, Sacred Vessel 11, Entering Nirvana 12, True Immortal 14, at most 16 with boosts. The lower
realms' thin qi gives half as many; an NPC cultivator draws in as a player gathering a tenth as fast.

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
| Longevity Pill | 2 Lingzhi, Huangjing, Golden Apple | 40 s | +20 years of lifespan per point of potency; each one taken in a life works half as well as the one before (Lifespan) |

The **Rebirth Pill** is no brew: it's found in the Upper Realm (Lifespan, below).

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

## Physiques

Every player is born with a **physique**, rolled the first time they join, told in chat and written in the journal; the
Stats tab names it (and its roots), with its grade's colour. An op can change it: `/physique get|set <id>|reroll [player]`.
The rarer the grade, the rarer the roll. Out of every 100 births (evened out 2026-10-20, the user's ask): **Low 56, Mid 24,
High 12, Supreme 5, Immortal 3**. The grade is the body's **talent** (2026-10-21/22): what the years meditation costs are divided
by, Low x1, Mid x1.5, High x2.1, Supreme x4.5, Immortal x16 (Lifespan: the talent race), and a small edge to its pace on both
paths on top of the body's own, Low x1, Mid x1.1, High x1.2, Supreme x1.35, Immortal x1.5 (`Physique.Grade#talent`, `#speed`;
`Physique#talent`, `#speed`: the Eternal Reincarnation Body, for all its Immortal grade, has a Low one's).

| Physique | Grade | Births | What it does |
|---|---|---|---|
| Five Elements Spiritual Roots | Low | 40 | Any element's methods, at the ordinary pace: the plain body (it took the old Mortal Body's share, removed 2026-10-19 as the same body; a save that names it reads as this) |
| Cursed Meridian Body | Low | 10 | Qi gathered 0.8 times as fast into a pool 0.8 times as deep |
| Shattered Meridians | Low | 6 | Can't cultivate: no heaven methods, no Marrow Cleansing Elixir. **Body refining** instead: past Mortal Peak, eight stages (Copper Skin, Iron Flesh, Steel Sinew, Jade Bone, Silver Marrow, Golden Blood, Diamond Heart, Indestructible Body), each a body as strong as a cultivator's a stage higher, the last Core Formation Early's; tempered as a mortal is, and by meditation. It lives as long as that cultivator too (Lifespan, below) |
| Three Elements Spiritual Roots | Mid | 7 | Methods of three elements only, cultivated 1.5 times as fast |
| Heaven Sacred Body | Mid | 5 | No Ritual to the Heavens wanted at Qi Refining Mid; heaven methods 1.2 times as fast |
| Yin Soul Body | Mid | 4 | Yin corruption never takes hold; the ghost path 1.2 times as fast |
| Innate Alchemy Body | Mid | 4 | Pills and elixirs brewed by its hands (it put in the last ingredient) work twice as strongly |
| Spirit-Eye Body | Mid | 4 | Sees souls and the dead from its first day, a mortal's too, as the Ghost Eye does (outside the Netherworld; the dead and the Method Steles through walls within 32 blocks), clearly and for nothing |
| True Element Spiritual Root | High | 3 | One element's methods only, twice as fast |
| Innate Consciousness Body | High | 2 | A Consciousness Domain as wide as a Four Axis cultivator's (160) from the day it opens; it never grows |
| Blessed Meridian Body | High | 3 | Qi gathered 1.5 times as fast into a pool 1.5 times as deep; Qi Sense from a mortal's first day |
| Nine Yang Body | High | 2 | Fire and lava never burn it (its flames go out; a star's heat is fire too); heaven methods 1.3 times as fast, the ghost path at half the pace |
| Starborn Body | High | 2 | The void bears it from **Core Formation** (others need Nascent Soul); a star's heat never burns it; space's thin qi is whole to it (x1, not x0.25); it flies through space 1.5 times as fast, and pays the years of its flight there by as much less (a year every 150 blocks) |
| Jade Poison Body | Supreme | 1.25 | No medicinal toxicity; a pill's or fruit's qi refined the moment it is taken (what doesn't fit before a bottleneck waits); no poison nor wither takes hold of it, and its blows poison (5 s: I, II from Core Formation, III from Heavenly Being) |
| Ancient Holy Body | Supreme | 1.25 | Health (the whole of it) and strength 1.3 times; a tribulation's strikes three quarters as strong; tempering twice as fast |
| Blood Demon Body | Supreme | 1.25 | Heals by 15% of the health its blows take (spells too) |
| Hundred Refinements Body | Supreme | 1.25 | One stat point more at every breakthrough |
| Fallen True Immortal | Immortal | 0.6 | Any element; both paths five times as fast; a method's new layers need no settling |
| Ghost Devil Body | Immortal | 0.6 | The ghost path twice as fast; a Ghost King's form of its own anywhere from the start (held to its ghost path's might while shown); a Ghost King when dead; learning a ghost foundation of Core Formation or deeper takes the ghost path straight to that realm's Early |
| Heaven-Defying Body | Immortal | 0.5 | **The heavens can't strike it down**: a tribulation's killing strike leaves it one heart, and the tribulation fails (the dead don't scatter for it). **No failed tribulation costs it a stage** (the user's, 2026-10-20): struck down, fled or killed by something else, it keeps its stage, its cultivation and any breakthrough pill taken, and may face the heavens again at once. The lifespan tribulation (Lifespan) is another thing: it dies to that as anyone does, and its life is ended. No Ritual to the Heavens wanted. Heaven methods 0.75 times as fast, and the righteous think 15 less of it |
| Primordial Chaos Body | Immortal | 0.4 | Meditation feeds **both paths at once**, each at its full pace (the living, the ghost path begun); no element barred; its spells (the heaven methods' qi, the ghost path's harm) strike a quarter harder. The heavens hate it: every tribulation strikes half as many times again (rounded up: Core Formation's 6 become 9, Four Axis's 100 become 150), at its realm's pace, so it takes the longer; its lifespan tribulation strikes half as often again |
| Soul-Devouring Body | Immortal | 0.4 | Every cultivator it slays, NPC or player, gives it a tenth of what the victim's stage takes, on the path it cultivates (as far as fits before a bottleneck); a player it slays loses a stage of their own, on their higher path (the user's ask, 2026-10-20: a friend killed again and again to feed one pays for every time); each righteous or neutral one drags its alignment 10 toward the demonic; the righteous think 20 less of it |
| Heavenly Thunder Body | Immortal | 0.3 | A tribulation's lightning feeds it: no harm, the qi pool filled, and the strikes bank up to 60% of the new stage's cultivation for the breakthrough. The lifespan tribulation's it drinks whole only up to half its power (cultivation into its bar), less and less after: at the last it is too much even for this body |
| Eternal Reincarnation Body | Immortal | 0.1 | **A Low grade's talent and pace** (x1, no edge) for all its grade, but reborn (a life spent, a soul scattered; a Rebirth Pill has no hold on it, below) it **keeps this body and all its cultivation** (the user's ask, 2026-10-22: "you can reach it if you work hard and you dont lose stuff"): each path's realm, stage and progress, every method as deep as it was, the stat points, the axes of virtue and the obsession, Heaven's Favour, a breakthrough pill taken, its alignment, its abilities and wheels; only the life is new (16, its realm's years ahead of it). A Low talent that keeps everything reaches True Immortal in some 60 lives with nothing saving its years, 4 with all of it (it was set back to half its realm until then, which a Low talent never outgrew: `PlayerCultivation#carryOver`); the rarest, with the next |
| Myriad Form Body | Immortal | 0.1 | Born with **two more physiques** of Mid to Supreme grade, by their weights, with all their gifts (paces and pools multiply); never a pair that clashes: at most one of roots (Three or True Element), never the Nine Yang with the Yin Soul |

Roots' elements are drawn at birth, the elements of any methods already known first (so a save from before keeps its methods);
a Myriad Form Body's form of roots gives the body its roots. An element limit never touches the ghost path. A player already
cultivating is never given Shattered Meridians. Code asks `PlayerCultivation#hasPhysique` (a Myriad Form Body's forms count), its
`physiqueCultivationFactor` / `GhostFactor` / `QiFactor`; `Physique`, `Physiques` (the roll, the command, the gifts that live in no
other system: fire, poison, blood, devouring, Chaos's spells, the righteous's unease), `BodyRefining`; tests `PhysiqueGameTests`,
`PhysiquesPillsGameTests`.

## Stat points

Every breakthrough gives **stat points** to put into the body's six stats: **2** for a stage, **5** for a realm. They're
counted from the cultivation reached (the higher of the two paths), so a cultivator already far along has all of theirs at
once; a mortal on neither path has none. Each point is a share more of what its stat governs:

| Stat | Per point | Governs |
|---|---|---|
| Strength | +1% | Melee harm |
| Constitution | +1% | Maximum health |
| Agility | +0.5% | Walking speed and attack speed |
| Endurance | +1% | Armour and its toughness |
| Spirit | +1% | Every path's pool: the qi pool and the ghostly qi pool |
| Insight | +1% | Every path's cultivation gathered meditating, heaven and ghost alike (and half as much to qi and ghostly qi gathering) |

**The stats are the cultivator's, not one path's** (the user's rule, 2026-10-19): every path's pool, gathering and
cultivation goes through `StatPoints#poolFactor`, `#cultivationFactor` and `#gatherFactor`, and any path added later is to
do the same. The methods' layers settle from the same gain, so Insight speeds them too.

They're spent on the **Stat Points** page of the Stats tab (the two pages are buttons at its top; the Points button turns
gold, with the count, while there are points to spend): each stat with its points, what they give, and a **+** (shift-click:
five). The six stats' spread is drawn as a hexagon, each corner a stat, the gold shape reaching toward it as far as its points
go against the most in any one. **A point spent is locked in** (the user's rule, 2026-10-19). Should cultivation fall back below
what was spent (a golden core detonated, the Chief Judge's gaze), only the points the lost stages gave are taken back, **the
last put in first**; everything spent before the stage you fell to stays where it is, and you're told how many went. Climb
back and those points are earned again, to spend anew (`PlayerCultivation#statPointOrder`, saved). An op can take back all their own with `/cultivation
resetstats`. Code: `StatPoints` (the attribute modifiers, and the shared factors the pools and gathering read), client
`CultivationScreen#renderStatPoints`; tests `StatPointsGameTests`.

## Lifespan

Everyone ages (the user's ask, 2026-10-19): **a year for every game day** spent online (20 minutes), from **16**. A mortal
has **100 years**; each realm reached adds its years on top of all those before it, by the **higher of the two paths**:

| Realm | Years it adds | Lifespan at it |
|---|---|---|
| Mortal / Qi Refining | none | 100 |
| Foundation Building | +200 | 300 |
| Core Formation | +400 | 700 |
| Nascent Soul | +600 | 1,300 |
| Heavenly Being | +1,200 | 2,500 |
| Four Axis | +10,000 | 12,500 |
| Dao Integration | +100,000 | 112,500 |
| Void Shattering | +10 million | some 10.1 million |
| Sacred Vessel | +100 million | some 110 million |
| Entering Nirvana | +10 billion | some 10.1 billion |
| True Immortal | without end | without end |

The user's years for the realms after Four Axis (2026-10-21). **A True Immortal's years have no end** (`Lifespan#endless`): it no
longer ages, no lifespan tribulation comes for it, and nothing costs it years: its meditation, flight through space, Life Force; nor
can its years be drawn out of it, and a Longevity Pill refuses to be wasted on it. Burn Lifespan burns on for nothing (the user's
call, 2026-10-22: left as it is for now). The Stats tab says "an immortal's years without end".

**Body refining** (Shattered Meridians) counts as the realm each of its stages matches (2026-10-20): Foundation Building's
years from Jade Bone, Core Formation's at the Indestructible Body.

Years taken from the grateful (the Axis of Longevity's wishes, below) add to it, and so do **Longevity Pills** (Alchemy): 20
years a point of potency, so 20 for a Low-Grade pill of fresh herbs up to 200 for an Immortal one of 10,000-year herbs (twice by
an Innate Alchemy Body). Each one taken in a life works **half** as well as the one before (the user's call, 2026-10-20, so no
one lives forever on pills: a life's Longevity Pills add at most twice the years of the best of them, so take the best first; the
tooltip says how much less the next will do). Within 10 years of the end you're warned,
and again at the last; the Stats tab shows your age against your span, in red within ten years. Your age **stops at your
span's end** (the user's ask, 2026-10-20), so any years you win after it are yours in full.

**The lifespan tribulation** (the user's ask, 2026-10-20; `LifespanTribulation`). A spent life isn't simply over: **the
heavens come to collect it**, a storm over you that grows for **five minutes** to its full power, its strikes ever faster (one
every 3 seconds at first, ten a second at the last) and ever heavier (a share of your max health: half a percent at first, two
fifths at the last), its cloud and bolts **swelling to three times their size**. Nobody bears its full power. Its strikes pass
armour, enchantments, effects, the heaven methods' wards and totems; healing holds it off for a while (unhealed, most last about
two minutes), and you can go on **meditating** under it. The whole server is told it has begun, and the HUD shows its power.
- **Nowhere to hide but the Spatial Gap**: it follows you into every realm, the Netherworld too, but in the gap the heavens
  lose sight of you and it **waits, as strong as it was**, until you come out. Logging out, it waits the same (it's saved).
- **It ends with years won again**: a breakthrough into a **new realm** (a Grand Perfection into the next realm's Early; a
  stage's breakthrough gives no years, so it goes on), or years taken from the grateful (the Axis of Longevity). Your
  breakthrough's own tribulation strikes on top of it. **No Longevity Pill may be taken under it** (a pill's few years would end
  it, only for it to begin again from nothing). The server is told you have won new years.
- **Any death while it runs ends the life whole**, its strike or anything else (a phoenix's rebirth holds off only other
  deaths, never its own strike): everything cultivated goes with it, both paths, every method, spell and stat point, the axes
  of virtue, and the body itself (a new physique is rolled), and you come back a mortal of 16. Only the journal remembers, and
  the Netherworld's own bookkeeping. The death drops your things as any death does, but it doesn't take you to the Netherworld:
  **you respawn at your own spawn point, alive** (your bed, or the world's spawn). An Eternal Reincarnation Body keeps itself and
  all its cultivation, its years begun anew (Physiques, above).
- **Physiques**: a **Heaven-Defying Body** dies to it like anyone (no heart is left it here); a **Heavenly Thunder Body** drinks
  its strikes whole up to half power (no harm, its qi full, cultivation into its bar), less and less after, and none at the last,
  so it dies too; an Ancient Holy Body and Rings of Transcendence soften it as any tribulation; a Primordial Chaos Body is struck
  half as often again.
- **A soul out of its body** (Soul Projection, possession): the strikes fall on your **body** where it sits, and its death is the
  whole life's. No soul goes out under it, and nobody may possess someone under it; one whose body another soul holds ages on
  (only watching), and when their life runs out the possessor is driven out.
- **Mortals** face it too. **The dead** face the Netherworld's version (yin lightning from a dark teal storm); struck down under it
  (by it or anything else), the soul **scatters into the Soul River** and is born again alive, a mortal of 16. One reborn at the
  Wheel mid-way is still spent: the heavens take over, as strong as it was.

**The Rebirth Pill** (2026-10-20): a way out of a bad roll. Eaten (only while sneaking, so never by a slip), it ends the life's
cultivation exactly as a spent life does, a new physique with it, **but you don't die**; the dead (a Ghost King, the one of them
with hands to eat it) are born again alive, as when their years run out among the dead. Any number may be taken, so it is rare: no
cauldron brews it, and it's found only in the **Upper Realm's** sect treasuries (one chest in 50; a treasury has three) and on
their sect masters. **It has no hold on an Eternal Reincarnation Body** (the user's call, 2026-10-22: born again losing nothing, it
would have had a whole new life of years from it): such a body won't eat it, sneaking or not, and is told so. Code:
`RebirthPillItem` (`#refuses`), `Lifespan#rebirth`, `loot_tables/chests/sect_treasury.json`.

**One clock, alive and dead** (the user's ask, 2026-10-20): the dead go on aging as the living do, against the same span, so
the lifespan limits the whole existence. Dying near the end gives no years back (the dead used to have a clock of their own,
so a death gave a whole span again), and coming back to life gives none either. When the years run out among the dead, the
Netherworld's lifespan tribulation comes (above). So the dead have reason to cultivate too. Creative and spectator players don't
age (but one whose body another soul holds). Saves from before lifespans begin at 16; saves from before the one clock add the
years they had spent among the dead to their age.

**Meditation costs years: the talent race** (the user's vision, 2026-10-21: talent decides how far one gets; it is a race for
most, not for the extraordinarily gifted). Every bit of cultivation won by meditating costs years, on either path and by body
refining. A realm's whole cultivation costs a **share of the years the realm gives**, shared out over its four stages by how long
each takes, and paid as it is won; it is **divided by your talent** (your physique's grade: shown on the Stats tab) and by **what
saves years** where you sit, all four multiplied together (the user's calls, 2026-10-21/22; each only ever helps, so going without
one never costs extra):
- **the meditation's boost**: mats, pedestals, height, tranquillity, garments (the percentage told as you sit down), up to about x3.65;
- **a method** practised: what it makes of the gain, by its element's share of the qi there (x1 at an even fifth, up to x1.5 where it
  runs thick) and x1.25 beside its Dao Stele, so up to x1.875 (on the ghost path, its stele's x1.25);
- **a spirit stone** burning in hand: x1.5;
- **dense qi**, where it is denser than the realm's own place: the Upper Realm's (x10) to a Heavenly Being or below, a rich planet's
  (up to x3) in the Astral Realm, the Nine Nether Peaks (x1.2) to the ghost path. Four Axis is cultivated only in the Upper Realm (its
  needs are ten times another's already), so it is measured against the Upper Realm's own and no planet makes it cheaper; so the
  dense qi helps the early realms, where the late ones decide the race.

Only what is won is paid for: waiting at a bottleneck costs nothing, nor do a reward's seconds of meditation, a kill or a
tribulation's strike. The Cultivation tab shows what the rest of your realm will cost against the years you have left (red when they
won't cover it). Besides the years, the grade gives a small **edge to the pace** (Physiques, above: up to x1.5 for an Immortal grade),
so the gifted get there a little sooner too; but the years decide how far.

| Realm | Its years | Share | Years at x1 |
|---|---|---|---|
| Qi Refining | 84 (a body's own, from 16) | 0.6 | 50 |
| Foundation Building | 200 | 0.9 | 180 |
| Core Formation | 400 | 1.6 | 640 |
| Nascent Soul | 600 | 2.3 | 1,380 |
| Heavenly Being | 1,200 | 3.3 | 3,960 |
| Four Axis | 10,000 | 4.8 | 48,000 |
| Dao Integration | 100,000 | 8 | 800,000 |
| Void Shattering | 10 million | 12 | 120 million |
| Sacred Vessel | 100 million | 16 | 1.6 billion |
| Entering Nirvana (the middle boundary's end) | 10 billion | 20.5 | 205 billion |
| True Immortal | without end | none: its cultivation costs no years | |

Where each gets to (the years left over from a realm carry into the next; a percentage is how far into that realm):

| Saving | Low x1 | Mid x1.5 | High x2.1 | Supreme x4.5 | Immortal x16 |
|---|---|---|---|---|---|
| None | Core Formation (71%) | Nascent Soul (76%) | Heavenly Being (75%) | Dao Integration (56%) | Entering Nirvana (78%) |
| Usual (x1.5: a mat, a method or a stone) | Nascent Soul (76%) | Heavenly Being (84%) | Four Axis (69%) | Dao Integration (88%) | **True Immortal** (17% to spare) |
| Good planning (x5: mats and pedestals, a fitting method, a stone) | Dao Integration (64%) | Dao Integration (99%) | Void Shattering (88%) | **True Immortal** (10%) | True Immortal (290%) |
| Everything right (x10.3, the most there is) | Void Shattering (86%) | Sacred Vessel (99%) | **True Immortal** (6%) | True Immortal (126%) | True Immortal (704%) |

So (the user's asks): an Immortal grade becomes a True Immortal at ease, a Supreme one only with good planning, a High one only by
doing everything right, and nobody lower; a Low grade with nothing saving years doesn't get past Core Formation. Longevity Pills,
Life Force's years and the grateful's push a little further. (The realms after Four Axis were planned at these years and shares
before they were built, 2026-10-22: the race was balanced to the whole road.) Code: `Lifespan#seclude`, `#seclusionYears`, `#saving`, `#placeSaving`,
`#realmSeclusion`, `SECLUSION_SHARE`, `Physique.Grade#talent`, `#speed`; the scratchpad's `talent-ladder.ps1` made the table.

**What years are spent on besides** (the user's asks, 2026-10-21, so that even a long life has to be watched):
- **Flying through space**: every 100 blocks flown there costs a year, **the same at every realm**, so the stars are a risk to
  everyone below Four Axis: the next star (some 16,000 blocks) about 160 years, the planets of one system tens of years apart. A
  **Starborn Body** pays the less by its speed there (1.5 times as fast: a year every 150 blocks). On a planet's land, and
  everywhere else, one ages as always; only flight in space counts (an arrival, a hold at a planet's edge, a teleport cost
  nothing). The HUD shows what the trip has cost, the years left and what flying straight home would cost, turning red (with a
  warning) once the years left can't pay for it; run out out there and the lifespan tribulation finds you there. The dead pay too;
  creative players don't, nor a True Immortal (the line goes away when one becomes one on the way, and a new trip's comes back
  if one stops being one). Fixed 2026-10-22 (the user's report): racing through space faster than its chunks load, a player's own
  tick stopped and the line froze (`AstralRealm#keepTicking` ticks such a one), and its billions of years left were told as
  2,147,483,647 (whole doubles now). Under it, in space, **the way home**: an arrow turned toward the Overworld from where you look,
  whether it lies above or below you, and its distance in blocks (the user's ask, 2026-10-22; a planet just come down onto says how
  to leave it again). Code: `AstralRealm#travel`, `YEARS_PER_BLOCK`, `#yearsPerBlock`, client `TribulationHud#drawWayHome`.
- **Meditating faster**: the Cultivation tab's pace button beside Meditate, x1 / x2 / x3. At x2 every gain of a meditation comes
  twice as fast (both paths, the methods settling, pill and fruit qi refined, a body tempered), but **each bit of it costs twice
  the years** (above: four times the years a minute); at x3, three times as fast, each bit for **four times** the years. A reward
  of so many seconds of meditation (a Merit foe's, a fruit's) is still the plain pace's. With 10 years left, and again with one, a
  fast meditation slows to x1 by itself (you may set it faster again). The base pace is unchanged. Code: `Lifespan#pacePrice`,
  `#cycleMeditationSpeed`, `MeditationManager#pace`, `PlayerCultivation#meditationSpeed`.
- **Burn Lifespan** (an ability from Qi Refining, for every cultivator, off until switched on): while it burns it costs **half a
  percent of your whole lifespan a second** (a share, so every realm alike: 6.5 years a second at Nascent Soul, 62.5 at Four
  Axis), and your blows and spells strike half again as hard and you move a quarter faster, wreathed in crimson. It goes out by
  itself before it would leave you fewer than 10 years (and won't light then), when you die and when you log out. Code:
  `BurnLifespan`.
- **Life Force's years** (Wood, Foundation Building): Life Force Gain grows a herb or fruit on **your own years** (10 a second for
  its 120 years of growth a second, never your last 10; a being is still healed with your health); Life Force Drain on another
  player takes **10 of their years a second** with their life, half of them yours (aged to their span's end, the heavens come
  for them). Beasts and NPCs have no lifespan to take; a herb drawn on still only heals you. Code: `WoodSpells.Life`,
  `Lifespan#draw`.
- **Old age weakens only mortals**: from 60% of their span a mortal's body fails a little more every year, to 40% less max
  health, 20% less speed and 30% weaker blows at the span's end (years won back restore it). A cultivator keeps its vigour to the
  very end, when the heavens come instead. Code: `Lifespan#oldAge`, `#weaken`.

Op: `/cultivation lifespan` (your age against your span), `/cultivation lifespan set <years>` (your age, alive or dead). An
obsession with Longevity (Name Your Obsession, below) makes the whole span x1.2. Code: `Lifespan`, `LifespanTribulation`,
`PlayerCultivation#age`/`extraYears`/`longevityPills`/`lifespanTrial`/`lifeEnded`; tests `LifespanGameTests`, `LifespanCostsGameTests`,
`PhysiquesPillsGameTests`.

## The realms after Four Axis

The user's realms (their order told 2026-10-17, built 2026-10-22): **Dao Integration**, **Void Shattering**, **Sacred Vessel**
and **Entering Nirvana**, which ends the middle boundary, then **True Immortal**, the first of the major boundary and the last
realm for now. Each has the four stages; all are the Upper Realm's alone, as Four Axis is (needing ten times the cultivation,
broken into only there or wherever else the lower realms' cap doesn't hold), every stage longer than the one before. Saves are
safe: realms are saved by their place in the line, and these come after Four Axis.

| Realm (Early; Grand Perfection x1.3) | Health | Strength | Qi pool | Qi a second | Its might over the realm before |
|---|---|---|---|---|---|
| Four Axis | 180 | 50 | 8,000 | 200 | x2 |
| Dao Integration | 360 | 100 | 18,000 | 450 | x2 |
| Void Shattering | 1,440 | 400 | 60,000 | 1,500 | **x4** (the user's: a larger leap) |
| Sacred Vessel | 2,880 | 800 | 130,000 | 3,250 | x2 |
| Entering Nirvana | 5,760 | 1,600 | 280,000 | 7,000 | x2 |
| True Immortal | 57,600 | 16,000 | 2,500,000 | 62,500 | **x10** (the user's: an immense leap) |

Armour, toughness and knockback resistance stay at Four Axis's: vanilla's caps hold them there anyway (80% of a blow turned).
**Vanilla's caps on max health (1,024) and attack damage (2,048) are lifted** (the user's ask; `BodyCaps`, a mixin accessor on
`RangedAttribute`), on the server and the client alike. The pool refills in some 40 s as Four Axis's does, and flight and the
sword never outrun the gathering. Breaking from Four Axis Grand Perfection into Dao Integration waits on the last axis of
virtue, Enlightenment (each axis holds its own stage's breakthrough). The rest of the mod follows: the heaven methods (below,
Heaven methods), the ghost path to True Immortal (Ghost methods), tribulations of 200 to 3,200 strikes (Tribulations), the years
(Lifespan: a True Immortal's without end; the talent race's shares, above), their few cultivators (Sects and cultivators), the
Netherworld's yin borne twice as long each realm, the Inner Realm's sky turning to the void's violet and then to gold, the
Quasi- names ("Quasi-Dao Integration"; only past True Immortal Grand Perfection "beyond"). No new spells or abilities yet.

**The top-left corner** (the user's asks, 2026-10-22, to pictures of theirs, in the mod's own style): with bodies of thousands of
hearts, vanilla's hearts at the bottom are held to **one row of ten** (`client/mixin/GuiHeartsMixin`: each heart a tenth of the
whole once past twenty; the armour row sits over it as for a body of twenty), and the top-left corner is a **portrait** of you in a
lacquer frame trimmed in antique gold, your name small above it, with **two thick bars** beside it (`client/VitalsHud`): your
**health** and your **qi**, each in the Qi bar's gold frame with its scroll roller and tapered tip, what it holds against its
maximum written across it ("1,407/1,407"). The health crimson, lit from above, notched at the quarters, a blow's pale trail
draining after the fill, absorption a band of gold along its top (its amount after the health, in gold); the qi the mod's blue, a
glint flowing along it while it fills (not in a Qi Flight that costs more than gathering brings in). Nothing of hunger, air or
armour up there (the user's word): those stay at the bottom, vanilla's. The health shows when vanilla's hearts do (not in creative
or spectator), the qi once your meridians open; under them the thin bars go on (a soul's own pool in another's body, the ghostly
qi, yin corruption, the readied spell and ability: `QiHud`, `NetherworldHud`), and under those the pressure mark and a projected
soul's readout.

**Swift** (the user's ask, 2026-10-22: "void shattering beings and onwards should be able to travel through space or just in
general super fast"). From Void Shattering on, Qi Flight and the free flight of the dead and of a Ghost King's form are far
faster, the more with every stage (`QiFlight#realmSpeed`): Void Shattering 2.4 to 3 times vanilla's flying speed, Sacred Vessel
3.2 to 3.8, Entering Nirvana 4 to 4.9, True Immortal 6 to 7.2 (some 27 to 80 blocks a second, twice that sprinting; Wandering Soul
Steps' +60% on top for the dead). Space is swifter still (The Astral Realm: to the next star, Void Shattering half a minute, True
Immortal five or six seconds). A sword stays a sword's speed. No lower realm holds them back (below), so they fly as swiftly
there. Vanilla snaps back anyone but the game's host who moves more than 10 blocks in a tick; a
flier may now move as far as its flight carries it, a second of it for packets that come in bunched (`QiFlight#moveAllowanceSq`,
`ServerGamePacketListenerImplMixin`), so a guest isn't pulled back mid-flight. Unexplored land loads only as fast as the server
makes it: at the top speeds over new land the client waits at the edge of what has loaded (space has none to wait for).
NPC cultivators fly as before.

**No lower realm holds Void Shattering** (the user's ask, 2026-10-22: "once reaching void shattering realm the lower realm no
longer supresses you"). From Void Shattering on, on either path, Realm Suppression in the Overworld, the Nether and the End holds a
cultivator no longer: its whole health, strength, qi pool, abilities and flight there (`PlayerCultivation#FREE_OF_LOWER_REALMS`,
`#isSuppressed`, `#sustained`, `#wielded`; an NPC cultivator the same, `CultivatorNpc#sustainedRank`). It meditates there too, at the
lower realms' thin qi, up to its stage's full bar; the breakthrough itself still waits for the Upper Realm (`#isBreakthroughLocked`).
Four Axis and Dao Integration are held to Heavenly Being Grand Perfection there as before.

**Mending** (the user's ask, 2026-10-22: the great bodies healed at vanilla's two a second, hours to mend). Natural regeneration
goes by the realm's body (`CultivationStats#mendFactor`, `mixin/FoodDataMixin`): every heal vanilla gives a fed body is multiplied
by the realm's body over 60, so with Qi Sustenance on a body of any realm mends from nothing to whole in about **30 seconds**
(the first realms' small bodies keep vanilla's quicker pace): Four Axis Grand Perfection about 4 times vanilla's, Void Shattering
some 30, a True Immortal over 1,000. Its food costs stay vanilla's. The dead's mending in the Netherworld goes by it too
(`GhostSystem`), and an NPC cultivator at rest mends its whole body in 30 s (`CultivationStats#mendPerSecond`; 2 a second at
least, as before). It runs in tribulations, the Spatial Gap and a star's heat too (the user's word: "its okay if they become
easier"), so from Nascent Soul up those are gentler than their tables say.

## Tribulations

A breakthrough into a new realm (and, from Nascent Soul, into every stage) brings the **Heavenly Tribulation**: a storm
gathers over you and strikes a set number of times (`TribulationManager#strikes`; the user's counts of 2026-10-20, Heavenly Being
50 and Four Axis 100, the realms below in step; each realm after Four Axis twice the one before, 2026-10-22):

| Into | Strikes | Apart | What lives through it (and the step below that dies) |
|---|---|---|---|
| Foundation Building | 2 | 3 s | nothing needed at full health |
| Core Formation | 6 | 3 s | iron (leather dies) |
| Nascent Soul, its Mid | 18 | ~1.5 s | diamond, or iron with Protection II (plain iron dies) |
| NS Late | 18 | ~1.5 s | diamond with Protection I, or iron with Protection II (plain diamond dies) |
| NS Grand Perfection | 18 | ~1.5 s | diamond with Protection II (Protection I dies) |
| Heavenly Being, its Mid and Late | 50 | 0.53 s | diamond with Protection III (II dies) |
| HB Grand Perfection | 50 | 0.53 s | Protection IV (III dies) |
| Four Axis, its Mid and Late | 100 | 0.26 s | Protection IV and Resistance I (Protection IV alone dies) |
| FA Grand Perfection | 100 | 0.26 s | Protection IV and Resistance II (Resistance I with Regeneration I dies) |
| Dao Integration, every stage | 200 | 0.13 s | Protection IV and Resistance II (Resistance I with Regeneration I dies) |
| Void Shattering | 400 | 15 a second | the same, against its own body's might |
| Sacred Vessel | 800 | 31 a second | the same |
| Entering Nirvana | 1,600 | 62 a second | the same |
| True Immortal | 3,200 | 123 a second (six a tick) | the same |

**No tribulation lasts more than 30 seconds** (the user's ask, 2026-10-22; `TribulationManager#MAX_TICKS`, but the lifespan
tribulation): its clouds gather for 3 s, its strikes come 3 seconds apart while they fit, else all of them fitted evenly into
the 26 seconds before the breakthrough's last second (`#strikeSpan`, `#strikesBy`), several a tick for the greatest; a Primordial
Chaos Body's extra strikes too, the faster (they used to take the longer). Each strike is weaker the more there are, retuned for
the new timing (a simulation of vanilla's damage rules, which gave the old table back exactly: scratchpad `trib-tune-1022b.ps1`)
so the same gear as before lives and the step below dies, with Qi Sustenance's regeneration on (at vanilla's 2 a second: since
2026-10-22 a body mends at its realm's pace, here too at the user's word, so from Nascent Soul up the table asks less than it says;
The realms after Four Axis, Mending); but at Four Axis Grand Perfection,
where in 30 s Regeneration I heals too little to tell one kit from another, Resistance II is wanted now, as in every realm after
it (Protection IV and a beacon are the best gear there is, so past Four Axis the body's own might bears the rest). Strikes in the
same tenth of a second share the bolt drawn (`MIN_BOLT_INTERVAL`), and only one bolt a second lights up the whole sky (the rest
don't, so a fast storm doesn't strobe). **The only lightning in
it is the strike that hits you** (the scattered bolts it once threw about for show are gone, the user's call 2026-10-19),
and the cloud brightens as each one lands. The storm grows with the realm you are breaking into (x1 at Foundation Building,
45% wider each realm after: `TribulationCloud#stormScale`), and its bolt grows as much **thicker**, a quarter of a realm's
growth more for each stage within the realm (`TribulationManager#boltScale`): vanilla's own at Foundation Building, x1.45 at
Core Formation, x1.9 to x2.24 through Nascent Soul's stages, x3.14 at Four Axis Grand Perfection, on to x5.39 at True Immortal's
(its storm some five times Foundation Building's across). Only its thickness: its
zig-zag stays vanilla's (`TribulationLightningRenderer` draws vanilla's bolt from the same seed, every layer that much wider;
scaled whole, the great tribulations' bolts sprawled across the sky). The Spatial Gap's storms are their own and unchanged.

**The Netherworld's tribulation** is the ghost path's (the user's ask, 2026-10-19; `TribulationManager#startGhost`): with the
ghost path drawn in (the Cultivation tab's Path button; the dead always), the Breakthrough button at its bottleneck brings
it. It comes at the same breakthroughs as the heavens' (every Grand Perfection, every stage from Nascent Soul) with the same
strikes, the same bolt sizes, but of **yin lightning** from a **dark teal storm**, its bolt cyan, and it comes **wherever you
stand, the Netherworld included** (it is the dead's realm's, not the heavens'). Its bolt is turned by armour as the heavens'
is, and the yin in it passes armour as magic does (`ModDamageTypes.YIN_LIGHTNING`, `YIN_TRIBULATION`); it reaches the dead
**as it does the living** (the user's ask, 2026-10-19): each strike, after their armour, takes the **same share of their
hearts** as it would of a life's body on the same paths, both whole (`GhostSystem#livingShare`: x1 for one whose ghost path
carries its body, as a life's heaven path would; less for one carried by half its heaven path). The living take it whole,
so one on the ghost path alone faces it on half a body. One of the common dead, with no hands for armour, turns the bolt
as an iron set would (`TribulationManager.GHOST_FORM_ARMOR`, what the balance table asks of the living at Core Formation,
the last tribulation the common dead meet; a ghost path of Core Formation is a Ghost King's, who wears armour as the living
do). Survived, the ghost path is through to the next stage or realm, its bar empty, its methods' new
layers to settle in. Failed, **the ghost path falls a stage** and its cultivation there is lost: struck down (the living
die; the dead scatter and reform at the Gate) or leaving mid-way (marked and saved at once, so you fall as you come back).
A Heavenly Thunder Body drinks in only the heavens' lightning. The HUD names it "The Netherworld's Tribulation".

**A Heaven-Defying Body** (Physiques) is struck down by neither: a killing strike leaves it one heart and the tribulation fails,
and no failed tribulation of either kind, however it failed, costs it a stage, its cultivation or its breakthrough pill
(`TribulationManager#holds`).

## Name Your Obsession

Before the tribulation out of **Nascent Soul Grand Perfection into Heavenly Being Early** (the user's ask, 2026-10-19), on either
path (the heavens' tribulation, or the Netherworld's on the ghost path), the Breakthrough button brings up **"Name Your
Obsession"**: the one thing your heart will not let go of. Pick one of four (click it, or press 1-4), then **Face the
Tribulation** (or Enter): it is kept and the tribulation begins at once. Close the screen instead and nothing happens; the
Breakthrough button asks again.

| Obsession | Its boon | And at Four Axis |
|---|---|---|
| **Longevity** | lifespan x1.2 (all of it: the body's years, the realms', the years taken from the grateful) | the Axis of Longevity fills twice as fast (250 years) |
| **Fortune** | x0.8 on every price you pay in trade, on top of what regard does | the Axis of Fortune fills twice as fast (100 stones' worth) |
| **Merit** | +10 regard with everyone (every cultivator's baseline for you) | the Axis of Merit fills twice as fast (100 hearts) |
| **Enlightenment** | cultivation x1.2, every path's (meditation's gain, the methods' layers settling with it) | the Axis of Enlightenment fills twice as fast (3 stages) |

"Twice as fast": that axis needs half its measure (below; rounded up). It is **named once, and
for good**: one obsession for the cultivator, whichever path asked first, never named again; only a spent life loses it, with
everything else. A cultivator already past Heavenly Being Early when obsessions came in (an older save) is asked at their next
tribulation. The Stats tab shows it beside your years. Op: `/cultivation obsession` (which, and what it gives), `/cultivation
obsession set <longevity|fortune|merit|enlightenment|none>`. Code: `Obsession`, client `ObsessionScreen`,
`PlayerCultivation#obsession`; tests `ObsessionGameTests`.

## The four axes of virtue

Four Axis is not climbed on qi alone (the user's ask, 2026-10-19). Each of its stages has its **axis of virtue**, and the
breakthrough out of that stage, on either path, waits until the axis is complete. Each axis is filled by **its own measure**
(the user's ask, 2026-10-20), and every bit of it comes through the wishes of the lower realms' people:

| Stage | Axis | Its wishes | What fills it |
|---|---|---|---|
| Early | **Axis of Longevity** | the sick and fading: heal them with a quarter of your qi, then take what years they can spare | **500 years** taken from the healed (each year onto your own lifespan too) |
| Mid | **Axis of Fortune** | bare stores: bring what they lack (an item and a count), and take a share of their fortune (3-5 Mid-grade Spirit Stones, marked as shared with you) | **200 Low-grade Spirit Stones' worth** of those shares, absorbed |
| Late | **Axis of Merit** | something preys on them (below): take it on, and it comes out of the land 24-36 blocks off, glowing; slay all of it | **200 hearts**: each creature of a threat slain by your hand counts its hearts (max health, halved) |
| Grand Perfection | **Axis of Enlightenment** | a lesser cultivator stuck at its wall: sit in meditation within 6 blocks of it for a minute, and it breaks through a stage; or take its body (Soul Projection's possession) and meditate in it until it breaks through a stage yourself | **6 stages** guided |

The axis of your obsession (named before Heavenly Being, above) needs half: 250 years, 100 stones' worth, 100 hearts, 3 stages.
Only the axis you stand at fills, the first incomplete one, **in order**: no Fortune before Longevity is done, even at Grand
Perfection; nothing past what completes it counts. The axes are the cultivator's, shared by both paths and saved on the record;
a spent life loses them. The Cultivation tab shows the axis you stand at as a **bar** like the cultivation bar, in the axis's
colour (jade Longevity, gold Fortune, vermilion Merit, pearl Enlightenment), how far it stands written on it in its measure
("Axis of Longevity: 230 / 500 years"), framed in amber at the bottleneck with the axis incomplete; then the tab (under the
bar), meditation and the Breakthrough button say how it is filled. A save from before the measures (100 virtue an axis) comes
over as far along in each, a complete axis still complete.

**The Axis of Longevity's years.** Healed, the grateful offer what years they can spare (20-100 for a mortal, more for a
cultivator: 40 more for every realm of theirs). Take none, a quarter, half or all of them: what you take fills the axis and goes
onto your own lifespan, and only while you stand at this axis (no wish of Longevity is yours to grant otherwise). The more you
take, the less they think of you (taking none is remembered warmly, and counts for nothing).

**The Axis of Fortune's stones.** A settlement's share of its fortune is marked as shared with you (its tooltip says so).
Absorbed by you, by holding use or by meditating it away, while you stand at the Axis of Fortune, each counts its worth (a
Mid-grade stone 9), even with your qi pool full (the stone's qi goes where there is room). Stones of no wish, or a share given to
someone else, fill no one's axis; crafted into other grades they lose the mark.

**The Axis of Merit's threats** (`Wishes.Foe`; each settler says which preys on them):

| Threat | What comes |
|---|---|
| A great beast | a ravager |
| Raiders on a beast | a ravager, a pillager riding it |
| A sorcerer | an evoker |
| Something of many faces | an illusioner |
| Raiders | two pillagers, two vindicators |
| A coven | three witches |
| The restless dead | four zombies (husks in deserts and badlands), two skeletons (strays in snow), helmed so the sun can't burn them |
| Riders of bone | three skeletons on skeleton horses (the riders are the threat) |
| A brood | three spiders, two cave spiders |
| A wolf pack | four wolves, angry at you |
| A demonic cultivator | a cultivator of the demonic path, Nascent Soul Early to Heavenly Being Late, out for you |

Each one of them that falls by your hand (your blow, arrow or spell, or your Earth Golem's) fills the Axis of Merit by its
hearts, as it stood before any Realm Suppress (a ravager 50, a pillager, vindicator or evoker 12, a wolf 4; a demonic
cultivator by its rank). The wish is granted when **every one of them is dead and at least one fell by your hand**. Slain only
by others (the sun, a fall, the settlers), or taken from the world some other way, the threat is gone but the wish stands, to be
taken on again. A threat leaves its spoils (its loot and gear, an evoker's totem) only when it falls
by your hand: slain by anything else it drops nothing, so letting it die and taking the wish on again farms nothing (the
2026-10-20 sweep). The **demonic cultivator** is out for you and you alone, whatever your path: it never
runs from your domain or a beating, never begs for its life, never goes off into seclusion or to ascend (a Heavenly Being of
the lower realms would), and never turns on the settlers. On **Peaceful**, where no monster lives, whatever preys on them comes
as a demonic cultivator (the wolves still come as wolves). A threat that dies
while its settler's land is unloaded counts when the settler is next about, even if you are offline by then. A threat left a day
lapses with its wish, and whatever of it is still about leaves too (any out of reach go when their land loads again).
Settlers don't go looking for fights, even with enemies of their path; they only defend themselves, and leave the threat to
whoever took it on. While a threat is out, **its settlement's people stay home** (the user's ask, 2026-10-20): each goes into its
hut (or to the well, with no hut standing) and keeps there, strolls, chats and meditating outdoors set aside, until all of it is
slain or the wish lapses; none of them fights the threat's creatures, or comes to your aid against them. You can still talk
with them there.

**Guidance from within.** Possess a settler who wishes for guidance and meditate in its body: when its cultivation breaks
through a stage (at the speed your soul lends it), the wish is granted, as if you had sat beside it. The settler keeps the stage
when you let go. It counts only if you could grant it (your own Four Axis, at the Axis of Enlightenment); otherwise the wish
waits for someone who can.

**Wishes** come from the settlements of the Astral Realm's worlds (below). Ask a settler under Ask, **"Their wish"**: anyone
hears it, and only a Four Axis cultivator at that wish's axis is offered the grant (anyone else is told why not). A mortal never
asks for guidance. A wish granted, the settler has another three days later. One taken on and left (a healed one's years never
taken, a threat that wandered off) lapses after a day, and a guidance after the guide leaves, and can be taken on afresh.

**The dead** fill their axes too (the user's ask, 2026-10-19): one of the dead whose **ghost path** is at Four Axis (a Ghost
King) grants wishes as the living do; the dead's heaven path counts for nothing here. A Ghost King may rise into the Astral
Realm (the common dead may not), but to the living it isn't there: the settlers (below Nascent Soul) see the dead only in a
Ghost King's **human form** (Form Shift), and one who comes to them in its true form is told so. In its human form any of the
living see it as one of their own. It heals out of its ghostly qi (none gathers in the Astral Realm, so come with a full pool),
the years it takes lengthen its lifespan as the living's do, and the beasts and the dead of the Merit threats don't fight back
against it (no mob of the living world hunts the dead), though a demonic cultivator, of Nascent Soul and above, sees it and does.

Code: `FourAxis`, `Wishes`, `SpiritStoneItem#fillsFortune`, `PlayerCultivation#axisProgress`/`isHeldByAxis`, `GhostMethods#heldByAxis`,
`Settlements#raiseAlarm`, `CultivatorGoals.ShelterGoal`; tests `FourAxisGameTests`.

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
and passives as it climbs, and open the way into new realms. There are **fifty-five**: one of each element for each realm, Qi
Refining to True Immortal (the twenty-five of the realms after Four Axis since 2026-10-22, the user's ask: their methods now, their
own spells still to come). Each has its own layers and a passive that grows with them, and teaches its element's **spells** of
its realm and every realm before it. The dead can't take one up (their meridians belong to the ghost path now), nor a mortal,
nor one whose roots won't take its element (see Physiques).

| Element | Dao Integration | Void Shattering | Sacred Vessel | Entering Nirvana | True Immortal |
|---|---|---|---|---|---|
| Wood | Myriad Woods Return to Dao Scripture | Void-Rooted Divine Tree Canon | Jianmu Sacred Vessel Sutra | Withering and Flourishing Nirvana Classic | Evergreen Immortal Sovereign Canon |
| Water | Hundred Rivers Merging Dao Scripture | Abyssal Void Tide Canon | Sealed Ocean Vessel Sutra | Frozen Moon Nirvana Classic | Primordial Waters Immortal Canon |
| Fire | One Flame Dao Scripture | Void-Burning Fire Lotus Canon | Heaven Furnace Vessel Sutra | Undying Ember Nirvana Classic | Primal Flame Immortal Canon |
| Earth | Ten Thousand Mountains Return to One Scripture | World-Breaking Ground Canon | Nine Cauldrons Sacred Vessel Sutra | Buried World Nirvana Classic | Houtu Immortal Canon |
| Metal | Myriad Swords Return to the Dao Scripture | Void-Splitting Sword Canon | Immortal-Forging Vessel Sutra | Sword Heart Nirvana Classic | Heaven-Slaying Immortal Sword Canon |

Their layers past Four Axis strengthen the element's passive as the realms' own might grows (`HeavenPassives#PER_LAYER`: the
healing, the qi and the edge with the realms' health, gathering and strength, Void Shattering's and True Immortal's leaps with
them; the fury and the ward a little a realm), so a True Immortal method's last layer heals some 1,200 a second, gathers some
10,150 qi more, swings 63% faster, turns 47.7% of a blow and edges a blow with some 6,840 more.

**Layers.** A method of a realm spans **Qi Refining up to that realm**, nine layers a realm (`MethodLayers`, the same layout as
the ghost foundations): a Qi Refining method has 9 layers, a Four Axis method 54, a True Immortal one 99. Within each realm's nine, layers 1-2 stand
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

  | Where | Qi Refining | Foundation Building | Core Formation | Nascent Soul | Heavenly Being | Four Axis | Dao Integration | Void Shattering | Sacred Vessel | Entering Nirvana | True Immortal |
  |---|---|---|---|---|---|---|---|---|---|---|---|
  | The Overworld | 600 | 250 | 60 | 8 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
  | Its rare lands (cherry groves, mushroom isles, ice spikes, eroded badlands, the deep jungles) | 300 | 250 | 150 | 40 | 3 | 0 | 0 | 0 | 0 | 0 | 0 |
  | The Upper Realm | 0 | 40 | 150 | 100 | 40 | 10 | 2 | 0 | 0 | 0 | 0 |
  | Its wondrous lands (Dense Qi Peaks, Thunder Peaks, Ancient Sword Graveyard, Spirit Forest, Peach Blossom Sanctuary) | 0 | 0 | 100 | 100 | 60 | 25 | 6 | 2 | 1 | 1 | 1 |

  The realms after Four Axis are carved rarer and rarer, as the user would have their cultivators (below, Sects and
  cultivators); their few cultivators teach and sell their own realm's method too.

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

**Spells.** Every element has its own spells in every realm: **three** in Qi Refining and Foundation Building (at the 2nd, 5th
and 8th layers of the realm, its Early, Mid and Late), **two** from Core Formation on (the 3rd and 7th, Mid and Late), seventy in
all. A spell belongs to its element and realm, not to one method: any heaven method of that element **of that realm or
deeper** teaches it once it is understood to the spell's layer (and your cultivation has reached the stage the layer stands
for), so a Four Axis fire method carries every fire spell. The Spells tab shows each one locked with both. They go on the spell
wheel (V) as they come, never the ability wheel, cost heaven qi and have cooldowns. A spell that needs something to take hold
of (a being looked at, the ground, earth to draw on) isn't cast without it, and nothing is paid. Their qi harm is the
element's own and passes armour; none touches its caster, spectators or its tamed creatures, and nothing of one (harm, hold,
push, drowning, burning) touches another player unless the server lets players harm each other (PvP on, teams allowing);
blessings like Life Force Gain still reach them. Terrain a spell carves or tears up is gone for good, the ground and whatever
is built on it alike (never a chest or anything else holding something, nor obsidian or anything harder), but nothing inside a
raised barrier (protective or concealment) is touched, nor the ground its shell runs through by anyone weaker than it; and
spawn protection and Adventure mode are respected.
Every one has its own models, animation and effects (nothing is only Minecraft's own particles), and blocks a spell sets down
(walls, trees, ice) are taken up again in time; broken or blown up before then they simply vanish, dropping nothing.
Every cast blooms its element's **seal** under the caster (its character, 火 水 木 土 金, within rings of trigrams and the star
of the five), turning, motes rising off it, from Core Formation with a pillar of its light; a spell cast at something also
raises a smaller seal upright before the chest. The great spells work in their seals too (a domain's, a field's, the mark of
what will fall, the seal a beam or jet comes out of), wards turn their seal at their bearer's feet, and the light of each
element (flame, water, wood's green, earth's amber, metal's gold) glows through its missiles, fields and blows.

**The five elements' cycle.** Fire overcomes metal, metal wood, wood earth, earth water, water fire. Some spells are **wards**
(Azure Coating water; Hardened Body and Mountain Stance earth; Crimson Tempering and Flame Armour fire; Ancient Bark Skin wood;
the Golden Bell and Vajra Body metal): a ward stops a spell of the element it overcomes outright (or half of one from a deeper
realm), and a spell of the element that overcomes it comes through a quarter harder. Wooden missiles burn to ash on a fire ward;
water puts out what burns.

**Qi Refining**

| Element | Spell (layer) | What it does |
|---|---|---|
| Fire | **Flame Bolt** (2) | Shoots a bolt of red flame from your hand at where you aim, up to 40 blocks. It strikes for 5 fire harm and sets its mark burning for 3 s. Water puts it out: a being under a water ward (Azure Coating) takes none of it. |
| Fire | **Flame Fist** (5) | For 15 s your fists burn with red and orange flame. Every blow you land sets its mark burning for 5 s and sears it for 1 more fire harm. Against a being under a water ward the flames only hiss out. |
| Fire | **Crimson Tempering** (8) | For 30 s fire and lava cannot burn you, and lava bears your weight: you can walk and run across it. A fire ward. |
| Wood | **Wood Bullet** (2) | Grows a sharp wooden stake in the air before you, then shoots it at where you aim, up to 40 blocks: 5 wood harm and a small knock back. Against a fire ward (Crimson Tempering, Flame Armour) the stake burns to ash. |
| Wood | **Emerald Healing Art** (5) | For 10 s you heal 1 health every second, fed or hungry, and glow a faint green. |
| Wood | **Verdant Growth** (8) | A tree grows up out of the ground under your feet in two seconds, lifting you 14 blocks into the air on its trunk, and spreads a blossoming crown round you at the top. It stands for 60 s, then withers away; its wood and leaves give nothing when broken. Cast it standing on the ground. |
| Earth | **Earth Spear** (2) | Raises clods of earth into the air before you, where they fuse into a podao of dried earth, then flings it at where you aim, up to 40 blocks: 6 earth harm and a heavy knock back. |
| Earth | **Hardened Body** (5) | For 30 s a thin skin of stone covers you, taking 7% off all harm you take (14% off water's). An earth ward. |
| Earth | **Dirt Tide** (8) | Raises a wave of the earth you stand on and carries you on its crest for 10 s, the way you face, faster than a sprint: forward drives it on, back reins it in, and it climbs a block at a time and rides over water. Sneak to step off. |
| Water | **Water Arrow** (2) | Forms an arrow of swirling water in the air and shoots it at where you aim, up to 40 blocks: 5 water harm, and whatever burns on its mark is put out. Strong against fire wards. |
| Water | **Azure Coating Technique** (5) | For 30 s a thin coat of water clings to you: fire cannot burn you and fire spells stop at it (a fire spell of a deeper realm than this still breaks half through). Lava boils it away. A water ward. |
| Water | **Azure Breathing Technique** (8) | For 60 s you can walk and run on water (sneak to sink into it) and breathe under it. |
| Metal | **Flying Sword** (2) | Forms a sword of metal in the air before you, shard by shard, and shoots it at where you aim, up to 48 blocks: 6 metal harm, and it flies on through its first mark to strike another. |
| Metal | **Blade Polish** (5) | For 30 s every melee blow you strike deals 1.2 times its harm, and the weapon in your hand gives off a faint white glow and wisps of white smoke. |
| Metal | **Steel Hurricane** (8) | You dash the way you look with your weapon thrust before you, spinning like a trident's riptide, for 1.6 s: every being you meet takes your weapon's blow and 3 metal harm more. No fall harm from it. |

**Foundation Building**

| Element | Spell (layer) | What it does |
|---|---|---|
| Fire | **Remote Yang Bomb** (11) | Leaves a ball of yang fire on the surface you look at, within 16 blocks. Only you, and those with Qi Sense open, can see it. While it waits it draws 1 qi a second from you. Cast the spell again within 48 blocks to set it off: 14 fire harm to everything within 4 blocks, burning for 4 s, and a blast that throws them back. Setting it off costs nothing; the spell then rests for 15 s. It gutters out if your qi runs dry or you go far from it. |
| Fire | **Flame Armour** (14) | For 30 s you are clad in armour made of red flame, from its burning crest to its greaves. It takes 10% off all harm you take, and 20% off metal's (metal spells, and blows of iron, gold, diamond or netherite weapons). A fire ward. |
| Fire | **Fire Serpent** (17) | A serpent of fire coils out of your hand and swims through the air at where you aim, bending toward its mark as it goes, up to 48 blocks: 12 fire harm and burning for 4 s. |
| Wood | **Life Force Drain** (11) | For 6 s you draw the life out of the being you look at or a herb growing there (a ginseng), within 12 blocks, or the years out of a single Cultivation Fruit or herb you hold (a fruit on its tree can't be drawn on). A being loses 1.5 health a second, and it heals you; another cultivator (a player) loses 10 years of their lifespan a second with it, half of them yours (2026-10-21). A herb or fruit loses 60 years a second, and you heal 1 a second. A stream of emerald light runs from it to you. It breaks if you stray 16 blocks apart. Herbs in a sect's or a Ghost King's treasury can't be touched while its seal holds. |
| Wood | **Life Force Gain** (14) | For 6 s you pour your own life into the being you look at, or into an aging herb or fruit growing within 12 blocks, or a single one you hold. A being heals 2 a second for 1 of your health a second (never below 4); a herb or fruit grows on your own years (2026-10-21): 120 years a second for 10 years of your lifespan a second, never your last 10. Herbs in a sealed treasury can't be touched. |
| Wood | **Mysterious Bristle Prison** (17) | Thorny bristles burst up out of the ground round the being you look at, within 20 blocks, and close over it: it is held where it stands for 4 s, torn for 1.5 wood harm a second. Three blows on the cage break it, from outside or in; but each one the prisoner strikes from inside tears it for 2 more. |
| Earth | **Earth Wall Technique** (11) | A wall of the ground you stand on, 6 blocks long and 4 tall, rises out of the earth 3 blocks ahead of you, across your way. If you are not on the ground, it forms in mid air, from its middle block outward. It stands for 30 s, then sinks back into the ground. |
| Earth | **Boulder Throw** (14) | Tears a boulder out of the ground ahead of you, leaving a round hole two blocks deep, gathers it in the air and throws it at where you aim, up to 48 blocks: 16 earth harm and a heavy knock back. It tears it from whatever lies ahead, ground or what is built there (not inside a barrier). |
| Earth | **Cave Formation** (17) | Carves a cave fit for one into the slope or ground you look at, within 6 blocks, crumbling it away before your eyes over 3 s: into a slope, an entrance one block wide and two tall with a room three blocks wide, deep and tall behind it; into the ground, three steps down to such a room. |
| Water | **Rain Call** (11) | Calls a rain cloud of your own over where you stand, 8 blocks round, for 20 s. Everyone under it but you is slowed by a tenth, and the rain puts out fires and whatever burns. |
| Water | **Water Prison** (14) | Encases the being you look at, within 16 blocks, in a sphere of water for 4 s. The air is drawn out of it at once: it drowns from the first second (no held breath), takes 2 water harm a second besides, and is held where it is. 30 harm struck on the sphere bursts it, the prisoner's own blows from inside too. |
| Water | **Torrential Spring** (17) | Gathers a great ball of water out of the air above and ahead of you over 1.5 s, then drops it on where you look, up to 32 blocks: 8 water harm to everything within 4 blocks, a heavy wash that throws them back, and a small lake pressed into the ground where it falls. |
| Metal | **Boulder Cleave** (11) | Flings a crescent of sword qi from you the way you look. It flies 15 blocks through the ground as through air, cutting a slot 3 blocks wide and 2 tall through everything it passes (what it cuts is gone), and cleaves every being in its way for 14 metal harm. |
| Metal | **Mineral Purification** (14) | Every ore within the reach of your consciousness (your Consciousness Domain's radius) gives up its ore: it streams to you as light and is yours, as mined and unsmelted, the rock it lay in left behind. At most 256 ores, the nearest first. Ores inside a raised barrier (protective or concealment) are beyond its reach. |
| Metal | **Advanced Flying Sword Technique** (17) | Six swords form in the air round you one after another, then fly one after another at where you look: 9 metal harm each, each passing through one being to strike another. |

**Core Formation**

| Element | Spell (layer) | What it does |
|---|---|---|
| Fire | **Crimson Swirl** (21) | A great tornado of fire rises 3 blocks ahead of you and creeps toward where you aim (up to 40 blocks away) for 10 s. It drags in and lifts whatever comes near, burns all within it for 6 fire harm a second, tears up the ground beneath it (and what is built there) and leaves fire behind. |
| Fire | **Solar Field** (25) | A small sun burns 8 blocks above you for 10 s. Everyone else within 40 blocks of you is set burning, every second, as if standing in fire (a water ward or a fire-proof body excepted). |
| Wood | **Thousand Leaf Storm** (21) | Sixteen razor leaves stream from your palm over a second, each bending a little toward a foe: 2.5 wood harm each, every one its own cut. Against a fire ward they burn to ash. |
| Wood | **Ancient Bark Skin** (25) | For 20 s ancient bark grows over you: 20% less harm taken, 40% less of earth's. A wood ward. |
| Earth | **Earth Dragon Surge** (21) | A ridge of jagged spikes of the ground's own rock bursts up along the ground from you toward where you face, 24 blocks out, running out as fast as a horse: whatever it passes over takes 20 earth harm and is thrown into the air. The spikes sink back a few seconds later. |
| Earth | **Mountain Stance** (25) | For 15 s you stand like a mountain, clad in slabs of stone: 25% less harm taken (50% less of water's), and nothing can knock you back, but you walk a fifth slower. An earth ward. |
| Water | **Tidal Lance** (21) | A jet of water bursts from your hands for 2 s, as far as 24 blocks: the first being in it takes 6 water harm every half second and is driven back along it. |
| Water | **Mist Veil** (25) | A thick mist rises 10 blocks round you for 15 s, moving with you: foes in it lose sight of you and your allies (unless within 3 blocks of them), and missiles flying into it slow to half their speed. |
| Metal | **Sword Rain** (21) | Twelve swords form in a ring high over where you look, up to 32 blocks, and plunge down round it: 6 metal harm each. |
| Metal | **Golden Bell Shield** (25) | A great golden bell comes down over you for 10 s and takes up to 40 harm for you, tolling with each blow; wood's harm costs it half as much, fire's twice. A metal ward. |

**Nascent Soul**

| Element | Spell (layer) | What it does |
|---|---|---|
| Fire | **Karmic Fire** (30) | Sets karmic fire on the being you look at, within 32 blocks, for 8 s, burning away its sins: each second it takes 1 harm if it is guiltless, up to 11 for a devil (by its alignment), 6 for a monster, a little for a beast or a villager. A sinner's alignment burns 2 a second toward neutral. |
| Fire | **Blazing Meteor** (34) | Calls a burning meteor down out of the sky onto where you look, up to 48 blocks, its mark glowing on the ground as it falls: 45 fire harm to everything within 6 blocks, burning for 6 s, and a crater 6 blocks round blown out of the ground (or whatever is built there), its floor burnt to magma and blackstone, fires round it. |
| Wood | **Thorn Spear Forest** (30) | Spears of thorny wood burst out of the ground 6 blocks round where you look, up to 32 blocks: every foe there takes 35 wood harm and is held where it stands for 2 s. |
| Wood | **Lotus of Rebirth** (34) | A great lotus opens at your feet for 10 s: you and every friendly being within 10 blocks are cleansed of harmful effects and fire at once, and heal 3 health a second while it blooms. |
| Earth | **Summon Golem** (30) | Raises an earth golem out of the ground beside you for 90 s. It flies (at three fifths of a cultivator's flight), keeps by you and fights for you: whatever strikes you or you strike, or a monster that comes for you. 80 health, 18 earth harm a blow. Two at most: a third sends the oldest back to the earth. |
| Earth | **Earthquake** (34) | The ground shakes 20 blocks round you for 4 s and splits: nine fissures up to 5 blocks deep run out from near you to its edge, and every foe on the ground takes 14 earth harm a second, is slowed hard and thrown about. |
| Water | **Glacial Spears** (30) | Five spears of ice form over your head and fly in a fan at where you look: 11 water harm each, slowness for 3 s and the cold's frost. |
| Water | **Great Tide** (34) | A wave 10 blocks tall and 14 wide rolls out 30 blocks the way you face: whatever it overtakes takes 30 water harm, is swept along hard and put out. |
| Metal | **Piercing Sword Light** (30) | A beam of sword light pierces everything along your look for 64 blocks, walls and hills too: it bores a tunnel 3 blocks across through whatever stands in its way, and every being along it takes 55 metal harm. |
| Metal | **Vajra Body** (34) | For 12 s your skin turns to gold: 35% less harm taken (70% less of wood's), nothing can knock you back, and whoever strikes you in melee takes 4 metal harm. A metal ward. |

**Heavenly Being**

| Element | Spell (layer) | What it does |
|---|---|---|
| Fire | **Phoenix Descent** (39) | A phoenix of fire takes wing from you and flies at where you aim, bending toward its mark, up to 64 blocks, and bursts there: 90 fire harm to everything within 5 blocks, burning for 6 s, and a scorched crater 4 blocks round. |
| Fire | **Sea of Flames** (43) | The ground 20 blocks round where you look, up to 48 blocks, becomes a sea of flame for 10 s: the land itself burns (grass scorched, patches turned to magma and netherrack, fires on whatever stands there, trees catching), and every foe standing in it takes 20 fire harm a second. |
| Wood | **Heaven-Piercing Bamboo** (39) | Giant bamboo bursts up out of the ground under as many as 6 foes within 24 blocks of where you look, breaking the ground open: 70 wood harm each, and they are thrown high into the air. |
| Wood | **Spring of Myriad Life** (43) | A green rain of life falls 32 blocks round you for 10 s: you and every friendly being in it heal 40 health at once, then 4 a second. |
| Earth | **Falling Mountain** (39) | A mountain of rock forms in the sky over where you look, up to 64 blocks, and falls on it: 140 earth harm to everything within 10 blocks, a shockwave, a crater 12 blocks round and 7 deep (its floor broken rubble), and seven fissures splitting the land out to 26 blocks. |
| Earth | **Heavy Earth Seal** (43) | Every foe within 24 blocks of you is pressed to the ground for 6 s: it cannot jump or fly (a flier is brought down), is slowed hard, and takes 20 earth harm a second. Under its weight the land 24 blocks round you sinks by up to 2 blocks, cracked and split by ten fissures (all but where you stand). |
| Water | **Heavenly River Torrent** (39) | A river pours out of the sky along a line from you to where you look, 32 blocks, for 2 s: everyone in its fall takes 60 water harm and is swept along it, and the river scours a channel 3 blocks across into the ground along its line. |
| Water | **Frozen Field** (43) | The cold runs out from you to 64 blocks round over two and a half seconds: open water freezes, frost lies on the ground within 32 blocks, and every being it reaches (but your allies and pets) is frozen solid for 3 s. The ice thaws over the next 20 to 40 s, and until it has, every being in the field takes 3 water harm a second. |
| Metal | **Mountain Cleave** (39) | Flings a crescent of sword qi as tall as a tower the way you look. It flies 140 blocks, cutting a slot 4 blocks wide and 16 tall through hills and mountains alike (what it cuts is gone), and cleaves every being in its way for 150 metal harm. |
| Metal | **Sword Domain** (43) | Thirty-six swords circle you 10 blocks round for 12 s: every foe within takes 20 metal harm every half second. |

**Four Axis**

| Element | Spell (layer) | What it does |
|---|---|---|
| Fire | **Nine Suns Descend** (48) | Nine suns fall out of the sky, one after another, onto where you look and round it, up to 64 blocks: each bursts for 150 fire harm 7 blocks round, burning for 6 s, and blows a scorched crater 6 blocks round out of the ground: together they leave the land a burning field of craters 16 blocks round. |
| Fire | **Phoenix Nirvana** (52) | For 60 s the phoenix's fire waits in you: if you would die, you rise again instead at full health, harmful effects burnt away, in a burst of flame that strikes everyone but your allies within 10 blocks for 200 fire harm. Once. |
| Wood | **Myriad Vine Binding** (48) | Vines burst up and seize as many as 12 foes within 40 blocks of where you look, up to 64 blocks: each is held fast for 5 s and takes 22 wood harm a second. |
| Wood | **World Tree** (52) | The World Tree grows out of the ground where you look (or before you), its roots heaving the land open in eight fissures, and towers over all for 12 s. Its vines reach from its boughs for every being but you within 32 blocks of its trunk (your pets spared), lift them into its branches and hold them there, drinking 10 of their health a second into you. |
| Earth | **Earthen Titan** (48) | Raises an earth titan three times a golem's size beside you for 60 s: it flies, keeps by you and fights for you, 400 health and 80 earth harm a blow, its fist striking the ground round its mark too. One at a time. |
| Earth | **Mountain Seal** (52) | A mountain of stone bursts up over where you look, up to 48 blocks, 7 blocks round and 9 tall, tearing the land open round its foot in nine fissures out to 18 blocks: everything inside is entombed, held fast for 5 s and crushed for 60 earth harm a second. The mountain crumbles away after 20 s; its stone gives nothing. |
| Water | **Absolute Zero** (48) | A beam of utter cold freezes the being you look at solid, within 48 blocks: 200 water harm, and it is held frozen for 5 s. When anything strikes it, or the cold lets go, it shatters for 60 more. |
| Water | **Boundless Sea** (52) | A sphere of the sea 16 blocks round fills the place you look at, up to 48 blocks, for 8 s: every foe inside is held in it, drowning, and takes 22 water harm a second. When it lets go, the ground under it is left crushed into a basin 10 blocks round with a lake in it. |
| Metal | **Heaven-Severing Sword** (48) | A sword of light as long as a tower forms in your hand and you swing it level across your front: a giant crescent of sword light some 110 blocks across leaves its edge and flies out level from your chest the way you face, 120 blocks, faster than anything can flee it: nothing stops it. It cuts through whatever its blade passes through, 10 blocks thick at its middle and thinner toward its tips: hills and mountains are sliced through (their tops left standing above the cut), trees felled, whatever is built in its way cut in two, and the land at its far end is cut in the crescent's curve. Every being its blade passes through takes 300 metal harm and is hurled ahead of it. |
| Metal | **Four Swords Array** (52) | Four giant swords plant themselves at the corners of a square 32 blocks across round where you look, up to 48 blocks, each driven deep into the ground in a crater 4 blocks round: for 8 s every foe inside is held in by the walls of light between them and takes 80 metal harm a second. |

Each spell's description in the Spells tab gives its numbers; the constants at the top of its element's class set them. Harm
grows by realm: about 5 at Qi Refining, 8-16 at Foundation Building, 16-30 at Core Formation, 30-60 at Nascent Soul, 60-150 at
Heavenly Being and 150-300 at Four Axis; qi costs likewise (8-20 at Qi Refining up to 1,000-2,000 at Four Axis).

**Wounds in the land.** From Nascent Soul up the great spells leave the land the way a walking disaster would: meteors, suns,
phoenixes and falling mountains blow out craters (burnt to magma and blackstone, or broken to rubble), quakes and seals split it
in fissures and sink it, the Heavenly River scours a channel, the Boundless Sea leaves a lake in a crushed basin, Piercing Sword
Light bores a tunnel, Mountain Cleave cuts a slot through hills, and the Heaven-Severing Sword's crescent, some 110 blocks across,
slices through whatever its blade passes through for 120 blocks: a slit 10 blocks tall at its middle and thinner toward its tips,
at the caster's chest, through hills and mountains (their tops left standing), its far end cut in the crescent's curve
(`MetalSpells#severThickness`, `#severLag`; the client draws it to the same numbers). Its way is asked to load as it is cast and
kept loading just ahead of it, since 120 blocks (and its tips) run past the usual simulation distance; land that was never made before is made
meanwhile, and the slash waits at its edge until it is. Every block goes by the same rule as the rest of the spells' terrain harm
(`HeavenSpells#carvable`: nothing that holds items, nothing harder than obsidian, nothing a raised barrier shelters, nothing the
caster couldn't dig) and is gone for good, dropping nothing; a few pieces are flung off as debris. A big wound is cut over
several ticks, nearest its heart first, 2,500 blocks a tick for all of them together (`Devastation`), so no tick has to take it
all. Water and wood were brought down beside metal and earth: Frozen Field reaches 64 blocks (was 100) and holds for 3 s,
Absolute Zero holds 5 s and shatters for 60, Boundless Sea and Water Prison hold for less, the World Tree drinks 10 a second for
12 s, Myriad Vine Binding and Heaven-Piercing Bamboo strike fewer and softer, Lotus of Rebirth and the Spring of Myriad Life heal
less.

**Commands (op):** `/heavenmethod learn <id> [layer]`, `/heavenmethod progress <amount>` (that much settling on the practised
method, no further than the realm), `/heavenmethod forget <id>`.

Code: `HeavenMethod` (the thirty, their ids and realms), `HeavenState` (what you know, saved), `HeavenMethods` (who, how far
understood, settling, the gate, the rate, learning), `HeavenPassives`, `MethodLayers`, `Spell` (the seventy, by element, realm
and layer), `HeavenSpells` (the kit: who is spared, targets, what lingers, temporary blocks, the elements' qi harm), `SpellAura`
and `SpellAuras` (wards and auras, synced), `FireSpells`, `WoodSpells`, `EarthSpells`, `WaterSpells`, `MetalSpells`, the spell
entities `SpellProjectile`, `SpellField`, `SpellBlock`, `EarthGolem`, `DirtTide`, `SpellFx` (passing moments) and
`Devastation` (craters, fissures, channels, the slash's canyon; budgeted); client
`SpellRender`, `SpellModels`, `SpellProjectileRenderer`, `SpellFieldRenderer`, `SpellBlockRenderer`, `SpellAuraLayer`,
`SpellAuraWorld`, `ClientSpellFx`, `HeavenLangText` (the methods' and spells' English). Tests: `HeavenMethodsGameTests`,
`ElementSpellsGameTests`.

## The Inner Realm

After **10 seconds of meditation** the screen fades to black and your soul turns inward: you open your eyes on your own
island in a void, while your body stays sitting where you were (other players see it, in your skin). Meditation inside is
**x1.5** as fruitful, and still gets the mats, pedestals, height and biome bonuses from around your body; suppression and
the Upper Realm's x10 also follow the body. The ghost path's meditation is x1.5 inside too, on the ghostly qi where the body
sits.

**The dead turn inward too** (the user's call, 2026-10-19), from anywhere, the Netherworld included (the living still can't
there): their ghostly form sits on where they meditated, in the dead's light (a Ghost King's crowned, seen as the dead are),
and a heart demon reaches them and they it, as it does the living. Nothing else about the island changes.

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
on it) and **click** to **select** that ability; **click it again**, **right-click**, or press the slot's **number** (1 at the
top, then clockwise, shown beside each slot) to switch it on or off now (or carry it out, for an act such as the Spatial
Tear); do as many as you like, then let go of the key to close it.
The middle names the ability under the pointer, whether it's On or Off, and "Selected" for the selected one. A slot's rim
is gold with a green lamp when its ability is on, grey and dimmed when it's off; the selected one is ringed in cyan, as the
spell wheel rings its readied spell.

**The selected ability** is switched by **X** ("Use Selected Ability", rebindable) without opening the wheel, the way C
casts the readied spell: Qi Flight on and off, the Spatial Tear opened, and so on. It's shown under the qi bar ("Ability:
Qi Sense On"), and each switch by key says "Qi Sense: On" above the hotbar once the server has done it. It's saved with
your cultivation; with none selected, X says how to select one. For a key of its own for any slot, bind the eight **Ability
Wheel Slot** keys in Controls (unbound at first). X and C are also vanilla's creative hotbar activators: Minecraft gives
each key to one binding only, so the mod's bindings are given presses on a key they share as well (`KeyMappingMixin`).

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

Code: `client/AbilityWheelScreen.java` (the held wheel), `client/QuickAbilities.java` (the selected ability, X and the slot
keys), `client/AbilityWheel.java` (icons, slot layout and drawing),
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
- **Changing worlds.** Flight is kept when you pass into another dimension however you go (a ghost crossing into the
  Overworld, a command's teleport): vanilla's teleport between dimensions doesn't resend what a player may do, so the client
  fell back to walking until something else resent it; `QiFlight` now resends it on every change of dimension.
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

From **Nascent Soul Early** on either path, a cultivator can send their nascent soul out of the body (born of the ghost path,
it is that path's: its strength, ghost qi for its costs and the ghost path's fall; see "The two paths"). Switch **Soul
Projection** on (ability wheel or Abilities tab) while standing on steady, dry ground with at least 10% of your qi: you sit in meditation for
**5 seconds** while the soul gathers itself (moving or being hurt breaks it off), then it rises out. Switch it again to
call it home: it is drawn back along its tether for **5 seconds** and you stand up where your body sat. A chat line marks
the day the soul forms, and the ability goes into the first empty slot of your wheel.

- **The body** stays sitting cross-legged where you left it, in your skin and armor, with your health and your armor's
  protection, **in the form you set out in**: set out in a Ghost King's form (or as one of the dead) and it sits on in that
  light, crowned for a Ghost King, seen as that form is (by everyone in the Netherworld and by the dead; elsewhere only by
  those who perceive souls, as clearly as they see a soul; always by you). You're told whenever it is hurt (a warning, and its
  health on screen), monsters nearby go for it, and if it is slain your soul dies with it. Players can only hurt it where
  they could hurt you (PvP).
- **The soul** is you, flying free of gravity through stone, wood and earth: it moves the way you look (look up and press
  forward to rise), jump and sneak lift and lower it, sprint doubles its pace, and it carries its momentum. It has its own
  hearts, **by the ghost path alone** (the user's call, 2026-10-19; `PlayerCultivation#soulHearts`): **1, and 3 more for
  every stage your ghost path stands past Nascent Soul Early** (10 at its Grand Perfection, 13 at Heavenly Being Early, 34 at
  Four Axis Grand Perfection). The heaven path adds none: a soul born of the heaven path alone sets out on 1 heart however
  high it stands (its tether still grows with it). They are all its own: constitution's points and your body's health count
  for nothing there. **Out of
  every body they never heal**, and what the soul loses it keeps: coming home you have the body's health, not the soul's,
  and the soul's wounds wait for it. It **mends only while it rests in a body**: home in your own (gathering to go out too),
  or in a being it possesses, from nothing to whole in **2 minutes** (`SoulProjection.SOUL_MEND_SECONDS`), so it sets out
  next time as whole as it has mended. While it's wounded, Soul Projection's description in the Abilities tab says how
  many hearts it has. A soul lost and formed again is born whole.
- **The tether**, a thread of bright blue light from your body's chest to the soul, lets it go **32 / 48 / 64 / 96** blocks
  through Nascent Soul's stages, 128-176 through Heavenly Being, 192-256 through Four Axis; it stretches against you as you
  near its end.
- **Qi**: half your realm's gathering a second at the body, rising to twice your gathering at the tether's end, and that
  much again while possessing (so clothing, pills and the Upper Realm's dense qi all help; there it costs nothing net).
  A Nascent Soul Early pool lasts about a minute at the tether's end. **Run dry and the soul snaps home at once** (no 5 s,
  no penalty). Taking hold of a body costs 10% of your pool on top.
- **What a soul can't do**: touch anything (in a person's body it can do all a player can: below). No blocks, chests, items (it picks nothing up and drops nothing), plates or
  tripwires; no meditating, no Inner Realm, no breakthroughs or tribulations (and you can't send the soul out during one),
  no portals, rift or Spatial Gap; no Qi Flight, Sword Flight or Realm Suppress. Qi Sense opens as it sets out (it is how a nascent
  soul sees), and you can shut it while out; it goes back as it was when you return. Blasts, fire, lava, falls, drowning, suffocation, arrows and monsters can't harm it.
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
River ("The heavens cannot see you here."); a soul already out can't take a rift up to it.

**The dead send out their souls too** (the user's call, 2026-10-19): a ghost or Ghost King whose path stands at Nascent Soul
sends its soul out of its ghostly form as the living do (a Ghost King come up into the living world, say), its form sitting
on in its light. The soul's rules are a soul's; struck down out there it is back in its form, its nascent soul lost, and
scattered to reform at the Gate as the dead are. **A soul of the dead takes a person's body too** (a player or an NPC
cultivator, as a living soul does, below), and in it **it is that living body**: it breathes, burns, falls, touches and is
touched as the living do, and no rule of the dead holds it (they go by the record it plays, `CultivationManager#get`). Out
of the body it is one of the dead again; a Ghost King's crown waits for it, unannounced.

**Harming a soul.** Only those who see it, by their own hand: a Nascent Soul cultivator (Qi Sense open) lands a quarter of
a blow at Early, 40% at Mid, 55% at Late, 70% at Grand Perfection (it's a haze to them); a Heavenly Being or above lands it
all, and can strike blind without Qi Sense. A Heavenly Being or above with Qi Sense open can also **strike the tether**:
swing at the thread itself. It holds 20 plus the soul's own health, mends if left alone for 10 seconds, and when it is cut
the soul is lost.

**Losing the soul.** A cut tether, a slain body, a soul struck down, or **logging out within 10 seconds of harm to the soul
or the body** (fleeing a fight) kills you where your body sat (your things fall there) and costs you the nascent soul: you
fall to **Core Formation Grand Perfection** with no cultivation, and must form it again through the Nascent Soul
tribulation. Logging out at peace, the server stopping, or a crash simply bring the soul home.

**Possession.** Right-click a being at least **one realm below** your own (a Nascent Soul takes Core Formation and
below, mortals and beasts; a Heavenly Being, Nascent Soul and below; Four Axis, Heavenly Being and below; monsters and
animals count as mortals) to take it over, until you let go, your qi runs out, you call the soul home, or it walks too
far along the tether. If the body dies, the soul is thrown out unharmed (and exposed again). How you hold
it depends on what it is:

**A person is truly possessed** (a player, only where PvP is on, or an NPC cultivator; `Possession`): your soul takes the
body's place and **you play it as a player**, with everything a player can do: walk, sprint, swim, mine, build, fight, eat,
use and throw things, open chests and your inventory, craft, ride. You have **what it carries, wears and holds**, its
health, hunger, experience and effects, and **its look and name to everyone** (its skin and cape, the name over its head
and in chat; in an NPC's body, its nameplate with its title and path; set out in a Ghost King's form, you are drawn as the
body all the same, not in your form's light). An NPC set aside shoves nothing (it follows its body a tick behind, and its
shove used to drift you on the way you last went). You have **its cultivation**: its realm and
strength, its qi pool and gathering, its abilities and spells; meditating in it cultivates the body, not you (no
breakthrough that brings a tribulation, though: a soul away from its own body can't face one), and **faster the higher
your soul stands above it**: once more as fast for every realm between you, counted by the stage (a quarter a stage, a
mortal's body a stage below Qi Refining Early; your own path's true rank against the body's higher path's), on both
paths, on top of every other boost (`Possession#guidance`): a Heavenly Being Early in a Qi Refining Early body cultivates
it 5 times as fast, a Nascent Soul Grand Perfection in a Core Formation Early one 2.75 times. You're told by how much as
you go in. **A soul of Heavenly Being Grand Perfection or above forces a mortal body's meridians open** as it goes in,
player's or NPC's (`Possession.OPENS_MERIDIANS`): Qi Refining Early, theirs to keep, with no Marrow Cleansing Elixir and no
tempering to Peak first (shattered meridians stay shut, as they do to the elixir). Its people take you for it:
an NPC's sect lets you through its barrier and onto its grounds as its own, and NPCs think of you what they think of the
body. Your own things wait for you, put by, and your soul's upkeep comes out of **your own pool**, apart from the body's
(the **Qi** bar is the body's, what its spells and abilities draw on; under it an **Own Qi** bar, in violet, is yours, or
**Own Ghost Qi** for a soul born of the ghost path; in a mortal's body yours takes the top). **Soul Projection leaves the body** (it is put on the
body's ability wheel for that, a mortal's body too). Inside, you are the body: it is harmed as anyone is, and your soul
itself is out of sight, but **your tether still runs** from your own body to the one you're in: you see it, and Heavenly
Beings and above with Qi Sense open see it and can strike it, as ever. To those who perceive souls with **Qi Sense** open,
a possessed body wears a **violet veil**: the soul's light washed over it, breathing slowly, with a halo about it.
- **Its tear and its core**: **Spatial Tear** is the body's own if it has one; if not and your soul has one, you tear space
  with your own, from your own pool. **Core Detonation** detonates the body's core (Core Formation and up): the body dies
  as its owner, its core lost (its owner falls to Foundation Building Grand Perfection), and your soul is thrown out.
- **Other realms**: take the body through a portal, a rift, your tear, the Soul River or a fall from the Upper Realm and
  your soul goes with it. Away from the realm your own body is in, the tether reaches across the realms: no thread to see
  or strike, and it costs only what it would beside your own body, so a Heavenly Being's full pool lasts about 100 seconds
  in the gap while possessing (about 60 after the tear). Let go of the body there and your soul goes home at once.
- **The player** isn't killed: they **watch through their body's eyes**, a spectator held to it (no other view, no
  teleporting off, nothing touched), and get it back where it stands, with whatever was done with it, when you leave.
  **If the body dies, they die**, as their own death by that blow (their things fall as keepInventory says), and you're
  thrown out. **Logging out doesn't free them**: the body plays on under you, and when they come back they're watching it
  again. If you let go while they're away, they wake where you left it with what it carried; if it died while they were
  away, they die as they come back. You logging out, or the server stopping, gives the body back first; a crash is put
  right as each comes back.
- **The NPC** is set aside, unseen and still, and goes where its body goes. Given back, it stands there again holding and
  wearing what the body does (anything else it took up, it carries among its winnings), at the body's health and any stage
  its cultivation came to, with its way into the next. If the body dies, it dies (its sect mourns it, and whatever struck it has the kill).
- Your ender chest and spawn point stay your own. The dead have no body to take.

**A beast or monster is ridden from inside**: you look out through its eyes and move it with its own speed and strength
(it runs as fast as it would run at you, no faster, and its melee is your attack); **sneak** to let go. **A flier flies** (a bee, parrot, allay, bat, ghast, phantom, vex or blaze): forward goes where you
look, so look up to climb and down to dive, and **jump** rises straight up; let go of the keys and it hangs in the air.
Each flies at its own pace (a bee or parrot gently, a vex fast; a phantom gathers speed while it holds its line, up to
far faster than anything else, and loses it when it turns), slower in water and slower still in lava. It takes no fall
damage while you fly it, and a roosting bat is roused. (A flier you hold is weightless, or a server without flight
allowed would kick you for floating a mount; it gets its weight back when you let go. A vex can't pass through walls
while you hold it: the server checks a ridden body against the blocks it moves through.)
Not bosses, Wardens, mounts, named creatures, or another player's pets. Whatever dies in your hands dies by
your hand (a possessed sect member walked into lava counts as murdered, not replaced). A possessed cultivator talks to no
one, and breaks off any talk it was in. (Before true possession a seized player died and a body in their likeness was left
to steer, `SoulVesselEntity`; one still in a world falls the moment it loads, with all it carried.)

**NPCs.** Cultivators of Nascent Soul and above sense souls as players do; below that a soul isn't there for them at all.
Sect members who sense one go for a soul that comes onto their grounds, Nascent Souls striking for less, as players do.
NPCs don't send out souls of their own yet.

Code: `SoulProjection.java` (the rules: going out and back, the tether, qi, harm, severing, possession, who is sent a soul),
`Possession.java` (a person's body taken and given back: what moves with it, the record that answers for it
(`CultivationManager#get`), the held player's view, the NPC set aside (`CultivatorNpc#isVacant`), crashes), client
`ClientBodies.java` and mixin `AbstractClientPlayerMixin` (its look and name), `SoulBodyEntity.java` (the body left behind),
`SoulVesselEntity.java` (a seized body from before), `SoulBarriers.java`
(formation barriers), mixins `SoulPlayerMixin`, `SoulEntityMixin`, `SoulLivingEntityMixin`, `SoulMobMixin`,
`SoulServerPlayerMixin` (only perceivers are sent it), `SoulProjectileMixin`; client `ClientSoul.java` (its flight, the
tether's pull, striking tethers), `SoulRenderer.java` with core shader `shaders/core/soul.*` (drawn on the sense layer so it
keeps its colour under Qi Sense), `SoulHud.java`, `SoulBodyRenderer` / `SoulVesselRenderer`, client mixins `SoulFlightMixin`,
`SoulInputMixin`, `SoulRenderDispatcherMixin`; tests in `SoulProjectionGameTests` and `PossessionGameTests`. Resource packs that replace core shaders
fall back to a plain violet translucent soul; under an Iris shader pack souls and ghosts are drawn as ever (the sense layer
comes after the pack's own passes), and a ghost's own first-person hand as flat glowing cyan (`compat/ShaderPacks`).

## Golden Core Detonation

From **Core Formation**, the living have a last resort on the ability wheel: detonating the golden core. Used once, the core
only trembles and you are warned; used again within 5 seconds (but not in the same half second: a double click is ignored),
it breaks (those near enough to be caught in it are warned on their action bar; nothing goes to chat), swells for 3 seconds
while you can barely move, and bursts. The higher your realm, the larger the blast. Everything living within its reach (55
blocks at Core Formation, 90 at Nascent Soul, 125 at Heavenly Being, 160 at Four Axis, 205 at Dao Integration, 295 at Void
Shattering, 340 at Sacred Vessel, 385 at Entering Nirvana, 545 at True Immortal, a tenth more for each stage) takes its harm: at the heart 1.1 times the whole health of a
body of your own stage, falling off with distance to nothing at the edge; for every realm a victim stands below you the harm
grows by the whole of it again, and for every realm above it is 0.3 times as much. It is an explosion, so armour and Blast
Protection turn some of it: a cultivator of your own realm in good armour may live through it, anything weaker near the heart
won't. The ground at its heart is blown away, a crater a third of the blast's reach across up to a Heavenly Being's (about
96 blocks wide; no larger beyond it) (what a spell may carve, nothing dropped), carved outward from the heart over the next few
moments like the great spells' craters, so it never stalls the world; the bigger the blast, the further off it is heard.
**You die** (nothing saves you, a totem
neither), and the core is lost: you return at **Foundation Building Grand Perfection**, your heaven methods understood no deeper
than that. Leaving the game while it breaks doesn't escape it. A creative player's core bursts without the death. Code:
`CoreDetonation`.

## The Spatial Gap, the Spatial Tear and floating meditation

**The Spatial Tear.** From **Heavenly Being**, the living can tear space open (on the ability wheel), for a quarter of their qi
pool going in and a tenth coming out (5 seconds between tears). The tear is a **rift that opens in front of you** (a couple of
blocks ahead, where there's room) and stands for **20 seconds**: nothing happens until you **walk into it**, and **anyone else
can walk through it too**, those who can't tear space themselves included (they come out of the gap by the crossing, the
floor, or a rift out that someone opens for them). Its rim shows where it leads: violet into the gap, green out to the
Overworld, gold out to the Upper Realm. Opening a new one closes your last.

**In the gap, everyone flies it the same way** (there are no trials any more: the 60-second trial that once carried those below
Heavenly Being up from the rift or down from the Upper Realm was removed 2026-10-18). However you come in (the rift at the
Overworld's 0, 0, falling off the Upper Realm, or a tear, yours or another's) there is no timer and no current: whoever can
fly **flies freely** there as anywhere (Qi Flight from Core Formation, Sword Flight from Foundation Building with a sword in
hand, a Ghost King's form; elytra don't open there), up and down as they like, and nothing throws them out until they rise
past the crossing or sink below the floor. Whoever can't fly falls: through the shallows, slowly through the lower layers, and
out of the floor into the Overworld's sky. **A block crossed there is 20 in the world**, both ways: you step in at a twentieth of where you stood and out at twenty
times where you are. The action bar shows where the world lies beneath you and how far the crossing (or the floor) is. Spatial
pressure still presses: **2.5 a second in the shallows** (where you come in at Y 128, between Y 60 and Y 200), and **8.2** in the
stormy layers above and below them (twice the old trial's 4.15 from 2026-10-17, eased to 8.2 on 2026-10-19). Qi Flight climbs about 7.5 blocks a second (measured 2026-10-19): the
upper layers take some 21 seconds, about 175 harm, and the whole climb from Y 128 some 31 seconds and 200 harm, before Qi
Sustenance's healing and the storms, so bring healing (a body mends at its realm's pace since 2026-10-22, in the gap too, so from
Nascent Soul up it presses the lighter: The realms after Four Axis, Mending). **Tearing out (a Heavenly Being's way) only ever leads back to the realm you came in from**; to pass
from one realm to the other you go **up or down**. Rise through the **upper layers** past **Y 360** and you are through, into
the Upper Realm, where the gap's place there lies (the arrival finds an island). Sink through the **lower layers** (under Y 60)
below **Y 12** and you fall out of the gap into the Overworld's sky (the first landing harmless); the gap is thick there, and
nothing sinks through it faster than 4 blocks a second (falling or flying down; about as fast as Qi Flight climbs), so the 48
blocks take 12 seconds: some 100 harm, before the storms (enough to kill a mortal or a weak cultivator who falls through).

**Telling the layers apart.** Flying the gap freely, its layers are plain to see: a veil of storm-light lies across each
boundary (magenta at Y 200, ember red at Y 60), the fog and sky darken and take the stormy layer's colour as you pass into it
(the shallows stay a calm deep blue), the motes there turn the layer's colour and the wind grows wilder, a storm's roar rises,
the screen's edge glows in the layer's colour, and a gauge at the left of the screen shows where you are: the shallows in the
middle, the stormy layers above and below, the crossing (gold, the Upper Realm) at its top and the floor (green, the
Overworld) at its foot.

**Arriving without a lag spike.** Coming out where nobody has been means generating the land there, and the server used to
do it on its own thread while everyone waited (an arrival in the Upper Realm could generate some 49 chunks of open sky looking
for an island). Now nothing waits on new land:
- **The island is found by numbers.** The Upper Realm's island masks (four noise samples a point) say where islands lie
  within 160 blocks of where you come out, and the generator's own heights confirm the nearest; no land is generated to look.
- **The land is loaded ahead.** Within 40 blocks of the crossing, or 20 of the floor, the land you're heading for is asked
  for, and generated on the world-generation threads while you climb or sink the rest of the way.
- **A short hold if it isn't ready.** Tearing out (or crossing) to land that hasn't loaded yet, you're held hovering where
  the tear opened while "space settles", for at most 5 seconds: no pressure and no storms meanwhile. Then you step out.
  Arriving reads only loaded land: if the Overworld's ground isn't loaded by then, you come down from its sky instead,
  the landing protected.

**Storms spread through the gap.** The physical storms (their lightning a tribulation strike) now gather all round everyone in
the gap: everyone in it has four round them in the shallows and ten in the upper layers they must rise through (or the
lower ones they must sink through), most of them gathering in the stormy layer nearer them (doubled 2026-10-17; at most 32 in
the gap at once).

**Floating meditation.** From **Core Formation**, the living float as they meditate, as the dead always have: rising gently
once seated, a little off the ground, bobbing; NPC cultivators of those realms too.

Code: `SpatialTrialHandler` (the routes, `tear`, `homeRealm`, travel, the hold), `SpatialArrivals` (island by numbers, land asked for ahead), `SpatialStorms`, `QiFlight`, client `GapSinkMixin`; `MeditationManager#floats`, client
`ClientMeditationTracker#hover`; tests `CoreAndGapGameTests` (the game-test server has no gap, so travel itself was checked in game).

## The Astral Realm

Beyond the Overworld's sky lies the **Astral Realm**: space, where the Overworld is one planet among **3,600**, round some
560 stars, and every other planet has land of its own to walk.

**Getting there.** A **Nascent Soul** or stronger on either path (as the realm lets them wield it), flying up past **Y 340** in the
Overworld, rises out of its sky into space, over the Overworld's lit side. Weaker cultivators are told the sky thins to nothing
above them. (Y 340: just above the build limit, so a Nascent Soul's pool can carry them there from a hill, and Mid and up from
the sea.) The **Upper Realm**'s sky is the same way out (since 2026-10-19: it is the heaven over the same world), past **Y 540**
there (since 2026-10-22, the user's ask: its islands rose past 340 and led out into space; 540 is above its build limit of 512, as
340 is above the Overworld's 320), and one who rose out of it comes home to it, down from Y 525 over its nearest island
(`AstralRealm#exitY`, `UPPER_EXIT_Y`, `UPPER_ARRIVAL_Y`). Of the dead, only a **Ghost King** may rise (since 2026-10-19, for the four axes); it flies space as
everyone there does. Space speed goes by the higher path too.

**Space** (`astral_void`). Nothing in it is a block: the stars and planets are drawn by the client where they truly are, and
you fly among them. Everyone in space is always flying, **on no qi**, at a speed of their realm: Nascent Soul 0.12 (vanilla
flying is 0.05), Heavenly Being 0.20, Four Axis 0.32, Dao Integration 0.44, a little more each stage; sprint for twice that. From
**Void Shattering** on, far swifter (the user's ask, 2026-10-22): Void Shattering 1.2, Sacred Vessel 2.0, Entering Nirvana 3.0, True
Immortal 6.0, more each stage (to the next star sprinting: Four Axis about two minutes, Void Shattering half a minute, True Immortal
five or six seconds, some 2,700 blocks a second). So swift a flier goes through space unswept for blocks (there are none:
`AstralRealm#passesThrough`), the client doesn't hold it while space's empty chunks load (`client/mixin/SpaceLoadingMixin`), and an
atmosphere crossed between two ticks still sets it down where its way met it (`AstralPlanets#atmosphereAlong`; the land there made
ready two seconds ahead along its way). Flying space faster than a chunk a tick loads none of its chunks meanwhile
(`AstralRealm#zooming`, `mixin/ChunkMapMixin`, as vanilla spares spectators): making, sending and saving a True Immortal's hundred
chunks a tick held the server at 60 to 100 ms a tick; they load round the flier two seconds after it slows (`ZOOM_HOLD`: its packets
come in bunched, and loading them at every lull cost as much), and no Vestige rises round it while it races. Qi gathers at **a
quarter** of the lower realms'. A star's heat burns whoever comes within 1.6 times its radius, and its fire consumes those who
reach it. Anything dropped in space (a death's things too) and any experience let loose there **floats** where it is, slowing
to a stop, rather than falling into the void below.
- **The stars and planets.** Star systems lie about 16,000 blocks apart, scattered up and down as well as across (space is as
  deep as it is wide), each a star with two to seven planets, some with moons (32%) or rings (14%). The Overworld's star,
  Taiyang, has three to five planets besides it. Each planet has a **kind** (verdant, temperate, ocean, desert, mesa, frozen,
  jungle, mushroom, volcanic, ashen, crystal, swamp, barren, blossom, savanna or taiga) and its own numbers within it (how much
  sea, how high its land and mountains, how warm and wet), so planets of one kind still differ; a few verdant ones come out
  much like the Overworld.
- **How they look.** Each planet is painted from its own land (`PlanetPainter`): the same continents, seas (shallows turquoise,
  the deep dark), ice caps, deserts, forests, mesas' bands, mountains and their relief, lava's glow on the night side, a crystal
  world's glints, under clouds and their shadows, lit by its own star from where that star lies, the terminator warm where the
  planet has air. Then its air's glow round the rim (lit on its sun's side, all round when the sun is behind it) and its rings,
  lit on either face and dark in its shadow. Textures are painted at the size the planet is seen, up to 2048 by 1024 for the one
  you're approaching (on threads of their own, sharper ones replacing the first as they're ready). Behind everything, nebulae and
  the galaxy's band with its dust lanes, and a field of stars. Other systems' suns are points of light, brighter the nearer.
- **Vestiges of the Void** hunt there (below).

**The planets' lands** (`astral_surfaces`). Fly into a planet's atmosphere and you come down in its sky, **over the very land
you flew toward** (the land is the planet's map: once round its equator east to west, pole to pole north to south), the first
landing harmless. Each planet's land is 960,000 by 480,000 blocks, in a square of its own; the land you're heading for is made
ready while you approach (you may be held a moment at the edge if it isn't). On the land:
- the Overworld's own biomes where they fit (forests, plains, deserts, badlands, jungles, taiga, snowfields and ice spikes,
  swamps, cherry groves, oceans of every warmth, rivers, beaches, peaks), with their trees, flowers, animals, ores, spirit stones,
  qi veins, herbs and Dao steles, and four lands of the realm's own: **volcanic** (basalt, blackstone and magma, lava seas, lava
  rivers, cones with lava standing in their craters), **ashen** (tuff, gravel and ash dunes, dead bushes), **crystal** (calcite
  and amethyst under a violet sky, glowing crystal spires) and **barren** (grey regolith and darker maria, craters great and
  small, no air: a black sky and the stars by day);
- the planet's **own qi** (`AstralPlanets.Planet#qi`), from a fifth of the lower realms' on barren worlds to up to 3 times on
  crystal and blossom ones (now and then a richer one), never past 3 times (it went to 6, and a rich planet outdid everything
  short of the Upper Realm's ten);
- its own **sky**: its star rising and setting with the day, sized by how near it is, its sister planets and moons where they truly
  hang, the stars by night (all day where there is no air), clouds only where there is air;
- **flight costs no qi** anywhere in the Astral Realm, so nobody is stranded on a world of thin qi.

Fly up past **Y 340** on a planet and you rise out into space over it; fall off a land's edge and you fall out into space below
it. Fly into the Overworld's atmosphere and you come down where you rose from (in the Upper Realm, if you rose out of it). The
Astral Realm is **no lower realm** (since 2026-10-19): no cap holds a breakthrough there and **nothing suppresses** a
cultivator, so Four Axis cultivators roam it freely after their axes. A Heavenly Being Grand Perfection may break through into
Four Axis **anywhere nothing suppresses them**, not only in the Upper Realm: the lower realms' cap holds only in the Overworld,
the Nether and the End (and never on Void Shattering or above: The realms after Four Axis). No Nether portals and no sects there.

**Settlements** (`Settlements`). The lower realms' people live on the worlds that would have them (not barren, ashen or volcanic
ones): their land is laid out in squares of 640 blocks, and about every other square has a settlement somewhere in its middle,
the same on every server of the seed. One is built when a player first comes within 96 blocks of its site: a **well**, **four
huts** round it in the world's own wood (oak, spruce on frozen and taiga worlds, acacia on dry ones, jungle, cherry on blossom
worlds, mangrove on swamps, dark oak on mushroom worlds, birch on crystal ones) with a doorway toward the well, windows, a
barrel, a workbench and a lantern, and **5-8 settlers**: mortals nearly half of them, the rest cultivators of Qi Refining (most)
to Core Formation (few), of the righteous path or none, each with a **wish** (the four axes, above) and keeping to within 18
blocks of the well. Settlers seek no fights (not even with the demonic path); they only defend themselves. A site whose ground is
water or lava stays empty. Each has a name ("Jade Spring Hamlet"), shown as you come
within 40 blocks. One with an axis to fill is shown now and then, while out of sight of any, the **smoke** of the nearest
settlement: how far, and which way. Which sites are built is saved with the Overworld.

**Kept alive** (2026-10-20, the user's ask): no monster is born within **64 blocks** of a settlement's well, and one that has
lost people takes in a **newcomer every two game days** from when it was first found short, until it has as many as it was built
with (a mortal or a cultivator as its founders were, with a wish of their own, walking in from its edge; those near are told).
Those whose time came while nobody was there arrive together. Its people are counted only while all its home ground is loaded.
Settlements from before were built with at least five, so they're kept to five.

**Vestiges of the Void** (`VoidVestige`). Black things of the deep void, shaped like the deep sea's dragonfish: a long eel's body
that follows the path its head swims, a great fanged lower jaw jutting past its snout (snapping shut as it bites), a huge pale
eye, a lure hanging from its chin, rows of violet lights along its belly, faint starlight in its skin and a violet sheen at its
edges. They hunt whoever flies **open space**, never the planets: they turn away from a planet's air, and reaching a planet's sky
is safety.
- **Strength:** a cultivator's, from **Nascent Soul Early** to **Heavenly Being Grand Perfection** (the most a lower realm
  sustains), with health half again a cultivator's of that stage and a bite of its attack.
- **Size:** larger with every stage, from 14 blocks long at Nascent Soul Early to nearly **100** at Grand Perfection.
- **Speed:** a little faster (8%) than a cultivator of its own stage sprinting through space, so one who has only just reached
  the void can't outrun one of their own strength; a stronger cultivator can. They lunge now and then.
- **Where:** after your first 30 seconds in space, every 10 seconds there's a chance one comes for you, 100-150 blocks off (at
  most two round you, 32 in all, none on Peaceful). Near the Overworld they are of Nascent Soul Early; a stage stronger at most for
  every 30,000 blocks out, Heavenly Being Grand Perfection beyond about 210,000. Felt before they are seen ("Something vast
  stirs in the dark..."). Unseen by anyone for 20 seconds, they slip away.
- **Drops:** spirit stones (low, mid or high grade by its strength) and experience.

**Commands.** `/astral where` (anyone: the planet you're on, its kind and qi, where on it; or in space the nearest star and planet
and the Overworld's distance). Op: `/astral visit <kind>` (beside the nearest planet of that kind), `/astral planet <index>`,
`/astral vestige <rank>` (12 Nascent Soul Early to 19 Heavenly Being Grand Perfection, near you in space).

Code: `AstralPlanets` (the catalog from the world's seed), `PlanetTerrain` (a planet's land, shared with the painter),
`PlanetSurfaceGenerator`, `PlanetBiomeSource`, `AstralRealm` (the ways in and out, qi, heat, `spaceSpeed`, the swift's crossings
and `zooming`), `QiFlight` (space's flight), `VoidVestige`, `VestigeSpawner`, `Settlements`, `AstralCommand`, mixins `PlayerMixin`
(the swift unswept), `ChunkMapMixin` (no chunks round them); client `AstralSky`, `AstralClient` (the planets' skies), `PlanetPainter`,
`PlanetTextures`, `PlanetDraw`, `PlanetLooks`, `VestigeRenderer`, `mixin/SpaceLoadingMixin`; tests `AstralGameTests`,
`FourAxisGameTests`, `SwiftAndMendingGameTests` (the game-test server has no Astral Realm, so the travel was checked in game, runs 17
and 17b of 2026-10-22 too; a settlement is built there on a floor of its own).

## The Netherworld

Above the Nether lies the **Netherworld**, the realm of the dead: misty plateaus of soul soil and blackstone between about
Y 40 and 200 over a bottomless abyss, dim (the ground is seen without a torch, barely), under a sky of its own. It is a
high realm, as the Upper Realm is above the Overworld: Realm Suppression doesn't reach it, portals don't work there, and
NPC cultivators come up into it from below. But its riches are another kind of qi. Ordinary qi is as thin there as
anywhere, and though no realm lock holds a breakthrough into Four Axis here (since 2026-10-19 only the lower realms do), the
heavens' tribulation can't come to take it; what the Netherworld has is **ghostly qi**, which the
dead and those who learn the ghost methods cultivate on. And the heavens can't see it: no tribulation, Ritual to the
Heavens, Inner Realm or Soul Projection there or on the river to it ("The heavens cannot see you here."); but the dead,
whose realm it is, turn inward there as anywhere (see The Inner Realm).

There are two ways in: **die** (you wake there as a ghost), or **go up alive** from the Nether.

Code: `HighRealm` (what the two high realms share), `ModDimensions` (`netherworld`, `soul_river`, `NETHERWORLD_MIN_Y`,
`NETHERWORLD_AMBIENT_LIGHT`), `ModBiomes`, `ModNoiseSettings#netherworld`, `NetherworldManager` (the per-second tick);
client `NetherworldClient`, `NetherworldSkyRenderer`, `UpperRealmClient` (its fog). Tests in `NetherworldGameTests`,
`NetherworldQiGameTests`, `NetherworldDeadGameTests`, `NetherworldMethodsGameTests`, `NetherworldInhabitantsGameTests`.

**Shader packs.** Under an Iris shader pack the Netherworld is lit as the pack lights the Nether, and its own sky (ghost
fires and aurora) is laid over the pack's: see Shader packs (Iris) below.

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
| Each realm after it | twice the one before: Dao Integration 40 minutes ... True Immortal 640 | `STAY_REALM_STEP` |

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

| Where | Density (a Soul Sand Valley = 1) | The pool fills, a second | The ghost path goes |
|---|---|---|---|
| The Netherworld | 10 (`GhostQi.NETHERWORLD`) | 2 | x10 |
| The Nine Nether Peaks | 12 (x `PEAKS_FACTOR`) | 2.4 | x12 |
| A Soul Sand Valley | 1 (`SOUL_SAND_VALLEY`) | 0.2 | x1 (the baseline) |
| The rest of the Nether | 0.5 (`NETHER`) | 0.1 | x0.5 |
| The Overworld, anywhere in it (the thinnest, the user's call 2026-10-19) | 0.25 (`OVERWORLD`) | 0.05 | x0.25 |
| Everywhere else (the Upper Realm, the Spatial Gap, the End, the Astral Realm) | 0 | nothing | not at all |

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
  light (under a shader pack, a flat cyan glow); it swings its arm when it strikes, as the living do, and when it meditates it sits cross-legged, floating a
  quarter of a block off the ground.
- has **its body's hearts, as every stat, by the dead's share** (see "The two paths"): its ghost path's whole, as a life's
  heaven path's (a ghost path of Nascent Soul Early, 65 health), its heaven path's half (one that died on the heaven path
  alone, half its living hearts), and heals a heart every two seconds. (Before 2026-10-19: half the living's, and a heart
  more for every layer of its deepest foundation.)
- **flies freely**: the dead need no ground. Double-tap jump to fly, at every realm and as fast as Qi Flight (swifter from
  Wandering Soul Steps' fourth layer, +10% a layer), spending nothing (`TwoPaths#fliesFreely`; the living in a Ghost King's form fly so too, see "The two paths"); not on the Soul River,
  nor while a Ghost-Quelling Talisman binds it. Holding jump while falling, it drifts down gently.
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
| Hearts (and every stat) | the ghost path's whole, the heaven path's half | `TwoPaths#pathShare`, `HALF_PATH` |
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

The ghost path is cultivated on ghostly qi, by the dead and the living alike, as the heaven path is on qi: **its own
cultivation bar**, the ghost methods riding on it as the heaven methods ride on the heaven path's (the user's ask,
2026-10-19; before, the deepest foundation's layers were the path). Each layer brings something, and some bring abilities
and spells.

**The ghost path.** A path of its own beside ordinary cultivation, and **neither builds on the other**: your ordinary realm
neither opens a ghost method nor speeds it, and a Four Axis cultivator who comes down starts the ghost path at Qi Refining
like anyone else. It **begins**, at Qi Refining Early (`GhostMethods#ghostRank`, saved as `NetherworldState`'s "PathRank"
and "PathCultivation"), with the first foundation you learn; or, **among the dead, with your first meditation on ghostly qi,
method or none** (`GhostMethods#begin`; the user's ask, 2026-10-19), as the living need no method for the heaven path. A
ghost path begun in death **stays yours when you are born again**: alive, the Path button draws in ghostly qi and you go on
with it, method or none. With no foundation known it goes at the heaven path's no-method pace, **three quarters**
(`GhostMethods#NO_METHOD_FACTOR`; "No ghost method" on the Cultivation tab), and no further than Qi Refining's Grand
Perfection until a foundation of Foundation Building or deeper is found (the gate below). Mortals, alive or dead, still
have no ghost path until their meridians are opened. But walking both paths makes you stronger than walking either
(see **The two paths** below).

**One qi or the other.** A meditation draws in **either** the heavens' qi **or** ghostly qi, never both: on both paths, the
Cultivation tab's **Path** button (between Meditate and Breakthrough) switches which, and the path drawn in has a short rule
under its column's name (`TwoPaths#cultivatesGhost`, `#switchPath`). Switched mid-meditation, the meditation goes on with
the other from its next breath. The dead draw in only ghostly qi. Both pools fill on their own as ever; only the
cultivation follows the switch. The Breakthrough button breaks through on the path drawn in.

**The bar.** Meditating while ghostly qi flows where you sit fills the ghost path's bar, and its stages step on by
themselves, as the heaven path's do, up to the bottlenecks (below). **Every stage takes longer than the one before**, from
one realm to the next too, on both paths alike (`GhostMethods#required`: `STAGE_SECONDS` 1,150 x 1.6 a realm x the stage's
time weight, the heaven path's own x1.3 / x1.5 / x1.67 / x1.88, `Stage#timeWeight`; the next realm's Early is x2.08): the
Qi Refining stages about 25, 29, 32 and 36 minutes (2 hours in all), Foundation Building's Early 40, the whole way to Four
Axis Grand Perfection about 53 hours, at the baseline (a living ghost path in a Soul Sand Valley); ten times as fast in the
Netherworld. The heaven path steps the same way: its stages' cultivation is x1.3 / x1.65 / x2.0 / x2.45 the realm's base,
cultivated at x1.0 / x1.1 / x1.2 / x1.3 (Qi Refining Early 130 s, Grand Perfection 189 s, Foundation Building Early 208 s,
about 4.6 hours to Four Axis Grand Perfection at the bare rate). Before 2026-10-19 a new realm's Early was quicker than
the Grand Perfection before it; the paths take as long in all as they did.
The Cultivation tab's ghost column shows the bar (the stage's cultivation on it, amber at a bottleneck, gold at the top)
and what meditating here adds a second.

**The bottlenecks: the Netherworld's tribulation.** Leaving Grand Perfection, and from Nascent Soul every stage, takes the
**ghost path's tribulation** (see Tribulations), as the heaven path's take the Heavenly Tribulation: the bar stops full and
the Breakthrough button (with the ghost path drawn in) brings it on. **Breaking into a ghost realm also wants a foundation
of that realm or deeper** (`GhostMethods#opensWay`): the Breathing Art carries the path no further than Qi Refining's Grand
Perfection, and only a deeper foundation, found, opens the way on (said under the details, in amber once the bar is full).

**The methods are understood as far as the path has come** (`GhostMethods#understoodTo`), as the heaven methods are: a
foundation to the layer standing for the path's stage, the fire and the steps three layers a ghost realm (up to the sixth
at Foundation Building, all nine from Core Formation). Learned, at once; and as the path reaches new stages, their new
layers **settle in** while you meditate on ghostly qi (`GhostMethods#settle`: half a layer's own cost, gathered on the path,
quicker by its own stele), every method you know at once, each told as it settles. A fall takes the layers past it
(`#clampTo`).

**The foundations** carry the ghost path **from Qi Refining up to their own realm**, nine layers to each realm they span
(`MethodLayers`): the Netherworld Breathing Art has 9 layers (Qi Refining alone), the Yellow Springs Marrow Art 18 (Qi
Refining, then Foundation Building), and so on to the Six Paths Reincarnation Canon's 54 (Qi Refining all the way through
Four Axis). The deeper the foundation, the further it opens the way and the rarer it is. Each foundation's spells come in its
own realm (its last nine layers):

| Foundation | Carries the ghost path | Layers | Spells (layer) | Where it is found |
|---|---|---|---|---|
| **Netherworld Breathing Art** | Qi Refining | 9 | Ghostly Wail (5); abilities Ghost Eye (3), Netherworld Veil (6) | **only** the stele on the Ghost Gate's plaza (none out in the realm); ghost cultivators teach it and sell its slip |
| **Yellow Springs Marrow Art** | to Foundation Building | 18 | Yellow Springs Tide (12), Marrow Ward (16) | steles everywhere (uncommon); Foundation Building ghosts teach it, sell its slip (180 stones) and now and then drop it |
| **Ghost Core Canon** | to Core Formation | 27 | Ghost Core Burst (21), King's Decree (25) | Core Formation Ghost Kings (most palaces); now and then a stele anywhere, more often on the Nine Nether Peaks |
| **Nine Nether Soul Sutra** | to Nascent Soul | 36 | Soul Rend (30), Nine Nether Step (34) | Nascent Soul kings (about one palace in four); Nine Nether Peaks steles |
| **Yama's Judgement Scripture** | to Heavenly Being | 45 | Yama's Verdict (39), Chains of Hell (43) | Heavenly Being kings (about one palace in eight); Nine Nether Peaks steles, rare |
| **Six Paths Reincarnation Canon** | to Four Axis | 54 | Wheel of Samsara (48), Six Paths Annihilation (52) | Four Axis kings (one palace in twenty); Nine Nether Peaks steles, about three in 1,371 |
| **Ten Thousand Ghosts Union Scripture** | to Dao Integration | 63 | none of its own yet | Nine Nether Peaks steles, about two in 1,377; the few Dao Integration ghosts |
| **Void-Rending Nether Canon** | to Void Shattering | 72 | none yet | Peaks steles, about one in 1,377; the handful of Void Shattering ghosts |
| **Hell-Forged Vessel Sutra** | to Sacred Vessel | 81 | none yet | Peaks steles, about one in 1,377; the one Sacred Vessel of the dead |
| **Underworld Nirvana Scripture** | to Entering Nirvana | 90 | none yet | Peaks steles, about one in 1,377 |
| **Netherworld Immortal Sovereign Canon** | to True Immortal | 99 | none yet | Peaks steles, about one in 1,377 |

**The ghost path goes on to True Immortal** (the user's ask, 2026-10-22), its last five foundations those of the realms after
Four Axis (their spells still to come), every stage of them longer than the one before as below it (`GhostMethods#required`), and
its tribulation the heavens' strike counts.

A king teaches its foundation to one it is Warm to, sells its slip to one it is Warm to (60 stones for the Breathing Art,
three times as much for each foundation deeper: 180, 540, 1,620, 4,860, and 14,580 for the Six Paths Reincarnation Canon),
and always leaves the slip when it falls.

Every layer of your deepest foundation adds +20 ghostly qi to the pool and a heart as a ghost, and (up to 18 layers) +10%
to the living's stay among the yin. Foundations don't stack: they all lay out the realms the same way, so only the deepest
counts (`GhostMethods#foundationDepth`). **A foundation found later is understood where your ghost path already stands**:
at Qi Refining Grand Perfection the Marrow Art is learned to its ninth layer and opens the way into Foundation Building; a
lesser foundation you've outgrown is learned whole, perfected at once, its spells in reach. Learning a foundation that goes
further than the one you practise makes it the one you practise (the one whose stele quickens you).

Besides the foundations, two methods of their own:

| Method | What its layers bring | Abilities | Spells |
|---|---|---|---|
| **Ghost Fire Scripture** (offence) | a ghost's blows +10% | | Ghost Fire (1), Ghost Fire Bolt (4); the fire leaps to a second foe (7) |
| **Wandering Soul Steps** (movement) | a ghost +3% swifter; from layer 4, the free flight of the dead and of a Ghost King's form +10% swifter a layer (+60% at 9; `QiFlight#freeSpeed`) | Yin Wind Steps (1) | Soul Dash (2) |

**Learning one.** First your meridians must be open (a Marrow Cleansing Elixir): a mortal, alive or dead, can't take up or
cultivate a ghost method (`GhostMethods#refusal`); anyone else can take up any of them. Meditate within 4 blocks of a
**Method Stele** for 15 seconds and you learn its method. Steles
stand on worn plinths across the Netherworld, one in about every 24 chunks: the fire's most often, then the steps', the
Marrow Art's, and now and then the Ghost Core Canon's (weights 300 / 200 / 150 / 10, of 660); on the **Nine Nether Peaks**
the deeper foundations are carved too (the fire 300, the steps 200, the Marrow Art 200, the Ghost Core Canon 40, the Nine
Nether Soul Sutra 20, Yama's Judgement Scripture 8, the Six Paths Reincarnation Canon 3, of 771; `GhostMethod#steleWeight`,
`#peakWeight`). **The Netherworld Breathing Art is carved on no stele out in the realm**: only on the one on the Ghost
Gate's plaza, by where the dead wake (the user's rule, 2026-10-19; worlds generated before keep the ones they have). They're unbreakable; touch one to read what it teaches (the dead can too), and the Ghost Eye shows them
through walls. A **Ghost Jade Slip**, read (right-click), teaches its method at once and isn't used up: ghost cultivators
drop them now and then, and Ghost Elders sell their own. A ghost cultivator knows its own realm's foundation five times
in ten (a Ghost King always), otherwise one of the first three methods, and one struck down leaves its foundation's slip
15% of the time. One who is Warm to you will **teach** you its method once a day: the method, or, if you know it, its
layers still settling settled at once and ten minutes on your ghost path's bar (`TalkSystem#teach`).

**Cultivating.** Meditate with ghostly qi drawn in (the Path button): while ghostly qi flows where you sit, the ghost path's
bar fills and the methods' new layers settle in on it (a ghost cultivates only this). The **Methods** tab shows where your
ghost path stands, how far your foundations open its way, every method you know, its layers understood (its next one
settling, how far, or the stage of the path that opens it), and a **Practise** button (the method practised is the one whose
stele quickens you). The layers' costs, in minutes of baseline meditation (`GhostMethods#layerCost`):

| Layer | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | All nine |
|---|---|---|---|---|---|---|---|---|---|---|
| The fire, the steps (half of it settles one in) | 5 | 8 | 12 | 20 | 30 | 45 | 60 | 90 | 120 | 6.5 h |
| A foundation, each realm's nine, before the realm's factor | 3 | 4 | 5 | 6 | 8 | 10 | 12 | 15 | 20 | 83 min |

A ghost path's stages no longer go by these layers (see The bar): each realm takes 1.6 times the one before, Qi Refining
2 hours, Foundation Building 3.2, Core Formation 5.2, Nascent Soul 8.3, Heavenly Being 13.3 and Four Axis 21.3, about 53
hours in all at the baseline: a living cultivator in a **Soul Sand Valley** (the realm's factor below is taken back out of
each stage's cultivation, so the ghost path's realm doesn't change how long a stage takes). The rate is `BASE_RATE` x the density (a Soul Sand Valley's 1 the baseline:
`GhostQi.SOUL_SAND_VALLEY`) x the ghost path's factor (`realmFactor`: -10% a realm below Nascent Soul, +10% a realm above)
x Qi Harmony x a Yin Gathering Pill's boost x 1.5 for the dead (`GHOST_FACTOR`) x 1.25 beside a stele of the method you're
practising (`STELE_BONUS`, within 8 blocks) x a possessor's guidance (Possession). Your ordinary realm doesn't come into it.

The ghost path can be cultivated **wherever ghostly qi flows**, the Overworld and the Nether included; the Netherworld is
simply the best place for it. Its qi is ten times as dense, so the bar fills **ten times as fast** there (twelve on the Nine
Nether Peaks): Qi Refining in about 12 minutes, the whole path in about 5.3 hours (the dead half as fast again). In a Soul
Sand Valley it goes at the baseline, in the rest of the Nether at half of it, and in the Overworld, the thinnest of all, at a
quarter of it.

**The foundations' layers and the path's stages.** Within each realm's nine:

| Layer within the realm | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| Understood from the ghost path's | Early | Early | Mid | Mid | Mid | Late | Late | Late | Grand Perfection |

So the Six Paths Reincarnation Canon's layer 10 opens at Foundation Building Early, its 21 at Core Formation Mid, its 54 at
Four Axis Grand Perfection (`GhostMethods#opensAt`). A ghost whose ghost path reaches Core Formation is a Ghost King (below),
whatever its ordinary cultivation.

**Old worlds.** A ghost path saved before the bar (2026-10-19) starts its bar where its deepest foundation's layers stood,
that layer's progress the bar's, the foundations' settling afresh (`NetherworldState#load`, once); saves from before the
foundations spanned the realms below their own are moved over first ("MethodLayout").

Code: `GhostMethod` (the methods, their realms, layers and rarity; ids saved, add new ones at the end), `MethodLayers`
(the realm-by-realm layout, shared with the heaven methods), `GhostMethods` (the ghost path's bar, its gate and bottlenecks,
the rate, the layers understood and settling, learning, what each layer grants), `TwoPaths#cultivatesGhost` (which qi is
drawn in), `MethodSteleBlock`, `MethodSteleFeature` (`NW_STELE_CHUNKS` in `ModPlacedFeatures`), `GhostJadeSlipItem`,
`MeditationManager#ghostMethods`, `TalkSystem#teach`, `GhostCultivatorNpc#method`; client `ClientMethods`,
`CultivationScreen` (the Cultivation tab's ghost column and Path button, the Methods tab); `/method path <realm> <stage>`
and `/method progress <seconds>` (Commands).

### Ghost Kings

One of the dead whose **ghost path** is **Core Formation or above** is a **Ghost King** (`GhostSystem#isKing`): it took
the Ghost Core Canon (or a deeper foundation) to Core Formation, in life or in death. **The heaven path makes no king**,
however high: a Four Axis cultivator with no ghost path dies one of the common dead (the user's rule, 2026-10-19; until
then dying at Core Formation on the heaven path crowned one too, and a world from before has those reigns undone as it
loads, unless the ghost path reached Core Formation as well). Never a mortal's ghost either, whose ghost path lies dormant
(below); a Ghost Devil Body is one from birth (Physiques). The dead are always drawn as the dead: a king crowned, the rest as
common ghosts. It is told so (the Journal keeps a note), and:

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
Wheel, it is alive, its cultivation kept, and the King's form stays its own: Form Shift takes it, ghost method or none (see
**The two paths**; `NetherworldState#reignedKing`).

Code: `GhostSystem` (`isKing`, `KING_RANK`, `watchKing`, `attack`), `Incorporeal` (`is` leaves kings out, `isGhost` and
`untouchable` keep them in), `SoulProjection#broadcastsTo`, `ModPackets#sendGhost` (the king flag); client `ClientGhosts`,
`SoulRenderer` (`drawCrown`, the shader's blue switch), `GhostKingCrown` (model layer
`defying-the-heavens:ghost_king_crown`, texture `textures/entity/ghost/king_crown.png`), `shaders/core/soul.fsh`.

### The two paths

Ordinary cultivation is the **heaven path**; the ghost methods' is the **ghost path**. Neither builds on the other, but a
cultivator who walks both is stronger than one who walks either (`TwoPaths`):

- **The body follows the mightier path, by its share** (the user's rule, 2026-10-19; `TwoPaths#body`, `#pathShare`).
  Health, attack damage, speed, armour, toughness and knockback resistance come from one path's realm and stage (the same
  table either way), and **the living have the heaven path's whole and the ghost path's half; the dead, the ghost path's
  whole and the heaven path's half**: half of every bonus, and half the **whole** of the health (the ten hearts a body
  starts with too). The body takes whichever is the mightier so. A living cultivator on the ghost path alone is the weaker
  for living: a ghost path of Nascent Soul Early gives 32.5 health alive (a heaven path's Nascent Soul Early, 65), and the
  whole 65 once dead. **Walk both and you lose nothing**, alive or dead: the path that is yours whole there carries the body
  as soon as it is the mightier for it (a heaven path of Core Formation Grand Perfection already outweighs half a Nascent
  Soul's ghost path), and harmony comes on top. The Stats tab shows the share (a red "bonus" where half the health is less
  than a body starts with), and the effective cultivation line names what it comes to. Stat points count by the higher
  path's rank, alive or dead. Realm Suppression and Realm Suppress hold the ghost path down as they do the heaven path.
  Qi and its pool are still the heaven path's.
- **Harmony.** The two paths together add to that body, the more the closer they stand: **+30%** with both in the same
  realm, **+20%** a realm apart, **+10%** two apart, nothing further (`TwoPaths#HARMONY`, by realm, not stage), on top of
  Heaven's Favour. The Stats tab names the path the body follows and the harmony; on the Cultivation tab the two paths
  stand **side by side, alike** (neither a side path): each its name (a short rule under the one whose qi is drawn in), realm
  and stage, its bar (each its own cultivation, the stage's written on it; amber at a bottleneck, gold at the last), and what
  meditating here adds a second (the ghost path's by the method practised); the path whose qi isn't drawn in reads
  "Resting (not drawn in)". The **Path** button between Meditate and Breakthrough (the three then share the row) switches
  the qi drawn in (see Ghost methods), and a bottleneck on the resting path says to switch first. Words too long for a
  column are written short. Under the two, the details (pills, path, harmony, favour, the Upper Realm's qi, ghostly qi, yin)
  share rows as they fit, then across the whole width what stands next on each path (the heaven path's breakthrough, pill
  or ritual; then the ghost path's tribulation, or the foundation its way waits on), or whatever holds you (pressure, Realm
  Suppress); the lines draw closer when there is much to say, so everything fits.
- **The realm's abilities come from either path** (`TwoPaths#awakens`): Qi Sustenance at Foundation Building, Qi Sense and
  Golden Core Detonation at Core Formation, Soul Projection at Nascent Soul, the Spatial Tear at Heavenly Being. The heaven
  path awakens one if it stands at its realm; otherwise the ghost path does, and then it is the ghost path's: Soul
  Projection's upkeep, the cost of taking hold of a body and the tear's cost come out of the **ghost qi** (a ghost path
  soul's upkeep is reckoned from what the ghostly pool gathers in the Netherworld, `SoulProjection#GHOST_SOUL_BASE`: half of
  that a second at the body, twice it at the tether's end, the same again while possessing; outside the Netherworld and the
  Nether the pool doesn't refill), the soul's hearts and tether are the ghost path's, losing that soul drops the ghost path to
  Core Formation Grand Perfection (its methods held there, `GhostMethods#fallTo`), and a detonated ghost core drops it to
  Foundation Building Grand Perfection. Who perceives a projected soul, how hard one may be struck, and how strong a player is
  to be possessed go by the higher path. **Qi Flight and Sword Flight stay the heaven path's**; the ghost path's flight is
  its form's (below).
- **Others weigh you by your body**, whichever path gives it (`TwoPaths#weighedRank`, the 2026-10-20 sweep: only the heaven
  path was weighed before): Realm Suppress, yours on others and theirs on you; how cultivators size you up (who is weaker in a
  fight, whom they dare turn on, who begs, a spar's holding back, your title in a sect, "too strong to join"); a formation's
  barrier and the strength of one you raise; the realm your Consciousness Domain reaches by and the realm others sense of you;
  your standing in a talk; breaking a sect treasury's seal; striking a soul's tether. A ghost path of Heavenly Being with a heaven
  path of Qi Refining is a Heavenly Being to them all.
- **Ghostly creatures fly freely.** The dead, and the living in a Ghost King's form, fly at every realm, as fast as Qi
  Flight (and +10% a layer from Wandering Soul Steps' fourth, +60% at its ninth), spending nothing (`TwoPaths#fliesFreely`):
  double-tap jump (the Spatial Gap too). The Soul River still carries
  the living, and a Ghost-Quelling Talisman grounds whoever it binds.
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
- **Two forms, while you live.** Once your ghost path reaches Core Formation (a Ghost King's might), or once you have been a
  Ghost King among the dead (kept when you are born again, even should your ghost path fall after), you have a **Ghost King form** beside your
  human one: crowned, in the dead's deeper light, as the Ghost Kings are drawn. Your **true form** is the
  form of your higher path: Four Axis on the heaven path and Nascent Soul on the ghost path, you look human; the ghost path
  higher, you look a Ghost King; level, human. The **Form Shift** ability (Abilities tab, or the ability wheel) takes the
  other form while it's on. In the Ghost King's form you are **seen as the dead are**: outside the Netherworld only by those
  who perceive souls (a Heavenly Being or above, a Nascent Soul with Qi Sense open, anyone with the Ghost Eye open, and the
  dead; `SoulProjection#seenOnlyBySeers`), so to anyone else's game you aren't there at all; in the Netherworld, by everyone.
  In all else you are still alive: you eat, mobs hunt you, arrows strike you. Without that form the living look human.
  You're told when your true form changes, and when a Ghost King born again keeps its form.
- **The dead are the dead.** Whatever the realms of their two paths, the dead are drawn as the dead: a Ghost King crowned
  (its ghost path at Core Formation; the heaven path's realm crowns no one), the rest as common ghosts. A **Ghost King has two forms**: its true
  form is the King's, and Form Shift takes its **human form**, in which it is drawn as a man and seen by everyone, as the
  living are (`TwoPaths#inHumanForm`), the living's cultivators too, who talk and trade with it as with anyone (since
  2026-10-19, `CultivatorNpc#senses`); in all else it keeps the rules of the dead. Common ghosts have no forms. Dying, or
  born again, you start in your true form (Form Shift switched off).
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

(**Ghost Flight**, which once let the dead fly as fast as on qi for ghostly qi, is retired: every ghost flies so now, for
nothing, and Wandering Soul Steps' fourth layer on speeds that flight instead. The ability stays in the list, never awakened,
so the saved switches after it keep their places.)

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
eye, Netherworld Veil a dark hood with two cold eyes, Yin Wind Steps a pale footprint on curling wind, Ghost Flight (retired) a
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
same standing (one far above it greets, hears, takes a gift and trades, Ghost Kings too); it makes no demands. The dead sell Yin
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
| **Yin Gathering Pill** | Alchemy Cauldron, 30 s: an Other-Shore Flower, a Netherworld Lotus, a ghast tear | the ghost path's bar fills +25% faster per point of potency for 5 minutes (`PillItem.YIN_BOOST_PER_POTENCY`); only for one whose ghost path has begun |

**The Ghost-Quelling Talisman.** Thrown, it flies past the living (and through a projected soul) and strikes only the
dead: a player's ghost, a ghost cultivator, a Hungry Ghost. It deals **6** of the dead's own harm, which lands in full, and
binds the one it strikes where it stands for **5 seconds** (the Quelled effect): no walking, jumping, flying or Soul
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
/method learn <id> [layer]  a ghost method at that layer (to 54; without one, as a stele teaches it), nothing settling
/method progress <seconds>  that many seconds of baseline meditation on the ghost path's bar (its layers settling on it too)
/method path <realm> <stage>   the ghost path set there (1-6, 1-4), its methods understood as far
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
library chest on the ground floor, which keeps **one or two Jade Slips** of heaven methods besides its books: Qi
Refining's most often, Foundation Building's now and then, Core Formation's rarely, `chests/sect_library`; a ladder climbs
to a meditation chamber at the top) with herb gardens (ginseng,
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
  later grow as usual), no ore blob reaches into a sect's buildings or a ghost palace (`mixin/OreFeatureMixin`: with
  Terralith, which counts blackstone as Overworld stone, vanilla's gravel, dirt and ores used to come up through the demonic
  sects' blackstone walls; the open grounds and what lies under them keep their ore), and the sects build with nothing a water
  or lava spring can open in (the plaster is smooth quartz, not calcite; checked by a game test).
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
- **Finding the way** (`CultivatorNavigation`, `CultivatorGoals#walkToward`): vanilla's path search gives up on any walk
  longer than 32 blocks, and from the back of the meditation hall to a gate behind it, out of the door and round, is longer:
  every path it found ended in the hall's corner nearest the gate, where gate guards stood for good. When the usual search
  falls short, a cultivator searches again for a walk of up to 96 blocks (a spot that search can't reach isn't tried again
  for 10 seconds), then for whichever of the sect's way points it can walk to soonest (out of the door). A path that goes
  the whole way is kept rather than planned afresh every couple of seconds (a guard swung between a way out and a dead end),
  and a walker that gets nowhere for a second beside the next point of its path goes on to the one after, stepping down
  onto it if it hangs on an edge above it (a guard rocked on the corner of the hall's plinth for good, where its path
  stepped down across the corner). A game test walls a walker into the back of a closed hall with its goal just behind the
  back wall and checks the way it finds leads out of the door and round; in game, every gate guard of a sect set on the
  meditation mat nearest its post reached its gate within 23 seconds.
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
  Refining Early to Four Axis Grand Perfection, and a quarter of those at its peak are of Dao Integration instead
  (`NpcSpawner#DAO_INTEGRATION_SHARE`). **Cultivators never despawn**, so rogues are kept rare by
  placement instead: there is never more than one wandering rogue within 160 blocks (sideways) of a player, because a new
  one keeps 352 blocks (twice that, plus room for strolling) from every other, loaded or not. Mortal wanderers are kept
  the same way among themselves, apart from the rogues, so you may meet one of each. `RogueIndex` remembers where each
  one is, saved with the Overworld. Sect members, and those seeking seclusion, in seclusion or on a journey don't count;
  a rogue a player has named (a name tag) still does, so naming one doesn't make room for another. Neither kind turns up
  within 160 blocks of a sect's grounds, nor near a sect site not settled yet (`SectSites.unsettled`: the sites round a
  player are looked at within seconds of their coming, so this only holds a spawn back a little).
  None turns up while 40 rogues and wanderers are loaded in the level (sect members don't count). Flying over land you
  meet one every 350-650 blocks or so; over water, none.
- **The few past Four Axis** (the user's lore, 2026-10-22; `HighRealms`): in a realm (a world of its own: the Upper Realm, the
  Netherworld among the dead) cultivators of **Dao Integration are rare** (24 at most), of **Void Shattering a handful** (5), of
  **Sacred Vessel one**; the many of **Entering Nirvana live elsewhere**, waiting for the way into the immortal realm to show itself,
  and no True Immortal walks the realms below it. A cultivator rises into one of those realms (by its own cultivation, as
  cultivators of the Upper Realm and the Netherworld do: no tribulation for them) only while its world has room in it, else it
  waits at the Grand Perfection below; and it holds its place there until it dies or is taken out of the world (one unseen for
  a hundred game days is given up as gone). Players are never held back, nor a body a player's soul cultivated (Possession). The
  census is saved with the Overworld. They carry the stones of their realms (Supreme and Immortal spirit stones) and wield
  netherite, and teach and sell their own realm's method.
- **In a fight** cultivators use what their realm gives them: a Consciousness Domain (rogues always, sect members only
  in combat), Realm Suppression and Qi Flight or Sword Flight, but only while their qi can sustain them. They flee below a quarter of their
  health (all but the Sect Master, who makes a last stand), and rogues and disciples also flee from a hostile domain four or more stages stronger once their own domain
  senses it. The Sect Master and the elders never flee a stronger domain: they stand and fight for their sect. A
  cultivator fleeing a stronger domain runs (or flies) to a refuge about 200 blocks away (160-240), on the far side from
  what it fears, and only then comes home; it gives up after 3 minutes. The refuge is kept within the area the world is
  simulating around players (so it doesn't freeze out there): fleeing from you, it ends up near the edge of your
  simulation distance if that is closer than 200 blocks (10-12 chunks by default, about 160-190 blocks). Badly hurt, a
  cultivator only backs away, and keeps backing away **for as long as whoever last struck it is within 32 blocks**
  (`CultivatorNpc#getPursuer`, `PURSUER_LOST`): not only for the five seconds vanilla remembers a blow, after which one
  that kept ahead of a Hungry Ghost used to stop with it right behind (fixed 2026-10-19). Struck by its pursuer at close
  quarters, a fleeing cultivator strikes back
  (`FLEE_DISTANCE`, `FLEE_SPREAD`, `FLEE_TIMEOUT` in `CultivatorNpc.java`). A fleeing cultivator is a little slower than a
  sprinting player of its own realm, and a badly hurt one well slower, on foot or in the air, so it can be run down
  (`RUN_SPEED`, `FLY_SPEED` in `CultivatorGoals.FleeGoal`).
- Every cultivator has a nameplate: their name ("Mo Lian", or what a name tag called them), title, a red health bar with
  numbers, and alignment, for example "Righteous (+120)". Under Qi Sense, meditating cultivators draw qi motes into themselves.

### Joining a sect

Players can join a sect and become one of its disciples (`SectMembership`).
- **Asking.** Talk to one of its members and Ask "to join the sect". A disciple sends you to an elder, and you are the sect's
  **guest**: its barrier lets you through for 10 minutes, and for as long as you stay on its grounds after (so it never shuts
  you in), until you turn on the sect (an alarm raised over you). An Elder, a Grand Elder
  or the Sect Master judges you. They turn away the dead, mortals, a disciple of another sect (who must leave it first), one
  the sect cast out or who just left it (see below), one it loathes (its standing with you, or the elder's regard, Hostile or
  worse), and anyone stronger than its Sect Master.
- **The entrance trial.** Otherwise the elder sets you a trial: Ask again, "take the entrance trial", within 5 minutes, and you
  spar with them, holding back to your own realm (fought as any spar is: blows pulled to half, lost at half your health).
  Win it, or stand your ground to the end, and you are taken in. Lose it and
  that sect won't try you again until tomorrow. A body of **High grade or better** (Physiques), or an elder **Warm** to you,
  takes you in without the trial.
- **Your title** is your strength among its members as they are: Outer Disciple, Inner Disciple, Elder or Grand Elder (the
  Sect Master's seat is always a cultivator's), changing as you break through or its members change; you're told when it
  does.
- **The Sect tab** (in the cultivation menu): your sect's name and path, your title, its regard for you, where its grounds
  lie (on them: the quarter faster), whether its barrier is raised, what the treasury's rule is for you (and whether you've
  been warned), how many cultivators and players it has, how long you've been a disciple, then every member by seniority,
  each with their title and realm (players after, "away" when not online), scrolling. Without a sect it says how to join
  one and names the nearest sect known within 4,000 blocks, how far and which way (and how many days it won't hear you, if
  it has barred you). The page is asked of the server as the tab opens (`SectMembership#sendInfo`, client `ClientSect`).
- **Belonging.** Its barrier lets you through. Its members are your fellows: never hostile to you, they come to your aid when
  you are struck near them, and your spells spare them. On its grounds you cultivate **a quarter faster**. Its elders teach
  you its method once Courteous (others need Warm).
- **Its rules.** The treasury belongs to its elders. Below Elder, the first hand you lay on it (opening or breaking) is stopped
  with a warning; the second casts you out, and after that you are an outsider there like anyone (the seal, theft). Striking a
  fellow member outside a spar also casts you out. Cast out, the sect won't hear you for **30 days**.
- **Leaving.** Ask an elder to "leave the sect". It takes you back after **3 days**.
- Commands (op): `/sect join <player>` joins the nearest sect, `/sect leave <player>`; `/sect info` lists its disciples too.

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
plate (personal name, title, sect, realm), what they say, your regard, and numbered replies (the box grows taller to hold
them all: an elder's Join, after its teachings, used to fall off the bottom). The wheel folds into tabs
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
  think of you: their paths' baseline (+10 between two of the same strong path, -10 when you lean the other way; +10 more
  from everyone for an obsession with Merit), plus their own regard, plus their sect's standing with you. At -50 or lower they attack you if they think they can win
  (`Regard`). Courteous opens rumours (and Spar with a senior), Warm a senior's guidance, Trusted the best trades; a
  Sworn friend gives you something now and then when you greet them, and comes to your aid when something attacks you
  within 24 blocks of them.

| What you do | Regard |
| --- | --- |
| Greet them | +2, once a day |
| Give them something (old herbs, pills, Cultivation Fruit, spirit stones; worth less to the stronger, and less each time the same kind of gift comes again that day, until a trifle is worth nothing more and they hand it back) | +1 to +25 (the day's first of a kind); the same again can be 0 |
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
| Their wish | A settler of the Astral Realm with a wish waiting | What they wish for; a Four Axis cultivator at its axis may grant it (see the four axes of virtue) |

Topics not open yet still show, greyed, with the regard they need.

- **Rumours** (`Rumours`): old herbs (a hundred years and more), Cultivation Fruit and Spirit Peach Trees, a secluded
  Heavenly Being's cave, a fallen sect's unclaimed Formation Core, a cultivator killed in the last three days (and by
  whom). Never coordinates: a direction, a rough distance, the country it's in, and for herbs a guess at the age ("five
  hundred and fifty to six hundred and fifty years"); the stronger the teller, the closer their guesses. Each cultivator
  tells you a thing once; rogues have heard of things up to 2,500 blocks away, sect members only within 400. The index is
  fed as those things come and go (their block entities loading, a sect falling, a killing), never by scanning chunks.
  Everything you are told goes into the **Journal** tab of the cultivation menu, newest first.
- **Spar** (`SparSystem`): a bout of several exchanges. Pick terms in the box: **friendly**, **a wager** (hold a herb, pill,
  fruit or spirit stones: you stake one, they stake something of like worth of their own, the winner takes both; one
  wager a day with each; never with one a realm or more beneath you, who couldn't win your stake: friendly only, the wager
  greyed out), and, with someone stronger, **don't hold back**. Neither of you starts below three quarters of
  your health. Three breaths, then it begins. Blows between you land at half strength (pulled), and it ends when either
  of you has lost half your health since it began, when you walk 16 blocks off from them (a forfeit), or after three
  minutes (a draw). (At a quarter's line, without pulled blows, one blow from an equal ended it at every realm up to
  Nascent Soul: such a blow takes 28-39% of one's health.) Nobody dies: a blow from the other that would go past the line stops at it. A stronger cultivator
  holds back to your realm unless told not to. Blows in a spar are no attack on their sect, and blows between you in the
  three seconds after it ends are pulled (one far stronger often ends it with the first blow, and the next swing came a
  moment later; it was taken for an attack, the whole sect turned out and the barrier put you outside), as are your stray
  blows on its sect's other members, in the bout or just after (a sweep or a spell's spread raises no alarm). Losing to a senior who is
  Warm to you can teach you their Insight. A weaker cultivator never runs from the one it is sparring with, however far
  above it they are (it would run from such a domain in a real fight); if it runs from someone else mid-bout, the spar ends
  with no result rather than as your forfeit.
- **Trade** (`CultivatorTrades`): barter on the trading screen, in spirit stones. They sell by station: Outer Disciples
  and commoners seeds and young herbs, Inner Disciples rarer seeds and Qi Gathering Pills, Elders pills and old herbs, the
  Sect Master the best of both; rogues odd things from far away. The stock changes each day. **Everyone trades, however
  far above you** (a Ghost King too; before 2026-10-19 one two realms up wouldn't). Regard opens better offers
  (one more at each of Polite, Courteous, Warm and Trusted) and lowers prices (x1.3 when Resentful down to x0.75 when
  Sworn; an obsession with Fortune, x0.8 more on top); **the offers not open to you yet are shown all the same, crossed out**, "Locked: sold to those it regards as
  Warm (you: Indifferent)" on the item (`CultivatorTrades#locked`), so you can see early what they have. They also buy
  what you carry that a cultivator has use for (herbs, pills, fruit), for half to four-fifths of its worth by regard.
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
/cultivator secluded [count]                      the secluded cultivators nearest you (this world first), click one to go there
/alignment get [player]
/alignment set <player> <value>
```

`/place` builds into land that is already generated, so it knocks loose some grass and flowers there (vanilla
structures do the same); sects that generate with the world don't.

`/cultivator secluded` lists every cultivator known to sit in seclusion or to be seeking a place for it: those loaded (with
their realm), then the rumoured caves of those whose land isn't loaded, nearest first (default 10, up to 100). Each one's
coordinates are a link that teleports you there (`/execute in <world> run tp`). After them come up to five spots nearby where
a hermit will be seated once someone first loads the land (if it holds a cave or a high peak); clicking one puts a teleport
to it in the chat.

Sects and cultivators' off-screen journeys are saved in the Overworld's `data/defying-the-heavens_sects.dat` and
`defying-the-heavens_npc_travel.dat`; formations in each dimension's `defying-the-heavens_formations.dat`.

The Netherworld's own commands (`/netherworld`, `/method`) are listed under The Netherworld. Its Ghost Gate is remembered
as built in the Netherworld's `data/defying-the-heavens_ghost_gate.dat`.

## Shader packs (Iris)

The mod runs with Iris shader packs (checked with Iris 1.7.6, Sodium 0.5.13 and Complementary Reimagined r5.9.3 at its
highest settings). Iris is never required: everything below is looked up by name and does nothing without it.

- **The realms' skies.** A pack draws the sky with its own programs and keeps nothing a realm's sky renderer draws, so in
  space, the Netherworld, the Soul River and the Inner Realm the realm's own sky is drawn again into a layer of its own at the
  end of the world's drawing and laid over the pack's finished frame wherever the world's depth shows open sky (`SkyLayer`,
  core shader `sky_layer`): space keeps its nebulae, stars, suns and planets, the Netherworld its ghost fires and aurora. The
  land stays the pack's, lit its way; Qi Sense greys the sky as before, and the senses' layer (souls, ghosts, motes) still goes
  on top. The Spatial Gap keeps the pack's own sky (its haze and stars, with the gap's storms in it): its own near-black void
  looked bare laid over a pack.
- **The Netherworld's light.** A pack gives a dimension it doesn't know its Overworld programs (Complementary:
  `dimension.world0=*`), an Overworld night of moonlight and clouds; the Netherworld is named the Nether to Iris instead
  (`compat/mixin/IrisDimensionMixin`, its own optional mixin config `defying-the-heavens.iris.mixins.json`, applied only with
  Iris), so it is lit as the Nether, by the fog's dark teal.
- **The Upper Realm** keeps the pack's sky (its sun, clouds, shadows and day), which a pack paints from the sky's colour alone;
  while a pack is on, that colour is turned toward the jade the realm's own sky shows (`compat/ShaderPacks#skyColour`).
- **A ghost's own hand** in first person is drawn as a flat cyan glow (the pack discards the soul shader's draw in its hand
  pass); ghosts and souls in the world are drawn as ever.
- Planet surfaces in the Astral Realm keep the pack's own sky, day and night.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
