package com.example.demonicascension.client;

import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.DemonSkill;
import com.example.demonicascension.demon.ModAttachments;
import com.example.demonicascension.network.UnlockSkillPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class SkillTreeScreen extends Screen {

    private static final int NODE_W = 96;
    private static final int NODE_H = 34;
    private static final int GAP_X = 40;
    private static final int GAP_Y = 16;

    /** Flat dim overlay. Vanilla's default background applies a blur. */
    private static final int BACKDROP = 0xC0101018;

    private static final int NODE_UNLOCKED = 0xFF2E5C34;
    private static final int NODE_AVAILABLE = 0xFF5A3A18;
    private static final int NODE_UNAFFORDABLE = 0xFF3A2A2A;
    private static final int NODE_LOCKED = 0xFF1E1E22;

    private static final int BORDER_UNLOCKED = 0xFF6BE07A;
    private static final int BORDER_AVAILABLE = 0xFFE0B44A;
    private static final int BORDER_DIM = 0xFF44444C;

    private static final int LINE_ACTIVE = 0xFF6BE07A;
    private static final int LINE_DIM = 0xFF3A3A42;

    private int originX;
    private int originY;

    public SkillTreeScreen() {
        super(Component.literal("Demonic Ascension"));
    }

    @Override
    protected void init() {
        int cols = 5; // 3 tiers + a gap column + the ultimate
        int rows = 3;
        int treeW = cols * NODE_W + (cols - 1) * GAP_X;
        int treeH = rows * NODE_H + (rows - 1) * GAP_Y;

        this.originX = (this.width - treeW) / 2;
        this.originY = (this.height - treeH) / 2 + 10;
    }

    /**
     * Vanilla's implementation calls renderBlurredBackground, and Screen.render
     * calls this on our behalf — so overriding here is what actually stops the
     * blur. A plain fill keeps the world sharp behind the tree.
     */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, BACKDROP);
    }

    private int nodeX(DemonSkill skill) {
        return originX + skill.getGridX() * (NODE_W + GAP_X);
    }

    private int nodeY(DemonSkill skill) {
        return originY + skill.getGridY() * (NODE_H + GAP_Y);
    }

    private DemonData clientData() {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        return player != null ? player.getData(ModAttachments.DEMON_DATA) : new DemonData();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        DemonData data = clientData();

        // --- Header ---
        g.drawCenteredString(this.font, "Demonic Ascension",
                this.width / 2, this.originY - 46, 0xFFB07ACC);

        String pts = "Skill Points: " + data.getSkillPoints();
        g.drawCenteredString(this.font, pts, this.width / 2, this.originY - 32, 0xFF6BD4E0);

        String souls = "Souls: " + data.getSouls() + " / " + DemonData.soulsPerPoint();
        g.drawCenteredString(this.font, souls, this.width / 2, this.originY - 20, 0xFF7A7A88);

        // --- Connecting lines, drawn first so nodes sit on top ---
        for (DemonSkill skill : DemonSkill.values()) {
            if (skill.isUltimate()) {
                continue; // eight converging lines would just be noise
            }
            for (DemonSkill prereq : skill.getPrerequisites()) {
                boolean active = data.hasSkill(prereq);
                int colour = active ? LINE_ACTIVE : LINE_DIM;

                int x1 = nodeX(prereq) + NODE_W;
                int y1 = nodeY(prereq) + NODE_H / 2;
                int x2 = nodeX(skill);
                int y2 = nodeY(skill) + NODE_H / 2;
                int midX = (x1 + x2) / 2;

                g.fill(x1, y1 - 1, midX, y1 + 1, colour);
                g.fill(midX - 1, Math.min(y1, y2), midX + 1, Math.max(y1, y2), colour);
                g.fill(midX, y2 - 1, x2, y2 + 1, colour);
            }
        }

        // --- Nodes ---
        DemonSkill hovered = null;

        for (DemonSkill skill : DemonSkill.values()) {
            int x = nodeX(skill);
            int y = nodeY(skill);

            boolean unlocked = data.hasSkill(skill);
            boolean prereqOk = skill.prerequisitesMet(data);
            boolean affordable = data.getSkillPoints() >= skill.getCost();

            int bg;
            int border;
            if (unlocked) {
                bg = NODE_UNLOCKED;
                border = BORDER_UNLOCKED;
            } else if (!prereqOk) {
                bg = NODE_LOCKED;
                border = BORDER_DIM;
            } else if (affordable) {
                bg = NODE_AVAILABLE;
                border = BORDER_AVAILABLE;
            } else {
                bg = NODE_UNAFFORDABLE;
                border = BORDER_DIM;
            }

            g.fill(x - 1, y - 1, x + NODE_W + 1, y + NODE_H + 1, border);
            g.fill(x, y, x + NODE_W, y + NODE_H, bg);

            int nameColour = unlocked ? 0xFFD8FFDD : (prereqOk ? 0xFFEDE0C8 : 0xFF6A6A72);
            g.drawCenteredString(this.font, skill.getDisplayName(),
                    x + NODE_W / 2, y + 6, nameColour);

            String sub = unlocked
                    ? "Unlocked"
                    : skill.getCost() + " pt" + (skill.getCost() == 1 ? "" : "s");
            int subColour = unlocked
                    ? 0xFF8FCF98
                    : (affordable && prereqOk ? 0xFFE0B44A : 0xFF6A6A72);
            g.drawCenteredString(this.font, sub, x + NODE_W / 2, y + 20, subColour);

            if (isOver(mouseX, mouseY, x, y)) {
                hovered = skill;
            }
        }

        g.drawCenteredString(this.font, "Click an available skill to unlock it",
                this.width / 2, this.originY + 3 * (NODE_H + GAP_Y) + 8, 0xFF6A6A72);

        super.render(g, mouseX, mouseY, partialTick);

        if (hovered != null) {
            g.renderTooltip(this.font, buildTooltip(hovered, data), mouseX, mouseY);
        }
    }

    private List<FormattedCharSequence> buildTooltip(DemonSkill skill, DemonData data) {
        List<FormattedCharSequence> lines = new ArrayList<>();

        lines.add(Component.literal(skill.getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText());

        lines.addAll(this.font.split(
                Component.literal(skill.getDescription()).withStyle(ChatFormatting.GRAY), 200));

        lines.add(Component.literal("").getVisualOrderText());

        if (data.hasSkill(skill)) {
            lines.add(Component.literal("Unlocked")
                    .withStyle(ChatFormatting.GREEN).getVisualOrderText());
        } else if (!skill.prerequisitesMet(data)) {
            if (skill.isUltimate()) {
                long remaining = skill.getPrerequisites().stream()
                        .filter(p -> !data.hasSkill(p))
                        .count();
                lines.add(Component.literal("Requires every other skill — "
                                + remaining + " remaining")
                        .withStyle(ChatFormatting.DARK_RED).getVisualOrderText());
            } else {
                String needed = skill.getPrerequisites().stream()
                        .filter(p -> !data.hasSkill(p))
                        .map(DemonSkill::getDisplayName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                lines.add(Component.literal("Requires: " + needed)
                        .withStyle(ChatFormatting.DARK_RED).getVisualOrderText());
            }
        } else if (data.getSkillPoints() < skill.getCost()) {
            lines.add(Component.literal("Cost: " + skill.getCost()
                            + " (you have " + data.getSkillPoints() + ")")
                    .withStyle(ChatFormatting.RED).getVisualOrderText());
        } else {
            lines.add(Component.literal("Cost: " + skill.getCost() + " — click to unlock")
                    .withStyle(ChatFormatting.YELLOW).getVisualOrderText());
        }

        return lines;
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + NODE_W && mouseY >= y && mouseY < y + NODE_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            DemonData data = clientData();

            for (DemonSkill skill : DemonSkill.values()) {
                if (!isOver(mouseX, mouseY, nodeX(skill), nodeY(skill))) {
                    continue;
                }
                // Client-side check is only for responsiveness; the server revalidates.
                if (skill.canUnlock(data)) {
                    PacketDistributor.sendToServer(new UnlockSkillPayload(skill.getId()));
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.playSound(
                                net.minecraft.sounds.SoundEvents.SOUL_ESCAPE.value(), 0.6F, 1.2F);
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}