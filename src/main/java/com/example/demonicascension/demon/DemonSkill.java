package com.example.demonicascension.demon;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum DemonSkill {

    // gridX = tier (column), gridY = branch (row)

    INFERNAL_VIGOR("infernal_vigor", "Infernal Vigor",
            "Your flesh becomes something harder than flesh. "
                    + "+10 maximum health and 30% damage reduction.", 1, 0, 0),

    RENDING_CLAWS("rending_claws", "Rending Claws",
            "Your hands are instruments of ruin. +7 attack damage, "
                    + "your strikes ignite, and you drain life from the wounded.", 1, 0, 1),

    CLOVEN_SWIFTNESS("cloven_swiftness", "Cloven Swiftness",
            "The abyss quickens you. +40% movement speed, +30% attack speed, "
                    + "and you no longer fear the fall.", 1, 0, 2),

    VOID_SIGHT("void_sight", "Void Sight",
            "Darkness holds no secrets. See in the dark, sense the living "
                    + "through stone within 32 blocks, and no shadow may blind you.",
            2, 1, 0, INFERNAL_VIGOR),

    SOUL_BOLT("soul_bolt", "Soul Bolt",
            "Hurl a lance of screaming souls. Pierces, ignites, and returns "
                    + "half of what it takes to you.", 2, 1, 1, RENDING_CLAWS),

    ABYSSAL_DASH("abyssal_dash", "Abyssal Dash",
            "Tear forward through space. Anything in your path is thrown "
                    + "aside, and for that instant nothing can touch you.", 2, 1, 2, CLOVEN_SWIFTNESS),

    HELLFIRE_BARRAGE("hellfire_barrage", "Hellfire Barrage",
            "One bolt becomes five, and they hunt. Each detonates on impact.",
            3, 2, 1, SOUL_BOLT),

    VOIDSTEP("voidstep", "Voidstep",
            "Do not cross the distance. Refuse it. The abyss collapses "
                    + "where you left and where you arrive.", 3, 2, 2, ABYSSAL_DASH),

    // --- The ultimate. Stands apart from the tree; demands everything else first. ---
    ABYSSAL_RIFT("abyssal_rift", "Abyssal Rift",
            "You have taken enough of the abyss into yourself that it answers. "
                    + "Tear open the world and step through into a place that is yours alone.",
            5, 4, 1,
            INFERNAL_VIGOR, RENDING_CLAWS, CLOVEN_SWIFTNESS, VOID_SIGHT,
            SOUL_BOLT, ABYSSAL_DASH, HELLFIRE_BARRAGE, VOIDSTEP);

    private final String id;
    private final String displayName;
    private final String description;
    private final int cost;
    private final int gridX;
    private final int gridY;
    private final List<DemonSkill> prerequisites;

    DemonSkill(String id, String displayName, String description, int cost,
               int gridX, int gridY, DemonSkill... prerequisites) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.cost = cost;
        this.gridX = gridX;
        this.gridY = gridY;
        this.prerequisites = List.of(prerequisites);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getCost() {
        return cost;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public List<DemonSkill> getPrerequisites() {
        return prerequisites;
    }

    /** The ultimate is drawn separately in the GUI, without prerequisite lines. */
    public boolean isUltimate() {
        return this == ABYSSAL_RIFT;
    }

    public boolean prerequisitesMet(DemonData data) {
        return prerequisites.stream().allMatch(data::hasSkill);
    }

    public boolean canUnlock(DemonData data) {
        return !data.hasSkill(this)
                && data.getSkillPoints() >= cost
                && prerequisitesMet(data);
    }

    public static Optional<DemonSkill> byId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equals(id)).findFirst();
    }
}