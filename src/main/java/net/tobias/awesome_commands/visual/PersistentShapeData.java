package net.tobias.awesome_commands.visual;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PersistentShapeData extends SavedData {
    private static final String DATA_KEY = "persistent_shapes";
    private final Map<UUID, ShapeRecord> shapes = new HashMap<>();

    public static PersistentShapeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PersistentShapeData::load, PersistentShapeData::new, DATA_KEY);
    }

    public static PersistentShapeData load(CompoundTag tag) {
        PersistentShapeData data = new PersistentShapeData();
        CompoundTag shapesTag = tag.getCompound("shapes");
        for (String key : shapesTag.getAllKeys()) {
            CompoundTag shapeTag = shapesTag.getCompound(key);
            ShapeRecord record = ShapeRecord.load(shapeTag);
            if (record != null) {
                data.shapes.put(record.id(), record);
            }
        }
        return data;
    }

    public Map<UUID, ShapeRecord> getShapes() {
        return shapes;
    }

    public void addOrReplace(ShapeRecord record) {
        shapes.put(record.id(), record);
        setDirty();
    }

    public void remove(UUID id) {
        shapes.remove(id);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag shapesTag = new CompoundTag();
        for (Map.Entry<UUID, ShapeRecord> entry : shapes.entrySet()) {
            shapesTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        tag.put("shapes", shapesTag);
        return tag;
    }

    public record ShapeRecord(UUID id, PersistentShape shape, double centerX, double centerY, double centerZ, float xRadius,
                              float zRadius, float red, float green, float blue, float alpha, boolean adaptToTerrain,
                              boolean persistAcrossWorlds, long expiresAtGameTime) {
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id.toString());
            tag.putString("shape", shape.name());
            tag.putDouble("centerX", centerX);
            tag.putDouble("centerY", centerY);
            tag.putDouble("centerZ", centerZ);
            tag.putFloat("xRadius", xRadius);
            tag.putFloat("zRadius", zRadius);
            tag.putFloat("red", red);
            tag.putFloat("green", green);
            tag.putFloat("blue", blue);
            tag.putFloat("alpha", alpha);
            tag.putBoolean("adaptToTerrain", adaptToTerrain);
            tag.putBoolean("persistAcrossWorlds", persistAcrossWorlds);
            tag.putLong("expiresAtGameTime", expiresAtGameTime);
            return tag;
        }

        public static ShapeRecord load(CompoundTag tag) {
            try {
                return new ShapeRecord(UUID.fromString(tag.getString("id")), PersistentShape.valueOf(tag.getString("shape")),
                        tag.getDouble("centerX"), tag.getDouble("centerY"), tag.getDouble("centerZ"),
                        tag.getFloat("xRadius"), tag.getFloat("zRadius"), tag.getFloat("red"), tag.getFloat("green"),
                        tag.getFloat("blue"), tag.getFloat("alpha"), tag.getBoolean("adaptToTerrain"),
                        tag.getBoolean("persistAcrossWorlds"), tag.getLong("expiresAtGameTime"));
            } catch (Exception ignored) {
                return null;
            }
        }

        public ShapeRecord withExpiry(long expiresAtGameTime) {
            return new ShapeRecord(id, shape, centerX, centerY, centerZ, xRadius, zRadius, red, green, blue, alpha, adaptToTerrain, persistAcrossWorlds, expiresAtGameTime);
        }
    }
}
