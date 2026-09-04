package com.example.demonicascension.event;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.config.ModConfigs;
import com.example.demonicascension.demon.DemonFormHandler;
import com.example.demonicascension.network.ModNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Runs the Abyssal Eclipse's server-wide side: the storm, cosmetic lightning, and
 * the sky/fog broadcast to every client. In-memory only — a 30-second effect has no
 * business surviving a server restart, unlike {@code AscensionState}/{@code
 * AbyssHallState}. Only one player can ever ascend in a world (see
 * {@code AscensionState}), so there's never more than one caster to track.
 */
@EventBusSubscriber(modid = DemonicAscension.MODID)
public class EclipseHandler {

    /** How long each refreshed dose of Darkness lasts — comfortably longer than the refresh interval below. */
    private static final int DARKNESS_DOSE_TICKS = 40;
    private static final int DARKNESS_REFRESH_INTERVAL_TICKS = 20;

    private static long activeUntilGameTime = 0L;
    private static boolean wasActiveLastTick = false;

    public static boolean isActive(long gameTime) {
        return gameTime < activeUntilGameTime;
    }

    public static long getActiveUntilGameTime() {
        return activeUntilGameTime;
    }

    /** Starts the storm, broadcasts it to every client, and refreshes the caster's buffed passives. */
    public static void activate(ServerPlayer caster, ServerLevel overworld, int durationTicks) {
        long now = overworld.getGameTime();
        activeUntilGameTime = now + durationTicks;
        wasActiveLastTick = true;

        overworld.setWeatherParameters(0, durationTicks, true, true);
        applyDarknessToOverworldPlayers(overworld);
        ModNetworking.broadcastEclipseState(activeUntilGameTime);

        // Passive skill bonuses are baked into attribute modifiers, applied only when
        // the form is (re)computed — force that now so the buff actually takes effect
        // immediately rather than waiting for some unrelated future trigger.
        DemonFormHandler.updateForm(caster);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        long now = overworld.getGameTime();
        boolean active = isActive(now);

        if (active) {
            if (now % ModConfigs.ECLIPSE_LIGHTNING_INTERVAL_TICKS.get() == 0) {
                strikeNearRandomPlayer(overworld);
            }
            // Refreshed periodically, rather than one long dose applied at activation,
            // so anyone who joins or enters the overworld mid-eclipse still gets it.
            if (now % DARKNESS_REFRESH_INTERVAL_TICKS == 0) {
                applyDarknessToOverworldPlayers(overworld);
                // The activation broadcast is a single reliable packet, but resending the
                // same absolute end time here is a free self-heal: idempotent for anyone
                // already correct, and it corrects anyone whose EclipseClientState fell
                // out of sync (a client-side mod conflict, a missed StartTracking resync
                // on join, etc.) within a second instead of them flickering or seeing a
                // stale sky for the rest of the window.
                ModNetworking.broadcastEclipseState(activeUntilGameTime);
            }
        } else if (wasActiveLastTick) {
            // Just expired: clear the storm and refresh every ascended player's form so
            // the buff drops off (matters for whoever cast it; a no-op for everyone
            // else). Darkness needs no explicit removal — the last refreshed dose just
            // runs out on its own.
            overworld.setWeatherParameters(6000, 0, false, false);
            for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
                DemonFormHandler.updateForm(player);
            }
        }

        wasActiveLastTick = active;
    }

    private static void applyDarknessToOverworldPlayers(ServerLevel overworld) {
        // Including the demon itself — Darkness dims the client's lightmap, which the
        // Void Sight flame's shader never samples (see VoidSightRenderer), so tracked
        // souls stay fully visible on their own without needing an exemption here. The
        // demon gets the same atmosphere as everyone else, and can still see its prey.
        for (ServerPlayer player : overworld.players()) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_DOSE_TICKS, 0,
                    false, false, false));
        }
    }

    /** Visual-only — no fire, no damage, no zombified piglins. Purely spectacle. */
    private static void strikeNearRandomPlayer(ServerLevel overworld) {
        var players = overworld.players();
        if (players.isEmpty()) {
            return;
        }

        ServerPlayer target = players.get(overworld.getRandom().nextInt(players.size()));
        var rng = overworld.getRandom();
        double x = target.getX() + (rng.nextDouble() - 0.5) * 40.0;
        double z = target.getZ() + (rng.nextDouble() - 0.5) * 40.0;
        double y = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(overworld);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(x, y, z);
        bolt.setVisualOnly(true);
        overworld.addFreshEntity(bolt);
    }
}
