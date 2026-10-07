# Windows MSI installer

The MSI installs Track Diagram Generator and its bundled Java runtime for the current Windows user. Java does not need to be installed separately. Start Menu and optional desktop shortcuts launch the application; Windows Settings > Apps provides uninstall support.

The default application location is `%LOCALAPPDATA%\TrackDiagramGeneratorApp`. The directory chooser permits another installation directory. User data always lives separately under `%LOCALAPPDATA%\TrackDiagramGenerator`:

- `settings.json`, created when settings are saved.
- `saved_diagrams/`, containing JSON diagrams.
- An optional `default.json` override; otherwise reset defaults come from the bundled resource.

The installer does not package or remove user data. To migrate from a portable folder, copy its `settings.json` and `saved_diagrams/` into this data directory. Copy a customized `default.json` too if desired. Keep a backup of diagrams.

## Build

From the project root in PowerShell with JDK 26+:

```powershell
.\packaging\build-msi.ps1 -JdkHome 'C:\Program Files\Java\jdk-26.0.2'
```

The script verifies the application with Maven, builds a native application image in installed mode, checks its runtime headlessly, and packages it as an MSI. It downloads the official [WiX v3.14.1 build tools](https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm) into ignored `work/tools/` if they are missing. You can supply existing WiX 3 binaries with `-WixDirectory`. `-SkipBuild` reuses the current application JAR and should only be used after verifying current source.

The output is `target/releases/TrackDiagramGenerator-1.0.0-windows-x64.msi` (architecture is taken from the selected JDK), with a SHA-256 checksum beside it. The MSI includes its matching architecture's runtime. The installer is unsigned.

The Windows job in the **Portable applications** GitHub Actions workflow also builds this MSI and uploads it alongside the portable ZIPs.

The fixed `--win-upgrade-uuid` in the build script identifies the application across versions. Keep that UUID when releasing higher versions; update the numeric version in `pom.xml` and the About dialog. MSI packages must be built on Windows. See [Oracle's jpackage options](https://docs.oracle.com/en/java/javase/26/docs/specs/man/jpackage.html).

Build verification checks JSON, settings, fonts, PNG, and JPG using the packaged runtime. A real install, shortcut launch, upgrade, and uninstall should also be tested on a clean Windows machine before public distribution.
