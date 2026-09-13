package com.kltyton.playeranimationlibrarymorerotation;

import net.minecraft.resources.ResourceLocation;

public final class Playeranimationlibrarymorerotation {
    public static final String MOD_ID = "playeranimationlibrarymorerotation";

    private Playeranimationlibrarymorerotation() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
