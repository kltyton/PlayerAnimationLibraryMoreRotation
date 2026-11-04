package com.kltyton.playeranimationlibrarymorerotation.mixin.compat.bendy;

import com.bawnorton.mixinsquared.TargetHandler;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.bendy.LegacyBendyRendering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves KBR's compile-boundary observation when bendy-lib draws cubes directly, retaining the draw chain. */
@Mixin(value = ModelPart.class, priority = 2500)
public abstract class LegacyBendyModelPartMixin {
    @Shadow @Final private List<ModelPart.Cube> cubes;

    @TargetHandler(mixin = "io.github.kosmx.bendylib.mixin.IModelPartMixin",
            name = "redirectRenderCuboids", prefix = "wrapOperation")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), remap = false)
    private void palMore$preserveCapture(ModelPart part, PoseStack.Pose pose, VertexConsumer output,
            int light, int overlay, int color, Operation<Void> original, CallbackInfo callback) {
        LegacyBendyRendering.captureBypassedModelPart(part, pose, cubes);
    }
}
