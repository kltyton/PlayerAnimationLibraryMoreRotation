package com.kltyton.playeranimationlibrarymorerotation.client.compat.bendy;

import com.kltyton.playeranimationlibrarymorerotation.mixin.compat.bendy.KbrHeldItemCaptureInvoker;
import com.kltyton.playeranimationlibrarymorerotation.mixin.compat.bendy.KbrVanillaModelCaptureInvoker;
import com.kltyton.playeranimationlibrarymorerotation.mixin.compat.bendy.LegacyModelPartAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.MutableCuboid;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;

public final class LegacyBendyRendering {
    private LegacyBendyRendering() {
    }

    public static void captureBypassedModelPart(ModelPart part, PoseStack.Pose pose, List<ModelPart.Cube> cubes) {
        LegacyModelPartAccess access = (LegacyModelPartAccess) (Object) part;
        if (access.palMore$getLegacyWorkaround() != ModelPartAccessor.Workaround.VanillaDraw
                || !access.palMore$hasMutatedCuboid()
                || cubes.size() == 1 && ((MutableCuboid) cubes.getFirst()).getActiveMutator() == null) {
            return;
        }

        KbrVanillaModelCaptureInvoker.palMore$record(pose, access.palMore$getCapturePath(), cubes);
        KbrHeldItemCaptureInvoker.palMore$modelPart(pose, cubes);
    }
}
