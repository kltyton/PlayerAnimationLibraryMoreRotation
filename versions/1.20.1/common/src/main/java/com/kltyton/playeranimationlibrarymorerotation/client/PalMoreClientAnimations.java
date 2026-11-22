package com.kltyton.playeranimationlibrarymorerotation.client;

import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController;
import com.kltyton.playeranimationlibrarymorerotation.PalMoreFirstPersonOptions;
import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-side API for playing and stopping animations through a dedicated playerAnimator layer.
 */
public final class PalMoreClientAnimations {
    public static final ResourceLocation ANIMATION_LAYER_ID = Playeranimationlibrarymorerotation.id("animation");
    private static final int ANIMATION_LAYER_PRIORITY = 1000;

    private static final AtomicBoolean LAYER_REGISTERED = new AtomicBoolean(false);
    private static final Map<ResourceLocation, PalMoreAnimationController> CONTROLLERS = new ConcurrentHashMap<>();

    static {
        registerController(PalMoreAnimationController.DEFAULT);
    }

    private PalMoreClientAnimations() {
    }

    /**
     * Registers this library's ModifierLayer factory with playerAnimator. Called during client init.
     */
    public static void init() {
        if (LAYER_REGISTERED.compareAndSet(false, true)) {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    ANIMATION_LAYER_ID,
                    ANIMATION_LAYER_PRIORITY,
                    player -> new ModifierLayer<>()
            );
        }
    }

    public static PalMoreAnimationController registerController(PalMoreAnimationController controller) {
        Objects.requireNonNull(controller, "controller");
        CONTROLLERS.put(controller.id(), controller);
        return controller;
    }

    public static PalMoreAnimationController getRegisteredController(ResourceLocation controllerId) {
        Objects.requireNonNull(controllerId, "controllerId");
        return CONTROLLERS.getOrDefault(controllerId, PalMoreAnimationController.DEFAULT);
    }

    public static boolean playLocal(AbstractClientPlayer avatar, ResourceLocation animationId) {
        return playLocal(avatar, animationId, PalMoreFirstPersonOptions.DISABLED);
    }

    public static boolean playLocal(
            AbstractClientPlayer avatar,
            ResourceLocation animationId,
            PalMoreFirstPersonOptions firstPersonOptions
    ) {
        return playLocalInternal(avatar, animationId, firstPersonOptions, PalMoreAnimationController.DEFAULT);
    }

    public static PalMoreAnimationController playLocal(
            AbstractClientPlayer avatar,
            ResourceLocation animationId,
            PalMoreAnimationController controller
    ) {
        playLocalInternal(avatar, animationId, PalMoreFirstPersonOptions.DISABLED, controller);
        return controller;
    }

    public static PalMoreAnimationController playLocal(
            AbstractClientPlayer avatar,
            ResourceLocation animationId,
            ResourceLocation controllerId
    ) {
        PalMoreAnimationController controller = getRegisteredController(controllerId);
        playLocalInternal(avatar, animationId, PalMoreFirstPersonOptions.DISABLED, controller);
        return controller;
    }

    public static PalMoreAnimationController playLocal(
            AbstractClientPlayer avatar,
            ResourceLocation animationId,
            PalMoreFirstPersonOptions firstPersonOptions,
            PalMoreAnimationController controller
    ) {
        playLocalInternal(avatar, animationId, firstPersonOptions, controller);
        return controller;
    }

    private static boolean playLocalInternal(
            AbstractClientPlayer avatar,
            ResourceLocation animationId,
            @Nullable PalMoreFirstPersonOptions firstPersonOptions,
            PalMoreAnimationController playbackController
    ) {
        Objects.requireNonNull(avatar, "avatar");
        Objects.requireNonNull(animationId, "animationId");
        Objects.requireNonNull(playbackController, "playbackController");

        ModifierLayer<IAnimation> layer = getLayer(avatar);
        if (layer == null) {
            return false;
        }

        KeyframeAnimation animation = lookupAnimation(animationId);
        if (animation == null) {
            return false;
        }

        PalMoreKeyframePlayer player = new PalMoreKeyframePlayer(avatar, animationId, animation, playbackController);
        applyFirstPersonOptions(player, firstPersonOptions);
        layer.setAnimation(player);
        return true;
    }

    public static boolean stopLocal(AbstractClientPlayer avatar) {
        Objects.requireNonNull(avatar, "avatar");

        ModifierLayer<IAnimation> layer = getLayer(avatar);
        if (layer == null) {
            return false;
        }

        boolean hadActive = layer.isActive();
        layer.setAnimation(null);
        return hadActive;
    }

    @SuppressWarnings("unchecked")
    public static @Nullable ModifierLayer<IAnimation> getLayer(AbstractClientPlayer avatar) {
        IAnimation animationLayer = PlayerAnimationAccess.getPlayerAssociatedData(avatar).get(ANIMATION_LAYER_ID);
        if (animationLayer instanceof ModifierLayer<?> modifierLayer) {
            return (ModifierLayer<IAnimation>) modifierLayer;
        }
        return null;
    }

    private static @Nullable KeyframeAnimation lookupAnimation(ResourceLocation animationId) {
        KeyframeAnimation direct = PlayerAnimationRegistry.getAnimation(animationId);
        if (direct != null) {
            return direct;
        }
        String lowerPath = animationId.getPath().toLowerCase(Locale.ROOT);
        if (!lowerPath.equals(animationId.getPath())) {
            return PlayerAnimationRegistry.getAnimation(new ResourceLocation(animationId.getNamespace(), lowerPath));
        }
        return null;
    }

    private static void applyFirstPersonOptions(
            KeyframeAnimationPlayer player,
            @Nullable PalMoreFirstPersonOptions options
    ) {
        if (options == null || !options.enabled()) {
            player.setFirstPersonMode(FirstPersonMode.NONE);
            return;
        }

        player.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        // playerAnimator 1.20.1 FirstPersonConfiguration exposes arm/item toggles only (no showArmor setter).
        player.setFirstPersonConfiguration(
                new FirstPersonConfiguration()
                        .setShowRightArm(options.showRightArm())
                        .setShowLeftArm(options.showLeftArm())
                        .setShowRightItem(options.showRightItem())
                        .setShowLeftItem(options.showLeftItem())
        );
    }
}
