package net.tobias.awesome_commands.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class CommandStunEffect extends MobEffect {
    public CommandStunEffect() {
        super(MobEffectCategory.HARMFUL, 16747520);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "57F1BADC-F545-4D89-B218-751C2FF8053D", 0.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, "3A2B2CDE-7B7B-4B52-BF2E-4D7B1D7E5D8B", 0.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, "3F4D7FB3-BF1D-47B0-B2E3-5CC35D5BEE7A", 0.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}