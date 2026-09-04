package com.example.demonicascension.dimension;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.config.ModConfigs;
import com.example.demonicascension.entity.AltarSwordEntity;
import com.example.demonicascension.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;
import java.util.UUID;

public class AbyssManager {

    public static final int PLATFORM_Y = 64;

    // The throne room shell is hand-built and scanned into this structure template
    // rather than generated procedurally — see the 0.2.0 roadmap stage 6 notes. Its
    // local (0,0,0) is wherever the structure block sat when it was scanned.
    private static final ResourceLocation THRONE_ROOM =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "throneroom");

    // Where the player should stand, relative to that (0,0,0) origin — the position
    // the structure block itself occupied within the scanned build.
    private static final BlockPos SPAWN_OFFSET = new BlockPos(27, 3, 3);

    // The altar sword marker: two placeholder iron blocks in the scanned build showing
    // where the sword should float. Cleared to air and replaced with a persistent item
    // entity the first time the hall is placed.
    private static final BlockPos SWORD_MARKER_LOWER = new BlockPos(27, 4, 45);
    private static final BlockPos SWORD_MARKER_UPPER = new BlockPos(27, 5, 45);

    /**
     * Each player gets a deterministic slot derived from their UUID, so the
     * same player always returns to the same hall.
     */
    public static BlockPos platformCenter(UUID playerId) {
        long gridWidth = ModConfigs.PLATFORM_GRID_WIDTH.get();
        int spacing = ModConfigs.PLATFORM_SPACING.get();

        // Non-negative hash, spread across a grid. Long math avoids overflow for a large configured grid.
        long slot = Math.floorMod((long) playerId.hashCode(), gridWidth * gridWidth);
        long gridX = slot % gridWidth;
        long gridZ = slot / gridWidth;

        return new BlockPos((int) (gridX * spacing), PLATFORM_Y, (int) (gridZ * spacing));
    }

    /**
     * Places the throne room structure at {@code entryPoint} if it isn't already there.
     * Returns where the player should actually stand.
     */
    public static BlockPos ensureHall(ServerLevel level, BlockPos entryPoint, UUID hallOwner) {
        // The player stands exactly where the structure block itself sat when the hall
        // was scanned.
        BlockPos spawnPoint = entryPoint.offset(SPAWN_OFFSET);

        // Whether the hall has been built has to be tracked explicitly — the entry
        // tile is where the structure block that scanned this build used to sit, and
        // the scanned template itself places air there, so it reads as "unplaced"
        // forever regardless of what's actually been built.
        AbyssHallState state = AbyssHallState.get(level);
        if (state.isHallBuilt(hallOwner)) {
            return spawnPoint;
        }

        StructureTemplateManager templateManager = level.getStructureManager();
        Optional<StructureTemplate> template = templateManager.get(THRONE_ROOM);
        if (template.isEmpty()) {
            DemonicAscension.LOGGER.error("Throne room structure {} could not be loaded", THRONE_ROOM);
            return spawnPoint;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);

        template.get().placeInWorld(level, entryPoint, entryPoint, settings, level.getRandom(), 2);

        level.setBlockAndUpdate(entryPoint.offset(SWORD_MARKER_LOWER), Blocks.AIR.defaultBlockState());
        BlockPos swordPos = entryPoint.offset(SWORD_MARKER_UPPER);
        level.setBlockAndUpdate(swordPos, Blocks.AIR.defaultBlockState());
        spawnAltarSword(level, swordPos);

        // The altar sword is a one-time gift: mark the hall built so a later visit —
        // whether or not the sword was ever taken — never spawns another one.
        state.markHallBuilt(hallOwner);

        return spawnPoint;
    }

    /** A persistent sword hovering above the altar, replacing its marker blocks. */
    private static void spawnAltarSword(ServerLevel level, BlockPos pos) {
        AltarSwordEntity sword = new AltarSwordEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(ModItems.ABYSSAL_SWORD.get()));
        level.addFreshEntity(sword);
    }

    /** Builds {@code hallOwner}'s hall if needed and puts {@code player} on its nave centreline. */
    public static void sendToAbyss(ServerPlayer player, ServerLevel abyss, UUID hallOwner) {
        BlockPos entryPoint = platformCenter(hallOwner);
        BlockPos spawnPoint = ensureHall(abyss, entryPoint, hallOwner);

        player.teleportTo(abyss,
                spawnPoint.getX() + 0.5,
                spawnPoint.getY(),
                spawnPoint.getZ() + 0.5,
                0.0F, 0.0F); // face +Z, looking down the hall

        player.resetFallDistance();
    }

    /** Builds the player's own hall if needed and sends them there. */
    public static void sendToAbyss(ServerPlayer player, ServerLevel abyss) {
        sendToAbyss(player, abyss, player.getUUID());
    }
}
