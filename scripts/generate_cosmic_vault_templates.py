#!/usr/bin/env python3
"""Cosmic Vault: versiegeltes Obsidian-Gewoelb (9x7x9) mit glaesernem Blickfenster
auf die Beutekiste. Seltenste Struktur des Void. Aus Repo-Root:
    python scripts/generate_cosmic_vault_templates.py
"""
import gzip
import os
import struct

DATA_VERSION = 3953
OUT_DIR = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure", "cosmic_vault")


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


class Template:
    def __init__(self, size):
        self.size = size
        self.palette = []
        self._index = {}
        self._blocks = {}   # pos -> (state_idx, nbt) — ein Eintrag pro Position (last write wins)

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


def _ints(values):
    return [struct.pack(">i", v) for v in values]


OBSIDIAN = "minecraft:obsidian"
GLASS_CYAN = "minecraft:cyan_stained_glass"
GOLD = "minecraft:gold_block"
LANTERN = "minecraft:sea_lantern"
AIR = "minecraft:air"

t = Template((9, 7, 9))

# Vollstaendig versiegelter Obsidian-Wuerfel (alle 6 Flaechen)
t.fill(0, 0, 0, 8, 6, 8, OBSIDIAN)

# Innenraum (Luft) 1..7 / 1..5 / 1..7
t.fill(1, 1, 1, 7, 5, 7, AIR)

# Blickfenster auf der Suedwand (z=8): 2x2 cyan Glas bei y2-3
t.fill(4, 2, 8, 5, 3, 8, GLASS_CYAN)

# Beute-Podest: Goldbloecke + Kiste + Leuchten
t.block(4, 1, 3, GOLD)
t.block(4, 1, 5, GOLD)
t.block(4, 1, 4, "minecraft:chest", {"facing": "south", "type": "single"},
        {"LootTable": "lit_spaceships:chests/cosmic_vault"})
t.block(4, 5, 4, LANTERN)

t.write("vault.nbt")
