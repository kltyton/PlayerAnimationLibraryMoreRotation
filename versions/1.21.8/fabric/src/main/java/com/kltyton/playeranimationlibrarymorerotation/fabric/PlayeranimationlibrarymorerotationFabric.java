package com.kltyton.playeranimationlibrarymorerotation.fabric;

import net.fabricmc.api.ModInitializer;

public class PlayeranimationlibrarymorerotationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PalMoreFabricNetworking.registerPayloads();
    }
}
