package com.kltyton.playeranimationlibrarymorerotation.mixin.access;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

/**
 * Exposes AnimationStack's ordered layer list so sidecar bend/scale channels
 * respect playerAnimator layer priority.
 */
@Mixin(value = AnimationStack.class, remap = false)
public interface AnimationStackAccessor {
    @Accessor("layers")
    ArrayList<Pair<Integer, IAnimation>> palMore$getLayers();
}
