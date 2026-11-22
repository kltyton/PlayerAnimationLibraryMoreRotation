package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.GeckoLibSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Sorts timestamped Bedrock/GeckoLib rotation and position keyframes in ascending timestamp
 * order before playerAnimator's GeckoLibSerializer iterates them.
 */
@Mixin(value = GeckoLibSerializer.class, remap = false)
public abstract class GeckoLibSerializerKeyframeSortMixin {
    @Inject(method = "readBone", at = @At("HEAD"), remap = false)
    private static void palMore$sortBoneKeyframes(
            KeyframeAnimation.StateCollection part,
            JsonObject boneObj,
            KeyframeAnimation.AnimationBuilder builder,
            CallbackInfo ci
    ) {
        palMore$sortChannelIfObject(boneObj, "rotation");
        palMore$sortChannelIfObject(boneObj, "position");
    }

    @Unique
    private static void palMore$sortChannelIfObject(JsonObject boneObj, String memberName) {
        if (!boneObj.has(memberName)) {
            return;
        }
        JsonElement channel = boneObj.get(memberName);
        if (channel == null || !channel.isJsonObject()) {
            return;
        }
        JsonObject channelObj = channel.getAsJsonObject();
        if (channelObj.size() <= 1 || channelObj.has("vector")) {
            return;
        }

        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(channelObj.entrySet());
        boolean hasNumericKeys = false;
        for (Map.Entry<String, JsonElement> entry : entries) {
            if (palMore$tryParseTimestamp(entry.getKey()) != null) {
                hasNumericKeys = true;
                break;
            }
        }
        if (!hasNumericKeys) {
            return;
        }

        entries.sort(Comparator.comparingDouble(entry -> {
            Float parsed = palMore$tryParseTimestamp(entry.getKey());
            return parsed != null ? parsed : Double.NEGATIVE_INFINITY;
        }));

        JsonObject sorted = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : entries) {
            sorted.add(entry.getKey(), entry.getValue());
        }
        boneObj.add(memberName, sorted);
    }

    @Unique
    private static Float palMore$tryParseTimestamp(String key) {
        try {
            float value = Float.parseFloat(key);
            return Float.isFinite(value) ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
