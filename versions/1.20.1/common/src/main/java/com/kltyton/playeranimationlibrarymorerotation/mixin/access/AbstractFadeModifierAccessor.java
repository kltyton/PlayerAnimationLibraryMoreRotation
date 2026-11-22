package com.kltyton.playeranimationlibrarymorerotation.mixin.access;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes AbstractFadeModifier's beginAnimation and getAlpha so sidecar bend/scale
 * channels blend smoothly across playerAnimator fade transitions.
 */
@Mixin(value = AbstractFadeModifier.class, remap = false)
public interface AbstractFadeModifierAccessor {
    @Accessor("beginAnimation")
    IAnimation palMore$getBeginAnimation();

    @Invoker("calculateProgress")
    float palMore$invokeCalculateProgress(float tickDelta);

    @Invoker("getAlpha")
    float palMore$invokeGetAlpha(String modelName, TransformType type, float progress);
}
