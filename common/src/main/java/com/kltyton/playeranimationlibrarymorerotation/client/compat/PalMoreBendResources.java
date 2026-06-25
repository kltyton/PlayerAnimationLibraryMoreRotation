package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import com.kltyton.playeranimationlibrarymorerotation.util.PalMoreDebug;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.Axis;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.loading.PlayerAnimatorLoader;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import com.zigythebird.playeranimcore.util.JsonUtil;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.parser.ast.AccessExpression;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.parser.ast.FloatExpression;
import team.unnamed.mocha.parser.ast.IdentifierExpression;
import team.unnamed.mocha.runtime.IsConstantExpression;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.zigythebird.playeranimcore.molang.MolangLoader.MOCHA_ENGINE;

public final class PalMoreBendResources implements ResourceManagerReloadListener {
    public static final Identifier KEY = Playeranimationlibrarymorerotation.id("bend_vectors");
    private static volatile Map<Animation, Map<String, KeyframeStack>> bendTracks = Map.of();
    private static volatile Map<Animation, Map<String, BendPartTracks>> bendPartTracks = Map.of();
    private static volatile Map<Animation, Identifier> animationIds = Map.of();

    public static Map<String, KeyframeStack> getBendTracks(Animation animation) {
        return bendTracks.getOrDefault(animation, Map.of());
    }

    public static Map<String, BendPartTracks> getBendPartTracks(Animation animation) {
        return bendPartTracks.getOrDefault(animation, Map.of());
    }

    public static @Nullable Identifier getAnimationId(Animation animation) {
        return animationIds.get(animation);
    }

    public static int getLoadedAnimationCount() {
        return bendTracks.size();
    }

    public PalMoreBendResources() {
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager manager) {
        Map<Animation, Map<String, KeyframeStack>> loadedTracks = new IdentityHashMap<>();
        Map<Animation, Map<String, BendPartTracks>> loadedPartTracks = new IdentityHashMap<>();
        Map<Animation, Identifier> loadedIds = new IdentityHashMap<>();

        for (var resourceEntry : manager.listResources("player_animations", id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = resourceEntry.getKey();

            try (InputStream stream = resourceEntry.getValue().open();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = PlayerAnimLib.GSON.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("animations")) {
                    continue;
                }

                JsonObject animations = root.getAsJsonObject("animations");
                for (Map.Entry<String, JsonElement> animationEntry : animations.entrySet()) {
                    Identifier animationId = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), animationEntry.getKey());
                    Animation animation = PlayerAnimResources.getAnimation(animationId);
                    if (animation == null || !animationEntry.getValue().isJsonObject()) {
                        PalMoreDebug.infoOnce("missing-animation:" + animationId,
                                "resource {} animation {} was visible to PalMore, but PAL registry returned {}",
                                resourceId, animationId, animation);
                        continue;
                    }

                    loadedIds.put(animation, animationId);
                    JsonObject animationObject = animationEntry.getValue().getAsJsonObject();
                    Map<String, KeyframeStack> tracks = loadAnimationBendTracks(animationObject);
                    if (!tracks.isEmpty()) {
                        loadedTracks.put(animation, Map.copyOf(tracks));
                    }
                    Map<String, BendPartTracks> partTracks = loadAnimationBendPartTracks(animationObject);
                    if (!partTracks.isEmpty()) {
                        loadedPartTracks.put(animation, Map.copyOf(partTracks));
                        if (PalMoreDebug.shouldLog(animationId)) {
                            PalMoreDebug.info("loaded bend-part tracks id={} resource={} bones={}", animationId, resourceId, partTracks.keySet());
                            for (Map.Entry<String, BendPartTracks> debugEntry : partTracks.entrySet()) {
                                BendPartTracks debugTracks = debugEntry.getValue();
                                PalMoreDebug.info("  target={} bend={} position={} scale={}",
                                        debugEntry.getKey(),
                                        debugTracks.bend().hasKeyframes(),
                                        debugTracks.position().hasKeyframes(),
                                        debugTracks.scale().hasKeyframes());
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                PlayerAnimLib.LOGGER.error("PlayerAnimationLibraryMoreRotation failed to load vector bend data from {} because:", resourceId, exception);
            }
        }

        bendTracks = Collections.unmodifiableMap(loadedTracks);
        bendPartTracks = Collections.unmodifiableMap(loadedPartTracks);
        animationIds = Collections.unmodifiableMap(loadedIds);
        PalMoreDebug.info("reload complete: vectorBendAnimations={} bendPartAnimations={} knownAnimations={}",
                bendTracks.size(), bendPartTracks.size(), animationIds.size());
    }

    private static Map<String, KeyframeStack> loadAnimationBendTracks(JsonObject animationObj) {
        JsonObject bonesObj = JsonUtil.getAsJsonObject(animationObj, "bones", new JsonObject());
        Map<String, KeyframeStack> tracks = new HashMap<>(bonesObj.size());

        for (Map.Entry<String, JsonElement> entry : bonesObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject boneObj = entry.getValue().getAsJsonObject();
            if (!boneObj.has("bend")) {
                continue;
            }

            String boneName = UniversalAnimLoader.getCorrectPlayerBoneName(entry.getKey());
            if (!isSupportedBendTransformBone(boneName)) {
                continue;
            }

            JsonElement bendRotation = getBendRotationElement(boneObj.get("bend"));
            if (bendRotation == null) {
                continue;
            }

            KeyframeStack stack = buildKeyframeStack(getKeyframes(bendRotation, TransformType.BEND), TransformType.BEND);
            if (stack.hasKeyframes()) {
                tracks.put(boneName, stack);
            }
        }

        return tracks;
    }

    private static Map<String, BendPartTracks> loadAnimationBendPartTracks(JsonObject animationObj) {
        JsonObject bonesObj = JsonUtil.getAsJsonObject(animationObj, "bones", new JsonObject());
        Map<String, BendPartTracks> tracks = new HashMap<>(bonesObj.size());

        for (Map.Entry<String, JsonElement> entry : bonesObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            String boneName = UniversalAnimLoader.getCorrectPlayerBoneName(entry.getKey());
            if (!isSupportedBendTransformBone(boneName)) {
                continue;
            }

            JsonObject boneObj = entry.getValue().getAsJsonObject();
            JsonElement bendElement = boneObj.get("bend");
            boolean compoundBend = isCompoundBendElement(bendElement);
            if (!compoundBend) {
                continue;
            }

            JsonElement bendRotation = getBendRotationElement(bendElement);
            KeyframeStack bend = bendRotation != null
                    ? buildKeyframeStack(getKeyframes(bendRotation, TransformType.BEND), TransformType.BEND)
                    : new KeyframeStack();
            JsonElement positionElement = getBendTransformElement(bendElement, "position");
            JsonElement scaleElement = getBendTransformElement(bendElement, "scale");
            KeyframeStack position = positionElement != null
                    ? buildKeyframeStack(getKeyframes(positionElement, TransformType.POSITION), TransformType.POSITION)
                    : new KeyframeStack();
            KeyframeStack scale = scaleElement != null
                    ? buildKeyframeStack(getKeyframes(scaleElement, TransformType.SCALE), TransformType.SCALE)
                    : new KeyframeStack();

            if (bend.hasKeyframes() || position.hasKeyframes() || scale.hasKeyframes()) {
                tracks.put(boneName, new BendPartTracks(bend, position, scale));
            }
        }

        return tracks;
    }

    private static boolean isSupportedBendTransformBone(String boneName) {
        return switch (boneName) {
            case "torso", "right_arm", "left_arm", "right_leg", "left_leg" -> true;
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

    private static @Nullable JsonElement getBendRotationElement(@Nullable JsonElement bendElement) {
        if (bendElement == null) {
            return null;
        }

        if (isCompoundBendElement(bendElement)) {
            JsonObject bendObj = bendElement.getAsJsonObject();
            return bendObj.has("rotation") ? bendObj.get("rotation") : null;
        }

        return bendElement;
    }

    private static @Nullable JsonElement getBendTransformElement(@Nullable JsonElement bendElement, String memberName) {
        if (isCompoundBendElement(bendElement)) {
            JsonObject bendObj = bendElement.getAsJsonObject();
            if (bendObj.has(memberName)) {
                return bendObj.get(memberName);
            }
        }

        return null;
    }

    private static List<FloatObjectPair<JsonElement>> getKeyframes(JsonElement element, TransformType type) {
        if (element == null) {
            return List.of();
        }

        if (element.isJsonPrimitive()) {
            element = scalarVector(element, type, false);
        }

        if (element.isJsonArray()) {
            return ObjectArrayList.of(FloatObjectPair.of(0, element.getAsJsonArray()));
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("vector")) {
                return ObjectArrayList.of(FloatObjectPair.of(0, obj));
            }

            if (obj.has("value")) {
                obj.add("vector", scalarVector(obj.get("value"), type, true));
                return ObjectArrayList.of(FloatObjectPair.of(0, obj));
            }

            List<FloatObjectPair<JsonElement>> list = new ObjectArrayList<>();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                float timestamp = readTimestamp(entry.getKey());

                if (timestamp == 0 && !list.isEmpty()) {
                    throw new JsonParseException("Invalid keyframe data - multiple starting keyframes? " + entry.getKey());
                }

                JsonElement entryValue = entry.getValue();
                if (entryValue.isJsonPrimitive()) {
                    list.add(FloatObjectPair.of(timestamp, scalarVector(entryValue, type, false)));
                    continue;
                }

                if (entryValue.isJsonObject()) {
                    JsonObject entryObj = entryValue.getAsJsonObject();
                    if (entryObj.has("value")) {
                        entryObj.add("vector", scalarVector(entryObj.get("value"), type, true));
                        list.add(FloatObjectPair.of(timestamp, entryObj));
                        continue;
                    } else if (!entryObj.has("vector")) {
                        addBedrockKeyframes(timestamp, entryObj, list, type);
                        continue;
                    }
                }

                list.add(FloatObjectPair.of(timestamp, entryValue));
            }

            sortKeyframes(list);
            return list;
        }

        throw new JsonParseException("Invalid object type provided to vector bend keyframes, got: " + element);
    }

    private static JsonArray extractBedrockKeyframe(JsonElement keyframe, TransformType type) {
        if (keyframe.isJsonArray()) {
            return keyframe.getAsJsonArray();
        }

        if (keyframe.isJsonPrimitive()) {
            return scalarVector(keyframe, type, false);
        }

        if (!keyframe.isJsonObject()) {
            throw new JsonParseException("Invalid keyframe data - expected array or object, found " + keyframe);
        }

        JsonObject keyframeObj = keyframe.getAsJsonObject();
        if (keyframeObj.has("vector")) {
            return keyframeObj.get("vector").getAsJsonArray();
        }
        if (keyframeObj.has("pre")) {
            return keyframeObj.get("pre").getAsJsonArray();
        }
        return keyframeObj.get("post").getAsJsonArray();
    }

    private static JsonArray scalarVector(JsonElement value, TransformType type, boolean keyedValue) {
        JsonArray array = new JsonArray(3);
        array.add(value);
        if (type == TransformType.BEND || keyedValue) {
            array.add(0);
            array.add(0);
        } else {
            array.add(value);
            array.add(value);
        }
        return array;
    }

    private static void addBedrockKeyframes(float timestamp, JsonObject keyframe, List<FloatObjectPair<JsonElement>> keyframes, TransformType type) {
        boolean addedFrame = false;

        if (keyframe.has("pre")) {
            addedFrame = true;
            JsonArray value = extractBedrockKeyframe(keyframe.get("pre"), type);
            JsonObject result = null;
            if (keyframe.has("easing")) {
                result = new JsonObject();
                result.add("vector", value);
                result.add("easing", keyframe.get("easing"));
                if (keyframe.has("easingArgs")) {
                    result.add("easingArgs", keyframe.get("easingArgs"));
                }
            }

            keyframes.add(FloatObjectPair.of(timestamp == 0 ? timestamp : timestamp - 0.001F, result != null ? result : value));
        }

        if (keyframe.has("post")) {
            JsonArray values = extractBedrockKeyframe(keyframe.get("post"), type);

            if (keyframe.has("lerp_mode")) {
                JsonObject keyframeObj = new JsonObject();
                keyframeObj.add("vector", values);
                keyframeObj.add("easing", keyframe.get("lerp_mode"));
                keyframes.add(FloatObjectPair.of(timestamp, keyframeObj));
            } else {
                keyframes.add(FloatObjectPair.of(timestamp, values));
            }

            return;
        }

        if (!addedFrame) {
            throw new JsonParseException("Invalid keyframe data - expected array, found " + keyframe);
        }
    }

    private static KeyframeStack buildKeyframeStack(List<FloatObjectPair<JsonElement>> entries, TransformType type) {
        if (entries.isEmpty()) {
            return new KeyframeStack();
        }

        List<Keyframe> xFrames = new ObjectArrayList<>();
        List<Keyframe> yFrames = new ObjectArrayList<>();
        List<Keyframe> zFrames = new ObjectArrayList<>();

        List<Expression> xPrev = null;
        List<Expression> yPrev = null;
        List<Expression> zPrev = null;

        float prevTimeX = 0;
        float prevTimeY = 0;
        float prevTimeZ = 0;

        for (FloatObjectPair<JsonElement> entry : entries) {
            JsonElement element = entry.right();
            float curTime = entry.leftFloat();

            boolean isForRotation = type == TransformType.ROTATION || type == TransformType.BEND;
            Expression defaultValue = type == TransformType.SCALE ? FloatExpression.ONE : FloatExpression.ZERO;

            JsonArray keyFrameVector = element.isJsonArray() ? element.getAsJsonArray() : JsonUtil.getAsJsonArray(element.getAsJsonObject(), "vector");
            List<Expression> xValue = MolangLoader.parseJson(isForRotation, keyFrameVector.get(0), defaultValue);
            List<Expression> yValue = MolangLoader.parseJson(isForRotation, keyFrameVector.get(1), defaultValue);
            List<Expression> zValue = MolangLoader.parseJson(isForRotation, keyFrameVector.get(2), defaultValue);

            JsonObject entryObj = element.isJsonObject() ? element.getAsJsonObject() : null;
            EasingType easingType = getEasingForAxis(entryObj, null, EasingType.LINEAR);
            List<List<Expression>> easingArgs = getEasingArgsForAxis(entryObj, null, new ObjectArrayList<>());

            if (isEnabled(xValue)) {
                xFrames.add(new Keyframe((curTime - prevTimeX) * 20, xPrev == null ? xValue : xPrev, xValue,
                        getEasingForAxis(entryObj, Axis.X, easingType), getEasingArgsForAxis(entryObj, Axis.X, easingArgs)));
                xPrev = xValue;
                prevTimeX = curTime;
            }
            if (isEnabled(yValue)) {
                yFrames.add(new Keyframe((curTime - prevTimeY) * 20, yPrev == null ? yValue : yPrev, yValue,
                        getEasingForAxis(entryObj, Axis.Y, easingType), getEasingArgsForAxis(entryObj, Axis.Y, easingArgs)));
                yPrev = yValue;
                prevTimeY = curTime;
            }
            if (isEnabled(zValue)) {
                zFrames.add(new Keyframe((curTime - prevTimeZ) * 20, zPrev == null ? zValue : zPrev, zValue,
                        getEasingForAxis(entryObj, Axis.Z, easingType), getEasingArgsForAxis(entryObj, Axis.Z, easingArgs)));
                zPrev = zValue;
                prevTimeZ = curTime;
            }
        }

        return new KeyframeStack(addArgsForKeyframes(xFrames, type), addArgsForKeyframes(yFrames, type), addArgsForKeyframes(zFrames, type));
    }

    private static EasingType getEasingForAxis(JsonObject entryObj, Axis axis, EasingType easingType) {
        String memberName = "easing";
        if (axis != null) {
            memberName += axis.name();
        }
        return entryObj != null && entryObj.has(memberName) ? EasingType.fromJson(entryObj.get(memberName)) : easingType;
    }

    private static List<List<Expression>> getEasingArgsForAxis(JsonObject entryObj, Axis axis, List<List<Expression>> easingArg) {
        String memberName = "easingArgs";
        if (axis != null) {
            memberName += axis.name();
        }
        return entryObj != null && entryObj.has(memberName)
                ? JsonUtil.jsonArrayToList(JsonUtil.getAsJsonArray(entryObj, memberName), ele -> Collections.singletonList(FloatExpression.of(ele.getAsFloat())))
                : easingArg;
    }

    private static List<Keyframe> addArgsForKeyframes(List<Keyframe> frames, TransformType type) {
        if (frames.isEmpty()) {
            return frames;
        }

        if (frames.size() == 1) {
            Keyframe frame = frames.getFirst();
            if (frame.easingType() != EasingType.LINEAR) {
                frames.set(0, new Keyframe(frame.length(), frame.startValue(), frame.endValue()));
                return frames;
            }
        }

        for (int i = 0; i < frames.size(); i++) {
            Keyframe frame = frames.get(i);
            if (frame.easingType() == EasingType.CATMULLROM) {
                frames.set(i, new Keyframe(frame.length(), frame.startValue(), frame.endValue(), frame.easingType(), ObjectArrayList.of(
                        i == 0 ? frame.startValue() : frames.get(i - 1).endValue(),
                        i + 1 >= frames.size() ? frame.endValue() : frames.get(i + 1).endValue()
                )));
            } else if (frame.easingType() == EasingType.BEZIER) {
                List<Expression> leftValue = frame.easingArgs().getFirst();
                List<Expression> rightValue = frame.easingArgs().get(2);
                List<Expression> rightTime = frame.easingArgs().get(3);
                if (type == TransformType.ROTATION || type == TransformType.BEND) {
                    rightValue = toRadiansForBezier(rightValue);
                    leftValue = toRadiansForBezier(leftValue);
                }

                frames.set(i, new Keyframe(frame.length(), frame.startValue(), frame.endValue(), frame.easingType(),
                        ObjectArrayList.of(leftValue, frame.easingArgs().get(1))));
                if (frame.easingArgs().size() > 4) {
                    frames.get(i).easingArgs().add(frame.easingArgs().get(4));
                    frames.get(i).easingArgs().add(frame.easingArgs().get(5));
                }
                if (frames.size() > i + 1) {
                    Keyframe nextKeyframe = frames.get(i + 1);
                    if (nextKeyframe.easingType() != EasingType.BEZIER) {
                        frames.set(i + 1, new Keyframe(nextKeyframe.length(), nextKeyframe.startValue(), nextKeyframe.endValue(),
                                EasingType.BEZIER, ObjectArrayList.of(PlayerAnimatorLoader.ZERO, PlayerAnimatorLoader.ZERO, rightValue, rightTime)));
                    } else {
                        nextKeyframe.easingArgs().add(rightValue);
                        nextKeyframe.easingArgs().add(rightTime);
                    }
                }
            }
        }

        return frames;
    }

    private static boolean isEnabled(List<Expression> expressions) {
        if (expressions.size() == 1
                && expressions.getFirst() instanceof AccessExpression access
                && access.object() instanceof IdentifierExpression id
                && "pal".equals(id.name())) {
            return !"disabled".equals(access.property()) && !"skip".equals(access.property());
        }

        return true;
    }

    private static List<Expression> toRadiansForBezier(List<Expression> expressions) {
        if (expressions.size() == 1 && IsConstantExpression.test(expressions.getFirst())) {
            return Collections.singletonList(FloatExpression.of(Math.toRadians(MOCHA_ENGINE.eval(expressions))));
        }

        PlayerAnimLib.LOGGER.warn("Invalid easing arguments for vector bend bezier: {}", expressions);
        return expressions;
    }

    private static float readTimestamp(String timestamp) {
        try {
            return Float.parseFloat(timestamp);
        } catch (Throwable throwable) {
            return 0;
        }
    }

    private static void sortKeyframes(List<FloatObjectPair<JsonElement>> keyframes) {
        if (keyframes.size() > 1) {
            keyframes.sort(Comparator.comparingDouble(FloatObjectPair::leftFloat));
        }
    }

    public record BendPartTracks(KeyframeStack bend, KeyframeStack position, KeyframeStack scale) {
    }

}
