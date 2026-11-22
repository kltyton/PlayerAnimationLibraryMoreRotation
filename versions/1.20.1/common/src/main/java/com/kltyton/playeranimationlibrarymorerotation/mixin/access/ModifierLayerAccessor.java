package com.kltyton.playeranimationlibrarymorerotation.mixin.access;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Exposes ModifierLayer's modifier chain so sidecar resolution honors MirrorModifier
 * and AbstractFadeModifier wrappers.
 */
@Mixin(value = ModifierLayer.class, remap = false)
public interface ModifierLayerAccessor {
    @Accessor("modifiers")
    List<AbstractModifier> palMore$getModifiers();
}
