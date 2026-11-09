package net.tobias.awesome_commands.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.tobias.awesome_commands.AwesomeCommands;
import net.tobias.awesome_commands.network.packet.RemovePersistentShapePacket;
import net.tobias.awesome_commands.network.packet.SpawnPersistentShapePacket;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(AwesomeCommands.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();
    private static boolean registered = false;

    private ModNetwork() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(SpawnPersistentShapePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpawnPersistentShapePacket::encode)
                .decoder(SpawnPersistentShapePacket::decode)
                .consumerMainThread(SpawnPersistentShapePacket::handle)
                .add();
        CHANNEL.messageBuilder(RemovePersistentShapePacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RemovePersistentShapePacket::encode)
                .decoder(RemovePersistentShapePacket::decode)
                .consumerMainThread(RemovePersistentShapePacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
