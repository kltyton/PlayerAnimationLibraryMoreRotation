package com.kltyton.playeranimationlibrarymorerotation;

import com.kltyton.playeranimationlibrarymorerotation.network.PalMoreNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class Playeranimationlibrarymorerotation implements ModInitializer {
    public static final String MOD_ID = "playeranimationlibrarymorerotation";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PalMoreNetworking.registerPayloads();
    }
}
