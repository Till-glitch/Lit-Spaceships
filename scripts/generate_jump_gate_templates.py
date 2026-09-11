#!/usr/bin/env python3
"""Jump Gate Ruins: kolossales zerbrochenes Ringtor (15x13x15) mit Void-Kristall.
    python scripts/generate_jump_gate_templates.py
"""
import gzip
import os
import struct

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "jump_gate")


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


import math

STONE_BRICKS = "minecraft:stone_bricks"
CRACKED = "minecraft:cracked_stone_bricks"
OBSIDIAN = "minecraft:obsidian"
AMETHYST = "minecraft:amethyst_block"
LANTERN = "minecraft:sea_lantern"
END_ROD = "minecraft:end_rod"
CHEST = "minecraft:chest"
AIR = "minecraft:air"

W, H, L = 15, 13, 15
cx, cz = 7, 7
radius = 6.0
t = Template((W, H, L))

# Ring in der X/Y-Ebene (Dicke 1, Radius 6) mit Bruch im oberen Segment
broken_angles = set()
for a in range(60, 105):  # Bruch von ~60..105 Grad (oberer rechter Bogen fehlt)
    broken_angles.add(a)

for deg in range(0, 360, 2):
    if deg in broken_angles:
        continue
    rad = math.radians(deg)
    px = radius * math.cos(rad)
    py = radius * math.sin(rad)
    x = int(round(cx + px))
    y = int(round(6 + py))
    if 0 <= x < W and 0 <= y < H:
        mat = CRACKED if (deg % 10) in (0, 2) else STONE_BRICKS
        # Ring in der Tiefe: 2 Ebenen (z=cz-1, cz)
        t.block(x, y, cz - 1, mat)
        t.block(x, y, cz, OBSIDIAN if deg % 45 == 0 else mat)

# Void-Kristall im Zentrum: Amethyst-Kern mit Leuchten + Endstaebe
t.block(cx, 6, cz, AMETHYST)
t.block(cx, 7, cz, LANTERN)
t.block(cx - 1, 7, cz, END_ROD, {"facing": "west"})
t.block(cx + 1, 7, cz, END_ROD, {"facing": "east"})

# Gate-Basis: Basaltsaeulen unter den Ringpfosten
for x in (cx - 6, cx + 6):
    t.block(x, 5, cz, "minecraft:basalt")
    t.block(x, 4, cz, "minecraft:basalt")

# Vergrabener Cache unter dem Zentrum
t.block(cx, 2, cz, BLACK := "minecraft:polished_blackstone_bricks")
t.fill(cx - 1, 1, cz - 1, cx + 1, 1, cz + 1, BLACK)
t.block(cx, 1, cz, CHEST, {"facing": "up", "type": "single"},
        {"LootTable": "lit_spaceships:chests/gate_cache"})

t.write("gate.nbt")
