package net.tobias.awesome_commands.commands;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tobias.awesome_commands.AwesomeCommands;

@Mod.EventBusSubscriber(modid = AwesomeCommands.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        AwesomeExplodeCmd.register(event.getDispatcher());
        AwesomeMeteorShowerCmd.register(event.getDispatcher());
    }
}