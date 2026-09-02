# Demonic Ascension

A Minecraft NeoForge mod. The player performs a ritual to obtain the **Abyssal Soul**,
uses it to ascend into a demonic form, harvests souls from kills to earn skill points,
and spends those on a skill tree of passives and active abilities. The capstone skill
tears open a rift into a private abyss dimension.

Version 0.1.0 is complete and running on a live multiplayer server inside a large
modpack without errors. Work in progress is version 0.2.0 (see **Roadmap** below).

---

## Environment

- **Minecraft** 1.21.1
- **NeoForge** 21.1.249
- **Java** 21 (Temurin, via SDKMAN)
- **Gradle** 9.2.1 with the NeoGradle plugin
- **OS** Bazzite (immutable Fedora derivative) — avoid `rpm-ostree`; user-space installs only
- **Project root** `/var/home/pgreive/Neoforgetest`
- **Mod ID** `demonicascension`
- **Base package** `com.example.demonicascension`

### Build and run

```bash
./gradlew build       # compile + package
./gradlew runClient   # launch a test client
./gradlew runServer   # launch a dedicated server
```

Always run `./gradlew build` after edits. Compile errors are frequent when touching
constructors or NeoForge APIs (see **Known pitfalls**).

### Dependencies

Two hard dependencies, loaded as local jars from `libs/`:

```groovy
implementation fileTree(dir: "libs", include: ["*.jar"])
```

- **FantasticWings** v21.1.2 — provides the evil wings and flight
- **PuzzlesLib** v21.1.52 — FantasticWings' own dependency

Both are declared `required` in `neoforge.mods.toml`. NeoForge mods ship in official
mappings since 1.20.2, so distributed jars compile directly with no deobfuscation.

---

## Architecture

### Package layout

```
com.example.demonicascension
├── DemonicAscension.java      main mod class, creative tab, registration
├── client/                    ALL client-only code (renderers, screens, keybinds)
├── command/                   /demon command tree
├── compat/                    FantasticWings integration
├── config/                    ModConfigs (currently unused — see Roadmap)
├── demon/                     core logic: data, skills, form, abilities
├── dimension/                 abyss dimension key + platform generation
├── entity/                    SoulBoltEntity, RiftEntity, ModEntities
├── event/                     event subscribers (sync, soul harvest, commands)
├── item/                      AbyssalSoulItem, ModItems
└── network/                   payloads and handlers
```

### Core data flow

`DemonData` is a NeoForge **Data Attachment** on the player. It is the single source
of truth for transformation state, skill points, souls, unlocked skills, and the
abyss return point. It is serialised via a `Codec` using `optionalFieldOf` for every
field, so **adding new fields is save-compatible**; renaming or removing is not.

Cooldowns live in `DemonData` but are **not** serialised — they reset on relog, which
is acceptable.

The attachment lives on the server. The client learns about it through
`DemonDataPayload`, broadcast by `ModNetworking.syncToAll()` using
`sendToPlayersTrackingEntityAndSelf`, so **other players' demon state is known to
your client** — this is what lets horns render on other people.

Sync happens on: login, respawn, dimension change, `StartTracking` (one player coming
into view of another), artifact use, skill unlock, and soul gain.

### Key classes

| Class | Responsibility |
|---|---|
| `DemonData` | Persistent per-player state. Attachment + Codec. |
| `DemonSkill` | Enum of all 9 skills: id, name, description, cost, grid position, prerequisites. |
| `DemonFormHandler` | Applies/removes attribute modifiers and effects for the form and its passive skills. |
| `AbilityHandler` | All active ability logic, cooldown checks, and denial feedback. |
| `SkillUnlockHandler` | Shared server-side unlock validation, used by both the GUI and the command. |
| `AscensionState` | `SavedData` on the overworld. Records which single player has claimed the abyss. |
| `AbyssManager` | Per-player platform coordinates and platform generation. |
| `ModNetworking` | Payload registration and the sync helpers. |

---

## Conventions

- **Server-side authority.** Every game-state change is guarded by
  `if (!level.isClientSide())` or an `instanceof ServerPlayer` check. The client is
  never trusted — `ServerPayloadHandler` revalidates everything, including skill
  unlocks the GUI has already checked.
- **Client-only isolation.** Anything touching `Minecraft`, `GuiGraphics`, renderers,
  or keybinds lives in `client/` and is annotated `@EventBusSubscriber(..., value = Dist.CLIENT)`.
  Loading these on a dedicated server crashes it.
- **Event bus.** `@EventBusSubscriber` auto-detects the bus from the event type.
  Do **not** pass `bus = EventBusSubscriber.Bus.MOD` — it is deprecated and marked
  for removal. Keep `value = Dist.CLIENT` where present; that is a different parameter.
- **Attribute modifiers** use `addTransientModifier` with a `ResourceLocation` id, and
  always `removeModifier` first so re-application cannot stack. `DemonData` is the
  source of truth, so modifiers are reapplied on login rather than persisted.
- **Comments** explain *why*, not *what*. Assume the reader can read Java.
- **Textures** are generated procedurally with Pillow scripts rather than hand-drawn.
  Palette: near-black blackstone shell, soul-fire cyan (`#84ECFF` core, `#28A8D2` mid),
  ember red (`#742220`) as an accent. Keep new assets consistent with this.

---

## Current feature set (0.1.0)

**Transformation.** Abyssal Soul item toggles the demon form. First use ascends the
player permanently and claims the world's single ascension slot via `AscensionState` —
**only one player per world can ever ascend**. Anyone else using the soul takes 8
magic damage and burns for 6 seconds.

**Base form.** +8 max health, +20% speed, +2 attack damage, infinite fire resistance,
evil wings from FantasticWings (which grant flight), soul-fire aura particles, horns
rendered on the player model.

**Souls.** Killing while transformed harvests souls: 250 for Ender Dragon or Wither,
50 for a player, `maxHealth / 5` for monsters, 1 for passives. 25 souls = 1 skill point.

**Skill tree.** 9 skills, 3 tiers plus an ultimate. Passives apply through
`DemonFormHandler`; actives are bound to keys. Opened with **K**.

| Skill | Cost | Type |
|---|---|---|
| Infernal Vigor | 1 | +10 health, 30% damage reduction |
| Rending Claws | 1 | +7 damage, ignite, 25% lifesteal |
| Cloven Swiftness | 1 | +40% speed, +30% attack speed, no fall damage |
| Void Sight | 2 | Night vision + see hostiles/players through walls (32 blocks) |
| Soul Bolt | 2 | **Active (R)** — 18 dmg piercing projectile, ignites, 50% lifesteal |
| Abyssal Dash | 2 | **Active (V)** — dash, 12 dmg + knockback, brief invulnerability |
| Hellfire Barrage | 3 | Upgrades R — 5 homing explosive bolts, 20 dmg each |
| Voidstep | 3 | Upgrades V — blink 40 blocks, 25 dmg burst at destination |
| Abyssal Rift | 5 | **Active (G)** — requires all 8 others. Opens a portal to the abyss. |

**Keybinds.** R = bolt, V = dash, G = rift, K = skill tree. All rebindable under a
"Demonic Ascension" category.

**Abyss dimension.** `demonicascension:abyss`. Flat void generator, End skybox
(`effects: minecraft:the_end`), no natural spawns. Each player gets a deterministic
platform derived from their UUID hash, spaced 20,000 blocks apart. Currently a 17×17
blackstone octagon with four soul-lantern pillars.

**Rift entity.** Spawns 2 blocks ahead, lives 100 ticks (5s), armed after 20 ticks.
Walking in teleports to the abyss and stores the return point in `DemonData`.
Using it from inside the abyss returns the player to exactly where they left.
Animated via an 8-frame vertical texture strip; the renderer scrolls UVs by `tickCount`.

**Commands.** `/demon info`, `skills`, `unlock <skill>`, and op-only `points <n>`,
`reset`, `abyss`, `host`, `release`.

---

## Known pitfalls

These have all bitten before. Check them first when something breaks.

1. **`DemonData`'s constructor has changed twice**, and both times
   `ClientPayloadHandler` broke because it calls the constructor directly. If you add
   a field, update that file. Consider refactoring to a builder if it changes again.
2. **`ClientRenderEvents` and `ModKeybinds`** both register on the mod bus and poll on
   the game bus. Getting these mixed up produces events that silently never fire.
3. **Custom screens must override `renderBackground`** to avoid the vanilla menu blur.
   `super.render()` calls it, so removing the explicit call is not enough.
4. **Through-wall rendering** (`VoidSightRenderer`) needs `MAIN_TARGET` and
   `NO_LAYERING`. `ITEM_ENTITY_TARGET` re-composites with depth and re-occludes the lines.
5. **Dimension JSON is read at world creation.** Changes require a brand new world;
   existing saves cache the old generator.
6. **`AbyssManager.ensurePlatform`** skips generation if the centre block is non-air.
   Any change to platform structure will not apply to players who already have one.
7. **Textures render magenta/black** when the path is wrong — that is a missing file,
   not a crash.
8. **`Enemy` is in `net.minecraft.world.entity.monster`**, not `net.minecraft.world.entity`.

---

## Roadmap — version 0.2.0

Ordered so each stage builds and tests independently. Config comes first because it
touches nearly every file; doing it later means retrofitting everything twice.

1. **Aura rework.** `DemonParticleHandler` only reads `mc.player`, so the soul-fire
   aura is invisible on other players. Loop over all players in the level and check
   each one's synced `DemonData`. At the same time, suppress particles near eye level
   for the local player (or entirely in first person) — they currently obscure vision.
2. **Config system.** `config/ModConfigs.java` exists but is unused. Expose every
   tunable: soul values, souls-per-point, all skill stats, all cooldowns, ability
   damage and ranges, skill costs, platform spacing. Note `DemonSkill` holds costs as
   enum constants, which cannot read config at class-load time — costs need to move to
   a lookup.
3. **Ascended passive boost.** A minor permanent buff for ascended players even when
   untransformed. `DemonFormHandler.removeForm` currently strips everything; it should
   apply a small "ascended" set instead. Keep it modest so transforming still matters.
4. **Glowing animated eyes.** A render layer on the player model, like the horns, but
   a flat emissive overlay on the face rather than protruding geometry. Animated via a
   frame strip like the rift, but subtler — a slow pulse or flicker, not writhing.
5. **The ritual.** Multiblock pattern detection to craft the Abyssal Soul. This is
   currently the only way to obtain it (it is creative-tab only right now), so it
   unblocks new players entirely. Note the one-host-per-world rule: decide whether the
   ritual fails for a second player, or succeeds and produces a soul that rejects them.
6. **Throne room shell.** Rewrite `AbyssManager` with box/wall/floor helpers, then
   build a large hall in plain blackstone. Confirm the scale before adding detail.
7. **Pillars and palette.** Pillar rows with bases, shafts, capitals. Varied blocks so
   surfaces read as built rather than poured.
8. **Soul flame details.** Lanterns, soul fire, campfires. Also the actual lighting —
   an unlit interior is just black.
9. **Dais and throne.** Raised platform, steps, throne. Decide whether it is functional.
10. **Altar and sword.** A lategame weapon found on an altar in the throne room.
    Well above netherite (15–20 base damage), soul-fire themed effects, custom texture
    in the established palette.
11. **Side rooms.** Two or three chambers off the main hall, purpose TBD.
12. **Arrival and migration.** Where players land, and handling players who already
    have the old 0.1.0 platform (see pitfall 6).

Open design questions are deliberately deferred to the stage that needs them.

---

## Working style

- The project owner is **new to Java and modding**. Explain what non-obvious code does
  and why, without being condescending.
- Prefer **complete files over snippets** when a change touches several places in one
  file — partial edits have caused unbalanced-brace errors repeatedly.
- **Verify NeoForge API details** rather than assuming. Several 1.21.1 signatures
  differ from 1.20.x tutorials, and getting them wrong has cost real time.
- **Run the build** after changes. Do not hand back code that has not compiled.
- Flag design consequences honestly — for example, if a number makes the mod trivialise
  vanilla progression, say so, then build what the owner decides.
