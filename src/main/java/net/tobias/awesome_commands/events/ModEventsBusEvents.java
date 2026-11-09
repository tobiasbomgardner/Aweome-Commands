package net.tobias.awesome_commands.events;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tobias.awesome_commands.AwesomeCommands;
import net.tobias.awesome_commands.entity.ModEntities;
import net.tobias.awesome_commands.entity.entities.AwesomeHammers;

@Mod.EventBusSubscriber(modid = AwesomeCommands.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventsBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.AWESOME_HAMMERS.get(), AwesomeHammers.createAttributes().build());
    }
}