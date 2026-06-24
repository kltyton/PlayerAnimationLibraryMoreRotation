package com.kltyton.playeranimationlibrarymorerotation.fabric.platform;

import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import com.kltyton.playeranimationlibrarymorerotation.platform.PalMorePlatform;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPalMorePlatform implements PalMorePlatform {
    @Override
    public void sendPlayerAnimation(ServerPlayer viewer, PlayerAnimationPayload payload) {
        if (ServerPlayNetworking.canSend(viewer, PlayerAnimationPayload.ID)) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }
}
