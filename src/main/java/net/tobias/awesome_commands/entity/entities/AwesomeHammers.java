package net.tobias.awesome_commands.entity.entities;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.tobias.awesome_commands.AwesomeCommands;
import org.joml.Vector3f;

import java.util.UUID;

public class AwesomeHammers extends Mob {
    private static final ResourceKey<DamageType> AWESOME_HAMMERS_OBLITERATION_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(AwesomeCommands.MOD_ID, "awesome_hammers_obliteration")
    );
    public static final float AWE_EXPLODE_SLAM_EXPLODE_LENGTH_SECONDS = 3.75F;
    public static final int AWE_EXPLODE_SLAM_EXPLODE_LENGTH_TICKS = Mth.ceil(AWE_EXPLODE_SLAM_EXPLODE_LENGTH_SECONDS * 20.0F);
    public static final float AWE_EXPLODE_SLAM_EXPLODE_DAMAGE_AT_SECONDS = 1.7083F;
    public static final int AWE_EXPLODE_SLAM_EXPLODE_DAMAGE_AT_TICKS = Math.round(AWE_EXPLODE_SLAM_EXPLODE_DAMAGE_AT_SECONDS * 20.0F);
    private static final float AWE_EXPLODE_SLAM_EXPLODE_DAMAGE_AMOUNT = 1000.0F;
    private static final EntityDataAccessor<Boolean> DATA_SLAM_OBLITERATION_STARTED =
            SynchedEntityData.defineId(AwesomeHammers.class, EntityDataSerializers.BOOLEAN);
    public final AnimationState aweExplodeSlamExplodeAnimationState = new AnimationState();
    private boolean slamAnimationStarted;
    private int slamAnimationStartTick;
    private boolean slamDamageApplied;
    private UUID slamDamageTargetUuid;
    private UUID slamExecutorUuid;
    private boolean hasLockedSpawnPosition;
    private double lockedX;
    private double lockedY;
    private double lockedZ;

    public AwesomeHammers(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
        this.setInvisible(false);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SLAM_OBLITERATION_STARTED, false);
    }

    public void startSlamAnimation() {
        if (this.slamAnimationStarted) {
            return;
        }

        this.slamAnimationStarted = true;
        this.slamAnimationStartTick = this.tickCount;
        this.aweExplodeSlamExplodeAnimationState.start(this.tickCount);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_SLAM_OBLITERATION_STARTED, true);
        }
    }

    public void setDamageTarget(Entity target) {
        this.slamDamageTargetUuid = target.getUUID();
    }

    public void setDamageExecutor(Entity executor) {
        this.slamExecutorUuid = executor.getUUID();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10000f)
                .add(Attributes.ARMOR, 100000f)
                .add(Attributes.ARMOR_TOUGHNESS, 10000f)
                .add(Attributes.ATTACK_DAMAGE, 20f)
                .add(Attributes.ATTACK_SPEED, 0f)
                .add(Attributes.ATTACK_KNOCKBACK, 0f)
                .add(Attributes.FOLLOW_RANGE, 0f)
                .add(Attributes.JUMP_STRENGTH, 0f)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10000f)
                .add(Attributes.MOVEMENT_SPEED, 0f)
                .add(Attributes.FLYING_SPEED, 0f)
                .add(Attributes.LUCK, 0f)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0f);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.slamAnimationStarted && this.entityData.get(DATA_SLAM_OBLITERATION_STARTED)) {
            this.slamAnimationStarted = true;
            this.slamAnimationStartTick = this.tickCount;
            this.aweExplodeSlamExplodeAnimationState.start(this.tickCount);
        }

        this.lockPositionAndStopMovement();
        this.tryApplySlamDamage();

        if (!this.level().isClientSide
                && this.slamAnimationStarted
                && this.tickCount - this.slamAnimationStartTick >= AWE_EXPLODE_SLAM_EXPLODE_LENGTH_TICKS) {
            this.discard();
        }
    }

    private void lockPositionAndStopMovement() {
        if (!this.hasLockedSpawnPosition) {
            this.lockedX = this.getX();
            this.lockedY = this.getY();
            this.lockedZ = this.getZ();
            this.hasLockedSpawnPosition = true;
        }

        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = false;
        this.setPos(this.lockedX, this.lockedY, this.lockedZ);
    }

    private void tryApplySlamDamage() {
        if (this.level().isClientSide
                || !this.slamAnimationStarted
                || this.slamDamageApplied
                || this.slamDamageTargetUuid == null) {
            return;
        }

        int elapsedTicks = this.tickCount - this.slamAnimationStartTick;
        if (elapsedTicks < AWE_EXPLODE_SLAM_EXPLODE_DAMAGE_AT_TICKS) {
            return;
        }

        this.slamDamageApplied = true;
        ServerLevel serverLevel = (ServerLevel) this.level();
        Entity target = serverLevel.getEntity(this.slamDamageTargetUuid);
        if (target instanceof LivingEntity livingTarget) {
            boolean useAssimilationDeathMessage = livingTarget instanceof net.minecraft.world.entity.player.Player
                    || (livingTarget instanceof TamableAnimal tamable && tamable.isTame())
                    || livingTarget.hasCustomName();
            DamageSource damageSource = this.damageSources().magic();
            Entity executor = null;
            if (useAssimilationDeathMessage && this.slamExecutorUuid != null) {
                executor = serverLevel.getEntity(this.slamExecutorUuid);
                if (executor != null) {
                    Holder<DamageType> damageType = serverLevel.registryAccess()
                            .registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(AWESOME_HAMMERS_OBLITERATION_DAMAGE_TYPE);
                    damageSource = new DamageSource(damageType, this, executor);
                }
            }

            boolean damaged = livingTarget.hurt(damageSource, AWE_EXPLODE_SLAM_EXPLODE_DAMAGE_AMOUNT);
            if (damaged) {
                spawnRainbowExplosion(serverLevel, livingTarget.position().add(0.0D, livingTarget.getBbHeight() * 0.5D, 0.0D));
                boolean isNamedNonPetNonPlayer = livingTarget.hasCustomName()
                        && !(livingTarget instanceof TamableAnimal tamable && tamable.isTame())
                        && !(livingTarget instanceof Player);
                if (livingTarget.isDeadOrDying() && isNamedNonPetNonPlayer && executor != null) {
                    Component deathMessage = Component.translatable(
                            "death.attack.awesome_hammers_assimilation.named_type",
                            livingTarget.getDisplayName(),
                            livingTarget.getType().getDescription(),
                            executor.getDisplayName()
                    );
                    serverLevel.getServer().getPlayerList().broadcastSystemMessage(deathMessage, false);
                }
            }
        }
    }

    private static void spawnRainbowExplosion(ServerLevel level, Vec3 center) {
        Vector3f[] rainbow = new Vector3f[] {
                new Vector3f(1.0F, 0.0F, 0.0F),   // red
                new Vector3f(1.0F, 0.5F, 0.0F),   // orange
                new Vector3f(1.0F, 1.0F, 0.0F),   // yellow
                new Vector3f(0.0F, 1.0F, 0.0F),   // green
                new Vector3f(0.0F, 0.6F, 1.0F),   // blue
                new Vector3f(0.3F, 0.0F, 1.0F),   // indigo
                new Vector3f(0.7F, 0.0F, 1.0F)    // violet
        };

        for (Vector3f color : rainbow) {
            DustParticleOptions dust = new DustParticleOptions(color, 1.5F);
            level.sendParticles(
                    dust,
                    center.x, center.y, center.z,
                    140,
                    1.1D, 1.1D, 1.1D,
                    0.08D
            );
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public void push(double x, double y, double z) {}

    @Override
    public void knockback(double strength, double xRatio, double zRatio) {}
}