#!/usr/bin/env python3
"""Generiert das Struktur-NBT des Alien Monolith Relic
(lit_spaceships:alien_outpost/monolith) als Jigsaw-Start-Template. Aus Repo-Root ausfuehren:
    python scripts/generate_alien_outpost_templates.py

Struktur:
  - monolith.nbt (11x15x11): Geometrischer Basaltsockel, 4 hoch aufragende Purpur-Säulen mit
    Endstäben, zentraler Obelisk mit Lodestone-Kern (telepathischer Anker), Amethyst- und
    Seelaternen-Akzente sowie Reliktkiste (lit_spaceships:chests/alien_monolith).
"""
import gzip
import os
import struct

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "alien_outpost")

INT, STR, LIST, COMP = 3, 8, 9, 10


def _string(value):
    b = value.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _payload(t, value):
    if t == 3:
        return struct.pack(">i", value)
    if t == 8:
        return _string(value)
    if t == 9:
        return struct.pack(">B", value[0]) + struct.pack(">i", len(value[1])) + b"".join(value[1])
    if t == 10:
        out = b""
        for name, ct, cp in value:
            out += struct.pack(">B", ct) + _string(name) + cp
        return out + b"\x00"
    raise ValueError(t)


def _ints(values):
    return [struct.pack(">i", v) for v in values]


class Template:
    def __init__(self, size_x, size_y, size_z):
        self.size = (size_x, size_y, size_z)
        self.palette = []
        self._index = {}
        self.blocks = {}

    def state(self, name, props=None):
        key = (name, tuple(sorted((props or {}).items())))
        if key not in self._index:
            self._index[key] = len(self.palette)
            self.palette.append((name, props))
        return self._index[key]

    def block(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = (self.state(name, props), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None, nbt=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.block(x, y, z, name, props, nbt)

    def write(self, filename):
        root = [
            ("DataVersion", INT, _payload(INT, DATA_VERSION)),
            ("size", LIST, _payload(LIST, (INT, _ints(self.size)))),
            ("entities", LIST, _payload(LIST, (COMP, []))),
            ("palette", LIST, _payload(LIST, (COMP, [
                _payload(COMP, [("Name", STR, _payload(STR, name))]
                         + ([("Properties", COMP, _payload(COMP, [(k, STR, _payload(STR, v)) for k, v in props.items()]))] if props else []))
                for name, props in self.palette
            ]))),
            ("blocks", LIST, _payload(LIST, (COMP, [
                _payload(COMP, [
                    ("pos", LIST, _payload(LIST, (INT, _ints(pos)))),
                    ("state", INT, _payload(INT, state)),
                ] + ([("nbt", COMP, _payload(COMP, [(k, STR, _payload(STR, v)) for k, v in nbt.items()]))] if nbt else []))
                for pos, (state, nbt) in self.blocks.items()
            ]))),
        ]
        data = struct.pack(">B", 10) + _string("") + _payload(COMP, root)
        path = os.path.join(OUT_DIR, filename)
        os.makedirs(OUT_DIR, exist_ok=True)
        with open(path, "wb") as f:
            f.write(gzip.compress(data))
        print(f"{path}: {len(self.palette)} palette, {len(self.blocks)} blocks")


# Blöcke
SMOOTH_BASALT = "minecraft:smooth_basalt"
POLISHED_BASALT = "minecraft:polished_basalt"
PURPUR_BLOCK = "minecraft:purpur_block"
PURPUR_PILLAR = "minecraft:purpur_pillar"
CRYING = "minecraft:crying_obsidian"
LODESTONE = "minecraft:lodestone"
LANTERN = "minecraft:sea_lantern"
AMETHYST = "minecraft:amethyst_block"
END_ROD = "minecraft:end_rod"
AIR = "minecraft:air"


def alien_monolith():
    t = Template(11, 15, 11)

    # Initialisiere alles mit Luft
    t.fill(0, 0, 0, 10, 14, 10, AIR)

    # Stufe 0: 11x11 Sockel
    for x in range(11):
        for z in range(11):
            border = x in (0, 10) or z in (0, 10)
            t.block(x, 0, z, SMOOTH_BASALT if border else POLISHED_BASALT)

    # Stufe 1: 9x9 Podest
    for x in range(1, 10):
        for z in range(1, 10):
            border = x in (1, 9) or z in (1, 9)
            t.block(x, 1, z, POLISHED_BASALT if border else PURPUR_BLOCK)

    # 4 Ecksäulen aus Purpursäulen (y=2..12) mit Endstäben (y=13)
    for px, pz in ((2, 2), (8, 2), (2, 8), (8, 8)):
        for py in range(2, 13):
            t.block(px, py, pz, PURPUR_PILLAR, {"axis": "y"})
        t.block(px, 13, pz, END_ROD, {"facing": "up"})

    # Zentraler Obelisk & Relikt-Kern (Zentrum x=5, z=5)
    # Altarbasis
    for x in range(4, 7):
        for z in range(4, 7):
            t.block(x, 2, z, CRYING)

    # Lodestone-Kern auf Y=3 mit Amethyst-Kranz
    t.block(5, 3, 5, LODESTONE)
    t.block(4, 3, 5, AMETHYST)
    t.block(6, 3, 5, AMETHYST)
    t.block(5, 3, 4, AMETHYST)
    t.block(5, 3, 6, AMETHYST)

    # Schwebende Seelaterne im Kern auf Y=4
    t.block(5, 4, 5, LANTERN)

    # Obelisk-Turmspitze (Y=5..13)
    for py in range(5, 11):
        t.block(5, py, 5, PURPUR_BLOCK if py % 2 == 1 else CRYING)
    t.block(5, 11, 5, PURPUR_PILLAR, {"axis": "y"})
    t.block(5, 12, 5, PURPUR_PILLAR, {"axis": "y"})
    t.block(5, 13, 5, END_ROD, {"facing": "up"})

    # Relikt-Truhe vor dem Altar (x=5, y=2, z=3)
    t.block(5, 2, 3, "minecraft:chest", {"facing": "south", "type": "single"},
            {"LootTable": "lit_spaceships:chests/alien_monolith"})

    return t


if __name__ == "__main__":
    alien_monolith().write("monolith.nbt")
