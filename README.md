# Defying The Heavens

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

### Windows launcher

Double-click **Play Defying The Heavens.exe** in this folder to build and launch
Minecraft with the mod. The first launch downloads a local Java runtime and the
development dependencies; later launches reuse them. Keep the EXE beside
`gradlew.bat` and the `launcher` folder.

To create the EXE after cloning the repository, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\launcher\Build-Launcher.ps1
```

See [launcher/README.md](launcher/README.md) for logs, saved worlds, and build details.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
