package net.tobias.awesome_commands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.tobias.awesome_commands.effects.ModEffects;
import net.tobias.awesome_commands.entity.ModEntities;
import net.tobias.awesome_commands.entity.entities.AwesomeHammers;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AwesomeExplodeCmd {
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("awesome-explode")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> {
                                    var targets = EntityArgument.getEntities(context, "targets");
                                    return awesomeExplode(context, targets);
                                })));
    }

    public static List<ArrayList<Integer>> getCords(Collection<? extends Entity> targets) {
        List<ArrayList<Integer>> cords = new ArrayList<>();
        ArrayList<Integer> x = new ArrayList<>();
        ArrayList<Integer> y = new ArrayList<>();
        ArrayList<Integer> z = new ArrayList<>();
        targets.forEach(target -> {
            x.add(target.getBlockX());
            y.add(target.getBlockY());
            z.add(target.getBlockZ());
        });
        cords.add(x);
        cords.add(y);
        cords.add(z);
        return cords;
    }

    public static int awesomeExplode(CommandContext<CommandSourceStack> context, Collection<? extends Entity> targets) throws CommandSyntaxException {
        List<Entity> targetList = new ArrayList<>(targets);
        Level level = context.getSource().getLevel();
        double topY = level.getMaxBuildHeight() - 1.0D;
        Entity commandExecutor = context.getSource().getEntity();
        if (commandExecutor != null) {
            for (Entity target : targetList) {
                if (target.getUUID().equals(commandExecutor.getUUID())) {
                    final SimpleCommandExceptionType SELF_TARGET_FORBIDDEN =
                            new SimpleCommandExceptionType(Component.literal("The command executor cannot target themself."));
                    throw SELF_TARGET_FORBIDDEN.create();
                }
            }
        }
        if (targets.size() > 1) {
            for (Entity target : targetList) {
                if (target instanceof LivingEntity livingTarget) {
                    var persistentData = livingTarget.getPersistentData();
                    persistentData.remove(ASCEND_TARGET_Y_KEY);
                    persistentData.remove(ASCEND_SPEED_KEY);
                    persistentData.remove(ASCEND_PENDING_HAMMER_KEY);
                    persistentData.remove(ASCEND_EXECUTOR_UUID_KEY);
                    persistentData.remove(TARGET_RESTORE_TICK_KEY);
                    persistentData.remove(TARGET_RESTORE_HAMMER_UUID_KEY);
                    persistentData.putDouble(TARGET_RESTORE_X_KEY, livingTarget.getX());
                    persistentData.putDouble(TARGET_RESTORE_Y_KEY, livingTarget.getY());
                    persistentData.putDouble(TARGET_RESTORE_Z_KEY, livingTarget.getZ());

                    spawnRainbowAscensionLine((ServerLevel) level, livingTarget.getX(), livingTarget.getZ(), livingTarget.getY(), topY);
                    teleportEntity(livingTarget, level, livingTarget.getX(), topY, livingTarget.getZ());

                    AwesomeHammers awesomeHammers = ModEntities.AWESOME_HAMMERS.get().create(level);
                    if (awesomeHammers == null) {
                        continue;
                    }
                    awesomeHammers.moveTo(livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(), 0.0F, 0.0F);
                    level.addFreshEntity(awesomeHammers);
                    awesomeHammers.setDamageTarget(livingTarget);
                    if (commandExecutor != null) {
                        awesomeHammers.setDamageExecutor(commandExecutor);
                    }
                    awesomeHammers.startSlamAnimation();
                    persistentData.putUUID(TARGET_RESTORE_HAMMER_UUID_KEY, awesomeHammers.getUUID());
                } else {
                    teleportEntity(target, level, target.getX(), topY, target.getZ());
                    AwesomeHammers awesomeHammers = ModEntities.AWESOME_HAMMERS.get().create(level);
                    if (awesomeHammers == null) {
                        continue;
                    }
                    awesomeHammers.moveTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
                    level.addFreshEntity(awesomeHammers);
                    awesomeHammers.setDamageTarget(target);
                    if (commandExecutor != null) {
                        awesomeHammers.setDamageExecutor(commandExecutor);
                    }
                    awesomeHammers.startSlamAnimation();
                }
            }
            context.getSource().sendSuccess(
                    () -> Component.literal("Awesome Exploded " + targets.size() + " Entities."),
                    false
            );
            return 1;
        } else if (targets.size() == 1) {
            var source = context.getSource();
            var executor = source.getEntity();
            if (executor == null) {
                final SimpleCommandExceptionType NO_EXECUTOR =
                        new SimpleCommandExceptionType(Component.literal("This command requires an entity executor."));
                throw NO_EXECUTOR.create();
            }

            double exX = executor.getX();
            double exY = executor.getY();
            double exZ = executor.getZ();
            float exYaw = executor.getYRot();
            float exPitch = executor.getXRot();
            var exFacing = executor.getLookAngle();
            double exFX = exFacing.x;
            double exFY = exFacing.y;
            double exFZ = exFacing.z;
            if (executor instanceof ServerPlayer player) {
                player.getPersistentData().putInt(SPIN_RESTORE_GAMEMODE_KEY, player.gameMode.getGameModeForPlayer().getId());
                player.setGameMode(GameType.SPECTATOR);
            }

            Entity target = targetList.get(0);
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(
                        ModEffects.COMMAND_STUN.get(),
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false,
                        false
                ));
            }

            double dx = executor.getX() - target.getX();
            double dz = executor.getZ() - target.getZ();
            double magnitude = Math.sqrt((dx * dx) + (dz * dz));
            if (magnitude < 0.001D) {
                dx = 1.0D;
                dz = 0.0D;
                magnitude = 1.0D;
            }

            double orbitX = target.getX() + (dx / magnitude) * 10.0D;
            double orbitZ = target.getZ() + (dz / magnitude) * 10.0D;
            teleportEntity(executor, level, orbitX, target.getY(), orbitZ);
            executor.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

            var executorData = executor.getPersistentData();
            executorData.remove(SPIN_RESTORE_TICK_KEY);
            executorData.remove(SPIN_RESTORE_HAMMER_UUID_KEY);
            executorData.putDouble(SPIN_RESTORE_X_KEY, exX);
            executorData.putDouble(SPIN_RESTORE_Y_KEY, exY);
            executorData.putDouble(SPIN_RESTORE_Z_KEY, exZ);
            executorData.putFloat(SPIN_RESTORE_YAW_KEY, exYaw);
            executorData.putFloat(SPIN_RESTORE_PITCH_KEY, exPitch);
            executorData.putDouble(SPIN_RESTORE_FACING_X_KEY, exFX);
            executorData.putDouble(SPIN_RESTORE_FACING_Y_KEY, exFY);
            executorData.putDouble(SPIN_RESTORE_FACING_Z_KEY, exFZ);
            executorData.putString(SPIN_RESTORE_MESSAGE_TARGET_NAME_KEY, target.getName().getString());
            executorData.putInt(SPIN_RESTORE_MESSAGE_TARGET_COUNT_KEY, 1);

            if (target instanceof LivingEntity livingTarget) {
                var targetData = livingTarget.getPersistentData();
                targetData.remove(TARGET_RESTORE_TICK_KEY);
                targetData.remove(TARGET_RESTORE_HAMMER_UUID_KEY);
                targetData.putDouble(TARGET_RESTORE_X_KEY, livingTarget.getX());
                targetData.putDouble(TARGET_RESTORE_Y_KEY, livingTarget.getY());
                targetData.putDouble(TARGET_RESTORE_Z_KEY, livingTarget.getZ());
                targetData.putDouble(ASCEND_TARGET_Y_KEY, topY);
                targetData.putDouble(ASCEND_SPEED_KEY, 3.5D);
                targetData.putBoolean(ASCEND_PENDING_HAMMER_KEY, true);
                targetData.putUUID(ASCEND_EXECUTOR_UUID_KEY, executor.getUUID());
            } else {
                teleportEntity(target, level, target.getX(), topY, target.getZ());
                teleportEntity(executor, level, orbitX, topY, orbitZ);
                executor.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                if (!target.getUUID().equals(executor.getUUID())) {
                    startForcedSpin(executor, target);
                }
                AwesomeHammers awesomeHammers = ModEntities.AWESOME_HAMMERS.get().create(level);
                if (awesomeHammers == null) {
                    final SimpleCommandExceptionType AWESOME_HAMMERS_NULL =
                            new SimpleCommandExceptionType(Component.literal("Failed to summon Awesome Hammers. Please try again."));
                    throw AWESOME_HAMMERS_NULL.create();
                }
                awesomeHammers.moveTo(target.getX(), target.getY(), target.getZ(), 0.0F, 0.0F);
                level.addFreshEntity(awesomeHammers);
                awesomeHammers.setDamageTarget(target);
                awesomeHammers.setDamageExecutor(executor);
                awesomeHammers.startSlamAnimation();
                executorData.putUUID(SPIN_RESTORE_HAMMER_UUID_KEY, awesomeHammers.getUUID());
            }
            if (!(executor instanceof LivingEntity)) {
                teleportEntity(executor, level, orbitX, topY, orbitZ);
                executor.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            }

            return 1;
        }
        return 0;
    }

    public static void startForcedSpin(Entity executor, Entity target) {
        double dx = executor.getX() - target.getX();
        double dz = executor.getZ() - target.getZ();
        double radius = 10.0D;

        var persistentData = executor.getPersistentData();
        persistentData.putUUID(SPIN_TARGET_UUID_KEY, target.getUUID());
        persistentData.putDouble(SPIN_RADIUS_KEY, radius);
        persistentData.putDouble(SPIN_CURRENT_ANGLE_KEY, Math.atan2(dz, dx));
        persistentData.putDouble(SPIN_Y_OFFSET_KEY, 0.0D);
        persistentData.putDouble(SPIN_RADIANS_PER_TICK_KEY, (Math.PI * 2.0D) / 40.0D);
        executor.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
    }

    public static void stopForcedSpin(Entity executor) {
        var persistentData = executor.getPersistentData();
        persistentData.remove(SPIN_TARGET_UUID_KEY);
        persistentData.remove(SPIN_RADIUS_KEY);
        persistentData.remove(SPIN_CURRENT_ANGLE_KEY);
        persistentData.remove(SPIN_Y_OFFSET_KEY);
        persistentData.remove(SPIN_RADIANS_PER_TICK_KEY);
    }

    private static void teleportEntity(Entity entity, Level level, double x, double y, double z) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo((ServerLevel) level, x, y, z, player.getYRot(), player.getXRot());
            return;
        }
        entity.teleportTo(x, y, z);
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

        double minY = Math.min(startY, endY);
        double maxY = Math.max(startY, endY);
        for (Vector3f color : rainbow) {
            DustParticleOptions dust = new DustParticleOptions(color, 1.5F);
            for (double y = minY; y <= maxY; y += 2.0D) {
                level.sendParticles(dust, x, y, z, 4, 0.05D, 0.05D, 0.05D, 0.0D);
            }
        }
    }
}