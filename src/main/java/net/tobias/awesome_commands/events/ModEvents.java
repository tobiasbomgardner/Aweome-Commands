package net.tobias.awesome_commands.events;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tobias.awesome_commands.AwesomeCommands;
import net.tobias.awesome_commands.commands.AwesomeExplodeCmd;
import net.tobias.awesome_commands.effects.ModEffects;
import net.tobias.awesome_commands.entity.ModEntities;
import net.tobias.awesome_commands.entity.entities.AwesomeHammers;
import net.tobias.awesome_commands.visual.PersistentShapeController;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = AwesomeCommands.MOD_ID)
public class ModEvents {
    private static final String COMMAND_STUN_LOCK_YAW_KEY = "awesome_commands_command_stun_lock_yaw";
    private static final String COMMAND_STUN_LOCK_PITCH_KEY = "awesome_commands_command_stun_lock_pitch";
    private static final String SPIN_TARGET_UUID_KEY = "awesome_commands_spin_target_uuid";
    private static final String SPIN_RADIUS_KEY = "awesome_commands_spin_radius";
    private static final String SPIN_CURRENT_ANGLE_KEY = "awesome_commands_spin_current_angle";
    private static final String SPIN_Y_OFFSET_KEY = "awesome_commands_spin_y_offset";
    private static final String SPIN_RADIANS_PER_TICK_KEY = "awesome_commands_spin_radians_per_tick";
    private static final String SPIN_RESTORE_TICK_KEY = "awesome_commands_spin_restore_tick";
    private static final String SPIN_RESTORE_X_KEY = "awesome_commands_spin_restore_x";
    private static final String SPIN_RESTORE_Y_KEY = "awesome_commands_spin_restore_y";
    private static final String SPIN_RESTORE_Z_KEY = "awesome_commands_spin_restore_z";
    private static final String SPIN_RESTORE_YAW_KEY = "awesome_commands_spin_restore_yaw";
    private static final String SPIN_RESTORE_PITCH_KEY = "awesome_commands_spin_restore_pitch";
    private static final String SPIN_RESTORE_FACING_X_KEY = "awesome_commands_spin_restore_facing_x";
    private static final String SPIN_RESTORE_FACING_Y_KEY = "awesome_commands_spin_restore_facing_y";
    private static final String SPIN_RESTORE_FACING_Z_KEY = "awesome_commands_spin_restore_facing_z";
    private static final String SPIN_RESTORE_HAMMER_UUID_KEY = "awesome_commands_spin_restore_hammer_uuid";
    private static final String SPIN_RESTORE_GAMEMODE_KEY = "awesome_commands_spin_restore_gamemode";
    private static final String SPIN_RESTORE_MESSAGE_TARGET_NAME_KEY = "awesome_commands_spin_restore_message_target_name";
    private static final String SPIN_RESTORE_MESSAGE_TARGET_COUNT_KEY = "awesome_commands_spin_restore_message_target_count";
    private static final String ASCEND_TARGET_Y_KEY = "awesome_commands_ascend_target_y";
    private static final String ASCEND_SPEED_KEY = "awesome_commands_ascend_speed";
    private static final String ASCEND_PENDING_HAMMER_KEY = "awesome_commands_ascend_pending_hammer";
    private static final String ASCEND_EXECUTOR_UUID_KEY = "awesome_commands_ascend_executor_uuid";
    private static final String TARGET_RESTORE_TICK_KEY = "awesome_commands_target_restore_tick";
    private static final String TARGET_RESTORE_X_KEY = "awesome_commands_target_restore_x";
    private static final String TARGET_RESTORE_Y_KEY = "awesome_commands_target_restore_y";
    private static final String TARGET_RESTORE_Z_KEY = "awesome_commands_target_restore_z";
    private static final String TARGET_RESTORE_HAMMER_UUID_KEY = "awesome_commands_target_restore_hammer_uuid";

    @SubscribeEvent
    public static void preventCommandStunMovement(LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        updateAscension(livingEntity);
        updateSpinAroundTarget(livingEntity);
        updatePendingSpinRestore(livingEntity);
        updatePendingTargetRestore(livingEntity);
        normalizeCommandStunVisuals(livingEntity);
        if (isCommandStunned(livingEntity)) {
            double y = livingEntity.getY();
            livingEntity.setDeltaMovement(0.0d, 0.0d, 0.0d);
            livingEntity.xxa = 0.0F;
            livingEntity.yya = 0.0F;
            livingEntity.zza = 0.0F;
            livingEntity.setPos(livingEntity.xo, y, livingEntity.zo);
            livingEntity.setSprinting(false);
            livingEntity.setSpeed(0.0f);
            if (livingEntity instanceof Mob mob) {
                mob.setJumping(false);
                mob.getNavigation().stop();
            }
        }
    }

    @SubscribeEvent
    public static void preventCommandStunPlayerMovement(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (isCommandStunned(event.player)) {
            lockCommandStunnedLookDirection(event.player);
            double y = event.player.getY();
            event.player.setDeltaMovement(0.0d, 0.0d, 0.0d);
            event.player.xxa = 0.0F;
            event.player.yya = 0.0F;
            event.player.zza = 0.0F;
            event.player.setPos(event.player.xo, y, event.player.zo);
            event.player.setSprinting(false);
            event.player.setSpeed(0.0f);
            applyLockedPlayerRotation(event.player);
        } else {
            clearCommandStunLookLock(event.player.getPersistentData());
        }
    }

    @SubscribeEvent
    public static void processPersistentShapeRemoval(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PersistentShapeController.processScheduledRemovals(event.getServer());
    }

    @SubscribeEvent
    public static void syncPersistentShapesOnPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PersistentShapeController.restorePersistedRemovals(player.server);
            PersistentShapeController.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void preventCommandStunAttack(AttackEntityEvent event) {
        if (isCommandStunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventCommandStunLivingAttack(LivingAttackEvent event) {
        if (isAscending(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof LivingEntity livingSource && isCommandStunned(livingSource)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventCommandStunUseItem(PlayerInteractEvent.RightClickItem event) {
        if (isCommandStunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventCommandStunUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isCommandStunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventCommandStunUseStart(LivingEntityUseItemEvent.Start event) {
        if (isCommandStunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventCommandStunBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isCommandStunned(event.getEntity())) {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void preventCommandStunBreakBlock(BlockEvent.BreakEvent event) {
        if (isCommandStunned(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private static boolean isCommandStunned(LivingEntity livingEntity) {
        return livingEntity.hasEffect(ModEffects.COMMAND_STUN.get());
    }

    private static boolean isAscending(LivingEntity livingEntity) {
        return livingEntity.getPersistentData().contains(ASCEND_TARGET_Y_KEY);
    }

    private static void normalizeCommandStunVisuals(LivingEntity livingEntity) {
        MobEffectInstance current = livingEntity.getEffect(ModEffects.COMMAND_STUN.get());
        if (current == null || (!current.isVisible() && !current.showIcon())) {
            return;
        }

        MobEffectInstance hidden = new MobEffectInstance(
                current.getEffect(),
                current.getDuration(),
                current.getAmplifier(),
                current.isAmbient(),
                false,
                false
        );
        livingEntity.addEffect(hidden);
    }

    private static void updateSpinAroundTarget(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide()) {
            return;
        }

        CompoundTag persistentData = livingEntity.getPersistentData();
        if (!persistentData.hasUUID(SPIN_TARGET_UUID_KEY)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) livingEntity.level();
        Entity target = serverLevel.getEntity(persistentData.getUUID(SPIN_TARGET_UUID_KEY));
        if (target == null) {
            clearSpinData(persistentData);
            return;
        }

        double radius = persistentData.getDouble(SPIN_RADIUS_KEY);
        double yOffset = persistentData.getDouble(SPIN_Y_OFFSET_KEY);
        double radiansPerTick = persistentData.getDouble(SPIN_RADIANS_PER_TICK_KEY);

        double angle = persistentData.getDouble(SPIN_CURRENT_ANGLE_KEY) + radiansPerTick;
        persistentData.putDouble(SPIN_CURRENT_ANGLE_KEY, angle);
        double x = target.getX() + (Math.cos(angle) * radius);
        double z = target.getZ() + (Math.sin(angle) * radius);
        double y = target.getY() + yOffset;

        livingEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (livingEntity instanceof ServerPlayer player) {
            player.teleportTo(serverLevel, x, y, z, player.getYRot(), player.getXRot());
        } else {
            livingEntity.teleportTo(x, y, z);
        }

        livingEntity.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
    }

    private static void clearSpinData(CompoundTag persistentData) {
        persistentData.remove(SPIN_TARGET_UUID_KEY);
        persistentData.remove(SPIN_RADIUS_KEY);
        persistentData.remove(SPIN_CURRENT_ANGLE_KEY);
        persistentData.remove(SPIN_Y_OFFSET_KEY);
        persistentData.remove(SPIN_RADIANS_PER_TICK_KEY);
    }

    private static void updateAscension(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide()) {
            return;
        }

        CompoundTag persistentData = livingEntity.getPersistentData();
        if (!persistentData.contains(ASCEND_TARGET_Y_KEY)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) livingEntity.level();
        double targetY = persistentData.getDouble(ASCEND_TARGET_Y_KEY);
        double speedPerTick = persistentData.contains(ASCEND_SPEED_KEY) ? persistentData.getDouble(ASCEND_SPEED_KEY) : 3.5D;
        double currentY = livingEntity.getY();
        double nextY = Math.min(targetY, currentY + speedPerTick);

        spawnRainbowAscensionLine(serverLevel, livingEntity.getX(), livingEntity.getZ(), nextY, targetY);
        teleportEntityPreserveRotation(livingEntity, serverLevel, livingEntity.getX(), nextY, livingEntity.getZ());
        if (persistentData.hasUUID(ASCEND_EXECUTOR_UUID_KEY)) {
            Entity executor = serverLevel.getEntity(persistentData.getUUID(ASCEND_EXECUTOR_UUID_KEY));
            if (executor != null) {
                if (executor instanceof ServerPlayer player) {
                    player.teleportTo(serverLevel, player.getX(), nextY, player.getZ(), player.getYRot(), player.getXRot());
                } else {
                    executor.teleportTo(executor.getX(), nextY, executor.getZ());
                }
                executor.lookAt(EntityAnchorArgument.Anchor.EYES, livingEntity.getEyePosition());
            }
        }

        if (nextY + 0.001D < targetY) {
            return;
        }

        persistentData.remove(ASCEND_TARGET_Y_KEY);
        persistentData.remove(ASCEND_SPEED_KEY);

        if (persistentData.getBoolean(ASCEND_PENDING_HAMMER_KEY)) {
            if (handleAscensionCompletedExplosion(livingEntity, serverLevel, persistentData)) {
                persistentData.remove(ASCEND_PENDING_HAMMER_KEY);
            }
        }
        persistentData.remove(ASCEND_EXECUTOR_UUID_KEY);
    }

    private static boolean handleAscensionCompletedExplosion(LivingEntity target, ServerLevel level, CompoundTag targetData) {
        Entity executor = null;
        if (targetData.hasUUID(ASCEND_EXECUTOR_UUID_KEY)) {
            executor = level.getEntity(targetData.getUUID(ASCEND_EXECUTOR_UUID_KEY));
            targetData.remove(ASCEND_EXECUTOR_UUID_KEY);
        }

        if (executor != null) {
            AwesomeExplodeCmd.startForcedSpin(executor, target);
        }

        AwesomeHammers awesomeHammers = ModEntities.AWESOME_HAMMERS.get().create(level);
        if (awesomeHammers == null) {
            return true;
        }

        awesomeHammers.moveTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
        level.addFreshEntity(awesomeHammers);
        awesomeHammers.setDamageTarget(target);
        if (executor != null) {
            awesomeHammers.setDamageExecutor(executor);
        }
        awesomeHammers.startSlamAnimation();

        if (executor instanceof LivingEntity livingExecutor) {
            livingExecutor.getPersistentData().putUUID(SPIN_RESTORE_HAMMER_UUID_KEY, awesomeHammers.getUUID());
        }
        targetData.putUUID(TARGET_RESTORE_HAMMER_UUID_KEY, awesomeHammers.getUUID());
        return true;
    }

    private static void spawnRainbowAscensionLine(ServerLevel level, double x, double z, double startY, double endY) {
        Vector3f[] rainbow = new Vector3f[] {
                new Vector3f(1.0F, 0.0F, 0.0F),
                new Vector3f(1.0F, 0.5F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(0.0F, 1.0F, 0.0F),
                new Vector3f(0.0F, 0.6F, 1.0F),
                new Vector3f(0.3F, 0.0F, 1.0F),
                new Vector3f(0.7F, 0.0F, 1.0F)
        };

        for (Vector3f color : rainbow) {
            DustParticleOptions dust = new DustParticleOptions(color, 1.5F);
            for (double y = startY; y <= endY; y += 2.0D) {
                level.sendParticles(dust, x, y, z, 4, 0.05D, 0.05D, 0.05D, 0.0D);
            }
        }
    }

    private static void teleportEntityPreserveRotation(LivingEntity entity, ServerLevel level, double x, double y, double z) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
            return;
        }

        entity.teleportTo(x, y, z);
    }

    private static void updatePendingTargetRestore(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide()) {
            return;
        }

        CompoundTag persistentData = livingEntity.getPersistentData();
        ServerLevel serverLevel = (ServerLevel) livingEntity.level();

        if (!persistentData.contains(TARGET_RESTORE_TICK_KEY) && persistentData.hasUUID(TARGET_RESTORE_HAMMER_UUID_KEY)) {
            Entity hammer = serverLevel.getEntity(persistentData.getUUID(TARGET_RESTORE_HAMMER_UUID_KEY));
            if (hammer == null) {
                persistentData.putLong(TARGET_RESTORE_TICK_KEY, serverLevel.getGameTime() + 20L);
                persistentData.remove(TARGET_RESTORE_HAMMER_UUID_KEY);
            }
        }

        if (!persistentData.contains(TARGET_RESTORE_TICK_KEY)) {
            return;
        }

        long restoreTick = persistentData.getLong(TARGET_RESTORE_TICK_KEY);
        if (serverLevel.getGameTime() < restoreTick) {
            return;
        }

        if (!livingEntity.isDeadOrDying()) {
            double x = persistentData.getDouble(TARGET_RESTORE_X_KEY);
            double y = persistentData.getDouble(TARGET_RESTORE_Y_KEY);
            double z = persistentData.getDouble(TARGET_RESTORE_Z_KEY);
            teleportEntityPreserveRotation(livingEntity, serverLevel, x, y, z);
            livingEntity.removeEffect(ModEffects.COMMAND_STUN.get());
        }

        persistentData.remove(ASCEND_TARGET_Y_KEY);
        persistentData.remove(ASCEND_SPEED_KEY);
        persistentData.remove(ASCEND_PENDING_HAMMER_KEY);
        persistentData.remove(ASCEND_EXECUTOR_UUID_KEY);
        persistentData.remove(TARGET_RESTORE_TICK_KEY);
        persistentData.remove(TARGET_RESTORE_X_KEY);
        persistentData.remove(TARGET_RESTORE_Y_KEY);
        persistentData.remove(TARGET_RESTORE_Z_KEY);
        persistentData.remove(TARGET_RESTORE_HAMMER_UUID_KEY);
    }

    private static void updatePendingSpinRestore(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide()) {
            return;
        }

        CompoundTag persistentData = livingEntity.getPersistentData();
        ServerLevel serverLevel = (ServerLevel) livingEntity.level();

        if (!persistentData.contains(SPIN_RESTORE_TICK_KEY) && persistentData.hasUUID(SPIN_RESTORE_HAMMER_UUID_KEY)) {
            Entity hammer = serverLevel.getEntity(persistentData.getUUID(SPIN_RESTORE_HAMMER_UUID_KEY));
            if (hammer == null) {
                persistentData.putLong(SPIN_RESTORE_TICK_KEY, serverLevel.getGameTime() + 20L);
                persistentData.remove(SPIN_RESTORE_HAMMER_UUID_KEY);
            }
        }

        if (!persistentData.contains(SPIN_RESTORE_TICK_KEY)) {
            return;
        }
        long restoreTick = persistentData.getLong(SPIN_RESTORE_TICK_KEY);
        if (serverLevel.getGameTime() < restoreTick) {
            return;
        }

        clearSpinData(persistentData);

        double x = persistentData.getDouble(SPIN_RESTORE_X_KEY);
        double y = persistentData.getDouble(SPIN_RESTORE_Y_KEY);
        double z = persistentData.getDouble(SPIN_RESTORE_Z_KEY);
        float yaw = persistentData.getFloat(SPIN_RESTORE_YAW_KEY);
        float pitch = persistentData.getFloat(SPIN_RESTORE_PITCH_KEY);
        double facingX = persistentData.getDouble(SPIN_RESTORE_FACING_X_KEY);
        double facingY = persistentData.getDouble(SPIN_RESTORE_FACING_Y_KEY);
        double facingZ = persistentData.getDouble(SPIN_RESTORE_FACING_Z_KEY);

        if (livingEntity instanceof ServerPlayer player) {
            player.teleportTo(serverLevel, x, y, z, yaw, pitch);
        } else {
            livingEntity.teleportTo(x, y, z);
            livingEntity.setYRot(yaw);
            livingEntity.setXRot(pitch);
        }

        livingEntity.setYHeadRot(yaw);
        livingEntity.lookAt(EntityAnchorArgument.Anchor.EYES, livingEntity.position().add(facingX, facingY, facingZ));

        if (livingEntity instanceof ServerPlayer player && persistentData.contains(SPIN_RESTORE_GAMEMODE_KEY)) {
            player.setGameMode(GameType.byId(persistentData.getInt(SPIN_RESTORE_GAMEMODE_KEY)));
        }

        if (livingEntity instanceof ServerPlayer player) {
            if (persistentData.contains(SPIN_RESTORE_MESSAGE_TARGET_NAME_KEY)) {
                String targetName = persistentData.getString(SPIN_RESTORE_MESSAGE_TARGET_NAME_KEY);
                player.sendSystemMessage(Component.literal("Awesome Exploded Entity: " + targetName + "."));
            } else if (persistentData.contains(SPIN_RESTORE_MESSAGE_TARGET_COUNT_KEY)) {
                int targetCount = persistentData.getInt(SPIN_RESTORE_MESSAGE_TARGET_COUNT_KEY);
                player.sendSystemMessage(Component.literal("Awesome Exploded " + targetCount + " Entities."));
            }
        }

        persistentData.remove(SPIN_RESTORE_TICK_KEY);
        persistentData.remove(SPIN_RESTORE_X_KEY);
        persistentData.remove(SPIN_RESTORE_Y_KEY);
        persistentData.remove(SPIN_RESTORE_Z_KEY);
        persistentData.remove(SPIN_RESTORE_YAW_KEY);
        persistentData.remove(SPIN_RESTORE_PITCH_KEY);
        persistentData.remove(SPIN_RESTORE_FACING_X_KEY);
        persistentData.remove(SPIN_RESTORE_FACING_Y_KEY);
        persistentData.remove(SPIN_RESTORE_FACING_Z_KEY);
        persistentData.remove(SPIN_RESTORE_HAMMER_UUID_KEY);
        persistentData.remove(SPIN_RESTORE_GAMEMODE_KEY);
        persistentData.remove(SPIN_RESTORE_MESSAGE_TARGET_NAME_KEY);
        persistentData.remove(SPIN_RESTORE_MESSAGE_TARGET_COUNT_KEY);
    }

    private static void lockCommandStunnedLookDirection(net.minecraft.world.entity.player.Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(COMMAND_STUN_LOCK_YAW_KEY)) {
            persistentData.putFloat(COMMAND_STUN_LOCK_YAW_KEY, player.getYRot());
        }
        if (!persistentData.contains(COMMAND_STUN_LOCK_PITCH_KEY)) {
            persistentData.putFloat(COMMAND_STUN_LOCK_PITCH_KEY, player.getXRot());
        }
    }

    private static void applyLockedPlayerRotation(net.minecraft.world.entity.player.Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(COMMAND_STUN_LOCK_YAW_KEY) || !persistentData.contains(COMMAND_STUN_LOCK_PITCH_KEY)) {
            return;
        }

        float lockedYaw = persistentData.getFloat(COMMAND_STUN_LOCK_YAW_KEY);
        float lockedPitch = persistentData.getFloat(COMMAND_STUN_LOCK_PITCH_KEY);

        player.setYRot(lockedYaw);
        player.setXRot(lockedPitch);
        player.setYHeadRot(lockedYaw);
        player.setYBodyRot(lockedYaw);
        player.yRotO = lockedYaw;
        player.xRotO = lockedPitch;
        player.yHeadRotO = lockedYaw;
        player.yBodyRotO = lockedYaw;
    }

    private static void clearCommandStunLookLock(CompoundTag persistentData) {
        persistentData.remove(COMMAND_STUN_LOCK_YAW_KEY);
        persistentData.remove(COMMAND_STUN_LOCK_PITCH_KEY);
    }
}