package com.kltyton.playeranimationlibrarymorerotation.network;

import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PalMoreNetworking {
    private static boolean registered;

    private PalMoreNetworking() {
    }

    public static void registerPayloads() {
        if (registered) {
            return;
        }

        PayloadTypeRegistry.clientboundPlay().register(
                PlayerAnimationPayload.ID,
                PlayerAnimationPayload.CODEC
        );
        registered = true;
    }
}
