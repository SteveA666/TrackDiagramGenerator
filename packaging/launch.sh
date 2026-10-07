#!/usr/bin/env sh
set -eu
portable_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$portable_root"
exec java -jar "$portable_root/TrackDiagramGenerator.jar" "$@"
