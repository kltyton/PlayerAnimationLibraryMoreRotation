package com.kltyton.playeranimationlibrarymorerotation.mixin;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreAnimationResolver;
import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendyCuboids;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendState;
import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.impl.IMutableModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies sidecar 3-axis bend rotation, bend-local position/scale, and bone scale
 * to player model parts and their overlay layers after playerAnimator's PlayerModelMixin
 * (priority 2000) and vanilla copyFrom calls have completed.
 */
@Mixin(value = PlayerModel.class, priority = 2500)
public abstract class PlayerModelExtendedAnimMixin<T extends LivingEntity> extends HumanoidModel<T> {
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

    protected PlayerModelExtendedAnimMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void palMore$resetPartScales(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        palMore$resetScale(this.head, this.hat);
        palMore$resetScale(this.body, this.jacket);
        palMore$resetScale(this.rightArm, this.rightSleeve);
        palMore$resetScale(this.leftArm, this.leftSleeve);
        palMore$resetScale(this.rightLeg, this.rightPants);
        palMore$resetScale(this.leftLeg, this.leftPants);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void palMore$applyExtendedBendAndScale(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        AnimationProcessor processor = null;
        if ((Object) this instanceof IMutableModel mutable && mutable.getEmoteSupplier() != null) {
            processor = mutable.getEmoteSupplier().get();
        }

        if (processor == null || !processor.isActive()) {
            palMore$clearPart(this.body, this.jacket);
            palMore$clearPart(this.rightArm, this.rightSleeve);
            palMore$clearPart(this.leftArm, this.leftSleeve);
            palMore$clearPart(this.rightLeg, this.rightPants);
            palMore$clearPart(this.leftLeg, this.leftPants);
            return;
        }

        palMore$applyScale(processor, "head", this.head, this.hat);
        palMore$applyScale(processor, "torso", this.body, this.jacket);
        palMore$applyScale(processor, "rightArm", this.rightArm, this.rightSleeve);
        palMore$applyScale(processor, "leftArm", this.leftArm, this.leftSleeve);
        palMore$applyScale(processor, "rightLeg", this.rightLeg, this.rightPants);
        palMore$applyScale(processor, "leftLeg", this.leftLeg, this.leftPants);

        palMore$applyBend(processor, "torso", this.body, this.jacket);
        palMore$applyBend(processor, "rightArm", this.rightArm, this.rightSleeve);
        palMore$applyBend(processor, "leftArm", this.leftArm, this.leftSleeve);
        palMore$applyBend(processor, "rightLeg", this.rightLeg, this.rightPants);
        palMore$applyBend(processor, "leftLeg", this.leftLeg, this.leftPants);
    }

    @Unique
    private static void palMore$resetScale(ModelPart main, ModelPart overlay) {
        main.xScale = 1.0F;
        main.yScale = 1.0F;
        main.zScale = 1.0F;
        overlay.xScale = 1.0F;
        overlay.yScale = 1.0F;
        overlay.zScale = 1.0F;
    }

    @Unique
    private static void palMore$applyScale(
            AnimationProcessor processor,
            String boneName,
            ModelPart main,
            ModelPart overlay
    ) {
        Vector3f scale = PalMoreAnimationResolver.resolveScale(processor, boneName);
        if (scale.x != 1.0F || scale.y != 1.0F || scale.z != 1.0F) {
            main.xScale = scale.x;
            main.yScale = scale.y;
            main.zScale = scale.z;
            overlay.xScale = scale.x;
            overlay.yScale = scale.y;
            overlay.zScale = scale.z;
        }
    }

    @Unique
    private static void palMore$applyBend(
            AnimationProcessor processor,
            String boneName,
            ModelPart main,
            ModelPart overlay
    ) {
        PalMoreBendState state = PalMoreAnimationResolver.resolveBend(processor, boneName);
        PalMoreBendyCuboids.applyVectorBend(main, state);
        PalMoreBendyCuboids.applyVectorBend(overlay, state);
    }

    @Unique
    private static void palMore$clearPart(ModelPart main, ModelPart overlay) {
        PalMoreBendyCuboids.clearPartState(main);
        PalMoreBendyCuboids.clearPartState(overlay);
    }
}
