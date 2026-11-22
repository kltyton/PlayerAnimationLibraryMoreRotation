package com.kltyton.playeranimationlibrarymorerotation.platform;

import java.util.ServiceLoader;

public final class Services {
    public static final PalMorePlatform PLATFORM = load(PalMorePlatform.class);

    private Services() {
    }

    private static <T> T load(Class<T> type) {
        return ServiceLoader.load(type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing service implementation for " + type.getName()));
    }
}
