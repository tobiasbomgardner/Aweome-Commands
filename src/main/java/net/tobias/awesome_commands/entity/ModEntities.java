package net.tobias.awesome_commands.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tobias.awesome_commands.AwesomeCommands;
import net.tobias.awesome_commands.entity.entities.AwesomeHammers;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AwesomeCommands.MOD_ID);

    public static final RegistryObject<EntityType<AwesomeHammers>> AWESOME_HAMMERS =
            ENTITY_TYPES.register("awesome_hammers", () -> EntityType.Builder.of(AwesomeHammers::new, MobCategory.MISC)
                    .build("awesome_hammers"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}