# Track Diagram Generator

A Java Swing editor for creating and drawing railway track diagrams. Its scope is diagram layout: nodes, track connections, stations, and platforms.

**Work in progress:** the interactive track editor is usable, but saving and loading are not implemented. Changes exist only in memory and are lost when the application closes.

## Features

- Create regular nodes and stub ends, with automatically assigned IDs.
- Select and drag nodes, with connected tracks following a preview before the move is committed.
- Create mainline, station, and siding tracks by dragging or clicking two existing nodes.
- Create missing endpoints when drawing a track into empty space, and reuse existing endpoints.
- Enable or disable snapping to a 20-unit grid.
- Pan, zoom around the pointer, fit the diagram, and reset the view.
- Draw U-shaped stub-end markers oriented to their connected tracks.
- Display validation feedback in the status bar and optional debug-node circles.

The model also supports stations and positioned side/island platforms. Station and platform editing controls and rendering are not implemented yet.

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
| Select or move a node | **Select / Move**, then click or drag a blue handle |
| Add a node | **N** or **Add Node**, then click empty space |
| Add a track | **T** or **Add Track**, then drag between endpoints or click two existing nodes |
| Choose a new object's type | **Node** and **Track** choices in the toolbar |
| Toggle grid snapping | **Snap to grid** |
| Pan | **Pan** tool with left-button drag, or middle-button drag from any tool |
| Zoom at the pointer | Mouse wheel |
| Zoom at the canvas centre | **+** and **-** buttons |
| Fit the diagram | **Fit** |
| Reset zoom and pan | **100%** |
| Cancel a pending edit | **Escape** or right click |
| Toggle debug circles | **View > Toggle Debug Nodes** |

Track drags can begin and end on existing nodes or empty space. Missing nodes are created only when the complete track passes validation. A click in empty space without a drag creates nothing in Add Track mode.

The zoom range is 10% to 800%. Node handles keep a constant screen size, and snapping uses diagram coordinates regardless of the view. Blue handles remain visible when black debug circles are hidden.

Switching tools, changing snapping, zooming, or starting a pan cancels pending model edits. N and T also update the toolbar selection and do not intercept typing in text fields.

See [the editor guide](docs/EDITOR.md) for detailed behaviour.

## Validation and model integrity

`DiagramEditor` centralizes canvas edits independently of Swing. It allocates unused positive IDs, checks node references and types, and rejects self-connections and zero-length track geometry. Rejected edits leave the diagram unchanged, including any endpoints that a track drag would have created.

`Network` maintains connectivity when segment endpoints change and rejects endpoint objects that merely share an ID with a registered node. Registered stations validate platform ownership, numbering, placement, and track references. Removing a track also removes dependent platforms; an island platform is removed if either supporting track is deleted.

Parallel tracks and overlapping unconnected nodes are allowed. Parallel tracks with identical endpoints overlap in the current straight-line renderer, so the editor reports that layout issue without rejecting the connection.

See [the model guide](docs/MODEL_UPDATE.md) for model APIs, platform placement, and deletion policies.

## Tests

From the project root:

```powershell
./tests/run-tests.ps1
```

The script compiles the application and tests into `work/model-tests`, then runs four dependency-free test suites in headless mode:

| Suite | Coverage |
| --- | --- |
| `ModelTests` | Connectivity, ownership, stations, platforms, validation, and deletion cleanup |
| `EditorTests` | ID allocation, editing rules, and rejected-edit consistency |
| `CanvasTests` | Mouse interactions, shortcuts, toolbar synchronization, previews, and cancellation |
| `NavigationTests` | View transforms, pan/zoom, track dragging, endpoint reuse, and stub-marker orientation |

## Project structure

```text
src/
  Main.java               Application entry point
  Launcher.java           Startup dialogue and window creation
  SampleNetwork.java      Example network fixture
  editor/                 Validated editing operations
  model/                  Network, nodes, tracks, stations, and platforms
  ui/                     Swing windows, canvas, toolbars, and view transforms
  io/                     Placeholder for diagram input/output
tests/                    Regression suites and PowerShell test runner
docs/                     Editor and model guides
```

`bin/` contains compiled application classes. `work/model-tests/` contains generated test classes.

## Remaining work

- Save/load and export, with validation of imported diagrams.
- Undo/redo, deletion tools, and unsaved-change handling.
- Station/platform rendering and editing controls.
- Properties editing for existing objects.

Several menu actions remain placeholders, including File > New, Open, Save, Save As, Export, and Settings. The working startup New Network action is separate from File > New.

Earlier documentation was fully or partially generated by DeepSeek V4.1 Flash. This README was updated with assistance from Codex.
