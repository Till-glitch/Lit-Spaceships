# NeoForge-Alpha: Mod-Architektur & Klassendesign

Dieses Dokument beschreibt die Architektur, Datenflüsse, Klassenhierarchien und mathematischen Modelle des Spaceship-, Schutzschild- und Raumkampf-Systems für **Minecraft 1.21 (NeoForge)** inklusive aller Concurrency-, Lifecycle- und Edge-Case-Absicherungen.

---

## 1. Architektur-Übersicht & Design-Prinzipien

Das Gesamtsystem folgt einer strikten **Model-View-Controller (MVC) / Service-Architektur** mit vollständiger Entkopplung zwischen logischem Server und Client:

1. **Server (Domain & Authoritative Services)**:
   * Hält die alleinige Autorität über alle Schiffe (`ShipState`), persistiert ausschließlich reine Domain-Daten (`ShipSavedData`) und berechnet komplexe Schiffsgeometrien asynchron oder zeitbegrenzt.
   * **Kampf- & Raycast-Mathematik (`com.peaceman.alpha.ship.combat.*`)**: Nutzt den **Amanatides-and-Woo 3D-DDA-Algorithmus** (`FastVoxelTraversal`) in Kombination mit einer zweistufigen Broadphase-/Narrowphase-Filterung (`LaserRaycastUtil`), um Strahlen kollisionsgenau in $O(\text{Ray-Länge})$ anstelle von $O(\text{Blockanzahl})$ zu berechnen.
   * **Time-Sliced Mover (`ShipMovementService`)**: Führt translatorische Schiffsbewegungen mit einem festen **10ms Tick-Budget** pro Server-Tick aus und forceloaded Ziel-Chunks via `TicketType` (`SHIP_TICKET`).
2. **Network (Deklarative StreamCodecs & Thread-Safety)**:
   * Alle Netzwerkinteraktionen sind als unveränderliche `CustomPacketPayload`-Records mit Mojang/NeoForge `StreamCodec`-Composites implementiert.
   * Client-Payloads werden strikt über `context.enqueueWork()` auf dem Render-Main-Thread synchronisiert.
3. **Spatial Hashing & Lifecycle-Synchronisation**:
   * Struktur- und Schilddaten werden via `ChunkWatchEvent.Sent` gezielt nur an Spieler gestreamt, die den entsprechenden Chunk laden.
   * Wenn Chunks clientseitig noch nicht geladen sind, puffert `ClientShipManager` die Pakete in einer Queue (`PENDING_SYNCS`) und wendet sie bei `ChunkEvent.Load` verzögerungsfrei an.
4. **Client (View Model, Predictive State & VRAM-Management)**:
   * Verwaltet Client-Sichten in `ClientShipManager` und `ClientLaserState`.
   * **Laserstrahlen-Rendering (`LaserBeamRenderer`)**: Rendert volumetrisch leuchtende Billboard-Strahlen mit additiver Farbüberlagerung (`GL_ONE`) und führt Client-Side-Surface-Clipping (`level.clip`) aus, sodass Strahlen exakt auf der Blockoberfläche terminieren.
   * **Translations-Invarianz**: Kontinuierliche Laserstrahlen werden über relative Voxel-Offsets (`shooterShipId + "_" + relativePos.asLong()`) verwaltet, wodurch sie vor, während und nach Schiffsbewegungen absolut synchron bleiben.
   * **VRAM-Freigabe**: Bei Chunk-Entladungen (`ChunkEvent.Unload`), Schiffsauflösung oder Logout werden VBOs und Laserstrahlen sofort freigegeben (`dispose()`).
5. **Voxel-Asset Pipeline & Split-Model Kinematik (Blockbench MCP)**:
   * **Automatisierte Voxel-Generierung**: Deterministische Erstellung von 16x16x16 Cube-Directional-Modellen (`spaceship_controller`, `spaceship_reactor`, `spaceship_shield`) und Texturen im Sci-Fi Industrial Design.
   * **Split-Model Kinematik**: Statische Basisplatte (`laser_base.json`, 16x4x16 mit 8x2x8 Sockel) für AABB-Kollision und BFS-Schiffsscan; dynamische, entkoppelte Turret-Köpfe (`laser_turret_heavy`, `laser_turret_pulse`, `laser_turret_mining`) mit exaktem Drehgelenk bei `[8, 0, 8]` zur Vermeidung von orbitalem Drift im BlockEntityRenderer (`PoseStack`).

---

## 2. Vollständiges Mermaid-Klassendiagramm

```mermaid
classDiagram
    direction TB

    %% ==========================================
    %% SERVER SIDE DOMAIN & SERVICES
    %% ==========================================
    namespace Server_Side {
        class ISpaceshipNode {
            <<interface>>
            +getShipId() UUID
            +setShipId(UUID shipId) void
            +getShip() ShipState
        }

        class AbstractSpaceshipNodeBlockEntity {
            <<abstract>>
            +getShipId() UUID
            +setShipId(UUID shipId) void
        }

        class AbstractLaserNodeBlockEntity {
            <<abstract>>
            +getTier() LaserWeaponTier
            +isContinuous() boolean
            +getFacing() Direction
            +getTargetYaw() float
            +getTargetPitch() float
            +getAimAngles() AimAngles
            +setAimAngles(AimAngles angles) void
            +isOccupied() boolean
            +setOccupied(boolean occupied) void
            +isAimLocked() boolean
            +setAimLocked(boolean locked) void
            +getGimbalLimits() GimbalLimits
            +getCurrentDrillPos() BlockPos
            +getDrillProgress() float
            +addDrillProgress(float amount) void
            +resetDrillProgress() void
            +clearDrillProgress(Level level) void
            +serverTick(Level level, BlockPos pos, BlockState state)* void
        }

        class IAimStrategy {
            <<interface>>
            +getType() AimType
            +requiresPassengerSeat() boolean
            +calculateAimAngles(ShipState ship, BlockPos weaponPos, Player player, GimbalLimits limits) AimAngles
        }

        class FreelookAimStrategy {
            +calculateAimAngles(ShipState ship, BlockPos weaponPos, Player player, GimbalLimits limits) AimAngles
        }

        class AimTransformMath {
            +calculateWorldLookVector(float yaw, float pitch)$ Vec3
            +transformWorldToLocal(Vec3 worldVec, Quaternionf rot)$ Vec3
            +transformLocalToWorld(Vec3 localVec, Quaternionf rot)$ Vec3
            +vectorToLocalEuler(Vec3 localVec)$ AimAngles
            +localEulerToVector(float yaw, float pitch)$ Vec3
            +compressAngle(float angle)$ short
            +decompressAngle(short compressed)$ float
            +interpolateAngle(float prev, float curr, float partialTick)$ float
        }

        class GimbalLimits {
            <<record>>
            +float minYaw
            +float maxYaw
            +float minPitch
            +float maxPitch
            +clamp(AimAngles angles) AimAngles
        }

        class TurretSeatEntity {
            -BlockPos weaponPos
            -Optional~UUID~ shipId
            +getWeaponPos() BlockPos
            +getShipId() UUID
            +setShipId(UUID shipId) void
        }

        class PulseLaserBlockEntity {
            -int cooldownRemaining
            +canFire() boolean
            +triggerCooldown() void
        }

        class HeavyBeamBlockEntity {
            -boolean isFiring
            +isFiring() boolean
            +setFiring(boolean firing) void
        }

        class MiningLaserBlockEntity {
            -boolean isMining
            +isMining() boolean
            +setMining(boolean mining) void
        }

        class ModAttachments {
            +Supplier~AttachmentType~UUID~~ SHIP_ID$
            +register(IEventBus bus)$ void
        }

        class ShipState {
            -UUID id
            -BlockPos controllerPos
            -Set~BlockPos~ blocks
            -Map~String, BlockPos~ homes
            -List~BlockPos~ reactors
            -List~BlockPos~ shields
            -List~BlockPos~ weapons
            -boolean isShieldActive
            -long shieldCooldownUntil
            -long movementCooldownUntil
            -VoxelGridCache hullVoxelCache
            -VoxelGridCache shieldVoxelCache
            +getImmutableBlockSnapshot() Set~BlockPos~
            +setBlocksRaw(Set~BlockPos~ blocks) void
            +recalculateHullBounds() void
            +isShieldOnCooldown(long gameTime) boolean
            +isMovementOnCooldown(long gameTime) boolean
        }

        class ServerShipManager {
            +Map~UUID, ShipState~ ACTIVE_SHIPS$
            +getShip(UUID shipId)$ ShipState
            +hasShip(UUID shipId)$ boolean
            +createShip(Level level, BlockPos startPos)$ ShipState
            +populateAndSyncShipState(Level level, ShipState ship)$ void
            +updateShipBlocks(Level level, ShipState ship)$ void
            +deleteShip(Level level, ShipState ship)$ void
            +saveData(Level level)$ void
            +onServerStarted(ServerStartedEvent event)$ void
            +onChunkSent(ChunkWatchEvent.Sent event)$ void
        }

        class ShipSavedData {
            +get(ServerLevel level)$ ShipSavedData
            +save(CompoundTag tag, Provider registries) CompoundTag
            +load(CompoundTag tag, Provider registries)$ ShipSavedData
        }

        class LaserCombatService {
            +fireWeapon(Level level, ShipState shooter, BlockPos weaponPos)$ boolean
            +tickContinuousWeapon(Level level, ShipState shooter, BlockPos pos, AbstractLaserNodeBE be)$ void
            -processPulseHit(Level level, ShipState shooter, LaserWeaponTier tier, RaycastHitResult hit)$ void
            -processContinuousHit(Level level, ShipState shooter, BlockPos pos, AbstractLaserNodeBE be, LaserWeaponTier tier, RaycastHitResult hit)$ void
            -destroyShipHullBlock(Level level, ShipState targetShip, BlockPos hitBlock, Vec3 worldHitPos)$ void
        }

        class LaserRaycastUtil {
            +raycast(Level level, UUID shooterId, Vec3 origin, Vec3 dir, double maxRange, boolean hitTerrain)$ RaycastHitResult
        }

        class FastVoxelTraversal {
            +traverse(VoxelGridCache cache, Vec3 localOrigin, Vec3 localDir, double maxDistance)$ Optional~VoxelHit~
        }

        class LaserWeaponTier {
            <<enum>>
            PULSE_LASER
            HEAVY_BEAM
            MINING_LASER
            +getMaxRange() double
            +getEnergyCost() int
            +getBaseDamage() float
            +getCooldownTicks() int
        }

        class RaycastHitResult {
            <<record>>
            +HitType type
            +UUID hitShipId
            +BlockPos relativeBlockPos
            +BlockPos worldBlockPos
            +Vec3 worldHitPos
            +Direction hitFace
            +double distance
            +isHit() boolean
            +isShipHit() boolean
        }

        class VoxelGridCache {
            +BlockPos minOffset
            +BitSet bitSet
            +isSet(int x, int y, int z) boolean
            +buildFromAbsolute(Collection~BlockPos~ abs, BlockPos ctrl)$ VoxelGridCache
        }

        class ShipMovementService {
            +TICK_BUDGET_NANOS long$
            +SHIP_TICKET TicketType~ChunkPos~$
            +moveShip(Level level, ShipState ship, int dx, int dy, int dz, Player player)$ void
            +isShipMoving(UUID shipId)$ boolean
            +onServerTick(ServerTickEvent.Post event)$ void
        }

        class ShipCollisionService {
            +SECTOR_SIZE double$
            +calculateSweptAABB(AABB currentBox, double dx, double dy, double dz)$ AABB
            +calculateTimeOfImpact(AABB movingBox, AABB targetBox, Vec3 velocity)$ double
            +calculateIntersection(AABB a, AABB b)$ Optional~AABB~
            +findPotentialCollisions(ShipState movingShip, Vec3 moveVec)$ List~BroadPhaseCandidate~
            +calculateVoxelIntersection(ShipState shipA, BlockPos originA, ShipState shipB, BlockPos originB, AABB intersectionBox)$ VoxelCollisionResult
        }

        class CollisionResolver {
            +ENERGY_PER_VOXEL_IMPACT int$
            +ENERGY_PER_VOXEL_DRILL int$
            +ENERGY_PER_VOXEL_SHIELD_CLASH int$
            +resolve(ServerLevel level, VoxelCollisionResult collision, Vec3 movementVector)$ CollisionResolution
            +resolveMultiple(ServerLevel level, List~VoxelCollisionResult~ collisions, Vec3 movementVector)$ CollisionResolution
        }

        class SpaceshipEnergyManager {
            +calculateMovementCost(ShipState ship, int dx, int dy, int dz)$ int
            +getTotalAvailableEnergy(Level level, ShipState ship)$ int
            +consumeEnergy(Level level, ShipState ship, int amount)$ void
            +tryConsumeEnergyAmount(Level level, ShipState ship, int amount)$ boolean
            +tryConsumeFlightEnergy(Level level, ShipState ship, int dx, int dy, int dz, Player player)$ boolean
        }
    }

    %% ==========================================
    %% NETWORK LAYER
    %% ==========================================
    namespace Network_Layer {
        class ModPayloads {
            +register(IEventBus bus)$ void
        }

        class ShipCombatActionPayload {
            <<record>>
            +Optional~UUID~ shipId
            +CombatAction action
            +Optional~BlockPos~ weaponPos
        }

        class OpenHelmConfigPayload {
            <<record>>
            +Optional~UUID~ shipId
        }

        class LaserFirePayload {
            <<record>>
            +UUID shooterShipId
            +Vec3 startPos
            +Vec3 endPos
            +LaserWeaponTier tier
        }

        class LaserStateSyncPayload {
            <<record>>
            +UUID shooterShipId
            +BlockPos weaponPos
            +boolean isFiring
            +LaserWeaponTier tier
        }

        class ShipStructureDeltaPayload {
            <<record>>
            +UUID shipId
            +List~BlockPos~ removedBlocks
        }

        class ShipPositionSyncPayload {
            <<record>>
            +UUID shipId
            +BlockPos newAnchorPos
        }

        class ShipStateSyncPayload {
            <<record>>
            +UUID shipId
            +int currentEnergy
            +boolean isShieldActive
            +long shieldCooldownRemainingTicks
            +long movementCooldownRemainingTicks
        }

        class TurretAimSyncPayload {
            <<record>>
            +BlockPos weaponPos
            +float yaw
            +float pitch
        }

        class TurretLockTogglePayload {
            <<record>>
            +BlockPos weaponPos
        }

        class ServerPayloadHandler {
            +handleAction(ShipActionPayload payload, IPayloadContext context)$ void
            +handleCombatAction(ShipCombatActionPayload payload, IPayloadContext context)$ void
            +handleTurretAimSync(TurretAimSyncPayload payload, IPayloadContext context)$ void
            +handleTurretLockToggle(TurretLockTogglePayload payload, IPayloadContext context)$ void
        }

        class ClientPayloadRegistrar {
            +registerClientPayloads(PayloadRegistrar registrar)$ void
        }

        class ClientPayloadHandler {
            +handleStructureDelta(ShipStructureDeltaPayload packet, IPayloadContext context)$ void
            +handlePositionSync(ShipPositionSyncPayload packet, IPayloadContext context)$ void
            +handleLaserFire(LaserFirePayload packet, IPayloadContext context)$ void
            +handleLaserStateSync(LaserStateSyncPayload packet, IPayloadContext context)$ void
            +handleTurretAimSync(TurretAimSyncPayload packet, IPayloadContext context)$ void
        }
    }

    %% ==========================================
    %% CLIENT SIDE VIEW & RENDERING
    %% ==========================================
    namespace Client_Side {
        class SpaceshipClientInputHandler {
            +onPlayerInteract(PlayerInteractEvent.RightClickBlock event)$ void
        }

        class ClientShipState {
            -UUID shipId
            -BlockPos anchorPos
            -VertexBuffer shieldMesh
            -boolean isShieldActive
            +updateMesh(Set~BlockPos~ relativeBlocks) void
            +removeStructureBlocks(List~BlockPos~ removed) void
            +dispose() void
        }

        class ClientLaserState {
            -CopyOnWriteArrayList~ActivePulseLaser~ ACTIVE_PULSES$
            -Map~String, ActiveContinuousBeam~ ACTIVE_CONTINUOUS_BEAMS$
            +addPulse(UUID shooterId, Vec3 start, Vec3 end, LaserWeaponTier tier)$ void
            +setContinuousBeam(UUID shooterId, BlockPos weaponPos, boolean firing, LaserWeaponTier tier)$ void
            +removeBeamsForShip(UUID shipId)$ void
            +clearAll()$ void
        }

        class LaserBeamRenderer {
            +onRenderLevelStage(RenderLevelStageEvent event)$ void
            -drawBeam(BufferBuilder buffer, Matrix4f mat, Vec3 cam, Vec3 start, Vec3 end, LaserWeaponTier tier, float alpha)$ void
        }

        class LaserNodeRenderState {
            <<record>>
            +Direction facing
            +float yaw
            +float pitch
            +LaserWeaponTier tier
            +extract(AbstractLaserNodeBlockEntity be, float partialTick)$ LaserNodeRenderState
            +getYaw() float
            +getPitch() float
            +getFacing() Direction
            +getTier() LaserWeaponTier
        }

        class TurretBlockEntityRenderer {
            +render(T laserBE, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) void
            +submit(LaserNodeRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) void
        }

        class ShieldRenderer {
            +renderShields(PoseStack stack, MultiBufferSource buffer, Camera camera, float partialTicks)$ void
        }

        class TurretDebugLogger {
            +logMount(String player, BlockPos pos, UUID shipId, boolean isClient)$ void
            +logDismount(String player, BlockPos pos, boolean isClient)$ void
            +logClientAimSent(BlockPos pos, float yaw, float pitch)$ void
            +logClientLockTriggered(BlockPos pos, String src)$ void
            +logServerAimReceived(String player, BlockPos pos, float yaw, float pitch, boolean locked)$ void
            +logServerLockToggled(String player, BlockPos pos, boolean newLock)$ void
            +logCombatAim(BlockPos pos, float yaw, float pitch, double dx, double dy, double dz)$ void
        }
    }

    %% ==========================================
    %% ==========================================
    %% DATA GENERATION & I18N PIPELINE
    %% ==========================================
    namespace Data_Generation {
        class ModI18n {
            +Tab Tab
            +Screen Screen
            +Message Message
            +Keybind Keybind
            +Tooltip Tooltip
            +Structure Structure
            +Biome Biome
        }

        class DataGenerators {
            +gatherData(GatherDataEvent event)$ void
        }

        class ModBlockStateProvider {
            +registerStatesAndModels() void
            -registerLaserBase(Block block, ModelFile baseModel) void
        }

        class ModItemModelProvider {
            +registerModels() void
        }

        class ModEnglishLanguageProvider {
            +addTranslations() void
        }

        class ModGermanLanguageProvider {
            +addTranslations() void
        }

        class ModLanguageProvider {
            +addTranslations() void
        }

        class ModLootTableProvider {
            +create(PackOutput output, CompletableFuture lookupProvider)$ LootTableProvider
        }

        class ModBlockLootTableProvider {
            +generate() void
            +getKnownBlocks() Iterable~Block~
        }
    }

    %% Relationships
    AbstractSpaceshipNodeBlockEntity ..|> ISpaceshipNode : implements
    AbstractLaserNodeBlockEntity --|> AbstractSpaceshipNodeBlockEntity : extends
    PulseLaserBlockEntity --|> AbstractLaserNodeBlockEntity : extends
    HeavyBeamBlockEntity --|> AbstractLaserNodeBlockEntity : extends
    MiningLaserBlockEntity --|> AbstractLaserNodeBlockEntity : extends
    TurretBlockEntityRenderer ..> LaserNodeRenderState : extracts & submits

    ServerShipManager o-- "0..*" ShipState : manages
    ShipState o-- "0..1" VoxelGridCache : holds
    LaserCombatService ..> LaserRaycastUtil : raycasts via
    LaserRaycastUtil ..> FastVoxelTraversal : 3D-DDA
    LaserRaycastUtil ..> RaycastHitResult : evaluates
    LaserCombatService ..> SpaceshipEnergyManager : drains FE

    ServerPayloadHandler ..> LaserCombatService : triggers combat
    LaserCombatService ..> LaserFirePayload : broadcasts
    LaserCombatService ..> LaserStateSyncPayload : broadcasts
    LaserCombatService ..> ShipStructureDeltaPayload : broadcasts

    ClientPayloadHandler ..> ClientLaserState : updates state
    ClientPayloadHandler ..> ClientShipManager : updates meshes
    LaserBeamRenderer ..> ClientLaserState : reads beams
    LaserBeamRenderer ..> ClientShipManager : resolves anchors
    ClientShipManager *-- "0..*" ClientShipState : holds

    DataGenerators ..> ModBlockStateProvider : instantiates client
    DataGenerators ..> ModItemModelProvider : instantiates client
    DataGenerators ..> ModEnglishLanguageProvider : instantiates client
    DataGenerators ..> ModGermanLanguageProvider : instantiates client
    DataGenerators ..> ModLootTableProvider : instantiates server
    ModEnglishLanguageProvider ..> ModI18n : references keys
    ModGermanLanguageProvider ..> ModI18n : references keys
    ModLootTableProvider ..> ModBlockLootTableProvider : creates
```

---

## 3. Mathematische Modelle & Algorithmen

### A. Amanatides & Woo 3D-DDA Voxel-Traversierung (`FastVoxelTraversal`)
Zur Erkennung von Treffern auf zusammenhängenden Schiffsvoxeln wird der 3D Digital Differential Analyzer eingesetzt:
1. **Ray-Parametrisierung**: Der Strahl wird im Local-Space des Zielschiffs über $R(t) = \vec{o} + t \cdot \vec{d}$ beschrieben.
2. **Initialisierung von $t_{\text{max}}$ und $\Delta t$**:
   $$\Delta t_x = \left|\frac{1}{d_x}\right|, \quad t_{\text{max}, x} = t_{\text{start}} + (\lfloor x_0 \rfloor + 1 - x_0) \cdot \Delta t_x \quad (\text{für } d_x > 0)$$
3. **Schrittweiser Voxel-Vorschub**: In jedem Schritt wird die Achse mit dem kleinsten $t_{\text{max}}$ inkrementiert und die entsprechende Eintrittsfläche (`Direction`) festgehalten:
   $$t_{\text{max}, x} < t_{\text{max}, y} \land t_{\text{max}, x} < t_{\text{max}, z} \implies x \leftarrow x + \text{step}_x, \quad \text{face} \leftarrow \text{WEST/EAST}$$
4. **Schutzgrenze**: Feste Obergrenze von maximal 1024 Iterationsschritten gegen Endlosschleifen bei extremen Distanzen.

### B. Progressiver Blockabbau & Zerstörungs-Skalierung
Dauerstrahlen (`HeavyBeam`, `MiningLaser`) berechnen den Zerstörungsfortschritt pro Server-Tick dynamisch anhand der Blockhärte $H = \text{DestroySpeed}$:
$$\Delta \text{Progress} = \frac{k_{\text{tier}}}{\max(0.5, H)}$$
* **Mining Laser**: $k_{\text{tier}} = 0.25$ (z. B. Stein mit $H=1.5 \implies \Delta P = 0.166 \implies 6\text{ Ticks} = 0.3\text{s}$).
* **Heavy Beam**: $k_{\text{tier}} = 0.15$ (z. B. Stein $\implies 10\text{ Ticks} = 0.5\text{s}$).
* **Optische Rückkopplung**: Der Server synchronisiert den Fortschritt über `level.destroyBlockProgress(id, pos, (int)(P \cdot 10))` direkt an alle Clients.

---

## 4. Datenfluss & Lebenszyklus-Matrix

| Aktion | Auslöser / Schicht | Ausführung | Netzwerk / Persistenz / Render |
| :--- | :--- | :--- | :--- |
| **Impuls-Laser abfeuern** | Pilot drückt `FIRE_PULSE` | `ServerPayloadHandler` $\rightarrow$ `LaserCombatService.fireWeapon()` | Zieht 250 FE ab, raycastet via `LaserRaycastUtil`. Zerstört 1 Block sofort (`destroyBlock` bzw. `SHIP_HULL`-Delta). Sendet `LaserFirePayload`. |
| **Dauerstrahl umschalten** | Pilot drückt `TOGGLE_HEAVY_BEAM` | `ServerPayloadHandler` $\rightarrow$ `LaserCombatService.fireWeapon()` | Schaltet `isFiring` im BE um, sendet `LaserStateSyncPayload`. BE konsumiert im `serverTick` 50 FE/Tick und führt progressiven Abbau durch. |
| **Energiemangel bei Dauerfeuer** | Reaktor leer (`tryConsumeEnergyAmount == false`) | `HeavyBeamBlockEntity.serverTick()` | Schaltet sich sofort ab (`setFiring(false)`), bricht Drill ab und sendet `LaserStateSyncPayload(isFiring = false)`. |
| **Schiffstranslation mit aktiven Lasern** | Navigation / Helm `MOVE` | `ShipMovementService.moveShip()` | Verschiebt Blöcke und Waffen (`newWeapons`). Ignoriert Deaktivierung in `onRemove` via `isShipMoving()`. Client-Map-Key (`relativePos.asLong()`) bleibt translations-invariant. |
| **Laser-Rendering (Client)** | Render-Frame (`AFTER_TRANSLUCENT_BLOCKS`) | `LaserBeamRenderer.onRenderLevelStage()` | Berechnet Strahlenursprung aus Schiffsanker + relativem Offset. Führt `level.clip()` aus $\rightarrow$ Strahl stoppt exakt auf der Blockoberfläche. Rendert Quads additiv (`GL_ONE`). |
| **VRAM & Laser-Freigabe** | Chunk entlädt / Schiff gelöscht / Logout | `ClientShipManager` & `ClientLaserState` | `ClientLaserState.removeBeamsForShip(shipId)` räumt Laser auf; `ClientShipState.dispose()` schließt VBOs im GL-Thread. |

---

## 5. Testing-Architektur & CI/CD Pipeline

Das Projekt erzwingt kontinuierliche Testabdeckung gemäß der **70/20-Regel**:

1. **JUnit 5 & Mockito Suite (51 Tests, 100% Erfolgsquote)**:
   * **`LaserNodeRenderStateTest`**: Thread-sichere Render-State Extraktion, interpolierte Kinematik (Yaw/Pitch), 180°-Winkel-Wrap und alle 6 `FACING`-Ausrichtungen (`UP`, `DOWN`, `NORTH`, `SOUTH`, `WEST`, `EAST`).
   * **`DataGeneratorsTest`**: Event-Handling für `GatherDataEvent`, Client/Server-Provider-Registrierung und HolderLookup-Lifecycle.
   * **`ModBlockStateProviderTest`**: 6-Achsen Euler-Winkel-Transformation (`rotX`, `rotY`) für `FACING` Split-Modell Basisplatten und `cubeAll` Generierung.
   * **`ModItemModelProviderTest`**: Parent-Referenzen auf Block-Basen (`laser_base`) und 2D-Item-Modelle (`backflip_tool`).
   * **`ModLanguageProviderTest`**: Symmetrische I18n- und L10n-Übersetzungen für `en_us` und `de_de` via `ModEnglishLanguageProvider` und `ModGermanLanguageProvider`.
   * **`ModI18nTest`**: Strict Lowercase-Taxonomie-Validierung, Duplikatsfreiheit und 100% Symmetrie-Coverage für alle Keys aus `ModI18n`.
   * **`ModLootTableProviderTest`**: `BlockLootSubProvider` Factory, Self-Drop-Logik und Vollständigkeitsprüfung via `getKnownBlocks()`.
   * **`ShipCollisionMathTest`**: Continuous Swept-AABB Extrusion & BitSet-Linearisierung.
   * **`ShipStateTest`**: Domain-Zustand, AABB-Neuberechnung, Controller-Translation, Cooldown-Arithmetik.
   * **`CombatLogicTest`**: 3D-DDA Ray-Traversal, Normalenflächen (`WEST`, `DOWN`), Fehlschuss- & Reichweitenbegrenzung, Tier-Konfigurationen.
   * **`PayloadSerializationTest`**: Symmetrische Serialisierung aller 10 Custom-Payloads via `FriendlyByteBuf` & `StreamCodecs`.
   * **`SpaceshipEnergyManagerTest`**: Multi-Reaktor-Bündelung, sequenzieller FE-Drain, Transaktionssicherheit (Rollback).
   * **`AimTransformMathTest`**: Quaternion-Transformationen, Euler-Winkel-Konvertierung, 16-Bit Kompression und GimbalLimits.
   * **`TurretSeatTest`**: TurretSeat DTO Attribute, NBT-Persistenz und Aim-Lock-Status.
2. **NeoForge GameTests (`@GameTestHolder`, 18 Tests auf Dedicated GameTest-Server, 100% Erfolgsquote)**:
   * **`ShipCollisionGameTests` (10 Tests)**:
     - `testOffVsOff_StandardCollision`: Gegenseitige Zerstörung von Hüllenvoxeln bei OFF vs. OFF.
     - `testOffVsOff_ExplosionDamage`: 5x5x8 Matrix (200 distinkte Voxel) mit Cluster-Explosionen im kinetischen Schwerpunkt.
     - `testOffVsOn_Absorption`: Kinetischer Aufprall auf aktiven Schild mit FE-Absorption und Translations-Stopp.
     - `testOffVsOn_ShieldCollapse`: Kinetischer Aufprall mit Energiemangel $\rightarrow$ Schild bricht zusammen.
     - `testOffVsOn_PointZeroBoundary`: Kritisches Grenzverhalten: Bei exakt 0 FE nach Aufprall bricht das Schild sofort zusammen (`isShieldActive = false`).
     - `testOnVsOff_Drill`: Fräs-/Bohrmodus: Schild schneidet durch Hüllenblöcke bei intaktem Schiffsmomentum.
     - `testOnVsOff_MidDrillCollapse`: 5x2x1 Schnitt (10 distinkte Voxel): Energiemangel mitten im Bohrvorgang bricht Schild ab und stoppt Bewegung.
     - `testOnVsOff_FloatingBlocksUpdate`: Lifecycle-Validierung: Zerstörung des Trägerblocks mit `Block.UPDATE_ALL` droppt abhängige Blöcke (Fackeln) asynchron als Item-Entities (`Items.TORCH`).
     - `testOnVsOn_StandardClash`: Schild-gegen-Schild Kollision mit beidseitigem FE-Drain und Stopp.
     - `testOnVsOn_AsymmetricCollapse`: Asymmetrischer Zusammenbruch des energieärmeren Schildes bei Kollision.
     - `testMultiCollision_ShieldPriority`: 3-Wege-Kollision (Schiff A schneidet gleichzeitig in Schild B und ungeschützte Hülle C): Schild-Blockade stoppt Schiff A deterministisch via `CollisionResolver.resolveMultiple()`, schützt Hülle C vor Phantom-Durchdringung.
   * **`ShipScannerGameTests`**: Validierung des BFS-Scanners, Ausschluss diagonaler Blöcke, Multiblock-Ergänzung (Türen).
   * **`ShipMovementGameTests`**: Physische Schiffstranslation im Testlevel mit `AIR`-Hinterlassung und Zielblock-Präsenz.
   * **`ShipAttachmentGameTests`**: Persistenz von `ModAttachments.SHIP_ID` an BlockEntities.
   * **`SpaceshipGameTests`**: Schiffserstellung über Kontrollblöcke.
3. **GitHub Actions CI/CD Pipeline (`.github/workflows/ci.yml`)**:
   * Vollautomatischer Workflow für `push` und `pull_request` auf `main`.
   * Sequenzielle Pipeline: `compileJava` $\rightarrow$ `test` $\rightarrow$ `runGameTestServer` $\rightarrow$ `build` $\rightarrow$ Artefakt-Upload (`peaceman_alpha-*.jar`).
