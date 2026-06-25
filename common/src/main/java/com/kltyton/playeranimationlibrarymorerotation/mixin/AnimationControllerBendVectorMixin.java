package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.kltyton.playeranimationlibrarymorerotation.util.PalMoreDebug;
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
import com.zigythebird.playeranimcore.enums.TransformType;
import net.minecraft.resources.Identifier;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
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
 * Computes sidecar bend values plus bend-only position/scale transforms after
 * PAL computes its native registered player bones in processCurrentAnimation.
 */
@Mixin(AnimationController.class)
public abstract class AnimationControllerBendVectorMixin {
    @Unique
    private static final KeyframeLocation PAL_MORE_EMPTY_KEYFRAME_LOCATION = new KeyframeLocation(new Keyframe(0), 0);

    @Shadow
    protected QueuedAnimation currentAnimation;
    @Final
    @Shadow
    protected Map<String, AdvancedPlayerAnimBone> bones;
    @Final
    @Shadow
    protected Map<String, PlayerAnimBone> activeBones;
    @Final
    @Shadow
    protected Map<String, PivotBone> pivotBones;
    @Final
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

        Animation animation = currentAnimation.animation();
        Identifier animationId = PalMoreBendResources.getAnimationId(animation);
        EasingType easingOverride = overrideEasingTypeFunction.apply((AnimationController) (Object) this);
        Map<String, KeyframeStack> tracks = PalMoreBendResources.getBendTracks(animation);
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
            if (PalMoreDebug.shouldLog(animationId)) {
                PalMoreDebug.verboseLimited(PalMoreDebug.FRAME,
                        "vector id={} target={} tick={} nativeBendX={} bendY={} bendZ={} yFrames={} zFrames={} easingOverride={} boneClass={}",
                        animationId,
                        entry.getKey(),
                        adjustedTick,
                        bone.bend,
                        bendY,
                        bendZ,
                        stack.yKeyframes().size(),
                        stack.zKeyframes().size(),
                        easingOverride,
                        bone.getClass().getName());
            }
        }

        Map<String, PalMoreBendResources.BendPartTracks> partTracks = PalMoreBendResources.getBendPartTracks(animation);
        for (Map.Entry<String, PalMoreBendResources.BendPartTracks> entry : partTracks.entrySet()) {
            PlayerAnimBone bone = bones.get(entry.getKey());
            if (bone == null) {
                bone = pivotBones.get(entry.getKey());
            }
            if (!(bone instanceof PalMoreBendHolder holder)) {
                if (PalMoreDebug.shouldLog(animationId)) {
                    PalMoreDebug.infoLimited(PalMoreDebug.FRAME,
                            "skip id={} target={} reason={} bones={} pivots={}",
                            animationId,
                            entry.getKey(),
                            bone == null ? "missing target bone" : "target does not implement PalMoreBendHolder",
                            bones.keySet(),
                            pivotBones.keySet());
                }
                continue;
            }

            activeBones.put(entry.getKey(), bone);
            PalMoreBendResources.BendPartTracks bendPartTracks = entry.getValue();
            KeyframeStack bendStack = bendPartTracks.bend();
            if (bendStack.hasKeyframes()) {
                float bendX = palMore$computeAnimValue(bendStack.xKeyframes(), adjustedTick, TransformType.BEND, easingOverride, bone.bend);
                float bendY = palMore$computeAnimValue(bendStack.yKeyframes(), adjustedTick, TransformType.BEND, easingOverride, 0.0F);
                float bendZ = palMore$computeAnimValue(bendStack.zKeyframes(), adjustedTick, TransformType.BEND, easingOverride, 0.0F);
                holder.palMore$setBend(bendX, bendY, bendZ);
                holder.palMore$setBendVectorOverride(true);
                if (PalMoreDebug.shouldLog(animationId)) {
                    PalMoreDebug.infoLimited(PalMoreDebug.FRAME,
                            "bend id={} target={} tick={} bend=({}, {}, {}) frames=({}, {}, {}) easingOverride={} active={} boneClass={}",
                            animationId,
                            entry.getKey(),
                            adjustedTick,
                            bendX,
                            bendY,
                            bendZ,
                            bendStack.xKeyframes().size(),
                            bendStack.yKeyframes().size(),
                            bendStack.zKeyframes().size(),
                            easingOverride,
                            activeBones.containsKey(entry.getKey()),
                            bone.getClass().getName());
                }
            }

            KeyframeStack positionStack = bendPartTracks.position();
            KeyframeStack scaleStack = bendPartTracks.scale();
            if (positionStack.hasKeyframes() || scaleStack.hasKeyframes()) {
                Vector3f position = new Vector3f(
                        palMore$computeAnimValue(positionStack.xKeyframes(), adjustedTick, TransformType.POSITION, easingOverride, 0.0F),
                        palMore$computeAnimValue(positionStack.yKeyframes(), adjustedTick, TransformType.POSITION, easingOverride, 0.0F),
                        palMore$computeAnimValue(positionStack.zKeyframes(), adjustedTick, TransformType.POSITION, easingOverride, 0.0F)
                );
                Vector3f scale = new Vector3f(
                        palMore$computeAnimValue(scaleStack.xKeyframes(), adjustedTick, TransformType.SCALE, easingOverride, 1.0F),
                        palMore$computeAnimValue(scaleStack.yKeyframes(), adjustedTick, TransformType.SCALE, easingOverride, 1.0F),
                        palMore$computeAnimValue(scaleStack.zKeyframes(), adjustedTick, TransformType.SCALE, easingOverride, 1.0F)
                );
                holder.palMore$setBendTransform(position.x, position.y, position.z, scale.x, scale.y, scale.z);
                holder.palMore$setBendTransformOverride(true);
                if (PalMoreDebug.shouldLog(animationId)) {
                    PalMoreDebug.infoLimited(PalMoreDebug.FRAME,
                            "transform id={} target={} tick={} pos=({}, {}, {}) scale=({}, {}, {}) posFrames=({}, {}, {}) scaleFrames=({}, {}, {}) easingOverride={} active={} boneClass={}",
                            animationId,
                            entry.getKey(),
                            adjustedTick,
                            position.x,
                            position.y,
                            position.z,
                            scale.x,
                            scale.y,
                            scale.z,
                            positionStack.xKeyframes().size(),
                            positionStack.yKeyframes().size(),
                            positionStack.zKeyframes().size(),
                            scaleStack.xKeyframes().size(),
                            scaleStack.yKeyframes().size(),
                            scaleStack.zKeyframes().size(),
                            easingOverride,
                            activeBones.containsKey(entry.getKey()),
                            bone.getClass().getName());
                }
            }
        }
    }

    @Unique
    private float palMore$computeVectorBendValue(List<Keyframe> frames, float tick, @Nullable EasingType easingOverride) {
        return palMore$computeAnimValue(frames, tick, TransformType.BEND, easingOverride, 0.0F);
    }

    @Unique
    private float palMore$computeAnimValue(List<Keyframe> frames, float tick, TransformType type, @Nullable EasingType easingOverride, float defaultValue) {
        if (frames.isEmpty()) {
            return defaultValue;
        }

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

        if (type == TransformType.ROTATION || type == TransformType.BEND) {
            if (!MolangLoader.isConstant(currentFrame.startValue())) {
                startValue = (float) Math.toRadians(startValue);
            }
            if (!MolangLoader.isConstant(currentFrame.endValue())) {
                endValue = (float) Math.toRadians(endValue);
            }
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
