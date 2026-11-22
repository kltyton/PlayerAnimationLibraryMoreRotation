package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreAnimationResolver;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendyCuboids;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendState;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.Helper;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends playerAnimator's HeldItemMixin (priority 1000) so held items inherit
 * sidecar 3-axis arm bend plus bend.position/bend.scale before vanilla item
 * orientation, and apply leftItem/rightItem scale after HeldItemMixin's item transforms.
 */
@Mixin(value = ItemInHandLayer.class, priority = 2500)
public abstract class ItemInHandLayerExtendedAnimMixin {
    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    ordinal = 0
            )
    )
    private void palMore$applyItemBendVector(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!Helper.isBendEnabled() || !(entity instanceof IAnimatedPlayer animated)) {
            return;
        }
        AnimationApplier applier = animated.playerAnimator_getAnimation();
        if (applier == null || !applier.isActive()) {
            return;
        }

        String armBone = arm == HumanoidArm.LEFT ? "leftArm" : "rightArm";
        PalMoreBendState state = PalMoreAnimationResolver.resolveBend(applier, armBone);
        if (!state.hasActiveBend()) {
            return;
        }

        Vec3f nativeBend = applier.get3DTransform(armBone, TransformType.BEND, new Vec3f(0.0F, 0.0F, 0.0F));
        PalMoreBendyCuboids.applyArmItemBend(
                poseStack,
                state,
                nativeBend.getX().floatValue(),
                nativeBend.getY().floatValue()
        );
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void palMore$applyItemScale(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!(entity instanceof IAnimatedPlayer animated)) {
            return;
        }
        AnimationApplier applier = animated.playerAnimator_getAnimation();
        if (applier == null || !applier.isActive()) {
            return;
        }

        String itemBone = arm == HumanoidArm.LEFT ? "leftItem" : "rightItem";
        Vector3f scale = PalMoreAnimationResolver.resolveScale(applier, itemBone);
        if (scale.x != 1.0F || scale.y != 1.0F || scale.z != 1.0F) {
            poseStack.scale(scale.x, scale.y, scale.z);
        }
    }
}
