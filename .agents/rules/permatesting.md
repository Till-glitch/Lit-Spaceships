---
trigger: always_on
---

REGEL: KONTINUIERLICHES TESTING (DEFINITION OF DONE)

Ab sofort gilt für JEDE Feature-Implementierung eine strikte "Kein Code ohne Test"-Regel. Deine Arbeit an einem Feature ist erst abgeschlossen, wenn die dazugehörigen automatisierten Tests generiert wurden.

Halte dich beim Schreiben neuer Features an folgenden Workflow:

1. Test-Kategorisierung (Entscheide vor dem Coden):

    Ist es reine Mathematik, Domain-Logik (ShipState), Payload-Serialisierung oder ein Service, der isoliert werden kann? -> JUnit 5 Test (mit Mockito) schreiben.

    Verändert es die Minecraft-Welt (Blöcke setzen, Entities spawnen, Chunk-Loading)? -> NeoForge GameTest (@GameTest) schreiben.

    Ist es reines Rendering (Blaze3D, Shaders, UI-Screens)? -> Kein Test (manuelle QA).

2. Edge-Cases abdecken:
Schreibe nicht nur den "Happy Path" (den Idealfall). Schreibe immer mindestens einen Test für den Worst-Case (z.B. Spieler hat zu wenig Energie, Block ist unzerstörbar, Schiffskoordinaten sind ungültig).

3. Liefer-Format:
Wenn ich dich beauftrage, ein Feature (z. B. "Laserwaffen" oder "Neue Dimension") zu implementieren, musst du in deiner Code-Ausgabe immer am Ende die entsprechende Test-Klasse (z. B. LaserMathTest.java oder LaserGameTest.java) mitliefern.

Akzeptiere keine Ausreden. Ohne begleitenden Test ist der Code ungültig.