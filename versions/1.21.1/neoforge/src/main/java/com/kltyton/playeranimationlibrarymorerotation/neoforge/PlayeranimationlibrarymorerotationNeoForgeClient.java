package com.kltyton.playeranimationlibrarymorerotation.neoforge;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.client.network.PalMoreClientPayloadHandler;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

public final class PlayeranimationlibrarymorerotationNeoForgeClient {
    private PlayeranimationlibrarymorerotationNeoForgeClient() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(PlayeranimationlibrarymorerotationNeoForgeClient::addClientReloadListeners);
    }

    static void handlePayload(PlayerAnimationPayload payload) {
        PalMoreClientPayloadHandler.handle(Minecraft.getInstance(), payload);
    }

    private static void addClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PalMoreBendResources());
    }
}
