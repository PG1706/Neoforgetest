package com.example.demonicascension.command;

import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.DemonFormHandler;
import com.example.demonicascension.demon.DemonSkill;
import com.example.demonicascension.demon.ModAttachments;
import com.example.demonicascension.dimension.ModDimensions;
import com.example.demonicascension.network.ModNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.example.demonicascension.dimension.AbyssManager;
import com.example.demonicascension.demon.AscensionState;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public class DemonCommand {

    /** Tab-completes skill IDs. */
    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(DemonSkill.values()).map(DemonSkill::getId), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("demon");

        // --- Available to everyone ---

        root.then(Commands.literal("info")
                .executes(DemonCommand::showInfo));

        root.then(Commands.literal("skills")
                .executes(DemonCommand::listSkills));

        root.then(Commands.literal("unlock")
                .then(Commands.argument("skill", StringArgumentType.word())
                        .suggests(SKILL_SUGGESTIONS)
                        .executes(DemonCommand::unlockSkill)));

        // --- Operator only ---

        root.then(Commands.literal("points")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 9999))
                        .executes(DemonCommand::grantPoints)));

        root.then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes(DemonCommand::resetProgress));

        root.then(Commands.literal("abyss")
                .requires(source -> source.hasPermission(2))
                .executes(DemonCommand::enterAbyss));

        root.then(Commands.literal("host")
                .requires(source -> source.hasPermission(2))
                .executes(DemonCommand::showHost));

        root.then(Commands.literal("release")
                .requires(source -> source.hasPermission(2))
                .executes(DemonCommand::releaseHost));

        dispatcher.register(root);
    }

    // --- /demon info ---

    private static int showInfo(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = playerOrNull(context);
        if (player == null) {
            return 0;
        }

        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        send(player, "--- Demonic Ascension ---", ChatFormatting.DARK_PURPLE);
        send(player, "Ascended: " + (data.hasAscended() ? "yes" : "no"), ChatFormatting.GRAY);
        send(player, "Transformed: " + (data.isTransformed() ? "yes" : "no"), ChatFormatting.GRAY);
        send(player, "Skill points: " + data.getSkillPoints(), ChatFormatting.AQUA);
        send(player, "Souls: " + data.getSouls() + " / " + DemonData.soulsPerPoint()
                + " toward next point", ChatFormatting.AQUA);
        send(player, "Skills unlocked: " + data.getUnlockedSkills().size()
                + " / " + DemonSkill.values().length, ChatFormatting.GRAY);

        return 1;
    }

    // --- /demon skills ---

    private static int listSkills(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = playerOrNull(context);
        if (player == null) {
            return 0;
        }

        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        send(player, "--- Skills ---", ChatFormatting.DARK_PURPLE);

        for (DemonSkill skill : DemonSkill.values()) {
            String status;
            ChatFormatting colour;

            if (data.hasSkill(skill)) {
                status = "UNLOCKED";
                colour = ChatFormatting.GREEN;
            } else if (!skill.prerequisitesMet(data)) {
                status = "LOCKED (needs prerequisites)";
                colour = ChatFormatting.DARK_GRAY;
            } else if (data.getSkillPoints() < skill.getCost()) {
                status = "NEEDS " + skill.getCost() + " POINTS";
                colour = ChatFormatting.RED;
            } else {
                status = "AVAILABLE (" + skill.getCost() + " points)";
                colour = ChatFormatting.YELLOW;
            }

            send(player, skill.getDisplayName() + " [" + skill.getId() + "] — " + status, colour);
        }

        return 1;
    }

    // --- /demon unlock <skill> ---

    private static int unlockSkill(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = playerOrNull(context);
        if (player == null) {
            return 0;
        }

        String id = StringArgumentType.getString(context, "skill");
        var maybeSkill = DemonSkill.byId(id);

        if (maybeSkill.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No such skill: " + id));
            return 0;
        }

        DemonSkill skill = maybeSkill.get();
        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        if (!data.hasAscended()) {
            context.getSource().sendFailure(
                    Component.literal("You must ascend before claiming power."));
            return 0;
        }

        if (data.hasSkill(skill)) {
            context.getSource().sendFailure(
                    Component.literal("You already possess " + skill.getDisplayName() + "."));
            return 0;
        }

        if (!skill.prerequisitesMet(data)) {
            String needed = skill.getPrerequisites().stream()
                    .filter(p -> !data.hasSkill(p))
                    .map(DemonSkill::getDisplayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            context.getSource().sendFailure(
                    Component.literal("First you must master: " + needed));
            return 0;
        }

        if (!data.spendSkillPoints(skill.getCost())) {
            context.getSource().sendFailure(Component.literal(
                    "Not enough skill points. Need " + skill.getCost()
                            + ", have " + data.getSkillPoints() + "."));
            return 0;
        }

        data.unlockSkill(skill.getId());
        player.setData(ModAttachments.DEMON_DATA, data);

        // Re-apply the form so new passives take effect immediately.
        DemonFormHandler.updateForm(player);
        ModNetworking.syncToAll(player);

        send(player, "You claim " + skill.getDisplayName() + ".", ChatFormatting.DARK_PURPLE);
        send(player, skill.getDescription(), ChatFormatting.GRAY);
        send(player, "Skill points remaining: " + data.getSkillPoints(), ChatFormatting.AQUA);

        return 1;
    }

    // --- /demon points <amount> ---

    private static int grantPoints(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = playerOrNull(context);
        if (player == null) {
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(context, "amount");

        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        data.addSkillPoints(amount);
        player.setData(ModAttachments.DEMON_DATA, data);
        ModNetworking.syncToAll(player);

        send(player, "Granted " + amount + " skill points. Total: "
                + data.getSkillPoints(), ChatFormatting.AQUA);

        return 1;
    }

    // --- /demon reset ---

    private static int resetProgress(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = playerOrNull(context);
        if (player == null) {
            return 0;
        }

        // Strip the form before wiping data, so no modifiers are left orphaned.
        DemonFormHandler.removeForm(player);

        player.setData(ModAttachments.DEMON_DATA, new DemonData());
        ModNetworking.syncToAll(player);

        // Free the world's ascension slot if this player was holding it,
        // otherwise the abyss stays locked to someone with no powers.
        if (player.getServer() != null) {
            AscensionState state = AscensionState.get(player.getServer());
            if (state.isAscendedPlayer(player.getUUID())) {
                state.release();
            }
        }

        send(player, "Demonic progress reset.", ChatFormatting.RED);

        return 1;
    }

    // --- /demon abyss ---

    private static int enterAbyss(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = playerOrNull(context);
        if (player == null) {
            return 0;
        }

        var server = player.getServer();
        if (server == null) {
            return 0;
        }

        ServerLevel abyss = server.getLevel(ModDimensions.ABYSS);
        if (abyss == null) {
            context.getSource().sendFailure(
                    Component.literal("The abyss dimension failed to load."));
            return 0;
        }

        AbyssManager.sendToAbyss(player, abyss);
        send(player, "You step into the abyss.", ChatFormatting.DARK_PURPLE);

        return 1;
    }

    private static int showHost(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = playerOrNull(context);
        if (player == null || player.getServer() == null) {
            return 0;
        }

        AscensionState state = AscensionState.get(player.getServer());

        if (state.isClaimed()) {
            send(player, "The abyss is bound to: " + state.getAscendedName(),
                    ChatFormatting.DARK_PURPLE);
        } else {
            send(player, "The abyss has no host. It waits.", ChatFormatting.GRAY);
        }

        return 1;
    }

    private static int releaseHost(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = playerOrNull(context);
        if (player == null || player.getServer() == null) {
            return 0;
        }

        AscensionState state = AscensionState.get(player.getServer());
        state.release();

        send(player, "The abyss releases its host. It may claim another.",
                ChatFormatting.RED);

        return 1;
    }
    // --- helpers ---

    /** Returns the executing player, or null after sending a failure message. */
    private static ServerPlayer playerOrNull(CommandContext<CommandSourceStack> context) {
        try {
            return context.getSource().getPlayerOrException();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Players only."));
            return null;
        }
    }

    private static void send(ServerPlayer player, String text, ChatFormatting colour) {
        player.sendSystemMessage(Component.literal(text).withStyle(colour));
    }
}