package net.tobias.awesome_commands.entity.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.tobias.awesome_commands.entity.animations.AwesomeHammersAnimations;
import net.tobias.awesome_commands.entity.entities.AwesomeHammers;

public class AwesomeHammersModel<T extends AwesomeHammers> extends HierarchicalModel<T> {
	private final ModelPart awesomeHammers;

	public AwesomeHammersModel(ModelPart root) {
		awesomeHammers = root.getChild("awesomeHammers");
		awesomeHammers.getChild("hammer1");
		awesomeHammers.getChild("hammer2");
		awesomeHammers.getChild("hammer3");
		awesomeHammers.getChild("hammer4");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition awesomeHammers = partdefinition.addOrReplaceChild("awesomeHammers", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition hammer1 = awesomeHammers.addOrReplaceChild("hammer1", CubeListBuilder.create().texOffs(62, 0).addBox(-1.0F, -17.0F, -1.0F, 3.0F, 21.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 18).addBox(-11.0F, -26.0F, -4.0F, 22.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(49.0F, -42.0F, 0.0F, 0.0F, 0.0F, -2.0944F));

		PartDefinition hammer2 = awesomeHammers.addOrReplaceChild("hammer2", CubeListBuilder.create().texOffs(62, 24).addBox(-1.0F, -17.0F, -1.0F, 3.0F, 21.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 36).addBox(-11.0F, -26.0F, -4.0F, 22.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-50.0F, -42.0F, 0.0F, -3.1198F, -0.0378F, -1.0476F));

		PartDefinition hammer3 = awesomeHammers.addOrReplaceChild("hammer3", CubeListBuilder.create().texOffs(62, 48).addBox(-1.0F, -17.0F, -1.0F, 3.0F, 21.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 54).addBox(-11.0F, -26.0F, -4.0F, 22.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -42.0F, -34.0F, -1.5194F, -0.7176F, -1.6478F));

		PartDefinition hammer4 = awesomeHammers.addOrReplaceChild("hammer4", CubeListBuilder.create().texOffs(0, 72).addBox(-1.0F, -17.0F, -1.0F, 3.0F, 21.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-11.0F, -26.0F, -4.0F, 22.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -44.2F, 38.7F, 1.6363F, 0.9387F, -1.5611F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		this.animate(entity.aweExplodeSlamExplodeAnimationState, AwesomeHammersAnimations.awe_explode_slam_explode, ageInTicks, 1.0F);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		awesomeHammers.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart root() {
		return awesomeHammers;
	}
}