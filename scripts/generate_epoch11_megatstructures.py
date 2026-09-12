# -*- coding: utf-8 -*-
"""Epoch 11: Megastrukturen - NBT-Templates via Jigsaw-Module.

behemoth_freighter : 4 Module (Bridge -> Cargo/Corridor -> Engineering, ~84 Blöcke lang)
relay_array        : Antennen-Gitter (16x48x16) mit Beacon
solar_collector    : Thermal-Plattform (24x10x24) mit Beacon
deep_outpost       : hohler Asteroid (19x20x19) mit Horchposten + Beacon
"""
import gzip
import math
import os
import struct

DATA_VERSION = 3953
OUT = os.path.join("src", "main", "resources", "data", "lit_spaceships", "structure")

ROOMS_FREIGHTER = "lit_spaceships:behemoth_freighter/sections"


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

    def jigsaw(self, x, y, z, orientation, pool, name, target):
        self.block(x, y, z, "minecraft:jigsaw", {"orientation": orientation}, {
            "name": name, "target": target, "pool": pool,
            "final_state": "minecraft:air", "joint": "rollable"})

    def write(self, sub, filename):
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
        path = os.path.join(OUT, sub, filename)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(gzip.compress(data))
        print(f"{path}: {len(self.palette)} palette, {len(self._blocks)} blocks")


IRON = "minecraft:iron_block"
GRAY = "minecraft:gray_concrete"
LIGHT = "minecraft:light_gray_concrete"
GLASS = "minecraft:glass"
GLASS_CYAN = "minecraft:light_blue_stained_glass"
LANTERN = "minecraft:sea_lantern"
AIR = "minecraft:air"
CHEST = "minecraft:chest"
COPPER = "minecraft:copper_block"
GRATE = "minecraft:copper_grate"
BULB = "minecraft:copper_bulb"
ROD = "minecraft:lightning_rod"
DEEPSLATE = "minecraft:deepslate"
STONE = "minecraft:stone"
OBSIDIAN = "minecraft:obsidian"
CRYING = "minecraft:crying_obsidian"
GILDED = "minecraft:gilded_blackstone"
BLACKSTONE = "minecraft:blackstone"
MAGMA = "minecraft:magma_block"
BASALT = "minecraft:smooth_basalt"
PACKED = "minecraft:packed_ice"
BLUE_ICE = "minecraft:blue_ice"
SNOW = "minecraft:snow_block"


def hull(t, w, h, l, wall, floor, ceil, window_x=None, window_z=None, mid_y=2):
    for x in range(w):
        for z in range(l):
            border = x in (0, w - 1) or z in (0, l - 1)
            t.block(x, 0, z, IRON if border else floor)
            t.block(x, h - 1, z, ceil)
    for x in range(w):
        for z in range(l):
            if x not in (0, w - 1) and z not in (0, l - 1):
                continue
            for y in range(1, h - 1):
                is_window = y == mid_y and (
                    (z in (0, l - 1) and window_x and x in window_x)
                    or (x in (0, w - 1) and window_z and z in window_z))
                t.block(x, y, z, GLASS if is_window else wall)
    t.fill(1, 1, 1, w - 2, h - 2, l - 2, AIR)


IN_NAME, OUT_NAME = "lit_spaceships:ship_in", "lit_spaceships:ship_out"
BEACON_FREQ_RESEARCH = "lit_spaceships:research_beacon"
BEACON_FREQ_ANOMALY = "lit_spaceships:anomalous_relic"


# ---------- Behemoth Freighter (4 Module, ~84 lang) ----------

def freighter_bridge():
    t = Template((16, 12, 16))
    hull(t, 16, 12, 16, IRON, GRAY, IRON, window_x=range(4, 12), window_z=range(4, 12))
    # Kommandobrücke: Konsole + Fensterfront
    t.fill(2, 1, 2, 6, 1, 2, LIGHT)
    t.block(4, 2, 2, "minecraft:observer", {"facing": "north"})
    for cx, cz in ((3, 13), (12, 13), (3, 2), (12, 2)):
        t.block(cx, 11, cz, LANTERN)
    t.jigsaw(15, 2, 8, "east_up", ROOMS_FREIGHTER, OUT_NAME, IN_NAME)
    return t


def freighter_cargo():
    t = Template((24, 14, 16))
    hull(t, 24, 14, 16, IRON, GRAY, IRON, window_x=range(4, 20), window_z=range(3, 13))
    # Frachtcontainer-Reihen
    for row, z0 in enumerate((3, 7, 11)):
        for x in range(3, 21, 4):
            t.fill(x, 1, z0, x + 1, 2, z0 + 1, COPPER)
    t.block(12, 13, 8, LANTERN)
    t.jigsaw(0, 2, 8, "west_up", ROOMS_FREIGHTER, IN_NAME, OUT_NAME)
    t.jigsaw(23, 2, 8, "east_up", ROOMS_FREIGHTER, OUT_NAME, IN_NAME)
    return t


def freighter_corridor():
    t = Template((24, 8, 12))
    hull(t, 24, 8, 12, GRAY, GRAY, GRAY, window_x=range(5, 19), window_z=range(3, 9))
    # Rumpfbruch: Loch in der Nordwand + freiliegende Konduite
    for x in range(8, 16):
        for y in range(2, 6):
            t.block(x, y, 0, AIR)
    t.fill(9, 2, 1, 14, 2, 1, COPPER)
    t.block(10, 3, 1, BULB)
    t.jigsaw(0, 2, 6, "west_up", ROOMS_FREIGHTER, IN_NAME, OUT_NAME)
    t.jigsaw(23, 2, 6, "east_up", ROOMS_FREIGHTER, OUT_NAME, IN_NAME)
    return t


def freighter_engineering():
    t = Template((20, 14, 16))
    hull(t, 20, 14, 16, IRON, GRAY, IRON, window_x=range(4, 16), window_z=range(4, 12))
    # Maschinenraum: Reaktorsaeule + Rohrleitungen + Manifest-Kiste
    t.fill(8, 1, 6, 11, 4, 9, IRON)
    for x in range(8, 12):
        for z in range(6, 10):
            t.block(x, 4, z, COPPER)
    t.block(9, 5, 7, LANTERN)
    t.block(10, 5, 8, LANTERN)
    t.block(5, 1, 8, CHEST, {"facing": "east", "type": "single"},
            {"LootTable": "lit_spaceships:chests/behemoth_manifest"})
    t.jigsaw(0, 2, 8, "west_up", ROOMS_FREIGHTER, IN_NAME, OUT_NAME)
    return t


# ---------- Relay Array (16x48x16 Antennen-Gitter) ----------

def relay_array():
    W, H, L = 16, 48, 16
    t = Template((W, H, L))
    # Fundament-Plattform
    t.fill(0, 0, 0, W - 1, 0, L - 1, GRAY)
    t.fill(1, 1, 1, W - 2, 1, L - 1, AIR)
    # Gitter-Mast (4 Beine + Querstreben, Kupfer)
    for bx, bz in ((4, 4), (11, 4), (4, 11), (11, 11)):
        for y in range(1, 40):
            t.block(bx, y, bz, COPPER)
    # Querstreben alle 6 Ebenen
    for y in range(4, 40, 6):
        t.fill(4, y, 4, 11, y, 4, GRATE)
        t.fill(4, y, 11, 11, y, 11, GRATE)
        t.fill(4, y, 5, 4, y, 10, GRATE)
        t.fill(11, y, 5, 11, y, 10, GRATE)
    # Spitze: Blitzableiter-Mast + Leuchten
    for y in range(40, 46):
        t.block(8, y, 8, ROD if y % 2 == 0 else COPPER)
    t.block(8, 46, 8, LANTERN)
    # Beacon am Fuss (Research)
    t.block(8, 1, 8, "minecraft:beacon", nbt={"frequency": BEACON_FREQ_RESEARCH})
    return t


# ---------- Solar Collector (24x10x24 Plattform) ----------

def solar_collector():
    W, H, L = 24, 10, 24
    t = Template((W, H, L))
    # Plattform
    t.fill(0, 0, 0, W - 1, 0, L - 1, GRAY)
    # Kollektor-Paneele (Glass auf Stelzen, 4 Quadrate)
    for px, pz in ((3, 3), (15, 3), (3, 15), (15, 15)):
        t.fill(px, 4, pz, px + 5, 4, pz + 5, GLASS_CYAN)
        for cx, cz in ((px, pz), (px + 5, pz), (px, pz + 5), (px + 5, pz + 5)):
            t.block(cx, 1, cz, IRON)
            t.block(cx, 2, cz, IRON)
            t.block(cx, 3, cz, COPPER)
    # Zentraler Thermal-Turm mit Lava-Kessel
    t.fill(11, 1, 11, 12, 3, 12, BASALT)
    t.block(11, 4, 11, "minecraft:lava_cauldron")
    t.block(12, 4, 12, LANTERN)
    # Beacon (Research)
    t.block(6, 1, 12, "minecraft:beacon", nbt={"frequency": BEACON_FREQ_RESEARCH})
    return t


# ---------- Deep Outpost (19x20x19 hohler Asteroid) ----------

def deep_outpost():
    W, H, L = 19, 20, 19
    cx, cz = 9, 9
    t = Template((W, H, L))
    # Asteroid: kompakte Kugel aus Stein/Deepslate
    r = 9.0
    for x in range(W):
        for y in range(H):
            for z in range(L):
                dx, dy, dz = x - cx, (y - 6) * 1.15, z - cz
                dist = math.sqrt(dx * dx + dy * dy + dz * dz)
                if dist <= r:
                    mat = DEEPSLATE if dist > r - 2.5 else STONE
                    if dist > r - 1.0 and random_ore(x, y, z):
                        mat = PACKED if random_ore(x + 1, y, z) else OBSIDIAN
                    t.block(x, y, z, mat)
    # Hohle Kammer im Inneren (Radius 4)
    for x in range(W):
        for y in range(H):
            for z in range(L):
                dx, dy, dz = x - cx, (y - 6) * 1.15, z - cz
                if math.sqrt(dx * dx + dy * dy + dz * dz) < 4.2:
                    t.block(x, y, z, AIR)
    # Horchposten: Boden, Konsolen, Beacon + Archiv-Kiste
    t.fill(cx - 3, 4, cz - 3, cx + 3, 4, cz + 3, LIGHT)
    t.block(cx, 5, cz - 3, LANTERN)
    t.block(cx - 2, 5, cz - 2, "minecraft:beacon", nbt={"frequency": BEACON_FREQ_ANOMALY})
    t.block(cx + 2, 5, cz, CHEST, {"facing": "west", "type": "single"},
            {"LootTable": "lit_spaceships:chests/deep_outpost_archive"})
    # Zufahrts-Tunnel zur Suedseite
    for y in range(5, 8):
        for x in range(cx - 1, cx + 2):
            t.block(x, y, L - 1, AIR)
            t.block(x, y, L - 2, AIR)
    return t


def random_ore(x, y, z):
    return (x * 31 + y * 17 + z * 13) % 7 == 0


if __name__ == "__main__":
    freighter_bridge().write("behemoth_freighter", "bridge.nbt")
    freighter_cargo().write("behemoth_freighter", "cargo_bay.nbt")
    freighter_corridor().write("behemoth_freighter", "corridor_fractured.nbt")
    freighter_engineering().write("behemoth_freighter", "engineering_bay.nbt")
    relay_array().write("relay_array", "array.nbt")
    solar_collector().write("solar_collector", "collector.nbt")
    deep_outpost().write("deep_outpost", "outpost.nbt")
