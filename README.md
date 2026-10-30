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

### Libraries

**GeckoLib** 4.8.4 (animated entity, armour and item models, made in Blockbench) is packed inside the mod's jar (jar-in-jar:
`include` in `build.gradle`), so players don't install it themselves; one who has a newer GeckoLib of their own (another
mod's) gets that one instead, as Fabric picks the newest. It comes from GeckoLib's Cloudsmith maven (`build.gradle`,
repositories); its version is `geckolib_version` in `gradle.properties`. Added 2026-10-23, ready for models to come: nothing
uses it yet.

## Creative tab

Everything the mod adds has its own creative tab, **Defying The Heavens** (`ModCreativeTab`, shown with a Cultivation
Fruit; with vanilla's tabs full it sits on the second page, behind the arrows at the top). In order: what cultivation is
done with (meditation mats, Spirit Pedestal, Heaven Altar, Alchemy Cauldron, Formation Core, Inscription Brush, the
rings, the storage formations, the Artifact Forge and every artifact at every grade, a banner of each element), what is taken (a one-year Cultivation Fruit, every pill at every grade from Low to Immortal, Spirit Dew), the
herbs (a year old) and their seeds, spores and pit, spirit stones of every grade and jade, the world's blocks (jade
stone, ores, qi veins, Blue Spirit Wood, leaves), the clothing (every path's colours, grade and piece), the
Netherworld's things (Yang Fire Talisman, Ghost-Quelling Talisman, Peach Wood Sword, Red Spider Lily, the Other-Shore Flower and Netherworld Lotus
a year old with their bulbs and seeds, Yin Jade with its ore and block, the Yang Preservation and Yin Gathering Pills at
every grade, a Ghost Jade Slip of each method), and the spawn eggs: Cultivator, Ghost Cultivator and Hungry Ghost. An
item added later that the list doesn't name still shows, at the end. The items stay in vanilla's tabs too. The mod's
other creatures (tribulation clouds, heart demons, the bodies left behind by the Inner Realm and Soul Projection) only
arise from what makes them, so they have no eggs.

## Advancements

The mod's own advancements (the user's ask, 2026-10-27: "advancements like normal Minecraft but for the mod"), 82 of them in five
tabs of the advancements screen (L), each with its toast and told in chat as vanilla's are (`announceAdvancements`); none gives a
reward (the user's pick).

| Tab | What it follows |
| --- | --- |
| **Defying the Heavens** | The realms, Qi Refining to Heavenly Lord (the highest of your paths: heaven, ghost or Demon Path); a tribulation survived, and one into True Immortal or past it; a High-grade physique; the Ritual granted; the four axes of virtue; a Law; the Five Decays weathered |
| **Arts and Treasures** | The first spell, the five elements' spells, a spell of both paths; Qi Sense, Qi Flight, Sword Flight, Meteor Flight; the Inner Realm, Soul Projection, Possession, a Clone, a core detonated (secret); spirit stones and an Immortal one, a storage formation refined, a thousand-year Cultivation Fruit eaten, a Sacred Vessel, a Dao Fragment, an Origin Stone; a demon core, a Chaos Fiend, an elder beast, a primordial beast, the Primordial Chaos |
| **Worlds Beyond** | The Spatial Gap, the Jade Firmament, the Astral Realm, the Great Void, its Ancient Battlefields, a Holy Land, a Demon Palace, the Shore of Nirvana, the Immortal Realm, the Chaos Frontier, a small world of your own |
| **The Yellow Springs** | Death as a ghost, the Soul River, the Nine Nether Peaks, a Hungry Ghost slain, the ghost path, a ghost spell, both paths among the living, a Ghost King's crown, Meng Po's soup, rebirth |
| **Sects and Cultivators** | A talk, an accepted gift, a sworn friend, a spar won, a sect joined, Elder, Sect Master, a beast's bloodline, a cultivator kneeling to you, a title taken, the Celestial Court |

Where a thing happens the mod fires its milestone (`Milestones`, through its own trigger `defying-the-heavens:milestone`); worlds,
treasures and beasts use vanilla's triggers. What is a standing state (a realm, a sect's title, a Ghost King's crown, a title, a
small world) is looked at every two seconds, so a world from before the advancements catches up as its players join (the user's
pick); one-off events (a spar, a spell) count from now on. Code: `Milestones`, `MilestoneTrigger`, `client/ModAdvancements` (the
tree and its English) and `client/ModAdvancementProvider` (datagen); tests `AdvancementsGameTests`.

## The Dao Codex

The mod's guide in game (the user's ask, 2026-10-27: "a little encyclopaedia or mod guide that players always have access to in game
... Preferably, it doesn't spoil too much (maybe info unlocks as you go?)"). The user's picks: the **J** key ("Open the Dao Codex",
rebindable) and a button on the cultivation menu; a sealed page shown as **???** with a vague hint; a **toast** and a **gold dot** for
what is new; a **player's guide**, what things are and how to do them, with few exact numbers.

- **133 pages in 12 chapters**: First Steps, The Way of Cultivation, The Realms, Methods and Spells, Abilities, Treasures and Alchemy,
  Sects and Cultivators, Beasts and Bloodlines, Bestiary, Worlds Beyond, The Yellow Springs, Laws and Immortality. About a fifth are open from
  the first day (First Steps whole: the Codex itself, the mortal body, the elixir, the cauldron, meditation, the menu, the keys, one's
  physique; and the plain things of the world: lifespan, spirit stones, fruit, herbs, clothing, jade, cultivators, talking, alignment,
  sects and their barriers, demon beasts, bloodlines).
- **The rest open with the advancements** (Advancements, above), any one of a page's: each realm's page as one reaches it (the page
  before tells the way to it), the worlds' pages at the realm that can reach them or on arriving, the dead's pages on death, the Laws
  at True Immortal. A page sealed shows ??? and its hint ("Cultivate further, and this page will open to you", "Only the dead know of
  this", ...); a chapter all sealed shows ??? and a darkened icon.
- **The screen** (since 2026-10-30 as its mockup in the cultivation menu's Hall look draws it, in the mockup's own pixels: see "Drawn
  as the mockups are" below; `client/CodexScreen`): the whole screen in ink with far mountains along its foot. Down a column at the
  left, the Codex's name and how many of its pages are open ("34 of 103 pages") over a rule, then the chapters by name, the chapter
  open in jade with a gold diamond, a chapter with a page unread a gold square (a long name a little smaller before it is cut
  short). Beside them the chosen chapter's pages under its name, the page open in jade with a gold diamond, one unread a gold square,
  one sealed ???; under them, while they all fit, what opens a page still sealed ("A page still closed: Hunt the great beasts to open
  this page.": the one pointed at, else the one shown, else the first). Then the page in a framed panel, its icon and title over a
  rule, its text wrapped to fit, points of a list with a gold diamond, the keys named as they are bound now (in gold, so a rebound
  key reads right), its pictures centred under the rule, a thin gold scrollbar when it runs long. On a screen wider than 16:9 the
  list and the page are kept together in the middle. The mouse wheel scrolls the list or the page (and steps through the chapters
  over their column); up and down choose the page, left and right the chapter, Page Up and Down scroll; J or Esc closes it, back to
  the cultivation menu if it came from there. It opens on the newest page not yet read (a new player: the welcome), else where it
  was left. It doesn't pause the game.
- **New pages**: a toast, "New in the Codex", naming the page (or counting them) with the key; a gold dot on each page not yet read,
  on its chapter, and on the cultivation menu's **Codex** at the foot of its column of tabs (the **book** outside the panel's top right
  where the menu is too narrow for the column; J opens the Codex from the menu too). A world from before the Codex opens what its
  player has already done the first time they come in, in one toast.
- **Kept on this computer**, per world (the singleplayer world's folder or the server's address): which pages were told of and which
  read, and the map's worlds seen, in `config/defying-the-heavens-codex.json`.
- **Pictures** (2026-10-28, the user's pick: diagrams and rows of items): a page line beginning `@` draws one between the words. `@items`
  a row of items in gold-rimmed slots (counts, `+` and arrows between); `@brew pill` the cauldron's recipe for it and `@craft id` a
  crafting recipe, both read from the game itself so they never go stale (the pills, the elixir, the cauldron, the altar, the Storage
  Ring, the World Seed, the stones' nine-to-one...); `@diagram` one of three drawn in ink and gold. Pointed at, an item shows its
  tooltip.
  - **Map of the Worlds** (the user's ask: "a diagram displaying all the realms ... branches between them showing which ones you can get
    to from what one", ??? until you have been there, then "its name and the qi amount and a recommended realm"): a new page, open
    from the first day, first in Worlds Beyond. Twelve worlds (the Overworld, Nether, End, Spatial Gap, Jade Firmament, Astral Realm,
    Great Void, Immortal Realm, Chaos Frontier, Netherworld, and the Inner Realm and small worlds framed apart as reached from
    anywhere) joined by their ways. A world is known by its advancements (so old saves know where they have been) or by having stood in
    it; pointed at, a known world tells its qi against the lower realms' and its ghostly qi against a soul sand valley's (read from the
    game's numbers), the realm to reach before going, and the way in; an unknown one ??? and its way in alone (the user's follow-up:
    the way always shows, or a world is hard to find). A gold ring marks the world one is in.
    Recommended realms: the realm each way opens at (Astral Nascent Soul, Spatial Gap and Jade Firmament Heavenly Being, Great Void Void
    Shattering, Immortal Realm Entering Nirvana Grand Perfection, Chaos Frontier and small worlds True Immortal, the Netherworld Nascent
    Soul for the living, the Inner Realm Qi Refining); the Nether's Qi Refining and the End's Foundation Building are judgement calls
    (no realm gates them), open to change.
  - **The ladder of the realms** (on Realms and Stages): all seventeen realms, each named once its page is open, one's own in gold,
    bracketed by where its qi is rich enough to cultivate it (the lower realms, the Jade Firmament, the Great Void, the Immortal Realm).
  - **The cycle of the five** (on The Five Elements, with a new paragraph on wards): fire, metal, wood, earth and water round a circle,
    each arrow in the colour of the element that overcomes, as `QiElement#overcomes` has it.
- **The Bestiary** (2026-10-28, the user's pick): a chapter of 28 pages, one for every creature (the 16 wild beasts, the 4 elders, the
  Kun-Peng, True Phoenix, True Dragon and Primordial Chaos, the Chaos Fiend, the Hungry Ghost, the Heart Demon and the Vestige of the
  Void). A page stays ??? ("Come upon this creature to record it") until its creature has been seen up close (in sight within 32 blocks,
  the vast ones farther; noted by the client, kept with the Codex's marks), then opens with a toast like any page. Each begins with
  `@beast id`: the creature itself turning slowly in a frame (the Hungry Ghost, which only the ghost shader draws in the world, as a faded
  body in its skin; the Heart Demon in the reader's own), beside it its realm range, element, how it goes about and its temper (all read
  from `BeastKind`), and under it "Drops:" with what it drops (its core and its kin's blood at its least realm, its remains; the
  primordial beasts' and chaos's own spoils), each with its tooltip; then a few lines on where it lives and how it fights. The chapters'
  column is 16 high now so twelve fit.

Code: `Codex` (the chapters, pages, hints and their English, on the common side so a test can hold the pages to the loaded
advancements; the picture lines), `CodexMap` (the map's worlds, ways, visits, qi and recommended realms), client `CodexScreen` (and its
`Book`; lays out and draws the pictures), `CodexDiagrams` (the three diagrams), `CodexState` (what is open and read, the worlds visited;
`mixin/ClientAdvancementsAccessor` reads the client's advancement progress), `CodexToast`; tests `CodexGameTests` (every picture real,
the map true).

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

## Storage Formations

The Storage Rings are gone (the user's ask, 2026-10-28): a **storage formation** folds a space into a sealed block instead.
Craft a Low-Grade one from a chest, four Low Spirit Stones and four jade (stones at the corners, jade at the sides, the chest
in the middle) and place it: it is yours, and only you and the players you trust open it. Its screen, in lacquer and gold:
what it holds as a grid (one well a kind, how many beneath it, shortened to k/M/B), a search line with the owner's Trust and
Refine buttons beside it, how full it is, and the pack below. Click a kind to take a stack (right-click: half; shift-click: straight
into the pack); click with something carried to put it in (right-click: one); shift-click the pack to put a stack in. A
packed formation can't go inside a space, nor anything carrying one (a shulker box, a bundle: the bug sweep of 2026-10-23).

**Its seal**: no pickaxe, blast, spell or hopper breaks it or draws from it. A stranger below the seal's realm is turned
away; one of its realm (on any path) forces it by staying within 6 blocks for its time, its owner warned when they begin and
when it breaks. A broken seal stays open to anyone until its owner opens the formation again, which mends it (a stranger
looking in then is shut out). It stays where it is set down, never a store to carry about (the user, 2026-10-30: "i dont
want them to be a portable form of storage otherwise players never make bases"): sneak + right-click with an empty hand, its
owner takes it up only once it holds nothing, a plain formation of its grade. One packed up with its things before then, or
an old ring unfolded, still sets down with all it held (on the ground it never despawns and is proof against fire and
blasts). **Refine** (beside its search line) raises its
grade in place, its space, owner and trust kept:

| Grade | Holds | Seal (and the realm to refine into it) | Forcing takes | Refining into it takes | Formations mastery |
|---|---|---|---|---|---|
| Low | 27 stacks | Foundation Building | 10 s | crafted | - |
| Mid | 54 stacks | Core Formation | 20 s | 16 Mid Spirit Stones, 8 jade | - |
| High | 81 stacks | Heavenly Being | 40 s | 16 High Spirit Stones, 4 shulker shells | - |
| Supreme | 108 stacks | Void Shattering | 80 s | 16 Supreme Spirit Stones, a nether star | Adept |
| Immortal | **without end**: any number of any number of kinds | True Immortal | 160 s | 8 Immortal Spirit Stones, 4 Immortal Jade | Master |

Crafting one and refining teach Formations mastery (CraftMastery). The world's treasuries (the sects', the clans' hoards,
the ghost palaces') keep their riches in formations of no owner, rolled the first time one is opened, their keepers' rules
over them as over the chests before (the elders' seal, the king's): Mid in the mortal world, High in the Upper Realm and
the Netherworld, Supreme in the Great Void, Immortal in the Immortal Realm and small worlds. A player's own formation set
down in a treasury is under its own seal, not the keepers'. An old Storage Ring a player carries (pack, ring slot, ender
chest) unfolds into the formation of its size with its very space: Mortal to Low, Spirit to Mid, Earth to Supreme, Heaven
and Boundless to Immortal (one worn goes to the pack).

Code: `StorageFormations` (tiers, opening, forcing, refining, treasuries, the old rings), `StorageSpaces` (the spaces, saved
data), `StorageFormationBlock`, `StorageFormationBlockEntity`, `StorageFormationItem`, `StorageFormationMenu`; client
`StorageFormationScreen`, `ClientStorageSpace`; tests `StorageFormationGameTests`.

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
  (Core Formation or higher, on any path, paid from that path's pool: the ghost path's ghost qi) can do it. It spends as much of
  your qi as buys whole years, and ages the tree and every
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
age stays the same on a pedestal. A sect treasury's pedestals (and a demon clan's hoard's) also hold the odd fine pill or
ring, which floats and turns there as itself; only fruit and ginseng have an aura and speed meditation.

Several things speed up meditation. Their shares add together, and the total multiplies the normal rate:

| Source | Boost |
| --- | --- |
| Sitting on a Meditation Mat / Red Silk Meditation Mat | +10% / +20% |
| Each fruit on a pedestal within 4 blocks (2 up or down); only the 4 oldest count | 1 year +1%, 10 years +7%, 100 years +13%, 1,000 years +19%, 10,000 years +25% (+6% for every tenfold age; halved in the retune of 2026-10-27) |
| Height, under open sky: from y 120 up to y 250 | up to +15% |
| Tranquil places: cherry grove, Peach Blossom Sanctuary, snowy slopes, frozen and jagged peaks | +10% |

The best setup today, a red mat and four 10,000-year fruit high on a snowy peak, gathers about 2.45 times as fast (Immortal
clothing adds +48% more, and the Inner Realm x1.25 on top). When you
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

The air's own motes are as thick as its qi (`QiSenseClientHandler#airQi`): some 45 about you in the lower realms, 450 in the Upper
Realm, 4,500 in the Great Void and **45,000 in the Immortal Realm** (its thousandfold air; the user's ask, 2026-10-27, when it showed
barely more than the Great Void's under the old cap of 6,000). Where the air is that crowded, the motes further than 9 blocks off are
drawn as small soft dots without their halo, so it costs no more to draw than the Great Void did. On the Great Void's Ancient
Battlefields their ghostly qi's **cyan yin** drifts besides, as thick as it (`#yinAir`): about half the motes there are yin.

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
- **Craft mastery** (2026-10-28, the user's pick: "the more you do it the better results you get"; `CraftMastery.java`).
  Alchemy, Forging and Formations each grow by practice through five ranks: Apprentice (0), Adept (20), Master (80),
  Grandmaster (250), Saint (600). Each recipe has a difficulty 0 to 3 (the elixir 0; Qi Gathering, Cultivation and Yang
  Preservation 1; Foundation and Yin Gathering 2; Core and Longevity 3) and teaches (difficulty + 1) x 2 practice a brew.
  A brew harder than the brewer's rank fails 12% of the time per rank short (the herbs ruined in smoke, half the practice
  still learned; the elixir never fails, so a first step is never lost). Each rank past Apprentice gives a 10% chance of a
  pill one grade better (`PillGrade#better`: never past the best grade herbs of that age can give at all), and a
  Grandmaster (10%) or a Saint (25%) sometimes draws a second pill. The cauldron keeps who started the brew and their rank
  then. Mastery is the player's own (a soul in another's body learns for itself), saved, synced, and carried over a
  reincarnation. The Stats tab has a third page, **Crafts**: each craft's rank, a gold bar toward the next, and what the rank
  does. A Codex page, Craft Mastery (Treasures and Alchemy, open from the start). Tests `CraftMasteryGameTests`.

```
/cultivation mortal                                   back to Mortal Low
/cultivation addtempering <amount>                    temper a mortal's body (500+ reaches Peak from Low)
/give @s defying-the-heavens:core_pill{PillGrade:4,PillAge:10000}   an Immortal-grade pill (grades 0-4)
```

## The Artifact Forge

Artifacts are forged (2026-10-28, the user's picks: "like the cauldron"; mastery "fail less, finer work"; no refining of
grades, forge new ones). The **Artifact Forge** (shaped: five copper ingots round a blast furnace, two iron ingots and a
Low-Grade spirit stone beneath) is a bronze furnace on four feet. Put a fire under it, as under the cauldron (lava, soul
fire or a magma block: 1.5 times as fast), right-click the materials in one at a time (anything that fits no working with
what's already in is refused, nine at most), then right-click with an **empty hand** to begin: some workings start the
same way (a flying sword's two metal and a stick are a glaive's but for a second stick), so none begins by itself. The one
who began it is its forger: they must stay within 5 blocks and it draws its grade's qi from them every second, or it
stands still (told why every few seconds). Empty hand: what's in, or how far along; sneak + empty hand: the materials
back (a working stops, its qi spent); breaking the forge drops them too.

| Grade | Metal | Stones (2) | Demon core | Time | Qi/s | Difficulty |
|---|---|---|---|---|---|---|
| Mortal | iron ingot | Low | none | 20 s | 1 | 0 |
| Spirit | gold ingot | Mid | Qi Refining+ | 30 s | 3 | 1 |
| Earth | diamond | High | Core Formation+ | 45 s | 8 | 2 |
| Heaven | netherite ingot | Supreme | Nascent Soul+ | 60 s | 25 | 3 |
| Immortal | Immortal Jade | Immortal | Dao Integration+ | 90 s | 200 | 4 |

| Kind | Make-up (besides the stones and core) | What it does |
|---|---|---|
| Flying sword (jian) | 2 metal, stick | blow 6-10, 1.6 swings/s; Sword Flight on it costs 10/20/30/40/50% less qi |
| Saber (dao) | 3 metal, leather | blow 7-12, 1.2/s; the most might (below) |
| Spear (qiang) | 1 metal, 2 sticks | blow 6-10, 1.3/s; pierces 1/1/2/2/3 foes behind the target for 50-75% of the blow |
| Glaive (guandao) | 2 metal, 2 sticks | blow 7-12, 1.0/s; sweeps everyone within 2.5-3.5 blocks for 40-65% of the blow |
| Element banner | 2 white wool, stick | held in either hand, its element's spells +5/8/12/16/20% (not added to a Sacred Vessel's: the greater holds) |
| Qi gourd | pumpkin, glass bottle (+1 metal from Earth) | holds 100/300/1,500/8,000/60,000 qi: sneak + use pours your qi in, use drinks it back |
| Protective jade | 2 jade, string (+1 metal from Earth) | in a ring slot, 10/15/20/25/30% of harm paid from qi instead (a whole health bar's worth costs half the pool); never a tribulation's, the Decays' or anything that bypasses invulnerability; two worn don't stack |

- **Might.** From Spirit grade up a blade adds a share of the wielder's whole attack (MULTIPLY_TOTAL): 5/10/15/25%, a
  saber 7/14/21/32%, so a blade stays worth holding as the realms' own strength grows. Every blade is a sword to ride
  (Sword Flight) and sweeps as vanilla swords do. Uses: 400/900/1,800/3,000/unbreakable, repaired with the grade's metal.
- **The banner's element** is its demon core's; a Mortal banner takes a token instead: blaze powder (fire), a prismarine
  shard (water), any sapling (wood), clay (earth), an iron nugget (metal). Its cloth is tinted the element's colour.
- **Mastery** (`CraftMastery`, Forging): a grade harder than the forger's rank fails 12% per rank short (the materials
  burnt, a demon core given back, half the practice still learned); each rank past Apprentice a 10% chance of **Fine**
  work, each rank from Master 5% of **Perfect** (rolled first). Fine raises all its powers 10%, Perfect 25% (NBT
  `ArtifactQuality`; a blade's blow by its work through Fabric's stack-aware `getAttributeModifiers`, so its tooltip
  reads as a sword's). A working teaches (difficulty + 1) x 2 practice.
- Heaven and Immortal artifacts are fireproof; Immortal and Perfect ones shimmer.
- **Where else.** Sect elders sell a Spirit one (Courteous), sect masters an Earth one (Warm), rogues now and then a
  Mortal one, the Immortal Realm's merchants a Heaven one and an Immortal one (Warm), the Court's Quartermaster the day's
  Immortal artifact to Captains (6 tokens). Sect treasuries hold Spirit and Earth ones, Heaven ones beyond the Overworld,
  Immortal ones in the Immortal Realm. Cultivators buy them back at two thirds of the price (`Artifacts#worth`).
- Code: `ArtifactForgeBlock(Entity)`, `ArtifactRecipes`, `ArtifactGrade`, `ArtifactKind`, `Artifacts`, `ArtifactWeaponItem`,
  `ElementBannerItem`, `QiGourdItem`, `ProtectiveJadeItem`; art `tools/beast-art/ArtifactArt.java`. Codex pages The
  Artifact Forge, Forging Artifacts and Artifacts (Treasures and Alchemy). Tests `ArtifactGameTests`.

### Weapons of the elements (phase 2)

Five weapons made of an element itself (2026-10-28, the user's ask "some made of their element"; `ElementalWeaponItem`),
forged at the Artifact Forge from **Spirit grade up** only: the element takes a weapon's shape through a demon core of that
element (the core's element must match; its realm Qi Refining / Core Formation / Nascent Soul / Dao Integration and up, as
the grade's). The grade's two spirit stones go in too, and the element's own matter:

| Weapon | Matter | Blow (Spirit..Immortal), swings/s | On each blow | Right-click art (reach by grade) |
|---|---|---|---|---|
| Flame Blade (fire) | 2 blaze rods, magma cream | 8/9/10/12, 1.6 | +20-40% of the strike as fire qi; burning 4/6/8/10 s | Flame Arc: a 110° sweep of fire, 4/5/6/8 blocks, sets burning |
| Tide Spear (water) | 3 prismarine crystals | 7/8/9/11, 1.3 | +15-30% as water qi; Slowness II 2-5 s (III at Immortal), puts out fire | Surging Tide: ride a wave 5/6/8/10 blocks forward (to the first wall), striking and slowing all in the way, no fall harm |
| Vine Whip (wood) | 2 vines, Blue Spirit Log | 6/7/8/10, 1.8 | +15-30% as wood qi; rooted 0.75-1.5 s (`WoodSpells#bind`), not again until 2 s after | Binding Lash: lash the first foe on a 8/10/12/14 block line and root it twice as long |
| Mountain Hammer (earth) | 2 obsidian, Jade Stone | 10/12/14/17, 0.8 | +15-30% as earth qi; staggered: knockback 0.8-1.5, Slowness III + Weakness 1-2.5 s | Mountain Quake: everyone within 3/4/5/6 blocks struck and staggered |
| Golden Light Sword (metal) | 2 gold ingots, glowstone dust | 8/9/11/13, 1.5 | +35/45/55/70% as metal qi: the cutting edge | Golden Crescent: a line of golden qi 10/13/16/20 blocks (to the first wall), cutting through all on it |

- The qi share is of the wielder's whole strike (`attackOf`: their attack attribute) and passes armour (`ModDamageTypes#qi`,
  the element spells' harm; the blow's moment of grace set aside for it). The element's effect comes once a full swing at
  most (`ArtifactWeaponItem#mayReach`, as the spear's pierce). Might (MULTIPLY_TOTAL) 6-28% (whip 5-24%, hammer 8-36%).
- **Arts** (main hand only): each costs 8% of the wielder's whole pool (`ART_COST`) and has a cooldown shown on the item
  (Flame 2.5 s, Tide 3 s, Lash 2 s, Quake 4 s, Crescent 2.5 s); each strikes for 60-160% of the strike as the element's qi.
- While held, the element drifts off it (server particles every 6 ticks). Mended with its element's matter; the Flame Blade
  is fireproof at every grade. Sold at half again the grade's price (`Artifacts#price(kind, grade)`): an NPC's random
  artifact may be the weapon of its own method's element; sect treasuries hold Spirit ones, the upper worlds' Heaven ones.
- Art: animated 8-frame strips with `.png.mcmeta` (`tools/beast-art/ElementalArt.java`, written with the artifacts).
  Codex page Weapons of the Elements. Tests `ElementalWeaponGameTests`.

```
/give @s defying-the-heavens:artifact_forge
/give @s defying-the-heavens:heaven_element_banner{ArtifactElement:"FIRE",ArtifactQuality:2b}   a Perfect Heaven fire banner
/give @s defying-the-heavens:immortal_golden_light_sword
```

### Legendary weapons (phase 3a)

Weapons never forged, one of each in the whole world (2026-10-28, the user's ask "some famous weapons ... unique immortal weapons
that some named cultivators have like tamed tribulation lightning", and their picks: one per world, dropped at death and re-bonded).
`Legend`, `Legends` (the record), `LegendaryWeaponItem`, `LeiZhen`, `SwordSpirit`, `LegendSites`; 3b below, the rest of the design in 3c.

- **One true stack**: `Legends` (SavedData) keeps each legend's serial; the stack made last carries it (`LegendSerial`). Any other
  (an older one, the creative menu's, a copy) is a **hollow echo**: no power, and shown so once a player carries it. One lost for
  good (out of the world, a dropped stack left to vanish) **reforms at home a day later**, its trial or keeper with it.
- **Too heavy** below its lowest realm: it slips from the hand. **Bonding**: two minutes' meditation with it in the main hand
  (`BOND_TICKS`); only its owner's hands wake its powers; whoever takes it after must bond anew. **Awakening** by the owner's realm:
  stage 1 at its lowest realm and one on, 2 from two realms on, 3 from four on: blow +2 a stage, might 40/55/70% (0 unwoken 20%).
- **Ganjiang** (yang, from Core Formation): a sword tomb on an Overworld mountain 1,000-2,200 blocks from spawn, laid when someone
  first comes within 96 (old paving, broken pillars, buried bronze-hilted blades: `BuriedBladeBlock`). Within 12 of its stone the old
  blades rise: two waves of sword spirits (3, then 4); beat both, then right-click the stone (`LegendStoneBlock`) to draw it.
  Leave, and the trial waits to be won again. Blows add 30/40/55% of the strike as fire qi and burn 3/4/6 s.
- **Moye** (yin, from Core Formation): a yin pool among the Other Shore's flowers in the Netherworld, 400-800 from its heart. Near
  it, her ghost rises (Core Formation Grand Perfection, a relic keeper) with the sword; slain, she drops it. Blows add yin (the
  dead's harm, passing armour) and chill; her blows reach the dead in full (`GhostSystem#reaches`, `#scaled`).
- **The pair**: one in each hand, both bonded, every blow strikes twice (fire and cold). **Twin Dragons** (right-click, 8% of the
  pool, 15 s): a spirit of each sword held flies out and fights the owner's foes for 6/9/12 s (`SwordSpirit#called`).
- **Heaven's Tribulation Spear** (from True Immortal), a bolt of heavenly tribulation itself (the user's word: drawn by the client's
  `TribulationSpearRenderer`, a jagged spear-shaped bolt of the tribulation's blue struck anew every two ticks, forks crackling off
  it). Carried by **Lei Zhen**, mad (the user's asks): Golden Immortal Grand Perfection (the fourteenth realm), he **roams the
  Immortal Realm's wilds under his own storm** (as the True Dragon's: the sky and fog grey near him, harmless lightning striking about
  him, the oftener while he fights), 800-2,600 blocks from the Southern Heavenly
  Gate and clear of its cities and sects, about three blocks a second whether anyone is near or not; in the world only while
  someone is within 192, he **goes for anyone in his consciousness** (160 blocks), players and cultivators alike (not one in
  creative), raving, and calls the spear's bolt down on whoever he fights. Slain, he drops it. A new owner tames it: two
  minutes' meditation, then three bolts it calls down to survive. Each blow's lightning leaps to 1/2/3 foes about the target.
  - **Heaven's Brand** (right-click, 10% of the pool, 6 s; reach 24/32/40): a tribulation bolt on the foe looked at; a **player** it
    strikes is branded, **10/15/20% more bolts in every heavenly tribulation** they face, for good (one at the least, `TribulationManager#branded`;
    100% at most; heaven's and the Demon Path's tribulations, not the Netherworld's). Lei Zhen's bolt never brands (the user's word).
  - **Held out in its owner's own tribulation** (either hand, bonded) it **drinks every third bolt**: none of those reach them, and
    each makes it the stronger for good (+1% might per 500 bolts, kept in the record so a reformed spear keeps it; the tooltip
    shows the count).
- **Rumours**: every cultivator talks of the legends, in every world (far ones by the world they lie in, near ones by direction
  and distance); a tale is told a player once, and untold legends come first. Codex chapter Legends (three pages, opened by the
  advancements Legends of the World, Yang and Yin, The Tribulation Breaker; One in All the Worlds for bonding).
- Art: Ganjiang's and Moye's 3D models and 16-frame living textures (fire rising, cold sinking along the blade:
  `tools/beast-art/LegendArt.java`, `java BeastArt.java <assets> legends [sheet.png]`). Tests `LegendGameTests`.

### Legendary weapons (phase 3b)

Three more legends, each with its own guardian (`LegendGuards`: a guard hunts its one prey, and whoever strikes it, until one of them
falls or the prey gets 128 blocks away; the hunts aren't saved). Their arts cost 10% of the pool; the vast ones are drawn by `Colossus`
(an item made vast for a moment, its harm landing at its moment: `ColossusRenderer`).

- **Xuanyuan Sword** (from Nascent Soul): a jade shrine (white quartz terrace inlaid with jade, eight pillars, a dais of gold) on the
  **Jade Firmament's highest peak within 640 blocks of its heart**. Only the **righteous** can draw it, and drawing it raises
  **Chiyou's shade** (Nascent Soul Grand Perfection, ox-headed, twice a man's height, the Fallen War God), which hunts the drawer; it is
  made flesh by its hatred, so the living's blows land on it in full. It **won't stay with the devilish** (it flies home to its stone)
  and answers only while its owner is righteous. Blows add 30/40/55% of the strike as golden (metal) qi, **twice that on the evil**
  (the devilish, the Demon Path and demon races, demon beasts, heart demons, the dead), and reach the dead in full.
  **Yellow Emperor's Judgement** (8 s; reach 24/32/40): the sword falls vast out of the sky where you look (10/13/16 times a held one),
  1.5/2.0/2.6 times the strike in golden qi within 4/5/6 blocks, twice on the evil, everyone thrown back.
- **Sun-Shooting Bow** (from Heavenly Being): a dead sun fell on the **barren or ashen world nearest the Overworld**, on its east-facing
  side (its land's middle): a crater of basalt and blackstone, magma and fires, a gilded cinder in its middle. Near it (24) the
  **Golden Crow** rises (a new beast, `BeastKind#GOLDEN_CROW`, three-legged, black and gold, Heavenly Being Grand Perfection, never born
  wild: `Tier#GUARDIAN`); slay it to draw the bow; leave (40) and it returns to the cinder. Rumours name the world. It **needs no
  arrows**: let go, an arrow of sun qi (a burning spectral arrow, full draw 0.9/1.1/1.4 of the strike, plus 30/40/55% as fire qi).
  **Falling Sun** (sneak and let go at full draw, its own 8 s wait: arrows still fly meanwhile; reach 32/40/48): a sun falls and
  bursts, 1.4/1.8/2.4 times the strike as fire within 4/5/6, burning.
- **Sea-Fixing Needle** (from Entering Nirvana): a black iron pillar banded with gold (`SeaPillarBlock`, unbreakable) standing 40
  blocks out of the **Immortal Realm's open sea of clouds**, 600-1,400 from the Southern Heavenly Gate, clear of people and 400 from the
  Kun's sea. A hand on it at Entering Nirvana or above **pulls it free**: the pillar shrinks into the hand, the sea surges (everyone
  within 24 thrown back, the clouds churning), and **Ao Guang, Dragon King of the East Sea** (a dragon-race True Immortal Early, aloft)
  rises to take it back. **Kept behind the ear**: never dropped at its holder's death (`Legends#keepBehindTheEar`, from
  `Player#dropEquipment` and a ghost's rebirth), given back as they live again. Blows add earth qi and throw. **As You Will**:
  right-click, it stretches 24/32/48 blocks where you look (6 s); sneak, it grows to a mountain (18/24/32 times a held one) and comes
  down where you look (10 s; 1.6/2.1/2.8 times the strike within 5/6/8).
- Codex pages The Xuanyuan Sword, The Sun-Shooting Bow, The Sea-Fixing Needle (advancements The Yellow Emperor's Sword, Nine Suns
  Fell, As You Will), and the Golden Crow's Bestiary page. Art: the Xuanyuan Sword (a golden jian, light flowing up its blade), the
  Needle (black iron, gold bands, old characters glowing down it), the bow (red lacquer and gold, a sun on its grip, its string and
  arrow of sunfire; at rest and three pulls), the pillar's blocks (`LegendArt`), and the Golden Crow's body (`Bodies#goldenCrow`).
- A legend's model now stands on its point in its stone and in the vast arts (+135 about z: a model's "fixed" view mirrors it first;
  at -135 the swords lay flat across their stones from 3a on).

### Legendary weapons (phase 3c)

- **Pangu's Axe** (from Earth Immortal, the realm past True Immortal): in the heart of the **Primordial Chaos**; slain, the Chaos drops
  it (floating, glowing) while it is there (`Legends#fromTheChaos`); lost, it is in the next Chaos. Blows add 30/40/55% of the strike as
  metal qi and **pass every ward** (auras, Golden Bell, wards, protective jade: `HeavenSpells#incoming`); **no barrier holds against
  it** (its wielder can break any, at the fastest, and every blow on one shatters it: `Formations#canBreak`, `#batter`); **foes' Law
  domains have no hold on its wielder**, and a blow on a domain's owner folds it. **Split Heaven and Earth** (10% qi, 12 s; 32/48/64
  blocks): a cleave along the land where you look, a tear in space down its length; everyone within 2.5 of it struck 2.0/2.6/3.4 times
  the strike and thrown aside; barriers it crosses shatter, Law domains on it fold; a player's (spell griefing on) splits the land.
- **The Venerables' five weapons** (from True Immortal), the Four Symbols and the Yellow Dragon: the **Azure Dragon Glaive** (wood),
  **White Tiger Saber** (metal), **Vermilion Bird Fan** (fire), **Black Tortoise Mace** (water), **Yellow Dragon Seal** (earth). Each
  in its Venerable's hand at the Celestial Court (`Legends#venerables`), fought with, its art cast now and then. Blows add the element's
  qi (metal's 1.3 times) and its touch: roots slow, burning, chill and throw, stagger and fliers brought down. Arts (10% qi, 20 s): the
  symbol's own Dao Integration spell cast through it at its wielder's realm (Azure Dragon, White Tiger's Pounce, Vermilion Bird's
  Descent, Black Tortoise Bastion, Seal of Ten Thousand Mountains).
- **Dao Duel** (`SparSystem.Terms#DUEL`): a **General of the Court** talking to a Venerable with its weapon in hand may choose Spar, then
  the Dao Duel, however far above them it stands, holding **32 Court Merit Tokens** as the stake; it holds back to their realm. Won, it
  hands the weapon over and keeps its post (`Legends#handOver`; tokens back); lost, it keeps the tokens. One a day with each. A lost
  weapon reforms in its Venerable's hand.
- Codex pages Pangu's Axe and The Venerables' Weapons (advancements Heaven Above, Earth Below; The Four Symbols; A Dao Duel Won).
  Art: six 3D models with living textures (`tools/beast-art/LegendArt3c.java`: a flat shape built a row at a time, each row its own
  slice of the shape's picture).

```
/legend list                          where each is, who has it, its serial (and the spear's bolts drunk)
/legend give <ganjiang|moye|tribulation_spear>   a new true stack (every other one an echo from then on)
/legend reform <legend>               made anew at home now
/legend goto <legend>                 to its resting place (or near Lei Zhen)
/legend brand <player> <percent>      set heaven's brand on a player (0 lifts it)
```

## Bloodlines: the demon races

**Built so far: three of four parts** (the user's ask, 2026-10-23): the races and their looks, the Demon Path and the races'
gifts, and the demon clans in their dens (below). The true beast forms come next, and a bloodline awakened in a human after them.

The first time a player joins, **Choose Your Bloodline** comes up: **Human**, or one of the demon races, **Tiger**, **Lizard**,
**Fox** or **Dragon**, each in three colourings (Golden, White, Black Tiger; Jade, Black, Crimson Lizard; Red, Snow, Silver
Fox; Azure, Golden, Crimson Dragon). One's own figure on the left wears what is picked, turning to follow the mouse. Begin
(or Enter) chooses it, **once and for good**. The screen can't be put aside: until something is chosen it comes up again each
time one joins. A character from a world before bloodlines gets it once too, keeping its realm and all.

- **A demon wears its race's whole look** (the user's rule: "Your own skin MUST change to match that of your race, if you're
  human, it stays as it is"):
  - its race's own skin in place of the player's, broad-armed: fur or scales all over, a lighter belly, clawed hands and
    feet, loose trousers and a sash, in first person too;
  - its beast's head (see Beasts' heads) and its tail.
  - **Tiger:** striped, with round ears, fangs and white cheek ruffs, and a long ringed tail with a dark tip.
  - **Lizard:** scaled, with slit eyes and a forked tongue, and a long tail down to the ground and along it behind. (First
    named a snake; the user renamed it for how it looks, 2026-10-23. An old "snake" bloodline reads as a lizard.)
  - **Fox:** tall dark-tipped ears, a white muzzle and chest, dark socks. **It has one bushy white-tipped tail per realm**:
    one at Qi Refining, nine at Sacred Vessel (the higher of its paths).
  - **Dragon:** antlers, whiskers, a mane and a beard, with a long scaled tail ridged along its back and tufted at its end.
- **Where the look shows:** everyone sees it, including on one's body sitting while the soul is out, a clone, a meditator's
  body, a heart demon, a soul vessel's likeness, and one's soul or ghost (drawn in its light).
  A player in another's body wears the body's blood; in an NPC's, the NPC's look. **Whatever coats a body coats the whole form**
  (the user's rule, 2026-10-23, for every race to come): the wards' skins, Flame Armour, Meteor Flight's heat, a possessed body's
  violet veil, a heart demon's shadow, the light of souls and the dead all take in the beast's head and tail.
- **Where it's kept:** with the world (`Bloodlines`), not in a life's record, so it stays one's own through every life.
- **Changing it:** `/cultivation bloodline` tells which; `set <human|tiger|lizard|fox|dragon> [colouring 1-3]` makes it one;
  `choose` undoes the choice and brings the picker back. A bloodline awakened in a human comes later.

Not the **devilish** path of the alignments (formerly "demonic"; renamed at the user's word, 2026-10-23), which a demon may walk
as well as a man. Code: `Bloodline.java`, `Bloodlines.java` (packets `bloodline_ask` S2C, `bloodline_choose` C2S, `bloodlines`
S2C), client `BloodlineScreen`, `ClientBloodlines`, `BeastLayer`, `BeastHeadModels`, `mixin/AbstractClientPlayerMixin`
(the race's skin) and `LivingEntityRendererMixin` (the man's head taken off); tests in `BloodlinesGameTests`.

### The Demon Path

A third path beside the heaven path and the ghost path, with **the same realm and stage names** (the user's pick) and its own
cultivation bar. The user's rule: "you should be able to cultivate both the heaven and demon path as a demon or human, but as
demons you have a demon core already however as humans you need to form one after receiving a bloodline of a beast. human jade
slips teach demons if they are studying heaven path."

- **A demon is born with its demon core:** its Demon Path begun at Qi Refining Early, told in chat and the journal. The core
  opens its meridians, so a demon is never a mortal: its heaven path starts at Qi Refining Early too. No demon keeps Shattered
  Meridians: one that rolled them as it joined (before its blood was chosen) is given another physique as its core opens them
  (the bug sweep of 2026-10-23: such a demon stayed a mortal with a core it could never cultivate; existing ones are mended as
  they next join). A life spent whole is born again with its core at once, and a body freed of a possessing soul gets the core
  it was born to meanwhile. A human has no Demon Path until it drinks a demon race's blood (below).
- **Demon blood** (the user's ask, 2026-10-24: "blood lines should be a physical item like slips... treated as methods basically,
  they just unlock your potential"): **Tiger, Lizard, Fox and Dragon Blood**, each of a realm ("Core Formation Fox Blood"), drunk
  like a potion; the phial finer the higher its realm, its blood its race's hue. It drops from the race's demons (a clan's cultivator
  one time in three, with its core; a demon player slain by another, with its core: blood of the realm its blood is awake to) and
  from the wild beasts of its kin, as often as their cores (the bloodfang tiger's tiger blood, the ember fox's fox blood, the jade
  viper's, jade-scaled python's and iron-jaw crocodile's lizard blood, the flood dragon's dragon blood); the creative tab has all 44.
  - **A demon** drinks its own race's alone. Its blood is awake to Qi Refining from birth; at a realm's Grand Perfection its Demon
    Path breaks through into the next realm only once it has drunk its race's blood of that realm or higher (as the heaven path
    waits on a method of the realm ahead), the Cultivation tab saying so. Blood it already holds is refused, not wasted. A demon
    from before blood has its blood awake as far as its Demon Path came.
  - **A human** drinks it and a demon core forms: a **half-blood**, its Demon Path begun at Qi Refining Early and its blood's
    element spells coming as it climbs. It still looks human (no tells: the user's word), lives a human's span and is a human to
    every sect and clan, but has its race's gifts at **half strength** (below). Each drink makes its blood purer: 4% for blood below
    the higher of its heaven and ghost paths' realms, 8% at it, 30% a realm above, 15% for blood it already holds. At 100%, or
    drinking blood two realms or more above those paths, it **turns**: wholly that race's (body, look, gifts, a demon's span), its
    Demon Path kept. A drink that would turn it is refused with a warning unless drunk sneaking; there is no way back, and once of a
    race no other race's blood takes. Never a mortal's (meridians sealed), the dead's, nor in another's body.
  - `/cultivation bloodline` tells a half-blood's race, purity and the realm its blood is awake to; the Cultivation tab shows its
    purity under the Demon Path's column.
- **One qi a meditation:** the Path button on the Cultivation tab steps through the paths one has (Heaven, Demon, Ghost) and
  meditation fills the one drawn in. The tab shows a column per path (the drawn one marked), side by side.
- **Its pace:** its realm's and stage's own rate, as the heaven path's, by the race's pace (a dragon's 0.8, the slowest). The
  years it costs (dense qi weighed against its own realm's place), its bottlenecks and seclusion as the heaven path's; each
  stage's gate is **its own tribulation** (the Breakthrough button with the Demon Path drawn in), its strikes 1.25 times as
  hard, Name Your Obsession before its Heavenly Being as on the other paths, and a Heavenly Thunder Body's drunk lightning
  banked into its new stage. Failing it costs a stage of the Demon Path (none for one whose physique holds its stage). A
  surge's reward (wisps, a sequence, a heart demon) counts at the drawn path's rate, the ghost path's too; the heart demon
  comes from Nascent Soul on any path, once in five minutes however often one logs in again.
- **What it teaches:** no methods (its blood opens its realms: above). Its blood teaches the spells of its element as the Demon Path reaches each one's realm (onto
  the spell wheel): **Tiger Metal, Lizard Earth, Fox Fire, Dragon Wood**. The heaven path's jade slips and methods teach a demon
  as they teach a man, for the heaven path. **With the heaven path beside it**, it opens the **spells of both paths**, one a
  realm from Foundation Building, heaven's gold light and a demon's crimson qi as one (since 2026-10-24; see The two paths).
- **Its body:** where the Demon Path stands above the heaven path the body takes its strength from it, 1.2 times; the paths'
  harmony from the two highest. Its qi pool is shared: the larger of the heaven path's and 0.85 of the Demon Path's realm's. A
  demon lives as long as a man (half again as long till the retune of 2026-10-27, when the user had it removed so the talent race
  holds for every race). The realm's abilities (Qi Flight and the rest) come from whichever path reaches the realm.
- **The races' gifts**, whatever path they walk (`RaceGifts`; a half-blood's at half strength: blows ×1.075, venom, roar and
  Illusion half as long and the roar milder, Shed Skin to 37.5%, Charm 5, a dragon's health ×1.1 and its might 4 blocks; sight in
  the dark and breath under water whole, but no swift swimming nor a dragon's early flight):
  - **Tiger:** its own blows of the hand 1.15 times as hard (not its spells, a flight's smash or a core's blast); it sees in the
    dark. From Core Formation, **Tiger's Roar** (ability wheel): all within 10 blocks two stages or more below it weakened and
    slowed for 5 s (10% of its qi, every 30 s).
  - **Lizard:** venomous blows (poison 5 s, stronger from Core Formation and again from Heavenly Being); it swims fast and
    breathes underwater; **Shed Skin**: brought below a quarter of its health, it heals to half (once in ten minutes).
  - **Fox:** Charm (NPCs think 10 the better of it); its tails (by its own realm, in another's body too). From Core Formation,
    **Illusion** (ability wheel): unseen for 6 s, broken by a blow of its own, any harm it does (an arrow, a spell) or a spell it
    casts (10% of its qi, every 45 s); a logout ends it.
  - **Dragon:** Qi Flight from Foundation Building (at Core Formation's cost); 1.2 times the health; **Dragon's Might**: those
    within 8 blocks two realms or more below it are slowed while near; the slowest Demon Path.
  - The roar and the might weigh a demon beast by its own rank, and spare what a spell spares (allies, pets, sect fellows,
    players one may not harm, bodies left sitting) and souls and the dead; the acts aren't a soul's out of its body nor a ruled
    ghost's. Their cooldowns outlast a logout (the bug sweep of 2026-10-23).

Op: `/cultivation set demon <realm> <stage>` sets it there (its blood's spells for the stages risen through onto the wheel), and
`/cultivation addcultivation demon <amount>` adds to its bar (stepping on to the next bottleneck); a human has none, so both refuse
until it has a beast's blood (`/cultivation bloodline set <race>`).

Not yet: the Demon Path's tribulation wears the heaven path's look. Code: `DemonState` (saved in the record as "Demon"),
`DemonPath`, `RaceGifts`, the Path switch in `TwoPaths`, `MeditationManager#demonPath`, `TribulationManager#startDemon`; tests in
`DemonPathGameTests`.

### Demon clans

A **demon clan** is a sect of one demon race (the user's design, 2026-10-23), named for it ("Iron Fang Tiger Clan", "Jade Scale
Lizard Clan", "Nine Tails Fox Clan", "Azure Cloud Dragon Clan"), living as a sect lives (titles by strength, the barrier and core,
the treasury and its seal, off-screen lives, replacements, ascension), its master the **Clan Head**, its element its race's (its
elders teach that element's heaven methods). Its people are demons of its race in the race's whole form (skin, head and tail, any
of the three colourings, a fox's tails by its realm), cultivating the Demon Path: never mortals, no Ritual to the Heavens on the
way.

- **Where:** in the Overworld some three in five as many as sects (since 2026-10-24: they came to a fifth, "super rare", 40 chunks
  apart and kept 9 chunks off every place a sect might stand; now 28 apart and 7 off), kept clear of villages and of wherever a sect may stand, each race in
  its own lands (tags `clan/<race>`): **Tiger** the forests, dark forests, taigas and jungles; **Lizard** the swamps, mangroves,
  deserts, badlands and savannas; **Fox** the birch woods, cherry groves and snowy plains and taigas; **Dragon** the highlands
  (meadows, windswept hills and forests, groves). In the Upper Realm 45 in a hundred of the sects' cells are clans' instead (a quarter before 2026-10-24), of any
  race, a dragon's two times in five. They come into being unseen near players, the spawn and the rift, as sects do.
  **With Terralith** (the user's ask, 2026-10-23), 60 of its 85 surface biomes are a race's too, by the same rule: Tiger its
  jungles and conifer forests (the Siberian taiga and grove, the shield, lush valley); Lizard its deserts, oases, mesas,
  savannas, shrublands and the orchid swamp; Fox its birch, sakura, lavender and moonlight woods and its snowy woods and
  shrublands; Dragon its highlands, misty and cloud mountains, windswept spires and plateaus. Its cliffs, peaks, volcanoes,
  sky islands, isles, beach, river and wastes are no race's. They are optional entries: without Terralith nothing changes.
- **Their dens** (the user's pick: wilder dens, a layout of each race's own), hollow mounds and caves on level ground, along
  trails, among the land's trees and undergrowth, with the core on its plaza in the middle:
  - **Tiger:** boulder mounds of mossy stone; the Clan Head's cave hall at the back (fires, bones, furs); the elders' smaller
    mounds; a long rock overhang where the disciples sleep; a hoard cave; a ring of standing stones to spar in, with clawed posts;
    two standing stones for a gate; boulders about the grounds.
  - **Lizard:** domes of mud brick and packed mud under arching mangrove roots; a basking pool in the head's dome; a low burrow
    and a hoard; a root arch for a gate; pools with reeds and lily pads about the grounds.
  - **Fox:** grassy hillocks with round birch-framed doors; the head's little white shrine with vermilion pillars on the
    greatest of them, steps up to it through a cutting; a tunnel of red torii up the way from the gate; stone lanterns.
  - **Dragon:** one great hollow crag of dark rock banded with jade, snow on its top, at the back of the grounds: inside, the
    disciples' mats across the floor, the head's floor raised at the back, the hoard (gold, raw gold, amethyst, chests, pedestals)
    in an alcove, the elders' ledges up the walls; rock spires and a pool before it.
- **Men** (the user's pick: wary): a clan thinks 20 the less of a man, 10 the more of another demon, 20 the more of its own race
  (`Regard#bloodShift`; demon rogues too). A man on a clan's grounds is set upon (not while he spars with one of them, or just
  after); off them he is let be unless he provokes it. A clan takes no man ("You smell of men..."), so its barrier never opens
  to one as its guest.
- **Joining** (the user's pick: any demon): as a sect, by asking an elder; one of the clan's own race needs no trial. No soul
  joins in another's body ("There is another soul behind those eyes..."), a sect of men neither. The Sect tab and `/sect info`
  name a clan's master its Clan Head. Clans keep 10 chunks from villages, as sects do.
- **Cultivators from below:** a demon ascended to the Upper Realm joins a clan, of its own race if one will have it; a man, a
  sect of men.
- **Demon rogues:** one wandering cultivator in eight is a demon of the race whose land it is in (any race in the Upper Realm).

Code: `DenStructure` (the den's plan, its sites), `DenPieces` (the dens drawn), `SectPalette#ofRace`, `Sect#race`,
`CultivatorNpc#setRace` (synced, saved "Race"), `SectNames#clan`, `SectSites` (clans' sites too), data
`worldgen/structure/demon_clan*.json`, `structure_set/demon_clans*.json`, `tags/worldgen/biome/clan/*`; tests in `ClanGameTests`.

### Wild demon beasts and demon cores

**Wild demon beasts** (the user's ask, 2026-10-23): beasts in their animal forms, with no mind yet (mobs), each with a rank on the
cultivation scale from Qi Refining Early up to **Core Formation Grand Perfection**, as strong as a cultivator of that rank (its
kind's health and armour on top) and the larger the higher it stands (a twelfth for each stage above its kind's least). A
consciousness domain senses its realm; Realm Suppress weighs it as a cultivator's. Sixteen kinds, each with its own animated model
(the user's pick: own models), in three tiers:

| Tier | Ranks | Kinds (signature move) |
|---|---|---|
| Common | QR Early..GP | Iron-Hide Boar (charge), Shadow Wolf (packs; a quarter harder at night), Jade Viper (venom), Rock-Shell Turtle (draws into its shell), Wind Hawk (swoops), Fire Monkey (throws burning stones, in troops) |
| Uncommon | QR Late..FB GP | Bloodfang Tiger (pounce), Frost Ape (ice slam: slows, freezes), Earthquake Bear (ground pound: throws all near up and back), Iron-Jaw Crocodile (holds, drags under in water), Lightning Antelope (spark charge), Ember Fox (fire breath) |
| Rare | FB Late..CF GP | Golden-Winged Roc (dive with a gust), Jade-Scaled Python (coils round its prey and crushes), Horned Thunder Rhino (stunning charge), Flood Dragon (water serpent: jets of water) |

- **Temper** (the user's pick: territorial): most go for players and cultivators who come near (14 blocks); the turtle and the
  antelope only fight back. None goes for one a realm or more (four stages) above it. Only the hunters keep a player from sleep.
- **Where:** they turn up round players by day and night, 24-64 blocks off and out of sight, in their kinds' lands (biome tags
  `beast/<id>`: e.g. tigers in jungles, savannas and dark forests; frost apes in the snows; flood dragons in oceans and rivers,
  in water deep enough), at most 6 round a player and 80 in a world, never within a sect's or clan's barrier or close to its
  grounds, and not in peace. The rare are rare (tier weights 60/30/10); in the Upper Realm they come far oftener (20/40/40) and
  stand higher. Gamerule `demonBeastsSpawn` (on) turns them off, and so does the world's own `doMobSpawning`. The caps hold for
  each of a pack. They wander off when no one is near, as monsters do. One brought into the world some other way with no rank
  (a command, another mod) is given one at once, whole at it.
- **Their moves** reach only what they see (no slam, breath or gust through a wall), throw the heavy the less by their knockback
  resistance, and never touch souls, the dead, spectators or the invulnerable; a roc's gust comes only with a dive that lands;
  a python's coil lets go of what is no longer its prey and puts it back only where there is room, never through a wall; a
  wolf pack turns only on what it may attack (the bug sweep of 2026-10-23).
  **With Terralith** (the user's ask, 2026-10-23) every one of its 85 surface biomes is some beasts' land, by its climate and
  look (tigers and wolves in the Siberian taiga, crocodiles and turtles at the oases, hawks and rocs over the sky islands, ember
  foxes and fire monkeys on the volcanoes...), as optional entries: without Terralith nothing changes.
  (`BeastGameTests#terralithsLandsHaveTheirBeastsAndClans` holds the list to Terralith 2.5.4's real biomes.)
- **Demon cores** (`DemonCoreItem`): one item, its realm, stage and element (the beast's, or the race's) and what it was taken from
  on it; it looks its realm (five looks, Qi Refining to Heavenly Being and beyond, larger and brighter, its light its element's
  colour). No use yet: it is to be essential to crafting and artifact forging. Who leaves one (the user's picks: rises with rank,
  slain by a player): a wild beast a third of the time at Qi Refining, two times in three at Foundation Building, always at Core
  Formation; a demon cultivator (clan member or rogue) always, of its rank (above Core Formation too); a demon player only when
  slain by another player (the killing blow's, or the kill's credit: struck by them and finished by a fall), never by another
  mod's machine (a FakePlayer), and they lose nothing else by it.
- Besides its core a beast leaves its kind's remains (hide, meat, bones, feathers, scales, blaze powder, prismarine).

Code: `BeastKind` (the kinds, tiers, signature moves, lands), `DemonBeast` (the mob: rank, strength, size, temper, drops),
`DemonBeastGoals` (moves and targets), `BeastShot` (stones and water jets), `BeastSpawner`, `DemonCoreItem`; client
`BeastModels` (renderer, code-driven animation: gaits, head turning, tails, winding serpents, wings, acts); art from
`tools/beast-art` (`java BeastArt.java <assets> bodies`: `Bodies` for the models, textures and glow masks, `Cores` for the core
icons and models; every cube's UVs are written face by face, `BeastArt#faceUv`: GeckoLib lays a box UV out by the cube's size
rounded down, so the beasts' many cubes a fraction of a pixel thick read the wrong pixels until 2026-10-24); data
`tags/worldgen/biome/beast/*`; tests in `BeastGameTests`.

## Physiques

Every player is born with a **physique**, rolled the first time they join, told in chat and written in the journal; the
Stats tab names it (and its roots), with its grade's colour. An op can change it: `/physique get|set <id>|reroll [player]`.
The rarer the grade, the rarer the roll. Out of every 100 births (evened out 2026-10-20, the user's ask): **Low 56, Mid 24,
High 12, Supreme 5, Immortal 3**. The grade is the body's **talent** (2026-10-21/22): what the years meditation costs are divided
by, Low x1, Mid x5, High x24, Supreme x40, Immortal x64 (the talent ladder of 2026-10-27; it was x1, x1.5, x2.1, x4.5, x16;
Lifespan: the talent race), and a small edge to its pace on both
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
| Primordial Chaos Body | Immortal | 0.4 | Meditation feeds **both paths at once**, each at its full pace (the living, the ghost path begun); no element barred; its spells (the heaven methods' qi, both paths' heaven-and-demon qi, the ghost path's harm) strike a quarter harder. The heavens hate it: every tribulation strikes half as many times again (rounded up: Core Formation's 6 become 9, Four Axis's 100 become 150), at its realm's pace, so it takes the longer; its lifespan tribulation strikes half as often again |
| Soul-Devouring Body | Immortal | 0.4 | Every cultivator it slays, NPC or player, gives it a tenth of what the victim's stage takes, on the path it cultivates (as far as fits before a bottleneck); a player it slays loses a stage of their own, on their higher path (the user's ask, 2026-10-20: a friend killed again and again to feed one pays for every time); each righteous or neutral one drags its alignment 10 toward the devilish; the righteous think 20 less of it |
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
- **the meditation's boost**: mats, pedestals, height, tranquillity, garments (the percentage told as you sit down), up to about x2.93;
- **the Inner Realm**: x1.25 while your soul is turned inward (since the retune of 2026-10-27; before, it only sped the gain);
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
| Qi Refining | 84 (a body's own, from 16) | 0.9 | 76 |
| Foundation Building | 200 | 3 | 600 |
| Core Formation | 400 | 30 | 12,000 |
| Nascent Soul | 600 | 18 | 10,800 |
| Heavenly Being | 1,200 | 26 | 31,200 |
| Four Axis | 10,000 | 30 | 300,000 |
| Dao Integration | 100,000 | 33 | 3.3 million |
| Void Shattering | 10 million | 48 | 480 million |
| Sacred Vessel | 100 million | 75 | 7.5 billion |
| Entering Nirvana (the middle boundary's end) | 10 billion | 88 | 880 billion |
| True Immortal | without end | none: its cultivation costs no years | |

Where each gets to, in each realm's own world (the years left over from a realm carry into the next; a percentage is how far into
that realm). "Gear" is a red mat, Immortal clothing, height and a calm place; "pedestals" four 10,000-year fruit; "everything" all
of that with the Inner Realm, a spirit stone and the method's stele:

| Talent | Nothing | Red mat + Inner (x1.5) | A method (x1.5) | Method + red mat + Inner (x2.25) | Method + gear + 4 pedestals + Inner (x5.5) | Everything (x10.3) |
|---|---|---|---|---|---|---|
| Low x1 | FB Mid (35%) | FB Late (58%) | FB Late (58%) | FB GP (94%) | CF Mid (26%) | CF Late (53%) |
| Mid x5 | CF Mid (23%) | CF Mid (37%) | CF Mid (37%) | CF Late (58%) | FA GP (96%) | SV Late (69%) |
| High x24 | FA GP (82%) | **VS GP (75%)** | VS GP (75%) | SV GP (73%) | True Immortal | True Immortal |
| Supreme x40 | VS GP (84%) | **SV GP (82%)** | SV GP (82%) | True Immortal | True Immortal | True Immortal |
| Immortal x64 | SV GP (88%) | **True Immortal** | True Immortal | True Immortal | True Immortal | True Immortal |

So (the user's ladder of 2026-10-27, the grades' talents and `SECLUSION_SHARE` retuned with scratchpad `tune3.ps1`; every boost
saving years as before): on a red mat in the Inner Realm alone, **a High grade reaches Void Shattering, a Supreme one Sacred Vessel
and an Immortal one True Immortal**, and further with more. A Mid grade (x5: x13 at first, brought nearer the Low grade the same
evening, "its okay if it cant reach true immortal") reaches Core Formation so, Four Axis with good gear and pedestals, Sacred Vessel
with everything. A Low grade with everything the lower realms give, the best Longevity Pills' 400 years too, runs out of years
inside Core Formation's Grand Perfection (87% of the realm); demons alike, now that they live no longer than men. Only denser qi than a realm's
own world lifts a talent past its line: a Heavenly Being in the Jade Firmament (x10), or anyone carried up into a denser realm, or a
guest in a small world (x1,000, left uncapped by the user's choice). Code: `Lifespan#seclude`, `#seclusionYears`, `#saving`,
`#placeSaving`, `#realmSeclusion`, `SECLUSION_SHARE`, `Physique.Grade#talent`, `#speed`.

**How long it takes** (the same retune). Every stage's cultivation is `PlayerCultivation#STAGE_LENGTH` (2.1: 4.2 at first, halved
the same night, "lets half the time in total it takes to reach each realm"; the years stay as they were) times its realm's base,
on the heaven path, the Demon Path and the ghost path (the world's cultivators keep their own pace). Qi Refining Early to True
Immortal, meditating at x1 in each realm's own world:

| Talent | Nothing | Red mat + Inner | A method | Method + red mat + Inner | Method + gear + 4 pedestals + Inner | Everything |
|---|---|---|---|---|---|---|
| Low | 3.7 days | 2.5 days | 44.9 h | 29.9 h | 7.7 h | 4.1 h |
| Mid | 3.4 days | 2.3 days | 40.8 h | 27.2 h | 7.0 h | 3.7 h |
| High | 3.1 days | 2.1 days | 37.4 h | 24.9 h | 6.4 h | 3.4 h |
| Supreme | 2.8 days | 44.3 h | 33.2 h | 22.1 h | 5.7 h | 3.0 h |
| Immortal | 2.5 days | 39.9 h | 29.9 h | 19.9 h | **5.1 h** | 2.7 h |

(The time the road would take, whether or not the years last for it: talent doesn't speed it beyond the grade's small edge. Surges
played well, up to x2, and the pace x2 or x3 make it shorter still, the pace at its price in years.)

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
and **Entering Nirvana**, which ends the middle boundary, then **True Immortal**, the first of the major boundary, and since phase 3
the ladder past it to **Heavenly Lord** (below, Past True Immortal). Each has the four stages; all are the Upper Realm's alone, as Four Axis is (needing ten times the cultivation,
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
| Earth Immortal | 115,200 | 32,000 | 5,000,000 | 125,000 | x2 (phase 3: the ladder, below) |
| Heaven Immortal | 230,400 | 64,000 | 10,000,000 | 250,000 | x2 |
| Golden Immortal | 460,800 | 128,000 | 20,000,000 | 500,000 | x2 |
| Taiyi Golden Immortal | 921,600 | 256,000 | 40,000,000 | 1,000,000 | x2 |
| Profound Sovereign | 1,843,200 | 512,000 | 80,000,000 | 2,000,000 | x2 |
| Heavenly Lord | 3,686,400 | 1,024,000 | 160,000,000 | 4,000,000 | x2 (the last) |

Armour, toughness and knockback resistance stay at Four Axis's: vanilla's caps hold them there anyway (80% of a blow turned).
**Vanilla's caps on max health (1,024) and attack damage (2,048) are lifted** (the user's ask; `BodyCaps`, a mixin accessor on
`RangedAttribute`), on the server and the client alike. The pool refills in some 40 s as Four Axis's does, and flight and the
sword never outrun the gathering. Breaking from Four Axis Grand Perfection into Dao Integration waits on the last axis of
virtue, Enlightenment (each axis holds its own stage's breakthrough). The rest of the mod follows: the heaven methods (below,
Heaven methods), the ghost path to True Immortal (Ghost methods), tribulations of 200 to 3,200 strikes (Tribulations), the years
(Lifespan: a True Immortal's without end; the talent race's shares, above), their few cultivators (Sects and cultivators), the
Netherworld's yin borne twice as long each realm, the Inner Realm's sky turning to the void's violet and then to gold, the
Quasi- names ("Quasi-Dao Integration"; only past the last realm's Grand Perfection, Heavenly Lord's since phase 3, "beyond"). Dao Integration has its own spells
(2026-10-24: the five elements' guardian beasts and the realm's methods' arts, below, Heaven spells: Dao Integration), and so does
Void Shattering (2026-10-24: space torn open and what comes through it, Heaven spells: Void Shattering); the realms after it none
yet, nor new abilities.

**The top-left corner** (the user's asks, 2026-10-22, to pictures of theirs, in the mod's own style; redone 2026-10-30 in the
cultivation menu's Hall look, the user's pick: "the hall panel but remove the black boxes behind the UI and shrink it slightly",
drawn as its mockup is, in the mockup's own pixels and Pixelify Sans: see "Drawn as the mockups are" below):
with bodies of thousands of hearts, vanilla's hearts at the bottom are held to **one row of ten** (`client/mixin/GuiHeartsMixin`:
each heart a tenth of the whole once past twenty; the armour row sits over it as for a body of twenty). Nothing in the corner has a
panel behind it: a gold diamond, your name and the higher of your paths' realms, then **two flat bars** (`client/VitalsHud`, drawn
by `client/HallArt`): your **health** and your **qi**, each in a thin dark-gold frame on a see-through track, what it holds written
across it in white outlined in black ("Health 1,407 / 1,407", or "1.40M / 5.80M": past ten thousand a number keeps three figures
and a letter, so the bars never grow with a realm). The health crimson, lit along its top, a blow's pale trail
draining after the fill, absorption a band of gold along its top; the qi the mod's blue, a glint flowing along it while it fills
(not in a Qi Flight that costs more than gathering brings in). Nothing of hunger, air or armour up there (the user's word): those
stay at the bottom, vanilla's. The health shows when vanilla's hearts do (not in creative or spectator), the qi once your meridians
open; under them thin bars of the same look go on (a soul's own pool in another's body, immortal essence while it holds any or the
land can fill it, the ghostly qi, yin corruption: `QiHud`, `NetherworldHud`), then the pressure line, and a projected soul's
readout hung from them on a dashed cord (its tether as a bar, red while it strains, and the body's health: `SoulHud`). The **readied
spell** and the **selected ability** sit in two slots right of the hotbar, past the offhand slot and the attack indicator
(`client/HotbarSlots`): each its icon, its word over it, its key in its corner; a cooling spell dim with its seconds
across it, an ability that is on framed in jade with a green diamond. Vanilla's boss bars keep clear of the corner (the user,
2026-10-26: "the qi bar covers the boss health bar"): where the centred bars would run under it (a large GUI scale), they are drawn
beside it on their own row, or under it on a screen too narrow for that (`client/HudCorner`, `client/mixin/BossHealthOverlayMixin`).

**The cultivation menu's Hall** (2026-10-30, the user's pick of the redesigns: the full-screen menu, drawn as its mockup is;
`client/CultivationScreen`): the menu takes the whole screen in ink, far mountains along its foot, its tabs down a column at the left
under your name and the higher of your paths' realms (the tab open in jade with a gold diamond), the Dao Codex at the column's foot.
The **Cultivation tab** is rebuilt for it: the **Ladder of Realms** beside it (a rung a realm, Qi Refining at its foot, a diamond on
the rung of each path walked: gold the heaven path, cyan the ghost path, red the Demon Path, closer together where all three share
a rung), then a panel with your title's field, your realm large and under it its stage (or what the body comes to), and a card for
each path you walk, the heaven path, the ghost path, then the Demon Path: its name, realm and stage, "Drawn in" on the one whose qi
a meditation draws, its bar in the path's colour (the heaven path gold, the ghost path teal, the Demon Path crimson; full at the very
last) with the stage's cultivation written in it and what meditating adds a second, its pool under it (the qi, the ghostly qi; the
Demon Path draws on the qi); then the details two to a row (method, effects, what meditating here adds) and the status (a held realm
in a crimson box, the virtue bars). What won't fit scrolls under a scrollbar, the buttons (Meditate, the pace, Path, Breakthrough)
staying at the panel's foot. The other tabs keep their contents in the new frame (a heading with a gold diamond, the page open of a
row of pages in jade, On switches framed in jade).

**Every other screen in the Hall's look** (the user's ask, 2026-10-30: "go through the other menus and make them consistent with the
others too"): the talk wheel and the dialogue, the ability and spell wheels, Choose Your Bloodline, Name Your Obsession, inscribing a
manual, the Formation Core, the mission board, the storage formation, the Heaven Altar, a seized body's pack and the Codex's toast:
ink panels in a thin dark-gold frame, headings with a gold diamond and a rule, the row or tab chosen in jade, cream words and gold for
what matters, and the Hall's buttons in place of vanilla's stone ones (`client/HallHud`, `client/HallButton`, `client/TalkUi`).

**Drawn as the mockups are** (the user's asks, 2026-10-30: "it doesnt look like what it looks like in the art artefact thing" and "i
want the UI and HUD to look EXACTLY the same as the artefact at all GUI scales"; their picks: Pixelify Sans, and the same size on the
screen always): the mockups are a 1280 x 720 frame, and that frame is fitted to the window whatever the GUI scale (`client/HallArt`:
window pixels to one of the mockup's), so their numbers are drawn as they stand. The HUD is drawn in those pixels; every screen of
the mod's is laid out in a frame of 427 x 240 at 16:9 (three of the mockup's pixels to one of the frame's, wider or taller on another
shape of window; `client/HallFrame` and its mixins: the screen's size, the mouse, the scissor), so the HUD, the cultivation menu, the
Codex and every other screen look the same at every GUI scale and grow with the window (vanilla's hotbar, hearts and inventory still
follow the GUI scale). Words are in **Pixelify Sans** (copyright 2021 The Pixelify Sans Project Authors,
github.com/eifetx/Pixelify-Sans, from Google Fonts, under the SIL Open Font Licence 1.1: `assets/defying-the-heavens/font/
pixelify_sans.ttf`, the licence beside it as `pixelify_sans_ofl.txt`), each drawn from a font made at the size it comes to on
the screen (`client/HallFonts`: one from 6 to 112 pixels, vanilla's own font standing in for the few marks Pixelify lacks), so they
stay crisp; placed as a browser places them (a CSS font size on a CSS line) and outlined two pixels each way where the mockups' are.

**Swift** (the user's ask, 2026-10-22: "void shattering beings and onwards should be able to travel through space or just in
general super fast"). From Void Shattering on, Qi Flight and the free flight were made far faster, the more with every stage
(Void Shattering 2.4 to 3 times vanilla's flying speed up to True Immortal 6 to 7.2); since 2026-10-23 that swiftness is
**Meteor Flight's** (below, Meteor Flight: True Immortal 200 blocks a second), switched on and off, and Qi Flight is creative
flight's own speed at every realm, for fine work. Space is swifter still (The Astral Realm: to the next star, Void Shattering half
a minute, True Immortal five or six seconds). A sword stays a sword's speed. Vanilla snaps back anyone but the game's host who
moves more than 10 blocks in a tick; a flier may now move as far as its flight carries it, a second of it for packets that come
in bunched (`QiFlight#moveAllowanceSq`, `ServerGamePacketListenerImplMixin`), so a guest isn't pulled back mid-flight.
NPC cultivators fly as before.

**No lower realm holds Void Shattering** (the user's ask, 2026-10-22: "once reaching void shattering realm the lower realm no
longer supresses you"). From Void Shattering on, on either path, Realm Suppression in the Overworld, the Nether and the End holds a
cultivator no longer: its whole health, strength, qi pool, abilities and flight there (`PlayerCultivation#FREE_OF_LOWER_REALMS`,
`#isSuppressed`, `#sustained`, `#wielded`; an NPC cultivator the same, `CultivatorNpc#sustainedRank`). It meditates there too, at the
lower realms' thin qi, up to its stage's full bar; the breakthrough itself still waits for the Upper Realm (`#isBreakthroughLocked`).
Four Axis and Dao Integration are held to Heavenly Being Grand Perfection there as before.

**Titles** (`PlayerTitles`, `ClientPlates`; the user's ask, 2026-10-27). From Void Shattering on (either path) you may name yourself:
type a title into the field under your realm on the Cultivation tab and press Enter, up to 50 characters (colour codes and control
characters are dropped, runs of spaces made one); empty it and press Enter to clear it. It stands over your name in gold, in brackets,
with your sect's title above it in the sect titles' own way, so both show at once, and cultivators call you by it (see Talking to
cultivators). Taking one is told to everyone on the server in chat, as an advancement is: "Name has taken the title [title]", the
title in gold with the realm you took it at over it (a few tries within thirty seconds are told to you alone). Below Void Shattering
it is kept but neither shown nor used.

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
someone else, fill no one's axis; crafted into other grades they lose the mark. **No more is shared than the axis still wants**
(the user's pick, 2026-10-24: wishes were an endless spring of stones): a share is cut to what is left of the axis' need, less
what you already carry unabsorbed, and while you carry enough to complete it a settler won't take your goods ("Take it into
yourself first"). What you carry is forgotten a tenth of the axis' need a day (`Wishes#owedFortune`), so stones lost or given away
never bar you for good.

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
| A devilish cultivator | a cultivator of the devilish path, Nascent Soul Early to Heavenly Being Late, out for you |

Each one of them that falls by your hand (your blow, arrow or spell, or your Earth Golem's) fills the Axis of Merit by its
hearts, as it stood before any Realm Suppress (a ravager 50, a pillager, vindicator or evoker 12, a wolf 4; a devilish
cultivator by its rank). The wish is granted when **every one of them is dead and at least one fell by your hand**. Slain only
by others (the sun, a fall, the settlers), or taken from the world some other way, the threat is gone but the wish stands, to be
taken on again. A threat leaves its spoils (its loot and gear, an evoker's totem) only when it falls
by your hand: slain by anything else it drops nothing, so letting it die and taking the wish on again farms nothing (the
2026-10-20 sweep). The **devilish cultivator** is out for you and you alone, whatever your path: it never
runs from your domain or a beating, never begs for its life, never goes off into seclusion or to ascend (a Heavenly Being of
the lower realms would), and never turns on the settlers. On **Peaceful**, where no monster lives, whatever preys on them comes
as a devilish cultivator (the wolves still come as wolves). A threat that dies
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
against it (no mob of the living world hunts the dead), though a devilish cultivator, of Nascent Soul and above, sees it and does.

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
Refining to True Immortal (the twenty-five of the realms after Four Axis since 2026-10-22, the user's ask: their methods then;
Dao Integration's and Void Shattering's own spells since 2026-10-24, the realms after them still to come). Each has its own layers and a passive that grows with them, and teaches its element's **spells** of
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
wheel (V) as they come, never the ability wheel, cost heaven qi and have cooldowns (a player's spells wait them out only with the
gamerule `spellCooldowns` on: off by default since 2026-10-24, the user's ask; cultivators keep theirs). A spell that needs something to take hold
of (a being looked at, the ground, earth to draw on) isn't cast without it, and nothing is paid. Their qi harm is the
element's own and passes armour; none touches its caster, spectators or its tamed creatures, and nothing of one (harm, hold,
push, drowning, burning) touches another player unless the server lets players harm each other (PvP on, teams allowing);
blessings like Life Force Gain still reach them. The spells that pick out foes in their reach (a Sword Prison, a whirlpool, the
void tree's roots, a lock on space, a quake's field, a sea of flames) take any cultivator your spells don't spare, minding its own
business or not (since 2026-10-24: a rogue took nothing from a Sword Prison), and their healing mends none of them; beasts and
village folk minding their own business are left be. Terrain a spell carves or tears up is gone for good, the ground and whatever
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
| Fire | **Blazing Meteor** (34) | Calls a burning meteor down out of the sky onto where you look, up to 48 blocks, its mark glowing on the ground as it falls: 45 fire harm to everything within 6 blocks, burning for 6 s, and a crater 13 blocks round blown out of the ground (or whatever is built there), its floor burnt to magma and blackstone, fires round it. |
| Wood | **Thorn Spear Forest** (30) | Spears of thorny wood burst out of the ground 6 blocks round where you look, up to 32 blocks: every foe there takes 35 wood harm and is held where it stands for 2 s. |
| Wood | **Lotus of Rebirth** (34) | A great lotus opens at your feet for 10 s: you and every friendly being within 10 blocks are cleansed of harmful effects and fire at once, and heal 3 health a second while it blooms. |
| Earth | **Summon Golem** (30) | Raises an earth golem out of the ground beside you for 90 s. It flies (at three fifths of a cultivator's flight), keeps by you and fights for you: whatever strikes you or you strike, or a monster that comes for you. 80 health, 18 earth harm a blow. Two at most: a third sends the oldest back to the earth. |
| Earth | **Earthquake** (34) | The ground shakes 20 blocks round you for 4 s and splits: fourteen fissures up to 7 blocks deep run out from near you as far as 40 blocks, and every foe on the shaking ground takes 14 earth harm a second, is slowed hard and thrown about. |
| Water | **Glacial Spears** (30) | Five spears of ice form over your head and fly in a fan at where you look: 11 water harm each, slowness for 3 s and the cold's frost. |
| Water | **Great Tide** (34) | A wave 10 blocks tall and 14 wide rolls out 30 blocks the way you face: whatever it overtakes takes 30 water harm, is swept along hard and put out. |
| Metal | **Piercing Sword Light** (30) | A beam of sword light pierces everything along your look for 64 blocks, walls and hills too: it bores a tunnel 5 blocks across through whatever stands in its way, and every being along it takes 55 metal harm. |
| Metal | **Vajra Body** (34) | For 12 s your skin turns to gold: 35% less harm taken (70% less of wood's), nothing can knock you back, and whoever strikes you in melee takes 4 metal harm. A metal ward. |

**Heavenly Being**

| Element | Spell (layer) | What it does |
|---|---|---|
| Fire | **Phoenix Descent** (39) | A phoenix of fire takes wing from you and flies at where you aim, bending toward its mark, up to 64 blocks, and bursts there: 90 fire harm to everything within 5 blocks, burning for 6 s, and a scorched crater 7 blocks round. |
| Fire | **Sea of Flames** (43) | The ground round where you look, up to 48 blocks, becomes a sea of flame for 10 s: the land itself burns 31 blocks round (grass scorched, patches turned to magma and netherrack, fires on whatever stands there, trees catching), and every foe standing within 20 blocks of its heart takes 20 fire harm a second. |
| Wood | **Heaven-Piercing Bamboo** (39) | Giant bamboo bursts up out of the ground under as many as 6 foes within 24 blocks of where you look, breaking the ground open 4 blocks round each: 70 wood harm each, and they are thrown high into the air. |
| Wood | **Spring of Myriad Life** (43) | A green rain of life falls 32 blocks round you for 10 s: you and every friendly being in it heal 40 health at once, then 4 a second. |
| Earth | **Falling Mountain** (39) | A mountain of rock forms in the sky over where you look, up to 64 blocks, and falls on it: 140 earth harm to everything within 10 blocks, a shockwave, a crater 22 blocks round and 13 deep (its floor broken rubble), and ten fissures splitting the land out to 48 blocks. |
| Earth | **Heavy Earth Seal** (43) | Every foe within 24 blocks of you is pressed to the ground for 6 s: it cannot jump or fly (a flier is brought down), is slowed hard, and takes 20 earth harm a second. Under its weight the land 37 blocks round you sinks by up to 3 blocks, cracked and split by fourteen fissures (all but where you stand). |
| Water | **Heavenly River Torrent** (39) | A river pours out of the sky along a line from you to where you look, 32 blocks, for 2 s: everyone in its fall takes 60 water harm and is swept along it, and the river scours a channel 6 blocks across into the ground along its line. |
| Water | **Frozen Field** (43) | The cold runs out from you to 64 blocks round over two and a half seconds: open water freezes, frost lies on the ground within 32 blocks, and every being it reaches (but your allies and pets) is frozen solid for 3 s. The ice thaws over the next 20 to 40 s, and until it has, every being in the field takes 3 water harm a second. |
| Metal | **Mountain Cleave** (39) | Flings a crescent of sword qi as tall as a tower the way you look. It flies 140 blocks, cutting a slot 6 blocks wide and 23 tall through hills and mountains alike (what it cuts is gone), and cleaves every being in its way for 150 metal harm. |
| Metal | **Sword Domain** (43) | Thirty-six swords circle you 10 blocks round for 12 s: every foe within takes 20 metal harm every half second. |

**Four Axis**

| Element | Spell (layer) | What it does |
|---|---|---|
| Fire | **Nine Suns Descend** (48) | Nine suns fall out of the sky, one after another, onto where you look and round it, up to 64 blocks: each bursts for 150 fire harm 7 blocks round, burning for 6 s, and blows a scorched crater 9 blocks round out of the ground: together they leave the land a burning field of craters 19 blocks round. |
| Fire | **Phoenix Nirvana** (52) | For 60 s the phoenix's fire waits in you: if you would die, you rise again instead at full health, harmful effects burnt away, in a burst of flame that strikes everyone but your allies within 10 blocks for 200 fire harm. Once. A tribulation's killing strike it doesn't hold off. |
| Wood | **Myriad Vine Binding** (48) | Vines burst up and seize as many as 12 foes within 40 blocks of where you look, up to 64 blocks: each is held fast for 5 s and takes 22 wood harm a second. |
| Wood | **World Tree** (52) | The World Tree grows out of the ground where you look (or before you), its roots heaving the land open in twelve fissures out to 40 blocks, and towers over all for 12 s. Its vines reach from its boughs for every being but you within 32 blocks of its trunk (your pets spared), lift them into its branches and hold them there, drinking 10 of their health a second into you. |
| Earth | **Earthen Titan** (48) | Raises an earth titan three times a golem's size beside you for 60 s: it flies, keeps by you and fights for you, 400 health and 80 earth harm a blow, its fist striking the ground round its mark too. One at a time. |
| Earth | **Mountain Seal** (52) | A mountain of stone bursts up over where you look, up to 48 blocks, 7 blocks round and 9 tall, tearing the land open round its foot in fourteen fissures out to 40 blocks: everything inside is entombed, held fast for 5 s and crushed for 60 earth harm a second. The mountain crumbles away after 20 s; its stone gives nothing. |
| Water | **Absolute Zero** (48) | A beam of utter cold freezes the being you look at solid, within 48 blocks: 200 water harm, and it is held frozen for 5 s. When anything strikes it, or the cold lets go, it shatters for 60 more. |
| Water | **Boundless Sea** (52) | A sphere of the sea 16 blocks round fills the place you look at, up to 48 blocks, for 8 s: every foe inside is held in it, drowning, and takes 22 water harm a second. When it lets go, the ground under it is left crushed into a basin 18 blocks round with a lake in it. |
| Metal | **Heaven-Severing Sword** (48) | A sword of light as long as a tower forms in your hand and you swing it level across your front: a giant crescent of sword light some 110 blocks across leaves its edge and flies out level from your chest the way you face, 120 blocks, faster than anything can flee it: nothing stops it. It cuts through whatever its blade passes through, 5 blocks thick at its middle and thinner toward its tips: hills and mountains are sliced through (their tops left standing above the cut), trees felled, whatever is built in its way cut in two, and the land at its far end is cut in the crescent's curve. Every being its blade passes through takes 300 metal harm and is hurled ahead of it. |
| Metal | **Four Swords Array** (52) | Four giant swords plant themselves at the corners of a square 32 blocks across round where you look, up to 48 blocks, each driven deep into the ground in a crater 7 blocks round: for 8 s every foe inside is held in by the walls of light between them and takes 80 metal harm a second. |

**Dao Integration** (2026-10-24, the user's ask: the five elements' guardian beasts at layer 57, the realm's methods' own arts at 61)

| Element | Spell (layer) | What it does |
|---|---|---|
| Wood | **Azure Dragon** (57) | The Azure Dragon of the east, a dragon of living wood with jade light in its grain, bursts out of the ground where you look, up to 82 blocks, and coils up into the sky, 56 blocks high. Everything within 18 blocks of where it rises takes 480 wood harm and is caught in its coils, carried up with it and held there for 3 s. The land it bursts from is torn open in fourteen fissures out to 51 blocks. |
| Wood | **Myriad Woods Return to the Dao** (61) | A spirit tree of jade light grows where you stand and holds the land 41 blocks round for 15 s: every foe in it is rooted and drained of 40 health a second, all of it shared out as healing to you and your own within the woods. |
| Fire | **Vermilion Bird's Descent** (57) | The Vermilion Bird of the south, a bird of living flame, its wings 53 blocks across, stoops out of the sky behind you and sweeps low along your look for 154 blocks, over the hills in its way: everything in its 20-block path takes 450 fire harm and burns for 8 s, and the land beneath it is scorched into a burning trench. |
| Fire | **One Flame Burns the Heavens** (61) | A seed of white flame flies from your hand to where you look, up to 82 blocks, and blooms into a pillar of fire 10 blocks round that rises to the clouds, 118 blocks, for 6 s: everything in it takes 120 fire harm a second and burns (a water ward of a lower realm only halves it). The ground under it melts into a glassy pit 20 blocks round and 13 deep, it burns a shaft up through whatever stands over it to the open sky (cast underground, a tunnel of fire to the surface: its walls magma, netherrack and blackstone, fire on every ledge), and the land 33 blocks round where it breaks out is set ablaze. |
| Earth | **Qilin Tramples the Land** (57) | A qilin of rock, molten gold in its cracks, rears beside you and stamps, then gallops out along your look for 128 blocks, over the land: every stride cracks a crater in it, and everything it tramples takes 480 earth harm and is thrown high. |
| Earth | **Seal of Ten Thousand Mountains** (61) | An imperial seal of carved stone as great as a hill falls out of the sky onto where you look, up to 82 blocks: 550 earth harm to everything within 18 blocks, held pressed to the ground for 3 s, and the land 41 blocks across pressed down 4 blocks, split by twelve fissures out to 40. The seal stands there a while, then fades. |
| Water | **Black Tortoise Bastion** (57) | The Black Tortoise of the north, black water under a dome of warding light, rises round you, its serpent a stream coiled about its shell, and keeps by you for 12 s: you and your own within 10 blocks of you take 40% less harm, and fire's not at all (a fire spell of a deeper realm still breaks half through), and its serpent strikes a foe within 20 blocks every second for 200 water harm, throwing it back. A water ward. |
| Water | **Hundred Rivers Converge** (61) | A hundred streams pour together into a great whirlpool where you look, up to 82 blocks, for 5 s: every foe within 31 blocks is dragged into it and held, taking 90 water harm a second. As it turns its rivers pour in from its rim and flood the whole of it within 3 s, the land scoured into a basin as wide (8 deep at its heart) full to the brim. When it bursts it flings them out, and the lake stays. |
| Metal | **White Tiger's Pounce** (57) | The White Tiger of the west, made of steel and sword light, leaps from you onto where you look, up to 82 blocks: 500 metal harm to everything within 10 blocks where it lands. Then its claws rake out ahead of it: four gouges 102 blocks long are cut through the land, and everything in them takes 300 metal harm. |
| Metal | **Myriad Swords Return to One** (61) | Ten thousand swords gather in a turning vortex high over where you look, up to 82 blocks, and fuse into one colossal sword 53 blocks long that falls: 600 metal harm to everything within 15 blocks, a cross of slots 60 blocks out each way and a pit 19 deep where it is driven in. It stands there a while, then fades. |

**Void Shattering** (2026-10-24, the user's ask: space torn open and what comes through it, at layers 66 and 70; Jianmu Pierces the
Heavens, a tree, became Spring Thunder of the Azure Emperor, two spells already growing one)

| Element | Spell (layer) | What it does |
|---|---|---|
| Wood | **Spring Thunder of the Azure Emperor** (66) | A storm cloud of jade, the thunder trigram ☳ burning under it, gathers 40 blocks over where you look, up to 118 blocks, and calls spring thunder down in six volleys a second apart: every foe within 26 blocks of the spot is struck by a bolt of jade lightning for 220 wood harm and rooted where it stands, and two more bolts strike the land in each volley, each leaving a little crater. |
| Wood | **Void Withering** (70) | Space tears open in a slash across the sky 33 blocks over where you look, up to 118 blocks, and once it is open the roots of the Void-Rooted Divine Tree spill down out of it. For 8 s they reach out and drink: a root lashes down to each foe within 52 blocks, from a little over the rift down to 40 blocks under the land beneath it (eight at most at once; 37 round and 16 down before 2026-10-24, the user's ask for longer roots), coils round it and hauls it 9 blocks up into the air (a great beast is only held where it stands), and every foe within reach loses 200 health a second, weakened and slowed, all of it shared out as healing to you and your own within reach; the land under them withers outward from their heart (grass to coarse dirt, plants dead). Then they let go (what they held lands unharmed by the fall) and draw back up into the rift. |
| Fire | **Golden Crow Descends** (66) | The sky tears open 61 blocks over where you look, up to 118 blocks, and the Golden Crow, the three-legged crow that carries the sun, made of the sun's own fire, bursts out of it and dives: everything within 22 blocks of where it strikes takes 1,600 fire harm and burns for 10 s, and the land melts into a glassy crater 13 blocks round, burning 26 round. |
| Fire | **Sun Pulled Through the Void** (70) | Space tears open in a slash across the sky over where you look, up to 118 blocks, and a sun is dragged out of it, swelling as it comes, down clear of it to hang 29 blocks over the land; the rift shrinks away behind it. It hangs there 5 s: everything within 41 blocks beneath it takes 100 fire harm a second and burns, and the land scorches. Then it collapses and bursts: 1,200 fire harm to everything within 26 blocks, thrown out and burning, and a glassy crater 17 blocks round and 9 deep. |
| Earth | **Buzhou Falls** (66) | The sky tears open 70 blocks over where you look, up to 118 blocks, and the broken pillar of Mount Buzhou, that once held up heaven, falls out of it, 59 blocks long: everything within 22 blocks takes 1,600 earth harm and is pinned to the ground for 2 s, the land is smashed into a crater 17 blocks round and 13 deep, and fourteen fissures split it out to 70. The pillar stands in it 5 s, then fades. |
| Earth | **Earth Veins Lock the Void** (70) | The earth itself heaves up round you: nine ribs of rough rock burst out of the land and bend in to meet over you in a dome 26 blocks round, the earth's molten gold showing in their cracks, boulders torn up drifting inside it, the earth trigram ☷ burning under its keystone, and for 6 s space inside it is locked: every foe in it, or that comes in, is held where it stands (it can't walk, fly or be thrown out), crushed for 200 earth harm a second, and can't step through space (Nine Nether Step, a Spatial Tear). Then the veins sink back, and the land is left as it was. |
| Water | **Weak Water from the Void** (66) | Space tears open 41 blocks over where you look, up to 118 blocks, lying flat across the sky and open below, and the Weak Water, that bears nothing up, not even a feather, pours out of its underside onto the land for 6 s: everything within 26 blocks of where it falls sinks (nothing flies in it, the dead neither), is slowed, has its fires put out, and takes 160 water harm a second. As it pours its flood runs out from where it falls over all 26 blocks within 2.5 s, the land sinking under it into a basin (7 deep at its heart) full to the brim, and the lake stays. |
| Water | **Kun Rises from the Northern Abyss** (70) | The land where you look, up to 118 blocks, opens onto the northern abyss, 41 blocks across, and the Kun, the great fish of the north made of the abyss's black water, breaches out of it and leaps 59 blocks high across your sight: everything within 22 blocks takes 1,500 water harm and is thrown high; as it dives back in, 500 more, and the land there is crushed into a basin 18 blocks round with a lake in it. |
| Metal | **Void-Severing Sword Light** (66) | A sword of light thrusts out of your hands along your look, 147 blocks in a third of a second, through everything, and cleaves the land under it into a chasm in the shape of the rift it leaves: pointed at both ends, some 13 blocks across and 17 deep at its middle, deepest along its middle, its sides and floor ragged (and a slot through whatever stands on its line). Every being on its line or down in the chasm takes 1,500 metal harm and is hurled on along it. Behind it space stays cut open for 3 s, and whoever stands in the cut, on the line or down in the chasm, takes 400 more a second. |
| Metal | **Ten-Direction Sword Prison** (70) | Space tears open in ten places round where you look, up to 118 blocks: out of eight rifts in a ring 41 blocks up, great swords plunge into the land 13 blocks round the spot (walls of sword light between them, a crater at each), and for 6 s every foe inside is held in and takes 150 metal harm a second. Then the last two swords, out of a rift high over its heart and one torn in the ground beneath it, meet in its heart: 1,000 metal harm to everything inside, and a pit 7 blocks round where they met. |

**Sacred Vessel** (2026-10-24, the user's ask: the sacred treasures of the old tales, each called down and wielded, at layers 75 and 79)

| Element | Spell (layer) | What it does |
|---|---|---|
| Wood | **Heaven-Swallowing Gourd** (75) | A gourd of living jade wood on its vine comes down out of the sky mouth first, to hang 19 blocks over where you look, up to 136 blocks, and its stopper flies off: for 5 s it swallows. The land under it is torn up into it, a bowl 19 blocks round, and every foe within 30 blocks is dragged up toward its mouth: whatever reaches it is swallowed, held inside, weakened and drained for 450 wood harm a second (a great beast is only dragged and drained where it stands). Then it spits them out: 900 wood harm, hurled far. |
| Wood | **Seven-Treasure Bodhi Branch** (79) | A colossal branch of the bodhi tree, hung with the seven treasures, forms high at your left and sweeps down and across your front, an arc 150 degrees wide out to 85 blocks: everything it sweeps through takes 3,500 wood harm and is swept away along its stroke, and the land under it is scoured away, as much as 6 blocks deep midway out (nothing within 6 blocks of you). Then its treasures burst into their seven lights. |
| Fire | **Wind-Fire Wheels** (75) | Two great wheels of fire and whirling wind flare up before you and roll out along your look for 128 blocks, over the land, weaving 13 blocks out to either side and crossing midway: everything a wheel runs over, within 6 blocks of it, takes 1,800 fire harm and burns for 10 s, and each burns a trench into the land behind it. Where they meet again at the end they burst: 1,200 fire harm to everything within 17 blocks, thrown out, and a scorched crater 11 blocks round. |
| Fire | **Eight Trigrams Furnace** (79) | Laozi's furnace of the eight trigrams, bronze on three legs with Samadhi fire in it, falls out of the sky onto where you look, up to 136 blocks: 1,000 fire harm to everything under it, and a scorched crater 11 blocks round. For 6 s it burns: every foe within 26 blocks is drawn into its belly and held there, burning for 300 fire harm a second, and every 1.5 s jets of Samadhi fire burst out of its eight windows along the land for 51 blocks, striking whatever is in their way for 500 and burning trenches. Then it bursts open: 1,500 fire harm to everything within 26 blocks, hurled out, a glassy pit 19 blocks round and the land scorched 34 round. |
| Earth | **Xirang, the Self-Growing Soil** (75) | A clod of Xirang, the soil that grows of itself, that Gun stole from heaven to dam the flood, flies from your hands to where you look, up to 136 blocks, and swells: the land there heaves up into a mound 17 blocks round and 13 high, stone at its heart and earth and grass over it, burying whatever stands there. Every foe in it is held fast, buried, for 4 s and crushed for 500 earth harm a second; then the mound settles on them, 600 more. The mound stays. |
| Earth | **Nine Cauldrons Suppress the Land** (79) | The nine bronze cauldrons Yu the Great cast for the nine provinces fall out of the sky one after another, the first on where you look (136 blocks), the rest in a ring 30 blocks round it: each strikes everything within 11 blocks of where it lands for 1,500 earth harm and smashes a crater 9 blocks round (the first 13). Once all nine stand, for 6 s every foe inside the ring is pressed down (it can't fly or leap, slowed to a crawl, weakened) and takes 250 earth harm a second, the land within sinking 6 blocks. Then they rise away. |
| Water | **Twenty-Four Sea-Fixing Pearls** (75) | The twenty-four pearls that fix the sea unwind round you, circling 7 blocks out, and fly off one after another at the foes within 64 blocks, nearest first, round and round them (at where you look, with none): each bursts with the weight of the sea, 140 water harm to everything within 4 blocks, and leaves a little crater. |
| Water | **Ocean-Sealing Vase** (79) | A vase of white jade painted with waves, that holds an ocean, comes over where you look, up to 136 blocks, its mouth tipped down 30 blocks over the land, and drinks for 5 s: every drop of water within 42 blocks is drawn up out of the land into it, and every foe within it dragged up into its streams; whatever reaches its mouth is held there and crushed for 400 water harm a second. Then it seals itself and comes down mouth first: 1,500 water harm to everything within 21 blocks, thrown out, and the ocean it held poured back into a crater 17 blocks round, a lake in it. |
| Metal | **Immortal-Binding Golden Ropes** (75) | A ring of golden rope hung with charms and bells comes to hang 21 blocks over where you look, up to 136 blocks, and its ropes lash out to the foes within 30 blocks of the spot, eight at most, nearest first: each is bound (it can't walk, fly or strike back), hauled up to hang under the ring and held there for 5 s, constricted for 450 metal harm a second. Then the ropes cinch, 900 more, and let go; what they held lands unharmed. A great beast is bound where it stands. |
| Metal | **Golden Dragon Scissors** (79) | A pair of colossal golden scissors, each blade a dragon of gold 64 blocks long, opens 17 blocks over where you look, up to 136 blocks, lying level across your sight, and snaps shut: everything between its blades as they close takes 3,500 metal harm and is cut down to the ground. The land is cut 5 blocks wide and 15 deep where the blades meet, and scarred along where they lay open. |

**Entering Nirvana** (2026-10-24: death and rebirth, at layers 84 and 88; its two sword spells redesigned the same day at the user's ask)

| Element | Spell (layer) | What it does |
|---|---|---|
| Wood | **Seed of Withering Rebirth** (84) | A seed half withered and half alive flies from your hands to the being you look at, up to 154 blocks (to where you look, with none), and roots in it: it is bound where it stands, and for 5 s the seed's vines drink 500 of its health a second, half of it mending you. Then the flower of rebirth blooms out of it: 2,500 wood harm to what it rooted in, 1,200 to every other foe within 12 blocks, and the land 29 blocks round flourishes. |
| Wood | **Withering and Flourishing** (88) | The wheel of the seasons, spring's green and autumn's dry earth turning about a tree half in blossom and half bare, grows out of the land where you look, up to 154 blocks, 34 blocks round, and turns for 8 s: every foe on it is drained of 450 health a second and slowed, three tenths of all it drains shared out mending you and your own on it; the land to your left flourishes, to your right withers. Then its last turn: 3,000 wood harm to every foe on it, a fifth of it mending you. |
| Fire | **Undying Ember** (84) | The ember the phoenix leaves when it burns, wrapped in feathers of flame, flies to where you look, up to 154 blocks, and hangs 5 blocks over the land. It bursts three times, a second and a quarter apart, each greater than the last: everything within 10, then 13, then 17 blocks takes 1,100, then 1,500, then 2,100 fire harm, burning for 6 s, thrown out, the land blown into scorched craters 6, 8 and 11 blocks round. |
| Fire | **Pyre of the Heavens** (88) | A ring of cribs of great logs, stacked crosswise and burning, walls of fire between them, rises 29 blocks round where you look, up to 154 blocks, and burns for 8 s: no foe inside gets out through its walls of fire, and every foe within takes 150 fire harm a second, 100 more each second after. Then it collapses on its heart: 2,500 fire harm to every foe within, a scorched crater 29 blocks round and 12 deep, burning 43 round. |
| Earth | **Gravestone Falls** (84) | A colossal stele written with the names of the dead, on a stone tortoise, falls onto where you look, up to 154 blocks: 3,000 earth harm to everything within 10 blocks, pinned for 3 s, a crater 12 blocks round cracked out to 34. It stands 2 s, then sinks into the land, dragging every foe within 24 blocks to it and burying whatever is within 14: 1,500 more, and a pit 10 blocks round and 14 deep. |
| Earth | **Buried World** (88) | A ring of eight standing stones, carved with the spirals and runes of the old world, heaves up 34 blocks round where you look, up to 154 blocks. For 6 s the land within sinks, 4 blocks deeper every second, every foe within dragged down with it and crushed for 500 earth harm a second. Then the stones topple in: 3,500 earth harm to every foe within, the land left a vast pit. |
| Water | **Frozen Moon in the Well** (84) | A crescent moon of ice rises over where you look, up to 154 blocks, its reflection shining up out of a well of still black water 18 blocks round, and the land 24 blocks round freezes. For 4 s every foe in the well is held frozen in it and takes 400 water harm a second. Then the moon shatters: 3,200 water harm to every foe in the well. |
| Water | **The Frozen Moon Falls** (88) | A moon of ice, cratered and cracked with cold light, falls out of the sky from 96 blocks onto where you look, up to 154 blocks: 6,500 water harm to everything within 24 blocks, thrown out, a crater of ice 29 blocks round and 14 deep, frozen round it out to 38, every foe within chilled for 8 s. |
| Metal | **Sword Tomb Awakens** (84) | The grave of fallen swordsmen, a ring of 24 of their swords buried point-down in the earth, bursts up 12 blocks round where you look, up to 154 blocks: everything within the ring takes 1,000 metal harm, thrown up. The swords tear free, rise over the ring, points turned in on its heart, and plunge into it one after another: 160 metal harm each within 6 blocks of where it lands (3,840 from all 24), the last leaving a crater 7 blocks round and 5 deep. |
| Metal | **Nine Rebirths of the Sword** (88) | A colossal sword wreathed in white flame forms over where you look, up to 154 blocks, its hilt over the spot and its 55-block blade reaching out, and sweeps round it eight times, 210 degrees a sweep, turning the other way each time and reborn in its white flame between them, its point skimming the land, rising off it or falling onto it: everything within 53 blocks it passes over takes 600 metal harm, once a sweep. Reborn a ninth time high over the spot, point-down, it plunges into it: 2,000 metal harm within 24 blocks, a crater of glass 14 blocks round and 7 deep, cracks running out 38. |

**True Immortal** (2026-10-24: the elements as they were before heaven and earth were parted, at layers 93 and 97; the realm's leap of x10 in its harm)

| Element | Spell (layer) | What it does |
|---|---|---|
| Wood | **Primal Wind of Creation** (93) | A cyclone of the primal wind, green and gold, the leaves and seeds of creation whirled up in it, forms where you look, up to 218 blocks, and wanders on along your look for 8 s, 56 blocks: every foe within 41 blocks is drawn in to it, and those within 17 whirled up round it as high as 24 and struck for 3,000 wood harm every half second (a great beast only drawn, slowly); the land it passes over flourishes. At its end it flings them out, 12,000 each; a twentieth of all its harm mends you and your own near it. |
| Wood | **First Seed of Creation** (97) | The seed of creation comes down out of the light where you look, up to 218 blocks, and the dome of life grows over it, great vines arching up out of the land 48 blocks round to meet over it; the land within flourishes. For 6 s it drinks: every half second every foe within is drawn on for 2,500 wood harm and slowed, none can leave, and three tenths of all it draws mends you and your own within. Then the tree of creation bursts up through the dome: 45,000 wood harm to every foe within, thrown up, the same share mending your own. |
| Fire | **Samadhi True Fire** (93) | The three true fires of Samadhi, the heart's crimson, the spirit's gold and the essence's white, erupt where you look, up to 218 blocks, in three pillars braided about one another, and burn for 8 s: every foe within 34 blocks is drawn in to them, and every half second every foe within 17 takes 2,500 fire harm, true fire that burns the fire-proof too. Then the three meet in one white flare: 20,000 fire harm within 27 blocks, thrown out, a scorched crater 17 blocks round and 9 deep. |
| Fire | **First Flame Before Heaven** (97) | The flower of chaos-fire that burned before heaven, its petals of white flame and black by turns, opens where you look, up to 218 blocks, 34 blocks round, and burns for 5 s: every half second every foe within takes 3,000 fire harm. Then its petals slam shut on its heart, drawing every foe within in, and its heart bursts: 45,000 fire harm to every foe within, thrown out, a crater of glass 27 blocks round and 12 deep. |
| Earth | **Hand of Houtu** (93) | The hand of Houtu, mother of the earth, of the land's yellow jade veined with gold, rises open out of the land where you look, up to 218 blocks, and closes: every foe within 17 blocks takes 15,000 earth harm and is caught up in its fist, 15 blocks up (a great beast is only crushed, and held slowed). It holds them a moment, then brings its fist down forward onto the land, carrying them with it: they and everything within 20 blocks of where it lands take 45,000 earth harm, a crater 20 blocks round and 9 deep, cracks running out 61. |
| Earth | **Falling Continent** (97) | A land torn up whole, meadows, forests, snow-capped mountains and a lake on it, its bedrock hanging jagged under it, falls out of the sky from 136 blocks onto where you look, up to 218 blocks, in 2 s, tumbling, its weight pressing down every foe under it. It strikes: 75,000 earth harm to everything within 41 blocks, thrown out, a crater 31 blocks round and 12 deep, cracks running out 82. |
| Water | **Glacier of the First Age** (93) | A wall of ancient ice 48 blocks across, ranks of seracs taller toward its back, heaves up 14 blocks ahead of you and grinds forward along your look, 102 blocks in 5 s: every foe before its face is shoved along with it (one it overruns thrown out ahead; a great beast only ground past), struck for 2,500 water harm every quarter second and chilled, the land in its wake frozen. At its end the great slabs of its face calve forward: 10,000 water harm to everything within 20 blocks before it. |
| Water | **The Primordial Sea** (97) | The waters before heaven and earth well up where you look, up to 218 blocks, in a ring of towering waves 39 blocks round, facing in, the sea within them wheeling in a whirlpool. For 5 s every foe within is dragged round in it (a great beast only slowed), none can leave, and every half second it drowns them for 3,000 water harm. Then its waves crash in over its heart: 45,000 water harm to every foe within, the land under it left a lake 27 blocks round and 9 deep. |
| Metal | **Heaven-Slaying Sword Rises** (93) | A sword of sword-light that could slay heaven itself, 94 blocks long, its guard of gold swept up like wings and a halo round its point, rises point first out of the land where you look, up to 218 blocks: everything within 10 blocks of its blade takes 40,000 metal harm, thrown high. It stands a moment, then flares: eight cuts run out from its foot across the land, 82 blocks long, 20,000 metal harm to everything within 5 blocks of one (once), the land cut along them 4 wide and 7 deep. |
| Metal | **Primal Axe Splits Chaos** (97) | Pangu's axe, that split heaven from earth, stands behind you, 85 blocks tall, is raised back over you, and chops down forward along your look: everything within 10 blocks of its line, out to 89 blocks, takes 75,000 metal harm, thrown aside, and the land is split into a chasm along it, 9 blocks wide and 27 deep (from 10 blocks ahead of you), cracks running out 34 where its head lands. |
**The great spells' living shapes** (the user's ask: "use geckolib for animations and also use unique and well-crafted detailed
models"). Each of Dao Integration's and Void Shattering's spells comes as a living shape of its own, a GeckoLib entity (`SpellConstruct`,
its kind one of thirty: Dao Integration's seventeen, the dragon, the spirit tree, the bird, the flame's seed and its pillar, the qilin,
the seal, the tortoise, the whirlpool, the tiger, the vortex of swords and the colossal sword, the ghost foundation's procession and gate
of the dead, the Heaven-Demon Avatar of both paths and its two hands, heaven's palm and the demon's claw; Void Shattering's thirteen, the
jade storm cloud, the void tree's roots, the Golden Crow, the sun, Buzhou's broken pillar in its chains, the dome of the earth's rock,
the Weak Water's fall, the Kun, the sword of light, the sword prison, the Nine Nethers' rings of ghost fire, the Naihe Bridge's spans, and
the Heaven-Demon fist), drawn see-through and full bright with its glow (`SpellConstructRenderer`,
GeckoLib's `AutoGlowingGeoLayer`). Its spell moves and turns it tick by tick (on every watcher's screen) and calls its moves: the
dragon's roar, the bird's stoop and flare, the seed's bloom, the pillar's eruption, the qilin's rearing, the seal's stamp (its face's
four characters of seal script spreading over the ground round it), the serpent's strike, the whirlpool's burst, the tiger's pounce
and its claw, the swords' fusing, the gate's slam, the avatar's two strikes; the cloud's flash at each volley, the roots drawing back,
the crow's strike, the sun's collapse, the pillar's impact, the veins sinking, the water drying up, the prison's last two swords
meeting, the Nethers' sealing, the fist's impact. No glyph drawn on any of them makes a swastika
(`Fire#glyph` draws another in its place). A shape forms in its first pose and fades in; a spell ended lets its shape fade out over half a
second (`SpellConstruct#dissolve`), and the flame's seed and the vortex of swords fade into the pillar and the sword they become.
Every move and enter keys each bone its shape's loop moves (what it leaves out goes on with the loop's own motion) and ends where the
loop begins, so nothing snaps from one to the next; checked in game frame by frame, every bone logged (2026-10-24). They are never
saved. The five spirit beasts are made of their elements (the user's ask: "made out of the element they are cast from rather than
looking like actual animals"): the dragon of living wood, the bird of flame, the qilin of rock and molten gold, the tortoise of black
water under its dome of light, the tiger of steel and sword-light; the true beasts they first were are kept for later in
`tools/spell-art/stored` (`java SpellArt.java --stored <dir>` writes them again; their files go back into the assets as they are). The
Immortal Realm's True Dragon is made here too (`java SpellArt.java --primordial <assets>` writes it among the demon beasts'). The
models, their textures and
animations are written by code (`tools/spell-art`, run with JDK 22 or later: `java SpellArt.java <assets> [ids]`, and
`java Preview.java <assets> <out dir> <id>` to draw what it wrote), as the demon beasts' are (`tools/beast-art`); a model's every face
is painted at its own density, so a thin feather or flame takes texture only on its two broad sides. Their icons too
(`java Icons.java <assets>`).

**Sacred Vessel's, Entering Nirvana's and True Immortal's shapes** (2026-10-24) go the same way: Sacred Vessel's thirteen
treasures (the swallowing gourd on its vine, the bodhi branch hung with the seven treasures, the wind-fire wheel, Laozi's
furnace, Xirang's clod, the nine cauldrons, a sea-fixing pearl, the ocean-sealing vase, the binding ring and its golden ropes
reaching to what they bind, the dragon scissors, the karma mirror, the soul-locking pagoda, the Heaven-Demon Bell);
Entering Nirvana's fourteen of death and rebirth (the seed of withering rebirth and its flower, the wheel of the seasons, the
undying ember, the pyre's ring of burning cribs, the gravestone on its tortoise, the standing stones, the frozen moon over its
well and the moon of ice that falls, the sword grave of twenty-four buried swords, the reborn sword in its white flame, the White
and the Black Impermanence, the Book of Life and Death, the Heaven-Demon Eclipse); and True Immortal's thirteen of the beginning
(the cyclone of the primal wind, the seed of creation under its dome of living vines, the three braided pillars of Samadhi
fire, the flower of chaos-fire, Houtu's hand, the falling continent, the glacier of seracs, the ring of the first sea's waves
round its whirlpool, the heaven-slaying sword, Pangu's crescent axe, the walls of Fengdu, the helix of ten thousand souls, the
sphere of chaos). Each was checked in game, its moves frame by frame (`tools/spell-art`: `Vessel`, `Nirvana`, `Immortal`; their
icons `VesselIcons`, `NirvanaIcons`, `ImmortalIcons`).

**The spells of Four Axis and below, made over** (2026-10-25, the user's ask: "overhaul any spells you think need it (i think all the ones
4 axis and below should get looked at because they were done before geckolib and some look not great)"). Those drawn by hand out of
flat pictures and particles are living shapes now, as the great spells' are, their workings as they were (`tools/spell-art`: `Axis`
for the elements' and both paths', `Underworld` for the ghost path's):
- **Fire**: Nine Suns Descend's falling suns (fire boiling in cells, five tails of flame behind each, a burst where it lands), the phoenix of
  Nirvana (rising before you, folding its wings round you as you fall, and you reborn out of it), Phoenix Descent's bird of flame flown at
  its mark, Blazing Meteor's burning rock.
- **Wood**: Myriad Vine Binding's vines coiling up round what they bind and crushing, Heaven-Piercing Bamboo, the Lotus of Rebirth opening,
  the Thorn Spear Forest thrust up out of cracked ground.
- **Earth**: the earth golem (a GeckoLib model of its own, rising out of the ground and pounding), the Mountain Seal's talisman hung over
  its peak in chains staked round it, the Falling Mountain's peak, Earth Dragon Surge's dragon swimming through the land at the surge's
  front, its coils arching up out of the ground behind its head, the ridge of spikes bursting up in its wake.
- **Water**: Absolute Zero's coffin of ice (shattered by a blow).
- **Metal**: the Heaven-Severing Sword's stroke, Sword Domain's wheel of swords, Sword Rain's ring of swords and each sword that falls.
- **Both paths**: the palm thrust out along the twined light, the mantle's two rings, the Devouring Seal's maw biting, the claw's four
  talons of black horn raked down from over you through the place and drawn back into the sky, their gouges left burning, the vortex and
  pillar of the two made one.
- **The ghost path**: the cold fire kindled in your hands, the Ghost Fire Bolt itself flying, the wraith of mist a Soul Dash leaves, the
  wailing face over you and its cries flying out, the Yellow Springs' tide with hands of the dead reaching out of it, the Marrow Ward's
  ribcage of bone worn on you, the core's burst, the King's Decree unrolled over you with the Ghost King's seal (王 at its heart) under
  you, the pools of the Nine Nethers a step goes in and out by, Yama's tablet cast down behind the one judged, the Chains of Hell lashing
  up out of a way to hell at your feet and winding round every foe (links of black iron, ghost fire running along them, drawn by hand:
  `TendrilRenderer`), the Wheel of Samsara turning at your back, the six pillars of cold fire; Soul Rend a beam of the Nine Nethers' dark
  (`SpellField`).
They are made for the day as well as the night: GeckoLib's glow lays a shape's glowing texels over what is behind it, it adds no light,
so a pale see-through haze over a bright core reads tan or grey against a day sky; their hazes are kept faint and their fire saturated.
Each was checked in game. Two of the spells' own faults found doing it are mended: Earth Dragon Surge's spikes hid its dragon (they burst up
behind its last coil now, `EarthSpells#SURGE_WAKE`), and Nine Nether Step looked at the ground far ahead went nowhere (the body under the
eye's place sank into the ground and backing off along a level look never left it; it rises out of the ground first now). No glyph
makes a swastika, nor two opposite arms of a cross turning one way (`Fire#swastika`).
**Tears in space** (Void Shattering's, the user's ask: "make sure ... the rifts and tears/cracks in space looks cool"). What comes
through space comes through a tear in it: a `SpellConstruct` with no model, drawn by the client's own hand (`RiftRenderer`, as a
Heavenly Being's Spatial Tear is, but greater; its size its length in blocks). Its rim is ragged and crackling, white-hot at its very
edge, glowing the colour of what comes through it (`SpellConstruct#tint`: the sun's gold for the crow and the sun, the earth's amber
for Buzhou, the Weak Water's pale blue, the northern abyss's deep blue, sword light's white, ghost fire's teal, gold for the Void
Shatter; the void's own violet for the void tree's roots), and haloed; within it is the void, black, a nebula of its colour turning
slowly round its heart, stars twinkling, motes of light drawn in; round it space is cracked like glass, cracks running out from its rim
and forking, shards of space breaking off its edge and tumbling away. It opens as it forms, a slit that widens with its cracks running
out after it, and closes as its spell lets it go. Four kinds: a tear standing in the air, turned to whoever looks at it, upright or
lying across the sky as a slash (`VOID_RIFT`, its yaw turning it on the watcher's screen: the crow's, Buzhou's and the
sword prison's ten upright; the sun's and the roots' slashes across the sky, the sun's shrinking away once its sun is out, the user's
ask, 2026-10-24, the two had overlapped), or, pitched 90, lying flat in the sky open below, its shards falling out of it (the Weak
Water's, the user's ask, 2026-10-24: its water falls out of the rift's underside, not past an upright slit); an abyss lying open in the ground, its shards
rising out of it (`ABYSS_RIFT`: the Kun's, the Nine Nethers', the one under the sword prison); a cut along a line, running out as fast
as the stroke that cut it (`RIFT_LINE`: the Severing Sword Light's); and space shattered round a point like a struck pane, pieces
fallen out of it showing the void (`SHATTERED_SPACE`: the Void Shatter's).

**The void tree's grasping roots** (Void Withering's, the user's ask, 2026-10-24: "having branches actually reach out and grab things
would be cool"): each root that seizes a foe is a `SpellConstruct` of its own (`VOID_TENDRIL`) that knows whom it holds (synced to its
watchers), drawn by hand (`TendrilRenderer`): from the rift's underside down to its catch, wherever that is hauled to, aimed afresh each
frame, a tapering root of the void tree's black wood, gnarled, ridged and thorned, lashing out (a third of a second), then winding twice
round its catch from its chest to its knees, writhing as it holds; two threads of green-white life wind up it in swells running up toward
the rift, drops of the life it drinks well up it from its catch, and its catch glows where it is held. Let go, it unwinds and draws back up
into the rift as it fades.

Cultivators cast these spells too (see Sect members: Cultivators cast spells), by the same code and with the same harm, but their
spells never wound the land. Any caster, player or cultivator, goes through one kit (`HeavenSpells#level`, `#cultivation`, `#tell`,
`#carvesLand`, `#fightsWith`); a player's own (messages, inventory, the PvP rule, creative play) is asked of a player only.

Each spell's description in the Spells tab gives its numbers; the constants at the top of its element's class set them. Harm
grows by realm: about 5 at Qi Refining, 8-16 at Foundation Building, 16-30 at Core Formation, 30-60 at Nascent Soul, 60-150 at
Heavenly Being, 150-300 at Four Axis, 450-600 at Dao Integration, 1,500-1,600 a blow at Void Shattering (100-400 a second for
what lingers), up to 3,500 a blow at Sacred Vessel, some 6,500-7,000 in all at Entering Nirvana and 60,000-75,000 in all at
True Immortal (its leap of x10); qi costs likewise (8-20 at Qi Refining up to 1,000-2,000 at Four Axis, 2,600-3,800 at Dao Integration, 9,000-12,500
at Void Shattering, 20,000-27,000 at Sacred Vessel, 44,000-58,000 at Entering Nirvana and 380,000-500,000 at True Immortal).

**Wounds in the land.** From Nascent Soul up the great spells leave the land the way a walking disaster would: meteors, suns,
phoenixes and falling mountains blow out craters (burnt to magma and blackstone, or broken to rubble), quakes and seals split it
in fissures and sink it, the Heavenly River scours a channel, the Boundless Sea leaves a lake in a crushed basin, Piercing Sword
Light bores a tunnel, Mountain Cleave cuts a slot through hills, and the Heaven-Severing Sword's crescent, some 110 blocks across,
slices through whatever its blade passes through for 120 blocks: a slit cut in the land 2.5 blocks tall at its middle (it was 10,
then 5; since 2026-10-24 the lower half of its blade's height, the ground under it still scoured: `MetalSpells#SEVER_CUT_SHARE`;
what it strikes, its whole blade) and thinner toward its tips,
at the caster's chest, through hills and mountains (their tops left standing), its far end cut in the crescent's curve
(`MetalSpells#severThickness`, `#severLag`; the client draws it to the same numbers). Its way is asked to load as it is cast and
kept loading just ahead of it, since 120 blocks (and its tips) run past the usual simulation distance; land that was never made before is made
meanwhile, and the slash waits at its edge until it is. Every block goes by the same rule as the rest of the spells' terrain harm
(`HeavenSpells#carvable`: nothing that holds items, nothing harder than obsidian, nothing a raised barrier shelters, nothing the
caster couldn't dig) and is gone for good, dropping nothing; a few pieces are flung off as debris. A big wound is cut over
several ticks, nearest its heart first, 2,500 blocks a tick for all of them together, shared out evenly among the wounds
underway so a small one isn't kept waiting behind a great one (`Devastation`), so no tick has to take it all.

**How great by the realm** (the user's ask, 2026-10-23): the great spells' wounds grow with their realm as a golden core's
blast does (`Devastation#scale`: the realm's blast radius against Four Axis's, `CoreDetonation#radius`): Nascent Soul's are
0.56 of Four Axis's, Heavenly Being's 0.78, and Four Axis's own the yardstick, the Heaven-Severing Sword's scale; Dao Integration's
1.28 (the Azure Dragon's fissures to 51, the bird's burning trench 5 round and 3 deep along its 154 blocks, One Flame's glassy pit
10 round, the qilin's stride craters, the seal's land sunk 20 round and split to 40, the whirlpool's lake basin 23 round, the tiger's
four gouges 102 long and 4 deep, the colossal sword's pit 19 deep and its cross of slots 60 out each way). **Not only their
wounds** (the user's ask, 2026-10-24: "make sure the scale of spells increases with realm not just the destruction capability"):
the spells of the realms after Four Axis are greater in every way by the same measure (`HeavenSpells#greatness`, `#span`), their
reach, their areas and their living shapes' size alike (`SpellConstruct#sizedFor`): Dao Integration's reach 82 where Four Axis's
would reach 64, its dragon 54 blocks long, its bird's wings 53 across, its seal 36 blocks square, its pillar of fire 118 tall;
Void Shattering's 1.84 times Four Axis's (its reach 118, Buzhou's pillar 59 blocks long and its fissures out to 70, the sword light
147 long, the crow's glassy crater 13 round); Sacred Vessel's 2.13 times, Entering Nirvana's 2.41 and True Immortal's 3.41 (its reach 218, its continent 67 blocks across, its sword's blade 94 long). Only the
spells meant to lay the land waste go by it (craters, fissures, basins, channels, bores, cleaves; the sizes above); the small
carvings of the lower realms (a cleave's slot, a cave) stay as they were. Water and wood were brought down beside metal and earth: Frozen Field reaches 64 blocks (was 100) and holds for 3 s,
Absolute Zero holds 5 s and shatters for 60, Boundless Sea and Water Prison hold for less, the World Tree drinks 10 a second for
12 s, Myriad Vine Binding and Heaven-Piercing Bamboo strike fewer and softer, Lotus of Rebirth and the Spring of Myriad Life heal
less.

**Commands (op):** `/heavenmethod learn <id> [layer]`, `/heavenmethod progress <amount>` (that much settling on the practised
method, no further than the realm), `/heavenmethod forget <id>`.

Code: `HeavenMethod` (the fifty-five, their ids and realms), `HeavenState` (what you know, saved), `HeavenMethods` (who, how far
understood, settling, the gate, the rate, learning), `HeavenPassives`, `MethodLayers`, `Spell` (the ninety element spells, by element,
realm and layer), `HeavenSpells` (the kit: who is spared, targets, what lingers, temporary blocks, the elements' qi harm), `SpellAura`
and `SpellAuras` (wards and auras, synced), `FireSpells`, `WoodSpells`, `EarthSpells`, `WaterSpells`, `MetalSpells`, the spell
entities `SpellProjectile`, `SpellField`, `SpellBlock`, `EarthGolem`, `DirtTide`, `SpellFx` (passing moments) and
`Devastation` (craters, fissures, channels, the slash's canyon; budgeted); client
`SpellRender`, `SpellModels`, `SpellProjectileRenderer`, `SpellFieldRenderer`, `SpellBlockRenderer`, `SpellAuraLayer`,
`SpellAuraWorld`, `ClientSpellFx`, `HeavenLangText` (the methods' and spells' English). Tests: `HeavenMethodsGameTests`,
`ElementSpellsGameTests`.

## The Inner Realm

After **10 seconds of meditation** the screen fades to black and your soul turns inward: you open your eyes on your own
island in a void, while your body stays sitting where you were (other players see it, in your skin). Meditation inside is
**x1.25** as fruitful and its years cost x1.25 fewer (the retune of 2026-10-27: it was x1.5, and saved no years), and it still
gets the mats, pedestals, height and biome bonuses from around your body; suppression and the Upper Realm's x10 also follow the
body. The ghost path's and the Demon Path's meditation are x1.25 inside too, on the qi where the body sits. The Cultivation
tab shows it so: while you're inside, the server tells it the boost and the elements' qi about your body (`ModPackets#INNER_PLACE`,
client `ClientInnerPlace`; till 2026-10-27 the tab reckoned them from the island and showed the mats and pedestals gone, though
they counted all along).

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
- The island grows with the realm: radius 3 at Qi Refining and one more each realm, 13 at True Immortal (`InnerRealm#islandRadius`;
  the README said 3 to 8, from before the realms past Heavenly Being, until 2026-10-24).

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
casts the readied spell: Qi Flight on and off, the Spatial Tear opened, and so on. It's shown in a slot right of the hotbar
(its icon, "Ability" over it, its key in its corner; the frame jade and a green diamond in its corner while it's on: The
top-left corner, below), and each switch by key says "Qi Sense: On" above the hotbar once the server has done it. It's saved with
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

## Meteor Flight

**Qi Flight's fast mode** (the user's ask, 2026-10-23, after a "Viltrumite" flight they saw in a video). Qi Flight itself, and
the free flight of the dead and of a Ghost King's form, now fly at **creative flight's own speed at every realm** (11 blocks a
second, 22 sprinting), so even a True Immortal can make fine adjustments; swiftness is this ability's. From **Core Formation**
on either path, **Meteor Flight** (Abilities tab, the wheel, X; **off by default**) switches it on and off:

- **Hold forward to fly where you look**, free of gravity, your body laid out flat along the way you go (head first diving,
  upright climbing, a fist out ahead; others see it too). Forward alone cruises (40% of your top speed, never slower than Qi
  Flight's sprint); **sprint** speeds you up over a second and a half to your realm's **top speed**: Core Formation 45 blocks a
  second, Nascent Soul 60, Heavenly Being 75, Four Axis 95, Dao Integration 115, Void Shattering 135, Sacred Vessel 155, Entering
  Nirvana 175, True Immortal 200, each stage 4% more (`MeteorFlight#topSpeed`). Let go of forward and you hover and steer as in
  normal flight, the momentum running off.
- **Sprint held, the ground doesn't land you** (the user's ask, 2026-10-23): swooping low no longer sets you walking the moment
  you touch it (vanilla ends flight on touching ground; `MeteorFlightClient#keepAloft`, from `mixin/MeteorFlightMixin`). Let go
  of sprint to land. Meeting the ground (or a ceiling) at an angle, you **slide along it at your speed** rather than losing it.
- **The bulldozer** (the user's ask, 2026-10-23: running into things at speed stopped you dead). Flying faster than Qi Flight's
  sprint with **sprint held**, at any realm, the way ahead is cleared before you reach it, so you keep your speed:
  - **What's swept:** your body's own box (a hair wider), swept along the way you go every half block, out to this tick's
    move and three ticks more (+1.5 blocks; at most 48): so the reach grows with your speed (`Bulldozer#sweep`, `#reach`). A
    swept box rather than a single ray, so nothing the feet or shoulders would hit is missed, and nothing between ticks skipped.
  - **Only obstacles:** blocks you'd collide with (grass and flowers you fly through), and **never the ground under your feet**:
    skimming low clears what stands up ahead and slides along the floor, no trench. The hole is your own size; the ablaze carve
    (below) is the big tube.
  - **On your screen at once, settled by the server** (client-side prediction and server reconciliation, as vanilla does for
    a block you mine): your client clears the blocks immediately and tells the server (`meteor_doze` C2S, with a prediction
    sequence); the server takes what may go (`MeteorFlight#bulldoze`: flying in Meteor Flight with forward and sprint held,
    going fast as the server itself sees you (a quarter of the bulldozer's speed at least, its own measure lagging a flier
    setting off), in a cone ahead of where you look near where it saw you, and the great spells' rule, `HeavenSpells#carvable`)
    and acknowledges; whatever it refused (a sect's grounds, spawn protection, a formation's shelter) reappears and stops you as
    any wall. Waiting on the server alone left the wall standing on your screen for your latency, which is what stopped you.
    (Before the bug sweep of 2026-10-23 the server took any block within 72 of a hovering flier on its client's word, 384 a
    packet: a modified client could clear the land about it.)
  - **Cheap:** at most 384 blocks a tick however many words come, gone for good and dropping nothing; a few crumble with sound
    and pieces (yours at once, others' from the server). Off with the gamerule `meteorFlightSmashes` (your client is told, so it
    doesn't predict). **The dead don't bulldoze or smash** (they touch nothing of the living world), nor predict it.
- **The land has to keep up.** Minecraft makes and sends land as you go: where it hasn't come yet within a second and a half's
  flight ahead, you're slowed to reach its edge no sooner (`MeteorFlightClient#landAhead`), so you never fly into nothing; over
  land already made you go at full speed.
- **The sound barrier.** Past 60% of your top speed (60 blocks a second at most, so the high realms break it too where the land
  holds them back) it breaks: a boom (heard by all near) and a ring of air left hanging where it broke, then a white vapour trail
  behind you. Once, until you've slowed well below it again (and at most every three seconds).
- **Ablaze (from Nascent Soul).** Hold 90% of your top speed or more for two seconds, or fly **flat out** (sprinting, as fast as the
  land ahead lets you: your client says so) no slower than 54 blocks a second, and the heat rises. (A True Immortal's 90% is 180
  blocks a second, faster than land never made before comes: held back by it, it once caught alight and went out for good, the
  user's report 2026-10-23.) Within half a second you
  are **engulfed in flames** (only a sight: nothing burns, you take no harm), as in the video: your body itself turns to glowing
  gold, your skin showing dimly through, with flame flowing back over it (`SpellAuraLayer#meteorHeat`; no little flames or glow
  thrown about you, the user's word). **In first person too** (the user's ask): your arm glows gold with the flame flowing over
  it (when your hand is empty, as Minecraft shows it then), and fire burns all the way round the screen's edge: one front of
  flames, each its own height and changing as it goes (not rows of little flames alike), flowing from the bottom's middle out,
  up both sides and over the top, deepest at the bottom's corners and lowest under the hotbar, yellow-white at the edge to red at
  the tips, under the whole HUD so the hotbar and bars stay readable; the middle is left clear to steer by. Cooling, the fire
  dies down to the edge, which glows red and fades with you (`MeteorFlightClient#renderHud`, drawn first by `mixin/GuiMixin`). While ablaze and pressing on, **you smash through what's in your way**: a tube three blocks across about
  your body, torn open a little ahead of you as you go (`MeteorFlight#carve`), gone for good and dropping nothing, a few pieces
  flung aside. It follows the great spells' rules (`HeavenSpells#carvable`): nothing harder than obsidian, nothing with things
  kept in it (chests, furnaces), no barrier, nothing a formation shelters, no ground you couldn't dig (spawn protection, Adventure
  mode, a sect's grounds); what it can't take stops you as any wall. The gamerule **`meteorFlightSmashes`** (on by default)
  turns the smashing off for the whole server. **Slowing, you cool off**: the flames go after about four seconds, then your
  body glows red, fading over about four more.
- **Seen by others** as by you: laid out, golden, cooling red. While you fly fast (and two seconds after) the server tells
  those watching where you are outright each update (`mixin/ServerEntityMixin`): vanilla tells one who starts watching a moving
  player where it is, but counts later moves from where it last told the others, so at these speeds a newcomer saw you blocks
  ahead of where you were for up to twenty seconds.
- **Qi.** Flying fast costs **three times Qi Flight's qi**; from Heavenly Being on never more than you gather, so it never runs
  you dry (Qi Flight never does there either). Hovering in it costs what Qi Flight does. The dead's free flight costs nothing.
- **Not** in space (its own flight, swifter still), the Spatial Gap or on the Soul River, nor on a sword, nor in creative. So the
  swiftness isn't lost there (the user's ask, 2026-10-23): **in the Spatial Gap, Meteor Flight switched on gives Qi Flight (and
  the free flight) the speed it had there before** (`QiFlight#speedIn`: Void Shattering 2.4 to 3 times creative flight's, Sacred
  Vessel 3.2 to 3.8, Entering Nirvana 4 to 4.9, True Immortal 6 to 7.2; creative speed below Void Shattering, as it always was);
  switched off, creative speed. Space keeps its own speeds either way, unchanged (The Astral Realm). Nobody flies on the Soul
  River, whose current carries the living.
- **No flying punches** (the user's call).

Code: `MeteorFlight.java` (speeds, cost, the barrier, the heat, the smashing, the bulldozer's server side, syncing: packets
`meteor_streak` and `meteor_doze` C2S, `meteor_state` and `meteor_boom` S2C), `Bulldozer.java` (the sweep, the reach, what looks
carvable), `client/mixin/ClientLevelAccessor` (vanilla's block prediction, for the bulldozer), `QiFlight.java` (normal speed for all, the cost, room on the server for a second of top
speed a packet: `#moveAllowanceSq`), `ModGameRules#METEOR_FLIGHT_SMASHES`, `client/MeteorFlightClient.java` (steering, the land
ahead, the pose, the heat at the screen's edges, trails and rings), `client/SpellAuraLayer` (the heat on the body, and on one's
arm in first person), `client/mixin/MeteorFlightMixin` (the flight, in place of `Player#travel`),
`client/mixin/LivingEntityRendererMixin` and `HumanoidModelMixin` (laid out); tests in `MeteorFlightGameTests`.

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
  near its end. **From Void Shattering it has no end** (the user's ask, 2026-10-23; `PlayerCultivation#soulTetherEndless`):
  the soul goes as far as it will, at what it costs beside the body, and nothing pulls it back.
- **Qi**: the same at every realm (the user's ask, 2026-10-23: it was the realm's own gathering, the same share of the pool
  at every realm, which ran a True Immortal's soul as dry as a Nascent Soul's): half a Nascent Soul Early's gathering a second
  at the body (`SoulProjection.SOUL_QI_BASE`), rising to twice it at the tether's end, and that much again while possessing.
  So a Nascent Soul Early pool lasts about a minute at the tether's end, a Heavenly Being's hardly runs down, and from Four
  Axis it never does. **Run dry and the soul snaps home at once** (no 5 s, no penalty). Taking hold of a body costs 10% of
  your pool on top.
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
swing at the thread itself, within 5 blocks, up to 0.9 of a block off it (the server allows a little more, for the soul
having moved on). The swing is tested against the thread as it is drawn, sagging under its own length (up to 3 blocks at
its middle), and exactly along every piece of it (`SoulProjection#aimAtTether`; before 2026-10-23 it was tested at 24 points
on the straight line between the ends, blocks apart on a long tether, so it was hard to hit). It holds 20 plus the soul's
own health, mends if left alone for 10 seconds, and when it is cut the soul is lost.

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
  or strike, and it costs only what it would beside your own body (the same at every realm, as above). Let go of the body
  there and your soul goes home at once.
- **Its sect**: in a sect member's body you are that member (the user's ask, 2026-10-23): the Sect tab shows its sect and
  its title, and **Leave Sect** (or asking one of its elders to leave) takes the body out of it: an NPC cultivator goes its
  own way once its body is its own again (its seat, a Sect Master's too, passes to the strongest left), a player is a
  disciple no more. In a **Sect Master's or an elder's** body you can **banish** members below its title from the Sect tab:
  a cultivator cast out sets off as a rogue (a new one is taken in in time), a player is expelled as for violence (barred
  30 days).
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

## Techniques and their manuals

**Techniques** (the user's ask, 2026-10-23; `Technique`) are ways of the cultivator learned from a **Technique Manual**, not
awakened by a realm as the realm's abilities are. So far: **Clone** (Nascent Soul) and **Universe in a Sleeve** (Heavenly
Being), both below.

- **Finding them.** The sects keep them: each chest in a Sect Master's main hall holds one now and then (Clone's and Universe
  in a Sleeve's each one roll in about thirteen; `loot_tables/chests/sect_master_hall.json`, an elder's riches besides), and a
  library pagoda's chest more rarely (each one in twenty-one; `sect_library.json`). Later structures will keep them too.
- **Reading one** (use it): you learn what it holds if you stand at its realm, on either path as you wield it, and the
  manual isn't used up (pass it on). Below its realm you're told which realm it wants. A technique once learned is yours
  for good (saved with your cultivation, kept through death and rebirth), but **used only at its realm**: fall below it and
  it waits until you rise again.
- **Spell manuals** hold one spell (`TechniqueManualItem`, "Spell Manual: ..."): learned from one, it is yours apart from any
  method, and cast only at its realm (`Spell#byManual`): an element's spell by the heaven path's rank and roots that take its
  element (never by the dead), a ghost method's by the ghost path's. Spells of both paths have no manual.
- **Writing one out** (for whoever lost theirs, or never had it): craft a **Blank Manual** (a book and jade), hold it in one
  hand and the **Inscription Brush** in the other, and use the brush. A list opens of what you may write (your techniques at
  their realm, and every spell you can cast but those of both paths); pick one, and the blank (one of a stack) becomes its
  manual, "Written out by" you, the brush a use more worn. A reader still needs its realm.

Code: `Technique`, `TechniqueManualItem`, `BlankManualItem`, `InscriptionBrushItem#use` (the brush opens it), packets
`inscribe_options` / `inscribe`, client `InscribeScreen`; `PlayerCultivation` (NBT "Techniques", "ManualSpells"). Tests in
`TechniquesCloneGameTests`.

### Clone

From **Nascent Soul**, with Clone's manual read, the living can split a second body off their own (`Clones`; the ability
**Clone**, on the wheel; its switch shows On while a clone is out). It is played as yourself, not possessed: **no tether, no
upkeep**, anywhere in any realm.

- **Split it off** (use the ability): it takes **half your qi** (the clone's to begin with) and stands a step before you,
  in your likeness with your name over it, carrying nothing (only the body is split off).
- **Go into it** (use it again): your **main body sits down in meditation** where you are (its health, armour and gear
  seen on it; its land kept loaded), and you are in the clone wherever it stands. The clone is you **at your own realm and
  stage** (the user's call, 2026-10-23: half the realm would leave a True Immortal a Four Axis clone), with your methods,
  techniques, spells, physique, stat points and both wheels, but **half your strength**: half your health, the strength of
  your blows, armour, toughness and footing, and half your qi pool (`Clones.STAT_SHARE`). Your speed and flight are your own.
  **It is you**: it plays your own record, so what it learns, does and earns is yours (methods from slips and steles,
  techniques, pills and fruit, alignment, wishes granted, stat points; the bug sweep of 2026-10-23: it played a copy, and all of
  that was lost on the way back). Its pool is its own while you play it; your main body's is kept apart (its tag's "MainQi").
  It gathers nothing of itself: what it spends (flight, spells, pressing down) is **made up from your main pool**, which
  goes on gathering where your body sits. Overtaken by the Netherworld's yin, it is the clone's body that gives way (it
  dissolves as one slain does, the yin with it), not yours. It can't meditate, project its soul, break through, detonate a core or split off
  clones of its own. The HUD says "In your clone" with your body's health under it.
- **Go back** (use it again in the clone): you wake in your main body where it sat, wherever the clone has gone; **the clone
  is left standing** where it was, holding what it carried. Each body's things are its own, kept for it while you play the
  other.
- **Draw it back in**: sneak and use the ability within **8 blocks** of the other body (from either): its qi, its things and
  its experience are yours again, and you are in your main body.
- **Harm.** The body you aren't playing can be struck (by players only where they could strike you), and you're told. A
  clone slain dissolves, what it carried falling where it fell; slain while you play it, you wake in your main body. A clone
  lost so (or to the Netherworld's yin), **no other can be split off for 5 minutes** (`Clones.REFORM_TICKS`; saved, so a
  relog doesn't end it; the user's pick after the bug sweep of 2026-10-23). One drawn back in is no clone lost. **Your
  main body slain where it sits, you die** (your own death, by that blow), and the clone dissolves with you. Whenever you die,
  or your life ends whole, a clone out dissolves.
- **Both bodies have a consciousness** (the user's ask, 2026-10-23). While your Consciousness Domain is switched on, the
  body you aren't playing (your main body sitting, or the clone standing) has its own domain where it is, as well as the
  one you play: others near see its sphere and sense the body in theirs with a realm over its head, and a domain that touches
  it alerts its owner as yours would. You're told which of your bodies felt whom ("Your clone's consciousness touches
  another's", or "Your main body's"). The **clone's domain and the realm and stage it is sensed at are its effective
  cultivation**, what half your strength comes to (`TwoPaths#cloneRank`; about a realm lower: a Heavenly Being Mid's clone
  is sensed at Nascent Soul Mid, with that domain), not your true realm, whether you play it or it stands. Your main body
  keeps your true realm and domain. Switching the domain off shuts both. To you, your other body's domain, its outline and
  its realm label are **blue like your own domain**, not the red of a stranger's (it is an extension of yourself; the user's
  ask, 2026-10-23); everyone else sees both your bodies in red as any cultivator's (`TouchingDomain#own`).
- Logging out in the clone, or the server stopping, puts you back in your main body first (the clone stands on); a crash is
  put right as you come back. A soul can't take a player who is in their clone.

**Leaving the game** (every system's logout work: the clone, a held body, a soul out, a spar, a core breaking, the Inner
Realm; `CultivationEvents#leaving`) is done on the server thread before the player is saved, once a login: Fabric tells of a
logout on the network thread as the connection closes, and the save raced it (found 2026-10-23: a clone's player saved in
the clone's place). A mixin at the head of vanilla's `onDisconnect` makes sure of it (`ServerGamePacketListenerImplMixin`).

Code: `Clones` (the rules), `CloneBodyEntity` (the body not played: the clone standing or the main body sitting), the
clone's record answers for its player while they play it (`CultivationManager#get`, `Clones#record`), its strength in
`CultivationStats#apply` and `PlayerCultivation#maxQi`, its qi in `QiManager` (`Clones#feed`); client `CloneBodyRenderer`,
`ClientClone` (packet `clone_state`), `SoulHud`. Tests in `TechniquesCloneGameTests`.

### Universe in a Sleeve

From **Heavenly Being** (the first realm that tears its own way into the Spatial Gap), with its manual read, the living can carry
other players in their sleeve up through the ways between the realms, **whatever their realm** (`Sleeves`; the ability
**Universe in a Sleeve**, an act, on the wheel; its switch shows On while the sleeve holds anyone). The user's ask of 2026-10-26,
and their picks of 2026-10-27.

- **Taking someone in:** look at another player within **6 blocks** and use the ability. They're offered a place in the chat,
  with **[Accept]** and **[Refuse]** (`/sleeve accept|refuse <name>`), for 30 seconds. Accepted, they vanish into your sleeve:
  a spectator held to your eyes (they see what you see and nothing else), untouchable, still ageing. At most **4** at once. Not
  on the way between realms, and not one away from their body, held or holding one, in a clone, in a tribulation or in the Inner
  Realm (a tribulation of theirs is abandoned as they go in).
- **Carried:** they go wherever you go, through the **Spatial Gap**, the **Void Breach** and the **Immortal Gate** (the ways'
  floors and the Immortal Realm's are yours to meet, not theirs), and **the moment you come out into a world they step out
  beside you**, set down gently. Going into a small world they stay in your sleeve until you come out of it.
- **The cost:** every spatial pressure on you is **doubled for each person you carry** (one x2, two x4, three x8, four x16; the
  user's rule). A True Immortal, untouched at the Immortal Gate on its own, takes **a quarter of its body** over the 30-second
  climb while it carries anyone, doubled the same way (`ImmortalGate#carriedShare`; the user's pick).
- **Letting them out:** sneak and use the ability to let everyone out beside you (in the gap, out into it), anywhere but partway
  through the Breach's or the Gate's climb. Anyone carried may step out (`/sleeve leave`, or the [Step out] button) while you're
  out in a world.
- **You slain:** they're **cast out where you fell** (the user's pick): in the gap, into its pressure; partway through the
  Breach's climb, back at its foot; partway through the Gate's, back on the Shore of Nirvana; anywhere else, where you lay.
- **Leaving the game:** you log out and they're set down beside you (or, on a way, where you took them in). They log out and
  they're still in the sleeve when they come back; let out meanwhile, they come out where they were let out. A crash is put
  right from the record (kept with the world).
- **A small world does the same** (the user's ask of 2026-10-27): anyone inside your small world comes through the ways with
  you, at the same cost (see the small worlds).
- Art: `java tools/beast-art/BeastArt.java <assets> techniques` (the ability's icon: a silk sleeve with a sky of stars in its
  mouth).

Code: `Sleeves` (the rules and the record), `SleeveCommand`; `Technique#SLEEVE`, `Ability#SLEEVE`; the pressure in
`SpatialTrialHandler`, `VoidBreach` and `ImmortalGate#carriedShare`; the held view in `SoulServerPlayerMixin#setCamera` and
`ServerGamePacketListenerImplMixin`. Tests in `SleeveGameTests`.

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
won't. The ground at its heart is blown away (what a spell may carve, nothing dropped): a crater a third of the blast's reach
round up to **Heavenly Being Grand Perfection** (48 blocks round, 96 across, 38 deep); beyond it it goes on growing (the user's
ask, 2026-10-23: a True Immortal's had stopped at a Heavenly Being's), as the blast's reach to the power 2/3, shallower for its
size the greater it is, as great craters are (`CoreDetonation#craterRadius`, `#craterDepth`):

| Grand Perfection of | Heavenly Being | Four Axis | Dao Integration | Void Shattering | Sacred Vessel | Entering Nirvana | True Immortal |
|---|---|---|---|---|---|---|---|
| Crater across, deep (blocks) | 96, 38 | 114, 41 | 134, 45 | 170, 50 | 188, 53 | 204, 55 | **256, 62** |

It is carved outward from the heart like the great spells' craters, a share a tick so it never stalls the world (a True
Immortal's over some forty seconds, its land kept loaded till it's done even once you've woken elsewhere); the bigger the
blast, the further off it is heard.
**You die** (nothing saves you, a totem
neither), and the core is lost: you return at **Foundation Building Grand Perfection**, your heaven methods understood no deeper
than that. Leaving the game while it breaks doesn't escape it. A creative player's core bursts without the death. The core is
the body's it broke in: should you stand in another when it would burst (thrown out of a body you held, back in your own), its
light goes out and nothing bursts (the bug sweep of 2026-10-23: the core of whatever body you then stood in burst instead). A demon
beast takes the blast by its own realm, as a cultivator does. Code: `CoreDetonation`.

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

## The Great Void Realm

Above the **Jade Firmament** (the players' name since 2026-10-25 for the Upper Realm, after its jade sky; the code still says
`upper_realm`) lies the **Great Void Realm** (太虚界, `great_void`). It is floating continents, far larger than the Jade
Firmament's islands, under a black-violet void full of stars and drifting shards of broken space. This is phase 1 of the realm
plan of 2026-10-25. The Immortal Realm (below) is phase 2.

**Getting there: the Void Breach.**
- **Torn open.** From **Void Shattering** on (either path), a Spatial Tear torn high in the Jade Firmament's sky (Y 448 and up)
  opens a **Void Breach** instead of a way onto the gap. It costs half the qi pool. The opener holds it open while they stay
  within 24 blocks. Let go, it lingers 10 seconds for those who follow, and stays open a minute at most.
- **Who passes.** Void Shattering and above. **Dao Integration** only through a breach held open by someone stronger. Below that,
  no one: the void's pressure turns them back.
- **The climb.** Shattered space (`void_breach`), where no flight holds. The breach's current carries you up for 30 seconds while
  you steer. The void presses all the while (armour is no help). Shards of broken space fall where light first marks them (60
  harm). **Vestiges of the Void** come out of the dark for the climbers, each as mighty as the one it hunts but **never past
  Sacred Vessel's Grand Perfection** (`VoidVestige#MAX_MIGHT`, the user's ask of 2026-10-27: one past True Immortal met True
  Immortal Vestiges, and even an Entering Nirvana could hardly climb; old ones saved mightier come back capped). One matches the
  might of the climber it hunts as they stand now, its wounds kept (`VoidVestige#matchMight`): one left from a mightier climb, or
  come upon a weaker climber, is theirs (a climber fallen back from True Immortal was hunted by the Vestiges of their peak).
- **Out and back.** At the top you come out on the land nearest where the breach was torn. To go back down, fall off a
  continent's edge, or tear a Spatial Tear in the Great Void. Either way you land harmlessly in the Jade Firmament's sky over its
  nearest island.

**Qi ×100.** Meditation and the qi pool's refill are a hundred times the lower realms' there. The realms the Great Void alone
sustains need that many times their base cultivation: **Void Shattering** on (`PlayerCultivation#GREAT_VOID_QI_MULTIPLIER`,
`qiNeed`; the user's pick, "like the Upper Realm rule").
- Those realms take as long in the Great Void as they used to, and ten times longer in the Jade Firmament.
- **Dao Integration** stays at ×10, so one carried up races ahead.
- The years spent stay as they were (`Lifespan#placeSaving`).
- From Void Shattering on, nothing below suppresses you (the user's rule, kept).

**The lands.**
- **The void steppes.**
- **Five Dao fields**, each where one element's law runs strong. Its element is easier to gather. Spells of its element strike
  **×1.25**, and spells of the element it overcomes **×0.75** (`HeavenSpells#daoField`).
- **Ancient Battlefields** of the old war between Heaven and the demons. Their water runs red, and their **ghostly qi is x100**, ten
  times the Netherworld's (see Ghostly qi, under The Netherworld): the ghost path's place of dense qi outside the realm of the dead.
  The land is torn by the war (one remnant in every two chunks, `BattlefieldRuinFeature`; remade 2026-10-27 without the old giant swords):
  - **craters**, 5 to 25 blocks across, their rims thrown up and their floors burnt to blackstone and basalt; some glassed to
    obsidian at the heart, some with magma still smouldering, some holding a pool of dark water; the largest cracked out round
    their rims; now and then two or three small ones together where a volley fell;
  - **fissures** torn through the ground, their lips scorched, magma glowing in some;
  - **toppled spires**: a great pillar of rock blasted off at its base, the rest lying broken across the land;
  - **fields of rubble**: boulders, gravel and blackened bricks;
  - heaps of **bones**, a **broken wall** with its banner or a gate's **arch** fallen between its pillars, and a half-sunk
    **war chest** with its old weapons and slips.
- **The Shore of Nirvana** at the void sea's edge.

**Void currents** (`VoidCurrents`). Drifting rifts of streaming starlight that come out of the void near whoever is about, at
most two near anyone. Each is drawn lying along its flow, drifts slowly the way it flows, and closes after 90 seconds. Touch one
and it sweeps you off along its flow. You are blinded by its light while it bears you, for 1.5 to 5 seconds while the land ahead
loads. You come out 500 to 1,400 blocks on, onto the nearest continent there and never onto a Holy Land's barrier, and land
harmlessly. No current comes within 16 blocks of a Holy Land's or a Demon Palace's barrier: one is born only where its whole drift
keeps clear of them, and one that comes up against one all the same shuts there (the user, 2026-10-27).

**Holy Lands** (`HolyLands`, `HolyLandPlan`, `HolyLandPieces`). The sect tier above the sects, and the only sects in the Great
Void.
- **Rare:** about one in 2,000 × 2,000 blocks.
- **Grounds:** 201 × 261, laid along one great axis from the Mountain Gate to the Patriarch's Palace. The palace has triple eaves
  on three terraces. A lotus moat and its bridge, twin nine-storey pagodas, bell and drum towers, gilded guardian lions and walled
  elders' courts complete it, all in white jade and gold.
- **Barrier:** up to 136 (`Formations#HOLY_MAX_RADIUS`).
- **People:** a **Sacred Vessel Patriarch**, two Void Shattering Grand Elders, four Dao Integration Elders, and disciples of
  Heavenly Being to Four Axis.
- **Joining:** as any sect is.

**Demon Palaces** (`DemonPalaces`, `DemonPalacePlan`, `DemonPalacePieces`, `PalaceSculpt`; the user's yes, 2026-10-25). The demon
clans' counterpart of the Holy Lands: neither a Holy Land re-skinned nor a bigger den.
- **One race each, by its land:** the metal Dao field the White Tiger's, the fire field the Nine-Tailed Fox's, the earth field the
  lizard king's, the wood and water fields the Azure Dragon's. On the steppes, any race (a dragon's most often).
- **Rarity:** they share the Holy Lands' sites (structure set `holy_lands`, now tried every 20 × 20 chunks: one or the other at
  each, never on top of each other). Each comes to about one in 1,700 × 1,700 blocks (surveyed on the test seed: 20 palace sites
  and 17 Holy Land sites in 256).
- **Grounds:** 181 × 261, in dark stone in the race's colours (`SectPalette#palace`):
  - tiger: black stone and white, white ridges, iron bars, pale soul fire;
  - fox: dark nether brick and white plaster, vermilion pillars, red roofs, foxfire;
  - lizard: deepslate, green copper roofs with copper eaves, jade;
  - dragon: deepslate, azure prismarine roofs with gilt eaves, gold.
- **Laid along one way, front to back:**
  - **The great torii**, then two lesser ones. (A gate of the beast's head, its jaws open over the way, was tried and the user
    didn't like it.)
  - **The Way of Beasts:** two pairs of statues of the race's beast on plinths, facing across the road.
  - **The blood ring** round the Formation plaza at the palace's heart: the race's blood lit red from under its floor, four bridges
    over it, and eight great fangs of bone leaning in over it from its outer rim. Its land is the **Blood Ring** biome
    (`ModBiomes#BLOOD_RING`), set on the ring by its piece, whose water runs red. Water takes its colour from its land, and on the
    steppes the blood showed violet. The biome has nothing of its own and isn't in the Great Void's biome source, so worldgen lays
    nothing there. As an Ancient Battlefield, the ring took the battlefields' ruins.
  - **The Sovereign's hall** on a dais of three steps: a carpet up to the **Demon Sovereign's** throne under a screen of gold. Its
    front pillars stand every four blocks from the doors out to its corners, alike on either side (the user: "symmetrical
    pillars").
  - **The colossus** behind the hall, some fifty high, its head over the hall's roof: a crouching White Tiger, a nine-tailed fox
    sitting up with its tails fanned like a halo, a mountain-bearing lizard king lying long, or an azure dragon coiled (no legs:
    the user's notes).
  - Down the sides: six elders' houses, four disciples' halls, the vault and a sparring ground.
- **The great beasts** are hollow shells two blocks thick, their eyes and their race's fire alight. Nothing of them floats free
  (tested), and nothing in them is worth quarrying (no gold, no lapis).
- **People:** a Holy Land's twenty, all of the race: a **Sacred Vessel Demon Sovereign**, two of Void Shattering, four of Dao
  Integration, and disciples of Heavenly Being to Four Axis. Named `<word> <Race> Demon Palace`.
- **Joining:** as any clan is, by demons of its race.
- **Hoards:** its chests now and then keep a phial of its race's blood, Heavenly Being to Entering Nirvana, as often as its
  library's realms (`DemonBlood#GREAT_VOID_BLOOD`).

**The Shore of Nirvana's camp** (`NirvanaCamp`). Some twenty of **Entering Nirvana** wait on the shore for the Immortal Gate
(phase 2). They are hermits and old monsters, tradeable and rarely hostile: only the short-tempered of the devilish path go for a
player of the other path who comes within 6 blocks. One in five is of a demon race, and on the camp's ground nobody goes for a
demon at all (the user's rule). New ones come in from the south while there are fewer than twenty.

**Sacred Vessels** (`SacredVesselItem`, `SacredRelicFeature`, `SacredRelics`). One for each element:
- the Jianmu Bough Vase, the Heaven Furnace Censer, the Nine Provinces Ding, the Immortal-Forging Bell and the Sealed Ocean Gourd;
- each lies on a relic altar in the Great Void, guarded: a **Void Shattering Relic Keeper** rises when anyone comes within 24
  blocks;
- held in either hand, a vessel makes its element's spells strike ×1.25, on top of what a Dao field gives.

**Blood pools and the elder beasts** (`BloodPoolFeature`, `BloodPools`, `BeastKind` tier ELDER). On the Ancient Battlefields,
pools of blood each hold a phial of a demon race's blood of **Void Shattering to Entering Nirvana** on an altar.
- When anyone comes within 28 blocks, the race's **elder** rises out of the pool, of the blood's realm. It keeps to its pool and
  goes for whoever comes near, unless they stand a realm or more above it.
- The four elders (GeckoLib, `tools/beast-art`), never wild:
  - the **Elder White Tiger** (metal, pounces);
  - the **Nine-Tailed Fox** (fire, breathes fire);
  - the **Mountain-Bearing Lizard King** (earth, pounds);
  - the **Azure Dragon** (wood, flies and dives).
- Slain, an elder leaves its core, and half the time its race's blood of its realm.

Code: `VoidBreach`, `GreatVoidClient`, `VoidCurrents`, `SpatialArrivals`, `HolyLands`, `HolyLandPlan`, `HolyLandPieces`,
`DemonPalaces`, `DemonPalacePlan`, `DemonPalacePieces`, `PalaceSculpt`, `NirvanaCamp`, `SacredRelics`, `SacredRelicFeature`,
`BattlefieldRuinFeature`, `BloodPoolFeature`, `BloodPools`; tests `DemonPalaceGameTests` (the Holy Lands, currents, pools and
elders were checked in game).

## The Immortal Realm

Above the Great Void lies the **Immortal Realm** (`immortal_realm`): an endless sea of clouds with jade peaks rising out of it under a
golden sky, at a standing noon. Phase 2 of the realm plan (2026-10-25 and 26). **Nobody below Entering Nirvana gets in or is born
there** (the user's rule); one who falls below it inside stays.

**Getting there: the Immortal Gate** (`ImmortalGate`).
- It hangs in the Great Void's sky over the void sea, north of the Shore of Nirvana's camp. It **opens every five in-game days for
  three minutes**, told to everyone a minute before.
- Crossing it is a 30-second climb through its passage (`immortal_passage`) under its pressure: **True Immortals** cross unharmed,
  **Entering Nirvana Grand Perfection** survives with care (armour, pills), earlier Entering Nirvana is crushed, and anyone below is
  turned back.
- When it opens, a third of the camp (eight at most) rush it and are gone; new ones come in over the following days. They rise to the
  opening's height first and fly level into it, and the land between the camp and the Gate is kept running while any fly (they used to
  fly straight at it, into the underside of its cloud, and hang there out of the land the server ran: the user, 2026-10-27). Those it
  shuts on fly back the same way. Its four guards never go for a demon-race Entering Nirvana.
- **Its light** (`GatePortalRenderer`, 2026-10-27), the Immortal Realm's gold, fills both gates' openings while it stands open: clouds of
  light drifting in layers that shift against each other as you move, glints, a breathing heart and slow ripples out from it, the light
  spilling onto the threshold and the pillars' faces, golden motes. It blooms out from its middle as it opens and folds back into it as
  it shuts.
- You come out at the **Southern Heavenly Gate** on the main island. The **only way down** is back through it while the Gate stands
  open: no Spatial Tear opens in the realm, and falling below the clouds bears you back up onto the nearest land.

**The land** (noise `immortal_peaks`, biomes jade peaks, immortal gardens, celestial heights and the jade capital).
- The **sea of clouds** at Y 150 is drawn by the client (no blocks). The massifs rise out of it with terraces and ranges, and lone
  jade pinnacles stand alone out of it.
- **Qi ×1000**, and True Immortal needs a thousand times its base cultivation (the Great Void's pattern). Under Qi Sense its air
  holds ten times the Great Void's motes (some 45,000 about you).
- **No steles** (the user's rule).

**Immortal essence** (`ImmortalEssence`, an ability). A True Immortal's gold bar: 100, 150, 220 or 320 by stage. It fills only inside
the realm (in 10 minutes, four times faster meditating) and leaks away elsewhere (in 20). Lit, blows and spells strike **×1.5**, and a
full pool lasts 90 seconds. It does not make a spell bigger or longer, only harder-hitting.

**Immortal Jade.** Worth 9 Immortal Spirit Stones (59,049 Low), nine of those to one and back. True Immortals carry two to four.

**The main island.**
- The Southern Heavenly Gate at its south end (0, 0).
- The **Jade Capital** north of it, the biggest city, laid on its land as the land lies (`ImmortalCapital`, Beardifier).
- The **Celestial Court** on its pinnacle at the capital's north end (0, −440, Y 330), up twin stairs on gold-banded arcades.
- **Other immortal cities** elsewhere (`ImmortalCityStructure`), one site per 640-block cell where the land holds one, their ground
  rolled gently.

**Markets** (`CityMarkets`, `CultivatorTrades#immortalGoods`). Six merchants at the capital and four in every city, at their stalls.
- Immortal-grade pills, herbs five to ten thousand years old, Cultivation Fruit.
- A Sacred Vessel of their element, to those they are Warm to.
- Their element's **True Immortal slip** (60 jade) to the courteous. One merchant in five sells the Netherworld Immortal Canon instead.
- **A demon race's True Immortal blood** (60 jade, a race a day) to the courteous, and each realm's of the ladder past it (70 jade for
  an Earth Immortal's, ten more a realm, 120 for a Heavenly Lord's, paid in two stacks of jade; the user's ask, 2026-10-26). Past True
  Immortal the blood alone climbs no realm: the Laws are wanted too.
- The realm's immortals also teach their True Immortal methods.

**Immortal sects** (`ImmortalSects`, `ImmortalSectPlan`, `ImmortalSectPieces`). Floating **archipelagos of fourteen islands** over the
open sea of clouds, joined by thirteen white-jade bridges (the user's rules: small floating islands joined by paths, over the sea and
not above a giant island, every one of the old ground court's buildings on them). Reached only by flight.
- **The spine:** the gate island (the Mountain Gate, the Spirit Way, four lions), the core island (a plaza, the bell and drum
  towers), the arena, and the Sect Lord's main hall, highest of all.
- **The sides:** two disciples' courts, the altar and the treasury, the library and the alchemy hall, two herb gardens, two elders'
  islands of three houses each.
- **Twenty members:** an **Earth Immortal** Sect Lord (Early to Late; a True Immortal of Grand Perfection before phase 3), two True
  Immortals Late or Grand Perfection, four Early or Mid, and thirteen of Entering Nirvana. Joinable like any sect.
- Their libraries keep True Immortal and Entering Nirvana slips. Their elders and Sect Lord sell a race's True Immortal blood to those
  they are Warm to.
- Those who can fly catch themselves when they step off an island.

**The Celestial Court** (`CelestialCourt`; the user: "Yes, build the Celestial Court as you described").
- **Its people:**
  - the **Heavenly King** on the palace's throne, a **Profound Sovereign** of Grand Perfection;
  - five **Heavenly Venerables**, one per element in the Court's five halls, **Taiyi Golden Immortals** of Grand Perfection. The **Metal Venerable is a
    tiger demon and the Wood Venerable a dragon demon** (the user's ask: at least two demons among them);
  - four **Immortal Generals** at the Court's gate;
  - the **Quartermaster** on the palace terrace.
- **Their strength** is their realms' own (phase 3: the ladder past True Immortal, **each twice the last**): Earth Immortal ×2, Heaven
  Immortal ×4, Golden Immortal ×8, Taiyi Golden Immortal ×16, Profound Sovereign ×32, Heavenly Lord ×64 (of a True Immortal's). Before
  phase 3 they were True Immortals made ×32 and ×16 as strong.
- **Joining:** ask a Venerable (Entering Nirvana and up). You start as an **Officer**.
- **Merit** (Court Merit Tokens):
  - one a minute standing watch on the Southern Heavenly Gate's plaza while it is open;
  - two for a devilish Entering Nirvana slain in the realm, five for a devilish True Immortal;
  - twenty to each officer who fought a primordial beast that was slain.
- **Ranks:** Officer, **Captain** at 30, **General** at 100, shown in the Sect tab.
- **The Quartermaster** trades: a token for 3 Immortal Jade, any element's True Immortal slip for 20, more for Captains and Generals.
  Lines not yet open show the rank they want.
- **A Venerable teaches its element's True Immortal method to Captains and up**, and tells the way to the primordial beasts.

**The primordial beasts** (`PrimordialBeast`, `PrimordialBeasts`; GeckoLib: the Peng and the Phoenix from `tools/beast-art`'s
`Primordials`, the True Dragon from `tools/spell-art`'s `TrueDragon`, the Kun the spell's own). World bosses, **one of each at a time**.
- **Strength:** True Immortals of Grand Perfection, a little stronger (×1.25: the user's word).
- **Boss bar:** shown within 112 blocks.
- **Lairs:** each keeps to its lair, 1,000 to 2,000 blocks from the Gate, the three a third of the way round from each other, all
  clear of the cities and the sects. It comes when anyone is within 160 blocks. Slain, the next comes five days later.
- **Left alone** two minutes, it is whole again.
- **Speed:** they fly 1.5 times as fast as they first did, their rams and dives with them, and turn up to 8 degrees a tick
  (`PrimordialBeast#SWIFT`, `#TURN`; the user, 2026-10-27: "all the primordial beasts are a little slow"): about 0.6 blocks a tick
  round their lairs and 1 round a foe.
- **The Kun-Peng:** the **Kun**, the same Kun that Kun Rises from the Northern Abyss summons (black water, glowing wave scrolls),
  some fifty blocks long, swims the sea of clouds over the open sea, its back and sail breaking them. It throws spouts of cloud up
  under whoever is over it and rams up out of the clouds.
  - Brought to half its strength, it breaks out as the **Peng**, a white-and-azure bird whose wings span some sixty blocks: blasts
    of wind, volleys of feather blades, dives.
  - Left alone, it goes back into the clouds as the Kun.
  - It leaves a True Immortal slip of its form's element (water as the Kun, metal as the Peng).
- **The True Phoenix:** crimson and gold, its wings some forty blocks across, five streamers of the five colours. It roosts high
  over a peak and fights with fire breathed in a cone, a rain of burning feathers, and the shock of its wings.
  - Slain the first time, it burns down to an **ember** for ten seconds (untouchable) and **rises again whole**. The second time it
    dies.
  - It leaves **True Immortal fox blood**.
- **The True Dragon:** some eighty blocks long, a serpent's body of gold scales with a thread of lightning down each flank, a crest
  of storm-light along its spine, a silver-white mane and beard, ivory antlers, storm-white eyes, five-clawed feet.
  - **Its head is alive** (`BeastModels.Model#gaze`; the user, 2026-10-27: "its head never moves"): it looks slowly about as it flies,
    leads into its turns, rises as it climbs and dips as it dives, turns hard to its foe, and rolls a little. Its neck carries part
    of each turn, so it bends rather than kinking at the head, and the body behind flies on as before. What is turned on top of the
    animation is taken off again before GeckoLib reads the bones each frame: GeckoLib eases into a move from a snapshot of the bones,
    and a turn left in it was counted twice (the head jerked some seventy degrees each time the Dragon roared).
  - It **rides its storm**: the sky and the fog darken as you near it, and lightning strikes about it without kindling anything.
  - Its moves: lightning called down where its foe stood (it roars, and sparks warn of it first), the sweep of its tail, its coils,
    its dive.
  - It leaves **True Immortal dragon blood**.
- **What they leave** (cores, Immortal Jade, blood or slip) **hangs glowing in the air where they fell**: over the sea of clouds it
  would be lost.
- **The fourth**, the Primordial Chaos (part 3c), keeps its lair out in the Chaos Frontier (below).

**True Immortal blood.** Demon-race immortals inside the realm drop their True Immortal blood too (`DemonBlood#carried`), and it has
its own golden phial.

Code: `ImmortalGate`, `ImmortalRealm`, `ImmortalRealmClient`, `ImmortalEssence`, `ImmortalCapital`, `CityPlanner`, `CityPieces`,
`ImmortalCityStructure`, `CityMarkets`, `ImmortalSects`, `ImmortalSectPlan`, `ImmortalSectPieces`, `CelestialCourt`,
`CourtMeritTokenItem`, `PrimordialBeast`, `PrimordialBeasts`, `PrimordialLooks`; tests `ImmortalCitiesGameTests`,
`ImmortalEssenceGameTests`, `ImmortalMarketsGameTests`, `ImmortalSectsGameTests`, `CelestialCourtGameTests`,
`PrimordialBeastsGameTests`. Everything was also checked in game.

## Past True Immortal: the Laws and the Five Decays

Phase 3 of the realm plan (2026-10-26; the user's picks: the five elements' Laws and three greater ones, a weathered Decay's Law chosen
by the player, a failed judgement a stage lost and the life kept). Built in parts: **3a** the ladder, the Laws and the Five Decays;
**3b** the Law domains (below); **3c** the Chaos Frontier (below); **3d** the small worlds (below).

**The ladder** (`Realm`). Six realms after True Immortal, four stages each: **Earth Immortal**, **Heaven Immortal**, **Golden
Immortal**, **Taiyi Golden Immortal**, **Profound Sovereign** and **Heavenly Lord**, the last (the user named the last two). Each is
**twice the last** (body, blows, qi; the table under The realms after Four Axis), wants eight times the cultivation for five times the
gain (every stage still longer than the one before), and is the Immortal Realm's to cultivate (its needs a thousand times, as True
Immortal's). Lifespan without end. Walking and flying speeds stay at True Immortal Grand Perfection's (the land loads no faster); the
core's blast grows 45 blocks a realm. **Breakthroughs into True Immortal and past it are made only in the Immortal Realm** (the user's
rule, 2026-10-26: Entering Nirvana Grand Perfection into True Immortal Early, and every stage and realm after it, on every path):
elsewhere the bar waits full and the Breakthrough button says to go up through the Immortal Gate (`PlayerCultivation#isImmortalRealmLocked`;
the realm's own cultivators are held there by the census already). Tribulations keep True Immortal's 3,200 strikes, each heavier, tuned as before (Protection IV and
a beacon's Resistance II live, Resistance I with Regeneration dies; `trib-tune-1026.ps1`). No method, foundation or blood carries one
up it: **the Laws do**. In the worlds: the immortal sects' Sect Lords are Earth Immortals, the Celestial Court's Venerables Taiyi
Golden Immortals and its King the one Profound Sovereign; a handful more may rise in the Immortal Realm; no Heavenly Lord walks any world.
The merchants sell each realm's demon blood (above).

**The Laws** (`Law`, `Laws`, `LawState`). **Metal, Wood, Water, Fire, Earth**, and the greater **Space, Time, Life and Death**, nine
levels each.
- **The way up:** breaking into each realm of the ladder wants so many levels of the Laws together: **3, 7, 12, 18, 25, 35**, on the
  heaven path, the ghost path and the Demon Path alike (the Demon Path wants its race's blood besides).
- **Comprehending:** by True Immortals and those past them. **Meditating in the Immortal Realm** comprehends the Law in focus,
  whichever qi the meditation draws in, at its pace: level L wants L quarter-hours (the first fifteen minutes, the ninth two and a
  quarter hours). A foe of Entering Nirvana or above slain by an element's spell gives a **glimpse** of its Law one time in four (a
  quarter of its next level). Each **Five Decays weathered** gives a level outright, chosen by the player. The greater Laws open to
  comprehension at **Golden Immortal**.
- **What they give:** an element's Law its spells **+10% harm and +5% reach** a level (the reach of what they aim at). **Space:**
  Void Shattering's spells as much harder and farther, Nine Nether Step and Soul Dash 5% farther a level, the Spatial Tear 3% sooner
  ready. **Time:** every spell's cooldown 3% shorter a level. **Life and Death:** the ghost path's spells 10% harder and all the
  healing you take 10% greater a level. Players' alone.
- **The Methods tab's Laws page** (from True Immortal on): the levels held and what the realm ahead wants, the Law in focus, levels
  owed to choose, when the Five Decays come; each Law with Focus (and +1 when a level is owed), its pips, its insight toward the next
  level, what it gives and gives now.

**The Five Decays of Heaven and Man** (`FiveDecays`). Every True Immortal and higher player, every **ten in-game days**, warned the day
before. Five signs a minute apart, each staying till it ends: the **robes stain** (armour to 70%), the **flowers wither** (nothing
mends you), the **qi falters** (gathering halved, immortal essence draining wherever you are), the **body sweats** (blows and spells to
70%), the **mind can't rest** (no meditation). A minute later the **judgement**: a storm, thirty seconds, the strikes of the
tribulation into the stage after yours, past every ward. **Weathered:** a level of the Laws to choose. **Struck down** (by it or
anything else while it runs): you live on **a stage lower**, at half your health. The Decays wait where the heavens can't see (the
Netherworld, the Spatial Gap, the Inner Realm), for a soul away from its body or in a clone, for the dead, for creative players and
through a logout. The realm's own immortals never decay.

**Not done:** the spells don't grow in size past their own realm's (each spell's size is fixed by its realm); the ladder's might shows in
the harm (each realm twice the last) and in the Laws' reach.

**The Law domains** (`LawDomains`, part 3b, 2026-10-26). A Law comprehended to its first level, an immortal awakens its **domain**: an
ability for each Law on the Abilities tab and the wheel (Metal Domain ... Life and Death Domain), off to begin with, **one lit at a
time** (lighting one folds the other away). Lit, the Law's ground unfurls round you, a sphere that goes where you go: **8 blocks and 2
more a level** of its Law, a tenth again for each realm past True Immortal (some 42 blocks for a Heavenly Lord at level 9). It burns
**immortal essence**: a full pool in **120 s** (beside what the Immortal Essence switch burns), won't unfurl on less than 5 s of it,
and folds away when the pool runs dry, at a death or a logout, while your soul is out of your body, and in a passage. Creative play burns
none. It works on **your foes** alone: monsters, whatever attacks you, and the players and cultivators you are at odds with (blows passed
between you in the last 10 s); never your pets, your sect's fellows, a player you may not harm, nor anyone behind a barrier you couldn't
break, so a domain carried through a city falls on none of its merchants. In it **your Law's spells strike them 25% harder** (the
element's spells; for Space a spell of space, for Life and Death the ghost path's). Each second (harm a share of your blow, as its
element's qi: twice again each realm, and the Law's +10% a level besides):
- **Metal:** 15% of your blow; their armour cut 4% a level while in it.
- **Wood:** roots drain them for 15%, half of it back to you as life; Slowness (I, II from level 4, III from level 8).
- **Water:** 15% and Slowness; what burns on them goes out, and the fire they deal is 4% a level weaker.
- **Fire:** true fire, 25%, past any resistance or immunity to fire; they're set alight.
- **Earth:** 15% and Slowness; fliers dragged down out of the air (no flying in it), spared the fall; a cultivator's flight ended.
- **Space:** no harm; space locked on them (no Nine Nether Step, Soul Dash or Spatial Tear), nothing it catches leaves it (drawn back
  from its edge, put back in if thrown out), Slowness.
- **Time:** no harm; heavy Slowness (II up to V) and slower blows, their spells cooling 10% a level slower.
- **Life and Death:** no harm; nothing in it heals at all, and 3% a level of the harm you deal them comes back to you as life.

Seen by everyone near: a sphere of the Law's colour, faint and rim-lit (fainter from within), patterned with the Law's own stuff
drifting round it (blade-light, vines, water, tongues of flame, molten veins, stars, rings, black smoke), its seal turning on the ground
beneath (the five elements' seals; Space's 空 and Time's 時, new, from the rune; Life and Death's taiji) with a ring running out over it,
motes of the Law in it. Icons and seals: `java tools/beast-art/BeastArt.java <assets> domains`. The realm's own immortals have no Laws,
and so no domains.

Code: `Law`, `Laws`, `LawState`, `FiveDecays`, `LawDomains`, client `ClientLawDomains`; tests `LawsGameTests`, `LawDomainsGameTests`.

**The Chaos Frontier** (`ChaosFrontier`, part 3c, 2026-10-26). The Immortal Realm's edge, where chaos from before heaven and earth still
churns. From **4,000 blocks out from the main island's heart** the realm's land breaks up, and past **4,150** (its edge) it is all the
Frontier's: **floating shards of grey-violet land**, ten to sixty blocks across, between the sea of clouds and about Y 260, tuff and
deepslate veined with amethyst and crying obsidian; the sea of clouds below grey-violet and churning, the sky bruised violet, the sun
lost, the fog close. Further out the shards **join into one wasteland** (from **4,300 blocks out**, whole by about **5,000**): they grow,
settle toward one level and run together, first into rafts with gaps between, then one land with holes in it, then no holes at all. The
wasteland rolls in low hills some 30 to 50 blocks thick, floating over the sea of clouds: dig down through it and the clouds are below.
No city, sect, wandering immortal or other beast of the realm's comes out there. In worlds made before it, land already generated past
4,000 blocks stays as it was (a seam where the new begins).
- **Its storm:** a player out in it unsheltered loses **1% of their health a second**, through armour and every ward, and their body
  doesn't mend of itself (nor spends food on it): spells and pills still heal. Any harm breaks a meditation. **Shelter:** standing within
  a lit **Law domain**, your own or one whose owner isn't at odds with you, or in the **First Flame's light**. Creative and spectating
  players are untouched.
- **Chaos Fiends:** wraiths of its chaos (grey-violet smoke round broken stone, long clawed arms, white-fire eyes, shards circling them).
  They come out of the air round anyone in it, up to 6 near each, the stronger the deeper: **True Immortal Mid to Grand Perfection**
  within 1,000 blocks past the edge, **Earth Immortals** to 2,000, **Heaven Immortals** beyond. They fly, circle loosing chaos bolts and
  dive to claw; they leave alone anyone a realm or more above them, and anyone in the First Flame's light (where they burn away). They
  leave a **Chaos Origin Stone** 40% of the time and a **Dao Fragment** 5%: no core, blood or hide.
- **The Primordial Chaos:** Hundun's remnant, the fourth primordial beast, a faceless sack of churning chaos banded in the five colours
  fading to grey, six legs and four wings, some forty blocks across: a **Golden Immortal of Grand Perfection, ×1.25**. Its lair is
  **5,600 blocks out**, which way round chosen by the world's seed; it comes when someone nears (a purple bar), returns 5 days after it
  is slain, and the Heavenly Venerables tell its way. **The Unmaking:** it draws in on itself for two seconds (told, chaos streaming into
  it, a ring marking its reach), then bursts 30 blocks round, hurling everything out and **folding away every Law domain whose owner it
  catches**. **Devour:** everything within 40 blocks pulled in for three seconds and crushed against it. A fan of nine chaos bolts. Heaven
  Immortal fiends called out of the chaos about it. Slain: **6 Dao Fragments** of six Laws, **12 Chaos Origin Stones**, **12 Immortal
  Jade**, hanging where it fell; the Court pays its officers who fought it **40 merit**.
- **The First Flame's spring:** one in every world, on its own level island **4,600 blocks south** of the heart, open sky round it. Its
  basin is laid the first time someone comes near: a ring of quartz round a bed of the **First Flame** (a new block: white-gold, the
  brightest light, harming none, taken by no one), a gold-topped post at each quarter. Its light, **16 blocks** about the flame, shelters
  from the storm; fiends burn away in it; **meditating in it comprehends the Law in focus three times as fast**.
- **Dao Fragment:** a shard of one Law, coloured by it. Use it: **half of that Law's next level of insight**, if you may comprehend it
  (an immortal; a greater Law only from Golden Immortal). **Chaos Origin Stone:** chaos set hard, what a small world's seed is made of
  (part 3d). From the fiends, the Primordial Chaos and **Chaos Origin Ore** in its land, about as rare as diamonds (a diamond pickaxe or better;
  Fortune helps).
- Art: `java tools/beast-art/BeastArt.java <assets> chaos` (the fiend, the Primordial Chaos, the ore, the stone, the fragment, the
  flame).

Code: `ChaosFrontier`, `ChaosFiend`, `FirstFlame`, `FirstFlameBlock`, `DaoFragmentItem`, `ChaosOriginStoneItem`, the Frontier's land in
`ModNoiseSettings#immortalRealm`, the Primordial Chaos in `PrimordialBeast`/`PrimordialBeasts`, client `ImmortalRealmClient`; tests
`ChaosFrontierGameTests`.

**The small worlds** (`SmallWorlds`, part 3d, 2026-10-26; the user's pick of who comes in: you and whoever you allow). An immortal's own
world: a floating island under the stars.
- **The World Seed:** crafted from **5 Chaos Origin Stones and 4 Immortal Jade** (stones in the corners and the middle, jade on the
  sides). A **True Immortal** (or higher) holds it in use for **10 seconds** to refine it into their own small world; it is used up.
  One world each; below True Immortal the seed won't take.
- **The world:** in a dimension of its own, every world in its own slot 4,096 blocks from the next. Grass and earth over stone that
  hangs into the void, roots here and there beneath; a pool and three cherry trees about its heart; its **gate**, a sheet of starlight
  in a frame of jade, behind the way in. Always lit as by day, under a sky that is all stars. Nothing is born there, it never rains,
  and beds work (set your spawn there). Its land reaches **16 blocks out, 2 more for every level of the Laws you hold** (up to 160): new
  land is laid around its edge as your Laws grow, never over anything that stands there.
- **Its qi is the Immortal Realm's** (a thousandfold): your immortal essence fills there, and meditating there comprehends your Law in
  focus. A breakthrough into True Immortal or past it is still the Immortal Realm's alone.
- **The door** (the **Small World** ability, an act, on the Abilities tab and the wheel): a door of starlight opens before you, almost
  anywhere (not in a passage, the Inner Realm or a small world, nor with your soul out, nor in a tribulation), for **30 seconds**. Walk
  through it and you're on your island. Use the ability again inside, or walk through the gate, to come out where you went in. Fallen
  off the island, or flown far out into the nothing round it, you're set back on it unharmed.
- **Guests:** `/smallworld guest add <player>`, `/smallworld guest remove <name>`, `/smallworld guests`, `/smallworld` (its reach and
  guests). Guests on the list may walk through your open door (anyone else is turned away) and leave by the gate; one taken off the
  list while inside is sent out.
- **Carried up the ways** (the user's ask of 2026-10-27: anyone with a small world can ascend with the people in it, at the same
  cost as Universe in a Sleeve). Your world goes with you. Take the Spatial Gap, the Void Breach or the Immortal Gate and its door
  shuts behind you, and everyone inside (and anyone in their sleeves) comes through with you, whatever their realm. **Every spatial
  pressure on you doubles for each of them**, counted with any in your own sleeve, and a True Immortal takes the Gate's quarter body
  the same way. When you come out into a world, the world's gate leads out **beside you there**. If you die on the way, they stay
  safe inside, and the gate still leads back to where they came in. Stepping out partway, a guest comes out where they came in.
- Art: `java tools/beast-art/BeastArt.java <assets> worlds` (the seed, the ability's icon).

Code: `SmallWorlds`, `SmallWorldDoor`, `SmallWorldGateBlock`, `WorldSeedItem`, `SmallWorldCommand`, client `SmallWorldClient`,
`SmallWorldDoorRenderer`, `SmallWorldGateRenderer`; tests `SmallWorldGameTests`.

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
blocks of the well. Settlers seek no fights (not even with the devilish path); they only defend themselves. A site whose ground is
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
0 to 100%, its percentage written across it with an arrow (up while rising, down while ebbing, "=" while a pill holds it).
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
Netherworld, and as thick as the Great Void's own qi over its Ancient Battlefields (about half the motes there).

| Where | Density (a Soul Sand Valley = 1) | The pool fills, a second | The ghost path goes |
|---|---|---|---|
| The Great Void's **Ancient Battlefields**, where the armies of Heaven and the demons fell (the user's ask, 2026-10-27: a place denser than the Netherworld) | 100 (`GhostQi.ANCIENT_BATTLEFIELD`) | 20 | x100 |
| The Netherworld | 10 (`GhostQi.NETHERWORLD`) | 2 | x10 |
| The Nine Nether Peaks | 12 (x `PEAKS_FACTOR`) | 2.4 | x12 |
| A Soul Sand Valley | 1 (`SOUL_SAND_VALLEY`) | 0.2 | x1 (the baseline) |
| The rest of the Nether | 0.5 (`NETHER`) | 0.1 | x0.5 |
| The Overworld, anywhere in it (the thinnest, the user's call 2026-10-19) | 0.25 (`OVERWORLD`) | 0.05 | x0.25 |
| Everywhere else (the Upper Realm, the Spatial Gap, the End, the Astral Realm, the rest of the Great Void) | 0 | nothing | not at all |

On a battlefield the Cultivation tab says it is **teeming**, and a ghost-path seclusion there costs a tenth of the Netherworld's years
(`Lifespan#ghostPlaceSaving`, as the Peaks save a little). Yin corruption stays the Netherworld's alone.

The pool fills at density x 0.2 a second at Qi Refining Early (`FILL_PER_DENSITY`, the table's numbers); spirit stones and
Spirit Dew don't fill it. Everyone who has one sees it as a cyan **Ghost Qi** bar: every ghost, and every living cultivator who
knows a ghost method. It holds 30 (`BASE_POOL`), plus 10 for every layer known (`POOL_PER_LAYER`: your deepest foundation's, and
the fire's and the steps'; `GhostMethods#layersKnown`), plus 20 more for every layer of your deepest foundation
(`BREATHING_POOL_PER_LAYER`). Ghost abilities and spells are paid from it, never from your qi.

**It grows with the ghost path's realm** (the user's ask, 2026-10-27: "ghost qi pool should increase with realm in ghost path similarly
to how it does with the others"; before, a Heavenly Lord's ghost path with no methods held 30 and filled at 0.05 a second in the
Overworld). On top of the methods' share the pool gets what the heaven path's qi pool gets at the same realm and stage
(`GhostQi#realmPool`, `CultivationStats#maxQi`: 100 at Qi Refining Early, 2,500,000 at True Immortal Early, 160 million at
Heavenly Lord), and it fills as many times faster as that realm gathers qi over Qi Refining Early (`GhostQi#realmFill`,
`CultivationStats#qiGather`: x62,500 at True Immortal Early), the place still its measure (the Overworld worst, the battlefields
best). As Realm Suppression and pressure leave the path; nothing from a mortal's dormant one.

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
  Wandering Soul Steps' fourth layer, +10% a layer; Meteor Flight's fast mode from a Core Formation path, see Meteor Flight), spending nothing (`TwoPaths#fliesFreely`; the living in a Ghost King's form fly so too, see "The two paths"); not on the Soul River,
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
| **Ten Thousand Ghosts Union Scripture** | to Dao Integration | 63 | Procession of Ten Thousand Ghosts (57), Gate of Ten Thousand Ghosts (61) | Nine Nether Peaks steles, about two in 1,377; the few Dao Integration ghosts |
| **Void-Rending Nether Canon** | to Void Shattering | 72 | Rift to the Nine Nethers (66), Bridge of No Return (70) | Peaks steles, about one in 1,377; the handful of Void Shattering ghosts |
| **Hell-Forged Vessel Sutra** | to Sacred Vessel | 81 | Mirror of Retribution (75), Soul-Locking Pagoda (79) | Peaks steles, about one in 1,377; the one Sacred Vessel of the dead |
| **Underworld Nirvana Scripture** | to Entering Nirvana | 90 | Black and White Impermanence (84), Book of Life and Death (88) | Peaks steles, about one in 1,377 |
| **Netherworld Immortal Sovereign Canon** | to True Immortal | 99 | Fengdu's Walls Rise (93), Ten Thousand Souls Ascend (97) | Peaks steles, about one in 1,377 |

**The ghost path goes on to True Immortal** (the user's ask, 2026-10-22), its last five foundations those of the realms after
Four Axis (Dao Integration's and Void Shattering's spells built, 2026-10-24: `GhostSpells`; the rest still to come), every stage of them longer than the one before as below it (`GhostMethods#required`), and
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
- **Harmony.** The two paths together add to that body, the more the closer they stand: **+30%** with both at the same
  stage, **+20%** a stage apart, **+10%** two stages apart, nothing further (`TwoPaths#HARMONY`, `#stageGap`; the stages are
  counted across realms, so Four Axis Grand Perfection and Dao Integration Early are one apart), on top of Heaven's Favour.
  (By realm, within three realms, before the user's call of 2026-10-24: too generous.) Since 2026-10-24 (the user's ask: with
  every path at True Immortal Grand Perfection the health all but doubled and the qi didn't) the harmony raises the qi pool and the
  ghostly qi pool by the same share as the body. The Stats tab names the path the body follows and the harmony; on the Cultivation tab the two paths
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
  | at the same stage | x1.3 | Grand Perfection | Quasi- next realm | Quasi- next realm | Quasi- next realm |
  | a stage apart | x1.2 | Late | Quasi- next realm | Quasi- next realm | Quasi- next realm |
  | two stages apart | x1.1 | Mid | Quasi-Grand Perfection | Quasi- next realm | Quasi- next realm |

  (The body is the higher path's, so "Mid" means the higher path at Mid.) Mid on both paths comes 5% (Qi Refining)
  to 18% (Heavenly Being) of the way to the next realm. Even Grand Perfection on both paths with full Favour (x1.3 x1.15) stops
  short of the next realm's Early: Foundation Building's comes about half way, Heavenly Being's most of it.
- **Spells of both paths**, one for each realm from Foundation Building, are the **heaven path's and the Demon Path's**
  (the user's ask, 2026-10-24: "the dual path spells are now from demon and heaven path instead of heaven and ghost"): they need
  **both of those at that realm**, are cast by the living alone, and cost **qi alone** (the Demon Path shares the qi pool; below).
  The heaven path and the ghost path have none together now: the five they had were built on the ghost path's half, so they were
  made anew. When both paths reach a realm you're told, and its spell goes on the spell wheel.
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

| Spell of both paths | Heaven and Demon Paths at | Qi | Cooldown | What it does |
|---|---|---|---|---|
| **Heaven-Demon Palm** | Foundation Building | 35 | 6 s | a palm of gold light, a demon's crimson claw within it, strikes the first being within 16 blocks: 5 hearts of harm, raked and thrown back; a quarter of the harm mends you |
| **Heaven-Demon Mantle** | Core Formation | 85 | 30 s | gold light and crimson demon scales wrap you for 10 s: arrows, fireballs and anything else shot at you from within 3.5 blocks are turned back, the shooter's no longer; Resistance I |
| **Devouring Seal** | Nascent Soul | 190 | 20 s | the being you look at (32 blocks) is sealed under heaven's gold seal for 8 s while a demon's maw devours it: it loses 2 hearts a second and half of it mends you; slowed while sealed. Looking at nothing, it isn't cast (nothing paid) |
| **Heaven-Demon Claw** | Heavenly Being | 410 | 25 s | a great claw of gold light and crimson qi rakes down where you look (32 blocks): everything within 5 blocks takes 15 hearts of harm, is thrown up and weakened for 5 s |
| **Heaven and Demon as One** | Four Axis | 920 | 2 min | heaven and demon made one: everything within 16 blocks is dragged in and takes 25 hearts of harm; a tenth of all of it mends you, and Strength II for 10 s |
| **Heaven-Demon Avatar** | Dao Integration | 3,200 | 100 s | a colossal avatar of gold light and crimson demon qi, 41 blocks tall, rises behind you for 8 s (a living shape: below), turning to where you look; twice its hands come down out of the sky where you look (82 blocks), heaven's palm then the demon's claw: each strikes everything within 14 blocks for 140 harm, throws it down and slows it; a fifth of all the harm mends you. The palm crushes a crater 18 blocks round and 8 deep into the land with cracks running out 40 blocks; the claw rakes four gouges 28 blocks long and 8 deep through it, and a lesser crater where it struck |
| **Heaven-Demon Void Shatter** | Void Shattering | 11,000 | 100 s | a colossal fist forms over your shoulder, heaven's gauntlet of gold plates closed round a demon's crimson claws (a living shape), and punches out to where you look (118 blocks): where it lands space shatters like glass (Heaven methods: Tears in space): everything within 18 blocks takes 520 harm and is thrown out, and the shattered space, 37 blocks across, hangs there for 5 s, cutting whoever stays in it for 60 a second. The land under it shatters too (landing within 18 blocks of it): a crater 28 blocks round and 15 deep, twelve cracks running out of it 55 blocks like a pane's |
| **Heaven-Demon Bell** | Sacred Vessel | 24,000 | 100 s | a colossal bell, heaven's gold on one side and the demon's crimson bronze on the other, falls over where you look (136 blocks), its rim pressing a crater 19 blocks round into the land, and tolls three times a second apart: each strikes everything under it, within 19 blocks, for 500 harm through armour, and its wave runs out to 38 blocks, 200 more and thrown back, the land cracking out to 40; a fifth of all the harm mends you |
| **Heaven-Demon Eclipse** | Entering Nirvana | 52,000 | 100 s | heaven's sun and the demon's moon come together 58 blocks over where you look (154 blocks), the moon before the sun; a beam of darkness pours down and pulses three times, three quarters of a second apart: 1,200, then 600, then 600 harm through armour to everything under it within 22 blocks, held where it stands, the first burning a crater of black glass 19 blocks round and 7 deep; a fifth of all the harm mends you |
| **Primordial Chaos** | True Immortal | 450,000 | 100 s | a sphere of the chaos before heaven and earth were parted, heaven's gold and the demon's crimson swirled together, swells 24 blocks over where you look (218 blocks) and churns for 5 s: everything within 54 blocks is drawn in toward it, and every half second everything within 27 of its heart takes 1,000 harm through armour; then it bursts, 15,000 harm through armour within 34 blocks, thrown out, a crater of glass 24 blocks round and 10 deep; a fifth of all the harm mends you |
They took the places of the heaven and ghost paths' five (Yin-Yang Palm, Taiji Shield, Life and Death Seal, Heaven and Hell
Pillar, Primordial Chaos), keeping their reach and strength, their costs the two pools' added into qi. Their harm is heaven's and
a demon's qi as one (`ModDamageTypes#HEAVEN_DEMON`): through armour, as the elements' qi, no element's ward reading it; it reaches
the dead as a living blow does. Cultivators don't cast them. Icons: the Palm an open gold hand with three crimson claw-marks;
the Mantle a round mantle, gold light above and crimson scales below, an arrow turned back off it; the Seal a crimson maw in a
ring of gold; the Claw four crimson rakes edged with gold; As One two arms, gold and crimson, wound into a heart of white light;
the Avatar its split head, haloed and horned; the Void Shatter a fist, gold and crimson, struck into space, cracks running out from
where it struck; Form Shift a face half a man's and half a crowned ghost's.

Op commands for every path (the user's ask, 2026-10-24: before, only the heaven path's could be set). Without a path named they
are the heaven path's, as they always were:

```
/cultivation set [heaven|ghost|demon] <realm 1-11> <stage 1-4>   that path there, its stage's cultivation none
/cultivation addcultivation [heaven|ghost|demon] <amount>       that much on its bar (the ghost path's in seconds of meditation)
```

The ghost path's set opens a mortal's meridians (a mortal's ghost path lies dormant) and leaves its methods understood exactly as
far as it (as `/method path`; `/method learn <id>` gives one). The Demon Path is a demon race's alone: a human's refuses.

Code: `TwoPaths` (the paths, `body`, `harmony`, `bothReach`, `walksHeavenAndDemon`, the forms and `look`, and the once-a-second
watch that tells of new forms and spells), `CultivationStats#apply`, `Spell` (`isDual`, `dualRealm`, `qiCost`, `realm`),
`HeavenDemonSpells` (`palm`, `Mantle`, `Seal`, `claw`, `asOne`, `Avatar`, `Shatter`; `PALM_*`, `MANTLE_*`, `SEAL_*`, `CLAW_*`, `ONE_*`,
`AVATAR_*`, `SHATTER_*`), `Ability#FORM_SHIFT`, `ModPackets#sendGhost` (the look); client `ClientGhosts#drawnAsLight`/`crowned`,
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
once. **C** ("Cast Readied Spell") casts the readied one; its icon, and the seconds left while it cools, sit in a slot right
of the hotbar, C in its corner. Each costs ghostly qi (the spells of both paths, above, cost qi instead) and has a cooldown; none can be cast on the
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
| **Procession of Ten Thousand Ghosts** | Ten Thousand Ghosts Union 57 | 200 | 45 s | a column of the dead, ghost fire and mist, pours out along your look for 61 blocks (short of a wall), 8 wide: every being it passes through takes 90 ghostly harm and is slowed and weakened for 5 s; the dead take half as much again |
| **Gate of Ten Thousand Ghosts** | Ten Thousand Ghosts Union 61 | 300 | 90 s | a gate of the dead, bone-white mist and ghost fire, rises where you look (82 blocks) facing you and opens for 8 s: every foe within 18 blocks is dragged toward its doorway and takes 30 ghostly harm a second; then it slams shut, 120 ghostly harm to everything within 18 blocks; a quarter of all its harm mends you |
| **Rift to the Nine Nethers** | Void-Rending Nether Canon 66 | 280 | 60 s | the ground where you look (118 blocks) tears open onto the Nine Nethers, nine rings of ghost fire turning down into it, each narrower and deeper: for 6 s every foe within 22 blocks is dragged toward it and down and takes 100 ghostly harm a second; then it seals, 300 ghostly harm to everything within 8 blocks of it, weakened for 8 s; the dead take half as much again |
| **Bridge of No Return** | Void-Rending Nether Canon 70 | 400 | 90 s | the Naihe Bridge, that the dead cross over the River of Forgetting, rises out of the mist along your level look, span after span, 118 blocks: every being within 6 blocks of a span as it rises takes 300 ghostly harm and forgets every blessing on it (its good effects gone); then for 5 s whoever stays on or under it is slowed and pays its toll, 50 ghostly harm a second; the dead take half as much again |
| **Mirror of Retribution** | Hell-Forged Vessel Sutra 75 | 320 | 60 s | the karma mirror of Yama's court rises over and behind your head and turns with your look for 3 s: its light pours out along your look, 64 blocks, and every being in it is shown its sins: 140 ghostly harm a second, weakened and slowed, and glowing wherever it hides for 8 s; then the mirror flashes, 500 ghostly harm to everything in its light; the dead take half as much again |
| **Soul-Locking Pagoda** | Hell-Forged Vessel Sutra 79 | 460 | 90 s | a pagoda of seven tiers, black iron and bone forged in the lowest hell, falls onto where you look (136 blocks): 300 ghostly harm to everything under it, and every foe within 15 blocks is locked in it for 6 s (brought back to its walls), 120 ghostly harm a second; then it seals, 500 ghostly harm to all locked in it and Weakness II for 10 s; the dead take half as much again |
| **Black and White Impermanence** | Underworld Nirvana Scripture 84 | 360 | 60 s | the two soul-catchers of the underworld rise out of the mist to either side of the soul you look at (72 blocks) and come for it: the White's banner, 500 ghostly harm, slowing it; the Black's chain, holding it 3 s at 300 a second; then their share of it, a tenth of its whole health (no more than 1,200); the dead take half as much again |
| **Book of Life and Death** | Underworld Nirvana Scripture 88 | 520 | 90 s | the book in which every soul's span is written opens over your head and finds the names of the foes round you, eight at most within 72 blocks: each marked, glowing, 250 ghostly harm a second for 3 s; then the brush strikes their names out, 900 each, and any below a quarter of its whole health is struck out of life (the Ender Dragon, the Wither, the Warden take 2,000 more instead); the dead take half as much again |
| **Fengdu's Walls Rise** | Netherworld Immortal Sovereign Canon 93 | 400 | 60 s | eight lengths of the walls of Fengdu, the city of the dead, rise in a ring 37 blocks round where you look (136 blocks), facing in: for 5 s no foe within gets out past them, and every half second the city's dead fall on every foe within, 800 ghostly harm, slowing them; then the walls topple in, 4,000 ghostly harm to every foe within; the dead take half as much again |
| **Ten Thousand Souls Ascend** | Netherworld Immortal Sovereign Canon 97 | 600 | 90 s | a helix of ten thousand souls rises out of the dark where you look (136 blocks) and spirals for 5 s: every foe within 34 blocks is drawn in, those within 14 carried up round it as high as 20 (a great beast only drawn), 1,000 ghostly harm every half second; then it goes back down, dragging them with it, 5,000 to each; the dead take half as much again |
None of a caster's spells touch the caster, spectators or the caster's own tamed creatures; harm to another player needs
PvP on. The numbers are the `Spell` enum (layer, cost, cooldown) and `Spells` (`GHOST_FIRE_*`, `BOLT_*`, `DASH_*`, `WAIL_*`,
`TIDE_*`, `WARD_TICKS`, `BURST_*`, `DECREE_*`, `REND_*`, `STEP_*`, `VERDICT_*`, `CHAINS_*`, `ANNIHILATION_*`); Dao Integration's
to True Immortal's, `GhostSpells` (`PROCESSION_*`, `GATE_*`, `NETHERS_*`, `BRIDGE_*`, `MIRROR_*`, `PAGODA_*`, `IMPERMANENCE_*`, `BOOK_*`,
`FENGDU_*`, `SOULS_*`): living shapes of GeckoLib's as the realms'
heaven spells' (the column of the dead, its lantern and soul-banners; the gate of bone under its roof, its doors of black lacquer
swinging in on a whirl of ghost fire, hands of the dead reaching out; the nine rings of ghost fire turning down into the rift, souls
drawn down between them and talismans round its rim; the bridge's spans of bone-white stone, lanterns of ghost fire on them, the
dark river and red lilies under them), the rift onto the Nine Nethers a tear in space as Void Shattering's heaven spells' are (Heaven
methods: Tears in space), their sizes by their realm, their harm on their own scale, below the heaven spells'; they leave the land
as it was, and ghost cultivators cast them too.

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

A beast's head worn on a man's body is a GeckoLib model of its own, drawn in place of the human head and its hat layer:
it turns and nods with the head it replaces (its pivot is the neck), so any humanoid body can wear one. Ox-Head and
Horse-Face wear them, and the demon races (Bloodlines, below) wear theirs with a tail. Since 2026-10-23 they are animated
(the user's ask: "actual beast heads instead of the stuck on model"): the ears flick now and then, the eyes blink, a
lizard's tongue flicks, a dragon's whiskers and mane drift, Ox-Head and Horse-Face chew, and the jaw drops as one strikes;
a tail sways standing and swings as its wearer walks, a fox spirit's fanned out one for each tail it has.

- **The list** is `BeastHead` (shared by both sides, so the server can name one and sync it): `OX`, `HORSE`, `TIGER`,
  `LIZARD`, `FOX`, `DRAGON`. Saved and synced by id; add new heads at the end.
- **The models** are GeckoLib models (Blockbench's Bedrock geometry: pixels, y up, the face toward -z, about the neck; a tail
  about the top of the body): `geo/beast/<id>_head.geo.json` and `<id>_tail.geo.json`, animations
  `animations/beast/<id>.animation.json` ("idle", "strike"), textures `textures/entity/beast_head/<id>.png` (Ox-Head's,
  Horse-Face's) or `textures/entity/bloodline/<race>_<colouring>_beast.png` (a race's head and tail on one texture). Client
  `BeastHeadModels` draws them (a tail's sway in code); each head names its **brow**, where a talisman sealed to it hangs.
- **Wearing one**: hide the body model's `head` and `hat`, `on(wearer, look)` (whose animations, which colouring),
  `follow(model)` after the body's `setupAnim`, and draw it with the same transforms as the body, in its `texture()`.
  `SoulRenderer#drawBody` does so for souls and the dead, `BeastLayer` for the living.
- **Making or changing one**: the models, their animations and all the textures (the race skins too) are written by
  `tools/beast-art` (`java BeastArt.java <the mod's assets folder>`; JDK 22 or later), cube by cube with a painter for each
  (fur, stripes, scales, eyes, nose...). The .geo.json files open in Blockbench as GeckoLib models to be tuned by hand there.

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
/method path <realm> <stage>   the ghost path set there (1-11, 1-4), its methods understood as far
/method forget <id>         as if it had never been learned
/cultivation set ghost <realm> <stage>        the same as /method path (a mortal's meridians opened: its ghost path would lie dormant)
/cultivation addcultivation ghost <seconds>   the same as /method progress
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
Refining's most often, Foundation Building's now and then, Core Formation's rarely, `chests/sect_library`; in the Upper Realm, Core
Formation's to Entering Nirvana's, each realm up rarer (since 2026-10-25, see the user's calls on the third sweep's suggestions); a ladder climbs
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
crimson pillars and dark nether-brick roofs trimmed in red with soul lanterns for the devilish.

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
  Terralith, which counts blackstone as Overworld stone, vanilla's gravel, dirt and ores used to come up through the devilish
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

**Spells stop at the barrier** (the user's pick, 2026-10-24). Whoever the barrier keeps out (too weak to break it, not let
through, not in creative) reaches nobody on its other side with a spell, just as their blows and projectiles don't. No zone,
quake, sweep, hold, push, rain or roar of theirs crosses the shell either way (`Formations#walls`, `HeavenSpells#walled`).
Anyone strong enough to break it, and those it lets through, cast across it as before. Before, a weak cultivator's area spells
cast beside a sect's barrier reached everyone inside.

**The strong break barriers** (the user's ask, 2026-10-24). The battery's stored qi is the barrier's health:
- Anyone strong enough to break it (its rank or above) wears it down with each blow on the shell, each shot that strikes it
  and each spell cast across it: 40 qi a blow from one just strong enough (a shot half, a spell twice), doubling for every two
  stages above it; once a second at most for each of them. Their spells' wounds also carve the ground it shelters and the
  cells of its shell as any land, each cell taken costing it as one broken by hand.
- One a **whole realm above it** (a True Immortal over all but the strongest barriers) empties it at once, with any blow, shot or
  spell: the whole barrier shatters, and can't rise for 2 minutes. A concealment barrier simply comes down.
- The weaker do nothing to it, as before. A sect is told its barrier was assailed either way.

**A guest pass** (the user's ask, 2026-10-24: there was no peaceful way in for one too strong to be welcomed as a guest). Every
cultivator of a sect (or clan) sells a **Guest Pass of** its sect first on its trade list: 30 Low spirit stones before regard,
one a day, to anyone at all it isn't resentful toward, weak or strong. Carried anywhere about you (the inventory or a hand), the sect's barrier lets
you through as it does its own, for a day (20 minutes) from its sale; the tooltip says how long is left. Raise the sect's
alarm and every pass of it you carry crumbles. Code: `SectPassItem`, `Formation#allows`, `CultivatorTrades#goodsFor`.

A barrier is raised, mended and watched over only while its land ticks (a player near enough, or the land held for a
moment: `NpcLedger`). Land that is merely loaded is left alone, so it can unload: reading its blocks every tick used to
hold it loaded for good, freezing the cultivators in it.

- **Power.** The core holds a battery of 2,000 qi. The barrier costs `0.5 + radius² / 80` qi per second (1.3 at radius 8,
  13 at 32, 52 at 64); each **Qi Vein** joined to the core gathers 8 qi per second. A raised barrier stays up while the
  veins and battery can carry it. Each barrier block broken drains 40 qi and each one mended costs 5, so a sustained
  assault can empty the battery and shatter the barrier, which then can't rise for 2 minutes (blows and spells of the strong
  drain it too: above).
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

### Concealment: the core's second function

Since 2026-10-28 (the user's ask: "the formation core now acts as a core for all types of formations except the storage one")
a Formation Core's screen has two tabs, **Barrier** and **Concealment**, each with its own radius (4 to the barrier's
greatest) and switch, both paid from the same veins and battery. A concealment costs three quarters of a barrier's upkeep
(`0.75 × (0.5 + radius² / 80)` qi per second); short of qi, it falls first, and the barrier only if the veins still can't
carry it. Its owner's mastery of formations (CraftMastery) takes 5% off both upkeeps for each rank past Apprentice, and every
ten minutes a function stands, its owner (if about) gains a point of practice for each. Sect cores keep no concealment.

Inside a raised concealment's sphere, anyone who can't see through it is shown **the land as it was before players changed
it**: houses, walls, digging, farms, the core and its seals, the barrier's cells (as the air or water they stand in), and
**the players inside** (with their sounds, particles and the cracks of the blocks they break) are all hidden. Its owner, the
players they trust, an operator in creative or spectator, and **anyone stronger than its owner** (the user's pick) see it as it
is. Walking in reveals nothing. **It hides; it doesn't block** (the user, 2026-10-28: "concealment barriers should no longer
provide an invisible wall that blocks people", it takes both "to be unseen and not able to enter"): the server reckons the
movement of those it hides from by what they are shown (`mixin/BlockStateBaseVeilMixin`, `Concealments#illusoryShape`), so they
walk through hidden work and over the ground they see over a hidden pit, are not smothered inside a hidden wall
(`mixin/EntityVeilMixin`), and feel nothing hidden (fire, thorns, a pressure plate); they can't mine or use the hidden blocks.
Only the barrier, raised as well, keeps them out: its cells keep their own rule though hidden. Both can stand at once if the
veins carry both.

How (`Concealments`): the world keeps a **land ledger** (`LandLedger`): for every block a player's hands (break, place, use an
item on a block or in the air: `mixin/ServerPlayerGameModeLedgerMixin`), a player's blast (`mixin/ExplosionMixin`) or a
player's spell (`HeavenSpells#carvable`) changes, the state that stood there first; a block set back to it is struck off. What the
world itself then changes at or beside such a block is noted too (the user's catch: the grass under a hut's walls died to dirt
and showed its outline): grass dying under a wall, a planted tree growing, poured water running on, fire spreading, a felled
tree's leaves falling. The weather's snow and ice count only lying on players' work (snow on a hidden roof), or a bare strip
round hidden walls would show them. It is written only where players have worked (one counter's and one field's check on every
other block change, `mixin/LevelChunkLedgerMixin`) and kept on each chunk, saved in the chunk's own file entry
(`mixin/ChunkAccessLedgerMixin`, `mixin/ChunkSerializerMixin`), so only loaded chunks hold theirs and a concealment raised long
after a base was built still hides it all. When a chunk goes to a player a
veil hides it from, its changed blocks and barrier cells are put right at once after it (`mixin/ChunkMapMixin`); every block
change, a hidden block's own data (a sign's words) and the light changes of such a chunk are rewritten or kept back as they
go out to each player (`mixin/ServerGamePacketListenerImplMixin`, `Concealments#shownPacket`); sounds and block events
(`mixin/PlayerListVeilMixin`), particles and breaking cracks (`mixin/ServerLevelVeilMixin`) reach only those it doesn't hide
from; the players inside are never tracked for them (`mixin/SoulServerPlayerMixin`). Once a second those near a veil are
weighed again, shown again if their seeing changed, and re-tracked when their standing among the veils changes. Where no
concealment stands, every check stops at an empty array. Veils are kept per world (`Concealments.Data`, as barriers are).
Tests: `ConcealmentGameTests`.

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
  monsters only rouse the members near by, as above (`SectManager.alert`). **Trespass** is breaking anything on a sect's
  grounds, by hand, or by a spell or Meteor Flight (the user's pick, 2026-10-24: wrecking them with a spell or a streak
  from outside the barrier, or anywhere with it down, went unanswered; `SectManager#spoiled`). A spell that takes or burns
  their ground, floods it, or strips its ores is answered as a pick would be, once a second however much it takes
  (theft, in the treasury). Its own disciples, and creative players, are no trespassers.
- **Cultivators cast spells** (the user's ask, 2026-10-24; `CultivatorSpells`, `Spells#castBy`).
  - **What they know:** a living cultivator knows its heaven method's element's spells, up to its method's realm and as far as its
    rank has come, as a player who knew that method would. That element is its sect's, its blood's, or its own. One of the dead
    knows its ghost foundation's spells.
  - **When they cast:** in a fight, every three to six seconds, out of their own qi and on their own cooldowns. They face their
    foe to cast. Badly hurt, they ward or mend themselves first; with a foe close, they cast a spell about themselves; otherwise
    one at their foe, the deepest the most often. Flying, they keep ten seconds of their flight's qi back.
  - **Whom they hit:** only whom they fight: their foe, whoever struck them, monsters, anything set on them, and their enemies.
    Their sect, their pupils, villagers and passers-by are spared, and their blessings are for their own.
  - **No terrain:** their spells leave the land as it was. No crater, cut, tunnel, fissure, lake or lasting fire, even where a
    player's spell would carve; walls, trees and ice set down for a while still come and go.
  - **In a spar** they cast nothing deeper than the realm they hold back to.
  - **Not theirs:** the spells that carry the caster (dashes, waves, a tree's lift, water walking), the yang bomb, those that take
    the land or its ore, and Life Force Gain stay a player's.
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
  (righteous, neutral or devilish) that will take it, as far as 8,192 blocks off (nearest first, so it goes further only
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
  spar with them, holding back to your own realm (fought as any spar is: blows pulled to half, lost at half your health),
  on the sect's sparring ground (they lead you there first: see Spar below).
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
  In another's body (Possession) it is that body's sect and title (`SectMembership#seat`), with a **Leave Sect** button for
  the body. In the body of a Sect Master or an elder (only so: a player's own Elder or Grand Elder title casts out no one,
  the user's pick after the bug sweep of 2026-10-23), each member below its title has a **Banish** button (click it twice
  to be sure): a cultivator is cast out to wander as a rogue (`SectManager#banish`), a player expelled and barred **30
  days** (C2S `sect_act`).
- **Belonging.** Its barrier lets you through. Its members are your fellows: never hostile to you, they come to your aid when
  you are struck near them, and your spells spare them. On its grounds you cultivate **a quarter faster**. Its elders teach
  you its method once Courteous (others need Warm).
- **Its rules.** The treasury belongs to its elders. Below Elder, the first hand you lay on it (opening or breaking) is stopped
  with a warning; the second casts you out, and after that you are an outsider there like anyone (the seal, theft). Striking a
  fellow member outside a spar also casts you out. Cast out, the sect won't hear you for **30 days**. No hopper (or hopper
  minecart) draws anything out of a treasury's chests, a sect's or a ghost palace's (`HopperBlockEntityMixin`,
  `SectManager#treasuryAt`): one set under a chest emptied it with no hand laid on it.
- **Leaving.** Ask an elder to "leave the sect". It takes you back after **3 days**.
- Commands (op): `/sect join <player>` joins the nearest sect, `/sect leave <player>`; `/sect info` lists its disciples too.

### The sect mission board

Every sect keeps a **mission board** on its plaza (`SectMissions`; the user's "A sect mission board is a great idea", 2026-10-28:
the missions are the players' alone, and the sect's people only talk of them in passing; the rest is the plan they approved).
- **The board** (`MissionBoardBlock`): two red-lacquered posts, the board between them under a little tiled roof, its missions
  pinned up on slips. A sect sets it up itself (`SectMissions#layBoards`), old sects too, the first time a player comes within
  128 blocks of its Formation Core: 4-9 blocks out from the core toward its gate, on firm ground with room above it, two blocks
  clear of every post, its face to the gate. Nothing breaks it; one found gone is set up again the next day. Its item is the
  creative menu's alone. Its art is `tools/beast-art/MissionArt.java` (`java BeastArt.java <assets> missions`), its model
  `models/block/mission_board.json`.
- **Its page** (`MissionBoardScreen`, S2C `mission_board`, C2S `mission_act`), in the Codex's lacquer and gold: the sect's name, your
  title and Contribution (all you have earned, pointed at), and two halves. **Missions**: those you hold, then the day's postings,
  the one chosen in full beside them with what may be done with it (Take it; Hand in, A new slip, Abandon). **Exchange**: its
  wares, the one chosen beside them with its own description. What came of what you did shows at its foot. Not a disciple of it,
  the page only tells you so (and to ask to join).
- **Postings** are each disciple's own: five a game day, fitted to your realm (as cultivators weigh it): one **Easy** (a stage
  below you), two **Fitting**, one **Hard** (a stage or two above) and one more of either. Hold **three** at once. Six kinds,
  each the next round that can be had there:

| Kind | What | Pay (before realm and grade) | Stands |
| --- | --- | --- | --- |
| Hunt | A beast troubling the sect's lands, 100-400 blocks out: a wild demon beast of the mission's rank (Qi Refining to Core Formation; a pack of three Shadow Wolves), or for a sect of the Immortal Realm a Chaos fiend in the Frontier, as deep past its edge as its strength. No beast that strong (or Peaceful): the next kind | 20 | 2 days |
| Purge | A cultivator of the path that preys on the sect (the devilish on a righteous or neutral sect's people, a righteous zealot on a devilish sect's), named on its posting, of the mission's rank, 100-400 blocks out | 24 | 2 days |
| Gather | What the stores want, by realm: herbs of at least so many years (10 at Qi Refining up to 1,000 past Entering Nirvana), demon cores of a realm, jade, metal; the Immortal Realm's own past True Immortal. Easy asks three quarters as many, Hard half again | 12 | 3 days |
| Craft | Pills to brew (by realm, of a least grade: Low to Supreme, a grade less if Easy, more if Hard) or an artifact to forge (its grade by realm; a Hard one the weapon made of the sect's element) | 16 | 3 days |
| Deliver | A sealed letter to another sect of its world 300-3,000 blocks off, not of the other path (a clan's to a clan), handed to any of its people (talk: Ask, "I carry a sealed letter for your sect"); its sect thinks the better of the courier (+2) | 10, and more every 100 blocks | 2 days, a day more every 1,000 blocks |
| Guard | The gate watch: 3 minutes within 12 blocks of the gate, two waves (Easy) or three coming at it from 20 blocks out past it (1, 2 or 3 foes each by grade, a stage below the mission: wild beasts, or cultivators of the path that preys on it). Begun and left for 20 seconds, it is lost (-1 regard) | 20 | 1 day |

- **The pay** is Contribution: the kind's, half as much again for each realm, times 0.75 / 1 / 1.5 by grade; and the sect's regard
  (+1, +2 for a Hard one), so missions open the elders' Courteous and Warm talk too.
- **In the field.** Taking a mission gives a **Mission Slip** (a delivery's is the **Sealed Letter** itself: `MissionSlipItem`):
  its tooltip names it; held (either hand), it shows the way over the hotbar (to the quarry once it is out, the gate, the
  letter's sect; the watch's time left); its mission over, it crumbles. A lost slip: "A new slip" at the board. A hunt's or a
  purge's quarry comes out of the land, glowing, when its taker draws within 48 blocks of its ground (the land there running),
  and goes for its taker alone (`DemonBeast#hunts`, `CultivatorNpc#isEnemy`, the targeting goal); a cultivator never runs or
  begs. It is theirs when it falls by their hand (its spoils too: `ThreatLootMixin`); slain by another, no spoils, and it comes
  out again a minute later. A quarry come back with its land after its mission is over slips away.
- **Lapses.** A taken mission stands its days; past them it lapses (the sect thinks a little less of you: -1). Abandoning costs
  nothing, but its posting is gone for the day.
- **Contribution** is a number on your record in the sect, never an item (nothing to drop, steal or trade). The Sect tab shows
  it, what you have earned all told, and the missions you hold. **Leave the sect and it stays behind**, with your missions.
- **The exchange** (`SectExchange`): wares of the sect's world (the lower realms', the Jade Firmament's, the Great Void's, the
  Immortal Realm's: a grade up each time, prices x1, x2, x4, x6), each kept for a title and above, so many a day:
  - Outer Disciples: Qi Gathering (8) and Cultivation Pills (12), Ginseng (6) and Spirit Ginseng (10), Yang Fire Talismans (6), and
    a few spirit stones at a poor rate (4 Low for 10; 2 Mid, 2 High or an Immortal Jade higher up).
  - Inner Disciples: a Foundation Pill (40), a Core Pill (80), the day's two manuals of the sect's element's spells up to your
    realm (30, and 30 more for each realm of the spell's), the day's artifact (60), a storage formation (50).
  - Elders: the weapon made of the sect's element (150), the day's method of another element at your realm (120; never past what
    the worlds teach outside the Immortal Realm), the techniques' manuals you stand at the realm of (Clone 300, Universe in a
    Sleeve 500), a Longevity Pill (200).
- **The sect's talk** (`NpcLife`, `NpcLines`): a fellow disciple passing you speaks now and then of your deed of the day ("They
  say you slew the Bloodfang Tiger"), of the gate watch you have taken and not begun, or of the day's postings you haven't looked
  at; two of its people talk over a deed one of its disciples did today, or a mission one has taken on. Asked about the sect,
  its people mention the board.
- **Advancements**: Duty to the Sect (a mission seen through), Of Every Service (one of every kind), Pillar of the Sect (1,000
  Contribution earned in one sect). Codex: "The Mission Board" (Sects and Cultivators).
- Game tests: `SectMissionGameTests`.

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
  the grounds (by name, or by their title), and the rumours going round (`Rumours`: herbs, caves, killings). Rogues who meet
  on the road pass a few words too.
- **Juniors bow** to a passing Elder or the Sect Master, who may nod back.
- **Passing a cultivator** (within 6 blocks, 2.5 sneaking; once every few minutes each), it calls you as it would in talk
  (**What they call you**, under Talking to cultivators). One realm beneath you it bows; two or three, a deep bow; from
  four it **kneels** as you pass (within 5 blocks, rising once you are 8 away), its forehead to the ground from six. One who
  likes you greets you warmly, one who doesn't says so (stiffly, to a senior), one above you has a word for a junior, a
  mortal is warned, one of your own sect welcomes you home; a gate guard welcomes a friend and halts a stranger (with
  deference, a senior). One who needn't bow low to you speaks its mind of what it sees: a Supreme or Immortal spirit stone
  or a Cultivation Fruit in your hand, your wounds, your flight, your meditating in the open, a demon's head (its kin's, to
  a demon), the Five Decays on you, your Law domain lit, you wandering its grounds after dark.
- They remark on the rain and the thunder, dusk and dawn; grumble patrolling in the rain (nobody practises or strolls in
  it); mumble in their sleep; shout when the sect is attacked, its barrier struck or its treasury robbed; cheer a
  breakthrough (theirs, or a fellow's); cry out at a player's tribulation within 40 blocks.
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

Alignment runs from -200 to +200: **Devilish** below -50 (red on black), **Neutral** from -50 to 50 (blue), **Righteous**
above 50 (white). A sect shares one alignment (40% righteous, 30% neutral, 30% devilish); rogues are random.

- Neutral cultivators are peaceful unless provoked. Righteous and devilish cultivators attack each other on sight, and a
  sect treats players the same way by alignment.
- Killing a cultivator who hadn't provoked you costs 10 alignment, plus 2 for each realm of theirs; killing a devilish one
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
  the devilish mocking. A mortal player is spoken to as a mortal (Greet, Ask, Trade, Gift).

| Their realm against yours | How they treat you | What the wheel offers |
| --- | --- | --- |
| 2+ realms above | Dismissive | Greet, Ask (the basics: no rumours, nor their elders), Gift |
| 1 realm above | A senior, courteous but distant | All but Spar, until they are Courteous to you |
| The same realm | "Fellow Daoist", and they offer tea | All six |
| 1 realm below | They answer freely: how far along their stage they are, their elders' realms | All six |
| 2+ realms below | As below; pressed down by your Realm Suppress, they kneel | All six, and Demand and Release while you press them |

- **What they call you** (`NpcAddress`; the user's ask, 2026-10-27: a Qi Refiner shouldn't call a Four Axis or a Sacred Vessel
  just "Senior"). By how many realms you stand above them, as each shows itself (below Void Shattering the lower realms hold
  everyone to Heavenly Being, them and you alike, so there Ancestor is for Void Shattering and on):

| You stand | They call you | And |
| --- | --- | --- |
| 2+ realms below them | little friend, junior, ant, little soul (righteous, neutral, devilish, the dead) | |
| 1 realm below | young friend, junior, brat, young soul | |
| The same realm | Fellow Daoist | |
| 1 realm above | Senior | bow |
| 2 or 3 above | Esteemed Senior | bow deeply |
| 4 or 5 above | Revered Senior | kneel to you (`NpcAddress#KNEEL_GAP`), in talk and as you pass |
| 6 or more above | Ancestor; **Lord Immortal** from True Immortal on | kneel |

  A mortal calls any cultivator "Immortal" and their name (Lord Immortal from True Immortal on), another mortal "friend". A
  title you have taken (from Void Shattering on) is what they call you in place of all of those, bare ("young" it, or
  "little" it, from one above you); an Elder, Grand Elder or Sect Master of their own sect over them is called by that title.
  Nobody is called a Venerable or a Patriarch: those are ranks the Court and the sects' NPCs hold. Their greetings and
  answers in talk are written for every rung in every voice (`NpcLines` `talk.greet.*`, `talk.ask.*`, and a friend's own).

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
| Provoke them | -15, and they may attack: likelier on the devilish path or if they are stronger, never if a realm below you |
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
  fruit or spirit stones: you stake one, they stake something of like worth of their own, the winner takes both; lost,
  forfeit or left unsettled, theirs is never theirs to keep: it was only put up to be won (the bug sweep of 2026-10-23: they
  kept it too, richer for every wager, dropping it all when slain); one wager a day with each; never with one a realm or more
  beneath you, who couldn't win your stake: friendly only, the wager greyed out), and, with someone stronger, **don't hold
  back**. Who is senior goes by your body's realm on any path. A clan's other people leave a man sparring with one of them be. Neither of you starts below three quarters of
  your health. **One of a sect's people on its own grounds leads you to its sparring ground first** (the user's ask,
  2026-10-23; an entrance trial too): its training ground's ring, or the plaza for a sect without one ("Follow me to the
  sparring ground"). It walks to its place there and you to yours across from it (within 3 blocks); not followed within
  **90 seconds**, the bout is called off, and if it can't reach its place in a minute it begins wherever it stands, once
  you are by it. A rogue, or one off its grounds, spars where it stands. Three breaths, then it begins. Blows between you land at half strength (pulled), and it ends when either
  of you has lost half your health since it began, when you walk 16 blocks off from them (a forfeit), or after three
  minutes (a draw). (At a quarter's line, without pulled blows, one blow from an equal ended it at every realm up to
  Nascent Soul: such a blow takes 28-39% of one's health.) Nobody dies: a blow from the other that would go past the line stops at it. A stronger cultivator
  holds back to your realm unless told not to. Blows in a spar are no attack on their sect, and blows between you in the
  three seconds after it ends are pulled (one far stronger often ends it with the first blow, and the next swing came a
  moment later; it was taken for an attack, the whole sect turned out and the barrier put you outside), as are your stray
  blows on its sect's other members, in the bout or just after (a sweep or a spell's spread raises no alarm). Losing to a senior who is
  Warm to you can teach you their Insight. A weaker cultivator never runs from the one it is sparring with, however far
  above it they are (it would run from such a domain in a real fight); if it runs from someone else mid-bout, the spar ends
  with no result rather than as your forfeit. **A spar may go into the air** (the user's call, 2026-10-24: fine for you, and
  for them too). A cultivator that can fly (Core Formation and up, or Foundation Building on a sword) takes off after you as in
  a real fight. With its qi too low to stay up much longer, it comes down gently instead of dropping out of the sky, and rests
  ten seconds before flying again. One of your own sect fights a spar too: before, it stood there and was beaten.
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
  worth (225 for a Mid, and so on); the stone is used up. A stone part spent by meditation gives only the qi left in it
  (it gave all of it before the bug sweep of 2026-10-23), and the next of its stack starts whole.
- **Meditate** with one in either hand to cultivate 50% faster while its qi lasts: 30 seconds for every Low stone's
  worth (a Mid 4.5 minutes, a High 40, a Supreme 6 hours), then it is spent and the next of the stack begins. A bar under the
  stone, in its grade's colour, shows what is left of its qi (since 2026-10-27; before, only its tooltip did). In creative a
  stack is never used up, as with any item.
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
  together for devilish black and crimson.
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

## The second bug sweep (2026-10-23, night)

A sweep of the whole mod for bugs and holes (the user's ask: "Sweep the code for any bugs and gameplay loopholes or fall
throughs"). Besides what is told above where it belongs (the Demon Path, the races' gifts, the bulldozer, a breaking core, the
wager, the beasts, rings, qi feeding, the clans), it put right:

- **Spells:** a Vajra Body's thorns answer only blows of the hand (two Vajra Bodies' qi blows bounced off each other until the
  server crashed); an Earth spell's wall is raised from natural ground only (it copied a floor's planks, ores or anything built);
  Mineral Purification finding no ore pays nothing (its qi given back, its cooldown lifted), never takes a spell's own ore or what
  the caster couldn't dig by hand, nor anything in a sealed treasury or the Ghost Gate; no spell's terrain, fire, lake or
  devastation touches a sealed treasury, a palace's under its king's seal (an explosion neither) or the Ghost Gate; a container a
  great spell destroys spills what it held; spells spare the bodies of souls, clones and those turned inward as they spare their
  owners; spell cooldowns are kept on the record (a relog made them ready at once); a projected soul casts nothing; the Ghost
  Fire Bolt sets only what it harmed burning, and spares whom spells spare; Phoenix Nirvana holds off no death that nothing holds
  off (/kill, old age, one's own core), nor a soul's out of its body; the Remote Yang Bomb's setting off costs nothing; a melee
  blow's element touch comes after the blow, not inside it; Realm Suppress weighs no body left sitting.
- **The Chief Judge's eye** spares a True Immortal (it spared no one), and strips every path back, the Demon Path's and the
  ghost path's (a Ghost King kept its crown).
- **The dead and souls:** the Spatial Gap's pressure reaches the dead; a ghost's blow lays on its yin only if it lands at full
  strength; the Wheel's turn stops if the ghost is scattered meanwhile; one of the dead beaten by its heart demon is sent back to
  its body as the living are; the cursor's and crafting grid's things are left behind at the Wheel with the rest; a soul out of
  its body pays a barrier from its own path's pool, and is set back however fast it crossed the shell; a soul's sitting body is
  carried off by no portal, piston or current; the land held loaded about a soul's or a clone's body is let go where it was held.
- **Possession:** Burn Lifespan goes out going into a body and coming out of it; a soul spends no body's stat points; a
  possessed beast's half-done move ends, and its blows (the soul's) strike no player it may not harm.
- **The rest:** coming down from space into the Upper Realm is fall-protected until one lands; a clone can't sit to meditate
  or turn inward by any way; a cultivator slain of a blow's after-effects (a fall, fire, poison) within half a minute answers to
  whoever struck it; leaves on a formation's shell are displaced (they decayed into holes); a dragon clan's third hoard guard
  stands on the alcove's floor (it stood in a pedestal); a site lived in a while with no sect is decided at once.

What it left alone, for the user to decide, is listed in the session's report. Tests: `SweepGameTests`.

### The user's picks (2026-10-24)

Of what the sweep left for the user to decide, these were closed. The rest stand as they were, at the user's word.

- **Every ward gives way to a tribulation's strike.** No aura or bell takes anything from it, and no rebirth (Phoenix Nirvana, a
  lingering spell) holds off a killing one (`TribulationManager#isStriking`). Before, Vajra, Mountain Stance, Ancient Bark and the
  Golden Bell stacked against the heavens' strikes.
- **A Jade Poison Body's pills cost their years.** What it refines the moment it is taken (a pill, a fruit) costs the years
  meditating it would have, by its talent and savers (`Physiques#refinedAtOnce`). It cost none.
- **Fortune wishes give no more than the axis wants** (see The four axes of virtue). They were an endless spring of spirit stones.
- **The server's movement allowance.** A move may carry a flier only as far as their speed covers since the server last took
  their position (five seconds at most), plus two ticks' slack (`QiFlight#moveAllowanceSq`). Every packet was allowed a second's
  flight, so a client sending five a tick flew twenty times as fast.
- **Long steps through space pay their years.** A step longer than a tick's flight is free only when the server itself moved you
  (a command, a pearl, a rift: `AstralRealm#teleported`). Every long step was free.
- **The void and the gap reach the dead.** The Vestiges of the Void hunt ghosts and bite them in full. The Spatial Gap's storms strike
  the dead as the Netherworld's tribulation does (`SpatialStorms#hurt`): their ghost-form share of it, the bolt turned by ghost
  armour. A heart demon's blows land on the dead (they landed as nothing). A storm's strength goes by the higher of your paths.
- **The Inner Realm's island** is now told at its true size (above).
- **Spells stop at barriers** (see Protective formations).
- **Spars may go into the air** (see Talking to cultivators). Every cultivator's gentle landing no longer strikes it with the whole
  way down as a fall: its flight ends a little above the ground.
- **Wrecking a sect's grounds with a spell or Meteor Flight is trespass**, as digging there by hand is (see Sect members).
- **A Four Axis Ghost King's seal** stays breakable from Dao Integration, a full realm above it, as every seal is (the user's call).

Tests: `LoopholePicksGameTests`.

## The third bug sweep (2026-10-24, night)

A sweep after the spells of the realms past Four Axis were all in (the user's ask: "go through the entire code fixing any bugs that need
fixing and giving suggestions"). It put right:

- **A barrier is worn down only by harm that lands across it.** Asking whom a spell spares battered every barrier between caster and
  being: a heal, a golem choosing its foe, a Black Tortoise warding its own, a dragon's passive might, even an aim wore a sect's barrier
  down and raised its alarm against the player, though no one was harmed. Now only a spell's blow landing across a barrier its caster
  is strong enough for batters it (`Formations#harmedAcross`); the question itself is only asked.
- **Weighing a cut costs nothing.** Boulder Throw and Cave Formation weigh the ground before they take it; the weighing breached a
  barrier's cells and told the trespass, and the taking did it again (`HeavenSpells#mayCarve`).
- **A spell's mending goes to its caster's own.** The share of what Withering and Flourishing, Primal Wind and the First Seed of
  Creation drink went to everyone they spared: a monster walled off behind a barrier took a cut, the dead and spectators thinned it,
  and a cultivator's went to every passer-by. Now a player's goes to those its spells spare that are alive, present and not walled
  off, a cultivator's to its own (`HeavenSpells#own`).
- **The great cultivators tell their own years.** Every cultivator past Four Axis said it had cultivated Four Axis's 1,000 to 3,000
  years; they now say thousands (Dao Integration), hundreds of thousands, millions, tens of millions, and a True Immortal ages.
- **Nothing left to grow for good:** a cultivator's golems are no longer remembered after they crumble (a cultivator never logs out to
  have them forgotten), nor the barriers each being battered, nor a cultivator's lapsed debts to those who spared it, nor an elder's
  lapsed offers of the trial; and the Nine Cauldrons can't fail on a cauldron that never came.

Tests: `SweepThreeGameTests` (and `BarrierPassGameTests`, which now takes its sect cores down again: the test world is kept between
runs).

### The user's calls on its suggestions (2026-10-25)

- **Demon blood in the clans' chests** (the user: "demons should be able to loot the blood (although rarely, as rare as getting a heaven
  method or ghost method at that level from a library) from chests and stuff in the demon clans"). Each chest of a demon clan's den
  keeps, now and then, one phial of the clan's own race's blood, laid in it as the den is made (its loot fills in round it when it is
  opened): a hoard's chest 12%, the head's hall's 12%, an elder's 8% (`DemonBlood#hoarded`, `SectPiece#chest`). Its realms are a sect
  library's slips' in the same world, as often (below): a clan of the lower realms keeps Qi Refining's 60%, Foundation Building's 30%,
  Core Formation's 10%; one of the Upper Realm Core Formation's 20%, Nascent Soul's 20%, Heavenly Being's 18%, Four Axis's 15%, Dao
  Integration's 11%, Void Shattering's 7%, Sacred Vessel's 5% and Entering Nirvana's 4%: the Demon Path's way past Sacred Vessel, whose
  blood nothing else left. A sect of men's chests keep none.
- **The Upper Realm's sect libraries keep the Upper Realm's slips** (the user, the same day: "you can get EN phials from clans but you
  can't get EN methods from sects"). A library chest still keeps one or two Jade Slips, its element any of the five alike; in the Upper
  Realm they are now of the realms its clans' chests keep blood of, as often: Core Formation 20%, Nascent Soul 20%, Heavenly Being 18%,
  Four Axis 15%, Dao Integration 11%, Void Shattering 7%, Sacred Vessel 5%, Entering Nirvana 4% (before, the lower realms' Qi Refining to
  Core Formation). The library's chest takes its world's slips when it is first opened (`chests/sect_library`: a pool for the Upper
  Realm, by a location check, and one for everywhere else), so the unopened libraries of sects already standing take them too. The lower
  realms' clans keep blood of a library's realms there, Qi Refining to Core Formation (they kept Nascent Soul's 7% of the time, which no
  library there matched).
- **A great beast is judged against its caster** (the user: "Judge it relative to the caster"). Too great for a spell to hold, carry,
  swap or swallow is now a being of more than three times its caster's whole health (`HeavenSpells#greatBeast`, `#GREAT_BEYOND`), or
  one of the old world's great beasts (the Ender Dragon, the Wither, the Warden, an Elder Guardian: their fights are their own). Before,
  it was anything of 300 health or more, which made every cultivator from Dao Integration up too great for the very spells meant for them.
- **One caster's wounds in the land waiting to be cut are held to 250,000 blocks, everyone's to 2,000,000** (left to my judgement): a
  wound past it is cut short, what lies nearest its heart kept, or not cut at all; a burst golden core's goes whole (`Devastation#allowance`).
- **A guest pass is made out to its buyer** (the user: "Bind guest passes to the buyer"): sold made out to the one buying it (its
  tooltip names them), it lets them alone through its sect's barrier (`SectPassItem#boundTo`). One made out to no one (a command's) lets
  anyone through.
- **Nothing of True Immortal's is to be had in the worlds** (the user: "Make sure you can't get true immortal slips or methods in lower or
  upper realms, the highest you should be able to get is entering nirvana"; `Realm#taughtInTheWorld`). No Dao Stele carves a True
  Immortal heaven method (the Upper Realm's wondrous lands carved one in 296), no Method Stele the ghost path's True Immortal foundation
  (the Nine Nether Peaks carved one in 777), no cultivator drops, sells or teaches one, and a demon's blood beyond Entering Nirvana's
  (a cultivator's, a slain demon player's) leaves Entering Nirvana's. A stele carved with one before stays unreadable. They are kept for
  the Immortal Realm.
- Left as they are: spell cooldowns off by default (the user: "Leave it off for now"), Book of Life and Death's execute ("fine for now").
  For when the Immortal Realm is made: Entering Nirvana cultivators gathered about the way into it, waiting for a chance to ascend (the
  user's plan; no Entering Nirvana or True Immortal cultivator is born in the worlds until then).

Tests: `SuggestionPicksGameTests` (its wounds' test in a batch of its own); the realms' hardy husks in the spell tests are now more than
three times their caster's health, great beasts to them as every hardy husk was before.

## The spell overhaul (2026-10-25)

Two passes over the spells, from the user's lists (the first: "polish and quality over quantity ... spells need weight ... reuse the mod's
ghosts"; the second, after playing it: what still looked wrong, and "look over and polish all the skills again old and new").

### Switches

- **Spell grief** (the gamerule `spellGriefing`, on by default; a **Grief: On/Off** button at the foot of the cultivation menu's Spells tab,
  that an operator or the world's host may turn, every player told). Off, a player's spells leave the land as it was: no craters, cuts,
  fissures, lakes or ice for good, their fire only a moment's (it catches nothing and puts itself out), the water they pour draining away.
  What a spell sets down for a while (walls, trees, ice, cages) comes and goes either way. Meteor Flight keeps its own `meteorFlightSmashes`.
- **Life Force's mode** (Life Force Drain and Gain; a switch beside them in the Spells tab, kept): what they move between a being and their
  caster: its **health** (as before), its **qi** (another player's pool, or a being's life burnt into qi; given, the caster's own qi), or its
  **years** of life (a player's only). A herb or a fruit is drawn on, or given, its years whatever the mode. The stream runs green, blue or gold.

### Masteries

From the 19th layer of a method (Core Formation's last), its element is mastered (`HeavenPassives#masters`): **fire's** own fire (the flames its
spells set, the land they scorch, the burning they leave) does its caster no harm; **water's** caster walks on water (sneak to go in), takes no
harm falling onto it, swims as anyone under it, and is borne up onto it as their head nears its face.

### What spells are made of

- **Of the world's own blocks** (`SpellConstruct.Form.BLOCKS`: laid out block by block, the same on every screen, meshed off the render thread
  as the land is, drawn moving; `BlockMasses`, `EarthMasses`, `SpellMasses`): Falling Continent's land; Falling Mountain's peak; Buzhou's
  pillar; the Seal of Ten Thousand Mountains; Gravestone Falls' stele and tortoise; the Nine Cauldrons; the Buried World's standing stones;
  Xirang's clod; the Earth Veins' dome; the Hand of Houtu (its fingers curling joint by joint); Seven-Treasure Bodhi Branch's tree; the
  falling ice moon and the crescent over the well (each shattering into pieces); Blazing Meteor's rock; the Soul-Locking Pagoda; Fengdu's
  walls; the Naihe Bridge; the World Tree; the earth Xirang heaves up round those it buries.
- **Real blocks set down for a while:** the frost Glacier of the First Age leaves on its way (snow over the land, ice over its water), and
  the land a spell chars or withers with the spells' griefing off (`HeavenSpells#stainTemporary`), each given back as it was after a while.
- **Drawn by hand** (`Form.DRAWN`: `SpellDrawn`, `SoulVortex`, `ImpermanenceFigures`): the Heaven-Demon Eclipse (sun and moon as discs from any
  side, its beam poured from the moon's face as wide as its harm reaches and down to the ground), Ten Thousand Souls Ascend (Minecraft's own
  soul faces up a whirlwind of ghost fire), the two Impermanences (painted figures, `models/figure/*.json`, `tools/spell-tex/figures.py`),
  the Sea-Fixing Pearls and their columns of sea water, the Ocean-Sealing Vase's streams and whirlpool, the Nine Suns, the sun pulled through
  the void, Glacier of the First Age's surge of crystals, the Primordial Sea's waves and storm, Xirang's soil growing out over the land.
- **The mod's ghosts** (`Form.GHOSTS`: `SpellGhosts`, the spirit and soul shaders): Soul Dash's afterimages and Ghostly Wail's giant (the
  caster's own ghost), the Procession, the Nine Nethers, and the Heaven-Demon Avatar: a colossal projection of the caster's own body, gold on
  its side of heaven and crimson on the demon's, whose own arm comes down out of the sky for each strike.
- **GeckoLib, where a model serves** (creatures, vessels, weapons): the Azure Dragon now flies off along a path worked out alike on both sides
  (`DragonFlight`), its twenty body segments laid along where its head has been each frame (`DragonPose`), rather than turned about whole.
  Every modelled shape is drawn once unseen as a world is joined (`SpellConstructRenderer#warm`), so none is first made as its spell comes.

### Feel

- **Sounds** a lingering spell plays are its own voices (`SpellSounds`, `ClientSpellSounds`): they follow what they belong to and fade out as
  the spell ends, never playing on after it.
- **Shapes worn on a body** (a tortoise's shell, a mirror, a domain) are drawn where the body is drawn each frame, never a step behind it.
- **Ground-running spells set off from the ground under their caster** however high they fly (`HeavenSpells#footing`).
- **Ghost Fire** clothes the forearms and hands in moving blue flame (Flame Armour's fire, cold), in first person too.
- **A lake's water** is poured into its crater over a while (`Devastation` job rate), not thousands of sources in one tick.
- **Meteor Flight's bulldozer** clears the way on its flier's screen only once the server, going by what it sees, will take it too, and
  never again a block the server put back (`MeteorFlightClient#watchTheWay`): set off at full tilt from a jump, the way was cleared, put back
  on the flier, and cleared again, over and over.

### New

- **Return to the Bough** (wood, Four Axis, 400 qi): the Cultivation Fruit held fly up into the tree looked at and hang there again under its
  leaves (six at most), each as old as when it was picked: on the bough a fruit ripens on with the years again.
- **Seed of Withering Rebirth** is shot, plants only on the ground, and seals a being it strikes in a pod of roots and blossom.

### The third pass (2026-10-26)

From the user's list after playing the second (with screenshots), and "a lot of the horns ... are angled inward":

- **Fixes.** A spell's missile cast at a being closer than it forms (Wood Bullet point-blank, and every missile alike) strikes it: one that
  formed inside or past a being flew out of it unseen (`SpellProjectile#firstIn`). Boulder Throw works with the spells' griefing off: the
  boulder is torn up out of ground left whole. The Gate of Ten Thousand Ghosts is closed between its pillars over the doors (the sky showed
  through either side of its plaque). Falling Mountain's purple mist is gone (the Slowness it lays on all under it, given anew each tick, was
  shown in swirls), and it falls from 100 blocks over 64 ticks (46 over 34), no faster. Earth Dragon Surge's spikes are closed spires of rock
  faced at the rock's own scale (they were cones missing a sliver between every two faces, their texture smeared).
- **Smoothness.** A missile on a watcher's screen goes where the server says a tick behind it, never flying on by itself between (it leapt back
  and forth every tick), and its trail is laid from where it is drawn: Fire Serpent's body no longer jerks, and its fire flows along it.
- **No more seeing through suns.** A sun's body is solid light (`SpellRender#core`, drawn first, writing depth): what is behind it or in it is
  hidden by it, not laid over it (Solar Field, the Nine Suns; the eclipse's sun stays see-through, its moon passing before it).
- **Remade.** Sun Pulled Through the Void is a sun of the sky as Solar Field's is, vast: boiling, its corona and rays wheeling, prominences
  looping off it, a shaft of heat poured onto the land under it with the fire's seal burning there, collapsing white-hot. The World Tree is
  built of the world's own wood and leaves: a fluted trunk, buttress roots running out over the land, eight great boughs and three leaders
  crowned in leaves, glow-berry vines hanging. Glacier of the First Age surges out as rows of colossal faceted crystals of ice bursting up as
  its front comes, frosting the land it passes, and shatters at its end. The Primordial Sea's waves curl in, its sea churns round a whirlpool
  under a storm, lightning striking into it, rain over all of it and heavy all round one who stands within. Xirang shows what it does: its
  clod sinks into the land, roots of gold and shoots run out from it to its edge, and the earth heaves up round each being it buries.
- **Polished.** The Sea-Fixing Pearls are true pearls in a skin of sea with currents wheeling round them (two squares of water turned on
  each before). Flame Armour: a white-hot skin under its flame, its fire flowing up it unbroken, a cape of fire, a crest, flames round the
  feet, embers; on one's own arm in first person too. Tidal Lance: a whirlpool at the hands, surges running down the torrent, strands
  twisting round it, spray, a crown of water bursting off what it strikes. Torrential Spring's seals wheeling round its ball are rings of
  water; the water seals on Rain Call and Heavenly River Torrent are faint. Black and White Impermanence rise turning out of a whirl of the
  underworld, float and look about, lunge as they strike, haul on the chain, tear the soul away, spirit paper and ghost fire about them.
- **The land charred and withered by a gradient.** Fire leaves blackstone and basalt at its heart, burnt earth about it, dry and browned at its
  edge (`Devastation#charred`); withering leaves coarse and rooted earth and a little mud at its heart, podzol and some grass at its edge
  (`Devastation#withered`). With the spells' griefing off both are only for a while (`Devastation#charFor`, `#witherFor`): the land given back
  as it was after about 18 s (36 s at the heart). Even small fire spells (Flame Bolt, Fire Serpent, a yang bomb) leave a scorch mark for a while.
- **After playing it (the same day).** Glacier of the First Age follows the land as it goes, down off a cliff and over a river (its mist, its
  reach and its crystals all kept to the height it rose at), and freezes the land the whole way as its end did (`Devastation#freezeAt`: water to
  ice, packed toward its middle, snow over the land, deepest along its middle; for good with the spells' griefing on, else thawing). The
  Sea-Fixing Pearls seek only foes before their caster, those nearest their aim first (they took the nearest all round, and the first flew
  off behind them). The Gate of Ten Thousand Ghosts' dark doorway stands within its frame, tucked into its pillars and up into its frieze (set
  a little behind them, the sky showed through above it). The World Tree, which was never made (its trunk's heights one too few), grows.
- **Horns.** The Lightning Antelope's horns, the Azure Dragon's antlers and the dragon race's lean out (a bone's z turn outward is `side * +`,
  as the ears'; they leant in and crossed); Ox-Head's horns curve up.

Previews (no game needed): `run/previews/animals` (the beasts' gaits), `run/previews/figures` (the two Impermanences turning and striking).

## The overhauls (2026-10-27)

The user's batch after the fourth bug sweep:

- **Game tests in the mod's own worlds.** The test server is given the mod's dimensions as a played world has them (the Great Void,
  the Immortal Realm, the Netherworld, the Spatial Gap and the rest: `test/mixin/GameTestServerMixin`, gametest source set only), so
  tests can go there; `OverhaulsGameTests` does (a battlefield found in the Great Void, a tear into the gap, a small world entered).
- **The spells' land kept across a crash** (`HeavenSpells.Land`). Every block a spell sets for a while (walls, ice, tame fire, the water
  poured, the charred and frozen ground) is kept in its level's own record as it is set and struck off as it is taken up, written out
  every 10 seconds while it changes. A server stopping takes them all up as before; one that crashed or was killed left them standing,
  and they are taken up as it next starts (only where the spell's own block still stands).
- **The Great Void's battlefields** have craters and ruin in place of the giant swords, and **ghostly qi x100** with its cyan yin
  motes under Qi Sense (The Great Void Realm; Under Qi Sense).
- **The Immortal Realm's qi motes** are ten times the Great Void's, as its qi is (Under Qi Sense).
- **The camp rushes the Gate**, and **the Gate's light** is golden (The Immortal Realm).
- **Titles** from Void Shattering on (The realms after Four Axis), and **what cultivators call you**, kneeling from four realms beneath
  you, with many more lines in passing and in talk (Sects and cultivators: Everyday life, Talking to cultivators).

### The evening's fixes (2026-10-27)

From the user's play at Heavenly Lord:

- **The ghostly qi pool grows with the ghost path's realm**, and fills as much faster (Ghostly qi, under The Netherworld).
- **King of the Dead** says what it asks: to be among the dead with a ghost path of Core Formation or higher (a living cultivator's ghost
  path gives the Ghost King's form, not the crown).
- **The Cultivation tab's bars** shorten their numbers past a million when the whole figures won't fit ("174.14T / 56.29Q"), and draw
  the text smaller if even that won't (`CultivationScreen#big`, `#barText`): a Heavenly Lord's seventeen figures ran off the column.
- **Release pressure** lets a cultivator stand for its five minutes even four realms or more beneath you (`TalkSystem#kneelsTo`,
  `RealmSuppressSystem#spares`): the Patriarch went straight back down on one knee.
- **A Holy Land's walkers go round the Hall of the Great Dao** (`HolyLandPlan#ways`): its ways joined the front of the grounds to the
  Patriarch's Palace only down the axis, through the hall, which has no back door, so the Patriarch (on its way to its seat in the
  palace) ran round in circles before it. Walkers now go by way points down the lawns either side of the hall, and none inside it;
  worked out from the core and the seat, so the Holy Lands already founded have them too. More way points lead over the moat's
  bridge and up the palace's terraces to its door and in to the seat. The palace's first stair stood two blocks above the lawn, too
  high to step; a Patriarch now sets one more step beneath it, once, when it goes to its seat (`HolyLandPlan#mendPalaceSteps`).
  Checked in game: at the Holy Land founded earlier and at a fresh one, the Patriarch sits on its seat.
- **Sect members sit at their posts again** (the user: "none of the NPCs are cultivating"; `CultivatorGoals.SectDutyGoal`). The
  duty's stuck check counted its walk in the same clock as its stay at the post, and a seat's stay (1,200 ticks for an elder, 2,400
  and more for a disciple's mat) was already past the 900-tick limit at the first step. So every seat not already reached was dropped
  at once and chosen again the next tick, with a path planned each time (much of a great sect's server time). An Inner Disciple,
  given a different mat each time, zigzagged and never sat; only short strolls survived. The walk now has its own count.
  - A spot to stand at (a stroll's, a patrol's) is reached standing there on whatever ground is there: a Holy Land's spots round its
    plaza were set at the plaza's floor, three blocks over the lawn, and are now set on the lawn. A seat is reached standing at it
    within a block and a half of its height, and a post over open air is taken down to the mat (or the ground) under it: some of a
    Holy Land's elders' mats are set a block too high.
  - A walker's step toward a goal near it (within 64 blocks) no longer lands on a building's roof: the ground there well above both
    the walker and the goal is a roof, and the step is set on the way between their heights instead, so the path is looked for in at
    the door. A Holy Land's disciples planned for their meditation halls' roofs and stood before them for good.
  - **The land meets every sect and city building's paving** (`SectBeard`, through `NoiseBasedChunkGeneratorMixin`). Their structures
    shape the land round each building (`beard_thin`), and vanilla's Beardifier draws the land up to a piece's floor only for a
    village's pieces; every other piece's land it left a block under its floor. A building's floor is its paving, so the land lay a
    block under every door. A meditation hall's one step stood a block and a half over the lawn (two and a half at one), and the
    palace's first stair two: no walker could climb in. Land generated now is drawn up to the paving, as the Immortal Realm's capital
    already had it. In a Holy Land founded before, the Patriarch lays the missing steps before the four meditation halls' doors as it
    does the palace's (`HolyLandPlan#mendPalaceSteps`).
    The same drop let the side courts' pools spill: in a Holy Land generated before, some 400 blocks of water flood its courts and stand
    on the footpaths beside them; one generated now keeps its water in its pools.
  - **No lake is dug on a sect's grounds** (water or lava; `SectClearingMixin`, as trees and springs already were not): dug before the
    sect is built, a Void Steppes or Dao Field of Water basin, or the Dao Field of Fire's lava, stood through the grounds with the ways
    laid over it as causeways.
  - A seat is reached from a block away (paths are planned to within a block of their end): a Holy Land's disciples, in their halls at
    last, stood beside their mats for good. Checked in game: at a Holy Land founded before and at two fresh ones, all six Inner
    Disciples sit on their mats, the elders on theirs and the Patriarch on its seat.
  - **A Holy Land's great round plaza lies level** with its dais (`SectPiece#plazaLevel`): 26 across round the dais's own 12, it was laid
    on the land as it lies, stepping down in rings, and at one sank eight deep in a hollow before the great hall. Now it is level with
    the dais's paving out to 20 from the core, then eases a block a step to its rim, filled with its stone under it or cut down to it
    (no more than 12: past a cliff it follows the land).
  - **The sects' trees stand three clear of the ways** (`SectLayout#plant`, two before): as far as a crown spreads, so none hangs over a
    way (a mangrove's bough hung low over a Holy Land's Spirit Way).
  - The Formation Core, the Heaven Altar, the Spirit Pedestal and the steles are no longer taken for open ground by walkers' paths
    (not whole blocks, vanilla let paths cross them): an Inner Disciple whose path led across its sect's core pushed against it for
    good.
- **Strike one of the Shore of Nirvana's camp and the whole camp answers** (`NirvanaCamp#rally`; the user: "if you attack one of the
  EN campers they all should retaliate"): every camper about the camp takes the striker for its foe, as if struck itself, and goes for
  them if it fights no one else. Not those flying for the Gate, and a quarrel between two of the camp stays theirs.
- **From Entering Nirvana up, cultivators stand their ground** (`CultivatorNpc#holdsGround`, `#standsAsImmortal`; the user: they
  "shouldnt run from people with opposing alignment or higher strength than them"): no stronger domain of the other path sends them
  running, as it does those below. Badly hurt, with their foe on them, they still back away.
- **The Immortal Realm is above the quarrel of the two paths** (`CultivatorNpc#abovePathQuarrels`; the user: "make everyone in the
  immortal realm not attack on sight actually, they treat those righteous vs devilish quarrels as beneath them"): there, the righteous
  and the devilish are not each other's enemies. Nobody goes for you on sight for your path, nobody runs from you for it, and they talk
  and trade with you whatever it is (`TalkSystem#isPathEnemy`). A grudge of its own still counts, and struck, it fights back.
- **Long tooltips wrap** (`TooltipWrap`): the mod's items' lines wider than 220 GUI pixels break onto lines of their own, keeping
  their colours (the Court Merit Token's ran off the screen).
- **Lag at the great sects** (the user: "entering holy lands and stuff causes massive lag spikes"; found by sampling the server's and the
  client's threads in game, slow ticks of 1 to 2.6 seconds):
  - A Holy Land's, a Demon Palace's or an immortal sect's barrier (a radius up to 136 or 176) asks some 232 (or 388) qi a second, and
    its plaza's eight veins give 64: it ran its battery dry in seconds, fell all at once (some 230,000 blocks cleared in one tick, every
    client near taking them in too) and rose again, over and over. Now a sect's people make up what its barrier asks beyond its eight
    veins, with 32 a second over to mend it (`FormationCoreBlockEntity#supply`, `SECT_SURPLUS`); a lesser sect's barrier is its veins'
    alone as before, and a great sect's veins mined, it runs short as before.
  - Barriers (and walkers' looks at the ground ahead, a patrol's round, a seclusion site, a Holy Land's way points) touch only land
    loaded whole (`Formations#ready`): `Level#hasChunkAt` says yes as soon as land is meant to load, and reading a block there held the
    whole server until it was generated.
  - A barrier rises at 1,024 cells a tick (was 2,048), a Holy Land's in some eleven seconds. A cell that held something (leaves, a
    plant, water) costs 8 of those (`Formations#HEAVY_CELL`): its neighbours hear of it and what was there drops.
  - A barrier cell set in open air (or cleared back to it) no longer asks its neighbours to change their shape: looking at a
    neighbour across the edge of a chunk still loading held the server until that chunk was done. A cell is also set only when the
    land on every side of it is loaded whole. Checked in game: at the Holy Land founded earlier, none of the server's slow ticks were
    the barrier's.
  - What is left at 32 chunks of view and simulation distance is mostly Minecraft's own: saving and generating chunks.

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
