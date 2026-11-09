package net.tobias.awesome_commands.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.tobias.awesome_commands.client.particles.ClientPersistentShapeManager;
import net.tobias.awesome_commands.visual.PersistentShape;

import java.util.UUID;
import java.util.function.Supplier;

public class SpawnPersistentShapePacket {
    private final UUID id;
    private final PersistentShape shape;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float xRadius;
    private final float zRadius;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private final boolean adaptToTerrain;

    public SpawnPersistentShapePacket(UUID id, PersistentShape shape, double centerX, double centerY, double centerZ, float xRadius,
                                      float zRadius, float red, float green, float blue, float alpha, boolean adaptToTerrain) {
        this.id = id;
        this.shape = shape;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.xRadius = xRadius;
        this.zRadius = zRadius;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.adaptToTerrain = adaptToTerrain;
    }

    public static void encode(SpawnPersistentShapePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.id);
        buffer.writeEnum(packet.shape);
        buffer.writeDouble(packet.centerX);
        buffer.writeDouble(packet.centerY);
        buffer.writeDouble(packet.centerZ);
        buffer.writeFloat(packet.xRadius);
        buffer.writeFloat(packet.zRadius);
        buffer.writeFloat(packet.red);
        buffer.writeFloat(packet.green);
        buffer.writeFloat(packet.blue);
        buffer.writeFloat(packet.alpha);
        buffer.writeBoolean(packet.adaptToTerrain);
    }

    public static SpawnPersistentShapePacket decode(FriendlyByteBuf buffer) {
        return new SpawnPersistentShapePacket(buffer.readUUID(), buffer.readEnum(PersistentShape.class), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()
        );
    }

    public static void handle(SpawnPersistentShapePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPersistentShapeManager.addOrReplace(packet.id, packet.shape, packet.centerX, packet.centerY, packet.centerZ,
                        packet.xRadius, packet.zRadius, packet.red, packet.green, packet.blue, packet.alpha, packet.adaptToTerrain
                )));
        context.setPacketHandled(true);
    }
}
