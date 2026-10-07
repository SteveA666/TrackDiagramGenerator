# Track Diagram Generator

A Java Swing editor for creating and drawing railway track diagrams. Its scope is diagram layout: nodes, track connections, stations, platforms, and custom text. It does not simulate trains, services, or timetables.

**Work in progress:** the interactive track editor is usable, but saving and loading are not implemented. Changes exist only in memory and are lost when the application closes.

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
- Enable or disable snapping to a 20-unit grid.
- Pan, zoom around the pointer, fit the diagram, and reset the view.
- Draw U-shaped stub-end markers oriented to their connected tracks.
- Display validation feedback in the status bar and optional debug-node circles.

Stations are drawn as a square marker with their name. Platforms are filled, numbered surfaces that follow their supporting tracks.

## Run

Install a JDK and make sure `java` and `javac` are available. The current application uses Swing and standard Java libraries; no external dependencies are required.

From the project root in PowerShell:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
$sourceFiles = @((Get-ChildItem src -Recurse -Filter '*.java').FullName)
javac -d bin @sourceFiles
java -cp bin Main
```

Choose **New Network** in the startup dialogue to open an empty diagram. The startup **Open Network** option is not implemented yet. `SampleNetwork` remains available in the source as a small example fixture.

In VS Code, the Java project settings use `src` for sources and `bin` for compiled classes. Run `Main` to launch the application.

## Controls

| Action | Control |
| --- | --- |
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
src/
  Main.java               Application entry point
  Launcher.java           Startup dialogue and window creation
  SampleNetwork.java      Example network fixture
  editor/                 Validated editing operations
  model/                  Network, nodes, tracks, stations, platforms, and custom text
  ui/                     Swing windows, canvas, toolbars, and view transforms
  io/                     Placeholder for diagram input/output
docs/                     Editor and model guides
```

`bin/` contains compiled application classes.

## Remaining work

- Save/load and export, with validation of imported diagrams.
- Undo/redo, node/track deletion tools, and unsaved-change handling.
- Properties editing for existing nodes and tracks.

Several menu actions remain placeholders, including File > New, Open, Save, Save As, Export, and Settings. The working startup New Network action is separate from File > New.

Earlier documentation was fully or partially generated by DeepSeek V4.1 Flash. This README was updated with assistance from Codex.
