package com.kltyton.playeranimationlibrarymorerotation.mixin.access;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes KeyframeAnimationPlayer's current partial tickDelta so sidecar tracks
 * sample at the exact same float tick as playerAnimator's native channels.
 */
@Mixin(value = KeyframeAnimationPlayer.class, remap = false)
public interface KeyframeAnimationPlayerAccessor {
    @Accessor("tickDelta")
    float palMore$getTickDelta();
}
