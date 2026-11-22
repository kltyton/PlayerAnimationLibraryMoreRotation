package com.kltyton.playeranimationlibrarymorerotation;

import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import com.kltyton.playeranimationlibrarymorerotation.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Common-side API for broadcasting player animations for a player on Minecraft 1.20.1.
 */
public final class PalMoreAnimations {
    private static final ResourceLocation EMPTY_ANIMATION = Playeranimationlibrarymorerotation.id("empty");

    private PalMoreAnimations() {
    }

    /**
     * Plays an already loaded animation on the given player for all same-dimension viewers
     * that understand this library's packet.
     */
    public static void play(ServerPlayer animatedPlayer, ResourceLocation animationId) {
        send(animatedPlayer, animationId, false);
    }

    /**
     * Plays an animation with a named client-side PalMore playback controller.
     *
     * <p>Only the controller id is sent to clients. Register the same controller id on the
     * physical client before playback if it has keyframe handlers.</p>
     */
    public static PalMoreAnimationController play(
            ServerPlayer animatedPlayer,
            ResourceLocation animationId,
            PalMoreAnimationController controller
    ) {
        send(animatedPlayer, animationId, false, controller);
        return controller;
    }

    public static PalMoreAnimationController play(
            ServerPlayer animatedPlayer,
            ResourceLocation animationId,
            ResourceLocation controllerId
    ) {
        return play(animatedPlayer, animationId, PalMoreAnimationController.create(controllerId));
    }

    /**
     * Stops the currently triggered animation on the given player for all same-dimension viewers
     * that understand this library's packet.
     */
    public static void stop(ServerPlayer animatedPlayer) {
        send(animatedPlayer, EMPTY_ANIMATION, true);
    }

    public static void send(ServerPlayer animatedPlayer, ResourceLocation animationId, boolean stop) {
        send(animatedPlayer, animationId, stop, PalMoreAnimationController.DEFAULT);
    }

    public static void send(
            ServerPlayer animatedPlayer,
            ResourceLocation animationId,
            boolean stop,
            PalMoreAnimationController controller
    ) {
        Objects.requireNonNull(animatedPlayer, "animatedPlayer");
        Objects.requireNonNull(animationId, "animationId");
        Objects.requireNonNull(controller, "controller");

        MinecraftServer server = animatedPlayer.level().getServer();
        if (server == null) {
            return;
        }

        PlayerAnimationPayload payload = new PlayerAnimationPayload(
                animatedPlayer.getId(),
                animationId,
                stop,
                controller.id()
        );
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer.level() == animatedPlayer.level()) {
                Services.PLATFORM.sendPlayerAnimation(viewer, payload);
            }
        }
    }
}
