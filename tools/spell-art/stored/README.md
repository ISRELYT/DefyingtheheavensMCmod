# Stored: the spirit beasts as true beasts

The five spirit beasts of the Dao Integration spells as they were first made (2026-10-24): the Azure Dragon (scaled, ivory-clawed),
the Vermilion Bird (a feathered phoenix with peacock eyes), the Qilin (golden-scaled, hooved), the Black Tortoise (a dark-scaled
tortoise and serpent) and the White Tiger (white-furred, striped). The spells now draw their beasts made of their elements
(wood, fire, earth, water, metal); these are kept for later, at the user's word: "keep the current models in storage though for later".

The files here are laid out as the mod's assets (`src/main/resources/assets/defying-the-heavens/`):

- `geo/spell/<id>.geo.json` — the model
- `animations/spell/<id>.animation.json` — its animations (the same as the elemental beast's: the bones are the same)
- `textures/entity/spell/<id>.png` and `<id>_glowmask.png` — its texture and glow

To bring one back, copy its four files into the assets over the elemental one (the ids are the same: `azure_dragon`,
`vermilion_bird`, `qilin`, `black_tortoise`, `white_tiger`). To write them again from their code (`Wood.azureDragonBeast`,
`Fire.vermilionBirdBeast`, `Earth.qilinBeast`, `Water.blackTortoiseBeast`, `Metal.whiteTigerBeast`), from `tools/spell-art`:

```
java SpellArt.java --stored stored [ids...]
```
