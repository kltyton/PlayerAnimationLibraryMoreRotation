package com.kltyton.playeranimationlibrarymorerotation.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Function;

/** Exposes the legacy Bendable Cuboids vertex transform so extended bends use its original mesh positions. */
@Mixin(value = ModelPart.Cube.class, priority = 2500)
public interface CubePositionInvokerMixin {
    @Invoker(value = "bc$iteratePositions", remap = false)
    void palMore$iteratePositions(Function<Vector3f, Vector3f> transform);
}
