package com.kltyton.playeranimationlibrarymorerotation.mixin.compat.bendy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zigythebird.bendable_cuboids.impl.BendableCuboid;
import io.github.kosmx.bendylib.MutableCuboid;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores bendy-lib's native cube dispatch when a Bendable Cuboids subclass has an active mutator. */
@Mixin(BendableCuboid.class)
public abstract class LegacyBendyCubeMixin extends ModelPart.Cube {
    protected LegacyBendyCubeMixin(int u, int v, float x, float y, float z,
            float width, float height, float depth, float growX, float growY, float growZ,
            boolean mirror, float textureWidth, float textureHeight, Set<Direction> faces) {
        super(u, v, x, y, z, width, height, depth, growX, growY, growZ,
                mirror, textureWidth, textureHeight, faces);
    }

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void palMore$dispatchLegacyMutator(PoseStack.Pose pose, VertexConsumer output,
            int light, int overlay, int color, CallbackInfo callback) {
        if (((MutableCuboid) (Object) this).getActiveMutator() != null) {
            super.compile(pose, output, light, overlay, color);
            callback.cancel();
        }
    }
}
