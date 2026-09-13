package com.kltyton.playeranimationlibrarymorerotation.neoforge.platform;

import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import com.kltyton.playeranimationlibrarymorerotation.platform.PalMorePlatform;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgePalMorePlatform implements PalMorePlatform {
    @Override
    public void sendPlayerAnimation(ServerPlayer viewer, PlayerAnimationPayload payload) {
        PacketDistributor.sendToPlayer(viewer, payload);
    }
}
