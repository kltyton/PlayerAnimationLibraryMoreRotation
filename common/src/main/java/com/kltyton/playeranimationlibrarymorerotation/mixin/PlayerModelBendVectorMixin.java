package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendableCuboids;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies sidecar bend Y/Z values to bendable-cuboids player model parts after
 * bendable-cuboids' PlayerModelMixin_playerAnim handler has applied native X bend.
 */
@Mixin(value = PlayerModel.class, priority = 2500)
public abstract class PlayerModelBendVectorMixin extends HumanoidModel<AvatarRenderState> {
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

    @TargetHandler(
            mixin = "com.zigythebird.bendable_cuboids.mixin.playeranim.PlayerModelMixin_playerAnim",
            name = "bc$updatePart",
            prefix = "handler"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("RETURN"), remap = false)
    private void palMore$applyVectorBend(AvatarAnimManager emote, ModelPart part, PlayerAnimBone bone, CallbackInfo originalCi, CallbackInfo ci) {
        if (part == this.head || !(bone instanceof PalMoreBendHolder holder)) {
            return;
        }

        if (!holder.palMore$hasBendVectorOverride()) {
            holder.palMore$setBend(bone.bend, 0.0F, 0.0F);
        }

        PalMoreBendableCuboids.applyVectorBend(part, holder);

        if (part == this.body) {
            PalMoreBendableCuboids.applyVectorBend(jacket, holder);
        } else if (part == this.rightArm) {
            PalMoreBendableCuboids.applyVectorBend(rightSleeve, holder);
        } else if (part == this.leftArm) {
            PalMoreBendableCuboids.applyVectorBend(leftSleeve, holder);
        } else if (part == this.rightLeg) {
            PalMoreBendableCuboids.applyVectorBend(rightPants, holder);
        } else if (part == this.leftLeg) {
            PalMoreBendableCuboids.applyVectorBend(leftPants, holder);
        }
    }

    @TargetHandler(
            mixin = "com.zigythebird.bendable_cuboids.mixin.playeranim.PlayerModelMixin_playerAnim",
            name = "bc$resetAll",
            prefix = "handler"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("RETURN"), remap = false)
    private void palMore$clearVectorBendState(AvatarAnimManager emote, CallbackInfo originalCi, CallbackInfo ci) {
        PalMoreBendableCuboids.clearPartState(body);
        PalMoreBendableCuboids.clearPartState(leftArm);
        PalMoreBendableCuboids.clearPartState(rightArm);
        PalMoreBendableCuboids.clearPartState(leftLeg);
        PalMoreBendableCuboids.clearPartState(rightLeg);
        PalMoreBendableCuboids.clearPartState(jacket);
        PalMoreBendableCuboids.clearPartState(leftSleeve);
        PalMoreBendableCuboids.clearPartState(rightSleeve);
        PalMoreBendableCuboids.clearPartState(leftPants);
        PalMoreBendableCuboids.clearPartState(rightPants);
    }
}
