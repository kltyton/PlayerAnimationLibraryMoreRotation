package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.QueuedAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeLocation;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.bones.PivotBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.unnamed.mocha.MochaEngine;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Computes sidecar bend Y/Z values after PAL computes its native scalar bend X
 * value in AnimationController.processCurrentAnimation.
 */
@Mixin(AnimationController.class)
public abstract class AnimationControllerBendVectorMixin {
    @Unique
    private static final KeyframeLocation PAL_MORE_EMPTY_KEYFRAME_LOCATION = new KeyframeLocation(new Keyframe(0), 0);

    @Shadow
    protected QueuedAnimation currentAnimation;
    @Shadow
    protected Map<String, AdvancedPlayerAnimBone> bones;
    @Shadow
    protected Map<String, PivotBone> pivotBones;
    @Shadow
    protected MochaEngine<AnimationController> molangRuntime;
    @Shadow
    protected Function<AnimationController, EasingType> overrideEasingTypeFunction;

    @Shadow
    public abstract boolean isAnimationPlayerAnimatorFormat();

    @Shadow
    public abstract boolean isLoopStarted();

    @Inject(method = "processCurrentAnimation", at = @At("RETURN"))
    private void palMore$applyVectorBendValues(float adjustedTick, AnimationData animationData, CallbackInfo ci) {
        if (currentAnimation == null || currentAnimation.animation() == null) {
            return;
        }

        Map<String, KeyframeStack> tracks = PalMoreBendResources.getBendTracks(currentAnimation.animation());
        if (tracks.isEmpty()) {
            return;
        }

        EasingType easingOverride = overrideEasingTypeFunction.apply((AnimationController) (Object) this);
        for (Map.Entry<String, KeyframeStack> entry : tracks.entrySet()) {
            PlayerAnimBone bone = bones.get(entry.getKey());
            if (bone == null) {
                bone = pivotBones.get(entry.getKey());
            }
            if (bone == null) {
                continue;
            }

            KeyframeStack stack = entry.getValue();
            float bendY = palMore$computeVectorBendValue(stack.yKeyframes(), adjustedTick, easingOverride);
            float bendZ = palMore$computeVectorBendValue(stack.zKeyframes(), adjustedTick, easingOverride);
            PalMoreBendHolder holder = (PalMoreBendHolder) bone;
            holder.palMore$setBend(bone.bend, bendY, bendZ);
            holder.palMore$setBendVectorOverride(true);
        }
    }

    @Unique
    private float palMore$computeVectorBendValue(List<Keyframe> frames, float tick, @Nullable EasingType easingOverride) {
        Animation animation = currentAnimation.animation();
        KeyframeLocation location = palMore$getCurrentKeyFrameLocation(
                frames,
                tick,
                isAnimationPlayerAnimatorFormat() && currentAnimation.loopType().shouldPlayAgain(null, animation),
                animation.length(),
                currentAnimation.loopType().restartFromTick(null, animation)
        );
        Keyframe currentFrame = location.keyframe();
        float startValue = molangRuntime.eval(currentFrame.startValue());
        float endValue = molangRuntime.eval(currentFrame.endValue());

        if (!MolangLoader.isConstant(currentFrame.startValue())) {
            startValue = (float) Math.toRadians(startValue);
        }
        if (!MolangLoader.isConstant(currentFrame.endValue())) {
            endValue = (float) Math.toRadians(endValue);
        }

        float lerpValue = currentFrame.length() > 0 ? location.startTick() / currentFrame.length() : 0;
        return EasingType.lerpWithOverride(molangRuntime, startValue, endValue, currentFrame.length(), lerpValue,
                currentFrame.easingArgs(), currentFrame.easingType(), easingOverride);
    }

    @Unique
    private KeyframeLocation palMore$getCurrentKeyFrameLocation(List<Keyframe> frames, float ageInTicks, boolean isPlayerAnimatorLoop, float animTime, float returnToTick) {
        if (frames.isEmpty()) {
            return PAL_MORE_EMPTY_KEYFRAME_LOCATION;
        }

        Keyframe firstFrame = returnToTick == 0 ? frames.getFirst() : Keyframe.getKeyframeAtTime(frames, returnToTick);
        float totalFrameTime = 0;

        for (Keyframe frame : frames) {
            totalFrameTime += frame.length();

            if (totalFrameTime > ageInTicks) {
                if (isPlayerAnimatorLoop && isLoopStarted() && frame == firstFrame) {
                    float stopTickMinusLastKeyframe = animTime - Keyframe.getLastKeyframeTime(frames);
                    return new KeyframeLocation(new Keyframe(frame.length() + stopTickMinusLastKeyframe, frames.getLast().endValue(), frame.endValue(),
                            frame.easingType(), frame.easingArgs()), ageInTicks + stopTickMinusLastKeyframe);
                }
                return new KeyframeLocation(frame, ageInTicks - (totalFrameTime - frame.length()));
            }
        }

        if (isPlayerAnimatorLoop) {
            return new KeyframeLocation(new Keyframe(firstFrame.length() + animTime - totalFrameTime, frames.getLast().endValue(), firstFrame.endValue(),
                    firstFrame.easingType(), firstFrame.easingArgs()), ageInTicks - totalFrameTime);
        }

        return new KeyframeLocation(frames.getLast(), ageInTicks);
    }
}
