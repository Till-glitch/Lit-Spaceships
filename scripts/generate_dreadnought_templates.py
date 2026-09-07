#!/usr/bin/env python3
"""Generiert die 3 Jigsaw-Module des Derelict Dreadnought Warship
(lit_spaceships:dreadnought_wreck) als Struktur-NBTs. Aus Repo-Root ausfuehren:
    python scripts/generate_dreadnought_templates.py

Module:
  - command_bridge.nbt (START, 13x7x13): Gepanzertes Brückenmodul, rote Glasscheiben, Kontrollstationen.
  - corridor_breached.nbt (SECTIONS, 11x7x13): Durchbrochener Rumpf, offen zum Vakuum, Magma-/Obsidian-Risse.
  - engineering_core.nbt (SECTIONS, 13x9x13): Instabiler Reaktorkern (Netherit/Magma/Crying Obsidian) + Waffenkammer-Kiste.
"""
import gzip
import os
import struct

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "dreadnought_wreck")
SECTIONS_POOL = "lit_spaceships:dreadnought_wreck/sections"

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

    def jigsaw(self, x, y, z, orientation, name, target, pool=SECTIONS_POOL):
        self.block(x, y, z, "minecraft:jigsaw", {"orientation": orientation}, {
            "name": name,
            "target": target,
            "pool": pool,
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


# Blöcke
BLACKSTONE = "minecraft:polished_blackstone"
BRICKS = "minecraft:polished_blackstone_bricks"
DEEPSLATE = "minecraft:polished_deepslate"
CRYING = "minecraft:crying_obsidian"
BASALT = "minecraft:smooth_basalt"
RED_GLASS = "minecraft:red_stained_glass"
RED_LAMP = "minecraft:redstone_lamp"
MAGMA = "minecraft:magma_block"
NETHERITE = "minecraft:netherite_block"
LANTERN = "minecraft:sea_lantern"
IRON_BARS = "minecraft:iron_bars"
AIR = "minecraft:air"

IN_NAME = "lit_spaceships:dreadnought_in"
OUT_NAME = "lit_spaceships:dreadnought_out"


def command_bridge():
    """START-Modul (13x7x13): Gepanzertes Brückenmodul mit Sichtfenster im Norden."""
    t = Template(13, 7, 13)
    # Boden & Decke
    for x in range(13):
        for z in range(13):
            border = x in (0, 12) or z in (0, 12)
            t.block(x, 0, z, BLACKSTONE if border else BASALT)
            t.block(x, 6, z, BRICKS if border else DEEPSLATE)

    # Wände
    for x in range(13):
        for z in range(13):
            if x not in (0, 12) and z not in (0, 12):
                continue
            for y in range(1, 6):
                # Nordfenster (Sichtbrücke)
                if z == 0 and 4 <= x <= 8 and y in (2, 3):
                    t.block(x, y, z, RED_GLASS)
                elif (x + y + z) % 7 == 0:
                    t.block(x, y, z, CRYING)
                else:
                    t.block(x, y, z, BRICKS)

    # Innenluft
    t.fill(1, 1, 1, 11, 5, 11, AIR)

    # Deckenlampen
    for lx, lz in ((3, 3), (9, 3), (3, 9), (9, 9)):
        t.block(lx, 6, lz, RED_LAMP)

    # Kontrollkonsolen an der Front
    for cx in (4, 5, 7, 8):
        t.block(cx, 1, 1, DEEPSLATE)
        t.block(cx, 1, 2, IRON_BARS)

    # Ausgang nach Süden (Connector)
    t.jigsaw(6, 2, 12, "south_up", OUT_NAME, IN_NAME)
    return t


def corridor_breached():
    """SECTIONS-Modul (11x7x13): Beschädigter Verbindungskorridor mit Rumpfbruch nach Osten."""
    t = Template(11, 7, 13)
    # Boden & Decke
    for x in range(11):
        for z in range(13):
            border = x in (0, 10) or z in (0, 12)
            # Rumpfbruch im Boden bei x >= 7, z=5..7
            if x >= 7 and 5 <= z <= 7:
                t.block(x, 0, z, MAGMA if (x + z) % 2 == 0 else AIR)
            else:
                t.block(x, 0, z, BLACKSTONE if border else BASALT)

            # Decke
            if x >= 7 and 5 <= z <= 7:
                t.block(x, 6, z, AIR)  # Durchschlag nach oben
            else:
                t.block(x, 6, z, BRICKS)

    # Wände
    for x in range(11):
        for z in range(13):
            if x not in (0, 10) and z not in (0, 12):
                continue
            for y in range(1, 6):
                # Großer Rumpfbruch an der Ostwand (x=10, z=4..8, y=1..5)
                if x == 10 and 4 <= z <= 8:
                    if y in (1, 5) and (z == 4 or z == 8):
                        t.block(x, y, z, CRYING)
                    else:
                        t.block(x, y, z, AIR)  # Rumpfbruch ins All!
                elif (x + y + z) % 5 == 0:
                    t.block(x, y, z, CRYING)
                elif (x + y + z) % 6 == 0:
                    t.block(x, y, z, MAGMA)
                else:
                    t.block(x, y, z, BRICKS)

    # Innenluft
    t.fill(1, 1, 1, 9, 5, 11, AIR)

    # Magma & Crying Obsidian Trümmer im Korridor
    t.block(3, 1, 5, MAGMA)
    t.block(4, 1, 8, CRYING)
    t.block(5, 5, 6, IRON_BARS)

    # Jigsaws: Eingang im Norden, Ausgang im Süden
    t.jigsaw(5, 2, 0, "north_up", IN_NAME, OUT_NAME)
    t.jigsaw(5, 2, 12, "south_up", OUT_NAME, IN_NAME)
    return t


def engineering_core():
    """SECTIONS-Modul (13x9x13): Instabiler Reaktor & Waffenkammer."""
    t = Template(13, 9, 13)
    # Boden & Decke
    for x in range(13):
        for z in range(13):
            border = x in (0, 12) or z in (0, 12)
            t.block(x, 0, z, BLACKSTONE if border else BASALT)
            t.block(x, 8, z, BRICKS)

    # Wände
    for x in range(13):
        for z in range(13):
            if x not in (0, 12) and z not in (0, 12):
                continue
            for y in range(1, 8):
                if (x + y + z) % 6 == 0:
                    t.block(x, y, z, CRYING)
                else:
                    t.block(x, y, z, BRICKS)

    # Innenluft
    t.fill(1, 1, 1, 11, 7, 11, AIR)

    # Instabiler Reaktorkern (x=6, z=6)
    t.block(6, 1, 6, NETHERITE)
    t.block(6, 2, 6, MAGMA)
    t.block(6, 3, 6, LANTERN)
    t.block(6, 4, 6, MAGMA)
    t.block(6, 5, 6, CRYING)
    t.block(6, 6, 6, NETHERITE)

    # Reaktorsäulen & Gitter
    for px, pz in ((4, 4), (8, 4), (4, 8), (8, 8)):
        for py in range(1, 7):
            t.block(px, py, pz, DEEPSLATE)
        t.block(px, 7, pz, RED_LAMP)

    for py in range(2, 5):
        t.block(5, py, 6, IRON_BARS)
        t.block(7, py, 6, IRON_BARS)
        t.block(6, py, 5, IRON_BARS)
        t.block(6, py, 7, IRON_BARS)

    # Waffenkammer-Kiste im Süden
    t.block(6, 1, 10, "minecraft:chest", {"facing": "north", "type": "single"},
            {"LootTable": "lit_spaceships:chests/dreadnought_armory"})

    # Jigsaw Eingang im Norden
    t.jigsaw(6, 2, 0, "north_up", IN_NAME, OUT_NAME)
    return t


if __name__ == "__main__":
    command_bridge().write("command_bridge.nbt")
    corridor_breached().write("corridor_breached.nbt")
    engineering_core().write("engineering_core.nbt")
