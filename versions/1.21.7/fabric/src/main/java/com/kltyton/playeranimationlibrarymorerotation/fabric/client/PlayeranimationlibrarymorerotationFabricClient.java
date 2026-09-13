package com.kltyton.playeranimationlibrarymorerotation.fabric.client;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.client.network.PalMoreClientPayloadHandler;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.Collection;
import java.util.List;

public class PlayeranimationlibrarymorerotationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PalMoreBendResources resources = new PalMoreBendResources();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return PalMoreBendResources.KEY;
            }

            @Override
            public Collection<ResourceLocation> getFabricDependencies() {
                return List.of(PlayerAnimResources.KEY);
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                resources.onResourceManagerReload(manager);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayerAnimationPayload.ID, (payload, context) ->
                context.client().execute(() -> PalMoreClientPayloadHandler.handle(context.client(), payload)));
    }
}
