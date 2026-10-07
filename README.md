# Track Diagram Generator

A Java Swing editor for creating and drawing railway track diagrams. Its scope is diagram layout: nodes, track connections, stations, platforms, and custom text. It does not simulate trains, services, or timetables.

**Work in progress:** diagrams can be edited with undo/redo, saved, and reopened as JSON files in `saved_diagrams/`, and exported as PNG/JPG images. Persistent settings customize the editor.

## Features

- Create regular nodes and stub ends, with automatically assigned IDs.
- Select and drag nodes, with connected tracks following a preview before the move is committed.
- Create mainline, station, and siding tracks by dragging or clicking endpoints.
- Create missing endpoints when drawing a track into empty space, and reuse existing endpoints.
- Place named stations, then select, drag, or double-click to rename them.
- Place multiline custom text with a font, size, and RGB colour; drag or double-click to edit it.
- Create and draw numbered side and island platforms attached to station tracks.
- Double-click platforms to edit their number, tracks, sides, extents, and offsets, or delete them.
- Toggle Continuous Draw to use each completed track endpoint as the next starting point.
- Enable or disable snapping to a configurable grid (20 units by default).
- Pan, zoom around the pointer, fit the diagram, and reset the view.
- Draw U-shaped stub-end markers oriented to their connected tracks.
- Display validation feedback in the status bar and optional debug-node circles.
- Save and reopen complete diagrams as versioned UTF-8 JSON, with unsaved-change prompts.
- Undo and redo committed edits, including complete track gestures and platform changes.
- Customize editing, appearance, navigation, object defaults, and save behavior in Settings.
- Set window dimensions or maximized startup; reset preferences from `default.json`.

Stations are drawn as a square marker with their name. Platforms are filled, numbered surfaces that follow their supporting tracks.

## Build, test, and run

Install JDK 26 or newer and make sure `java` is on PATH (or set `JAVA_HOME` to the JDK directory). The Maven Wrapper pins Maven 3.10.0, so a global Maven installation is optional. The first build downloads Maven and build/test dependencies and requires internet access. Gson handles JSON; tests use JUnit Jupiter. The packaged executable JAR includes its runtime dependencies.

From the project root in PowerShell:

```powershell
.\mvnw.cmd clean verify
java -jar target/track-diagram-generator-0.1.0-SNAPSHOT.jar
```

Run only the tests with `.\mvnw.cmd test` or `./tests/run-tests.ps1`. Model, editor, canvas, JSON persistence, undo/redo, and settings tests run through JUnit in headless mode. Reports are written to `target/surefire-reports/`. `start.bat` builds, tests, and launches the application; `start.bat --build-only` only builds and tests.

Choose **New Network** in the startup dialogue to open an empty diagram, or **Open Network...** to choose a saved JSON diagram. `SampleNetwork` remains available in the source as a small example fixture.

In VS Code, install **Extension Pack for Java** and open this project folder. VS Code imports `pom.xml` automatically; the Maven view uses the wrapper. Run/debug `Main` using the Java extensions, or run the build lifecycle from the Maven view. Source and output paths are managed by Maven. If VS Code still shows the old layout, run **Java: Clean Java Language Server Workspace** from the Command Palette and reload.

## Save and open

Use **File > Save** to name and save a diagram in the project's `saved_diagrams/` folder. **Save As** chooses another name; **Open** lists saved JSON files. The folder is relative to the working directory, so launch from the project root or use `start.bat`. New, Open, and closing prompt to save unsaved edits. Invalid loads leave the current diagram intact. See the [I/O guide](docs/IO.md) for details and the JSON format.

## Controls

| Action | Control |
| --- | --- |
| Undo / Redo | **Edit > Undo / Ctrl+Z**, **Edit > Redo / Ctrl+Y** or **Ctrl+Shift+Z** |
| Preferences | **Edit > Settings...** or **View > Settings** |
| Create or open a diagram | **File > New** / **Ctrl+N**, **File > Open...** / **Ctrl+O** |
| Save or save under another name | **File > Save** / **Ctrl+S**, **File > Save As...** / **Ctrl+Shift+S** |
| Select or move an object | **Select / Move**, then click or drag a node handle, station marker/name, or text |
| Add a station | **S** or **Add Station**, click a position, then enter its name |
| Add a platform | **P** or **Add Platform**, click a track, then choose a station and edge placement |
| Edit or delete a platform | **Select / Move**, then double-click its surface or number |
| Add custom text | **X** or **Custom Text**, click a position, then enter text, font, size, and colour |
| Edit a station or text label | **Select / Move**, then double-click the label |
| Add a node | **N** or **Add Node**, then click empty space |
| Add a track | **T** or **Add Track**, then drag between endpoints or click endpoints (empty space adds nodes) |
| Choose a new object's type | **Node** and **Track** choices in the toolbar |
| Chain tracks | **Continuous Draw**, then click or drag each new endpoint |
| Toggle grid snapping | **Snap to grid** |
| Pan | **Pan** tool with left-button drag, or middle-button drag from any tool |
| Zoom at the pointer | Mouse wheel |
| Zoom at the canvas centre | **+** and **-** buttons |
| Fit the diagram | **Fit** |
| Reset zoom and pan | **100%** |
| Cancel a pending edit | **Escape** or right click |
| Toggle debug circles | **View > Toggle Debug Nodes** |

### Drawing tracks

Choose **T / Add Track** and the desired track type. There are two ways to draw:

- **Click:** click an existing node or empty space for the start, then click the other endpoint. An empty first click creates a starting node immediately; the second click reuses an existing node or creates one with the completed track.
- **Drag:** drag between existing nodes or empty positions. A dashed line previews the track. Missing endpoints are created only when the track passes validation on release.

New endpoints use the selected node type and optional grid snapping. Existing endpoints are reused. Cancelling a connection keeps explicitly clicked starting nodes, while cancelling a drag creates no missing endpoints.

**Continuous Draw** is off by default. Enable it to make every successfully completed track's endpoint the starting point for the next track. Continue by clicking or dragging, and mix the two as needed. Invalid connections leave the last valid starting point available for another attempt.

Escape or right-click ends the chain without deleting completed tracks. Disabling Continuous Draw, switching tools, changing snapping, or changing the view also clears the pending connection. Enabling the toggle preserves an already selected starting point. The preference stays enabled after ending a chain, so the next track begins another chain.

### Stations and platforms

Use **S / Add Station** to place a named station. A station groups its platforms under a stable identity. Dragging its marker/name moves only the station label; its platforms remain attached to their tracks. Double-click the station label to rename it.

To add a platform, first create a station and its supporting tracks. Choose **P / Add Platform**, then click a track to preselect it in the placement dialog. Choose the owning station, an unused positive platform number, and one or two track edges:

- **Side platform:** one edge, drawn as a 12-unit-wide strip extending away from the track.
- **Island platform:** two edges on distinct tracks, joined into one filled surface. Choose the edges facing the space between the tracks.

Each edge has a start/end percentage along its track and a non-negative offset in diagram units. Extents must satisfy `0 <= start < end <= 100`. The side choices use **Left/Right of screen** for mostly vertical tracks and **Above/Below track** for mostly horizontal tracks, regardless of endpoint direction. Diagonal tracks use their dominant axis, with vertical winning a tie. Percentages still follow track start to end.

Select a platform surface or number and double-click to edit its number, track edges, side/island form, extents, or offsets, or choose **Delete platform**. Editing preserves the owning station, and deletion leaves its tracks intact. Cancel leaves the original platform unchanged.

Platforms follow their track endpoints, including node-drag previews; they are not dragged independently. Node moves that would cross or collapse attached platform surfaces are rejected. Fit includes platform outlines and numbers.

### Custom text

Use **X / Custom Text** to place an annotation with multiple lines, a font family, a positive integer font size, and an RGB colour such as `#000000`. Position is the text block's top-left corner in diagram coordinates, and text size scales with zoom.

Select and drag text to reposition it, or double-click to edit its content and appearance. Invalid settings remain in the dialog for correction; Cancel leaves the diagram unchanged. Fit includes the full multiline text bounds and works with label-only diagrams.

### Navigation and cancellation

The zoom range is 10% to 800%. Node handles keep a constant screen size, and snapping uses diagram coordinates regardless of the view. Blue handles remain visible when black debug circles are hidden.

Switching tools, changing snapping, zooming, or starting a pan cancels pending model edits. N, T, S, P, and X also update the toolbar selection and do not intercept typing in text fields. Shift plus the same letter works too.

See [the editor guide](docs/EDITOR.md) for detailed behaviour.

## Validation and model integrity

`DiagramEditor` centralizes canvas edits independently of Swing. It allocates unused positive IDs, checks node references and types, and rejects self-connections and zero-length track geometry. Rejected edits leave the diagram unchanged, including any endpoints that a track drag would have created.

`Network` maintains connectivity when segment endpoints change and rejects endpoint objects that merely share an ID with a registered node. Registered stations validate platform ownership, numbering, placement, and track references. Removing a track also removes dependent platforms; an island platform is removed if either supporting track is deleted.

Parallel tracks and overlapping unconnected nodes are allowed. Parallel tracks with identical endpoints overlap in the current straight-line renderer, so the editor reports that layout issue without rejecting the connection.

See [the model guide](docs/MODEL_UPDATE.md) for model APIs, platform placement, and deletion policies.

## Project structure

```text
src/main/java/
  Main.java               Application entry point
  Launcher.java           Startup dialogue and window creation
  SampleNetwork.java      Example network fixture
  editor/                 Validated editing operations, document state, and undo history
  model/                  Network, nodes, tracks, stations, platforms, and custom text
  ui/                     Swing windows, canvas, toolbars, and view transforms
  io/                     JSON encoding, validation, and the saved-diagram folder
  settings/               Validated preferences and settings-file persistence
src/test/java/            JUnit entry points for the existing check suites
docs/                     Editor and model guides
saved_diagrams/           User-created JSON diagrams (ignored by Git)
default.json              Default preferences, also bundled into the application JAR
settings.json             Saved user preferences (created on save, ignored by Git)
```

`target/` contains compiled classes, test reports, and the executable JAR. Build output is ignored by Git.

## Remaining work

- Additional export formats.
- Node/track deletion tools.
- Properties editing for existing nodes and tracks.

Use **File → Export image...** (**Ctrl+E**) to export the whole diagram as PNG or JPG to a location you choose. Scale controls resolution (200% by default); padding adds a pixel margin. The dialog displays output dimensions. PNG supports transparency; both formats support white or canvas-color backgrounds. JPG quality is adjustable. Export includes tracks, stub ends, stations, platform numbers, and custom text, with no grid, editing handles, selections, or pending gestures. Export does not change the document, history, or current view. Existing files require overwrite confirmation; images above 16,384 pixels per side or 40 million pixels are rejected before allocation.

New, Open, Save, Save As, Export, Exit, Undo/Redo, and Settings are implemented. See the [settings and history guide](docs/SETTINGS_AND_HISTORY.md).

Earlier documentation was fully or partially generated by DeepSeek V4.1 Flash. This README was updated with assistance from Codex.
