package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears PALMore vector bend sidecar state after PAL copied vanilla part data
 * and before AvatarAnimManager lets the current animation stack write this bone.
 */
@Mixin(AvatarAnimManager.class)
public class AvatarAnimManagerBendVectorResetMixin {
    @Inject(method = "updatePart", at = @At("HEAD"))
    private void palMore$resetVectorBendBeforeAnimation(ModelPart part, PlayerAnimBone bone, CallbackInfo ci) {
        if (bone instanceof PalMoreBendHolder holder) {
            holder.palMore$setBend(bone.bend, 0.0F, 0.0F);
            holder.palMore$setBendVectorOverride(false);
            holder.palMore$setBendTransform(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            holder.palMore$setBendTransformOverride(false);
        }
    }
}
