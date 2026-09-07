#!/usr/bin/env python3
"""Generiert die 4 Jigsaw-Module der Abandoned Orbital Research Station
(lit_spaceships:space_station) als Struktur-NBTs. Aus Repo-Root ausfuehren:
    python scripts/generate_space_station_templates.py

Alle Verbindungen liegen auf lokalem Y=2 => alle Module ergeben bündige Böden.
Jigsaw-Konvention: Hub-/OUT-Connector  name=station_out target=station_in;
IN-Connector         name=station_in  target=station_out; pool = rooms-Pool.
"""
import gzip
import os
import struct

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "space_station")

ROOMS_POOL = "lit_spaceships:space_station/rooms"


# ---------- NBT-Writer (int=3, string=8, list=9, compound=10) ----------

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


def _named(name, t, value):
    return struct.pack(">B", t) + _string(name) + _payload(t, value)


INT, STR, LIST, COMP = 3, 8, 9, 10


def _ints(values):
    return [struct.pack(">i", v) for v in values]


# ---------- Template-Builder ----------

class Template:
    def __init__(self, size_x, size_y, size_z):
        self.size = (size_x, size_y, size_z)
        self.palette = []
        self._index = {}
        self.blocks = {}  # (x, y, z) -> (state_idx, nbt|None)

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

    def jigsaw(self, x, y, z, orientation, name, target):
        self.block(x, y, z, "minecraft:jigsaw", {"orientation": orientation}, {
            "name": name,
            "target": target,
            "pool": ROOMS_POOL,
            "final_state": "minecraft:air",
            "joint": "rollable",
        })

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


# ---------- Hilfen ----------

IRON = "minecraft:iron_block"
GRAY = "minecraft:gray_concrete"
WHITE = "minecraft:white_concrete"
GLASS = "minecraft:glass"
PANEL = "minecraft:light_blue_stained_glass"
LANTERN = "minecraft:sea_lantern"
SMOOTH = "minecraft:smooth_stone"
COPPER = "minecraft:copper_block"
MAGMA = "minecraft:magma_block"
AIR = "minecraft:air"

IN_NAME, OUT_NAME = "lit_spaceships:station_in", "lit_spaceships:station_out"


def hub_room(t, w, h, l, wall, floor, ceil, window_x, window_z):
    """Raum mit Boden/Rand, Wänden, Fensterstreifen, Deckenleuchten, Luft-Innenraum."""
    # Boden (0) + Decke (h-1)
    for x in range(w):
        for z in range(l):
            border = x in (0, w - 1) or z in (0, l - 1)
            t.block(x, 0, z, IRON if border else floor)
            t.block(x, h - 1, z, ceil)
    # Wände y1..h-2
    for x in range(w):
        for z in range(l):
            if x not in (0, w - 1) and z not in (0, l - 1):
                continue
            for y in range(1, h - 1):
                is_window = y in (2, 3) and (
                    (z in (0, l - 1) and x in window_x) or (x in (0, w - 1) and z in window_z))
                t.block(x, y, z, GLASS if is_window else wall)
    # Innenluft
    t.fill(1, 1, 1, w - 2, h - 2, l - 2, AIR)


def dock_hub():
    t = Template(11, 7, 11)
    hub_room(t, 11, 7, 11, IRON, GRAY, IRON, window_x=range(3, 8), window_z=range(3, 8))
    for cx, cz in ((3, 3), (7, 3), (3, 7), (7, 7)):
        t.block(cx, 6, cz, LANTERN)
    t.jigsaw(10, 2, 5, "east_up", OUT_NAME, IN_NAME)
    t.jigsaw(0, 2, 5, "west_up", OUT_NAME, IN_NAME)
    return t


def solar_wing():
    t = Template(11, 5, 7)
    for x in range(11):
        for z in range(7):
            t.block(x, 0, z, GRAY)
            t.block(x, 4, z, IRON)
    for x in range(11):
        for z in range(7):
            if x in (0, 10) or z in (0, 6):
                for y in (1, 2, 3):
                    panel = z in (0, 6) and 1 <= x <= 9 and y == 2
                    t.block(x, y, z, PANEL if panel else IRON)
    t.fill(1, 1, 1, 9, 3, 5, AIR)
    t.block(3, 4, 3, LANTERN)
    t.block(7, 4, 3, LANTERN)
    t.jigsaw(0, 2, 3, "west_up", IN_NAME, OUT_NAME)
    t.jigsaw(10, 2, 3, "east_up", OUT_NAME, IN_NAME)
    return t


def laboratory():
    t = Template(11, 7, 11)
    hub_room(t, 11, 7, 11, WHITE, WHITE, WHITE, window_x=range(3, 8), window_z=range(3, 8))
    t.fill(2, 1, 1, 8, 1, 1, SMOOTH)   # Arbeitsbank Nordwand
    t.fill(2, 1, 9, 8, 1, 9, SMOOTH)   # Arbeitsbank Südwand
    t.block(5, 1, 5, "minecraft:chest", {"facing": "south", "type": "single"},
            {"LootTable": "lit_spaceships:chests/space_station_core"})
    t.block(5, 6, 5, LANTERN)
    t.block(2, 6, 2, LANTERN)
    t.block(8, 6, 2, LANTERN)
    t.block(2, 6, 8, LANTERN)
    t.block(8, 6, 8, LANTERN)
    t.jigsaw(0, 2, 5, "west_up", IN_NAME, OUT_NAME)
    return t


def reactor_room():
    t = Template(9, 7, 9)
    hub_room(t, 9, 7, 9, IRON, GRAY, IRON, window_x=range(3, 6), window_z=range(3, 6))
    t.block(4, 1, 4, COPPER)
    t.block(4, 2, 4, MAGMA)
    t.block(4, 3, 4, COPPER)
    t.block(4, 6, 4, LANTERN)
    t.jigsaw(0, 2, 4, "west_up", IN_NAME, OUT_NAME)
    return t


if __name__ == "__main__":
    dock_hub().write("docking_hub.nbt")
    solar_wing().write("solar_wing.nbt")
    laboratory().write("laboratory.nbt")
    reactor_room().write("reactor_room.nbt")
