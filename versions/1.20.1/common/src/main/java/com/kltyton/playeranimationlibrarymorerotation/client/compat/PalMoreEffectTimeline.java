package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.CustomInstructionKeyframeData;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.ParticleKeyframeData;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.SoundKeyframeData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Sorted Bedrock/PAL effect keyframe timelines (timeline instructions, sound_effects, particle_effects).
 */
public record PalMoreEffectTimeline(
        List<CustomInstructionKeyframeData> instructions,
        List<SoundKeyframeData> sounds,
        List<ParticleKeyframeData> particles
) {
    private static final float TICKS_PER_SECOND = 20.0F;

    public static final PalMoreEffectTimeline EMPTY = new PalMoreEffectTimeline(List.of(), List.of(), List.of());

    public boolean isEmpty() {
        return this.instructions.isEmpty() && this.sounds.isEmpty() && this.particles.isEmpty();
    }

    public static PalMoreEffectTimeline parse(JsonObject animationObj) {
        List<CustomInstructionKeyframeData> instructions = parseInstructions(animationObj.get("timeline"));
        List<SoundKeyframeData> sounds = parseSounds(animationObj.get("sound_effects"));
        List<ParticleKeyframeData> particles = parseParticles(animationObj.get("particle_effects"));
        if (instructions.isEmpty() && sounds.isEmpty() && particles.isEmpty()) {
            return EMPTY;
        }
        return new PalMoreEffectTimeline(
                List.copyOf(instructions),
                List.copyOf(sounds),
                List.copyOf(particles)
        );
    }

    private static List<CustomInstructionKeyframeData> parseInstructions(JsonElement timelineEl) {
        if (timelineEl == null || !timelineEl.isJsonObject()) {
            return List.of();
        }
        List<CustomInstructionKeyframeData> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : timelineEl.getAsJsonObject().entrySet()) {
            Float sec = tryParseFloat(entry.getKey());
            if (sec == null) {
                continue;
            }
            float tick = sec * TICKS_PER_SECOND;
            JsonElement val = entry.getValue();
            if (val.isJsonPrimitive()) {
                result.add(new CustomInstructionKeyframeData(tick, val.getAsString()));
            } else if (val.isJsonArray()) {
                StringBuilder joined = new StringBuilder();
                for (JsonElement item : val.getAsJsonArray()) {
                    if (item.isJsonPrimitive()) {
                        if (!joined.isEmpty()) {
                            joined.append('\n');
                        }
                        joined.append(item.getAsString());
                    }
                }
                if (!joined.isEmpty()) {
                    result.add(new CustomInstructionKeyframeData(tick, joined.toString()));
                }
            }
        }
        result.sort(Comparator.comparingDouble(CustomInstructionKeyframeData::startTick));
        return result;
    }

    private static List<SoundKeyframeData> parseSounds(JsonElement soundsEl) {
        if (soundsEl == null || !soundsEl.isJsonObject()) {
            return List.of();
        }
        List<SoundKeyframeData> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : soundsEl.getAsJsonObject().entrySet()) {
            Float sec = tryParseFloat(entry.getKey());
            if (sec == null) {
                continue;
            }
            float tick = sec * TICKS_PER_SECOND;
            addSoundEntries(tick, entry.getValue(), result);
        }
        result.sort(Comparator.comparingDouble(SoundKeyframeData::startTick));
        return result;
    }

    private static void addSoundEntries(float tick, JsonElement value, List<SoundKeyframeData> out) {
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (value.isJsonPrimitive()) {
            out.add(new SoundKeyframeData(tick, value.getAsString()));
        } else if (value.isJsonObject()) {
            JsonObject obj = value.getAsJsonObject();
            String effect = getString(obj, "effect");
            if (!effect.isEmpty()) {
                out.add(new SoundKeyframeData(tick, effect));
            }
        } else if (value.isJsonArray()) {
            JsonArray arr = value.getAsJsonArray();
            for (JsonElement item : arr) {
                addSoundEntries(tick, item, out);
            }
        }
    }

    private static List<ParticleKeyframeData> parseParticles(JsonElement particlesEl) {
        if (particlesEl == null || !particlesEl.isJsonObject()) {
            return List.of();
        }
        List<ParticleKeyframeData> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : particlesEl.getAsJsonObject().entrySet()) {
            Float sec = tryParseFloat(entry.getKey());
            if (sec == null) {
                continue;
            }
            float tick = sec * TICKS_PER_SECOND;
            addParticleEntries(tick, entry.getValue(), result);
        }
        result.sort(Comparator.comparingDouble(ParticleKeyframeData::startTick));
        return result;
    }

    private static void addParticleEntries(float tick, JsonElement value, List<ParticleKeyframeData> out) {
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (value.isJsonObject()) {
            JsonObject obj = value.getAsJsonObject();
            String effect = getString(obj, "effect");
            String locator = getString(obj, "locator");
            String script = getString(obj, "pre_effect_script");
            if (script.isEmpty()) {
                script = getString(obj, "script");
            }
            out.add(new ParticleKeyframeData(tick, effect, locator, script));
        } else if (value.isJsonArray()) {
            for (JsonElement item : value.getAsJsonArray()) {
                addParticleEntries(tick, item, out);
            }
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return "";
        }
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : "";
    }

    private static Float tryParseFloat(String raw) {
        try {
            float parsed = Float.parseFloat(raw);
            return Float.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
