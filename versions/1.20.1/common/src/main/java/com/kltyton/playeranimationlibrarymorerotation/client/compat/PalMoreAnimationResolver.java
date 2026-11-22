package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendState;
import com.kltyton.playeranimationlibrarymorerotation.mixin.access.AbstractFadeModifierAccessor;
import com.kltyton.playeranimationlibrarymorerotation.mixin.access.AnimationProcessorAccessor;
import com.kltyton.playeranimationlibrarymorerotation.mixin.access.AnimationStackAccessor;
import com.kltyton.playeranimationlibrarymorerotation.mixin.access.KeyframeAnimationPlayerAccessor;
import com.kltyton.playeranimationlibrarymorerotation.mixin.access.ModifierLayerAccessor;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationContainer;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.core.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Resolves sidecar bend and scale values across playerAnimator's layered animation stack,
 * respecting layer priority, MirrorModifier, and AbstractFadeModifier transitions.
 */
public final class PalMoreAnimationResolver {
    private PalMoreAnimationResolver() {
    }

    public static PalMoreBendState resolveBend(@Nullable AnimationProcessor processor, String boneName) {
        if (processor == null || !processor.isActive()) {
            return PalMoreBendState.NONE;
        }
        AnimationProcessorAccessor accessor = (AnimationProcessorAccessor) processor;
        IAnimation root = accessor.palMore$getAnimation();
        if (root == null) {
            return PalMoreBendState.NONE;
        }
        return resolveBend(root, boneName, accessor.palMore$getTickDelta(), PalMoreBendState.NONE);
    }

    public static Vector3f resolveScale(@Nullable AnimationProcessor processor, String boneName) {
        Vector3f fallback = new Vector3f(1.0F, 1.0F, 1.0F);
        if (processor == null || !processor.isActive()) {
            return fallback;
        }
        AnimationProcessorAccessor accessor = (AnimationProcessorAccessor) processor;
        IAnimation root = accessor.palMore$getAnimation();
        if (root == null) {
            return fallback;
        }
        return resolveScale(root, boneName, accessor.palMore$getTickDelta(), fallback);
    }

    private static PalMoreBendState resolveBend(
            IAnimation anim,
            String boneName,
            float tickDelta,
            PalMoreBendState current
    ) {
        if (anim == null || !anim.isActive()) {
            return current;
        }

        if (anim instanceof AnimationStack stack) {
            List<Pair<Integer, IAnimation>> layers = ((AnimationStackAccessor) stack).palMore$getLayers();
            PalMoreBendState state = current;
            for (Pair<Integer, IAnimation> layerPair : layers) {
                IAnimation layer = layerPair.getRight();
                if (layer != null && layer.isActive()) {
                    if (FirstPersonMode.isFirstPersonPass() && !layer.getFirstPersonMode(tickDelta).isEnabled()) {
                        continue;
                    }
                    state = resolveBend(layer, boneName, tickDelta, state);
                }
            }
            return state;
        }

        if (anim instanceof ModifierLayer<?> layer) {
            List<AbstractModifier> modifiers = ((ModifierLayerAccessor) layer).palMore$getModifiers();
            if (modifiers != null && !modifiers.isEmpty()) {
                return resolveBend(modifiers.get(0), boneName, tickDelta, current);
            }
            return resolveBend(layer.getAnimation(), boneName, tickDelta, current);
        }

        if (anim instanceof MirrorModifier mirror) {
            if (mirror.isEnabled()) {
                String mirroredBone = MirrorModifier.mirrorMap.getOrDefault(boneName, boneName);
                PalMoreBendState inner = resolveBend(mirror.getAnim(), mirroredBone, tickDelta, current.mirrored());
                return inner.mirrored();
            }
            return resolveBend(mirror.getAnim(), boneName, tickDelta, current);
        }

        if (anim instanceof AbstractFadeModifier fade) {
            AbstractFadeModifierAccessor accessor = (AbstractFadeModifierAccessor) fade;
            float progress = accessor.palMore$invokeCalculateProgress(tickDelta);
            PalMoreBendState toState = resolveBend(fade.getAnim(), boneName, tickDelta, current);
            if (progress > 1.0F) {
                return toState;
            }
            IAnimation begin = accessor.palMore$getBeginAnimation();
            PalMoreBendState fromState = begin != null && begin.isActive()
                    ? resolveBend(begin, boneName, tickDelta, current)
                    : current;
            float alpha = accessor.palMore$invokeGetAlpha(boneName, TransformType.BEND, progress);
            return PalMoreBendState.lerp(fromState, toState, alpha);
        }

        if (anim instanceof AnimationContainer<?> container) {
            return resolveBend(container.getAnim(), boneName, tickDelta, current);
        }

        if (anim instanceof KeyframeAnimationPlayer player) {
            PalMoreAnimationExtras extras = PalMoreBendResources.getExtras(player.getData());
            if (extras.isEmpty()) {
                return current;
            }
            float tick = getCurrentFloatTick(player);
            PalMoreBendState sampled = extras.sampleBend(boneName, tick);
            return sampled.hasActiveBend() ? sampled : current;
        }

        return current;
    }

    private static Vector3f resolveScale(
            IAnimation anim,
            String boneName,
            float tickDelta,
            Vector3f current
    ) {
        if (anim == null || !anim.isActive()) {
            return current;
        }

        if (anim instanceof AnimationStack stack) {
            List<Pair<Integer, IAnimation>> layers = ((AnimationStackAccessor) stack).palMore$getLayers();
            Vector3f state = current;
            for (Pair<Integer, IAnimation> layerPair : layers) {
                IAnimation layer = layerPair.getRight();
                if (layer != null && layer.isActive()) {
                    if (FirstPersonMode.isFirstPersonPass() && !layer.getFirstPersonMode(tickDelta).isEnabled()) {
                        continue;
                    }
                    state = resolveScale(layer, boneName, tickDelta, state);
                }
            }
            return state;
        }

        if (anim instanceof ModifierLayer<?> layer) {
            List<AbstractModifier> modifiers = ((ModifierLayerAccessor) layer).palMore$getModifiers();
            if (modifiers != null && !modifiers.isEmpty()) {
                return resolveScale(modifiers.get(0), boneName, tickDelta, current);
            }
            return resolveScale(layer.getAnimation(), boneName, tickDelta, current);
        }

        if (anim instanceof MirrorModifier mirror) {
            String targetBone = mirror.isEnabled()
                    ? MirrorModifier.mirrorMap.getOrDefault(boneName, boneName)
                    : boneName;
            return resolveScale(mirror.getAnim(), targetBone, tickDelta, current);
        }

        if (anim instanceof AbstractFadeModifier fade) {
            AbstractFadeModifierAccessor accessor = (AbstractFadeModifierAccessor) fade;
            float progress = accessor.palMore$invokeCalculateProgress(tickDelta);
            Vector3f toState = resolveScale(fade.getAnim(), boneName, tickDelta, current);
            if (progress > 1.0F) {
                return toState;
            }
            IAnimation begin = accessor.palMore$getBeginAnimation();
            Vector3f fromState = begin != null && begin.isActive()
                    ? resolveScale(begin, boneName, tickDelta, current)
                    : current;
            float alpha = Math.max(0.0F, Math.min(1.0F, accessor.palMore$invokeGetAlpha(boneName, TransformType.POSITION, progress)));
            return new Vector3f(
                    fromState.x + (toState.x - fromState.x) * alpha,
                    fromState.y + (toState.y - fromState.y) * alpha,
                    fromState.z + (toState.z - fromState.z) * alpha
            );
        }

        if (anim instanceof AnimationContainer<?> container) {
            return resolveScale(container.getAnim(), boneName, tickDelta, current);
        }

        if (anim instanceof KeyframeAnimationPlayer player) {
            PalMoreAnimationExtras extras = PalMoreBendResources.getExtras(player.getData());
            if (extras.isEmpty()) {
                return current;
            }
            float tick = getCurrentFloatTick(player);
            Vector3f sampled = extras.sampleBoneScale(boneName, tick);
            return sampled != null ? sampled : current;
        }

        return current;
    }

    private static float getCurrentFloatTick(KeyframeAnimationPlayer player) {
        return player.getCurrentTick() + ((KeyframeAnimationPlayerAccessor) player).palMore$getTickDelta();
    }
}
