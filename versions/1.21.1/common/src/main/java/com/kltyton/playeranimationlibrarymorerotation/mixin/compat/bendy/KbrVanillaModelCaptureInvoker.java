package com.kltyton.playeranimationlibrarymorerotation.mixin.compat.bendy;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Links the optional KBR public capture API without introducing a mandatory runtime dependency. */
@Pseudo
@Mixin(targets = "com.kltyton.bedrockruntime.client.render.vanilla.VanillaModelCapture", remap = false)
public interface KbrVanillaModelCaptureInvoker {
    @Invoker("record")
    static void palMore$record(PoseStack.Pose pose, String path, List<ModelPart.Cube> cubes) {
        throw new AssertionError("KBR capture invoker was not transformed");
    }
}
