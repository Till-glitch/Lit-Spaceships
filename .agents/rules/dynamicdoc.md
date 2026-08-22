---
trigger: always_on
---

REGEL: KONTINUIERLICHE DOKUMENTATION (LIVING DOCUMENTATION)

Code-Änderungen existieren nicht isoliert. Die Projektdokumentation ist ein "First-Class Citizen". Ein Feature oder Refactoring gilt erst dann als zu 100 % abgeschlossen, wenn die zentralen Dokumentationsdateien aktualisiert wurden.

Wenn du Code implementierst, erweiterst oder löschst, bist du verpflichtet, abschließend folgende Dateien zu prüfen und anzupassen:

1. @README.md (Das Handbuch):

    Füge neue Gameplay-Features, Items, Blöcke oder Mechaniken (z.B. Laser, Dimensionen) in die "Features"-Liste ein.

    Aktualisiere die Erklärungen für den Spieler (z. B. wie man neue Systeme bedient, craftet oder mit Energie versorgt).

    Entferne veraltete Informationen.

2. @architecture.md / @Target_architecture.mmd (Der Blueprint):

    Halte das Klassendiagramm (Mermaid) strikt aktuell.

    Füge neue Services (z.B. LaserAimingService), Payloads (z.B. ShipImpactEventPayload) oder Domain-Objekte (LaserNode) in das Diagramm ein.

    Ziehe die korrekten Abhängigkeits-Pfeile (wer ruft wen auf, wer sendet welches Paket).

    Dokumentiere neue Architektur-Entscheidungen (z. B. "Laser nutzen Raycasting im Local-Space des VoxelGridCache").

Verhaltensvorgabe:
Am Ende deines Implementierungs-Workflows (nachdem der Code und die Tests geschrieben wurden), musst du proaktiv den aktualisierten Inhalt für @README.md und @architecture.md generieren. Verstecke diese Updates nicht in Fließtext, sondern gib sie als saubere Code-Blöcke aus, die ich direkt übernehmen kann.