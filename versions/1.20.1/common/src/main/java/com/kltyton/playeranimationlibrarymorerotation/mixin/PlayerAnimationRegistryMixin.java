package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs immediately after playerAnimator finishes reloading {@code player_animation/*.json}
 * so MoreRotation also registers {@code player_animations/*.json} and extracts sidecar
 * bend/scale/effect tracks on both Fabric and Forge.
 */
@Mixin(value = PlayerAnimationRegistry.class, remap = false)
public abstract class PlayerAnimationRegistryMixin {
    @Inject(method = "resourceLoaderCallback", at = @At("RETURN"), remap = false)
    private static void palMore$loadExtendedAnimations(ResourceManager manager, Logger logger, CallbackInfo ci) {
        PalMoreBendResources.onAfterPlayerAnimatorReload(manager, logger);
    }
}
