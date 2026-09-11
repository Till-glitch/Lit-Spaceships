#!/usr/bin/env python3
"""The Silent Monolith: schwarzer Monolith (3x8x2) auf einem treibenden
Void-Rock (9x14x9) mit VERBORGENER Kammer im Gestein darunter.
    python scripts/generate_monolith_templates.py
"""
import gzip
import os
import struct

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "the_monolith")


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
        self._blocks = {}

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


OBSIDIAN = "minecraft:obsidian"
DEEPSLATE = "minecraft:deepslate"
BLACK_BRICKS = "minecraft:polished_blackstone_bricks"
BASALT = "minecraft:basalt"
GOLD = "minecraft:gold_block"
DIAMOND_ORE = "minecraft:diamond_ore"
AIR = "minecraft:air"

cx = 4
t = Template((9, 14, 9))

# Treibender Void-Rock: Deepslate-Block von y0..3 (mit Ausnahme der Kammer)
t.fill(0, 0, 0, 8, 3, 8, DEEPSLATE)

# Verborgene Kammer im Rock (polierte Blackstone-Bricks, 5x3x5 bei y1..3)
t.fill(cx - 2, 1, cx - 2, cx + 2, 3, cx + 2, BLACK_BRICKS)
t.fill(cx - 1, 1, cx - 1, cx + 1, 2, cx + 1, AIR)

# Das Geheimnis: Opfergaben + Kiste
t.block(cx, 1, cx, "minecraft:chest", {"facing": "north", "type": "single"},
        {"LootTable": "lit_spaceships:chests/monolith_secret"})
t.block(cx - 1, 1, cx, GOLD)
t.block(cx + 1, 1, cx, DIAMOND_ORE)
t.block(cx, 3, cx, "minecraft:sea_lantern")

# Basalt-Sockel (y4) und der Monolith (3x8x2, y5..12)
t.fill(cx - 3, 4, cx - 3, cx + 3, 4, cx + 3, BASALT)
t.fill(cx - 1, 5, cx - 1, cx + 1, 12, cx, OBSIDIAN)

# Krone: Endstaebe (kein lautes Leuchten - der Monolith bleibt still)
t.block(cx - 1, 13, cx - 1, "minecraft:end_rod", {"facing": "up"})
t.block(cx + 1, 13, cx, "minecraft:end_rod", {"facing": "up"})

t.write("monolith.nbt")
