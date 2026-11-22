package com.kltyton.playeranimationlibrarymorerotation;

import dev.kosmx.playerAnim.core.impl.event.EventResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * PalMore playback controller configuration for Minecraft 1.20.1 (playerAnimator backend).
 *
 * <p>The object itself is not synced over the network. Server playback sends only
 * {@link #id()}, and clients resolve that id through their local controller registry.</p>
 */
public final class PalMoreAnimationController {
    public static final ResourceLocation DEFAULT_ID = Playeranimationlibrarymorerotation.id("default");
    public static final PalMoreAnimationController DEFAULT = new PalMoreAnimationController(DEFAULT_ID);

    private final ResourceLocation id;
    private @Nullable CustomInstructionKeyframeHandler customInstructionKeyframeHandler;
    private @Nullable ParticleKeyframeHandler particleKeyframeHandler;
    private @Nullable SoundKeyframeHandler soundKeyframeHandler;

    private PalMoreAnimationController(ResourceLocation id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public static PalMoreAnimationController create(ResourceLocation id) {
        return new PalMoreAnimationController(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public PalMoreAnimationController setCustomInstructionKeyframeHandler(
            @Nullable CustomInstructionKeyframeConsumer handler
    ) {
        if (handler == null) {
            this.customInstructionKeyframeHandler = null;
            return this;
        }

        this.customInstructionKeyframeHandler = context -> {
            handler.handle(context);
            return EventResult.SUCCESS;
        };
        return this;
    }

    public PalMoreAnimationController setCustomInstructionKeyframeHandler(
            @Nullable CustomInstructionKeyframeHandler handler
    ) {
        this.customInstructionKeyframeHandler = handler;
        return this;
    }

    public PalMoreAnimationController setParticleKeyframeHandler(@Nullable ParticleKeyframeConsumer handler) {
        if (handler == null) {
            this.particleKeyframeHandler = null;
            return this;
        }

        this.particleKeyframeHandler = context -> {
            handler.handle(context);
            return EventResult.SUCCESS;
        };
        return this;
    }

    public PalMoreAnimationController setParticleKeyframeHandler(@Nullable ParticleKeyframeHandler handler) {
        this.particleKeyframeHandler = handler;
        return this;
    }

    public PalMoreAnimationController setSoundKeyframeHandler(@Nullable SoundKeyframeConsumer handler) {
        if (handler == null) {
            this.soundKeyframeHandler = null;
            return this;
        }

        this.soundKeyframeHandler = context -> {
            handler.handle(context);
            return EventResult.SUCCESS;
        };
        return this;
    }

    public PalMoreAnimationController setSoundKeyframeHandler(@Nullable SoundKeyframeHandler handler) {
        this.soundKeyframeHandler = handler;
        return this;
    }

    public @Nullable CustomInstructionKeyframeHandler customInstructionKeyframeHandler() {
        return this.customInstructionKeyframeHandler;
    }

    public @Nullable ParticleKeyframeHandler particleKeyframeHandler() {
        return this.particleKeyframeHandler;
    }

    public @Nullable SoundKeyframeHandler soundKeyframeHandler() {
        return this.soundKeyframeHandler;
    }

    @FunctionalInterface
    public interface CustomInstructionKeyframeConsumer {
        void handle(CustomInstructionKeyframeContext context);
    }

    @FunctionalInterface
    public interface CustomInstructionKeyframeHandler {
        EventResult handle(CustomInstructionKeyframeContext context);
    }

    @FunctionalInterface
    public interface ParticleKeyframeConsumer {
        void handle(ParticleKeyframeContext context);
    }

    @FunctionalInterface
    public interface ParticleKeyframeHandler {
        EventResult handle(ParticleKeyframeContext context);
    }

    @FunctionalInterface
    public interface SoundKeyframeConsumer {
        void handle(SoundKeyframeContext context);
    }

    @FunctionalInterface
    public interface SoundKeyframeHandler {
        EventResult handle(SoundKeyframeContext context);
    }

    public record CustomInstructionKeyframeData(float startTick, String instructions) {
        public String getInstructions() {
            return this.instructions;
        }
    }

    public record ParticleKeyframeData(float startTick, String effect, String locator, String script) {
        public String getEffect() {
            return this.effect;
        }

        public String getLocator() {
            return this.locator;
        }

        public String getScript() {
            return this.script;
        }
    }

    public record SoundKeyframeData(float startTick, String sound) {
        public String getSound() {
            return this.sound;
        }
    }

    public record CustomInstructionKeyframeContext(
            float animationTick,
            Player avatar,
            ResourceLocation animationId,
            CustomInstructionKeyframeData keyframeData
    ) {
        public String instructions() {
            return this.keyframeData.getInstructions();
        }
    }

    public record ParticleKeyframeContext(
            float animationTick,
            Player avatar,
            ResourceLocation animationId,
            ParticleKeyframeData keyframeData
    ) {
        public String effect() {
            return this.keyframeData.getEffect();
        }

        public String locator() {
            return this.keyframeData.getLocator();
        }

        public String script() {
            return this.keyframeData.script();
        }
    }

    public record SoundKeyframeContext(
            float animationTick,
            Player avatar,
            ResourceLocation animationId,
            SoundKeyframeData keyframeData
    ) {
        public String sound() {
            return this.keyframeData.getSound();
        }
    }
}
