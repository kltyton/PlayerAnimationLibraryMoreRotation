package com.kltyton.playeranimationlibrarymorerotation;

import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.ParticleKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.SoundKeyframeData;
import com.zigythebird.playeranimcore.event.EventResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * PalMore playback controller configuration.
 *
 * <p>The object itself is not synced over the network. Server playback sends only
 * {@link #id()}, and clients resolve that id through their local controller registry.</p>
 */
public final class PalMoreAnimationController {
    public static final Identifier DEFAULT_ID = Playeranimationlibrarymorerotation.id("default");
    public static final PalMoreAnimationController DEFAULT = new PalMoreAnimationController(DEFAULT_ID);

    private final Identifier id;
    private @Nullable CustomInstructionKeyframeHandler customInstructionKeyframeHandler;
    private @Nullable ParticleKeyframeHandler particleKeyframeHandler;
    private @Nullable SoundKeyframeHandler soundKeyframeHandler;

    private PalMoreAnimationController(Identifier id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public static PalMoreAnimationController create(Identifier id) {
        return new PalMoreAnimationController(id);
    }

    public Identifier id() {
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

    public record CustomInstructionKeyframeContext(
            float animationTick,
            Avatar avatar,
            AnimationController animationController,
            CustomInstructionKeyframeData keyframeData,
            AnimationData animationData
    ) {
        public String instructions() {
            return this.keyframeData.getInstructions();
        }
    }

    public record ParticleKeyframeContext(
            float animationTick,
            Avatar avatar,
            AnimationController animationController,
            ParticleKeyframeData keyframeData,
            AnimationData animationData
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
            Avatar avatar,
            AnimationController animationController,
            SoundKeyframeData keyframeData,
            AnimationData animationData
    ) {
        public String sound() {
            return this.keyframeData.getSound();
        }
    }
}
