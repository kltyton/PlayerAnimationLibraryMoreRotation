package com.kltyton.playeranimationlibrarymorerotation.fabric.client;

import com.kltyton.playeranimationlibrarymorerotation.client.PalMoreClientAnimations;
import com.kltyton.playeranimationlibrarymorerotation.client.network.PalMoreClientPayloadHandler;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class PlayeranimationlibrarymorerotationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PalMoreClientAnimations.init();
        ClientPlayNetworking.registerGlobalReceiver(
                PlayerAnimationPayload.ID,
                (client, handler, buf, responseSender) -> {
                    PlayerAnimationPayload payload = PlayerAnimationPayload.decode(buf);
                    client.execute(() -> PalMoreClientPayloadHandler.handle(client, payload));
                }
        );
    }
}
