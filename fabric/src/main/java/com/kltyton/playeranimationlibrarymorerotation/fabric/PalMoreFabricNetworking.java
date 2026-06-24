package com.kltyton.playeranimationlibrarymorerotation.fabric;

import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PalMoreFabricNetworking {
    private PalMoreFabricNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(PlayerAnimationPayload.ID, PlayerAnimationPayload.CODEC);
    }
}
