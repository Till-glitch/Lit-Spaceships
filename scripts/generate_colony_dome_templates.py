#!/usr/bin/env python3
"""Abandoned Colony Dome: ueberwucherte Glaskuppel (13x10x13) mit Habitat-Pods,
Lager-Barrels und Vorrats-Kiste. Amethyst hat die Kolonie uebernommen.
    python scripts/generate_colony_dome_templates.py
"""
import gzip
import os
import struct
import math

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "colony_dome")


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


GLASS = "minecraft:glass"
FLOOR = "minecraft:gray_concrete"
TILE = "minecraft:light_gray_concrete"
IRON = "minecraft:iron_block"
AMETHYST = "minecraft:amethyst_block"
BUD = "minecraft:budding_amethyst"
CAMPFIRE = "minecraft:campfire"
AIR = "minecraft:air"

W, H, L = 13, 10, 13
cx, cz = 6, 6
t = Template((W, H, L))

# Boden
t.fill(0, 0, 0, W - 1, 0, L - 1, FLOOR)
for x in range(0, W, 2):
    for z in range(0, L, 2):
        t.block(x, 0, z, TILE)

# Glaskuppel (Halbkugel r=6), Eingangsloch auf der Suedseite
r = 6.0
for x in range(W):
    for z in range(L):
        for y in range(1, H):
            dx, dz = x - cx, z - cz
            dist = math.sqrt(dx * dx + (y - 0.5) * (y - 0.5) * 1.2 + dz * dz)
            if abs(dist - r) < 0.75:
                # Eingang: Sueden (z gross), y 1-2, x 5..7 offen
                if z > cz + 3 and y <= 2 and 5 <= x <= 7:
                    continue
                t.block(x, y, z, GLASS)

# Lagerfeuer der Siedler + Habitat-Pods (Eisen-Nischen mit Barrels)
t.block(cx, 1, cz, CAMPFIRE, {"facing": "north", "lit": "true"})
for px, pz, fx in ((2, 2, IRON), (10, 2, IRON), (2, 10, IRON), (10, 10, IRON)):
    t.block(px, 1, pz, "minecraft:barrel", {"facing": "up", "open": "false"})

# Die Kolonie ist ueberwuchert: Amethyst buerst sich durch den Boden
overgrowth = [(3, 3), (9, 4), (4, 9), (8, 9), (6, 3), (3, 7)]
for i, (gx, gz) in enumerate(overgrowth):
    t.block(gx, 1, gz, AMETHYST if i % 3 == 0 else BUD)

# Vorrats-Kiste neben dem Lagerfeuer
t.block(cx + 2, 1, cz, "minecraft:chest", {"facing": "west", "type": "single"},
        {"LootTable": "lit_spaceships:chests/colony_larder"})

t.write("dome.nbt")
