#!/bin/sh
set -eu
portable_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$portable_root"
if [ -d "$portable_root/TrackDiagramGenerator.app" ]; then
    exec "$portable_root/TrackDiagramGenerator.app/Contents/MacOS/TrackDiagramGenerator" "$@"
fi
exec java -jar "$portable_root/TrackDiagramGenerator.jar" "$@"
