package com.panda.promisedend.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Walks a construct up to its target and no nearer than {@code stopAt}, watching it the while. It never strikes: each
 * construct hurts only through its own mechanic, which it shows before it lands (a melee goal would hit unannounced).
 */
public final class ApproachGoal extends Goal {
    /** Ticks between one path to the target and the next. */
    private static final int REPATH = 10;

    private final PathfinderMob mob;
    private final double speed, stopAt;
    private int repath;

    public ApproachGoal(PathfinderMob mob, double speed, double stopAt) {
        this.mob = mob;
        this.speed = speed;
        this.stopAt = stopAt;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target);
    }

    @Override
    public void start() {
        this.repath = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (mob.distanceToSqr(target) <= stopAt * stopAt) {
            mob.getNavigation().stop();
            return;
        }
        if (--repath <= 0) {
            this.repath = REPATH + mob.getRandom().nextInt(5);
            mob.getNavigation().moveTo(target, speed);
        }
    }
}
