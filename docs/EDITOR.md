# Interactive diagram editor

The application provides editing and navigation toolbars above the canvas. Choose New Network in the startup dialogue to open an empty diagram.

## Controls

Keyboard shortcuts: **N** selects Add Node; **T** selects Add Track. Switching tools cancels pending edits and updates the toolbar selection. Shortcuts do not intercept typing in text fields.

- **Select / Move:** click a blue node handle to select it. Drag to preview a new position, then release to commit it. Tracks follow the preview. Clicking empty space clears the selection.
- **Add Node:** choose Regular or Stub end and click empty space. Identities are assigned automatically. Clicking an existing handle selects that node instead of creating a duplicate.
- **Add Track:** choose Mainline, Station, or Siding. Drag from an existing node or empty space to another node or empty space. A dashed line previews the track; missing endpoints are created together with the track on release. Existing node handles and exact snapped endpoint positions are reused. You can also click two existing nodes as before. An empty-space click without a drag creates nothing. The node type choice applies to new endpoints only.
- **Snap to grid:** enabled initially, with 20-unit spacing. Disable it for exact placement. Selecting an off-grid node does not move it; only an actual drag applies snapping.
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

This increment supports node selection, creation, movement, track creation by clicking or dragging, and view navigation. Node and track types are chosen for new objects. Saving/loading, deletion tools, undo/redo, and drawing stations/platforms are still future work. Edits currently exist only in memory and are lost when the application closes.

## Tests

Run `./tests/run-tests.ps1` with a JDK installed. It compiles the entire application and runs model checks, editing validation checks, and real Swing mouse/toolbar interactions in headless mode. It checks drag preview versus commit, cancellation, snapping, invalid-edit atomicity, ID allocation, view transforms, pointer-anchored zoom, panning, track gestures, endpoint reuse, oriented stub rendering, and rendering without a window.
