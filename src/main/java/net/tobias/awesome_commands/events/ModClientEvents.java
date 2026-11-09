package net.tobias.awesome_commands.events;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.tobias.awesome_commands.AwesomeCommands;
import net.tobias.awesome_commands.client.particles.ClientPersistentShapeManager;
import net.tobias.awesome_commands.effects.ModEffects;

@Mod.EventBusSubscriber(modid = AwesomeCommands.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {
    private static final String COMMAND_STUN_LOCK_YAW_KEY = "awesome_commands_command_stun_lock_yaw";
    private static final String COMMAND_STUN_LOCK_PITCH_KEY = "awesome_commands_command_stun_lock_pitch";

    @SubscribeEvent
    public static void applyCommandStunFovModifier(ViewportEvent.ComputeFov event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(ModEffects.COMMAND_STUN.get())) {
            event.setFOV(event.getFOV() * 0.85D);
        }
    }

    @SubscribeEvent
    public static void lockCommandStunCamera(ViewportEvent.ComputeCameraAngles event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ModEffects.COMMAND_STUN.get())) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(COMMAND_STUN_LOCK_YAW_KEY) || !persistentData.contains(COMMAND_STUN_LOCK_PITCH_KEY)) {
            return;
        }

        event.setYaw(persistentData.getFloat(COMMAND_STUN_LOCK_YAW_KEY));
        event.setPitch(persistentData.getFloat(COMMAND_STUN_LOCK_PITCH_KEY));
    }

    @SubscribeEvent
    public static void renderPersistentShapes(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        ClientPersistentShapeManager.render(event);
    }

    @SubscribeEvent
    public static void clearPersistentShapesOnWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientPersistentShapeManager.clear();
        }
    }

    @SubscribeEvent
    public static void clearPersistentShapesOnDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        ClientPersistentShapeManager.clear();
    }
}
