package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.google.gson.JsonElement;
import com.zigythebird.playeranimcore.loading.AnimationLoader;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

/**
 * Sorts PAL JSON timeline keyframes by timestamp after AnimationLoader parses
 * them, keeping exporter output that writes timestamps out of order predictable.
 */
@Mixin(AnimationLoader.class)
public class AnimationLoaderKeyframeSortMixin {
    @Inject(method = "getKeyframes", at = @At("RETURN"), cancellable = true)
    private static void palMore$sortKeyframes(JsonElement element, CallbackInfoReturnable<List<FloatObjectPair<JsonElement>>> cir) {
        List<FloatObjectPair<JsonElement>> keyframes = cir.getReturnValue();
        if (keyframes.size() > 1) {
            keyframes.sort(Comparator.comparingDouble(FloatObjectPair::leftFloat));
        }
    }
}
