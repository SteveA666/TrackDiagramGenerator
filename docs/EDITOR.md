# Interactive diagram editor

The application provides editing and navigation toolbars above the canvas. Choose New Network in the startup dialogue to open an empty diagram.

## Controls

Keyboard shortcuts: **N** selects Add Node; **T** selects Add Track; **S** selects Add Station; **P** selects Add Platform; **X** selects Custom Text. Shift plus the same letter also works. Switching tools cancels pending edits and updates the toolbar selection. Shortcuts do not intercept typing in text fields.

- **Select / Move:** click a blue node handle to select it. Drag to preview a new position, then release to commit it. Tracks follow the preview. Clicking empty space clears the selection.
- **Add Node:** choose Regular or Stub end and click empty space. Identities are assigned automatically. Clicking an existing handle selects that node instead of creating a duplicate.
- **Add Track:** choose Mainline, Station, or Siding. Drag from an existing node or empty space to another node or empty space. A dashed line previews the track; missing endpoints are created together with the track on release. Existing node handles and exact snapped endpoint positions are reused. You can also click endpoints: an empty first click places a starting node, and the next click connects it to an existing or newly created endpoint. Escape or switching tools cancels the connection but retains the explicitly placed starting node. The node type choice applies to new endpoints only.
- **Snap to grid:** enabled by default, with configurable 20-unit spacing. Disable it for exact placement. Grid visibility is independent of snapping. Selecting an off-grid node does not move it; only an actual drag applies snapping.
- **Escape or right click:** cancel a pending connection or drag. Switching tools, changing snapping, or zooming also cancels pending edits. Panning starts by cancelling pending model edits.
- **Pan:** select the Pan tool and drag with the left mouse button, or drag with the middle button from any tool. Panning changes only the view.
- **Zoom:** use the mouse wheel to zoom around the pointer, or the + and - buttons to zoom around the canvas centre. The zoom range is 10% to 800%. The scale label updates automatically.
- **Fit / 100%:** Fit centres the current diagram and chooses a scale within the zoom limits. Very large diagrams may exceed the available view at the minimum scale. 100% resets both zoom and pan. Fit is also safe on an empty diagram.
- **View > Toggle Debug Nodes:** independently controls the original black debug circles. Small blue squares are editing handles and stay visible so endpoints can be selected.

The status bar explains the current action and reports rejected edits in red. Hovering the status bar displays the full message when it is too long for the window.

## Validation

DiagramEditor is independent of Swing. Canvas edits use createNode, moveNode, createTrack, and createTrackBetween; future input forms and import tools can use the same operations.

The editing layer allocates unused positive IDs, validates references and types, and rejects self-connections and tracks whose endpoints have identical positions. Moving a node onto a connected endpoint is rejected before either coordinate is modified. Invalid actions leave the diagram unchanged, and a rejected track destination keeps an existing start available for a corrected second click. Track drags validate geometry, node references, types, and all allocated IDs before adding any endpoints.

Unconnected nodes may overlap, and parallel tracks remain valid. Parallel track creation displays a drawing warning because straight segments with identical endpoints overlap. These layout choices are not treated as corrupt graph data. The model's registration, ownership, and adjacency checks continue to apply beneath the editing layer.

The model permits coincident coordinates for distinct nodes; visible track length is an editor rule, not an additional Network invariant. Use DiagramEditor for visual geometry edits rather than calling Node setters directly.

## Stub end symbols

Stub ends draw a U-shaped buffer marker across the track, with both arms pointing away from the incoming track. The symbol rotates for horizontal, vertical, and diagonal tracks and follows node drag previews. An isolated stub uses an upright marker; a coincident connected endpoint is skipped when finding a direction. If several tracks are connected to a stub, the first nonzero connected segment determines orientation. Blue editing handles remain separate from the black symbol.

## Scope

The editor supports node and track creation, station and custom text labels, side and island platform editing, view navigation, JSON saving/loading, undo/redo, and persistent settings. Node/track deletion tools remain future work. Use File > Save to preserve edits in `saved_diagrams/`; see the [I/O guide](IO.md) and [settings and history guide](SETTINGS_AND_HISTORY.md).

## Tests

Run `.\mvnw.cmd test` (or `./tests/run-tests.ps1`) with JDK 26 or newer installed. It compiles the entire application and runs model checks, editing validation checks, and real Swing mouse/toolbar interactions in headless mode. It checks drag preview versus commit, cancellation, snapping, invalid-edit atomicity, ID allocation, view transforms, pointer-anchored zoom, panning, track gestures, endpoint reuse, oriented stub rendering, and rendering without a window.

## Stations and custom text

The paint toolbar includes **Add Station** and **Custom Text**. Click a diagram position to open the corresponding dialog. Stations require a nonblank name. Custom text supports multiple lines, a font family, a positive integer size in diagram units, and a colour in `#RRGGBB` format. Invalid text settings keep the dialog open for correction; Cancel creates nothing.

Use **Select / Move** to select or drag a station marker/name or any line of custom text. Double-click to rename a station or edit text and its appearance. Movement preserves the grab offset, follows grid snapping, and previews until release. Escape, right-click, tool changes, and view changes discard pending moves. Overlapping labels are selected in drawing order, with custom text above stations and nodes.

Stations store their own diagram position and retain their platform associations when moved or renamed. Their marker and name do not move connected tracks or platforms. Platform placement and editing are described below. Fit includes the complete station and text bounds, including diagrams containing only labels.

## Platforms

1. Create a station and at least one track.
2. Choose **Add Platform** and click a track. The hovered track is highlighted, and clicking preselects it in the placement dialog. Clicking empty space also opens the dialog with track choices.
3. Choose the station and an unused positive platform number. Track choices show their start/end coordinates so you can identify them.
4. For a side platform, choose one track edge. For an island, enable **Island platform** and configure two different track edges.
5. Each edge has a screen-side choice, Start/End percentages (`0 <= start < end <= 100`), and a non-negative perpendicular offset in diagram units. For mostly vertical tracks, choose Left of screen or Right of screen. For mostly horizontal tracks, choose Above track or Below track. These choices work regardless of endpoint direction; only the percentages follow track start to end. Diagonal tracks use their dominant axis (vertical on a tie). Defaults cover the middle 60% of the track with an 8-unit offset.
6. Create the platform. Invalid input stays in the dialog for correction; Cancel creates nothing.

A side platform is a 12-unit-wide strip extending away from its track edge. An island is a filled quadrilateral between its two configured edges. Choose sides facing the space between the tracks. Opposite track directions are handled when joining the ends. Crossed, collapsed, or touching opposite edges are rejected by the drawing editor.

Use **Select / Move** to select a platform surface or number. Double-click to change the number, convert between side and island forms, or change edge tracks, sides, extents, and offsets. Existing platforms keep their owning station. **Delete platform** removes only that platform and leaves its tracks intact. Cancel preserves the original values.

Platforms follow their track endpoints, including node-drag previews. A node move that would collapse or cross an attached platform is rejected before changing anything. Platform placement is relative to its tracks, so platforms are edited with the placement fields rather than dragged independently. Grid snapping applies to node movement, not percentage/offset fields.

Platform surfaces are drawn behind tracks and labels. Labels and node handles take selection priority; among overlapping platforms, the last drawn surface wins. Fit includes the platform outline and number. Deleting a supporting track through the model removes dependent platforms, including the whole island if either edge loses its track.

## Continuous Draw

Enable **Continuous Draw** in the paint toolbar to keep drawing from each completed track's endpoint. It applies to both clicks and drags, and you can mix the two. Existing endpoint nodes are reused. Failed connections leave the last valid starting point available so you can try another endpoint.

The toggle is off by default. Enabling it preserves a starting node already selected for a connection. Disabling it ends the pending connection. Escape, right-click, switching tools, changing snapping, or changing the view also ends the current chain; completed tracks and explicitly placed nodes remain. The preference stays enabled until you turn it off, so the next independent track starts another chain.
