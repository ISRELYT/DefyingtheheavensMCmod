# Cultivation menu backup (2026-10-09)

The single-page cultivation menu as it was before the tabbed redesign: realm, stage, qi bar and rate,
bottleneck/suppression status and the stat list on one panel, with Meditate and Breakthrough buttons at the bottom.

- `CultivationScreen.java` is the source of that screen. On 2026-10-09 the qi pool arrived and the progress currency was
  renamed in code (`getQi`/`qiRequired` → `getCultivation`/`cultivationRequired`, `ModLang.QI*` → `ModLang.CULTIVATION*`);
  this copy was updated to those names, nothing else, so it still drops in. It doesn't show the new Max Qi / Qi Gather rows.
- `preview-*.png` show what it looked like in the Upper Realm (Four Axis Mid) and in the Overworld (Heavenly Being Mid).

This folder is outside `src`, so it is not compiled.

## How to revert

1. Copy `CultivationScreen.java` from this folder over
   `src/client/java/com/example/defyingtheheavens/client/CultivationScreen.java`.
2. Build (`gradlew build`). No other file needs to change.

The translation keys the old screen uses (`STATS_HEADER`, `STAT_HEALTH` ... `STAT_KNOCKBACK` and the shared
ones) are still defined in `ModLang`/`ModEnglishLangProvider`. The keys added for the tabbed menu (`TAB_*`,
`STATS_*`, `PERK*`, `METHODS_*`, `SPELLS_*`) can stay; unused translations do no harm.
