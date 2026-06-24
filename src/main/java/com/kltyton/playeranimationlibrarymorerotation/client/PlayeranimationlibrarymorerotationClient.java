package com.kltyton.playeranimationlibrarymorerotation.client;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreBendResources;
import com.kltyton.playeranimationlibrarymorerotation.client.network.PalMoreClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class PlayeranimationlibrarymorerotationClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PalMoreBendResources.registerClientReloadListener();
        PalMoreClientNetworking.registerReceivers();
    }
}
