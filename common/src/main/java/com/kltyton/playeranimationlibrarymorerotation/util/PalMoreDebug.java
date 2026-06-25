package com.kltyton.playeranimationlibrarymorerotation.util;

import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PalMoreDebug {
    public static final boolean BEND_DEBUG = Boolean.parseBoolean(System.getProperty("palmorerotation.debugBend", "false"));

    private static final Set<String> ONCE_KEYS = ConcurrentHashMap.newKeySet();

    private PalMoreDebug() {
    }

    public static boolean shouldLog(@Nullable Identifier animationId) {
        if (!BEND_DEBUG) {
            return false;
        }
        if (animationId == null) {
            return true;
        }

        String path = animationId.getPath();
        return Playeranimationlibrarymorerotation.MOD_ID.equals(animationId.getNamespace())
                || path.contains("animation.unknown");
    }

    public static void info(String message, Object... args) {
        if (BEND_DEBUG) {
            PlayerAnimLib.LOGGER.info("[PalMore/BendDebug] " + message, args);
        }
    }

    public static void infoOnce(String key, String message, Object... args) {
        if (BEND_DEBUG && ONCE_KEYS.add(key)) {
            PlayerAnimLib.LOGGER.info("[PalMore/BendDebug] " + message, args);
        }
    }
}
