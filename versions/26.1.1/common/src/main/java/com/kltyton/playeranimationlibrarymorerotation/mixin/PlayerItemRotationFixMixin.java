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
import net.minecraft.world.item.ItemStack;
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
            ItemStack itemStack,
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

        poseStack.scale(1.0F / bone.scale.x, 1.0F / bone.scale.y, 1.0F / bone.scale.z);
        palMore$undoPalSwappedRotations(poseStack, bone);
        palMore$applyCorrectRotations(poseStack, bone);
        poseStack.scale(bone.scale.x, bone.scale.y, bone.scale.z);
    }

    @Unique
    private static boolean palMore$hasInvertibleScale(PlayerAnimBone bone) {
        return Math.abs(bone.scale.x) > MIN_INVERTIBLE_SCALE
                && Math.abs(bone.scale.y) > MIN_INVERTIBLE_SCALE
                && Math.abs(bone.scale.z) > MIN_INVERTIBLE_SCALE;
    }

    @Unique
    private static void palMore$undoPalSwappedRotations(PoseStack poseStack, PlayerAnimBone bone) {
        if (bone.rotation.x != 0.0F) {
            poseStack.mulPose(Axis.XP.rotation(bone.rotation.x));
        }
        if (bone.rotation.y != 0.0F) {
            poseStack.mulPose(Axis.YP.rotation(bone.rotation.z));
        }
        if (bone.rotation.z != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(bone.rotation.y));
        }
    }

    @Unique
    private static void palMore$applyCorrectRotations(PoseStack poseStack, PlayerAnimBone bone) {
        if (bone.rotation.z != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(-bone.rotation.z));
        }
        if (bone.rotation.y != 0.0F) {
            poseStack.mulPose(Axis.YP.rotation(-bone.rotation.y));
        }
        if (bone.rotation.x != 0.0F) {
            poseStack.mulPose(Axis.XP.rotation(-bone.rotation.x));
        }
    }
}
