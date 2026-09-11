#!/usr/bin/env python3
"""Pirate Satellite Outpost: zwielichtige Blackstone-Plattform (11x6x11) mit
Fallen-Kiste (TNT direkt unterm Cache!). Aus Repo-Root:
    python scripts/generate_pirate_outpost_templates.py
"""
import gzip
import os
import struct

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "pirate_outpost")


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


INT, STR, LIST, COMP = 3, 8, 9, 10


def _ints(values):
    return [struct.pack(">i", v) for v in values]


class Template:
    def __init__(self, size):
        self.size = size
        self.palette = []
        self._index = {}
        self._blocks = {}   # ein Eintrag pro Position (last write wins)

    def state(self, name, props=None):
        key = (name, tuple(sorted((props or {}).items())))
        if key not in self._index:
            self._index[key] = len(self.palette)
            self.palette.append((name, props))
        return self._index[key]

    def block(self, x, y, z, name, props=None, nbt=None):
        self._blocks[(x, y, z)] = (self.state(name, props), nbt)

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
                for pos, (state, nbt) in self._blocks.items()
            ]))),
        ]
        data = struct.pack(">B", 10) + _string("") + _payload(COMP, root)
        path = os.path.join(OUT_DIR, filename)
        os.makedirs(OUT_DIR, exist_ok=True)
        with open(path, "wb") as f:
            f.write(gzip.compress(data))
        print(f"{path}: {len(self.palette)} palette, {len(self._blocks)} blocks")


BLACK = "minecraft:blackstone"
BRICKS = "minecraft:polished_blackstone_bricks"
CHISELED = "minecraft:chiseled_polished_blackstone"
BARS = "minecraft:iron_bars"
SOUL = "minecraft:soul_lantern"
AIR = "minecraft:air"

t = Template((11, 6, 11))

# Offene Plattform: Deckplatten-Ebene y2 (x0..10, z0..10), Luftraum darunter mit Stelzen
t.fill(0, 2, 0, 10, 2, 10, BRICKS)
t.fill(0, 0, 0, 10, 1, 10, AIR)

# Stelzen (4 Ecken, doppelte Pfosten) + Mittelstelzen
for cx, cz in ((0, 0), (10, 0), (0, 10), (10, 10), (5, 0), (5, 10), (0, 5), (10, 5)):
    t.block(cx, 1, cz, BLACK)
    t.block(cx, 0, cz, BLACK)

# Plattform-Deko: Flicken aus Blackstone + Chiseled-Bordstein
for x, z in ((2, 2), (7, 3), (3, 8), (8, 7), (5, 5)):
    t.block(x, 2, z, BLACK)
for x in range(0, 11):
    for z in (0, 10):
        if (x % 3) == 0:
            t.block(x, 2, z, CHISELED)
for z in range(0, 11):
    for x in (0, 10):
        if (z % 3) == 0:
            t.block(x, 2, z, CHISELED)

# Geländer: Iron Bars an der Nord- und Süd-Kante (y3)
for x in range(0, 11):
    t.block(x, 3, 0, BARS)
    t.block(x, 3, 10, BARS)

# Beute: Cache-Kiste auf TNT-Falle! (Kiste (5,3,5), TNT (5,2,5) in der Plattform)
t.block(5, 2, 5, "minecraft:tnt")
t.block(5, 3, 5, "minecraft:chest", {"facing": "south", "type": "single"},
        {"LootTable": "lit_spaceships:chests/pirate_cache"})

# Piraten-Deko: Fässer (Barrels), Seelenlaternen an den Stelzen
t.block(3, 3, 5, "minecraft:barrel", {"facing": "up", "open": "false"})
t.block(7, 3, 5, "minecraft:barrel", {"facing": "up", "open": "false"})
t.block(0, 3, 0, SOUL)
t.block(10, 3, 10, SOUL)

t.write("platform.nbt")
