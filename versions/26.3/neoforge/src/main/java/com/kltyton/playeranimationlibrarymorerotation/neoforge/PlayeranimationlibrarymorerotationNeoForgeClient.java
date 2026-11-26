package com.kltyton.playeranimationlibrarymorerotation.neoforge;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.client.network.PalMoreClientPayloadHandler;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class PlayeranimationlibrarymorerotationNeoForgeClient {
    private PlayeranimationlibrarymorerotationNeoForgeClient() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(PlayeranimationlibrarymorerotationNeoForgeClient::registerClientPayloadHandlers);
        eventBus.addListener(PlayeranimationlibrarymorerotationNeoForgeClient::addClientReloadListeners);
    }

    private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(PlayerAnimationPayload.ID, (payload, context) ->
                PalMoreClientPayloadHandler.handle(Minecraft.getInstance(), payload));
    }

    private static void addClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(PalMoreBendResources.KEY, new PalMoreBendResources());
        event.addDependency(PlayerAnimResources.KEY, PalMoreBendResources.KEY);
    }
}
