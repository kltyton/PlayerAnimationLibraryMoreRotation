package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.kltyton.playeranimationlibrarymorerotation.util.PalMoreDebug;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.bones.ToggleablePlayerAnimBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds PlayerAnimationLibraryMoreRotation's bend X/Y/Z sidecar fields to PAL's
 * PlayerAnimBone and keeps them in sync through PAL's bone copy/composition APIs.
 */
@Mixin(PlayerAnimBone.class)
public class PlayerAnimBoneBendVectorMixin implements PalMoreBendHolder {
    @Unique
    private float palMore$bendX;
    @Unique
    private float palMore$bendY;
    @Unique
    private float palMore$bendZ;
    @Unique
    private boolean palMore$bendVectorOverride;
    @Unique
    private float palMore$bendPositionX;
    @Unique
    private float palMore$bendPositionY;
    @Unique
    private float palMore$bendPositionZ;
    @Unique
    private float palMore$bendScaleX = 1.0F;
    @Unique
    private float palMore$bendScaleY = 1.0F;
    @Unique
    private float palMore$bendScaleZ = 1.0F;
    @Unique
    private boolean palMore$bendTransformOverride;

    @Override
    public float palMore$getBendX() {
        return palMore$bendX;
    }

    @Override
    public float palMore$getBendY() {
        return palMore$bendY;
    }

    @Override
    public float palMore$getBendZ() {
        return palMore$bendZ;
    }

    @Override
    public void palMore$setBend(float bendX, float bendY, float bendZ) {
        palMore$bendX = bendX;
        palMore$bendY = bendY;
        palMore$bendZ = bendZ;
    }

    @Override
    public boolean palMore$hasBendVectorOverride() {
        return palMore$bendVectorOverride;
    }

    @Override
    public void palMore$setBendVectorOverride(boolean active) {
        palMore$bendVectorOverride = active;
    }

    @Override
    public void palMore$setBendTransform(float positionX, float positionY, float positionZ, float scaleX, float scaleY, float scaleZ) {
        palMore$bendPositionX = positionX;
        palMore$bendPositionY = positionY;
        palMore$bendPositionZ = positionZ;
        palMore$bendScaleX = scaleX;
        palMore$bendScaleY = scaleY;
        palMore$bendScaleZ = scaleZ;
    }

    @Override
    public float palMore$getBendPositionX() {
        return palMore$bendPositionX;
    }

    @Override
    public float palMore$getBendPositionY() {
        return palMore$bendPositionY;
    }

    @Override
    public float palMore$getBendPositionZ() {
        return palMore$bendPositionZ;
    }

    @Override
    public float palMore$getBendScaleX() {
        return palMore$bendScaleX;
    }

    @Override
    public float palMore$getBendScaleY() {
        return palMore$bendScaleY;
    }

    @Override
    public float palMore$getBendScaleZ() {
        return palMore$bendScaleZ;
    }

    @Override
    public boolean palMore$hasBendTransformOverride() {
        return palMore$bendTransformOverride;
    }

    @Override
    public void palMore$setBendTransformOverride(boolean active) {
        palMore$bendTransformOverride = active;
    }

    @Inject(method = "<init>(Lcom/zigythebird/playeranimcore/bones/PlayerAnimBone;)V", at = @At("RETURN"))
    private void palMore$copyVectorBendFromCtor(PlayerAnimBone bone, CallbackInfo ci) {
        palMore$copyVectorBend(bone);
        palMore$copyBendTransform(bone);
    }

    @Inject(method = "setToInitialPose", at = @At("RETURN"))
    private void palMore$resetVectorBend(CallbackInfo ci) {
        palMore$setBend(0.0F, 0.0F, 0.0F);
        palMore$setBendVectorOverride(false);
        palMore$resetBendTransform();
    }

    @Inject(method = "scale", at = @At("RETURN"))
    private void palMore$scaleVectorBend(float value, CallbackInfoReturnable<PlayerAnimBone> cir) {
        palMore$bendX *= value;
        palMore$bendY *= value;
        palMore$bendZ *= value;
        palMore$bendPositionX *= value;
        palMore$bendPositionY *= value;
        palMore$bendPositionZ *= value;
    }

    @Inject(method = "add", at = @At("RETURN"))
    private void palMore$addVectorBend(PlayerAnimBone bone, CallbackInfoReturnable<PlayerAnimBone> cir) {
        if (bone instanceof PalMoreBendHolder holder && holder.palMore$hasBendVectorOverride()) {
            palMore$bendX += holder.palMore$getBendX();
            palMore$bendY += holder.palMore$getBendY();
            palMore$bendZ += holder.palMore$getBendZ();
            palMore$setBendVectorOverride(true);
        } else {
            palMore$bendX += bone.bend;
        }
        if (bone instanceof PalMoreBendHolder holder && holder.palMore$hasBendTransformOverride()) {
            palMore$bendPositionX += holder.palMore$getBendPositionX();
            palMore$bendPositionY += holder.palMore$getBendPositionY();
            palMore$bendPositionZ += holder.palMore$getBendPositionZ();
            palMore$bendScaleX *= holder.palMore$getBendScaleX();
            palMore$bendScaleY *= holder.palMore$getBendScaleY();
            palMore$bendScaleZ *= holder.palMore$getBendScaleZ();
            palMore$setBendTransformOverride(true);
        }
    }

    @Inject(method = "applyOtherBone", at = @At("RETURN"))
    private void palMore$applyVectorBend(PlayerAnimBone bone, CallbackInfoReturnable<PlayerAnimBone> cir) {
        palMore$addVectorBend(bone, cir);
    }

    @Inject(method = "copyOtherBone", at = @At("RETURN"))
    private void palMore$copyVectorBend(PlayerAnimBone bone, CallbackInfoReturnable<PlayerAnimBone> cir) {
        palMore$copyVectorBend(bone);
        palMore$copyBendTransform(bone);
    }

    @Inject(method = "copyOtherBoneIfNotDisabled", at = @At("RETURN"))
    private void palMore$copyVectorBendIfEnabled(PlayerAnimBone bone, CallbackInfoReturnable<PlayerAnimBone> cir) {
        if (!(bone instanceof ToggleablePlayerAnimBone toggleableBone) || toggleableBone.isBendEnabled()) {
            palMore$copyVectorBend(bone);
        }

        palMore$copyBendTransform(bone);
    }

    @Inject(method = "beginOrEndTickLerp", at = @At("RETURN"))
    private void palMore$copyVectorBendDuringTickLerp(AdvancedPlayerAnimBone bone, float animTime, Animation animation, CallbackInfo ci) {
        if (bone.isBendEnabled()) {
            palMore$copyVectorBend(bone, ((PlayerAnimBone) (Object) this).bend);
        }

        palMore$copyBendTransform(bone);
    }

    @Unique
    private void palMore$copyVectorBend(PlayerAnimBone bone) {
        palMore$copyVectorBend(bone, bone.bend);
    }

    @Unique
    private void palMore$copyVectorBend(PlayerAnimBone bone, float bendX) {
        if (bone instanceof PalMoreBendHolder holder && holder.palMore$hasBendVectorOverride()) {
            palMore$setBend(bendX, holder.palMore$getBendY(), holder.palMore$getBendZ());
            palMore$setBendVectorOverride(true);
        } else {
            palMore$setBend(bendX, 0.0F, 0.0F);
            palMore$setBendVectorOverride(false);
        }
    }

    @Unique
    private void palMore$copyBendTransform(PlayerAnimBone bone) {
        if (bone instanceof PalMoreBendHolder holder && holder.palMore$hasBendTransformOverride()) {
            palMore$setBendTransform(
                    holder.palMore$getBendPositionX(),
                    holder.palMore$getBendPositionY(),
                    holder.palMore$getBendPositionZ(),
                    holder.palMore$getBendScaleX(),
                    holder.palMore$getBendScaleY(),
                    holder.palMore$getBendScaleZ()
            );
            palMore$setBendTransformOverride(true);
            if (PalMoreDebug.enabled()) {
                PlayerAnimBone self = (PlayerAnimBone) (Object) this;
                PalMoreDebug.infoLimited(PalMoreDebug.COPY,
                        "copy target={} from={} bend=({}, {}, {}) pos=({}, {}, {}) scale=({}, {}, {}) sourceClass={}",
                        self.getName(),
                        bone.getName(),
                        holder.palMore$getBendX(),
                        holder.palMore$getBendY(),
                        holder.palMore$getBendZ(),
                        holder.palMore$getBendPositionX(),
                        holder.palMore$getBendPositionY(),
                        holder.palMore$getBendPositionZ(),
                        holder.palMore$getBendScaleX(),
                        holder.palMore$getBendScaleY(),
                        holder.palMore$getBendScaleZ(),
                        bone.getClass().getName());
            }
        } else {
            palMore$resetBendTransform();
        }
    }

    @Unique
    private void palMore$resetBendTransform() {
        palMore$setBendTransform(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        palMore$setBendTransformOverride(false);
    }
}
