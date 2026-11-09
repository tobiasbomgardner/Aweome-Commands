package net.tobias.awesome_commands.client.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.tobias.awesome_commands.visual.PersistentShape;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPersistentShapeManager {
    private static final Map<UUID, ShapeInstance> SHAPES = new ConcurrentHashMap<>();
    private static final double Y_LIFT = 0.0D;
    private static final double SURFACE_OFFSET = 0.0625D;

    private ClientPersistentShapeManager() {}

    public static void addOrReplace(UUID id, PersistentShape shape, double centerX, double centerY, double centerZ, float xRadius,
                                    float zRadius, float red, float green, float blue, float alpha, boolean adaptToTerrain) {
        SHAPES.put(id, new ShapeInstance(shape, centerX, centerY, centerZ, xRadius, zRadius, red, green, blue, alpha, adaptToTerrain));
    }

    public static void remove(UUID id) {
        SHAPES.remove(id);
    }

    public static void clear() {
        SHAPES.clear();
    }

    public static void render(RenderLevelStageEvent event) {
        if (SHAPES.isEmpty()) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        double cameraX = event.getCamera().getPosition().x;
        double cameraY = event.getCamera().getPosition().y;
        double cameraZ = event.getCamera().getPosition().z;
        double renderDistanceSq = 128.0D * 128.0D;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.pushPose();
        poseStack.translate(-cameraX, -cameraY, -cameraZ);
        Matrix4f matrix = poseStack.last().pose();
        for (ShapeInstance shape : SHAPES.values()) {
            double dx = shape.centerX - cameraX;
            double dy = shape.centerY - cameraY;
            double dz = shape.centerZ - cameraZ;
            if ((dx * dx) + (dy * dy) + (dz * dz) > renderDistanceSq) {
                continue;
            }
            if (shape.adaptToTerrain) {
                drawTerrainAdaptiveShape(matrix, shape, level);
                continue;
            }
            switch (shape.shape) {
                case CIRCLE -> drawCircle(matrix, shape);
                case SQUARE, RECTANGLE -> drawRectangle(matrix, shape);
            }
        }
        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawTerrainAdaptiveShape(Matrix4f matrix, ShapeInstance shape, ClientLevel level) {
        if (shape.shape == PersistentShape.CIRCLE) {
            drawAdaptiveCircle(matrix, shape, level);
            return;
        }

        int minX = Mth.floor(shape.centerX - shape.xRadius);
        int maxX = Mth.floor(shape.centerX + shape.xRadius);
        int minZ = Mth.floor(shape.centerZ - shape.zRadius);
        int maxZ = Mth.floor(shape.centerZ + shape.zRadius);
        Map<Long, Double> heightCache = new HashMap<>();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                if (!isInsideShape(shape, blockX, blockZ)) {
                    continue;
                }

                double topY = getTerrainTop(level, blockX, blockZ, heightCache);
                addTopQuad(builder, matrix, blockX, blockZ, topY, shape);
            }
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void drawAdaptiveCircle(Matrix4f matrix, ShapeInstance shape, ClientLevel level) {
        int minX = Mth.floor(shape.centerX - shape.xRadius);
        int maxX = Mth.floor(shape.centerX + shape.xRadius);
        int minZ = Mth.floor(shape.centerZ - shape.zRadius);
        int maxZ = Mth.floor(shape.centerZ + shape.zRadius);
        Map<Long, Double> heightCache = new HashMap<>();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                if (!isInsideShape(shape, blockX, blockZ)) {
                    continue;
                }

                double surfaceY = getTerrainTop(level, blockX, blockZ, heightCache) + Y_LIFT;
                double topPlaneY = surfaceY + SURFACE_OFFSET;
                addTopQuad(builder, matrix, blockX, blockZ, topPlaneY, shape);

                for (Direction direction : Direction.values()) {
                    int neighborX = blockX + direction.dx();
                    int neighborZ = blockZ + direction.dz();
                    boolean neighborInside = isInsideShape(shape, neighborX, neighborZ);
                    double neighborSurfaceY = getTerrainTop(level, neighborX, neighborZ, heightCache) + Y_LIFT;
                    if (!neighborInside || Math.abs(neighborSurfaceY - surfaceY) > 0.25D) {
                        double wallTopY = Math.max(surfaceY, neighborSurfaceY) + SURFACE_OFFSET;
                        double wallBottomY = Math.min(surfaceY, neighborSurfaceY) + SURFACE_OFFSET;
                        addSideQuad(builder, matrix, blockX, blockZ, direction, wallTopY, wallBottomY, shape);
                    }
                }
            }
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static boolean isInsideShape(ShapeInstance shape, int blockX, int blockZ) {
        double x = blockX + 0.5D;
        double z = blockZ + 0.5D;
        return switch (shape.shape) {
            case SQUARE, RECTANGLE -> Math.abs(x - shape.centerX) <= shape.xRadius && Math.abs(z - shape.centerZ) <= shape.zRadius;
            case CIRCLE -> {
                double normalizedX = (x - shape.centerX) / shape.xRadius;
                double normalizedZ = (z - shape.centerZ) / shape.zRadius;
                yield (normalizedX * normalizedX) + (normalizedZ * normalizedZ) <= 1.0D;
            }
        };
    }

    private static double getTerrainTopAt(ClientLevel level, double worldX, double worldZ) {
        int blockX = Mth.floor(worldX);
        int blockZ = Mth.floor(worldZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int maxY = level.getMaxBuildHeight() - 1;
        for (int y = maxY; y >= level.getMinBuildHeight(); y--) {
            pos.set(blockX, y, blockZ);
            var state = level.getBlockState(pos);
            if (!state.isAir() && isSolidTerrainBlock(level, pos, state)) {
                return y + 1.0D;
            }
        }
        return level.getMinBuildHeight();
    }

    private static boolean isSolidTerrainBlock(ClientLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.isSolidRender(level, pos)) {
            return true;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static double getTerrainTop(ClientLevel level, int blockX, int blockZ, Map<Long, Double> heightCache) {
        long key = toHeightKey(blockX, blockZ);
        Double cached = heightCache.get(key);
        if (cached != null) {
            return cached;
        }
        double topY = getTerrainTopAt(level, blockX + 0.5D, blockZ + 0.5D);
        heightCache.put(key, topY);
        return topY;
    }

    private static long toHeightKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private static void addTopQuad(BufferBuilder builder, Matrix4f matrix, int blockX, int blockZ, double topY, ShapeInstance shape) {
        addVertex(builder, matrix, blockX, topY, blockZ, shape);
        addVertex(builder, matrix, blockX, topY, blockZ + 1.0D, shape);
        addVertex(builder, matrix, blockX + 1.0D, topY, blockZ + 1.0D, shape);
        addVertex(builder, matrix, blockX + 1.0D, topY, blockZ, shape);
    }

    private static void addSideQuad(BufferBuilder builder, Matrix4f matrix, int blockX, int blockZ, Direction direction, double topY, double bottomY, ShapeInstance shape) {
        double minX = blockX;
        double maxX = blockX + 1.0D;
        double minZ = blockZ;
        double maxZ = blockZ + 1.0D;
        double outwardOffset = SURFACE_OFFSET;

        switch (direction) {
            case NORTH -> {
                double northZ = minZ - outwardOffset;
                addVertex(builder, matrix, minX, topY, northZ, shape);
                addVertex(builder, matrix, maxX, topY, northZ, shape);
                addVertex(builder, matrix, maxX, bottomY, northZ, shape);
                addVertex(builder, matrix, minX, bottomY, northZ, shape);
            }
            case SOUTH -> {
                double southZ = maxZ + outwardOffset;
                addVertex(builder, matrix, maxX, topY, southZ, shape);
                addVertex(builder, matrix, minX, topY, southZ, shape);
                addVertex(builder, matrix, minX, bottomY, southZ, shape);
                addVertex(builder, matrix, maxX, bottomY, southZ, shape);
            }
            case WEST -> {
                double westX = minX - outwardOffset;
                addVertex(builder, matrix, westX, topY, minZ, shape);
                addVertex(builder, matrix, westX, topY, maxZ, shape);
                addVertex(builder, matrix, westX, bottomY, maxZ, shape);
                addVertex(builder, matrix, westX, bottomY, minZ, shape);
            }
            case EAST -> {
                double eastX = maxX + outwardOffset;
                addVertex(builder, matrix, eastX, topY, maxZ, shape);
                addVertex(builder, matrix, eastX, topY, minZ, shape);
                addVertex(builder, matrix, eastX, bottomY, minZ, shape);
                addVertex(builder, matrix, eastX, bottomY, maxZ, shape);
            }
        }
    }

    private static void drawRectangle(Matrix4f matrix, ShapeInstance shape) {
        double y = shape.centerY;
        double minX = shape.centerX - shape.xRadius;
        double maxX = shape.centerX + shape.xRadius;
        double minZ = shape.centerZ - shape.zRadius;
        double maxZ = shape.centerZ + shape.zRadius;

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addVertex(builder, matrix, minX, y, minZ, shape);
        addVertex(builder, matrix, minX, y, maxZ, shape);
        addVertex(builder, matrix, maxX, y, maxZ, shape);
        addVertex(builder, matrix, maxX, y, minZ, shape);
        BufferUploader.drawWithShader(builder.end());
    }

    private static void drawCircle(Matrix4f matrix, ShapeInstance shape) {
        double y = shape.centerY;
        int segments = Math.max(48, (int) Math.ceil((2.0D * Math.PI * Math.max(shape.xRadius, shape.zRadius)) * 2.0D));

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        addVertex(builder, matrix, shape.centerX, y, shape.centerZ, shape);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            double x = shape.centerX + (Math.cos(angle) * shape.xRadius);
            double z = shape.centerZ + (Math.sin(angle) * shape.zRadius);
            addVertex(builder, matrix, x, y, z, shape);
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private static void addVertex(BufferBuilder builder, Matrix4f matrix, double x, double y, double z, ShapeInstance shape) {
        builder.vertex(matrix, (float) x, (float) y, (float) z)
                .color(shape.red, shape.green, shape.blue, shape.alpha)
                .endVertex();
    }

    private enum Direction {
        NORTH(0, -1),
        SOUTH(0, 1),
        EAST(1, 0),
        WEST(-1, 0);

        private final int dx;
        private final int dz;

        Direction(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public int dx() {
            return dx;
        }

        public int dz() {
            return dz;
        }
    }

    private record ShapeInstance(PersistentShape shape, double centerX, double centerY, double centerZ, float xRadius, float zRadius,
                                 float red, float green, float blue, float alpha, boolean adaptToTerrain) {}
}
