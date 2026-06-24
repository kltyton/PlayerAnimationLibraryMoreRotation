package com.kltyton.playeranimationlibrarymorerotation.fabric.client;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.client.network.PalMoreClientPayloadHandler;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

public class PlayeranimationlibrarymorerotationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        loader.registerReloadListener(PalMoreBendResources.KEY, new PalMoreBendResources());
        loader.addListenerOrdering(PlayerAnimResources.KEY, PalMoreBendResources.KEY);

        ClientPlayNetworking.registerGlobalReceiver(PlayerAnimationPayload.ID, (payload, context) ->
                context.client().execute(() -> PalMoreClientPayloadHandler.handle(context.client(), payload)));
    }
}
