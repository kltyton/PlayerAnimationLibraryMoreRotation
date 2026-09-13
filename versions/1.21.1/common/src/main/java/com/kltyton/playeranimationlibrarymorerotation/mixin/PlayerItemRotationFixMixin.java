package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zigythebird.playeranim.accessors.IAnimatedPlayer;
import com.zigythebird.playeranim.animation.PlayerAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
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
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void palMore$fixItemRotation(
            LivingEntity renderState,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!(renderState instanceof IAnimatedPlayer state)) {
            return;
        }

        PlayerAnimManager manager = state.playerAnimLib$getAnimManager();
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
