package net.tobias.awesome_commands.entity.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import net.tobias.awesome_commands.AwesomeCommands;
import net.tobias.awesome_commands.entity.client.ModModelLayers;
import net.tobias.awesome_commands.entity.client.models.AwesomeHammersModel;
import net.tobias.awesome_commands.entity.entities.AwesomeHammers;

public class AwesomeHammersRenderer extends MobRenderer<AwesomeHammers, AwesomeHammersModel<AwesomeHammers>> {
    public AwesomeHammersRenderer(EntityRendererProvider.Context context) {
        super(context, new AwesomeHammersModel<>(context.bakeLayer(ModModelLayers.AWESOME_HAMMERS_LAYER)), 0f);
    }

    public void render(AwesomeHammers entity, float entityYaw, float partialTicks, PoseStack matrixStack,
                       MultiBufferSource buffer, int packedLight) {
        //matrixStack.scale(0.5f, 0.5f, 0.5f);

        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AwesomeHammers entity) {
        return new ResourceLocation(AwesomeCommands.MOD_ID, "textures/entity/awesome_hammers.png");
    }

    @Override
    protected @Nullable RenderType getRenderType(AwesomeHammers entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.energySwirl(this.getTextureLocation(entity), 0.0F, 0.0F);
    }
}