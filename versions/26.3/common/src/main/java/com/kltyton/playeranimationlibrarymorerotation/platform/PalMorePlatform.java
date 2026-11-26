package com.kltyton.playeranimationlibrarymorerotation.platform;

import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-specific bridge for sending this library's playback payload.
 */
public interface PalMorePlatform {
    void sendPlayerAnimation(ServerPlayer viewer, PlayerAnimationPayload payload);
}
