# Mod Alpha (NeoForge Spaceship Mod)

An advanced spaceship, energy shield, and naval combat mod for **Minecraft 1.21** built on **NeoForge**. It allows players to construct modular, functional spaceships from arbitrary blocks, fly them across the world with continuous swept collision and passenger handling, defend them using procedurally generated hexagonal energy shields, and engage in tactical space combat with pulse lasers, heavy continuous beams, and mining lasers.

---

## Features

* **Spaceship Control Block:** The heart and core of every ship. Detects connected blocks via breadth-first search (BFS), binds them to a ship entity, and manages lifecycle operations (creation, structure update, deletion) with real-time hull highlighting.
* **Spaceship Helm (Navigation & Combat Console):** Full 6-axis flight controls (Forward, Backward, Left, Right, Up, Down), integrated waypoint bookmarks for automated navigation, and cockpit tactical weapon triggers.
* **Spaceship Reactor:** Energy storage supporting standard **Forge Energy (FE)** with up to 1,000,000 FE capacity. Powers flight maneuvers, shield absorption, and laser weapon systems. *(Dev-Tip: Right-click with Redstone to charge 50,000 FE!)*
* **Shield Generator & Hex-Shader:** Protects ship blocks against explosive damage (TNT, Creepers) and unauthorized block manipulation. Shields consume reactor energy upon impact and are rendered as procedural hexagon bubble meshes via custom shaders with impact ripples and low-energy alerts.
* **Shipborne Laser Weapon System & Dynamic Turrets:**
  * **Pulse Laser:** High-energy burst cannon (250 FE/shot, 20 ticks cooldown). Instantly vaporizes 1 block on hit or inflicts massive shield drain with kinetic shockwaves.
  * **Heavy Beam:** High-intensity continuous combat beam (50 FE/tick). Progressively melts and burns through hull blocks and terrain with visual breaking animations.
  * **Mining Laser:** Continuous industrial excavation laser (25 FE/tick). Rapidly drills through asteroid stone, ores, and terrain without causing entity damage.
  * **Co-Pilot / Freelook Aiming System:** Right-click laser turrets to man the gunner seat (`TurretSeatEntity`). Aim turrets dynamically in real-time via camera freelook, stabilized against ship translation and rotation using quaternion transformations ($Q_{ship}^{-1} \otimes \vec{V}_{world} \otimes Q_{ship}$).
  * **Left-Click Aim Locking & Persistence:** Left-click while seated to lock/arretiere the turret's orientation with acoustic feedback and Action-Bar notification. When dismounting, during ship movement, and across interdimensional teleports, the locked angle remains permanently stored in NBT until intentionally realigned.
  * **Mechanical Gimbal Limits:** Strict joint angle clamping prevents turrets from cutting or firing into the ship's own hull.
  * **Delta-Tick Network Throttling:** 16-bit short compression and 20 Hz delta-throttling ($\Delta \ge 0.5^\circ$) eliminate network spam and deliver 0 ms client-predicted aiming response.
* **Deep Space Dimension (`peaceman_alpha:space`):**
  * **Infinite Void Environment:** Custom procedural dimension from $Y = -64$ to $Y = 320$ with permanent cosmic night, zero natural monster spawns, and no vanilla bedrock floors.
  * **Asteroid Fields & Ice Comets:** 3D procedural asteroid generation with diverse crusts (Stone, Basalt, Tuff, Deepslate) containing rich ore cores (Iron, Gold, Redstone, Diamond, Netherite Debris) and frozen ice comets.
  * **Derelict Spacecraft Wrecks:** Rare abandoned shipwrecks featuring intact spaceship reactor cores and ancient treasure chests (`END_CITY_TREASURE`).
* **Cross-Dimensional Ship Travel (Core Teleportation Service):**
  * Fully transactional 6-phase warp travel (Suspension, Forceloading, Clipboard Serialization, Excision, Materialization, Passenger Entity Transition) across any dimension with ticket locking and zero chunk-boundary ghosting.
* **Backflip Tool (Klasingscher Degen):** Developer item demonstrating entity manipulation by launching targets into the air with forced backflips.

---

## Architecture & Project Structure

The codebase is built on modern **NeoForge 1.21** best practices, adhering to a strict **Model-View-Controller (MVC) / Service-Layer** architecture with complete logical client/server decoupling.

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
            +clearDrillProgress(Level level) void
        }

        class ModAttachments {
            +Supplier~AttachmentType~UUID~~ SHIP_ID
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
            -VoxelGridCache hullVoxelCache
            -VoxelGridCache shieldVoxelCache
            +recalculateHullBounds() void
            +syncShieldBubbleToClients(Level level) void
        }

        class ServerShipManager {
            +Map~UUID, ShipState~ ACTIVE_SHIPS$
            +getShip(UUID shipId)$ ShipState
            +createShip(Level level, BlockPos startPos)$ ShipState
            +updateShipBlocks(Level level, ShipState ship)$ void
            +deleteShip(Level level, ShipState ship)$ void
            +onChunkSent(ChunkWatchEvent.Sent event)$ void
        }

        class LaserCombatService {
            +fireWeapon(Level level, ShipState shooter, BlockPos pos)$ boolean
            +tickContinuousWeapon(Level level, ShipState shooter, BlockPos pos, AbstractLaserNodeBE be)$ void
        }

        class LaserRaycastUtil {
            +raycast(Level level, UUID shooterId, Vec3 origin, Vec3 dir, double range, boolean terrain)$ RaycastHitResult
        }

        class FastVoxelTraversal {
            +traverse(VoxelGridCache cache, Vec3 origin, Vec3 dir, double maxDist)$ Optional~VoxelHit~
        }

        class ShipMovementService {
            +moveShip(Level level, ShipState ship, int dx, int dy, int dz, Player player)$ void
            +isShipMoving(UUID shipId)$ boolean
            +onServerTick(ServerTickEvent.Post event)$ void
        }

        class SpaceshipEnergyManager {
            +tryConsumeFlightEnergy(Level level, ShipState ship, int dx, int dy, int dz, Player player)$ boolean
            +tryConsumeEnergyAmount(Level level, ShipState ship, int amount)$ boolean
            +getTotalAvailableEnergy(Level level, ShipState ship)$ int
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
            +UUID shipId
            +CombatAction action
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

        class ShipStateSyncPayload {
            <<record>>
            +UUID shipId
            +int currentEnergy
            +boolean isShieldActive
            +long shieldCooldownRemainingTicks
            +long movementCooldownRemainingTicks
        }
    }

    %% ==========================================
    %% CLIENT SIDE VIEW & RENDERING
    %% ==========================================
    namespace Client_Side {
        class ClientShipState {
            -UUID shipId
            -BlockPos anchorPos
            -VertexBuffer shieldMesh
            -boolean isShieldActive
            +updateMesh(Set~BlockPos~ relativeBlocks) void
            +dispose() void
        }

        class ClientLaserState {
            +addPulse(UUID shooterId, Vec3 start, Vec3 end, LaserWeaponTier tier)$ void
            +setContinuousBeam(UUID shooterId, BlockPos pos, boolean firing, LaserWeaponTier tier)$ void
            +removeBeamsForShip(UUID shipId)$ void
            +clearAll()$ void
        }

        class LaserBeamRenderer {
            +onRenderLevelStage(RenderLevelStageEvent event)$ void
        }

        class ShieldRenderer {
            +renderShields(PoseStack stack, MultiBufferSource buffer, Camera camera, float partialTicks)$ void
        }
    }

    %% Relationships
    AbstractSpaceshipNodeBlockEntity ..|> ISpaceshipNode : implements
    AbstractLaserNodeBlockEntity --|> AbstractSpaceshipNodeBlockEntity : extends
    ServerShipManager o-- "0..*" ShipState : manages
    ServerShipManager ..> LaserCombatService : combat coordination
    LaserCombatService ..> LaserRaycastUtil : raycasts via
    LaserRaycastUtil ..> FastVoxelTraversal : 3D-DDA Voxel Scan
    LaserCombatService ..> LaserFirePayload : broadcasts pulse
    LaserCombatService ..> LaserStateSyncPayload : broadcasts beam state
    LaserCombatService ..> ShipStructureDeltaPayload : syncs block damage

    LaserBeamRenderer ..> ClientLaserState : reads active beams
    LaserBeamRenderer ..> ClientShipManager : anchors to ships
    ClientLaserState ..> ClientShipManager : relative anchor offsets
```

---

### Key Architectural Highlights

#### 1. Server-Side Domain & Services (`com.peaceman.alpha.ship.*`)
* **`ShipState`**: Pure domain Model holding authoritative ship data (UUID, controller position, functional block lists, reactor/shield associations, weapons list, waypoints, and `VoxelGridCache` bitsets). Contains **zero** client/rendering dependencies.
* **`ServerShipManager`**: Central lifecycle and CRUD controller. Coordinates ship creation, updates, and spatial hashing distribution.
* **`LaserCombatService`**: Handles combat routing for pulse weapons and continuous beams, energy transactions, kinetic shield shockwaves, and progressive block destruction with `destroyBlockProgress` scaling by block hardness.
* **`LaserRaycastUtil` & `FastVoxelTraversal`**: High-performance Amanatides & Woo 3D Digital Differential Analyzer (3D-DDA) traversing voxels in $O(\text{Ray Length})$ time, protected by broadphase AABB intersection filters and step bounds (1024 steps).
* **`ShipMovementService`**: Translates blocks and passengers using an incremental **Time-Slicing Tick-Budget (10ms per tick)** executed during `ServerTickEvent.Post`, with chunk region tickets (`TicketType`) and translation-invariant weapon tracking.

#### 2. Network Layer (`com.peaceman.alpha.network.*`)
* **`CustomPacketPayload` Records**: 100% typed payload definitions using Mojang/NeoForge `StreamCodec` composites.
* **`ShipCombatActionPayload`**: Dispatches pilot weapon commands (`FIRE_PULSE`, `TOGGLE_HEAVY_BEAM`, `TOGGLE_MINING_LASER`, `FIRE_ALL`).
* **`LaserFirePayload` & `LaserStateSyncPayload`**: Broadcasts visual laser events and continuous beam states to chunk-tracking clients.
* **`ShipStructureDeltaPayload`**: Transmits destroyed voxel lists during combat to update client highlight meshes without re-sending the entire ship structure.

#### 3. Client View Model & Rendering (`com.peaceman.alpha.client.*`)
* **`ClientLaserState`**: Thread-safe collection (`ConcurrentHashMap`, `CopyOnWriteArrayList`) managing pulse fadeouts and continuous beams keyed by invariant relative offsets (`shooterShipId + "_" + relativePos.asLong()`).
* **`LaserBeamRenderer`**: Volumetric Blaze3D billboard beam rendering with additive blending (`GL_ONE`), core/glow dual-cylinder quads, oscillating pulses, and client-side block surface clipping (`level.clip`) preventing laser pass-through.
* **`ShieldRenderer`**: Blaze3D rendering pipeline consuming compiled VBOs (`VertexBuffer`) from `ClientShipState`.
* **VRAM Lifecycle Safety**: `ClientShipManager` and `ClientLaserState` guarantee immediate GPU buffer disposal on chunk unload, ship deletion, or client logout.

---

## Testing & Quality Assurance

The project enforces continuous testing according to the **70/20 Rule** (70% Unit / Math Tests, 20% Engine GameTests, 10% Manual QA).

### Automated Test Matrix (33 Unit Tests & 4 GameTest Suites)

| Test-Suite | Typ | Abdeckung |
| :--- | :--- | :--- |
| **`ShipCollisionMathTest`** | JUnit 5 | Continuous Swept-AABB Extrusion (positive/negative/zero), VoxelGridCache BitSet Indexing & Bounds. |
| **`ShipStateTest`** | JUnit 5 | Domain-Zustand, AABB-Neuberechnung bei Blockmutation, Controller-Translation, Cooldown-Arithmetik. |
| **`CombatLogicTest`** | JUnit 5 | FastVoxelTraversal 3D-DDA Treffererkennung, Normalenflächen (`WEST`, `DOWN`), Reichweiten- & Tier-Konstanten. |
| **`PayloadSerializationTest`** | JUnit 5 | Symmetrische Serialisierung & Deserialisierung aller 10 CustomPacketPayload-Records via StreamCodecs. |
| **`SpaceshipEnergyManagerTest`** | JUnit 5 (Mockito) | Reaktor-Bündelung, sequenzieller Energieabzug, Rollback bei Energiemangel, Flugkosten-Berechnung. |
| **`ShipScannerGameTests`** | GameTest | Orthogonale BFS-Erkennung, Ausschluss diagonaler Blöcke, Multipart-Erfassung (Türen, Betten, Truhen). |
| **`ShipMovementGameTests`** | GameTest | Physische Welt-Translation, `AIR`-Hinterlassung und Zielblock-Präsenzprüfung. |
| **`ShipAttachmentGameTests`** | GameTest | Typsichere Persistenz von `ModAttachments.SHIP_ID` an BlockEntities. |
| **`SpaceshipGameTests`** | GameTest | Schiffserstellung und UUID-Verknüpfung via Kontrollblock. |

### CI/CD Pipeline (`.github/workflows/ci.yml`)

The repository runs an automated GitHub Actions CI/CD pipeline on every `push` and `pull_request` to `main`:
1. **JDK 21 (Temurin)** & Gradle Setup with Dependency Caching (`setup-gradle@v3`).
2. **Compile:** `./gradlew compileJava`
3. **Unit Tests:** `./gradlew test` (JUnit 5 & Mockito)
4. **GameTests:** `./gradlew runGameTestServer` (Headless Minecraft Server GameTests)
5. **Package & Artifact:** `./gradlew build` and automated upload of `peaceman_alpha-*.jar` via `upload-artifact@v4`.

---

## Package Directory Structure

```text
src/main/java/com/peaceman/alpha/
├── Alpha.java                       # Main mod initialization & GameTest registration
├── Config.java                      # Mod configuration
├── block/                           # Blocks & ISpaceshipNode interface
│   ├── entity/                      # AbstractSpaceshipNodeBE, Laser BEs, Reactor BE, Shield BE
│   ├── HeavyBeamBlock.java          # Heavy Beam Laser Block
│   ├── MiningLaserBlock.java        # Mining Laser Block
│   ├── PulseLaserBlock.java         # Pulse Laser Cannon Block
│   ├── SpaceshipControlBlock.java   # Central Ship Core Block
│   ├── SpaceshipHelmBlock.java      # Navigation Console Block
│   ├── SpaceshipReactorBlock.java   # FE Energy Reactor Block
│   └── SpaceshipShieldBlock.java    # Shield Generator Block
├── client/                          # Client-only lifecycle & rendering
│   ├── network/                     # ClientPayloadHandler
│   ├── render/                      # LaserBeamRenderer, ShieldRenderer, ShipHighlightRenderer
│   ├── screen/                      # UI Screens (Control, Helm, Reactor)
│   └── state/                       # ClientShipState, ClientShipManager, ClientLaserState
├── menu/                            # Container menus (SpaceshipReactorMenu)
├── network/                         # CustomPacketPayload definitions & ServerPayloadHandler
├── registry/                        # DeferredRegisters (Blocks, Items, BlockEntities, ModAttachments)
├── ship/                            # Domain models, combat & movement services
│   ├── combat/                      # FastVoxelTraversal (3D-DDA), LaserCombatService, LaserRaycastUtil, LaserWeaponTier
│   ├── domain/                      # ShipState (Domain Model), VoxelGridCache
│   └── service/                     # ServerShipManager, ShipMovementService, ShipScannerService, ShipCollisionService
└── tests/                           # NeoForge GameTests (Scanner, Movement, Attachments, Lifecycle)
```

---

## Building & Development

### Requirements
* **Java 21** (JDK)
* **Gradle 8.8+** (or included Gradle wrapper `./gradlew`)
* **NeoForge 1.21**

### Build Commands

```bash
# Compile and run test suite
./gradlew compileJava
./gradlew test --rerun

# Run GameTests on headless test server
./gradlew runGameTestServer

# Build production JAR
./gradlew build

# Run Client / Server for local gameplay testing
./gradlew runClient
./gradlew runServer
```
