package com.kltyton.playeranimationlibrarymorerotation.mixin.compat.bendy;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Links the optional KBR held-item callback used by its ModelPart.compile capture hook. */
@Pseudo
@Mixin(targets = "com.kltyton.bedrockruntime.client.render.item.HeldItemCapture", remap = false)
public interface KbrHeldItemCaptureInvoker {
    @Invoker("modelPart")
    static void palMore$modelPart(PoseStack.Pose pose, List<ModelPart.Cube> cubes) {
        throw new AssertionError("KBR held-item invoker was not transformed");
    }
}
