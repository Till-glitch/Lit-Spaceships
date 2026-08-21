# NeoForge Alpha Spaceship Mod – Technical Architecture Master Document

## 1. Projekt-Übersicht

Diese Mod implementiert ein komplexes, server-authoritative Raumschiff-System für NeoForge 1.21. Das Kernziel ist nicht nur „ein Schiff im Minecraft-Chunk bewegen“, sondern die Erstellung eines modularen Raumfahrtsystems mit:

- struktureller Erkennung von Schiffen aus beliebigen verbundenen Blöcken
- Subsystemen für Energie, Schilde, Navigation und Bewegung
- persistenter Server-Logik mit Savegame-Integration
- physischer World-Representation über Block- und BlockEntity-Schicht
- Client-seitigem Rendering und UI-Layer für visuelles Feedback und Bedienung

Das Projekt ist als Prototyp/Architektur-Experiment für ein KI-gestütztes Refactoring angelegt. Die aktuelle Implementierung zeigt bereits ein funktionierendes Gesamtsystem, aber mit klaren Symptomen hoher Kopplung: Das Datenmodell, die Serielle Logik, die Rendering-Mechanik und die Welt-Interaktionen sind teilweise in dieselben Objekte verwoben.

Das Systemmodell lässt sich als folgende Architektur lesen:

- Logic Layer: `com.peaceman.alpha.ship`
- World / BlockEntity Layer: `com.peaceman.alpha.block`, `com.peaceman.alpha.block.entity`
- UI / Client Layer: `com.peaceman.alpha.client`, `com.peaceman.alpha.client.render`, `com.peaceman.alpha.client.screen`, `com.peaceman.alpha.menu`
- Networking: `com.peaceman.alpha.network`

Die zentrale Annahme der Mod ist: Ein Schiff ist eine strukturierte Menge von Blöcken mit einem Controller, optionalen Reaktoren, Schildmodulen und einem berechneten „Schutzvolumen“. Die Server-Instanz hält den echten State; Client-Informationen sind nur als Synced-View und Render-Kontext gedacht.

---

## 2. Kern-Architektur (Backend / Logic Layer)

### 2.1 Paketstruktur und Entwurfsprinzipien

Das Paket `com.peaceman.alpha.ship` enthält die logische Mitte der Mod. Die zentrale Idee ist eine zentrale Registry der aktiven Schiffe, die als System-of-Record für alle dynamischen Schiffsinstanzen dient.

Wichtige Klassen:

- `Spaceship` – Zustand des Schiffs und seine Metadaten
- `SpaceshipManager` – Registry, Lifecycle, Erstellung/Löschung und Struktur-Aktualisierung
- `ShipSavedData` – persistente Server-Savegame-Serialisierung
- `SpaceshipScanner` – blockbasierte Schiffs-Erkennung
- `ShieldMorphology` – Shield-Bubble-Kalkulation
- `SpaceshipMover` – Translations- und Teleport-Mechanik
- `SpaceshipNavigationManager` – Homes, Warp-Logik, Travel-Commands
- `SpaceshipEnergyManager` – Energieberechnung und Verbrauch
- `SpaceshipShieldHandler` – Status der Schildaktivierung

Die Architektur ist derzeit stark „manager-zentralisiert“ und „repo-like“: Der Server verwaltet die aktive Menge `ACTIVE_SHIPS`, und nahezu alle Logikpfade laufen über globale statische Manager oder direkte Objektbeziehungen.

### 2.2 Spaceship & SpaceshipManager: State-Management und Lifecycle

#### `Spaceship`

`Spaceship` ist das zentrale Datenmodell für ein Schiff. Es enthält:

- eindeutige `UUID id`
- `BlockPos controllerPos`
- `Set<BlockPos> blocks`
- `Map<String, BlockPos> homes`
- `List<BlockPos> reactors`
- `List<BlockPos> shields`
- `Set<BlockPos> shieldBubble`
- `Boolean isShieldActive`

Das Objekt ist bewusst als Aggregat und nicht als reines DTO begriffen: Es hält nicht nur reine Daten, sondern auch berechnete Zustände wie die ShieldBubble sowie aktionale Informationen (z. B. Schild aktiv/inaktiv).

Wesentlich ist Methode `setBlocks(Set<BlockPos>, Level level)`:

1. `blocks` wird neu gesetzt
2. bestehende Reaktor- und Schildlisten werden geleert
3. der Level wird durchlaufen und passende BlockEntity-Typen erkannt (`SpaceshipReactorBlockEntity`, `SpaceshipShieldBlockEntity`)
4. wenn Schilde existieren, wird `ShieldMorphology.calculateShieldBubble(...)` aufgerufen
5. auf dem Server wird anschließend ein `ShieldBubbleSyncPacket` an alle Clients gesendet

Dieses Design macht die Shield-Bubble zu einem „precomputed derived state“: Sie wird nicht bei jedem Render-Frame neu berechnet, sondern beim Struktur-Update der Schiffsblöcke.

#### `SpaceshipManager`

`SpaceshipManager` fungiert als Realtime-Registry für aktive Schiffe:

```java
public static final Map<UUID, Spaceship> ACTIVE_SHIPS = new HashMap<>();
```

Die relevanten Lifecycle-Hooks sind:

- `createShip(Level, BlockPos startPos)`
  - prüft, ob am Startblock ein `SpaceshipControlBlockEntity` liegt
  - scannt die komplette Struktur mit `SpaceshipScanner.scan(level, startPos)`
  - erzeugt neues `Spaceship`-Objekt
  - ruft `newShip.setBlocks(shipBlocks, level)` auf
  - registriert das Schiff in `ACTIVE_SHIPS`
  - setzt `shipId` auf allen `ISpaceshipNode`-BlockEntities
  - speichert NBT via `ShipSavedData`

- `updateShipBlocks(Level, Spaceship ship)`
  - rescanned die Struktur zum Controller
  - aktualisiert `ship.setBlocks(...)`
  - korrigiert `shipId` für alle betroffenen Knoten
  - persistiert danach

- `deleteShip(Level, Spaceship ship)`
  - räumt `shipId` der Knoten ab
  - entfernt die Instanz aus `ACTIVE_SHIPS`
  - markiert Savegame als dirty

Der Manager ist serverseitig zentraler Zugriffspunkt: Knoten, Network, UI und Befehle holen/setzen Schiffe meist über `SpaceshipManager.getShip(UUID)`.

### 2.3 ShipSavedData: Persistenz auf dem Server

`ShipSavedData` erweitert `SavedData` und serialisiert die aktiven Schiffe als `ActiveShips`-Liste in den Welt-Save.

Persistiert werden:

- `ID` (UUID)
- `Controller`-BlockPos
- `Blocks` als `ListTag` von IntArray-Blockkoordinaten
- `Homes` als Map von Name -> Position
- `Reactors` und `Shields`
- `ShieldBubble` als vorberechnete Menge von Positionen

Das Verhalten ist hier entscheidend:

- Beim Laden eines Weltsaves wird `SpaceshipManager.ACTIVE_SHIPS.clear()` ausgeführt
- Für jeden gespeicherten Schiffsnahmen wird ein neues `Spaceship`-Objekt rekonstruiert
- `setShieldBubble(loadedBubble)` wird direkt gesetzt

Das ist ein klarer Hinweis auf eine „optimistische Serialisierung-Strategie“: Es wird nicht der volle Signalfluss der Runtime berechnet, sondern der bereits vorberechnete Shield-Bubble-Pool gespeichert und später wieder verwendet.

Stärken:

- einfache, robustes Savegame-Format
- Schiffs-Definition und Client-Render-Input sind serialisierbar

Schwächen:

- keine Trennung zwischen Domain-State und Render-Graph-State
- Savegame enthält technisch abgeleitete Daten wie `ShieldBubble` statt nur Basisstate
- beim Chunk-Load / Welt-Reset kann es zu inkonsistenten Zuständen kommen, wenn `ACTIVE_SHIPS` nicht vollständig wiederhergestellt wird

### 2.4 SpaceshipMover & SpaceshipNavigationManager: Bewegungslogik

#### `SpaceshipMover`

Die eigentliche Schiffsbewertung existiert in `SpaceshipMover`. Die Bewegung erfolgt auf Basis von Server-Positionen und dient als server-authoritative Transformationslogik.

Wesentliche Logik:

- verschiebt die komplette Block-Menge eines Schiffs um `(dx, dy, dz)`
- prüft Energieverbrauch und ggf. visauliziert Bedingungen
- entfernt ggf. betroffene Blöcke aus der Welt, setzt sie an neue Positionen
- sorgt für das korrekte Handling von `BlockEntity`s und NBT-Data
- transportiert dabei auch die `shipId` und relevante Informationen nach vorne

Das Design nutzt einen Snapshot-/Restore-Mechanismus: Beim Bewegen wird der bestehende Blockzustand und das zugehörige BlockEntity-NBT quasi „gesichert“ und nach dem Übersetzen wiederhergestellt. Dadurch verhindern sie Item-Drops bzw. Block-Desyncs, obwohl das Schiff in einer großen Menge von Blöcken „verschoben“ wird.

Wesentliche Eigenschaft:

- `SpaceshipMover.moveShip(...)` arbeitet mit einem kompletten Schiffs-Offset und verschiebt damit die strukturelle Welt-Representation, nicht nur eine einzelne Physik-Entity

#### `SpaceshipNavigationManager`

Dieser Manager stabilisiert die Navigation und die „Home-Positions-Mechanik“:

- `saveHome(level, ship, name)` speichert BlockPos als Home
- `teleportToHome(level, ship, homeName, player)` nutzt gespeicherte Positionsdaten
- `player` wird als perspektivischer Bindungspunkt verarbeitet, sodass Teleportierung als relevante Spieler-Interaktion funktioniert

Es ist ein typisch „command-oriented“ Layer: Befehle wie `SAVE_HOME`, `TP_HOME`, `MOVE_FORWARD`, `MOVE_UP`, `TOGGLE_SHIELD` werden als Payloads an den Server gesendet und dort in methodische Aktionen übersetzt.

### 2.5 SpaceshipScanner & ShieldMorphology: Struktur-Erfassung und Schildgeometrie

#### `SpaceshipScanner`

`SpaceshipScanner.scan(Level, BlockPos startPos)` startet bei einem Controllerblock und nutzt Breitensuche (`Queue<BlockPos>`), um alle verbundenen, nicht-leeren Nachbarblöcke zu sammeln.

Die Erkennung ist bewusst auf „grobe, blockbasierte Gesamtstruktur“ ausgelegt, nicht auf komplizierte voxel basierte Mesh-Analyse. Der Scanner hat dabei eine `maxBlocks = 2000`-Grenze.

Besonders wichtig:

- `ensureMultipartBlocks(...)` erweitert die Struktur für teilige Blöcke wie
  - Türen
  - Betten
  - Doppeltruhen

Dadurch werden sogenannte „multipart blocks“ nicht als separate oder unvollständige Strukturen erkannt; die Schiffs-Menge bleibt konsistent.

#### `ShieldMorphology`

Die Schildblase wird auf Grundlage einer morphologischen Dilatation berechnet. Der Algorithmus ist in zwei Schritte aufgeteilt:

1. `getSurfaceBlocks(shipBlocks)`
   - bestimmt die äußeren Blöcke, indem für jeden Block geprüft wird, ob ein direkter Nachbar nicht zum Schiff gehört
2. `calculateShieldBubble(shipBlocks, radius)`
   - für jeden Oberflächenblock wird eine Kugel im Radius `r` in einem 3D-Volumen durchlaufen
   - wenn `(x*x + y*y + z*z <= r^2)` gilt, wird der Block als Teil der Blase aufgenommen
   - danach werden die inneren Schiffsblöcke addiert

Das Resultat ist ein Set aller Blöcke, die gemäß dieser Morphologie „innerhalb des Schildes“ liegen. Die Schildblase ist also kein reiner Render-Hack, sondern ein geplanter, serverseits berechneter Schutzbereich.

### 2.6 SpaceshipEnergyManager & SpaceshipShieldHandler: Energieverbrauch und Schildstatus

#### `SpaceshipEnergyManager`

Der Energie-Layer ist derzeit zentral und eher als Commit/Cost-Domain konzipiert. Reaktoren sind „Energiequellen“ und Schiffe sammeln sie über ihre Reaktorlisten. Dadurch wird Energie nicht als allgemeines Capability-System modelliert, sondern als direkte Datenstruktur auf dem Schiffsobjekt.

Ein Schiffsobjekt hält `reactors` und `shields` als Listen von BlockPositions. Der Energie-Manager scheint die Verbrauchsberechnung auf Basis der vorhandenen Reaktoren und der aktiven Schilde durchzuführen.

Zentraler Effekt:

- Schilde können nur aktiv sein, wenn ausreichende Energiequelle vorhanden ist
- Energie ist nicht nach „generator / consumer network“ modularisiert, sondern als einfache, serverseitig aggregierte Kostenfunktion modelliert

#### `SpaceshipShieldHandler`

`SpaceshipShieldHandler` verwaltet den Shield-Status und den Aktivierungszustand. Das `Spaceship`-Objekt selbst trägt ein `Boolean isShieldActive`, das durch `toggleShieldActive()` oder `setShieldActive(Boolean)` verändert wird.

Da die Logik in `ShipCommandPayload` über `TOGGLE_SHIELD` ausgelöst wird, ist das Design derzeit klar „command-driven“ statt „event-driven state machine“.

---

## 3. Die physische Welt (Block & Entity Layer)

### 3.1 Das Interface `ISpaceshipNode`

`ISpaceshipNode` ist das zentrale Interface für sämtliche Schiffs-BlockEntity-Knoten. Es gilt als Brücke zwischen der Welt und dem zentralen Schiffs-Registry-State.

Wesentliche Methoden:

- `UUID getShipId()`
- `void setShipId(UUID shipId)`

Damit können BlockEntity-Knoten zuverlässig an ein Schiff gebunden werden.

### 3.2 `AbstractSpaceshipNodeBlockEntity`

Diese abstrakte BlockEntity-Klasse liefert die gemeinsame Basis für alle Schiffsbezogenen Knoten. Sie übernimmt:

- Speicherung von `shipId` in NBT (`saveAdditional`, `loadAdditional`)
- `setShipId()` mit `setChanged()`
- Server-seitige Synchronisierung über `level.sendBlockUpdated(...)`
- `getUpdateTag()` und `getUpdatePacket()` für BlockEntity-Sync

Das ist die Schlüsselstelle, über die der Server das Schiffs-Identitäts-Tracking an den Client kommuniziert.

### 3.3 Physische Schiffsblöcke

#### `SpaceshipControlBlock`

Ein Controllerblock, der direkt die Schiffsbeziehung initiiert und als Ankerpunkt für Struktur-Analyse dient. Er dient als Startposition für die Erkennung und als feste Referenz für Schiffsdefinition.

#### `SpaceshipHelmBlock`

Der Helmblock ist das Bedienelement für das Schiff, typischerweise mit GUI-Komponenten für Steuerung, Navigation oder Sicht/Logik.

#### `SpaceshipReactorBlock`

Reaktorblöcke repräsentieren Energiequellen im Schiff. Sie werden beim Struktur-Update in `Spaceship.reactors` eingetragen. Dadurch wird der Reaktorlayer als Teil des Schiff-State modelliert.

#### `SpaceshipShieldBlock`

Shield-Blöcke tragen zu `Spaceship.shields` bei und beeinflussen die Shield-Bubble und den aktiven Schildstatus. Dabei ist die Logik derzeit auf direkte Block-Listen und die zentrale berechnete Bubble angewiesen.

### 3.4 Interaktion zwischen Blocks und SpaceshipManager

Die Wechselwirkung verläuft derzeit typischerweise so:

1. Spieler interagiert mit Kontroll- oder Helmblock
2. GUI-/Payload-Mechanik sendet Befehl an Server (`ShipCommandPayload`)
3. Server findet das passende `Spaceship` über `SpaceshipManager.getShip(shipId)`
4. Manager/Navigation/Mover aktualisieren Struktur oder Position
5. für alle betroffenen Blöcke wird `setShipId(...)` erneut gesetzt
6. `ShipSavedData` wird „dirty“ markiert und persistiert

Also: Die physischen Blöcke sind nicht als eigenständige, autonome Simulationen konzipiert; sie sind vielmehr Manifestationen des zentralen State-Registry-Modells. Das ist ein starkes Zeichen für eine frühere „BlockEntity as view model“ Herangehensweise.

---

## 4. Rendering & Frontend (Client Layer)

### 4.1 ShieldRenderer: Voxel-Volumen und Shader-Integration

`com.peaceman.alpha.client.render.ShieldRenderer` ist der auffälligste Render-Teil. Es baut das Schild als 3D-Volumen aus einer Menge von relativen Blockpositionen auf.

Wichtige Bestandteile:

- `ACTIVE_CLIENT_SHIELDS: Map<UUID, ClientShieldData>`
- `ClientShieldData { anchorPoint, relativeBubbleBlocks, vertexBuffer }`
- `buildShieldMesh(Set<BlockPos> relativeBlocks)`
- `drawFace(...)` für die einzelnen Seiten eines Voxel-Blocks

Die Renderpipeline ist:

1. Server sendet `ShieldBubbleSyncPacket`: `shipId`, `anchorPos`, `relativeBubbleBlocks`
2. Client empfängt Paket und setzt `ACTIVE_CLIENT_SHIELDS`
3. `ShieldRenderer` baut ein Mesh aus dessen Voxel-Teilmenge
4. `BufferBuilder` setzt `POSITION_TEX`-Vertices für jedes sichtbare Gesicht
5. `VertexBuffer` wird mit Shadern gezeichnet

Wichtige technische Besonderheit: Für den World-Space Lock wird eine Kamera-Rotation invertiert:

```java
Quaternionf cameraRotation = camera.rotation();
Quaternionf inverseCamRot = new Quaternionf(cameraRotation).invert();
poseStack.mulPose(inverseCamRot);
```

Das ist eine bewusste Umgehung eines klassischen Render-Engine-Problems: Der Shield wird nicht „an der Kamera festgeklebt“, sondern in einer statischen Welt-Relation gezeichnet, die robust gegen Engine-Overrides bleibt.

Zusätzlich werden Custom-Shader-Uniforms gesetzt:

- `HexModelViewMat`
- `HexProjMat`

Das vermeidet, dass der Standard-Minecraft-Shader-Mechanismus das benutzerdefinierte Jelly/Shield-Rendering überschreibt. Das ist ein sehr technischer, gezielter Escape-Hatch und zeigt eine bewusste Dissonanz zwischen Engine-Defaults und eigener Visualisierung.

### 4.2 UI Layer: Screens und Menüs

#### `AbstractSpaceshipScreen`

`AbstractSpaceshipScreen` ist die gemeinsame Basisklasse für alle Schiffs-GUIs. Sie holt die aktuelle `shipId` aus dem BlockEntity am `blockPos`:

```java
if (minecraft.level.getBlockEntity(blockPos) instanceof ISpaceshipNode node) {
    shipId = node.getShipId();
}
```

Diese Methode ist zentraler Mechanismus: UI-Komponenten müssen nicht das Schiffsobjekt selbst kennen, sondern nur die eindeutige ID und den Zielblock.

#### Menü- und Screen-Klassen

- `SpaceshipControlScreen`
- `SpaceshipHelmScreen`
- `SpaceshipReactorScreen`
- `SpaceshipReactorMenu`

Diese Screens nutzen das `ShipCommandPayload`-Pattern für Befehle: Beim Klick auf einen Button wird ein Paket erzeugt, das Server-Aufruf (`MOVE_*`, `SAVE_HOME`, `TP_HOME`, `TOGGLE_SHIELD`, etc.) kapselt.

Das UI ist daher derzeit klar als Thin Client / Controller-Schicht abgestimmt: Es stellt Befehle bereit, aber nicht die echte Logik des Schiffs. Das ist ein gutes und verständliches Entwurfsmuster, obwohl es verschiedentlich auf direkte BlockEntity-Querys angewiesen ist.

---

## 5. Netzwerk & Synchronisation

### 5.1 `ShieldBubbleSyncPacket`

`ShieldBubbleSyncPacket` ist ein Custom Payload, der das Shield-Volumen an alle Spieler sendet.

Kernfelder:

- `UUID shipId`
- `BlockPos anchorPos`
- `Set<BlockPos> relativeBubbleBlocks`

Wichtige Designentscheidung: Die Bubble wird nicht als absolute Weltposition gesendet, sondern als relative Koordinaten zum Controller (`absPos.subtract(controllerPos)`). Dadurch:

- steigt Effizienz auf dem Client
- wird bei Schiffsbewegung/Teleportation unnötiger weltweiter Synchronisations-Overhead reduziert
- bleiben Render-Daten lokal mit einem festen Anchor verknüpft

Clientseitig wird dieses Paket in `ACTIVE_CLIENT_SHIELDS` aufgenommen und als Mesh-Input benutzt.

### 5.2 `ShipCommandPayload`

`ShipCommandPayload` ist der zentrale Command-Carrier zwischen Client und Server.

Felder:

- `Optional<UUID> shipId`
- `BlockPos pos`
- `String command`
- `int value`
- `String homeName`

Befehle sind derzeit String-basiert und werden in der `handleData(...)`-Methode ausgewertet, z. B.:

- `CREATE`
- `SAVE_HOME`
- `TP_HOME`
- `MOVE_UP`, `MOVE_DOWN`, `MOVE_FORWARD`, `MOVE_BACKWARD`, `MOVE_LEFT`, `MOVE_RIGHT`
- `UPDATE_BLOCKS`
- `DELETE_SHIP`
- `TOGGLE_SHIELD`

Dadurch entsteht ein flexibles Command-Protocol, das praktisch zu jeder UI-Interaktion passt. Die derzeitige Form ist aber als „primitive message bus“ zu lesen: nicht als polymorphes Domain-Event-System, sondern als String-switch-Dispatch.

### 5.3 Client-Server-Ablauf in der Praxis

Einer der typischen Datenflüsse im System ist:

1. Server erzeugt oder aktualisiert Schiff
2. `Spaceship.setBlocks(...)` berechnet `shieldBubble`
3. `ShieldBubbleSyncPacket` wird an alle Clients verteilt
4. Client schreibt Shield-Mesh in `ACTIVE_CLIENT_SHIELDS`
5. Client-Render-Layer zeichnet visuelles Schild
6. UI-Interaktion verschickt `ShipCommandPayload` an den Server
7. Server führe Logik aus und aktualisiert State

Die größte Stärke dieses Systems ist seine direkte Verständlichkeit. Die größte Schwäche ist seine starke Abhängigkeit von stringbasierten Befehlen und globalen statischen Maps, was spätere Erweiterung, Testbarkeit und Domain-Model-Abstraktion erschwert.

---

## 6. Schwachstellen & Refactoring-Ziele

### 6.1 Aktuelle Kopplungspunkte

Die Architektur zeigt mehrere klare Koppelstellen:

1. State und Rendering sind nicht getrennt
   - `Spaceship` enthält `shieldBubble`, eine abgeleitete Render-Repräsentation
   - diese wird direkt in der Server-Logik berechnet und serialisiert

2. BlockEntity- und Manager-Level sind zu eng verbunden
   - `ISpaceshipNode` bindet konkrete Weltblöcke an zentrale Manager-Registry
   - der Logik-Stack kennt die BlockEntity-Implementierungen direkt

3. Befehls-String-System ist fragil
   - `ShipCommandPayload` verarbeitet Zeichenketten statt Typed Commands
   - keine typsichere Domain-Command-Klasse / separierte Action-Modelle

4. Dirigierte globale Singletons
   - `SpaceshipManager.ACTIVE_SHIPS` ist ein globaler State-Singleton
   - dadurch werden Nebenläufigkeit und Testbarkeit schlechter

5. Persistenz enthält abgeleitete Daten
   - `ShieldBubble` ist ein Vektor aus berechneter Sichtbarkeit, nicht primäre Persistenzlogik

### 6.2 Typische Probleme für Skalierung und Wartbarkeit

#### Thread-/Concurrency-Sicherheit

`SpaceshipMover` arbeitet mit vollständiger Schiffs-Transformation und Welt-Block-Updates. In einer Mod, die mehrere Server-Operationen gleichzeitig abhandelt, ist die Sicherheit nicht klar durch ein eigenes, isoliertes Domain-Modell abgesichert. Ein globales statisches `ACTIVE_SHIPS`-Map ist für parallelisierte Operationen grundsätzlich kritisch. Die aktuelle Logik ist funktional, aber nicht sauber in eine thread-safe Simulation-Domain übersetzt.

#### Chunk-Loading / World Sync

`ShipSavedData` lädt Schiffe aus der Welt-Datenbank. Wenn ein Chunk mit Schiffsknoten geladen wird, sind Synchronisation und Identitäts-Aktualisierung der `shipId` auf BlockEntity-Ebene empfindlich. Eine effiziente Synchronisation beim Chunk-Laden ist derzeit nicht klar als eigener Service modelliert.

#### Domain-/View-Trennung

`Spaceship` enthält Charakteristika einer reinen Engine-Domain sowie eines Render-View-State. Das ist für ein Refactoring problematisch, weil zwei unterschiedliche Verantwortlichkeiten in einem einzigen Objekt zusammenliegen.

#### Struktur-Update-Overhead

`SpaceshipScanner.scan()` und `ShieldMorphology.calculateShieldBubble()` sind O(n) bis O(n * radius³)-ähnliche Berechnungen je nach Größenklasse. Diese werden bei jedem Update auf Vollschiffen ausgeführt. Für größere Schiffe ist das potenziell teuer.

### 6.3 Refactoring-Ziele für ein KI-gestütztes Architektur-Konzept

Ziel ist keine vollständige Neuimplementierung aus dem Nichts, sondern eine klare Aufteilung in die folgenden Schichten:

1. Domain Model
   - `ShipState` / `ShipDefinition` als reines Datenmodell
   - keine direkte Renderer-/Welt-Referenzen in der Domain

2. Repository / Server Manager
   - `ShipRepository` / `ShipRegistry` statt statischer globaler Maps
   - zentrale Oktetten-Identität und Lebenszyklusverwaltung verwalten

3. System / Application Layer
   - `ShipStructureService`
   - `ShipMovementService`
   - `ShipEnergyService`
   - `ShipShieldService`
   - `ShipPersistenceService`

4. Render/View Layer
   - `ShieldMeshBuilder` als Client-spezifischer, reiner Renderer
   - gereinigte Client-Side State-Daten statt Engine-Domain-State

5. Command / Event Layer
   - Typed Command objects statt String-Schalter
   - optionales Event-Bus-Konzept oder Capability-basiertes Routing

6. Persistence Boundary
   - nur primäre Structure- und Domain-Daten speichern
   - abgeleitete Render- oder Bubble-Daten als cacheable view

### 6.4 Konkrete Architektur-Optionen

#### Option A: MVC / Ports-and-Adapters

Gut geeignet, wenn die Mod möglichst klar in Server- und Client-Layer aufgeteilt werden soll:

- Model: ShipState + Structure + Energy resource snapshots
- View: ShieldRenderer, Screen, HUD, UI overlays
- Controller: Manager/CommandHandlers + Shipping-Payload translation

#### Option B: ECS (Entity-Component-System)

Gut für sehr viele Knoten und dynamische Subsysteme:

- `ShipComponent`
- `EnergyComponent`
- `ShieldComponent`
- `NavigationComponent`
- `TransformComponent`
- `StructureComponent`

Das eignet sich für größere Schiffs-Subsysteme, aber nur, wenn die Modularchitektur von Grund auf sauber aufgeteilt wird.

#### Option C: Capability-/Service-basierte Architektur

Gut für NeoForge-Integration und modularen Subsystem-Status:

- Capability für Energie
- Capability für Steuerung
- Capability für Shield-State
- Capability für Navigation

Diese Option passt gut zu Minecraft-Tooling, aber sie wird schnell komplex und muss sauber gegen rein logische Domain-Services abgegrenzt werden.

---

## Evaluierung des aktuellen Systems

Die Mod ist bemerkenswert, weil sie bereits die wesentlichen Charakteristika eines komplexen, funktionalen Schiffs-Systems umgesetzt hat:

- Struktur-Erkennung via BFS
- Persistente Speicherung via `SavedData`
- Server-Authoritative State-Management
- Command-Oriented Client-Server-Interaction
- Visualisierung eines Schutzvolumens per custom Mesh + Shader

Gleichzeitig zeigt die Codebasis deutlich, dass die Architektur in der Entwicklung vor allem auf Funktionalität und schnelle Iteration ausgerichtet war – nicht auf langfristige Modularität. Für ein Architektur-Refactoring ist genau dieser Zustand ideal: Es liegt bereits ein funktionsfähiger Prototyp vor, dessen Domain-Strukturen und Interfaces sauber in ein skalierbares, langfristiges Modell überführt werden können.

Das ist der ideale Ausgangspunkt für ein DeepResearch / AI-Architecture-Design: Die Systemgrenzen sind sichtbar, die Kernprinzipien sind klar, und die wichtigsten Refactoring-Targets sind bereits in der Codebasis erkennbar.

---

## Schlussfolgerung

Das Projekt ist im Kern eine server-authoritative Raumschiff-Engine mit:

- zentralem Schiff-Registry-State
- strukturbezogener Schiffs-Erkennung
- geometrischer Shield-Bubble-Berechnung
- BlockEntity- und World-Integration
- Client-seitigem Rendering und UI-Befehlsprozessor

Die zentrale Herausforderung für das nächste Refactoring ist nicht mehr die Funktionalität selbst, sondern die sauberere Trennung zwischen:

- Domain-State
- Welt-/BlockEntity-Integration
- Client-Rendering
- Netzwerk-/Command-Handling
- Persistenz

Wenn diese Grenzen sauber modelliert werden, können Schiffslogik, Energie, Navigation und Rendering unabhängig weiterentwickelt und durch AI-gestützte Design-Entscheidungen skalierbar gemacht werden.
