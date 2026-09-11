#!/usr/bin/env python3
"""Frozen Leviathan: kolossales Knochen-Skelett (17x8x21) im Frozen Expanse.
Rippen, Wirbelsaeule, Schaedel mit blauem Eiskern — und ein Hoard im Herzen.
    python scripts/generate_leviathan_templates.py
"""
import gzip
import os
import struct

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "leviathan_bones")


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


BONE = "minecraft:bone_block"
BLUE_ICE = "minecraft:blue_ice"
PACKED = "minecraft:packed_ice"
SNOW = "minecraft:snow_block"
MAGMA = "minecraft:magma_block"
AIR = "minecraft:air"

W, H, L = 17, 8, 21
cx = 8
t = Template((W, H, L))

# Frostboden (gepacktes Eis) ueber die ganze Flaeche
t.fill(0, 0, 0, W - 1, 0, L - 1, PACKED)

# Wirbelsaeule entlang Z (Mitte), leicht aufgebahrt
for z in range(4, L - 2):
    t.block(cx, 1, z, BONE)

# Schaedel am Kopf (z 0..3): Knochenbox mit blauem Eiskern und Augenhöhlen
t.fill(cx - 3, 1, 0, cx + 3, 4, 3, BONE)
t.fill(cx - 2, 2, 1, cx + 2, 3, 2, AIR)
t.block(cx - 1, 2, 1, BLUE_ICE)
t.block(cx + 1, 2, 1, BLUE_ICE)
t.block(cx, 2, 1, MAGMA)

# Herz der Bestie: blaues Eis + Magma auf der Wirbelsaeule, Hoard-Kiste darunter
t.block(cx, 2, 10, BLUE_ICE)
t.block(cx, 3, 10, MAGMA)
t.block(cx, 1, 10, "minecraft:chest", {"facing": "east", "type": "single"},
        {"LootTable": "lit_spaceships:chests/leviathan_hoard"})

# Rippenpaare: abnehmende Hoehe vom Kopf zum Schwanz
rib_heights = [6, 6, 5, 5, 4, 4, 3, 3]
for i, h in enumerate(rib_heights):
    z = 5 + i * 2
    spread = 2 + (h - 3)
    for x in (cx - spread, cx + spread):
        for y in range(1, h + 1):
            t.block(x, y, z, BONE)
        # Rippenbogen oben zur Mitte ziehen
        if h >= 5:
            t.block(cx - spread + 1, h, z, BONE)
            t.block(cx + spread - 1, h, z, BONE)

# Schneeflocken-Deko: Schneebloecke auf dem Eis
for x, z in ((2, 6), (13, 8), (4, 14), (12, 17), (cx, 19), (2, 2), (14, 2)):
    t.block(x, 1, z, SNOW)

t.write("skeleton.nbt")
