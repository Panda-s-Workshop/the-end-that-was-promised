package com.panda.promisedend.entity;

import com.panda.promisedend.logic.DragonPhase3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Where the dragon's void-breath dive will land, for clients to draw: the ring on the ground through the telegraph and
 * the charge, and the breath cloud's dark edge after. It has no body and is never saved; the dive sets its stage, and it
 * removes itself once the breath is over, or when nothing has moved it on for longer than a dive can take.
 *
 * <p>Of the {@link Kind#BREACH} kind it is the Void Leviathan's breach instead: its ring on the pool's surface, as wide
 * as the breach strikes, through the telegraph and the breach, and then gone.
 */
public class DiveMark extends Entity {
    public enum Stage { TELEGRAPH, CHARGE, BREATH }

    public enum Kind { DIVE, BREACH }

    /** The longest it can live: the telegraph, the longest charge and the breath, and a second to spare. */
    private static final int MAX_TICKS = DragonPhase3.TELEGRAPH_TICKS + DragonPhase3.CHARGE_TIMEOUT + DragonPhase3.BREATH_TICKS + 20;

    /** A breach's: its telegraph and the breach, and a tick to spare. */
    private static final int BREACH_MAX_TICKS = VoidLeviathan.TELEGRAPH_TICKS + VoidLeviathan.BREACH_TICKS + 2;

    private static final EntityDataAccessor<Byte> DATA_STAGE = SynchedEntityData.defineId(DiveMark.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(DiveMark.class, EntityDataSerializers.BYTE);

    private int stageTicks;
    /** Clients count the ticks since the stage last changed themselves. */
    private Stage clientStage = Stage.TELEGRAPH;
    private int clientStageTicks;

    public DiveMark(EntityType<? extends DiveMark> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STAGE, (byte) Stage.TELEGRAPH.ordinal());
        builder.define(DATA_KIND, (byte) Kind.DIVE.ordinal());
    }

    public Kind kind() {
        return Kind.values()[Math.clamp(this.entityData.get(DATA_KIND), 0, Kind.values().length - 1)];
    }

    public void setKind(Kind kind) {
        this.entityData.set(DATA_KIND, (byte) kind.ordinal());
    }

    public Stage stage() {
        return Stage.values()[Math.clamp(this.entityData.get(DATA_STAGE), 0, Stage.values().length - 1)];
    }

    public void setStage(Stage stage) {
        this.entityData.set(DATA_STAGE, (byte) stage.ordinal());
        this.stageTicks = 0;
    }

    /** Ticks since the stage last changed, as this client saw it. */
    public int clientStageTicks() {
        return clientStageTicks;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            Stage stage = stage();
            if (stage != clientStage) {
                clientStage = stage;
                clientStageTicks = 0;
            } else {
                clientStageTicks++;
            }
            return;
        }
        stageTicks++;
        if (kind() == Kind.BREACH ? tickCount > BREACH_MAX_TICKS : tickCount > MAX_TICKS || stage() == Stage.BREATH && stageTicks > DragonPhase3.BREATH_TICKS) discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
