package com.kltyton.playeranimationlibrarymorerotation.client;

import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.CustomInstructionKeyframeContext;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.CustomInstructionKeyframeData;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.CustomInstructionKeyframeHandler;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.ParticleKeyframeContext;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.ParticleKeyframeData;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.ParticleKeyframeHandler;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.SoundKeyframeContext;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.SoundKeyframeData;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController.SoundKeyframeHandler;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreEffectTimeline;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.impl.event.EventResult;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * KeyframeAnimationPlayer subclass that dispatches Bedrock/PAL timeline, sound_effects,
 * and particle_effects keyframes to a PalMoreAnimationController.
 */
public final class PalMoreKeyframePlayer extends KeyframeAnimationPlayer {
    private final AbstractClientPlayer player;
    private final ResourceLocation animationId;
    private final PalMoreAnimationController controller;
    private final PalMoreEffectTimeline timeline;
    private int lastEvaluatedTick = -1;

    public PalMoreKeyframePlayer(
            AbstractClientPlayer player,
            ResourceLocation animationId,
            KeyframeAnimation animation,
            PalMoreAnimationController controller
    ) {
        super(animation);
        this.player = player;
        this.animationId = animationId;
        this.controller = controller;
        this.timeline = PalMoreBendResources.getExtras(animation).effectTimeline();
    }

    @Override
    public void tick() {
        boolean wasActive = isActive();
        int currentTick = getCurrentTick();
        if (wasActive && !this.timeline.isEmpty()) {
            dispatchRange(this.lastEvaluatedTick, currentTick);
            this.lastEvaluatedTick = currentTick;
        }
        super.tick();
        if (isActive() && getCurrentTick() < currentTick) {
            this.lastEvaluatedTick = getCurrentTick() - 1;
        }
    }

    private void dispatchRange(int previousTick, int currentTick) {
        float minExclusive = previousTick;
        float maxInclusive = currentTick;

        dispatchInstructions(this.timeline.instructions(), minExclusive, maxInclusive);
        dispatchParticles(this.timeline.particles(), minExclusive, maxInclusive);
        dispatchSounds(this.timeline.sounds(), minExclusive, maxInclusive);
    }

    private void dispatchInstructions(
            List<CustomInstructionKeyframeData> keyframes,
            float minExclusive,
            float maxInclusive
    ) {
        if (keyframes.isEmpty()) {
            return;
        }
        CustomInstructionKeyframeHandler handler = this.controller.customInstructionKeyframeHandler();
        if (handler == null) {
            return;
        }
        for (CustomInstructionKeyframeData data : keyframes) {
            if (isInWindow(data.startTick(), minExclusive, maxInclusive)) {
                handler.handle(new CustomInstructionKeyframeContext(
                        data.startTick(),
                        this.player,
                        this.animationId,
                        data
                ));
            }
        }
    }

    private void dispatchParticles(
            List<ParticleKeyframeData> keyframes,
            float minExclusive,
            float maxInclusive
    ) {
        if (keyframes.isEmpty()) {
            return;
        }
        ParticleKeyframeHandler handler = this.controller.particleKeyframeHandler();
        if (handler == null) {
            return;
        }
        for (ParticleKeyframeData data : keyframes) {
            if (isInWindow(data.startTick(), minExclusive, maxInclusive)) {
                handler.handle(new ParticleKeyframeContext(
                        data.startTick(),
                        this.player,
                        this.animationId,
                        data
                ));
            }
        }
    }

    private void dispatchSounds(
            List<SoundKeyframeData> keyframes,
            float minExclusive,
            float maxInclusive
    ) {
        if (keyframes.isEmpty()) {
            return;
        }
        SoundKeyframeHandler handler = this.controller.soundKeyframeHandler();
        for (SoundKeyframeData data : keyframes) {
            if (!isInWindow(data.startTick(), minExclusive, maxInclusive)) {
                continue;
            }
            EventResult result = EventResult.PASS;
            if (handler != null) {
                EventResult handled = handler.handle(new SoundKeyframeContext(
                        data.startTick(),
                        this.player,
                        this.animationId,
                        data
                ));
                if (handled != null) {
                    result = handled;
                }
            }
            if (result == EventResult.PASS) {
                playDefaultSound(data.sound());
            }
        }
    }

    private void playDefaultSound(String soundIdRaw) {
        if (soundIdRaw == null || soundIdRaw.isEmpty() || this.player.level() == null) {
            return;
        }
        ResourceLocation soundLocation = ResourceLocation.tryParse(soundIdRaw);
        if (soundLocation == null) {
            return;
        }
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundLocation);
        if (soundEvent != null) {
            this.player.level().playLocalSound(
                    this.player.getX(),
                    this.player.getY(),
                    this.player.getZ(),
                    soundEvent,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F,
                    false
            );
        }
    }

    private static boolean isInWindow(float tick, float minExclusive, float maxInclusive) {
        return tick > minExclusive && tick <= maxInclusive;
    }
}
