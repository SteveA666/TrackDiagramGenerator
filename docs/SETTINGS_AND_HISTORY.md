# Undo, redo, and preferences

## Undo and redo

Use **Edit > Undo / Ctrl+Z** and **Edit > Redo / Ctrl+Y** (also **Ctrl+Shift+Z**). Menus show the next action and are disabled when no action is available.

History covers node creation/movement, track creation, station creation/movement/renaming, custom text creation/movement/appearance, and platform creation/editing/deletion. A dragged track and its missing endpoints are one operation. An explicitly clicked starting node is its own operation. Continuous Draw records each completed track separately. A completed drag is one move.

Cancelled previews, rejected operations, unchanged values, selection, navigation, and preferences do not add history entries. A new edit after Undo clears redo; rejected and unchanged edits preserve it. Undo/Redo cancels pending gestures and clears selections, while preserving the current tool, zoom, and pan. Restoration rebuilds registered objects; code retaining model objects outside the editor should look them up again by ID afterward.

Saving keeps history. The unsaved marker compares actual contents with the last successful save: undoing back to those contents clears the marker, and undoing past them marks the document unsaved again. New and successful Open start fresh histories. Failed or cancelled opens preserve history. History is in memory and is not written to diagram files.

The default limit is 100 edits, configurable from 1 to 1000. Lowering it discards oldest undo entries first, then farthest redo entries if necessary. Snapshots preserve diagram order, stable IDs, graph references, and platform definitions.

## Settings

Open **Edit > Settings...** or **View > Settings**.

| Tab | Preferences |
| --- | --- |
| Window | Width, height, and maximized startup; dimensions apply when saved and on next launch |
| Editing | Default snapping, grid spacing (1–200 units), Continuous Draw, default node/track types, undo limit |
| Appearance | Grid visibility independent of snapping, debug circles, smoothing, background and grid colours |
| Navigation | Wheel zoom step (1–100%), inverted wheel direction, fit on open |
| New objects | Text font/size/colour; platform start/end percentages and offset |
| Saving | Previous-save `.bak` backup and Save As overwrite confirmation |

**Save settings** validates and persists the complete draft, then applies it. **Cancel** preserves active and saved preferences. **Reset defaults** reads `default.json` into the draft; save to apply it. Invalid input and failed writes keep the page open. Applying settings cancels pending gestures while preserving committed objects, the view, and diagram dirty state. Object defaults affect new objects only. Lowering the undo limit may discard history.

Preferences live in `settings.json` in the working directory, separately from diagrams and ignored by Git. `default.json` is the tracked default configuration, also bundled into the executable JAR. The app reads a working-directory `default.json` when present, otherwise the bundled copy. Missing user files and fields inherit those defaults. Reset Defaults never overwrites `default.json`; saving the reset draft updates `settings.json`. Malformed files produce an error and are not overwritten. The diagram folder remains `saved_diagrams/`.

The Window tab accepts widths of 800–7680 and heights of 400–4320 in Swing's logical pixels. Oversized windows are constrained to the current monitor's usable area, excluding taskbars. **Open maximized** applies after saving and on future launches. Ordinary manual resizing remains available; it does not automatically change the saved dimensions. Changing unrelated settings preserves the current window size and position.

Toolbar changes affect the current canvas. Settings controls the persisted defaults used for a fresh New/Open canvas. The settings page displays those defaults; saving it also applies them to the current canvas.

When backups are enabled, replacing a diagram first copies its previous contents to `filename.json.bak`. The first save creates no backup; later replacements update it. Backups are not listed in Open. To restore one, copy or rename it to a `.json` filename in `saved_diagrams/`. If a backup cannot be written, saving aborts and preserves the original file. Backups are off by default; normal saves still use temporary-file replacement.

## Verification

Run `.\mvnw.cmd verify`. Tests cover all editor operations through undo/redo, atomic track gestures, continuous chains, branching, limits, saved-state transitions, failed opens, canvas gestures, menus, settings persistence/validation/defaults, toolbar and zoom behavior, platform defaults, and backup failure handling, along with existing regression suites.
