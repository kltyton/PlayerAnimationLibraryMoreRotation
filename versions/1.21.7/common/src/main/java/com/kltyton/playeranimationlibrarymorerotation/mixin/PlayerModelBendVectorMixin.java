package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendableCuboids;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.zigythebird.playeranim.accessors.IPlayerAnimationState;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies sidecar bend Y/Z values and bend-only position/scale mesh transforms
 * to bendable-cuboids player model parts after its playeranim handler has
 * applied native X bend.
 */
@Mixin(value = PlayerModel.class, priority = 2500)
public abstract class PlayerModelBendVectorMixin extends HumanoidModel<PlayerRenderState> {
    @Shadow
    @Final
    public ModelPart leftSleeve;
    @Shadow
    @Final
    public ModelPart rightSleeve;
    @Shadow
    @Final
    public ModelPart leftPants;
    @Shadow
    @Final
    public ModelPart rightPants;
    @Shadow
    @Final
    public ModelPart jacket;

    public PlayerModelBendVectorMixin(ModelPart root) {
        super(root);
    }

    @Shadow(remap = false) @Final private PlayerAnimBone pal$torso;
    @Shadow(remap = false) @Final private PlayerAnimBone pal$rightArm;
    @Shadow(remap = false) @Final private PlayerAnimBone pal$leftArm;
    @Shadow(remap = false) @Final private PlayerAnimBone pal$rightLeg;
    @Shadow(remap = false) @Final private PlayerAnimBone pal$leftLeg;

    @TargetHandler(
            mixin = "com.zigythebird.bendable_cuboids.mixin.playeranim.PlayerModelMixin_playerAnim",
            name = "setupPlayerAnimation",
            prefix = "handler"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("RETURN"), remap = false)
    private void palMore$applyVectorBend(PlayerRenderState state, CallbackInfo originalCi, CallbackInfo ci) {
        boolean active = state instanceof IPlayerAnimationState animated
                && animated.playerAnimLib$getAnimManager() != null
                && animated.playerAnimLib$getAnimManager().isActive();
        palMore$applyPart(body, jacket, pal$torso, active);
        palMore$applyPart(rightArm, rightSleeve, pal$rightArm, active);
        palMore$applyPart(leftArm, leftSleeve, pal$leftArm, active);
        palMore$applyPart(rightLeg, rightPants, pal$rightLeg, active);
        palMore$applyPart(leftLeg, leftPants, pal$leftLeg, active);
    }

    @org.spongepowered.asm.mixin.Unique
    private void palMore$applyPart(ModelPart part, ModelPart overlay, PlayerAnimBone bone, boolean active) {
        if (active && bone instanceof PalMoreBendHolder holder) {
            if (!holder.palMore$hasBendVectorOverride()) {
                holder.palMore$setBend(bone.getBend(), 0.0F, 0.0F);
            }
            PalMoreBendableCuboids.applyVectorBend(part, holder);
            PalMoreBendableCuboids.applyVectorBend(overlay, holder);
        } else {
            PalMoreBendableCuboids.clearPartState(part);
            PalMoreBendableCuboids.clearPartState(overlay);
        }
    }
}
