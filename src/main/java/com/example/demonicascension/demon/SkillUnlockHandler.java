package com.example.demonicascension.demon;

import com.example.demonicascension.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SkillUnlockHandler {

    /**
     * Attempts to unlock a skill for a player, server-side.
     * Returns empty on success, or a failure reason.
     */
    public static Optional<String> tryUnlock(ServerPlayer player, DemonSkill skill) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        if (!data.hasAscended()) {
            return Optional.of("You must ascend before claiming power.");
        }
        if (data.hasSkill(skill)) {
            return Optional.of("You already possess " + skill.getDisplayName() + ".");
        }
        if (!skill.prerequisitesMet(data)) {
            String needed = skill.getPrerequisites().stream()
                    .filter(p -> !data.hasSkill(p))
                    .map(DemonSkill::getDisplayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            return Optional.of("First you must master: " + needed);
        }
        if (!data.spendSkillPoints(skill.getCost())) {
            return Optional.of("Not enough skill points. Need " + skill.getCost()
                    + ", have " + data.getSkillPoints() + ".");
        }

        data.unlockSkill(skill.getId());
        player.setData(ModAttachments.DEMON_DATA, data);

        DemonFormHandler.updateForm(player);
        ModNetworking.syncToAll(player);

        return Optional.empty();
    }
}