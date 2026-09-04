package com.example.demonicascension.dimension;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players' throne room halls have already been built, on the abyss
 * {@link ServerLevel} itself. {@code AbyssManager.ensureHall} can't tell "already
 * built" from the world alone — the scanned structure's own entry tile is air both
 * before and after placement (it's where the structure block that scanned it used to
 * sit) — so this is the actual source of truth instead.
 */
public class AbyssHallState extends SavedData {

    private static final String DATA_NAME = "demonicascension_abyss_halls";

    private final Set<UUID> builtHalls = new HashSet<>();

    public static SavedData.Factory<AbyssHallState> factory() {
        return new SavedData.Factory<>(AbyssHallState::new, AbyssHallState::load, null);
    }

    public static AbyssHallState get(ServerLevel abyss) {
        return abyss.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    private static AbyssHallState load(CompoundTag tag, HolderLookup.Provider registries) {
        AbyssHallState state = new AbyssHallState();
        for (Tag id : tag.getList("builtHalls", Tag.TAG_INT_ARRAY)) {
            state.builtHalls.add(NbtUtils.loadUUID(id));
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new net.minecraft.nbt.ListTag();
        for (UUID id : builtHalls) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put("builtHalls", list);
        return tag;
    }

    public boolean isHallBuilt(UUID playerId) {
        return builtHalls.contains(playerId);
    }

    public void markHallBuilt(UUID playerId) {
        if (builtHalls.add(playerId)) {
            this.setDirty();
        }
    }
}
