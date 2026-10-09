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

Balance constants are in `FruitAge.java`. The game imports copies of the model and textures from
`Models/CultivationFruit`; source art is unchanged. The block copy is moved upward to hang from the canopy,
while the item retains the artist's display transforms. Invalid exported resource references and the missing
upper-bottom face texture are repaired in the imported copies.

Run `gradlew.bat runGameTest` for server-side fruit integration tests, or `gradlew.bat build` for tests and a distributable JAR.
Run `gradlew.bat runDatagen` separately before a build when updating generated translations.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
