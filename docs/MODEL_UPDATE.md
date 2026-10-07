# Diagram model update

The original model update and subsequent diagram editing additions are described below. It contains no train, service, timetable, signalling, or simulation model. The Swing renderer draws stations and custom text. Side and island platforms are drawn and can be edited through the platform tool. DiagramIO remains a placeholder; registering stations does not implement saving or loading.

## Install

Copy the archive's src/main/java/model, src/test/java, and docs folders into the matching folders in your existing project, replacing the model files. The archive does not contain or replace UI files, README.md, Git history, or local IDE settings. The prepared update has now been applied directly to this project after folder access was granted.

## Connectivity and editing

- Segment constructors and edits reject null endpoints and endpoints with the same node ID.
- A network accepts only its registered node objects as segment endpoints. Another object with the same ID is rejected.
- Existing setStart and setEnd calls remain supported. Once registered, a segment routes these edits through its owning Network so adjacency stays synchronized.
- Use segment.setEndpoints(start, end) or network.reconnectTrackSegment(id, start, end) to change both endpoints atomically, including reversing them.
- Segments belong to at most one network. Remove a segment before transferring it to another network.
- All validation happens before committing an edit. Failed edits preserve the previous values and connectivity.
- Node and track types cannot be null. Negative diagram coordinates remain valid.

## Stations

Construct stations with an explicit stable ID: new Station(10, "Central"). This replaces the old Station(String) constructor; no existing application code used it.

Network provides addStation, getStation, getAllStations, removeStation, and stationCount. Station names must be non-null and nonblank. Names are editable and do not determine identity.

Stations own their platforms. Platform numbers are positive and unique within each station, and renumbering checks for conflicts. A platform can belong to only one station. Lists returned by the model cannot be modified directly; use the provided edit methods.

Stations may be assembled before registration. Network.addStation validates every platform track reference before accepting the station. Platform edits and additions also validate references while the station is registered.

Removing a station detaches it with its platforms intact and leaves tracks untouched. Re-registering it validates its references again.

## Platform placement

A Platform represents one physical structure with one or two immutable PlatformEdge placements:

- One edge: side platform.
- Two edges on distinct tracks: island platform.

Each edge stores a registered TrackSegment, TrackSide.LEFT or RIGHT, start and end fractions, and an offset in diagram units. Fractions satisfy 0 <= start < end <= 1. Offset is finite and non-negative. Fractions are measured along a segment from its start node to its end node, and follow geometry edits automatically.

For a renderer using screen coordinates (x right, y down), given dx = end.x - start.x and dy = end.y - start.y, LEFT has unit normal (dy, -dx) / length and RIGHT has the opposite normal. An edge endpoint is the corresponding interpolated track point plus offset times the chosen normal. A renderer should handle zero-length track geometry explicitly, for example by skipping its platform edge. Distinct nodes at the same coordinates remain allowed.

Reversing a segment's endpoints reverses side and fraction meanings. To preserve absolute placement during a reversal, replace each affected edge with opposite side and fractions (1 - oldEnd, 1 - oldStart). Edge placement follows track direction by design; it is not anchored to fixed world coordinates.

Island edges have independent placement values so tracks with different lengths or directions can be represented. The model validates references and ranges, not whether the chosen edges form an attractive or non-intersecting polygon. That is a renderer/editor concern.

The platform number labels the whole physical structure. Independent labels for its two boarding faces are not currently modeled.

Example (upper and lower are registered parallel tracks, both directed left to right):

```java
Station central = new Station(10, "Central");
network.addStation(central);
Platform island = new Platform(1,
    new PlatformEdge(upper, TrackSide.RIGHT, 0.2, 0.8, 8),
    new PlatformEdge(lower, TrackSide.LEFT, 0.2, 0.8, 8));
central.addPlatform(island);

// Change extent, side, track, or side/island form in one validated edit.
island.setEdges(
    new PlatformEdge(upper, TrackSide.RIGHT, 0.3, 0.7, 8),
    new PlatformEdge(lower, TrackSide.LEFT, 0.3, 0.7, 8));
```

The old Platform(number, track) constructor remains available and creates a side platform spanning fractions 0..1 on the track's LEFT, offset by 8 units. getTrackSegment returns the first edge's track for compatibility; use getEdges for island platforms. setTrackSegment is supported for side platforms and preserves placement; use setEdges for islands.

The old setSidePlatform(boolean) is replaced by setEdges. A boolean cannot supply the second track and placement needed for a valid island. isSidePlatform and isIslandPlatform derive their values from the edges.

## Deletion policy

Deleting a track removes every platform that references it from registered stations. If either island edge references the deleted track, the entire island platform is removed rather than silently becoming a side platform. Removed platforms are detached from their stations. Stations remain registered even when empty.

Deleting a node uses the same policy for every connected track. Detached stations are outside the network's ownership and are revalidated if added again.

## Verification

With JDK 26 or newer installed, run from the project root:

```powershell
.\mvnw.cmd test
```

Maven compiles the application and runs all six existing check suites through JUnit in headless mode. Reports are written to target/surefire-reports. The model suite includes 491 checks. Coverage includes endpoint mutation and reversal, repeated graph edits, duplicate objects/IDs, failed-edit atomicity, ownership transfers, station registration/removal, collection protection, platform numbering, side/island conversion, placement ranges/non-finite values, foreign references, and cascading node/track deletion.


## Diagram labels

`Station(int id, String name, int x, int y)` adds an explicit drawing position. The two-argument constructor remains available and defaults to `(0, 0)`. Use `getX`, `getY`, and `setPosition` for the station marker's top-left position. Moving or renaming a station preserves its platforms.

`CustomText(String text, int x, int y, int size, String font, String color, int id)` represents a multiline diagram annotation. Its ID is final: changing an identity after registration would break network lookup. The former `setId` method is removed. Position is the text block's top-left corner. Text and font must be nonblank, size must be positive, and colour must match `#RRGGBB`. The model stores font names and colours as strings and has no Swing dependency.

`setAppearance(text, size, font, color)` validates every value before committing any change. Individual setters use the same validation. `Network` provides `addCustomText`, `getCustomText`, `getAllCustomTexts`, `removeCustomText`, and `customTextCount`. It rejects duplicate identities and multiple-network ownership; removing text detaches it. IDs are unique within each object type, as with stations and tracks.

`DiagramEditor` provides `createStation`, `moveStation`, `renameStation`, `createCustomText`, `moveCustomText`, and `editCustomText`. Creation allocates an unused positive ID. Edits reject missing objects and preserve existing data on validation failure. Persistence is still unimplemented.

## Platform drawing and editing

`Platform.setDefinition(number, edges...)` validates the number and every edge before committing either field. `DiagramEditor.createPlatform`, `editPlatform`, and `deletePlatform` operate within a registered station. Edits preserve the platform object and its ownership. Duplicate numbers, foreign track instances, unknown objects, and invalid geometry leave the model unchanged.

`PlatformGeometry` shares the track-relative outline among editor validation, canvas drawing, hit testing, and Fit. Side strips extend 12 units outward from the configured edge; islands join two edges into a simple visible quadrilateral, matching physical ends when track directions differ. Platform numbers use `CustomText` rendering. Model edge ranges remain general; the diagram editor additionally rejects zero-length supporting tracks and crossed or collapsed surfaces.

`moveNode` validates affected platform surfaces at the candidate position before committing the node coordinates. This adds diagram geometry protection to existing track-length validation. Programmatic model mutations still need to respect diagram geometry; the canvas skips invalid legacy surfaces rather than failing the whole painting operation.

The platform dialog translates screen-relative choices (Left/Right for mostly vertical tracks, Above/Below for mostly horizontal tracks) into directed `TrackSide` values. Existing model placements retain their meaning, and reopening a platform selects the matching screen side without flipping its geometry. Percentages still follow track start to end.
