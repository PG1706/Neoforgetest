package com.example.demonicascension.demon;

import com.example.demonicascension.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The ritual that crafts an Abyssal Soul: a polished blackstone altar with a three-tall
 * pillar at its centre, framed by polished blackstone corners, with soul soil on its
 * four sides. Right-clicking either of the pillar's top two blocks (the base is purely
 * structural) with the frame complete lights the soul soil with soul fire — which,
 * unlike regular fire, never extinguishes on its own, see {@code SoulFireBlock} — as a
 * permanent marker that the ritual here has been completed, and hands the player a
 * soul. The altar is never consumed, so anyone can return and repeat the ritual.
 */
public class RitualHandler {

    /** Height of the trigger block above the altar's base (ring) level. 1 = middle, 2 = top. */
    private static final int[] TRIGGER_HEIGHTS = {2, 1};

    /** North/south/east/west of the altar, at base level: soul soil, lit on activation. */
    private static final int[][] FLAME_OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** The four corners framing the altar, at base level: purely structural. */
    private static final int[][] FRAME_OFFSETS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    /**
     * Attempts to complete the ritual for a pillar whose top is at {@code clickedPos}
     * (the block the player right-clicked). Tries treating the click as either the
     * pillar's middle or top block. Returns true if a matching structure was found (and
     * the player was handed a soul), false otherwise.
     */
    public static boolean tryActivate(ServerLevel level, BlockPos clickedPos, ServerPlayer player) {
        for (int height : TRIGGER_HEIGHTS) {
            BlockPos basePos = clickedPos.below(height);
            if (matches(level, basePos)) {
                activate(level, basePos, player);
                return true;
            }
        }
        return false;
    }

    /** Blindness while the abyss looks through you, and lightning to announce it did. */
    private static final int BLINDNESS_DURATION_TICKS = 40; // 2 seconds
    private static final int LIGHTNING_STRIKES = 3;
    private static final double LIGHTNING_RADIUS = 10.0;

    private static void activate(ServerLevel level, BlockPos basePos, ServerPlayer player) {
        for (int[] offset : FLAME_OFFSETS) {
            BlockPos firePos = basePos.offset(offset[0], 1, offset[1]);
            if (level.getBlockState(firePos).isAir()) {
                level.setBlockAndUpdate(firePos, Blocks.SOUL_FIRE.defaultBlockState());
            }
        }

        ItemStack soul = new ItemStack(ModItems.ABYSSAL_SOUL.get());
        if (!player.getInventory().add(soul)) {
            player.drop(soul, false);
        }

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                basePos.getX() + 0.5, basePos.getY() + 1.5, basePos.getZ() + 0.5,
                80, 0.6, 1.4, 0.6, 0.15);
        level.playSound(null, basePos, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.8F, 0.6F);

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS));
        strikeLightningNearby(level, basePos);

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Something unholy has clawed its way out from the depths of the abyss...")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                false);
    }

    /** A few damage-free, fire-free bolts scattered around the altar for atmosphere. */
    private static void strikeLightningNearby(ServerLevel level, BlockPos basePos) {
        var random = level.getRandom();
        for (int i = 0; i < LIGHTNING_STRIKES; i++) {
            double x = basePos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0 * LIGHTNING_RADIUS;
            double z = basePos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0 * LIGHTNING_RADIUS;

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(x, basePos.getY(), z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }
    }

    private static boolean matches(ServerLevel level, BlockPos basePos) {
        if (!isBlock(level, basePos, Blocks.POLISHED_BLACKSTONE)
                || !isBlock(level, basePos.above(), Blocks.POLISHED_BLACKSTONE)
                || !isBlock(level, basePos.above(2), Blocks.POLISHED_BLACKSTONE)) {
            return false;
        }
        for (int[] offset : FLAME_OFFSETS) {
            if (!isBlock(level, basePos.offset(offset[0], 0, offset[1]), Blocks.SOUL_SOIL)) {
                return false;
            }
        }
        for (int[] offset : FRAME_OFFSETS) {
            if (!isBlock(level, basePos.offset(offset[0], 0, offset[1]), Blocks.POLISHED_BLACKSTONE)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlock(ServerLevel level, BlockPos pos, Block block) {
        return level.getBlockState(pos).is(block);
    }
}
