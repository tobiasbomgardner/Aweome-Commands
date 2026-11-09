package net.tobias.awesome_commands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.tobias.awesome_commands.visual.PersistentShape;
import net.tobias.awesome_commands.visual.PersistentShapeController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AwesomeMeteorShowerCmd {
    private static final long METEOR_Area_DURATION_TICKS = 1200L;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("awesome-meteor_shower")
                        .then(Commands.argument("target_cords", Vec3Argument.vec3())
                                .executes(context -> {
                                    Coordinates cords = Vec3Argument.getCoordinates(context, "target_cords");
                                    return meteorShower(context, cords);
                                }))
                        .then(Commands.argument("target_entity", EntityArgument.entity())
                                .executes(context -> {
                                    var entityTarget = EntityArgument.getEntity(context, "target_entity");
                                    List<Integer> cords = new ArrayList<>(List.of(entityTarget.getBlockX(), entityTarget.getBlockY(), entityTarget.getBlockZ()));
                                    return meteorShower(context, entityTarget, cords);
                                })));
    }

    public static int meteorShower(CommandContext<CommandSourceStack> context, Entity entity, List<Integer> cords) {
        double centerX = cords.get(0);
        double centerY = cords.get(1);
        double centerZ = cords.get(2);
        double radius = 70;
        spawnMeteorArea(context.getSource().getLevel(), centerX, centerY, centerZ, radius);
        return 1;
    }

    public static int meteorShower(CommandContext<CommandSourceStack> context, Coordinates cords) {
        var targetPos = cords.getPosition(context.getSource());
        double centerX = targetPos.x;
        double centerY = targetPos.y;
        double centerZ = targetPos.z;
        double radius = 70;
        spawnMeteorArea(context.getSource().getLevel(), centerX, centerY, centerZ, radius);
        return 1;
    }

    private static void spawnMeteorArea(ServerLevel level, double centerX, double centerY, double centerZ, double radius) {
        UUID shapeId = UUID.randomUUID();
        double actualCenterY = getHighestTerrainY(level, centerX, centerZ, radius);
        PersistentShapeController.spawnOrReplaceForAll(level, shapeId, PersistentShape.CIRCLE, centerX,
                actualCenterY, centerZ, (float) radius, (float) radius, 0.0F, 0.0F, 1.00F, 0.75F, true, true);
        PersistentShapeController.scheduleRemoval(level, shapeId, METEOR_Area_DURATION_TICKS);
    }

    private static double getHighestTerrainY(ServerLevel level, double centerX, double centerZ, double radius) {
        double maxY = Double.NEGATIVE_INFINITY;
        int sampleCount = 64;
        for (int i = 0; i < sampleCount; i++) {
            double angle = (Math.PI * 2.0D * i) / sampleCount;
            double x = centerX + (Math.cos(angle) * radius);
            double z = centerZ + (Math.sin(angle) * radius);
            maxY = Math.max(maxY, getHighestSolidY(level, x, z));
        }
        maxY = Math.max(maxY, getHighestSolidY(level, centerX, centerZ));
        return Double.isFinite(maxY) ? maxY : level.getMinBuildHeight();
    }

    private static double getHighestSolidY(ServerLevel level, double x, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int highestY = level.getMinBuildHeight();
        for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--) {
            pos.set(blockX, y, blockZ);
            if (!level.getBlockState(pos).isAir()) {
                highestY = y + 1;
                break;
            }
        }
        return highestY;
    }
}