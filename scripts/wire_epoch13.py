# -*- coding: utf-8 -*-
"""Epoch 13: FlightRecorder-Registrierung + i18n (en/de) + Dokumentations-Updates."""

mi = 'src/main/java/com/lit/spaceships/registry/ModItems.java'
s = open(mi, encoding='utf-8').read()
if 'FLIGHT_RECORDER' not in s:
    s = s.replace('import com.lit.spaceships.item.SignalScopeItem;',
'''import com.lit.spaceships.item.FlightRecorderItem;
import com.lit.spaceships.item.SignalScopeItem;''')
    anchor = '''    public static final DeferredItem<com.lit.spaceships.item.AccessCipherItem> ACCESS_CIPHER =
            ITEMS.register("access_cipher", () -> new com.lit.spaceships.item.AccessCipherItem(
                    new Item.Properties().stacksTo(1).durability(8)));'''
    assert anchor in s
    s = s.replace(anchor, anchor + '''

    public static final DeferredItem<FlightRecorderItem> FLIGHT_RECORDER =
            ITEMS.register("flight_recorder", () -> new FlightRecorderItem(
                    new Item.Properties().stacksTo(4)));''')
    open(mi, 'w', encoding='utf-8').write(s)
print('flight recorder registered')

# Item-Modell (Parent = generiertes Item, nutzt backflip_tool-Textur als Platzhalter-Disc)
im = 'src/main/java/com/lit/spaceships/datagen/provider/ModItemModelProvider.java'
s = open(im, encoding='utf-8').read()
if 'FLIGHT_RECORDER' not in s:
    anchor = '''        withExistingParent(ModItems.SIGNALSCOPE.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/backflip_tool"));'''
    assert anchor in s
    s = s.replace(anchor, anchor + '''

        withExistingParent(ModItems.FLIGHT_RECORDER.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/backflip_tool"));''')
    open(im, 'w', encoding='utf-8').write(s)
print('item model wired')

# i18n en/de: Item-Name + Header + 5 Logs + 4 Clues + decrypted
en = 'src/main/java/com/lit/spaceships/datagen/provider/ModEnglishLanguageProvider.java'
s = open(en, encoding='utf-8').read()
if 'flight_recorder.log1' not in s:
    anchor = 'add("item.lit_spaceships.access_cipher", "Access Cipher");'
    assert anchor in s
    s = s.replace(anchor, anchor + '''
        add("item.lit_spaceships.flight_recorder", "Flight Recorder");
        add("item.lit_spaceships.flight_recorder.header", "== FLIGHT DATA RECOVERY ==");
        add("item.lit_spaceships.flight_recorder.log1", "Day 12: Hull integrity failing. We sealed the cargo decks, but the ringing in the walls won't stop.");
        add("item.lit_spaceships.flight_recorder.log2", "Day 30: The rift pulls at our instruments. Navigation is a guess now. The stars here are wrong.");
        add("item.lit_spaceships.flight_recorder.log3", "Day 55: Reactor stage four. Coolant lines frozen. Whoever finds this - cut the panels, do not trust the core.");
        add("item.lit_spaceships.flight_recorder.log4", "Day 61: We picked up a research beacon on every channel. Someone is still transmitting out there.");
        add("item.lit_spaceships.flight_recorder.log5", "Day 88: Last entry. The colonists sealed the dome and went quiet. May the void keep them.");
        add("item.lit_spaceships.flight_recorder.clue_freighter", "Transponder chain resolved: freighter wreckage in the deep space lanes, cell drift %1$s");
        add("item.lit_spaceships.flight_recorder.clue_relay", "Relay intercept: an antenna lattice hums beyond the nebula veil, drift %1$s");
        add("item.lit_spaceships.flight_recorder.clue_collector", "Thermal bloom detected: solar collectors near the corona, drift %1$s");
        add("item.lit_spaceships.flight_recorder.clue_outpost", "Deep signal: a listening post hides inside a rift asteroid, drift %1$s");
        add("item.lit_spaceships.flight_recorder.decrypted", "== DATA DECRYPTED ==");''')
    open(en, 'w', encoding='utf-8').write(s)

de = 'src/main/java/com/lit/spaceships/datagen/provider/ModGermanLanguageProvider.java'
s = open(de, encoding='utf-8').read()
if 'flight_recorder.log1' not in s:
    anchor = 'add("item.lit_spaceships.access_cipher", "Zugriffsschluessel");'
    assert anchor in s
    s = s.replace(anchor, anchor + '''
        add("item.lit_spaceships.flight_recorder", "Flugschreiber");
        add("item.lit_spaceships.flight_recorder.header", "== FLUGDATEN-WIEDERHERSTELLUNG ==");
        add("item.lit_spaceships.flight_recorder.log1", "Tag 12: Huellenintegritaet bricht. Wir haben die Frachtdecks versiegelt, aber das Klingeln in den Waenden endet nicht.");
        add("item.lit_spaceships.flight_recorder.log2", "Tag 30: Die Spalte zieht an unseren Instrumenten. Navigation ist jetzt Raten. Die Sterne hier sind falsch.");
        add("item.lit_spaceships.flight_recorder.log3", "Tag 55: Reaktor Stufe vier. Kuehlleitungen eingefroren. Wer das findet - schneidet die Paneele, vertraut dem Kern nicht.");
        add("item.lit_spaceships.flight_recorder.log4", "Tag 61: Wir hoerten ein Forschungssignal auf jedem Kanal. Da draussen sendet noch jemand.");
        add("item.lit_spaceships.flight_recorder.log5", "Tag 88: Letzter Eintrag. Die Kolonisten versiegelten die Kuppel und schwiegen. Die Leere behuetet sie.");
        add("item.lit_spaceships.flight_recorder.clue_freighter", "Transponderkette aufgeloest: Frachterwrack in den Tiefraum-Lanes, Zellabweichung %1$s");
        add("item.lit_spaceships.flight_recorder.clue_relay", "Relais-Abfang: Ein Antennengitter summt hinter dem Nebelschleier, Abweichung %1$s");
        add("item.lit_spaceships.flight_recorder.clue_collector", "Thermische Anomalie: Solarkollektoren nahe der Corona, Abweichung %1$s");
        add("item.lit_spaceships.flight_recorder.clue_outpost", "Tiefensignal: Ein Horchposten verbirgt sich im Rift-Asteroiden, Abweichung %1$s");
        add("item.lit_spaceships.flight_recorder.decrypted", "== DATEN ENTSCHLUESSELT ==");''')
    open(de, 'w', encoding='utf-8').write(s)
print('i18n wired')
