package com.example.demonicascension.demon;

import com.example.demonicascension.config.ModConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DemonData {

    /** Souls needed to earn one skill point, honouring any config override. */
    public static int soulsPerPoint() {
        return ModConfigs.SOULS_PER_SKILL_POINT.get();
    }

    public static final Codec<DemonData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("transformed", false).forGetter(DemonData::isTransformed),
            Codec.BOOL.optionalFieldOf("hasAscended", false).forGetter(DemonData::hasAscended),
            Codec.INT.optionalFieldOf("skillPoints", 0).forGetter(DemonData::getSkillPoints),
            Codec.INT.optionalFieldOf("souls", 0).forGetter(DemonData::getSouls),
            Codec.STRING.listOf().optionalFieldOf("unlockedSkills", List.of())
                    .forGetter(d -> new ArrayList<>(d.getUnlockedSkills())),
            Codec.STRING.optionalFieldOf("returnDimension")
                    .forGetter(DemonData::getReturnDimension),
            Codec.DOUBLE.listOf().optionalFieldOf("returnPos", List.of())
                    .forGetter(DemonData::getReturnPosList)
    ).apply(instance, DemonData::new));

    private boolean transformed;
    private boolean hasAscended;
    private int skillPoints;
    private int souls;
    private final Set<String> unlockedSkills;

    /** Where the player was standing when they opened a rift into the abyss. */
    private String returnDimension;
    private double returnX;
    private double returnY;
    private double returnZ;
    private boolean hasReturn;

    // Cooldowns are runtime-only; resetting on relog is acceptable.
    private long bolterReadyAt = 0L;
    private long dashReadyAt = 0L;
    private long riftReadyAt = 0L;
    private long swordIgniteReadyAt = 0L;
    private long swordIgniteActiveUntil = 0L;

    public DemonData() {
        this(false, false, 0, 0, List.of(), Optional.empty(), List.of());
    }

    public DemonData(boolean transformed, boolean hasAscended, int skillPoints,
                     int souls, List<String> unlockedSkills,
                     Optional<String> returnDimension, List<Double> returnPos) {
        this.transformed = transformed;
        this.hasAscended = hasAscended;
        this.skillPoints = skillPoints;
        this.souls = souls;
        this.unlockedSkills = new HashSet<>(unlockedSkills);

        if (returnDimension.isPresent() && returnPos.size() == 3) {
            this.returnDimension = returnDimension.get();
            this.returnX = returnPos.get(0);
            this.returnY = returnPos.get(1);
            this.returnZ = returnPos.get(2);
            this.hasReturn = true;
        }
    }

    // --- Transformation state ---

    public boolean isTransformed() {
        return transformed;
    }

    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
    }

    public boolean hasAscended() {
        return hasAscended;
    }

    public void setAscended(boolean hasAscended) {
        this.hasAscended = hasAscended;
    }

    // --- Skill points ---

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public void addSkillPoints(int amount) {
        this.skillPoints += amount;
    }

    public boolean spendSkillPoints(int amount) {
        if (skillPoints < amount) {
            return false;
        }
        skillPoints -= amount;
        return true;
    }

    // --- Souls ---

    public int getSouls() {
        return souls;
    }

    public void setSouls(int souls) {
        this.souls = souls;
    }

    /**
     * Adds souls and converts full batches into skill points.
     * Returns how many points were earned so callers can notify the player.
     */
    public int addSouls(int amount) {
        this.souls += amount;
        int perPoint = soulsPerPoint();
        int earned = this.souls / perPoint;
        if (earned > 0) {
            this.souls %= perPoint;
            this.skillPoints += earned;
        }
        return earned;
    }

    // --- Unlocked skills ---

    public Set<String> getUnlockedSkills() {
        return unlockedSkills;
    }

    public boolean hasSkill(String skillId) {
        return unlockedSkills.contains(skillId);
    }

    public boolean hasSkill(DemonSkill skill) {
        return unlockedSkills.contains(skill.getId());
    }

    public void unlockSkill(String skillId) {
        unlockedSkills.add(skillId);
    }

    // --- Return point ---

    public boolean hasReturnPoint() {
        return hasReturn;
    }

    public Optional<String> getReturnDimension() {
        return hasReturn ? Optional.of(returnDimension) : Optional.empty();
    }

    /** Serialized as a 3-element list; empty when no return point is set. */
    public List<Double> getReturnPosList() {
        return hasReturn ? List.of(returnX, returnY, returnZ) : List.of();
    }

    public double getReturnX() {
        return returnX;
    }

    public double getReturnY() {
        return returnY;
    }

    public double getReturnZ() {
        return returnZ;
    }

    public void setReturnPoint(String dimension, double x, double y, double z) {
        this.returnDimension = dimension;
        this.returnX = x;
        this.returnY = y;
        this.returnZ = z;
        this.hasReturn = true;
    }

    public void clearReturnPoint() {
        this.hasReturn = false;
        this.returnDimension = null;
    }

    // --- Cooldowns (server-side only, not serialized) ---

    public boolean isBolterReady(long gameTime) {
        return gameTime >= bolterReadyAt;
    }

    public void setBolterCooldown(long gameTime, int ticks) {
        this.bolterReadyAt = gameTime + ticks;
    }

    /** Ticks left before the bolt slot is usable again. */
    public long getBolterRemaining(long gameTime) {
        return Math.max(0L, bolterReadyAt - gameTime);
    }

    public boolean isDashReady(long gameTime) {
        return gameTime >= dashReadyAt;
    }

    public void setDashCooldown(long gameTime, int ticks) {
        this.dashReadyAt = gameTime + ticks;
    }

    public long getDashRemaining(long gameTime) {
        return Math.max(0L, dashReadyAt - gameTime);
    }

    public boolean isRiftReady(long gameTime) {
        return gameTime >= riftReadyAt;
    }

    public void setRiftCooldown(long gameTime, int ticks) {
        this.riftReadyAt = gameTime + ticks;
    }

    public long getRiftRemaining(long gameTime) {
        return Math.max(0L, riftReadyAt - gameTime);
    }

    public boolean isSwordIgniteReady(long gameTime) {
        return gameTime >= swordIgniteReadyAt;
    }

    public long getSwordIgniteRemaining(long gameTime) {
        return Math.max(0L, swordIgniteReadyAt - gameTime);
    }

    /** Arms the ignite ability: active for {@code durationTicks}, then locked out for {@code cooldownTicks}. */
    public void activateSwordIgnite(long gameTime, int durationTicks, int cooldownTicks) {
        this.swordIgniteActiveUntil = gameTime + durationTicks;
        this.swordIgniteReadyAt = gameTime + cooldownTicks;
    }

    public boolean isSwordIgniteActive(long gameTime) {
        return gameTime < swordIgniteActiveUntil;
    }
}