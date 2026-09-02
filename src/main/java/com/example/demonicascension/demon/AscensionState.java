package com.example.demonicascension.demon;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;
import java.util.UUID;

/**
 * World-level record of who claimed the abyss. Stored on the overworld so it
 * is shared across dimensions and survives restarts.
 */
public class AscensionState extends SavedData {

    private static final String DATA_NAME = "demonicascension_ascension";

    private UUID ascendedPlayer;
    private String ascendedName = "";

    public static SavedData.Factory<AscensionState> factory() {
        return new SavedData.Factory<>(AscensionState::new, AscensionState::load, null);
    }

    public static AscensionState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld unavailable");
        }
        return overworld.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    private static AscensionState load(CompoundTag tag, HolderLookup.Provider registries) {
        AscensionState state = new AscensionState();
        if (tag.hasUUID("ascendedPlayer")) {
            state.ascendedPlayer = tag.getUUID("ascendedPlayer");
            state.ascendedName = tag.getString("ascendedName");
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (ascendedPlayer != null) {
            tag.putUUID("ascendedPlayer", ascendedPlayer);
            tag.putString("ascendedName", ascendedName);
        }
        return tag;
    }

    public boolean isClaimed() {
        return ascendedPlayer != null;
    }

    public Optional<UUID> getAscendedPlayer() {
        return Optional.ofNullable(ascendedPlayer);
    }

    public String getAscendedName() {
        return ascendedName;
    }

    public boolean isAscendedPlayer(UUID id) {
        return ascendedPlayer != null && ascendedPlayer.equals(id);
    }

    public void claim(UUID id, String name) {
        this.ascendedPlayer = id;
        this.ascendedName = name;
        this.setDirty();
    }

    /** For admins, so a world isn't permanently locked to a departed player. */
    public void release() {
        this.ascendedPlayer = null;
        this.ascendedName = "";
        this.setDirty();
    }
}