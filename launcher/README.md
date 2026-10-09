# Windows development launcher

Double-click **Play Defying The Heavens.exe** in the repository root. It runs this
checkout's `gradlew.bat runClient`, the same development client used from a terminal.
Keep the EXE beside `gradlew.bat` and this `launcher` folder. It is a launcher for
this project; keep the repository with it.

The first run downloads Eclipse Temurin JDK 25.0.4.1+1, verifies its SHA-256, and
lets Gradle download Minecraft, Fabric, and the build dependencies. Internet access
and several GB of free space are needed for the first run. Subsequent launches
reuse the cache and compile changed mod sources automatically.

The development player name is saved in `.launcher/player-name.txt` (default:
`Cultivator`), so cultivation and inventory saves keep the same player identity
between launches. This uses Gradle's
development session, just like `runClient`; it does not sign into a Microsoft account.
To save another development name, run `Start-Mod.ps1 -PlayerName YourName` (letters,
numbers, and underscores, up to 16 characters). Changing the name changes the
development player's identity.

Java and Gradle caches stay in `.launcher`. System Java settings are not changed.
JDK 25 matches the repository's CI; the mod still targets Java 17 bytecode.
The Java archive comes from the official [Eclipse Adoptium release](https://github.com/adoptium/temurin25-binaries/releases/tag/jdk-25.0.4.1%2B1).

The window displays setup/game output and saves it under `.launcher/logs`.
Minecraft saves and options live under `run`, including `run/saves` and
`run/logs/latest.log`. Close Minecraft through its menu before closing the launcher.
Closing the launcher during an active session minimizes it so it cannot interrupt
downloads or world saves. This does not affect the ordinary Minecraft Launcher.

## Rebuild the EXE

Run from the repository root in PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\launcher\Build-Launcher.ps1
```

This uses Windows' .NET Framework compiler. The EXE, runtime, logs, and caches are
ignored by Git; the launcher sources are versioned.

## Build without opening Minecraft

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\launcher\Start-Mod.ps1 -Mode Build
```

Only one launcher/build session may run for this checkout at once. If launching
fails, use **Open logs** to inspect the complete error, then **Play again** to retry.
