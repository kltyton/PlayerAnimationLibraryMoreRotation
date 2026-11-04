package com.kltyton.playeranimationlibrarymorerotation.mixin.compat.bendy;

import io.github.kosmx.bendylib.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the legacy draw branch and KBR part path needed to preserve a bypassed capture callback. */
@Mixin(value = ModelPart.class, priority = 2500)
public interface LegacyModelPartAccess {
    @Accessor(value = "hasMutatedCuboid", remap = false)
    boolean palMore$hasMutatedCuboid();

    @Accessor(value = "workaround", remap = false)
    ModelPartAccessor.Workaround palMore$getLegacyWorkaround();

    @Accessor(value = "kltyton_bedrock_runtime$path", remap = false)
    String palMore$getCapturePath();
}
