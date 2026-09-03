package com.peaceman.alpha.ship;

import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Persistenzschicht für Raumschiffdaten auf dem Server (Overworld).
 * Speichert reine Domain-Daten inklusive dimensionaler Zuordnung.
 */
public class ShipSavedData extends SavedData {

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag shipList = new ListTag();

        for (ShipState ship : ServerShipManager.ACTIVE_SHIPS.values()) {
            CompoundTag shipTag = new CompoundTag();

            // 1. UUID & Controller
            shipTag.putUUID("ID", ship.getId());
            BlockPos ctrl = ship.getControllerPos();
            shipTag.putIntArray("Controller", new int[]{ctrl.getX(), ctrl.getY(), ctrl.getZ()});

            // 2. Schiffsblöcke
            ListTag blockList = new ListTag();
            for (BlockPos block : ship.getBlocks()) {
                blockList.add(new IntArrayTag(new int[]{block.getX(), block.getY(), block.getZ()}));
            }
            shipTag.put("Blocks", blockList);

            // 3. Homes
            CompoundTag homesTag = new CompoundTag();
            for (Map.Entry<String, BlockPos> home : ship.getHomes().entrySet()) {
                BlockPos hp = home.getValue();
                homesTag.putIntArray(home.getKey(), new int[]{hp.getX(), hp.getY(), hp.getZ()});
            }
            shipTag.put("Homes", homesTag);

            // 4. Reaktoren und Schilde
            ListTag reactorList = new ListTag();
            for (BlockPos pos : ship.getReactors()) {
                reactorList.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
            }
            shipTag.put("Reactors", reactorList);

            ListTag shieldList = new ListTag();
            for (BlockPos pos : ship.getShields()) {
                shieldList.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
            }
            shipTag.put("Shields", shieldList);

            ListTag weaponList = new ListTag();
            for (BlockPos pos : ship.getWeapons()) {
                weaponList.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
            }
            shipTag.put("Weapons", weaponList);

            // Lokalisierte Schildzonen
            ListTag zoneList = new ListTag();
            for (com.peaceman.alpha.ship.domain.ShieldZone zone : ship.getShieldZones().values()) {
                CompoundTag zTag = new CompoundTag();
                zTag.putByte("ZoneId", zone.id());
                BlockPos gp = zone.generatorPos();
                if (gp != null) {
                    zTag.putIntArray("GenPos", new int[]{gp.getX(), gp.getY(), gp.getZ()});
                }
                zTag.putInt("Energy", zone.currentEnergy());
                zTag.putInt("MaxEnergy", zone.maxEnergy());
                zTag.putLong("Cooldown", zone.cooldownUntil());
                zTag.putBoolean("IsEnabled", zone.isEnabled());
                zoneList.add(zTag);
            }
            shipTag.put("ShieldZones", zoneList);

            // 5. Status & Dimension
            shipTag.putBoolean("ShieldActive", ship.isShieldActive());
            shipTag.putString("Dimension", ship.getDimension().location().toString());
            shipTag.putString("PowerPriority", ship.getPowerPriority().name());

            // 6. Cooldowns (absolute Weltzeit)
            shipTag.putLong("ShieldCooldownUntil", ship.getShieldCooldownUntil());
            shipTag.putLong("MovementCooldownUntil", ship.getMovementCooldownUntil());

            shipList.add(shipTag);
        }

        tag.put("ActiveShips", shipList);
        return tag;
    }

    public static ShipSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ShipSavedData data = new ShipSavedData();
        ServerShipManager.ACTIVE_SHIPS.clear();

        ListTag shipList = tag.getList("ActiveShips", Tag.TAG_COMPOUND);
        for (int i = 0; i < shipList.size(); i++) {
            CompoundTag shipTag = shipList.getCompound(i);

            UUID id = shipTag.getUUID("ID");

            int[] ctrlArray = shipTag.getIntArray("Controller");
            BlockPos ctrlPos = new BlockPos(ctrlArray[0], ctrlArray[1], ctrlArray[2]);

            Set<BlockPos> blocks = new HashSet<>();
            ListTag blockList = shipTag.getList("Blocks", Tag.TAG_INT_ARRAY);
            for (int j = 0; j < blockList.size(); j++) {
                int[] blockArray = blockList.getIntArray(j);
                blocks.add(new BlockPos(blockArray[0], blockArray[1], blockArray[2]));
            }

            Map<String, BlockPos> homes = new HashMap<>();
            CompoundTag homesTag = shipTag.getCompound("Homes");
            for (String key : homesTag.getAllKeys()) {
                int[] hpArray = homesTag.getIntArray(key);
                homes.put(key, new BlockPos(hpArray[0], hpArray[1], hpArray[2]));
            }

            List<BlockPos> loadedReactors = new ArrayList<>();
            if (shipTag.contains("Reactors")) {
                ListTag rList = shipTag.getList("Reactors", Tag.TAG_INT_ARRAY);
                for (int j = 0; j < rList.size(); j++) {
                    int[] arr = rList.getIntArray(j);
                    loadedReactors.add(new BlockPos(arr[0], arr[1], arr[2]));
                }
            }

            List<BlockPos> loadedShields = new ArrayList<>();
            if (shipTag.contains("Shields")) {
                ListTag sList = shipTag.getList("Shields", Tag.TAG_INT_ARRAY);
                for (int j = 0; j < sList.size(); j++) {
                    int[] arr = sList.getIntArray(j);
                    loadedShields.add(new BlockPos(arr[0], arr[1], arr[2]));
                }
            }

            List<BlockPos> loadedWeapons = new ArrayList<>();
            if (shipTag.contains("Weapons")) {
                ListTag wList = shipTag.getList("Weapons", Tag.TAG_INT_ARRAY);
                for (int j = 0; j < wList.size(); j++) {
                    int[] arr = wList.getIntArray(j);
                    loadedWeapons.add(new BlockPos(arr[0], arr[1], arr[2]));
                }
            }

            boolean isShieldActive = !shipTag.contains("ShieldActive") || shipTag.getBoolean("ShieldActive");

            ResourceKey<Level> dimension = Level.OVERWORLD;
            if (shipTag.contains("Dimension")) {
                ResourceLocation dimLoc = ResourceLocation.parse(shipTag.getString("Dimension"));
                dimension = ResourceKey.create(Registries.DIMENSION, dimLoc);
            }

            ShipState loadedShip = new ShipState(id, ctrlPos, blocks, homes, loadedReactors, loadedShields, isShieldActive, dimension);
            loadedShip.setWeapons(loadedWeapons);

            // Lokalisierte Schildzonen laden
            if (shipTag.contains("ShieldZones")) {
                ListTag zList = shipTag.getList("ShieldZones", Tag.TAG_COMPOUND);
                Map<Byte, com.peaceman.alpha.ship.domain.ShieldZone> loadedZones = new HashMap<>();
                for (int j = 0; j < zList.size(); j++) {
                    CompoundTag zTag = zList.getCompound(j);
                    byte zId = zTag.getByte("ZoneId");
                    BlockPos gp = null;
                    if (zTag.contains("GenPos")) {
                        int[] gArr = zTag.getIntArray("GenPos");
                        gp = new BlockPos(gArr[0], gArr[1], gArr[2]);
                    }
                    int energy = zTag.getInt("Energy");
                    int maxEnergy = zTag.getInt("MaxEnergy");
                    long cooldown = zTag.getLong("Cooldown");
                    boolean isEnabled = !zTag.contains("IsEnabled") || zTag.getBoolean("IsEnabled");
                    loadedZones.put(zId, new com.peaceman.alpha.ship.domain.ShieldZone(zId, gp, energy, maxEnergy, cooldown, isEnabled));
                }
                loadedShip.setShieldZones(loadedZones);
            }

            // Power-Priorität wiederherstellen
            if (shipTag.contains("PowerPriority")) {
                try {
                    loadedShip.setPowerPriority(com.peaceman.alpha.ship.domain.PowerPriority.valueOf(shipTag.getString("PowerPriority")));
                } catch (IllegalArgumentException ignored) {
                    loadedShip.setPowerPriority(com.peaceman.alpha.ship.domain.PowerPriority.BALANCED);
                }
            }

            // Cooldowns wiederherstellen
            if (shipTag.contains("ShieldCooldownUntil")) {
                loadedShip.setShieldCooldownUntil(shipTag.getLong("ShieldCooldownUntil"));
            }
            if (shipTag.contains("MovementCooldownUntil")) {
                loadedShip.setMovementCooldownUntil(shipTag.getLong("MovementCooldownUntil"));
            }

            // Voronoi-Zonen auf Hülle berechnen
            if (!loadedShip.getShields().isEmpty() && loadedShip.getHullVoxelCache() != null && !loadedShip.getHullVoxelCache().isEmpty()) {
                var coverages = com.peaceman.alpha.ship.service.ShipScannerService.calculateVoronoiZones(
                        loadedShip.getHullVoxelCache(), loadedShip.getShields(), loadedShip.getControllerPos()
                );
                loadedShip.setSectorCoverages(coverages);
            }

            ServerShipManager.registerShip(loadedShip);
        }
        return data;
    }

    public static ShipSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        SavedData.Factory<ShipSavedData> factory = new SavedData.Factory<>(
                ShipSavedData::new,
                ShipSavedData::load,
                null
        );
        return overworld.getDataStorage().computeIfAbsent(factory, "spaceship_data");
    }
}