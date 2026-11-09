package net.tobias.awesome_commands.visual;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.tobias.awesome_commands.network.ModNetwork;
import net.tobias.awesome_commands.network.packet.RemovePersistentShapePacket;
import net.tobias.awesome_commands.network.packet.SpawnPersistentShapePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PersistentShapeController {
    private static final Map<UUID, ScheduledRemoval> SCHEDULED_REMOVALS = new ConcurrentHashMap<>();

    private PersistentShapeController() {}

    public static void restorePersistedRemovals(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            PersistentShapeData data = PersistentShapeData.get(level);
            for (PersistentShapeData.ShapeRecord record : data.getShapes().values()) {
                if (record.expiresAtGameTime() <= 0L) {
                    continue;
                }
                long now = level.getGameTime();
                if (now >= record.expiresAtGameTime()) {
                    removeForAll(level, record.id());
                    continue;
                }
                SCHEDULED_REMOVALS.put(record.id(), new ScheduledRemoval(level.dimension(), record.expiresAtGameTime()));
            }
        }
    }

    public static void spawnOrReplaceForAll(ServerLevel level, UUID id, PersistentShape shape, double centerX, double centerY,
                                            double centerZ, float xRadius, float zRadius, float red, float green, float blue,
                                            float alpha, boolean adaptToTerrain, boolean persistAcrossWorlds) {
        PersistentShapeData.ShapeRecord record = new PersistentShapeData.ShapeRecord(id, shape, centerX, centerY, centerZ,
                Math.max(0.05F, xRadius), Math.max(0.05F, zRadius), clamp01(red), clamp01(green), clamp01(blue), clamp01(alpha),
                adaptToTerrain, persistAcrossWorlds, -1L);
        if (persistAcrossWorlds) {
            PersistentShapeData.get(level).addOrReplace(record);
        }

        SpawnPersistentShapePacket packet = new SpawnPersistentShapePacket(id, shape, centerX, centerY, centerZ, record.xRadius(),
                record.zRadius(), record.red(), record.green(), record.blue(), record.alpha(), adaptToTerrain);

        for (ServerPlayer player : level.players()) {
            ModNetwork.sendToPlayer(player, packet);
        }
    }

    public static void removeForAll(ServerLevel level, UUID id) {
        PersistentShapeData.get(level).remove(id);
        RemovePersistentShapePacket packet = new RemovePersistentShapePacket(id);
        for (ServerPlayer player : level.players()) {
            ModNetwork.sendToPlayer(player, packet);
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        PersistentShapeData data = PersistentShapeData.get(level);
        for (PersistentShapeData.ShapeRecord record : data.getShapes().values()) {
            if (!record.persistAcrossWorlds()) {
                continue;
            }
            SpawnPersistentShapePacket packet = new SpawnPersistentShapePacket(record.id(), record.shape(), record.centerX(),
                    record.centerY(), record.centerZ(), record.xRadius(), record.zRadius(), record.red(), record.green(), record.blue(),
                    record.alpha(), record.adaptToTerrain());
            ModNetwork.sendToPlayer(player, packet);
        }
    }

    public static void scheduleRemoval(ServerLevel level, UUID id, long delayTicks) {
        long clampedDelay = Math.max(0L, delayTicks);
        long expiresAtGameTime = level.getGameTime() + clampedDelay;
        PersistentShapeData data = PersistentShapeData.get(level);
        PersistentShapeData.ShapeRecord existing = data.getShapes().get(id);
        if (existing != null) {
            data.addOrReplace(existing.withExpiry(expiresAtGameTime));
        }
        SCHEDULED_REMOVALS.put(id, new ScheduledRemoval(level.dimension(), expiresAtGameTime));
    }

    public static void processScheduledRemovals(MinecraftServer server) {
        if (SCHEDULED_REMOVALS.isEmpty()) {
            return;
        }

        List<UUID> completedRemovals = new ArrayList<>();
        for (Map.Entry<UUID, ScheduledRemoval> entry : SCHEDULED_REMOVALS.entrySet()) {
            UUID shapeId = entry.getKey();
            ScheduledRemoval scheduledRemoval = entry.getValue();
            ServerLevel level = server.getLevel(scheduledRemoval.dimension());
            if (level == null || level.getGameTime() < scheduledRemoval.removeAtGameTime()) {
                continue;
            }
            removeForAll(level, shapeId);
            completedRemovals.add(shapeId);
        }

        for (UUID completedId : completedRemovals) {
            SCHEDULED_REMOVALS.remove(completedId);
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private record ScheduledRemoval(ResourceKey<Level> dimension, long removeAtGameTime) {}
}
