package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zigythebird.playeranimcore.loading.AnimationLoader;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

/**
 * Keeps PAL's loader compatible with this addon's extended bend JSON and sorts
 * PAL JSON timeline keyframes by timestamp after AnimationLoader parses them.
 */
@Mixin(AnimationLoader.class)
public class AnimationLoaderKeyframeSortMixin {
    @ModifyArg(
            method = "bakeBoneAnimations",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/zigythebird/playeranimcore/loading/AnimationLoader;getKeyframes(Lcom/google/gson/JsonElement;)Ljava/util/List;",
                    ordinal = 3
            ),
            index = 0
    )
    private static JsonElement palMore$unwrapCompoundBendForPal(JsonElement element) {
        if (!palMore$isCompoundBendElement(element)) {
            return element;
        }

        JsonObject bendObj = element.getAsJsonObject();
        return bendObj.has("rotation") ? bendObj.get("rotation") : null;
    }

    @Inject(method = "getKeyframes", at = @At("RETURN"), cancellable = true)
    private static void palMore$sortKeyframes(JsonElement element, CallbackInfoReturnable<List<FloatObjectPair<JsonElement>>> cir) {
        List<FloatObjectPair<JsonElement>> keyframes = cir.getReturnValue();
        if (keyframes.size() > 1) {
            keyframes.sort(Comparator.comparingDouble(FloatObjectPair::leftFloat));
        }
    }

    @Unique
    private static boolean palMore$isCompoundBendElement(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return false;
        }

        JsonObject obj = element.getAsJsonObject();
        return obj.has("rotation") || obj.has("position") || obj.has("scale");
    }
}
