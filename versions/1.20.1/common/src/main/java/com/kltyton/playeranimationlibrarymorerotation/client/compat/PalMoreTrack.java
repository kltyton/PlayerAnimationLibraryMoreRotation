package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Easing;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Timestamp-sorted keyframe track evaluator for Bedrock/PAL JSON bend and scale channels.
 */
public final class PalMoreTrack {
    private static final float TICKS_PER_SECOND = 20.0F;

    private PalMoreTrack() {
    }

    public enum ChannelKind {
        BEND(true, 0.0F),
        POSITION(false, 0.0F),
        SCALE(false, 1.0F);

        public final boolean isAngle;
        public final float defaultValue;

        ChannelKind(boolean isAngle, float defaultValue) {
            this.isAngle = isAngle;
            this.defaultValue = defaultValue;
        }
    }

    public enum InterpMode {
        EASE,
        CATMULLROM,
        STEP
    }

    public record ScalarKeyframe(float tick, float value, InterpMode mode, Ease ease) {
    }

    public record ScalarTrack(List<ScalarKeyframe> frames) {
        public static final ScalarTrack EMPTY = new ScalarTrack(List.of());

        public boolean isEmpty() {
            return this.frames.isEmpty();
        }

        public float sample(float tick, float defaultValue) {
            if (this.frames.isEmpty()) {
                return defaultValue;
            }
            if (this.frames.size() == 1) {
                return this.frames.get(0).value();
            }

            ScalarKeyframe first = this.frames.get(0);
            if (tick <= first.tick()) {
                return first.value();
            }

            int lastIndex = this.frames.size() - 1;
            ScalarKeyframe last = this.frames.get(lastIndex);
            if (tick >= last.tick()) {
                return last.value();
            }

            int segIndex = 0;
            for (int i = 0; i < lastIndex; i++) {
                if (this.frames.get(i + 1).tick() > tick) {
                    segIndex = i;
                    break;
                }
            }

            ScalarKeyframe prev = this.frames.get(segIndex);
            ScalarKeyframe next = this.frames.get(segIndex + 1);
            float span = next.tick() - prev.tick();
            if (span <= 1.0E-5F) {
                return next.value();
            }

            float rawT = (tick - prev.tick()) / span;
            if (rawT <= 0.0F) {
                return prev.value();
            }
            if (rawT >= 1.0F) {
                return next.value();
            }

            InterpMode mode = prev.mode() == InterpMode.CATMULLROM || next.mode() == InterpMode.CATMULLROM
                    ? InterpMode.CATMULLROM
                    : next.mode();

            if (mode == InterpMode.STEP) {
                return prev.value();
            }
            if (mode == InterpMode.CATMULLROM) {
                float p0 = segIndex > 0 ? this.frames.get(segIndex - 1).value() : prev.value();
                float p1 = prev.value();
                float p2 = next.value();
                float p3 = segIndex + 2 <= lastIndex ? this.frames.get(segIndex + 2).value() : next.value();
                return catmullRom(p0, p1, p2, p3, rawT);
            }

            float eased = next.ease().invoke(rawT);
            return prev.value() + (next.value() - prev.value()) * eased;
        }
    }

    public record VectorTrack(ScalarTrack x, ScalarTrack y, ScalarTrack z) {
        public static final VectorTrack EMPTY = new VectorTrack(ScalarTrack.EMPTY, ScalarTrack.EMPTY, ScalarTrack.EMPTY);

        public boolean hasKeyframes() {
            return !this.x.isEmpty() || !this.y.isEmpty() || !this.z.isEmpty();
        }

        public Vector3f sample(float tick, float defaultValue) {
            return new Vector3f(
                    this.x.sample(tick, defaultValue),
                    this.y.sample(tick, defaultValue),
                    this.z.sample(tick, defaultValue)
            );
        }
    }

    private record TimedEntry(float seconds, int order, JsonElement element) {
    }

    public static VectorTrack parse(@Nullable JsonElement element, ChannelKind kind) {
        if (element == null || element.isJsonNull()) {
            return VectorTrack.EMPTY;
        }

        List<TimedEntry> entries = collectTimedEntries(element, kind);
        if (entries.isEmpty()) {
            return VectorTrack.EMPTY;
        }

        entries.sort(Comparator.comparingDouble(TimedEntry::seconds).thenComparingInt(TimedEntry::order));

        List<ScalarKeyframe> xFrames = new ArrayList<>(entries.size());
        List<ScalarKeyframe> yFrames = new ArrayList<>(entries.size());
        List<ScalarKeyframe> zFrames = new ArrayList<>(entries.size());

        for (TimedEntry entry : entries) {
            float tick = entry.seconds() * TICKS_PER_SECOND;
            JsonElement value = entry.element();

            JsonArray vector;
            JsonObject obj = null;
            if (value.isJsonArray()) {
                vector = value.getAsJsonArray();
            } else if (value.isJsonObject()) {
                obj = value.getAsJsonObject();
                vector = extractVectorFromObject(obj, kind);
            } else if (value.isJsonPrimitive()) {
                vector = scalarVector(value, kind, false);
            } else {
                continue;
            }

            if (vector == null || vector.isEmpty()) {
                continue;
            }

            InterpMode defaultMode = parseInterpMode(obj, null, InterpMode.EASE);
            Ease defaultEase = parseEase(obj, null, Ease.LINEAR);

            Float vx = parseAxisValue(vector, 0, kind);
            Float vy = parseAxisValue(vector, 1, kind);
            Float vz = parseAxisValue(vector, 2, kind);

            if (vx != null) {
                xFrames.add(new ScalarKeyframe(tick, vx, parseInterpMode(obj, "X", defaultMode), parseEase(obj, "X", defaultEase)));
            }
            if (vy != null) {
                yFrames.add(new ScalarKeyframe(tick, vy, parseInterpMode(obj, "Y", defaultMode), parseEase(obj, "Y", defaultEase)));
            }
            if (vz != null) {
                zFrames.add(new ScalarKeyframe(tick, vz, parseInterpMode(obj, "Z", defaultMode), parseEase(obj, "Z", defaultEase)));
            }
        }

        return new VectorTrack(
                xFrames.isEmpty() ? ScalarTrack.EMPTY : new ScalarTrack(List.copyOf(xFrames)),
                yFrames.isEmpty() ? ScalarTrack.EMPTY : new ScalarTrack(List.copyOf(yFrames)),
                zFrames.isEmpty() ? ScalarTrack.EMPTY : new ScalarTrack(List.copyOf(zFrames))
        );
    }

    private static List<TimedEntry> collectTimedEntries(JsonElement element, ChannelKind kind) {
        if (element.isJsonPrimitive()) {
            return List.of(new TimedEntry(0.0F, 0, scalarVector(element, kind, false)));
        }

        if (element.isJsonArray()) {
            return List.of(new TimedEntry(0.0F, 0, element.getAsJsonArray()));
        }

        if (!element.isJsonObject()) {
            throw new JsonParseException("Invalid keyframe container: " + element);
        }

        JsonObject obj = element.getAsJsonObject();
        if (obj.has("vector") || obj.has("value")) {
            return List.of(new TimedEntry(0.0F, 0, normalizeSingleObject(obj, kind)));
        }

        List<TimedEntry> result = new ArrayList<>();
        int seq = 0;
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            Float timestamp = tryParseFloat(entry.getKey());
            if (timestamp == null) {
                continue;
            }

            JsonElement entryValue = entry.getValue();
            if (entryValue.isJsonPrimitive()) {
                result.add(new TimedEntry(timestamp, seq++, scalarVector(entryValue, kind, false)));
            } else if (entryValue.isJsonArray()) {
                result.add(new TimedEntry(timestamp, seq++, entryValue.getAsJsonArray()));
            } else if (entryValue.isJsonObject()) {
                JsonObject entryObj = entryValue.getAsJsonObject();
                if (entryObj.has("vector") || entryObj.has("value")) {
                    result.add(new TimedEntry(timestamp, seq++, normalizeSingleObject(entryObj, kind)));
                } else {
                    seq = addBedrockEntries(timestamp, seq, entryObj, result, kind);
                }
            }
        }

        return result;
    }

    private static JsonObject normalizeSingleObject(JsonObject src, ChannelKind kind) {
        if (src.has("vector")) {
            return src;
        }
        JsonObject copy = src.deepCopy();
        if (copy.has("value")) {
            copy.add("vector", scalarVector(copy.get("value"), kind, true));
        }
        return copy;
    }

    private static int addBedrockEntries(
            float timestamp,
            int seq,
            JsonObject keyframe,
            List<TimedEntry> out,
            ChannelKind kind
    ) {
        if (keyframe.has("pre")) {
            JsonArray preVec = extractVectorElement(keyframe.get("pre"), kind);
            if (preVec != null) {
                JsonObject preObj = new JsonObject();
                preObj.add("vector", preVec);
                if (keyframe.has("easing")) {
                    preObj.add("easing", keyframe.get("easing"));
                }
                if (keyframe.has("lerp_mode")) {
                    preObj.add("lerp_mode", keyframe.get("lerp_mode"));
                }
                float preTime = timestamp == 0.0F ? 0.0F : timestamp - 0.001F;
                out.add(new TimedEntry(preTime, seq++, preObj));
            }
        }

        if (keyframe.has("post")) {
            JsonArray postVec = extractVectorElement(keyframe.get("post"), kind);
            if (postVec != null) {
                JsonObject postObj = new JsonObject();
                postObj.add("vector", postVec);
                if (keyframe.has("lerp_mode")) {
                    postObj.add("lerp_mode", keyframe.get("lerp_mode"));
                }
                if (keyframe.has("easing")) {
                    postObj.add("easing", keyframe.get("easing"));
                }
                out.add(new TimedEntry(timestamp, seq++, postObj));
            }
        }

        return seq;
    }

    private static @Nullable JsonArray extractVectorFromObject(JsonObject obj, ChannelKind kind) {
        if (obj.has("vector")) {
            return extractVectorElement(obj.get("vector"), kind);
        }
        if (obj.has("value")) {
            return scalarVector(obj.get("value"), kind, true);
        }
        if (obj.has("post")) {
            return extractVectorElement(obj.get("post"), kind);
        }
        if (obj.has("pre")) {
            return extractVectorElement(obj.get("pre"), kind);
        }
        return null;
    }

    private static @Nullable JsonArray extractVectorElement(@Nullable JsonElement element, ChannelKind kind) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        if (element.isJsonPrimitive()) {
            return scalarVector(element, kind, false);
        }
        if (element.isJsonObject()) {
            return extractVectorFromObject(element.getAsJsonObject(), kind);
        }
        return null;
    }

    private static JsonArray scalarVector(JsonElement value, ChannelKind kind, boolean keyedValue) {
        JsonArray array = new JsonArray(3);
        array.add(value);
        if (kind == ChannelKind.BEND || keyedValue) {
            array.add(0);
            array.add(0);
        } else {
            array.add(value);
            array.add(value);
        }
        return array;
    }

    private static @Nullable Float parseAxisValue(JsonArray vector, int index, ChannelKind kind) {
        if (index >= vector.size()) {
            return kind.defaultValue;
        }
        JsonElement element = vector.get(index);
        if (element == null || element.isJsonNull()) {
            return kind.defaultValue;
        }

        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                float val = element.getAsFloat();
                return kind.isAngle ? (float) Math.toRadians(val) : val;
            }
            if (element.getAsJsonPrimitive().isString()) {
                String raw = element.getAsString().trim();
                if ("pal.disabled".equalsIgnoreCase(raw) || "pal.skip".equalsIgnoreCase(raw)) {
                    return null;
                }
                Float parsed = tryParseFloat(raw);
                if (parsed != null) {
                    return kind.isAngle ? (float) Math.toRadians(parsed) : parsed;
                }
            }
        }
        return kind.defaultValue;
    }

    private static InterpMode parseInterpMode(@Nullable JsonObject obj, @Nullable String axisSuffix, InterpMode fallback) {
        if (obj == null) {
            return fallback;
        }

        String easingName = getStringMember(obj, axisSuffix != null ? "easing" + axisSuffix : "easing");
        if (easingName == null && axisSuffix == null) {
            easingName = getStringMember(obj, "lerp_mode");
        }
        if (easingName == null) {
            return fallback;
        }

        String normalized = easingName.trim().toLowerCase(Locale.ROOT);
        if ("catmullrom".equals(normalized) || "catmull_rom".equals(normalized)) {
            return InterpMode.CATMULLROM;
        }
        if ("step".equals(normalized) || "constant".equals(normalized)) {
            return InterpMode.STEP;
        }
        return InterpMode.EASE;
    }

    private static Ease parseEase(@Nullable JsonObject obj, @Nullable String axisSuffix, Ease fallback) {
        if (obj == null) {
            return fallback;
        }

        String easingName = getStringMember(obj, axisSuffix != null ? "easing" + axisSuffix : "easing");
        if (easingName == null && axisSuffix == null) {
            easingName = getStringMember(obj, "lerp_mode");
        }
        if (easingName == null) {
            return fallback;
        }

        String normalized = easingName.trim().toLowerCase(Locale.ROOT);
        if ("linear".equals(normalized) || "catmullrom".equals(normalized) || "catmull_rom".equals(normalized)) {
            return Ease.LINEAR;
        }
        if ("step".equals(normalized) || "constant".equals(normalized)) {
            return Ease.CONSTANT;
        }

        Ease parsed = Easing.easeFromString(easingName);
        return parsed != null ? parsed : fallback;
    }

    private static @Nullable String getStringMember(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    private static @Nullable Float tryParseFloat(String raw) {
        try {
            float parsed = Float.parseFloat(raw);
            return Float.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * (
                (2.0F * p1)
                        + (-p0 + p2) * t
                        + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
                        + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3
        );
    }
}
