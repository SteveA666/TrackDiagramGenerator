# Saving and opening diagrams

The application stores native diagrams as `.json` files in `saved_diagrams/`, relative to the working directory. Start it from the project root (or use `start.bat`, which sets that directory) to use the project's dedicated folder. The folder is created automatically when listing or saving diagrams. Saved files are ignored by Git; `.gitkeep` preserves the empty directory.

## File actions

- **Save / Ctrl+S:** saves to the current filename. The first save asks for a name.
- **Save As / Ctrl+Shift+S:** asks for another filename, adds `.json` if missing, and confirms replacement of an existing file. Later Save actions use that filename.
- **Open / Ctrl+O:** lists JSON files in the dedicated folder. The startup **Open Network...** button uses the same list and loader. Cancelling or failing a startup load keeps the startup dialogue open.
- **New / Ctrl+N:** starts a fresh empty diagram.
- **Exit / window close:** closes the window after resolving unsaved changes.

New, Open, and closing offer **Save**, **Discard**, and **Cancel** when the committed diagram differs from its last saved contents. Cancelling a save or a failed save aborts the requested action. An asterisk in the title indicates unsaved changes. Selection, drag previews, zoom, pan, debug visibility, and tool preferences are not saved and do not mark the diagram changed. File actions cancel pending gestures; explicitly placed starting nodes remain real edits.

Filenames cannot include directory paths. To open a JSON file received elsewhere, place it in `saved_diagrams/`. A loaded diagram is fitted to the canvas. Failure to open preserves the current network, current filename, and unsaved contents.

## JSON format, version 1

Files are readable UTF-8 JSON. Every field shown below is required, and empty collections are empty arrays. Collection order is preserved because it can affect drawing and selection. Object IDs are unique within each object type, and platform numbers are unique within a station.

```json
{
  "formatVersion": 1,
  "nodes": [
    {"id": 1, "x": 100, "y": 100, "type": "REGULAR"},
    {"id": 2, "x": 400, "y": 100, "type": "STUB_END"}
  ],
  "tracks": [
    {"id": 1, "startNodeId": 1, "endNodeId": 2, "type": "STATION"}
  ],
  "stations": [
    {
      "id": 1, "x": 100, "y": 40, "name": "Central",
      "platforms": [
        {
          "number": 1,
          "edges": [
            {"trackId": 1, "side": "LEFT", "startFraction": 0.2, "endFraction": 0.8, "offset": 8}
          ]
        }
      ]
    }
  ],
  "customTexts": [
    {"id": 1, "x": 100, "y": 200, "text": "First line\nSecond line", "size": 18, "font": "Dialog", "color": "#000000"}
  ]
}
```

Node types are `REGULAR` and `STUB_END`; track types are `MAINLINE`, `STATION`, and `SIDING`. Platform edges use directed `LEFT` / `RIGHT`, as described in the [model guide](MODEL_UPDATE.md). Side platforms have one edge; islands have two edges referencing distinct tracks. Fractions and offsets retain their model meanings. Coordinates and IDs are 32-bit integers; negative coordinates are supported. Adjacency and platform outlines are rebuilt rather than saved.

The loader requires strict JSON and rejects duplicate properties, unsupported versions, missing fields, wrong value types, duplicate identities, missing references, invalid model values, zero-length tracks, and collapsed or crossed platform surfaces. Parallel tracks and overlapping unconnected nodes remain valid.

## Implementation and verification

`DiagramIO` encodes the document explicitly rather than serializing the model object graph. It builds nodes, tracks, stations/platforms, and text into a separate `Network`; the document swaps networks only after successful validation. Saves validate first, write a sibling temporary file, then replace the destination atomically where supported, falling back to normal replacement on other filesystems. Temporary files are cleaned up after failed writes or replacement. Failed saves do not change the document's current filename or saved baseline.

`DiagramStore` handles the dedicated directory and filenames; `DiagramDocument` tracks the active network, filename, and saved contents independently of Swing. Gson is bundled into the executable JAR by Maven Shade, so the normal `java -jar` command still works.

Run `.\mvnw.cmd verify` for persistence round trips, UTF-8 text, reference restoration, validation failures, save failure handling, document state, loaded-canvas rendering, and File menu shortcuts, together with the existing editor/model tests.
