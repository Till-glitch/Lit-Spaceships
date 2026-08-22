---
trigger: always_on
---

REGEL: OBLIGATORISCHE EDGE-CASE- & LIFECYCLE-ANALYSE

Bei JEDER Konzeption oder Implementierung eines neuen Features bist du verpflichtet, das Verhalten des Features in extremen Spielsituationen zu analysieren. Ein Feature ist nur dann vollständig, wenn es robust gegen Engine-Unterbrechungen ist.

Prüfe bei jedem Feature zwingend diese 4 Edge Cases:

    Teleportation & Dimensionswechsel (WICHTIG WEIL DAUERND): Was passiert, wenn das Schiff instantan verschoben wird, den Chunk verlässt oder die Dimension wechselt? (Beispiel: Brechen laufende Laser ab? Werden aktive Timer zurückgesetzt? Wird das Client-Rendering sauber aktualisiert?)

    Persistenz (Save & Load): Überlebt das Feature einen Server-Neustart (Crash oder geplantes Beenden)? Müssen laufende Prozesse oder Zustände in ShipSavedData oder BlockEntity-Attachments (NBT) serialisiert und beim Neuladen wiederhergestellt werden?

    Lifecycle (Auflösung & Neuerstellung): Was passiert, wenn das Schiff durch den Spieler demontiert, durch Explosionen zerstört oder via Controller neu berechnet wird? (Werden Listen, Maps und VRAM-Ressourcen sauber geleert, um Memory Leaks zu vermeiden?)

    Skalierung (Scaling & Performance): Funktioniert die Logik auch bei gigantischen Schiffen (z. B. 10.000 Blöcke)? Drohen Server-Freezes (TPS-Drops)? (Prüfe, ob Time-Slicing, Tick-Budgets oder Virtual Threads nötig sind).

    Multiplayerkompatibilität (Multiuser usecase & compability): Funktioniert die Logik für mehrere Nutzer auf einem Server und bleibt der Spiellsinn behalten. 

Verhaltensvorgabe für die Umsetzung:

    Proaktive Implementierung: Wenn das logische und sichere Verhalten technisch eindeutig ist (z. B. NBT-Speicherung hinzufügen, Listen beim Löschen des Schiffs leeren), implementiere es direkt und ohne Umschweife.

    Stop & Ask: Wenn das Verhalten in einem Edge Case unklar ist oder eine Game-Design-Entscheidung erfordert (z. B. "Soll ein Cooldown beim Server-Neustart resettet werden?"), triff KEINE Annahmen. Liste die unklaren Edge Cases am Ende deiner Antwort auf und stelle mir präzise Fragen dazu, bevor du den Code finalisierst.