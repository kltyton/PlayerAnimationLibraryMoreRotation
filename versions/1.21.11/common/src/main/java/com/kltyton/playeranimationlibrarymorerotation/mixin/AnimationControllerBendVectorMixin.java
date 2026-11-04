package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.QueuedAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.AnimationPoint;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.bones.PivotBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.TransformType;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.unnamed.mocha.MochaEngine;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Consumer;

/**
 * Computes sidecar bend values plus bend-only position/scale transforms after
 * PAL computes its native registered player bones in processCurrentAnimation.
 */
@Mixin(AnimationController.class)
public abstract class AnimationControllerBendVectorMixin {
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


    @Invoker("getAnimationPointAtTick")
    protected abstract AnimationPoint palMore$sampleNativePoint(QueuedAnimation queued, List<Keyframe> frames,
            float tick, TransformType type, Consumer<Float> transitionLengthSetter);

    @Inject(method = "processCurrentAnimation", at = @At("RETURN"))
    private void palMore$applyVectorBendValues(float adjustedTick, AnimationData animationData, CallbackInfo ci) {
        if (currentAnimation == null || currentAnimation.animation() == null) {
            return;
        }

        Animation animation = currentAnimation.animation();
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
        }

        Map<String, PalMoreBendResources.BendPartTracks> partTracks = PalMoreBendResources.getBendPartTracks(animation);
        for (Map.Entry<String, PalMoreBendResources.BendPartTracks> entry : partTracks.entrySet()) {
            PlayerAnimBone bone = bones.get(entry.getKey());
            if (bone == null) {
                bone = pivotBones.get(entry.getKey());
            }
            if (!(bone instanceof PalMoreBendHolder holder)) {
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
            }
        }
    }

    @Unique
    private float palMore$computeVectorBendValue(List<Keyframe> frames, float tick, EasingType easingOverride) {
        return palMore$computeAnimValue(frames, tick, TransformType.BEND, easingOverride, 0.0F);
    }

    @Unique
    private float palMore$computeAnimValue(List<Keyframe> frames, float tick, TransformType type, EasingType easingOverride, float defaultValue) {
        if (frames.isEmpty()) {
            return defaultValue;
        }

        return EasingType.lerpWithOverride(molangRuntime,
                palMore$sampleNativePoint(currentAnimation, frames, tick, type, null), easingOverride);
    }
}
