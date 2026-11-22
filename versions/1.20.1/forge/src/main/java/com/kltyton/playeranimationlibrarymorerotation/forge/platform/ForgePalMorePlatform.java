package com.kltyton.playeranimationlibrarymorerotation.forge.platform;

import com.kltyton.playeranimationlibrarymorerotation.forge.PalMoreForgeNetworking;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import com.kltyton.playeranimationlibrarymorerotation.platform.PalMorePlatform;
import net.minecraft.server.level.ServerPlayer;

public final class ForgePalMorePlatform implements PalMorePlatform {
    @Override
    public void sendPlayerAnimation(ServerPlayer viewer, PlayerAnimationPayload payload) {
        PalMoreForgeNetworking.send(viewer, payload);
    }
}
