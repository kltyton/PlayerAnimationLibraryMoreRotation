package com.kltyton.playeranimationlibrarymorerotation.client;

import com.kltyton.playeranimationlibrarymorerotation.PalMoreFirstPersonOptions;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Client-side API for directly operating PAL's default player animation controller.
 */
public final class PalMoreClientAnimations {
    private PalMoreClientAnimations() {
    }

    public static boolean playLocal(Avatar avatar, Identifier animationId) {
        return playLocal(avatar, animationId, PalMoreFirstPersonOptions.DISABLED);
    }

    public static boolean playLocal(Avatar avatar, Identifier animationId, PalMoreFirstPersonOptions firstPersonOptions) {
        Objects.requireNonNull(avatar, "avatar");
        Objects.requireNonNull(animationId, "animationId");

        PlayerAnimationController controller = getController(avatar);
        if (controller == null) {
            return false;
        }

        applyFirstPersonOptions(controller, firstPersonOptions);
        return controller.triggerAnimation(animationId);
    }

    public static boolean stopLocal(Avatar avatar) {
        Objects.requireNonNull(avatar, "avatar");

        PlayerAnimationController controller = getController(avatar);
        if (controller == null) {
            return false;
        }

        controller.setFirstPersonMode(FirstPersonMode.NONE);
        boolean stoppedTriggeredAnimation = controller.stopTriggeredAnimation();
        controller.stop();
        return stoppedTriggeredAnimation;
    }

    public static @Nullable PlayerAnimationController getController(Avatar avatar) {
        IAnimation animationLayer = PlayerAnimationAccess.getPlayerAnimationLayer(
                avatar,
                PlayerAnimLibMod.ANIMATION_LAYER_ID
        );
        return animationLayer instanceof PlayerAnimationController controller ? controller : null;
    }

    private static void applyFirstPersonOptions(
            PlayerAnimationController controller,
            @Nullable PalMoreFirstPersonOptions options
    ) {
        if (options == null || !options.enabled()) {
            controller.setFirstPersonMode(FirstPersonMode.NONE);
            return;
        }

        controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        controller.setFirstPersonConfiguration(
                new FirstPersonConfiguration()
                        .setShowArmor(options.showArmor())
                        .setShowRightArm(options.showRightArm())
                        .setShowLeftArm(options.showLeftArm())
                        .setShowRightItem(options.showRightItem())
                        .setShowLeftItem(options.showLeftItem())
        );
    }
}
