package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
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

    @Inject(method = "<init>(Lcom/zigythebird/playeranimcore/bones/PlayerAnimBone;)V", at = @At("RETURN"))
    private void palMore$copyVectorBendFromCtor(PlayerAnimBone bone, CallbackInfo ci) {
        palMore$copyVectorBend(bone);
    }

    @Inject(method = "setToInitialPose", at = @At("RETURN"))
    private void palMore$resetVectorBend(CallbackInfo ci) {
        palMore$setBend(0.0F, 0.0F, 0.0F);
        palMore$setBendVectorOverride(false);
    }

    @Inject(method = "scale", at = @At("RETURN"))
    private void palMore$scaleVectorBend(float value, CallbackInfoReturnable<PlayerAnimBone> cir) {
        palMore$bendX *= value;
        palMore$bendY *= value;
        palMore$bendZ *= value;
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
    }

    @Inject(method = "applyOtherBone", at = @At("RETURN"))
    private void palMore$applyVectorBend(PlayerAnimBone bone, CallbackInfoReturnable<PlayerAnimBone> cir) {
        palMore$addVectorBend(bone, cir);
    }

    @Inject(method = "copyOtherBone", at = @At("RETURN"))
    private void palMore$copyVectorBend(PlayerAnimBone bone, CallbackInfoReturnable<PlayerAnimBone> cir) {
        palMore$copyVectorBend(bone);
    }

    @Inject(method = "copyOtherBoneIfNotDisabled", at = @At("RETURN"))
    private void palMore$copyVectorBendIfEnabled(PlayerAnimBone bone, CallbackInfoReturnable<PlayerAnimBone> cir) {
        if (bone instanceof ToggleablePlayerAnimBone toggleableBone && !toggleableBone.isBendEnabled()) {
            return;
        }

        palMore$copyVectorBend(bone);
    }

    @Inject(method = "beginOrEndTickLerp", at = @At("RETURN"))
    private void palMore$copyVectorBendDuringTickLerp(AdvancedPlayerAnimBone bone, float animTime, Animation animation, CallbackInfo ci) {
        if (!bone.isBendEnabled()) {
            return;
        }

        palMore$copyVectorBend(bone, ((PlayerAnimBone) (Object) this).bend);
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
}
