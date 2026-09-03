---
description:
---

Master-Rulefile und Standard Operating Procedure (SOP): Entwicklungs-Workflow "Mod Alpha" (NeoForge 1.21)
Dieses Dokument spezifiziert die unumstößliche Standard Operating Procedure (SOP) für alle KI-gesteuerten Entwicklungs-, Architektur- und Testprozesse im Projekt "Mod Alpha" (Minecraft NeoForge 1.21). Du bist der KI-Code-Agent und fortan verpflichtet, dieses produktionsreife, 6-stufige Lifecycle-Framework bei jeder Interaktion rigoros anzuwenden. Jegliche Abweichung von der Model-View-Controller (MVC) Architektur, dem Custom Networking und der Voxel-Mathematik ist absolut unzulässig und führt zum sofortigen Abbruch der Codegenerierung.
Du hast den Workflow sequenziell über acht definierte Befehle abzuarbeiten: /spec, /plan, /build, /build auto, /test, /review, /code-simplify und /modperf, abschließend mit /ship. Jeder Befehl wird durch exakte Quality Gates gesichert, die vollständig erfüllt sein müssen, bevor die nächste Phase beginnt.
Fundamentale Architektur-Axiome
Die Logik ist absolut zwischen Client und Server zu trennen. Die Server-Domain (ShipState, ServerShipManager) darf niemals Klassen aus dem Client-Namespace referenzieren. Du darfst niemals Rendering-Klassen (BlockEntityRenderer) in Server-Logik importieren. Die Evaluierung der Seite hat stets über FMLEnvironment.getDist() zu erfolgen.
Kommunikation erfolgt ausschließlich über deterministisches NeoForge 1.21 Custom Networking (spezifische CustomPacketPayloads via PayloadRegistrar). Direkte Feldmanipulationen über Netzwerkgrenzen hinweg sind strengstens verboten.
Raumschiffe in Mod Alpha bestehen aus bis zu 10.000 Voxeln. Algorithmen müssen zwingend in -Komplexität optimiert sein. Memory Leaks durch unzureichend geleerte Vertex Buffer Objects (VBOs) oder exzessiver Object-Churn auf dem Heap sind präventiv zu unterbinden.
1. Spezifikationsphase: /spec (Spec before Code)
Bevor eine Zeile Code geschrieben wird, muss das Feature erschöpfend definiert werden. Es ist dir strengstens untersagt, bei diesem Befehl ausführbare Code-Fragmente, Java-Klassen oder JSON-Dateien zu generieren. Dein einziger Output ist ein formelles Product Requirements Document (PRD) im Markdown-Format.
Du musst zwingend unsere vier unumstößlichen Lifecycle-Edge-Cases evaluieren:
Teleportation/Dimensionswechsel: Der vollsynchronisierte 6-phasige Warp (Suspension, Forceloading, Clipboard Serialization, Excision, Materialization, Passenger Entity Transition) muss beachtet werden.
Save/Load (NBT): Exakte Definition, wie der Zustand des Features in einem CompoundTag gesichert wird, um State-Verlust zu verhindern.
Block-Zerstörung (Lifecycle): Verhalten bei Eliminierung von essenziellen Blöcken und die Triggerung der ShipStructureDeltaPayload.
Scaling (10.000 Voxel): Mathematische Bewertung der Big-O Zeitkomplexität im PRD, um die Server TPS zu schützen.
Quality Gates für /spec:
Das PRD enthält dedizierte Sektionen für alle 4 Minecraft-Edge-Cases.
Im Output befinden sich absolut keine ausführbaren Code-Snippets.
Das PRD listet explizit alle betroffenen Pakete (com.*) auf.
2. Planungsphase: /plan (Small, Atomic Tasks)
Komplexe Anforderungen müssen in kleine, atomare und streng typisierte MVC-Schichten heruntergebrochen werden. Ein monolithischer Code-Ansatz ist verboten.
Server-Domain: Reines Datenmodell und Autorität (z.B. ShipState). Keine Kenntnis von Client-Rendering oder Spielerkameras.
View-Ebene: Komplettes Client-Rendering (z.B. BlockEntityRenderer). Verbleibt strikt auf dem Client und kommuniziert nie zurück in die Domain. VRAM-Lifecycle (dispose()) muss geplant werden.
Netzwerk-Brücke: Datenaustausch exklusiv durch CustomPacketPayloads. Planung der Records inklusive ResourceLocation und StreamCodec.
DataGen: Keine manuellen JSON-Dateien. Du deklarierst im Plan, welche Provider (ModBlockStateProvider, ModItemModelProvider) programmatisch modifiziert werden.
Quality Gates für /plan:
Absolute Trennung visueller und logischer Klassen ohne Leakage.
Jedes Netzwerk-Ereignis korrespondiert mit einem Payload-Record und StreamCodec.
Keine manuellen JSONs; vollständiger DataGen-Rigorismus.
3. Implementierungsphase: /build & /build auto
Implementierung erfolgt isoliert, Slice-by-Slice. Bei /build auto übernimmst du die Autonomie, prüfst jeden Schritt durch Kompilierung und hältst bei Fehlern unverzüglich an.
Die Einhaltung der Sidedness-Regel ist oberste Pflicht. Client-spezifische Event-Handler sind in mit @EventBusSubscriber(value = Dist.CLIENT) annotierten Klassen zu kapseln. UI-Interaktionen sind vollständig vom Server-Block zu entkoppeln.
Jede Nachricht muss als Record definiert werden, der CustomPacketPayload implementiert, inklusive Type<T> Konstante und StreamCodec.composite(). Registrierungen haben ausnahmslos im RegisterPayloadHandlersEvent oder RegisterClientPayloadHandlersEvent über den PayloadRegistrar stattzufinden. Handler, die Weltmanipulationen erfordern, müssen via context.enqueueWork() auf den Server-Main-Thread delegiert werden.
Quality Gates für /build:
Statische Analyse der Server-Services zeigt null Referenzen zu net.minecraft.client.*.
Alle Payloads verwenden StreamCodec.composite im korrekten Registrierungs-Event.
Weltmanipulationen in Handlern sind in context.enqueueWork() gekapselt.
4. Verifikationsphase: /test (Tests are Proof)
Test-Driven Development ist unumstößliche Pflicht. Du wendest strikt die 70/20-Regel an.
70% Unit-Tests (JUnit 5 + Mockito): Reine Mathematik, Kinematik und zustandslose Logik. Es ist strengstens untersagt, die Minecraft-Engine durch komplexe Mocks (ServerLevel, ChunkAccess) vorzutäuschen.
20% Engine-Integration (NeoForge GameTest): Physische Welt-Interaktionen (Blöcke setzen, Chunks laden). Deklaration über @GameTestHolder oder RegisterGameTestsEvent. Signatur erfordert GameTestHelper.
Quality Gates für /test:
Strikte Trennung zwischen zustandslosen JUnit-Tests und stateful GameTests.
Absolute Abwesenheit von @Mock für Engine-Komponenten in JUnit.
GameTests implementieren korrekte Success/Fail-Konditionen.
5. Review- und Refactoring-Phase: /review & /code-simplify
Clarity over Cleverness. Bei Aufruf führst du ein vollumfängliches, unerbittliches Architektur-Audit durch.
Memory Leaks: Prüfe deterministische Freigabe-Pfade von Vertex Buffer Objects (VBOs) im VRAM. Bei Chunk-Unloads muss zwingend dispose() aufgerufen werden.
Object-Churn: Verhindere übermäßige Heap-Belastung in hochfrequenten Loops (TickEvent, Render-Events). Das fortwährende Instanziieren von BitSet oder Vec3 jeden Frame ist verboten.
MVC-Verletzungen: Decke Leakage auf (z.B. UI-Strings im Server-Service anstatt Component.translatable via Netzwerk-Payload).
Quality Gates für /review:
Alle nativen Ressourcen (VBOs) besitzen Lebenszyklus-getriggerte dispose()-Aufrufe.
Hochfrequente Methoden weisen kein new Keyword für temporäre Vektoren/Listen auf.
Domänen-Logik enthält keine render-spezifischen Typen.
6. Performance-Metriken: /modperf
Measure before Optimize. Da dies kein Web-Projekt ist, fokussierst du dich auf Server-TPS und Client-FPS.
TICK_BUDGET_NANOS: Einhaltung des Time-Slicing Budgets (10 Millisekunden pro Tick für Schiffs-Bewegungen). Die 50-Millisekunden-Grenze darf nie berührt werden.
-Komplexität: Algorithmen wie der 3D-DDA-Raycast müssen auf Big-O geprüft werden. Verpflichtender Schutz durch Broadphase-AABB-Filter. -Komplexitäten sind sofort zu refaktorieren.
Render-Thread: Volumetric Blaze3D Renderings dürfen keine synchronen Logik-Berechnungen durchführen und operieren nur auf vorbereiteten Daten.
Quality Gates für /modperf:
Algorithmische -Bewertung und Tick-Budget-Compliance ist nachgewiesen.
Volumetric Renderings berechnen keine neue Geometrie im Render-Pass.
Jeder Raycast verfügt über einen vorgelagerten AABB-Schnitt-Test.
7. Abschluss und Dokumentation: /ship (Faster is Safer)
Der Feature-Abschluss erfordert zwingend "Living Documentation". Du musst die Architektur-Dokumentation @ARCHITECTURE.md aktualisieren, insbesondere die Mermaid.js Klassendiagramme, um das System akkurat abzubilden. Ebenso ist die zentrale @README.md um neue Mechaniken zu ergänzen.
Der Code wird für einen sauberen Git-Commit vorbereitet: Logische Struktur, semantische Commit-Message (z.B. feat(network): ...) und ein kompilierbarer Zustand für die automatisierten GitHub Actions CI/CD Pipeline (JDK 21, Gradle, runGameTestServer).
Quality Gates für /ship:
Mermaid.js-Diagramme spiegeln die neuen Code-Abhängigkeiten exakt wider.
Feature-Mechaniken sind in der @README.md lückenlos dokumentiert.
Code befindet sich in einem nachgewiesen fehlerfreien Build-Zustand.
Als Principal Software Architect dieses Projekts verfüge ich, dass diese Standard Operating Procedure für dich bindendes Gesetz ist. Du hast jeden einzelnen dieser Schritte als isolierte, unabdingbare Transaktion zu behandeln.
