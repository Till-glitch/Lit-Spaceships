# Lit Spaceships: Mod-Architektur & Klassendesign

Dieses Dokument beschreibt die Architektur, Datenflüsse, Klassenhierarchien und mathematischen Modelle des Spaceship-, Schutzschild- und Raumkampf-Systems für **Minecraft 1.21.1 (NeoForge 21.1.x)** inklusive aller Concurrency-, Lifecycle- und Edge-Case-Absicherungen.

---

## 1. Architektur-Übersicht & Design-Prinzipien

Das Gesamtsystem folgt einer strikten **Model-View-Controller (MVC) / Service-Architektur** mit vollständiger Entkopplung zwischen logischem Server und Client:

1. **Server (Domain & Authoritative Services)**:
   * Hält die alleinige Autorität über alle Schiffe (`ShipState`), persistiert ausschließlich reine Domain-Daten (`ShipSavedData`) und berechnet komplexe Schiffsgeometrien asynchron oder zeitbegrenzt.
   * **Kampf- & Raycast-Mathematik (`com.lit.spaceships.ship.combat.*`)**: Nutzt den **Amanatides-and-Woo 3D-DDA-Algorithmus** (`FastVoxelTraversal`) in Kombination mit einer zweistufigen Broadphase-/Narrowphase-Filterung (`LaserRaycastUtil`), um Strahlen kollisionsgenau in $O(\text{Ray-Länge})$ anstelle von $O(\text{Blockanzahl})$ zu berechnen.
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

        class ShieldZone {
            <<record>>
            +byte id
            +BlockPos generatorPos
            +int currentEnergy
            +int maxEnergy
            +long cooldownUntil
            +isCollapsed(long currentTick) boolean
            +withEnergy(int newEnergy) ShieldZone
            +withEnergyAndCooldown(int newEnergy, long newCooldown) ShieldZone
        }

        class ShipState {
            -UUID id
            -BlockPos controllerPos
            -Set~BlockPos~ blocks
            -Map~String, BlockPos~ homes
            -List~BlockPos~ reactors
            -List~BlockPos~ shields
            -List~BlockPos~ weapons
            -Map~Byte, ShieldZone~ shieldZones
            -boolean isShieldActive
            -long shieldCooldownUntil
            -long movementCooldownUntil
            -VoxelGridCache hullVoxelCache
            -VoxelGridCache shieldVoxelCache
            +getImmutableBlockSnapshot() Set~BlockPos~
            +getShieldZone(byte id) ShieldZone
            +setShieldZone(ShieldZone zone) void
            +setShieldZones(Map~Byte, ShieldZone~ zones) void
            +updateShieldZoneEnergy(byte id, int energy) void
            +updateShieldZoneEnergyAndCooldown(byte id, int energy, long cooldown) void
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
            +syncShieldZoneStates(Level level, ShipState ship)$ void
            +calculateShieldActiveMask(ShipState ship, long gameTime)$ long
            +onServerStarted(ServerStartedEvent event)$ void
            +onChunkSent(ChunkWatchEvent.Sent event)$ void
        }

        class ShipScannerService {
            +MAX_SHIP_BLOCKS int$
            +MAX_SHIELD_GENERATORS int$
            +scan(Level level, BlockPos startPos)$ Set~BlockPos~
            +calculateVoronoiZones(VoxelGridCache cache, List~BlockPos~ generators, BlockPos controllerPos)$ void
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
            -processHullDrilling(Level level, ShipState targetShip, BlockPos weaponPos, AbstractLaserNodeBE laserBe, LaserWeaponTier tier, BlockPos targetPos, Vec3 worldHitPos)$ void
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
            +byte shieldId
            +isHit() boolean
            +isShipHit() boolean
        }

        class VoxelGridCache {
            +BlockPos minOffset
            +BitSet bitSet
            +byte[] shieldMap
            +isSet(int x, int y, int z) boolean
            +getShieldId(int x, int y, int z) byte
            +setShieldId(int x, int y, int z, byte shieldId) void
            +buildFromAbsolute(Collection~BlockPos~ abs, BlockPos ctrl)$ VoxelGridCache
        }

        class ShipMovementService {
            +TICK_BUDGET_NANOS long$
            +SHIP_TICKET TicketType~ChunkPos~$
            +moveShip(Level level, ShipState ship, int dx, int dy, int dz, Player player)$ void
            +rotateShip(Level level, ShipState ship, Rotation rotation, Player player)$ void
            +isShipMoving(UUID shipId)$ boolean
            +onServerTick(ServerTickEvent.Post event)$ void
        }

        class BlockDependencyGraph {
            +addNode(BlockPos oldPos, BlockPos newPos, BlockState state, CompoundTag nbt) RelocationNode
            +buildDependencies(Level level) void
            +resolveTopologicalBatches() List~List~RelocationNode~~
        }

        class RelocationNode {
            +oldPos BlockPos
            +newPos BlockPos
            +state BlockState
            +nbt CompoundTag
            +addDependency(RelocationNode dependency) void
            +getDependencies() Set~RelocationNode~
            +getDependents() Set~RelocationNode~
        }

        class BlockRelocationRegistry {
            +registerHandler(IBlockRelocationHandler handler)$ void
            +isImmune(BlockState state)$ boolean
            +isCluster(BlockState state)$ boolean
            +dispatchPreRelocation(BlockPos pos, BlockState state, BlockEntity be, CompoundTag nbt, RelocationContext ctx)$ void
            +dispatchPostRelocation(BlockPos oldPos, BlockPos newPos, BlockState state, BlockEntity be, RelocationContext ctx)$ void
        }

        class IBlockRelocationHandler {
            <<interface>>
            +shouldHandle(BlockState state) boolean
            +onPreRelocation(BlockPos pos, BlockState state, BlockEntity be, CompoundTag nbt, RelocationContext ctx) void
            +onPostRelocation(BlockPos oldPos, BlockPos newPos, BlockState state, BlockEntity be, RelocationContext ctx) void
            +getPriority() int
        }

        class RelocationContext {
            <<record>>
            +ServerLevel level
            +ShipState ship
            +int dx
            +int dy
            +int dz
            +Rotation rotation
        }

        class ShipRotationMath {
            +rotateRelativeBlockPos(BlockPos relPos, Rotation rot)$ BlockPos
            +rotateAbsoluteBlockPos(BlockPos pos, BlockPos pivot, Rotation rot)$ BlockPos
            +rotateEntityPos(Vec3 entityPos, BlockPos pivot, Rotation rot)$ Vec3
            +rotateYaw(float yaw, Rotation rot)$ float
            +normalizeYaw(float yaw)$ float
        }

        class ShipCollisionService {
            +SECTOR_SIZE double$
            +calculateSweptAABB(AABB currentBox, double dx, double dy, double dz)$ AABB
            +calculateTimeOfImpact(AABB movingBox, AABB targetBox, Vec3 velocity)$ double
            +calculateIntersection(AABB a, AABB b)$ Optional~AABB~
            +findPotentialCollisions(ShipState movingShip, Vec3 moveVec)$ List~BroadPhaseCandidate~
            +calculateVoxelIntersection(ShipState shipA, BlockPos originA, ShipState shipB, BlockPos originB, AABB intersectionBox)$ VoxelCollisionResult
            +checkRotationCollisions(ServerLevel level, ShipState ship, Rotation rotation)$ boolean
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
            +calculateRotationCost(ShipState ship, Rotation rotation)$ int
            +getTotalAvailableEnergy(Level level, ShipState ship)$ int
            +consumeEnergy(Level level, ShipState ship, int amount)$ void
            +tryConsumeEnergyAmount(Level level, ShipState ship, int amount)$ boolean
            +tryConsumeFlightEnergy(Level level, ShipState ship, int dx, int dy, int dz, Player player)$ boolean
            +tryConsumeRotationEnergy(Level level, ShipState ship, Rotation rotation, Player player)$ boolean
            +distributeEnergyToShields(Level level, ShipState ship)$ int
            +distributeEnergyToShields(int availableEnergy, ShipState ship, long currentGameTime)$ int
        }
    }

    %% ==========================================
    %% NETWORK LAYER
    %% ==========================================
    namespace Network_Layer {
        class ModPayloads {
            +register(IEventBus bus)$ void
        }

        class ShieldZoneStatePayload {
            <<record>>
            +UUID shipId
            +long activeMask
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
            +updateMesh(Map~BlockPos, Byte~ relativeBlocks) void
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

### C. Orthogonale 90°-Rotations-Transformation (`ShipRotationMath`)
Für 90°-Drehungen um den Spaceship-Controller als Pivot $(px, pz)$ wird eine diskrete 2D-Rotationsmatrix auf der horizontalen X/Z-Ebene angewendet:
1. **Relative Voxel-Koordinaten $(rx, rz)$**:
   * **90° CW (Rechtsdrehung)**:
     $$\begin{pmatrix} rx' \\ rz' \end{pmatrix} = \begin{pmatrix} 0 & -1 \\ 1 & 0 \end{pmatrix} \begin{pmatrix} rx \\ rz \end{pmatrix} = \begin{pmatrix} -rz \\ rx \end{pmatrix}$$
   * **90° CCW (Linksdrehung)**:
     $$\begin{pmatrix} rx' \\ rz' \end{pmatrix} = \begin{pmatrix} 0 & 1 \\ -1 & 0 \end{pmatrix} \begin{pmatrix} rx \\ rz \end{pmatrix} = \begin{pmatrix} rz \\ -rx \end{pmatrix}$$
2. **BlockState & Direction Transformation**:
   * `BlockState.rotate(level, pos, Rotation.CLOCKWISE_90 / COUNTERCLOCKWISE_90)` rotiert `FACING`, `HORIZONTAL_FACING` sowie Treppen-, Tür- und Hebel-Zustände konsistent.
3. **Passagier- & Kamera-POV-Rotation**:
   * Fließkomma-Positionen rotieren um das Pivot-Blockzentrum $(px + 0.5, pz + 0.5)$:
     $$x' = (px + 0.5) - (z - (pz + 0.5)), \quad z' = (pz + 0.5) + (x - (px + 0.5)) \quad (\text{für CW})$$
   * Der Blickwinkel (Yaw) wird additiv transformiert und auf $[-180^\circ, 180^\circ]$ normalisiert:
     $$\text{Yaw}' = \text{normalize}(\text{Yaw} \pm 90.0^\circ)$$
4. **Laser-Turret & Orientation-Synchronisation**:
   * Ausgerichtet bemannte und unbemannte Turrets rotieren ihren Ziel-Yaw (`rotateTurret`) synchron mit der Schiffsachse, um die relative Zielrichtung beizubehalten.

### C. 3D-Voronoi-Tesselierung & Proportionales Defizit-Routing
1. **3D-Voronoi-Partitionierung (`ShipScannerService`)**:
   Jedem Voxel des Schiffs wird der nächstgelegene Schildgenerator $g \in \{1, \dots, N\}$ ($N \le 64$) über die quadrierte euklidische Distanz zugewiesen:
   $$g^*(x,y,z) = \arg\min_{g} \left( (x - x_g)^2 + (y - y_g)^2 + (z - z_g)^2 \right)$$
   Grenzflächen mit identischer Distanz werden deterministisch über die niedrigere Generator-ID aufgelöst ($g_1 < g_2$).
   Die Ergebnisse werden in einem linearen Flach-Array `byte[] shieldMap` ($O(1)$ Indizierung) innerhalb von `VoxelGridCache` abgelegt.

2. **Proportionales Defizit-Routing (`SpaceshipEnergyManager`)**:
   Verfügbare Energie $E_{\text{avail}}$ wird im Verhältnis der Zonendefizite $D_i = E_{\text{max}, i} - E_{\text{curr}, i}$ auf alle aktiven Zonen (nicht kollabiert, kein Cooldown) verteilt:
   $$E_{\text{transfer}, i} = \left\lfloor E_{\text{avail}} \cdot \frac{D_i}{D_{\text{total}}} \right\rfloor, \quad \text{wobei } D_{\text{total}} = \sum_{j} D_j$$
   Ein nachgelagerter Rest-Tröpfchen-Loop verteilt verbleibende Rest-FE ($E_{\text{rest}} = E_{\text{avail}} - \sum E_{\text{transfer}, i}$) deterministisch (+1 FE pro Zone mit Restdefizit) für 100% verlustfreie Energieerhaltung.

3. **64-Bit Bitmasken-Synchronisation (`ShieldZoneStatePayload`)**:
   Zonen-Aktivitätszustände werden bitweise in einer einzelnen `long activeMask` gebündelt:
   $$\text{Bit } k = 1 \iff \text{Zone } (k+1) \text{ ist intakt} \quad (k \in [0, 63])$$
   Übermittlung an Clients erfolgt in unter 32 Bytes per Frame. Im Client-Shader (`hex_shield.fsh`) können inaktive Schildzonen lokal gerendert oder ausgeblendet werden.

---

## 4. Datenfluss & Lebenszyklus-Matrix

| Aktion | Auslöser / Schicht | Ausführung | Netzwerk / Persistenz / Render |
| :--- | :--- | :--- | :--- |
| **Impuls-Laser abfeuern** | Pilot drückt `FIRE_PULSE` | `ServerPayloadHandler` $\rightarrow$ `LaserCombatService.fireWeapon()` | Zieht 250 FE ab, raycastet via `LaserRaycastUtil`. Zerstört 1 Block sofort (`destroyBlock` bzw. `SHIP_HULL`-Delta). Sendet `LaserFirePayload`. |
| **Dauerstrahl umschalten** | Pilot drückt `TOGGLE_HEAVY_BEAM` | `ServerPayloadHandler` $\rightarrow$ `LaserCombatService.fireWeapon()` | Schaltet `isFiring` im BE um, sendet `LaserStateSyncPayload`. BE konsumiert im `serverTick` 50 FE/Tick und führt progressiven Abbau durch. |
| **Energiemangel bei Dauerfeuer** | Reaktor leer (`tryConsumeEnergyAmount == false`) | `HeavyBeamBlockEntity.serverTick()` | Schaltet sich sofort ab (`setFiring(false)`), bricht Drill ab und sendet `LaserStateSyncPayload(isFiring = false)`. |
| **Universelle DAG-Relokation & Mod-Kompatibilität** | Navigation / Helm `MOVE` / `ROTATE` | `ShipMovementService` $\rightarrow$ `BlockDependencyGraph` $\rightarrow$ `BlockRelocationRegistry` | Prüft `#c:relocation_immune` (bricht bei Bedrock/Portalen ab). Baut dynamischen DAG via `canSurvive` & `isFaceSturdy`. Löst Zyklen via Tarjan SCC. Löscht $P_{\text{alt}} \setminus P_{\text{neu}}$ mit Flag 48 (Y absteigend). Platziert topologische Batches mit Flag 52. Restauriert BlockEntities (`loadStatic`, `clearRemoved()`), ruft `IBlockRelocationHandler` Lifecycle-Hooks auf und synchronisiert via Flag 50 ohne Item-Drops oder X-Ray-Flackern. |
| **Orthogonale 90°-Rotation** | Helm `KEY_ROTATE_LEFT/RIGHT` / GUI-Button | `ServerPayloadHandler` $\rightarrow$ `ShipMovementService.rotateShip()` | Prüft Pre-Collision (`ShipCollisionService.checkRotationCollisions`) und Reaktor-Energie (`tryConsumeRotationEnergy`). Rotiert Voxel, NBT, Passagiere & Turrets im Time-Slicing Tick-Budget. Sendet `ShipStructureSyncPayload` & `ShieldBubbleSyncPacket`. |
| **Laser-Rendering (Client)** | Render-Frame (`AFTER_TRANSLUCENT_BLOCKS`) | `LaserBeamRenderer.onRenderLevelStage()` | Berechnet Strahlenursprung aus Schiffsanker + relativem Offset. Führt `level.clip()` aus $\rightarrow$ Strahl stoppt exakt auf der Blockoberfläche. Rendert Quads additiv (`GL_ONE`). |
| **VRAM & Laser-Freigabe** | Chunk entlädt / Schiff gelöscht / Logout | `ClientShipManager` & `ClientLaserState` | `ClientLaserState.removeBeamsForShip(shipId)` räumt Laser auf; `ClientShipState.dispose()` schließt VBOs im GL-Thread. |

### Weltraum-Weltgen: Datapack-Registry-Abhängigkeitsgraph

Alle Weltraum-Weltgen-Assets werden ausschließlich über Java-DataGen erzeugt:
`ModWorldGenProvider` (extends `DatapackBuiltinEntriesProvider`) koppelt einen
`RegistrySetBuilder` (`Registries.BIOME`, `Registries.CONFIGURED_FEATURE`, `Registries.PLACED_FEATURE`)
in `DataGenerators.java` unter `event.includeServer()`; `./gradlew runData` schreibt die JSONs
nach `src/generated/resources`. Manuelle JSON-Dateien unter `data/lit_spaceships/worldgen/` sind verboten.

| Datapack-Registry | Schlüssel | Bootstrap | Abhängigkeiten / Platzierungs-Mathe |
| :--- | :--- | :--- | :--- |
| `worldgen/biome` | `lit_spaceships:space_biome` | `ModBiomes` | → `asteroid_placed`, `space_wreck_placed` (Deko-Stufe 0); schwarzer Himmel/Nebel (0), `MobSpawnSettings.EMPTY` |
| `worldgen/biome` | `lit_spaceships:plasma_nebula` | `ModBiomes` | Violetter Nebel `#7F00FF`, dunkelvioletter Himmel `#1A0033`, violette `minecraft:dust`-Glanzpartikel (p = 0.006), keine Spawns, leere Feature-Liste |
| `worldgen/biome` | `lit_spaceships:frozen_expanse` | `ModBiomes` | Bleich-cyanfarbener Nebel `#00FFFF`, dunkler Cyan-Himmel `#003344`, `minecraft:snowflake`-Partikelströme (p = 0.015), keine Spawns → `ice_comet_placed` (Stufe 0) |
| `worldgen/biome` | `lit_spaceships:void_wastes` | `ModBiomes` | Vollkommene Schwärze (0), KEIN Partikel/Mood-Sound (sensorische Deprivation), keine Spawns → `asteroid_placed` + `wreck_field_placed` (Stufe 0) |
| `worldgen/configured_feature` | `lit_spaceships:asteroid` | `ModConfiguredFeatures` | → Runtime-Feature `lit_spaceships:asteroid` (`ModFeatures`, `AsteroidFeature`) |
| `worldgen/configured_feature` | `lit_spaceships:space_wreck` | `ModConfiguredFeatures` | → Runtime-Feature `lit_spaceships:space_wreck` (`ModFeatures`, `SpaceWreckFeature`) |
| `worldgen/configured_feature` | `lit_spaceships:ice_comet` | `ModConfiguredFeatures` | → Runtime-Feature `lit_spaceships:ice_comet` (`ModFeatures`, `IceCometFeature`) |
| `worldgen/configured_feature` | `lit_spaceships:mega_asteroid` | `ModConfiguredFeatures` | → Runtime-Feature `lit_spaceships:mega_asteroid` (`ModFeatures`, `MegaAsteroidFeature`) |
| `worldgen/placed_feature` | `lit_spaceships:asteroid_placed` | `ModPlacedFeatures` | → `lit_spaceships:asteroid`; Count 4, InSquare, Uniform $Y \in [-40, 280]$, Biome-Filter |
| `worldgen/placed_feature` | `lit_spaceships:space_wreck_placed` | `ModPlacedFeatures` | → `lit_spaceships:space_wreck`; Rarity 1/32, InSquare, Uniform $Y \in [0, 200]$, Biome-Filter |
| `worldgen/placed_feature` | `lit_spaceships:ice_comet_placed` | `ModPlacedFeatures` | → `lit_spaceships:ice_comet`; Count 8 (hohe Dichte), InSquare, Uniform $Y \in [-40, 280]$, Biome-Filter |
| `worldgen/placed_feature` | `lit_spaceships:wreck_field_placed` | `ModPlacedFeatures` | → `lit_spaceships:space_wreck` (Feature-Wiederverwendung); Rarity 1/4 (8x Dichte), InSquare, Uniform $Y \in [0, 200]$, Biome-Filter |
| `worldgen/placed_feature` | `lit_spaceships:mega_asteroid_placed` | `ModPlacedFeatures` | → `lit_spaceships:mega_asteroid`; Rarity 1/96 (Chunk-Generierungs-Budget für 40-70 Blöcke Durchmesser), InSquare, Uniform $Y \in [-40, 280]$, Biome-Filter. Radiale Schichten via `MegaAsteroidFeature.radialLayer` (Kruste → Erz-Mantel → Kaverne → Kalzit → Amethyst/sprossend → Geodenluft), in-World per GameTest verifiziert |
| `worldgen/noise_settings` | `lit_spaceships:space_noise` | `ModNoiseSettings` | Konstante Dichte $-1$ (reiner Void, keine Terrain-Geometrie); Temperatur = `minecraft:temperature`-Noise, Feuchte (vegetation) = `minecraft:vegetation`-Noise (beide Multi-Noise-Achsen), Rest 0 |
| `dimension_type` | `lit_spaceships:space_type` | `ModDimensions::bootstrapDimensionType` | Kosmische Nacht (`fixed_time` 18000), kein Skylight/Ceiling, $Y \in [-64, 320]$, Betten verboten, Respawn-Anker erlaubt, `monster_spawn_light_level` 0 |
| `dimension` (LEVEL_STEM) | `lit_spaceships:space` | `ModDimensions::bootstrapLevelStem` | `NoiseBasedChunkGenerator` + `minecraft:multi_noise` Biome-Quelle als lückenlose Rechteck-Partition über Temperatur × Feuchte: `frozen_expanse` (Temp $[-1.0, -0.3]$, Feuchte beliebig), `void_wastes` (Temp $[-0.3, 0.4]$, Feuchte $[-1.0, 0.0]$), `space_biome` (Temp $[-0.3, 0.4]$, Feuchte $[0.0, 1.0]$), `plasma_nebula` (Temp $[0.4, 1.0]$, Feuchte beliebig) → 3D-volumetrische Biome-Zonen |

---

## 5. Testing-Architektur & CI/CD Pipeline

Das Projekt erzwingt kontinuierliche Testabdeckung gemäß der **70/20-Regel**:

1. **JUnit 5 & Mockito Suite (159 Tests, 100% Erfolgsquote)**:
   * **`VirtualSupportTestViewTest`**: Datengetriebenes Support-Probing über virtuelle Nachbar-Maskierung mit `state.canSurvive()` (löst alle hardcodierten `instanceof`-Ketten für Mod-Attachables ab).
   * **`NbtCoordinateRemapperTest`**: Rekursives Umschreiben von internen `BlockPos`-Referenzen (`masterPos`, `controllerPos`, Int-Arrays, Longs) in BlockEntity-NBTs für Master-Slave-Multiblöcke.
   * **`BlockDependencyGraphTest`**: Validierung der gerichteten Kantenbildung via `canSurvive` und `isFaceSturdy` für Multiblöcke (Türen, Betten, ausgefahrene Pistons Base $\rightarrow$ Head) und Wand-Attachables (Wandfackeln); Prüfung der Schicht-Linearisierung.
   * **`CycleDetectionTest`**: Validierung von Tarjan's SCC-Algorithmus zur Erkennung und Bündelung von Zyklen ($A \leftrightarrow B$) in simultane Injektions-Cluster.
   * **`BlockRelocationRegistryTest`**: Immunitäts-Validierung für Vanilla-Weltblöcke (`BEDROCK`, `END_PORTAL`, `COMMAND_BLOCK`, `BARRIER`) sowie Registrierung, Prioritäts-Sortierung und Lifecycle-Dispatching (`onPreRelocation`, `onPostRelocation`) für `IBlockRelocationHandler`.
   * **`ShipMovement3PassTest`**: Validierung der 3-Pass-Klassifizierung (`PASS_1_SOLIDS`, `PASS_2_ROOTS_AND_NORMALS`, `PASS_3_ATTACHABLES_AND_TOPS`) für Vollblöcke, untere/obere Türhälften, Betten, Treppen, Fackeln, Redstone, Piston-Köpfe und $Y$-Sortierung.
   * **`ShipMovementFragileSortingTest`**: Validierung von `isFragileBlock` für Redstone Wire, Fackeln, Repeater, Hebel vs. Vollblöcke & Luft; Überprüfung der absteigenden Y-Entfernungs- und aufsteigenden Y-Platzierungs-Sortierung.
   * **`ShipRotationMathTest`**: Orthogonale 90° CW / CCW Transformation für alle 4 Quadranten, Pivot-Verschiebungen, Fließkomma-Entitätsrotationen um das Pivot-Zentrum, Yaw-Normalisierung über $[-180^\circ, 180^\circ]$, 4x 90° Identitätsinvarianz und `BlockState.rotate` für Directional Facing.
   * **`VoxelGridCacheShieldTest`**: $O(1)$ Flach-Array `byte[] shieldMap` Adressierung, Rand- und Out-of-Bounds-Absicherung im `VoxelGridCache`.
   * **`ShipStateShieldZoneTest`**: Thread-sichere CRUD-Operationen auf `shieldZones`, `isCollapsed`-Auswertung bei Cooldown und Energiemangel.
   * **`VoronoiTessellationTest`**: 3D-Voronoi-Tesselierung über quadrierte euklidische Distanz, deterministischer ID-Tie-Break und 64-Generatoren-Cap.
   * **`ProportionalEnergyRoutingTest`**: Proportionale FE-Verteilung im Verhältnis der Zonendefizite ($D_i / D_{total}$) und Ausschluss kollabierter Zonen.
   * **`EnergyRoutingRemainderTest`**: Exakter Rest-Tröpfchen-Loop (+1 FE) für verlustfreie Energieerhaltung bei krummen Primzahl-Werten (3333 FE auf 7 Generatoren).
   * **`FastVoxelTraversalShieldTest`**: 3D-DDA-Traversierung mit extrahierter `shieldId` im `VoxelHit` bei Treffern auf Hülle und Schild.
   * **`ShieldZonePayloadSerializationTest`**: Bit-genaue 64-Bit Bitmasken-Serialisierung und -Dekodierung in $< 32$ Bytes via `ShieldZoneStatePayload`.
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
   * **`PayloadSerializationTest`**: Symmetrische Serialisierung aller 12 Custom-Payloads via `FriendlyByteBuf` & `StreamCodecs`.
   * **`SpaceshipEnergyManagerTest`**: Multi-Reaktor-Bündelung, sequenzieller FE-Drain, Transaktionssicherheit (Rollback).
   * **`AimTransformMathTest`**: Quaternion-Transformationen, Euler-Winkel-Konvertierung, 16-Bit Kompression und GimbalLimits.
   * **`TurretSeatTest`**: TurretSeat DTO Attribute, NBT-Persistenz und Aim-Lock-Status.
2. **NeoForge GameTests (`@GameTestHolder`, 26 Tests auf Dedicated GameTest-Server, 100% Erfolgsquote)**:
   * **`ShipScannerVoronoiGameTest`**: Voronoi-Zonierung und ShieldZone-Erfassung bei mehreren Schildgeneratoren im Schiff.
   * **`LaserCombatPiercingGameTest`**: Zonen-Kollaps und Durchschlag auf darunterliegende Schiffshülle bei inaktiver ShieldZone.
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
   * **`ShipScannerGameTests` (4 Tests)**:
     - `testShipScannerConnectedBlocks`: Validierung des BFS-Scanners für verbundene Blöcke.
     - `testShipScannerDiagonalIgnored`: Ausschluss diagonaler Blöcke.
     - `testShipScannerDoorMultiblock`: Multiblock-Ergänzung für zweiflügelige Türen.
     - `testShipScannerPistonMultiblock`: Multiblock-Ergänzung für ausgefahrene Pistons und Piston-Heads.
   * **`ShipMovementGameTests` (6 Tests)**:
     - `testShipMovementRelocation`: Physische Schiffstranslation im Testlevel mit `AIR`-Hinterlassung und Zielblock-Präsenz.
     - `testShipMovementPreservesShieldEnergy`: Erhaltung der Schildzonen-Energie und Generator-Zuordnung bei Schiffstranslation.
     - `testShipMovementDownWithRedstoneAndTorch`: Abwärtsbewegung mit Redstone Wire & Fackel; Verifikation der korrekten Platzierung und 0 Item-Drops.
     - `testMovement_PreservesTorchesAndDoors`: Topologische Validierung: Verschiebung einer Wand mit Fackel und einer zweiflügeligen Tür mit Erhalt aller Hälften und 0 Item-Drops.
     - `testMovement_BlockedByImmuneBlock`: Immunitäts-Validierung: Verschiebung eines Schiffs mit unzerstörbarem `BEDROCK` wird präemptiv abgebrochen; Blöcke bleiben unbewegt.
     - `testMovement_PreservesExtendedPistons`: Erhaltung ausgefahrener Pistons (Base + Head nach oben) ohne Item-Drops oder Abbrechen.
   * **`ShipAttachmentGameTests`**: Persistenz von `ModAttachments.SHIP_ID` an BlockEntities.
   * **`SpaceshipGameTests`**: Schiffserstellung über Kontrollblöcke.
3. **GitHub Actions CI/CD Pipeline (`.github/workflows/ci.yml`)**:
   * Vollautomatischer Workflow für `push` und `pull_request` auf `main`.
   * Sequenzielle Pipeline: `compileJava` $\rightarrow$ `test` $\rightarrow$ `runGameTestServer` $\rightarrow$ `build` $\rightarrow$ Artefakt-Upload (`lit_spaceships-*.jar`).
