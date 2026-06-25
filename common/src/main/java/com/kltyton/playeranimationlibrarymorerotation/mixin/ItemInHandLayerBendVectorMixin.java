package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendableCuboids;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends bendable-cuboids' held-item X-bend compatibility so arm-held items
 * also inherit PALMore bend Y/Z rotation plus bend-only position/scale at the
 * same matrix point, before the vanilla item orientation and item-local PAL
 * transforms are applied.
 */
@Mixin(value = ItemInHandLayer.class, priority = 2500)
public class ItemInHandLayerBendVectorMixin {
    @Unique
    private final PlayerAnimBone palMore$rightArm = new PlayerAnimBone("right_arm");
    @Unique
    private final PlayerAnimBone palMore$leftArm = new PlayerAnimBone("left_arm");

    @TargetHandler(
            mixin = "com.zigythebird.bendable_cuboids.mixin.playeranim.ItemInHandLayerMixin_playerAnim",
            name = "renderMixin",
            prefix = "handler"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), remap = false)
    private void palMore$applyParentArmScale(
            ArmedEntityRenderState renderState,
            ItemStackRenderState itemStackRenderState,
            ItemStack itemStack,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo originalCi,
            CallbackInfo ci
    ) {
        PlayerAnimBone armBone = palMore$getActiveArmBone(renderState, arm);
        if (armBone == null) {
            return;
        }

        if (armBone.scale.x != 1.0F || armBone.scale.y != 1.0F || armBone.scale.z != 1.0F) {
            poseStack.scale(armBone.scale.x, armBone.scale.y, armBone.scale.z);
        }
    }

    @TargetHandler(
            mixin = "com.zigythebird.bendable_cuboids.mixin.playeranim.ItemInHandLayerMixin_playerAnim",
            name = "renderMixin",
            prefix = "handler"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("RETURN"), remap = false)
    private void palMore$applyItemBendVector(
            ArmedEntityRenderState renderState,
            ItemStackRenderState itemStackRenderState,
            ItemStack itemStack,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo originalCi,
            CallbackInfo ci
    ) {
        PlayerAnimBone armBone = palMore$getActiveArmBone(renderState, arm);
        if (armBone instanceof PalMoreBendHolder holder) {
            PalMoreBendableCuboids.applyArmItemBend(poseStack, holder);
        }
    }

    @Unique
    private PlayerAnimBone palMore$getActiveArmBone(ArmedEntityRenderState renderState, HumanoidArm arm) {
        if (!(renderState instanceof IAvatarAnimationState state)) {
            return null;
        }

        AvatarAnimManager manager = state.playerAnimLib$getAnimManager();
        if (manager == null || !manager.isActive()) {
            return null;
        }

        PlayerAnimBone armBone = arm == HumanoidArm.LEFT ? palMore$leftArm : palMore$rightArm;
        armBone.setToInitialPose();
        manager.get3DTransform(armBone);
        return armBone;
    }
}
