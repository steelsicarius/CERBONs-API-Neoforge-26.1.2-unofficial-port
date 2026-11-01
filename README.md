# CERBONs API (NeoForge 26.1.2)

Port of [CERBONs API](https://github.com/CERBON-MODS/CERBONs-API) (originally a multi-loader
Architectury project for Minecraft 1.21) to **NeoForge 26.1.2 / Minecraft 26.1.2 / Java 25**,
as a single-module ModDevGradle project (`net.neoforged.moddev` 2.0.148, same version as the
MDK used by the Bosses of Mass Destruction port).

Original (read-only reference): `../CERBONs-API-master` (Common + NeoForge sources merged here).

## Build

```bash
JAVA_HOME=/usr/java/jdk-25-oracle-aarch64 ./gradlew build
# -> build/libs/cerbons_api-neoforge-26.1.2-1.2.0.jar
```

The built jar is copied to `../Bosses-of-Mass-Destruction-Neoforge-26.1.2/libs/` as a
dependency of the BOMD port.

## Porting notes (1.21 → 26.1.2)

- `net.minecraft.resources.ResourceLocation` → `net.minecraft.resources.Identifier`
  (factory methods `fromNamespaceAndPath`, `tryParse`, `withDefaultNamespace` unchanged).
- `Registry.get(...)` → `Registry.getValue(...)`; `RegistryAccess.registryOrThrow` → `lookupOrThrow`.
- Items:
  - `ArmorItem` removed → plain `Item` with `Item.Properties.humanoidArmor(ArmorMaterial, ArmorType)`;
    `ArmorItem.Type` → `net.minecraft.world.item.equipment.ArmorType`.
  - `Tier`/`TieredItem`/`SwordItem.createAttributes` → `ToolMaterial` and
    `Item.Properties.sword/pickaxe/axe/shovel/hoe(material, damage, speed)`
    (same damage semantics: baseline + material bonus).
  - Block/Item ids must be set in the properties: `Properties.setId(ResourceKey)`.
- SavedData: `SavedData.Factory` → `SavedDataType<T>` (+ `Codec`), obtained via
  `Level.getDataStorage().computeIfAbsent(TYPE)` (`SavedDataStorage` replaced `DimensionDataStorage`).
- Particles:
  - `TextureSheetParticle` removed → `SingleQuadParticle` (same protected members: `sprite`,
    `setSprite`, `setSpriteFromAge`, `getU0...`).
  - Rendering no longer builds quads directly; particles are submitted through
    `SingleQuadParticle.extract(QuadParticleRenderState, Camera, float)`. `SimpleParticle` derives
    a (center, quaternion, scale) triple from the custom `IParticleGeometry` corners to keep
    arbitrary geometries working.
  - `ParticleRenderType` is now a record with only SINGLE_QUADS / ITEM_PICKUP / ELDER_GUARDIANS /
    NO_RENDER; use `SingleQuadParticle.Layer` + `getLayer()` instead.
  - `ParticleProvider.createParticle(...)` gained a trailing `RandomSource` parameter.
  - `Particle.setColor(float, float, float)` moved to `SingleQuadParticle`.
- Networking (NeoForge side): unchanged API (`RegisterPayloadHandlersEvent` +
  `PacketDistributor`), only the payload id type (`Identifier`).
- `ChunkPos` is a record: use `pos.x()`, `pos.z()` and `ChunkPos.containing(BlockPos)`.
- `Camera.getPosition()` → `position()`; `Level.random` → `getRandom()`;
  `Level.isClientSide` → `isClientSide()`; `ServerPlayer.serverLevel()` → `level()`.
- `Entity.absMoveTo` → `absSnapTo`; `Entity.interact` now takes the hit `Vec3`.
- `SoundEvent.getLocation()` → `location()` (record accessor).
- FML: `FMLLoader.getDist()`/`isProduction()` → static `FMLEnvironment.getDist()`/`isProduction()`.
- Mixin removed: `EntityRenderDispatcherMixin` (draws oriented hitbox outlines) because
  `EntityRenderDispatcher.renderHitbox` and `LevelRenderer.renderLineBox` no longer exist in
  26.1.2; the new debug system is Gizmos-based (`EntityHitboxDebugRenderer`). Pending
  re-implementation with `Gizmos.cuboid`/`GizmoStyle` if oriented-hitbox debug is wanted.
- `META-INF/services` declarations must live in `META-INF/services/` (ServiceLoader-backed
  platform helpers).

## Known limitations

- `LevelEventScheduler` (per-level event scheduler) is persisted as an empty NBT (same as
  the original: events are not saved across restarts).
- Clientbound payload handlers are registered on both `RegisterPayloadHandlersEvent` and the
  26.1.2-only `RegisterClientPayloadHandlersEvent` (mandatory for the client network registry
  validation).
