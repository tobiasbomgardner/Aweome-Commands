package net.tobias.awesome_commands.events;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tobias.awesome_commands.AwesomeCommands;
import net.tobias.awesome_commands.entity.client.ModModelLayers;
import net.tobias.awesome_commands.entity.client.models.AwesomeHammersModel;

@Mod.EventBusSubscriber(modid = AwesomeCommands.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.AWESOME_HAMMERS_LAYER, AwesomeHammersModel::createBodyLayer);
    }
}