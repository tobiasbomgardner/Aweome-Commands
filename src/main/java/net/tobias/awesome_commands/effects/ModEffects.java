package net.tobias.awesome_commands.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tobias.awesome_commands.AwesomeCommands;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS;
    public static final RegistryObject<MobEffect> COMMAND_STUN;

    static {
        EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AwesomeCommands.MOD_ID);
        COMMAND_STUN = EFFECTS.register("command_stun", CommandStunEffect::new);
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}