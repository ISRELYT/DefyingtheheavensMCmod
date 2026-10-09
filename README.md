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
- Harvested fruit stores its age permanently, cannot be placed back, and can be eaten even at full hunger.
- Eating grants up to **10 cultivation per year**. Existing suppression, stage caps, and tribulation rules still apply.
- The creative Food & Drinks tab has a one-year fruit. Saplings, farming, and automatic regrowth are not implemented yet.

### Wild fruit

Fruit generates on its own, hanging under the leaves of naturally generated trees in new chunks:
on average one per **6** tree-bearing chunks in the Overworld, and one per **3** in the Upper Realm.
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
natural age and gain 10 years per Minecraft day, up to 10,000 years, with the same age auras as fruit. They will be
ingredients for alchemy (pills and elixirs); for now a harvested root keeps its age and can be set on a Spirit Pedestal,
where it counts like a fruit of the same age.

- Right-click a plant to dig it up into your inventory (a full inventory leaves it in the ground). Breaking it, or the
  soil under it, drops it too. Harvested ginseng keeps its age, cannot be replanted and is not food.
- Ginseng grows only on soil (grass, dirt, podzol, moss...). Wild plants appear on grass or podzol, never in caves.
- Spawning, on average one plant per this many chunks: Ginseng 8 in Overworld forests, taiga and jungles and 3 in the
  Upper Realm; Spirit Ginseng 64 and 16. These are `GINSENG_CHUNKS_*` and `SPIRIT_GINSENG_CHUNKS_*` in
  `ModPlacedFeatures.java` (and the matching `placed_feature/*ginseng*.json`; re-run `runDatagen` after changing them).
- The plants are drawn like vanilla's sweet berry bush, whose leaves they reuse: Ginseng in a warm green with one cluster
  of red berries, Spirit Ginseng in pale jade with golden berries. The harvested items are the forked roots.
- Code: `GinsengBlock`, `GinsengBlockEntity`, `GinsengItem`, `GinsengFeature`, `client/GinsengAuraRenderer.java`.

With cheats/operator permissions:

```text
/ginseng plant 1000          plants 1,000-year Ginseng on the soil you look at
/ginseng plant 10000 spirit  the same, Spirit Ginseng
/ginseng give 500 [spirit]   a harvested root
```

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

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
