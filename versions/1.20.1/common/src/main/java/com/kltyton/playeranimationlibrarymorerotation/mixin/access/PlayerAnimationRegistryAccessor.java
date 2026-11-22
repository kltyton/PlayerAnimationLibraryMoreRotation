package com.kltyton.playeranimationlibrarymorerotation.mixin.access;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashMap;

/**
 * Exposes PlayerAnimationRegistry's backing animation map so animations stored in
 * PAL's {@code player_animations/} folder can be registered alongside {@code player_animation/}.
 */
@Mixin(value = PlayerAnimationRegistry.class, remap = false)
public interface PlayerAnimationRegistryAccessor {
    @Accessor("animations")
    static HashMap<ResourceLocation, KeyframeAnimation> palMore$getAnimations() {
        throw new AssertionError();
    }
}
