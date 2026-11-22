package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreTrack.ChannelKind;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreTrack.VectorTrack;
import com.kltyton.playeranimationlibrarymorerotation.mixin.access.PlayerAnimationRegistryAccessor;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.core.data.gson.GeckoLibSerializer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads animations from PAL's {@code player_animations/} folder into playerAnimator's registry
 * and parses sidecar bend/scale/effect tracks from both {@code player_animation/} and
 * {@code player_animations/}.
 */
public final class PalMoreBendResources {
    private static final Gson GSON = new Gson();
    private static final String[] ANIMATION_FOLDERS = {"player_animation", "player_animations"};

    private static volatile Map<KeyframeAnimation, PalMoreAnimationExtras> extrasByAnimation = Map.of();

    private PalMoreBendResources() {
    }

    public static PalMoreAnimationExtras getExtras(@Nullable KeyframeAnimation animation) {
        if (animation == null) {
            return PalMoreAnimationExtras.EMPTY;
        }
        return extrasByAnimation.getOrDefault(animation, PalMoreAnimationExtras.EMPTY);
    }

    public static int getLoadedExtrasCount() {
        return extrasByAnimation.size();
    }

    public static void onAfterPlayerAnimatorReload(ResourceManager manager, @Nullable Logger logger) {
        Logger log = logger != null ? logger : Playeranimationlibrarymorerotation.LOGGER;
        loadPluralFolderAnimations(manager, log);

        Map<KeyframeAnimation, PalMoreAnimationExtras> loaded = new IdentityHashMap<>();
        for (String folder : ANIMATION_FOLDERS) {
            scanSidecarFolder(manager, folder, loaded, log);
        }

        extrasByAnimation = Collections.unmodifiableMap(loaded);
    }

    private static void loadPluralFolderAnimations(ResourceManager manager, Logger log) {
        Map<ResourceLocation, KeyframeAnimation> registryMap = PlayerAnimationRegistryAccessor.palMore$getAnimations();
        Map<ResourceLocation, Resource> resources = manager.listResources(
                "player_animations",
                id -> id.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            try (InputStream stream = entry.getValue().open()) {
                List<KeyframeAnimation> animations = AnimationSerializing.deserializeAnimation(stream);
                for (KeyframeAnimation animation : animations) {
                    Object rawName = animation.extraData.get("name");
                    if (!(rawName instanceof String nameStr)) {
                        continue;
                    }
                    String normalized = PlayerAnimationRegistry.serializeTextToString(nameStr).toLowerCase(Locale.ROOT);
                    ResourceLocation id = new ResourceLocation(resourceId.getNamespace(), normalized);
                    registryMap.putIfAbsent(id, animation);
                }
            } catch (Exception exception) {
                log.error("PlayerAnimationLibraryMoreRotation failed to load animation from {}: {}", resourceId, exception.getMessage(), exception);
            }
        }
    }

    private static void scanSidecarFolder(
            ResourceManager manager,
            String folder,
            Map<KeyframeAnimation, PalMoreAnimationExtras> out,
            Logger log
    ) {
        Map<ResourceLocation, Resource> resources = manager.listResources(
                folder,
                id -> id.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            try (InputStream stream = entry.getValue().open();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (parsed == null || !parsed.isJsonObject()) {
                    continue;
                }
                JsonObject root = parsed.getAsJsonObject();
                if (!root.has("animations") || !root.get("animations").isJsonObject()) {
                    continue;
                }

                JsonObject animations = root.getAsJsonObject("animations");
                for (Map.Entry<String, JsonElement> animationEntry : animations.entrySet()) {
                    if (!animationEntry.getValue().isJsonObject()) {
                        continue;
                    }

                    String key = PlayerAnimationRegistry.serializeTextToString(animationEntry.getKey()).toLowerCase(Locale.ROOT);
                    ResourceLocation animationId = new ResourceLocation(resourceId.getNamespace(), key);
                    KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(animationId);
                    if (animation == null || out.containsKey(animation)) {
                        continue;
                    }

                    PalMoreAnimationExtras extras = parseAnimationExtras(animationEntry.getValue().getAsJsonObject());
                    if (!extras.isEmpty()) {
                        out.put(animation, extras);
                    }
                }
            } catch (Exception exception) {
                log.error("PlayerAnimationLibraryMoreRotation failed to load sidecar tracks from {}: {}", resourceId, exception.getMessage(), exception);
            }
        }
    }

    private static PalMoreAnimationExtras parseAnimationExtras(JsonObject animationObj) {
        Map<String, VectorTrack> bendRotations = new HashMap<>();
        Map<String, VectorTrack> bendPositions = new HashMap<>();
        Map<String, VectorTrack> bendScales = new HashMap<>();
        Map<String, VectorTrack> boneScales = new HashMap<>();

        if (animationObj.has("bones") && animationObj.get("bones").isJsonObject()) {
            JsonObject bonesObj = animationObj.getAsJsonObject("bones");
            for (Map.Entry<String, JsonElement> entry : bonesObj.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                String boneName = normalizeBoneName(entry.getKey());
                JsonObject boneObj = entry.getValue().getAsJsonObject();

                if (boneObj.has("bend") && isSupportedBendBone(boneName)) {
                    JsonElement bendElement = boneObj.get("bend");
                    if (isCompoundBendElement(bendElement)) {
                        JsonObject bendObj = bendElement.getAsJsonObject();
                        if (bendObj.has("rotation")) {
                            VectorTrack rot = PalMoreTrack.parse(bendObj.get("rotation"), ChannelKind.BEND);
                            if (rot.hasKeyframes()) {
                                bendRotations.put(boneName, rot);
                            }
                        }
                        if (bendObj.has("position")) {
                            VectorTrack pos = PalMoreTrack.parse(bendObj.get("position"), ChannelKind.POSITION);
                            if (pos.hasKeyframes()) {
                                bendPositions.put(boneName, pos);
                            }
                        }
                        if (bendObj.has("scale")) {
                            VectorTrack scale = PalMoreTrack.parse(bendObj.get("scale"), ChannelKind.SCALE);
                            if (scale.hasKeyframes()) {
                                bendScales.put(boneName, scale);
                            }
                        }
                    } else {
                        VectorTrack rot = PalMoreTrack.parse(bendElement, ChannelKind.BEND);
                        if (rot.hasKeyframes()) {
                            bendRotations.put(boneName, rot);
                        }
                    }
                }

                if (boneObj.has("scale") && isSupportedScaleBone(boneName)) {
                    VectorTrack scale = PalMoreTrack.parse(boneObj.get("scale"), ChannelKind.SCALE);
                    if (scale.hasKeyframes()) {
                        boneScales.put(boneName, scale);
                    }
                }
            }
        }

        PalMoreEffectTimeline timeline = PalMoreEffectTimeline.parse(animationObj);
        return new PalMoreAnimationExtras(
                Map.copyOf(bendRotations),
                Map.copyOf(bendPositions),
                Map.copyOf(bendScales),
                Map.copyOf(boneScales),
                timeline
        );
    }

    public static String normalizeBoneName(String rawName) {
        String camel = GeckoLibSerializer.snake2Camel(rawName);
        return switch (camel) {
            case "rightArm", "RightArm" -> "rightArm";
            case "leftArm", "LeftArm" -> "leftArm";
            case "rightLeg", "RightLeg" -> "rightLeg";
            case "leftLeg", "LeftLeg" -> "leftLeg";
            case "rightItem", "RightItem" -> "rightItem";
            case "leftItem", "LeftItem" -> "leftItem";
            case "torso", "Torso" -> "torso";
            case "body", "Body" -> "body";
            case "head", "Head" -> "head";
            default -> camel;
        };
    }

    private static boolean isSupportedBendBone(String boneName) {
        return switch (boneName) {
            case "torso", "rightArm", "leftArm", "rightLeg", "leftLeg" -> true;
            default -> false;
        };
    }

    private static boolean isSupportedScaleBone(String boneName) {
        return switch (boneName) {
            case "body", "head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg", "rightItem", "leftItem" -> true;
            default -> false;
        };
    }

    private static boolean isCompoundBendElement(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return false;
        }
        JsonObject obj = element.getAsJsonObject();
        return obj.has("rotation") || obj.has("position") || obj.has("scale");
    }
}
