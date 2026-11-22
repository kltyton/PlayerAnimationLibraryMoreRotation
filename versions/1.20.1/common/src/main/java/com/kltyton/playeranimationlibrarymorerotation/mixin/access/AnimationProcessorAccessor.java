package com.kltyton.playeranimationlibrarymorerotation.mixin.access;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes AnimationProcessor's underlying IAnimation root so sidecar bend/scale
 * channels can be resolved from active KeyframeAnimationPlayers.
 */
@Mixin(value = AnimationProcessor.class, remap = false)
public interface AnimationProcessorAccessor {
    @Accessor("animation")
    IAnimation palMore$getAnimation();

    @Accessor("tickDelta")
    float palMore$getTickDelta();
}
