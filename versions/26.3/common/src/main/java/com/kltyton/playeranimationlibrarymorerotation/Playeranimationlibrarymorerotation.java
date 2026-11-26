package com.kltyton.playeranimationlibrarymorerotation;

import net.minecraft.resources.Identifier;

public final class Playeranimationlibrarymorerotation {
    public static final String MOD_ID = "playeranimationlibrarymorerotation";

    private Playeranimationlibrarymorerotation() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
