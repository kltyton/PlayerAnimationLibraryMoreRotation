package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
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

    public static Map<String, KeyframeStack> getBendTracks(Animation animation) {
        return bendTracks.getOrDefault(animation, Map.of());
    }

    public static int getLoadedAnimationCount() {
        return bendTracks.size();
    }

    public PalMoreBendResources() {
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager manager) {
        Map<Animation, Map<String, KeyframeStack>> loadedTracks = new IdentityHashMap<>();

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
                        continue;
                    }

                    Map<String, KeyframeStack> tracks = loadAnimationBendTracks(animationEntry.getValue().getAsJsonObject());
                    if (!tracks.isEmpty()) {
                        loadedTracks.put(animation, Map.copyOf(tracks));
                    }
                }
            } catch (Exception exception) {
                PlayerAnimLib.LOGGER.error("PlayerAnimationLibraryMoreRotation failed to load vector bend data from {} because:", resourceId, exception);
            }
        }

        bendTracks = Collections.unmodifiableMap(loadedTracks);
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

            KeyframeStack stack = buildKeyframeStack(getKeyframes(boneObj.get("bend")), TransformType.BEND);
            if (stack.hasKeyframes()) {
                tracks.put(UniversalAnimLoader.getCorrectPlayerBoneName(entry.getKey()), stack);
            }
        }

        return tracks;
    }

    private static List<FloatObjectPair<JsonElement>> getKeyframes(JsonElement element) {
        if (element == null) {
            return List.of();
        }

        if (element.isJsonPrimitive()) {
            element = scalarBendVector(element);
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
                JsonArray array = new JsonArray(3);
                array.add(obj.get("value"));
                array.add(0);
                array.add(0);
                obj.add("vector", array);
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
                    list.add(FloatObjectPair.of(timestamp, scalarBendVector(entryValue)));
                    continue;
                }

                if (entryValue.isJsonObject()) {
                    JsonObject entryObj = entryValue.getAsJsonObject();
                    if (entryObj.has("value")) {
                        JsonArray array = new JsonArray(3);
                        array.add(entryObj.get("value"));
                        array.add(0);
                        array.add(0);
                        entryObj.add("vector", array);
                        list.add(FloatObjectPair.of(timestamp, entryObj));
                        continue;
                    } else if (!entryObj.has("vector")) {
                        addBedrockKeyframes(timestamp, entryObj, list);
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

    private static JsonArray extractBedrockKeyframe(JsonElement keyframe) {
        if (keyframe.isJsonArray()) {
            return keyframe.getAsJsonArray();
        }

        if (keyframe.isJsonPrimitive()) {
            return scalarBendVector(keyframe);
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

    private static JsonArray scalarBendVector(JsonElement value) {
        JsonArray array = new JsonArray(3);
        array.add(value);
        array.add(0);
        array.add(0);
        return array;
    }

    private static void addBedrockKeyframes(float timestamp, JsonObject keyframe, List<FloatObjectPair<JsonElement>> keyframes) {
        boolean addedFrame = false;

        if (keyframe.has("pre")) {
            addedFrame = true;
            JsonArray value = extractBedrockKeyframe(keyframe.get("pre"));
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
            JsonArray values = extractBedrockKeyframe(keyframe.get("post"));

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

}
