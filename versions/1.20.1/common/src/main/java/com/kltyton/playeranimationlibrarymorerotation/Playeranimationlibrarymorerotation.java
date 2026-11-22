package com.kltyton.playeranimationlibrarymorerotation;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Playeranimationlibrarymorerotation {
    public static final String MOD_ID = "playeranimationlibrarymorerotation";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Playeranimationlibrarymorerotation() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
