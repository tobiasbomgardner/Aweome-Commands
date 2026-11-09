package net.tobias.awesome_commands.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.tobias.awesome_commands.client.particles.ClientPersistentShapeManager;

import java.util.UUID;
import java.util.function.Supplier;

public class RemovePersistentShapePacket {
    private final UUID id;

    public RemovePersistentShapePacket(UUID id) {
        this.id = id;
    }

    public static void encode(RemovePersistentShapePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.id);
    }

    public static RemovePersistentShapePacket decode(FriendlyByteBuf buffer) {
        return new RemovePersistentShapePacket(buffer.readUUID());
    }

    public static void handle(RemovePersistentShapePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPersistentShapeManager.remove(packet.id)));
        context.setPacketHandled(true);
    }
}
