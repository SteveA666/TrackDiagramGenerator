# Portable releases

## Package types

| Archive | Launch | Java required on the user's machine |
| --- | --- | --- |
| `TrackDiagramGenerator-1.0.0-windows-x64.zip` | `TrackDiagramGenerator.exe` | No |
| `TrackDiagramGenerator-1.0.0-linux-<arch>.tar.gz` | `./bin/TrackDiagramGenerator` | No |
| `TrackDiagramGenerator-1.0.0-macos-<arch>.tar.gz` | `Launch.command` beside the `.app` | No |
| `TrackDiagramGenerator-1.0.0-universal-jar.zip` | `launch.cmd`, `sh launch.sh`, or `sh Launch.command` | Java 26+ |

Native bundles contain a runtime matched to their build OS and architecture. Build separately on Intel/AMD x64 and ARM64 when distributing both architectures. Linux native builds also depend on the host's desktop libraries and compatible system C library; build on the oldest Linux distribution you intend to support and test there. The universal JAR can use the user's matching Java runtime on either CPU architecture.

Extract the entire archive before running. On Linux/macOS, use `tar -xzf <archive>` to preserve executable permissions. On macOS use `Launch.command` from the enclosing portable folder; it runs the native launcher directly with that folder as the working directory. The app bundle stays beside the portable data, rather than being installed into Applications. Packages are unsigned; macOS may require approval in System Settings > Privacy & Security after the first launch attempt.

For the universal ZIP on macOS, `sh Launch.command` works without executable permission. Double-click launching requires `chmod +x Launch.command` first. Linux can use `sh launch.sh`. Windows uses `launch.cmd`.

## Data and relocation

Every distribution contains a `.portable` marker. The application walks up from its own JAR to find this marker and uses that directory for:

- `settings.json` (created when settings are saved).
- `default.json` (editable reset defaults; bundled defaults are also available).
- `saved_diagrams/` (created and populated as diagrams are saved).
- The initial image-export location (you may choose another destination).

This remains stable when the shell or shortcut has a different working directory. Keep the marker, launcher, runtime, and data together when moving the application. Use a writable folder, including a USB drive; read-only media cannot save portable data. Development and unmarked JAR launches retain the existing working-directory behavior. Advanced launches can explicitly choose a data directory with `java -Dtrackdiagram.home="/path/to/data" -jar ...`.

For an upgrade, extract the new version into a fresh folder and copy your `settings.json` and `saved_diagrams/` from the previous folder. Copy `default.json` too if you customized it. Keep a separate backup of diagrams. Builds package fresh defaults and do not include your existing diagrams or preferences.

## Build locally

Build prerequisites: JDK 26+ including `jpackage`, an internet connection for the first Maven Wrapper build, and a shell for the target OS. Windows also needs `tar.exe` (included in Windows 11). No WiX is required because these are application images, not installers.

Windows:

```powershell
.\packaging\build-portable.ps1 -JdkHome 'C:\Program Files\Java\jdk-26.0.2'
```

If `JAVA_HOME` already points to the JDK, omit `-JdkHome`. Windows produces a native ZIP and a universal JAR ZIP.

Linux or macOS:

```sh
export JAVA_HOME=/path/to/jdk-26
bash packaging/build-portable.sh
```

Each script runs `clean verify`, stages only the executable application JAR, creates an application image with the desktop/logging/unsupported modules and their dependencies, and tests the bundle using its own Java runtime. Smoke tests cover portable paths, settings defaults and persistence, JSON save/open, Swing font rendering, and PNG/JPG encoding and decoding. Outputs are placed in `target/releases/` with SHA-256 checksum files. Build staging remains under ignored `target/portable-build/`.

Use `-SkipBuild` on Windows or `--skip-build` on Unix only after building the current source; these options reuse the existing JAR. Runtime smoke tests still run.

## GitHub builds

The checked-in `.github/workflows/portable-build.yml` builds on Windows, Linux, and macOS with JDK 26. Run **Actions > Portable applications > Run workflow**, or push a version tag such as `v1.0.0`. Each job uploads its archives and checksums as workflow artifacts. The workflow does not publish a GitHub Release automatically.

Before distributing, launch and exercise the native GUI on each target OS: new diagram, save/open, settings, export, and a move to another writable folder. The build's smoke check is headless; it does not replace native GUI testing. Only the Windows native bundle can be built and checked on a Windows host; Linux/macOS native jobs must actually run on those platforms.

Set the numeric release version in `pom.xml`, update the About dialog and documentation, and build from that source. Attach the ZIP/tar.gz and its checksum to a GitHub Release for the matching tag.

The layouts and per-platform build requirement follow [Oracle's jpackage packaging guide](https://docs.oracle.com/en/java/javase/26/jpackage/packaging-overview.html).
