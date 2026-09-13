package com.kltyton.playeranimationlibrarymorerotation.client;

import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreFirstPersonOptions;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.event.EventResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side API for directly operating PAL's default player animation controller.
 */
public final class PalMoreClientAnimations {
    private static final Map<Identifier, PalMoreAnimationController> CONTROLLERS = new ConcurrentHashMap<>();

    static {
        registerController(PalMoreAnimationController.DEFAULT);
    }

    private PalMoreClientAnimations() {
    }

    public static PalMoreAnimationController registerController(PalMoreAnimationController controller) {
        Objects.requireNonNull(controller, "controller");
        CONTROLLERS.put(controller.id(), controller);
        return controller;
    }

    public static PalMoreAnimationController getRegisteredController(Identifier controllerId) {
        Objects.requireNonNull(controllerId, "controllerId");
        return CONTROLLERS.getOrDefault(controllerId, PalMoreAnimationController.DEFAULT);
    }

    public static boolean playLocal(Avatar avatar, Identifier animationId) {
        return playLocal(avatar, animationId, PalMoreFirstPersonOptions.DISABLED);
    }

    public static boolean playLocal(Avatar avatar, Identifier animationId, PalMoreFirstPersonOptions firstPersonOptions) {
        return playLocalInternal(avatar, animationId, firstPersonOptions, PalMoreAnimationController.DEFAULT);
    }

    public static PalMoreAnimationController playLocal(
            Avatar avatar,
            Identifier animationId,
            PalMoreAnimationController controller
    ) {
        playLocalInternal(avatar, animationId, PalMoreFirstPersonOptions.DISABLED, controller);
        return controller;
    }

    public static PalMoreAnimationController playLocal(Avatar avatar, Identifier animationId, Identifier controllerId) {
        PalMoreAnimationController controller = getRegisteredController(controllerId);
        playLocalInternal(avatar, animationId, PalMoreFirstPersonOptions.DISABLED, controller);
        return controller;
    }

    public static PalMoreAnimationController playLocal(
            Avatar avatar,
            Identifier animationId,
            PalMoreFirstPersonOptions firstPersonOptions,
            PalMoreAnimationController controller
    ) {
        playLocalInternal(avatar, animationId, firstPersonOptions, controller);
        return controller;
    }

    private static boolean playLocalInternal(
            Avatar avatar,
            Identifier animationId,
            @Nullable PalMoreFirstPersonOptions firstPersonOptions,
            PalMoreAnimationController playbackController
    ) {
        Objects.requireNonNull(avatar, "avatar");
        Objects.requireNonNull(animationId, "animationId");
        Objects.requireNonNull(playbackController, "playbackController");

        PlayerAnimationController controller = getController(avatar);
        if (controller == null) {
            return false;
        }

        applyPlaybackController(controller, playbackController);
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

    private static void applyPlaybackController(
            PlayerAnimationController controller,
            PalMoreAnimationController playbackController
    ) {
        controller.setCustomInstructionKeyframeHandler((animationTick, animationController, keyframeData, animationData) -> {
            PalMoreAnimationController.CustomInstructionKeyframeHandler handler =
                    playbackController.customInstructionKeyframeHandler();
            if (handler == null) {
                return EventResult.PASS;
            }

            return normalizeResult(handler.handle(new PalMoreAnimationController.CustomInstructionKeyframeContext(
                    animationTick,
                    controller.getAvatar(),
                    animationController,
                    keyframeData,
                    animationData
            )));
        });
        controller.setParticleKeyframeHandler((animationTick, animationController, keyframeData, animationData) -> {
            PalMoreAnimationController.ParticleKeyframeHandler handler = playbackController.particleKeyframeHandler();
            if (handler == null) {
                return EventResult.PASS;
            }

            return normalizeResult(handler.handle(new PalMoreAnimationController.ParticleKeyframeContext(
                    animationTick,
                    controller.getAvatar(),
                    animationController,
                    keyframeData,
                    animationData
            )));
        });
        controller.setSoundKeyframeHandler((animationTick, animationController, keyframeData, animationData) -> {
            PalMoreAnimationController.SoundKeyframeHandler handler = playbackController.soundKeyframeHandler();
            if (handler == null) {
                return EventResult.PASS;
            }

            return normalizeResult(handler.handle(new PalMoreAnimationController.SoundKeyframeContext(
                    animationTick,
                    controller.getAvatar(),
                    animationController,
                    keyframeData,
                    animationData
            )));
        });
    }

    private static EventResult normalizeResult(@Nullable EventResult result) {
        return result == null ? EventResult.PASS : result;
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
