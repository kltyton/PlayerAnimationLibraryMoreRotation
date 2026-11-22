package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreAnimationResolver;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies sidecar whole-body ("body") scale tracks right after playerAnimator's
 * PlayerRendererMixin applies body position and rotation in setupRotations.
 */
@Mixin(value = PlayerRenderer.class, priority = 2500)
public abstract class PlayerRendererScaleMixin {
    @Inject(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
            at = @At("RETURN")
    )
    private void palMore$applyBodyScale(
            AbstractClientPlayer player,
            PoseStack poseStack,
            float ageInTicks,
            float bodyYaw,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!(player instanceof IAnimatedPlayer animated)) {
            return;
        }
        AnimationApplier applier = animated.playerAnimator_getAnimation();
        if (applier == null || !applier.isActive()) {
            return;
        }

        Vector3f scale = PalMoreAnimationResolver.resolveScale(applier, "body");
        if (scale.x != 1.0F || scale.y != 1.0F || scale.z != 1.0F) {
            poseStack.scale(scale.x, scale.y, scale.z);
        }
    }
}
