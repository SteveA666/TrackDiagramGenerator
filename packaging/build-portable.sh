#!/usr/bin/env bash
set -euo pipefail
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_root"
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
command -v jpackage >/dev/null || { echo 'JDK 26+ with jpackage is required.' >&2; exit 1; }
case "$(uname -s)" in
    Linux) platform=linux ;;
    Darwin) platform=macos ;;
    *) echo 'Run this script on Linux or macOS. Use build-portable.ps1 on Windows.' >&2; exit 1 ;;
esac
case "$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*os.arch = //p')" in
    amd64|x86_64) architecture=x64 ;;
    aarch64|arm64) architecture=arm64 ;;
    *) echo 'Unsupported Java runtime architecture.' >&2; exit 1 ;;
esac
if [[ "${1:-}" != '--skip-build' ]]; then sh ./mvnw clean verify; fi
version=$(sed -n 's/^[[:space:]]*<version>\([^<]*\)<\/version>.*/\1/p' pom.xml | head -n 1)
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo 'Use a numeric x.y.z release version.' >&2; exit 1; }
jar="$project_root/target/track-diagram-generator-$version.jar"
[[ -f "$jar" ]] || { echo "Build first: missing $jar" >&2; exit 1; }
mkdir -p target/portable-build target/releases
stage=$(mktemp -d "$project_root/target/portable-build/build-XXXXXXXX")
mkdir -p "$stage/input" "$stage/distribution/TrackDiagramGenerator"
cp "$jar" "$stage/input/"
jpackage --type app-image --name TrackDiagramGenerator --app-version "$version" \
    --input "$stage/input" --main-jar "$(basename "$jar")" --main-class Main \
    --add-modules java.desktop,java.logging,jdk.unsupported \
    --jlink-options '--strip-debug --no-header-files --no-man-pages' \
    --dest "$stage/native" --description 'Railway track diagram editor'
portable_root="$stage/distribution/TrackDiagramGenerator"
if [[ "$platform" == macos ]]; then
    mv "$stage/native/TrackDiagramGenerator.app" "$portable_root/"
    cp packaging/Launch.command "$portable_root/"
    chmod +x "$portable_root/Launch.command"
else
    cp -R "$stage/native/TrackDiagramGenerator/." "$portable_root/"
fi
touch "$portable_root/.portable"
mkdir -p "$portable_root/saved_diagrams"
cp default.json packaging/PORTABLE.txt "$portable_root/"
javac -cp "$jar" -d "$stage/smoke" packaging/PortableSmoke.java
if [[ "$platform" == macos ]]; then
    app_directory="$portable_root/TrackDiagramGenerator.app/Contents/app"
    runtime_java="$portable_root/TrackDiagramGenerator.app/Contents/runtime/Contents/Home/bin/java"
else
    app_directory="$portable_root/lib/app"
    runtime_java="$portable_root/lib/runtime/bin/java"
fi
"$runtime_java" -Djava.awt.headless=true -cp "$app_directory/$(basename "$jar"):$stage/smoke" PortableSmoke "$portable_root"
archive="$project_root/target/releases/TrackDiagramGenerator-$version-$platform-$architecture.tar.gz"
tar -czf "$archive" -C "$stage/distribution" TrackDiagramGenerator
if [[ "$platform" == macos ]]; then
    (cd target/releases && shasum -a 256 "$(basename "$archive")" > "$(basename "$archive").sha256")
else
    (cd target/releases && sha256sum "$(basename "$archive")" > "$(basename "$archive").sha256")
fi
echo "Created $archive"
echo "Uncompressed app: $portable_root"
