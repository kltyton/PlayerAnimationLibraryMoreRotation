package com.kltyton.playeranimationlibrarymorerotation.forge;

import com.kltyton.playeranimationlibrarymorerotation.client.PalMoreClientAnimations;
import com.kltyton.playeranimationlibrarymorerotation.client.network.PalMoreClientPayloadHandler;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.minecraft.client.Minecraft;

public final class PlayeranimationlibrarymorerotationForgeClient {
    private PlayeranimationlibrarymorerotationForgeClient() {
    }

    public static void init() {
        PalMoreClientAnimations.init();
    }

    public static void handlePayload(PlayerAnimationPayload payload) {
        PalMoreClientPayloadHandler.handle(Minecraft.getInstance(), payload);
    }
}
