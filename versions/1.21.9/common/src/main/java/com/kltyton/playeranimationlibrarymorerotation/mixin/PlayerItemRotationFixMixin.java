package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reapplies left_item/right_item Y/Z rotations at PAL's original item render
 * injection point without depending on PAL's mixin-added handler locals.
 */
@Mixin(value = ItemInHandLayer.class, priority = 500)
public class PlayerItemRotationFixMixin {
    private static final float MIN_INVERTIBLE_SCALE = 1.0e-6F;

    @Unique
    private final PlayerAnimBone palMore$rightItem = new PlayerAnimBone("right_item");
    @Unique
    private final PlayerAnimBone palMore$leftItem = new PlayerAnimBone("left_item");
    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void palMore$fixItemRotation(
            ArmedEntityRenderState renderState,
            ItemStackRenderState itemStackRenderState,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!(renderState instanceof IAvatarAnimationState state)) {
            return;
        }

        AvatarAnimManager manager = state.playerAnimLib$getAnimManager();
        if (manager == null || !manager.isActive()) {
            return;
        }

        PlayerAnimBone bone = arm == HumanoidArm.LEFT ? palMore$leftItem : palMore$rightItem;
        bone.setToInitialPose();
        manager.get3DTransform(bone);

        if (!palMore$hasInvertibleScale(bone)) {
            return;
        }

        poseStack.scale(1.0F / bone.getScaleX(), 1.0F / bone.getScaleY(), 1.0F / bone.getScaleZ());
        palMore$undoPalSwappedRotations(poseStack, bone);
        palMore$applyCorrectRotations(poseStack, bone);
        poseStack.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    @Unique
    private static boolean palMore$hasInvertibleScale(PlayerAnimBone bone) {
        return Math.abs(bone.getScaleX()) > MIN_INVERTIBLE_SCALE
                && Math.abs(bone.getScaleY()) > MIN_INVERTIBLE_SCALE
                && Math.abs(bone.getScaleZ()) > MIN_INVERTIBLE_SCALE;
    }

    @Unique
    private static void palMore$undoPalSwappedRotations(PoseStack poseStack, PlayerAnimBone bone) {
        if (bone.getRotX() != 0.0F) {
            poseStack.mulPose(Axis.XP.rotation(bone.getRotX()));
        }
        if (bone.getRotY() != 0.0F) {
            poseStack.mulPose(Axis.YP.rotation(bone.getRotZ()));
        }
        if (bone.getRotZ() != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(bone.getRotY()));
        }
    }

    @Unique
    private static void palMore$applyCorrectRotations(PoseStack poseStack, PlayerAnimBone bone) {
        if (bone.getRotZ() != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(-bone.getRotZ()));
        }
        if (bone.getRotY() != 0.0F) {
            poseStack.mulPose(Axis.YP.rotation(-bone.getRotY()));
        }
        if (bone.getRotX() != 0.0F) {
            poseStack.mulPose(Axis.XP.rotation(-bone.getRotX()));
        }
    }
}
