package com.kltyton.playeranimationlibrarymorerotation;

import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Common-side API for broadcasting PlayerAnimationLibrary animations for a player.
 */
public final class PalMoreAnimations {
    private static final Identifier EMPTY_ANIMATION = Playeranimationlibrarymorerotation.id("empty");

    private PalMoreAnimations() {
    }

    /**
     * Plays an already loaded PAL animation on the given player for all same-dimension viewers
     * that understand this library's payload.
     */
    public static void play(ServerPlayer animatedPlayer, Identifier animationId) {
        send(animatedPlayer, animationId, false);
    }

    /**
     * Stops the currently triggered PAL animation on the given player for all same-dimension viewers
     * that understand this library's payload.
     */
    public static void stop(ServerPlayer animatedPlayer) {
        send(animatedPlayer, EMPTY_ANIMATION, true);
    }

    public static void send(ServerPlayer animatedPlayer, Identifier animationId, boolean stop) {
        Objects.requireNonNull(animatedPlayer, "animatedPlayer");
        Objects.requireNonNull(animationId, "animationId");

        MinecraftServer server = animatedPlayer.level().getServer();
        if (server == null) {
            return;
        }

        PlayerAnimationPayload payload = new PlayerAnimationPayload(animatedPlayer.getId(), animationId, stop);
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer.level() == animatedPlayer.level()
                    && ServerPlayNetworking.canSend(viewer, PlayerAnimationPayload.ID)) {
                ServerPlayNetworking.send(viewer, payload);
            }
        }
    }
}
