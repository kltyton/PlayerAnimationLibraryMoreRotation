package com.kltyton.playeranimationlibrarymorerotation.util;

import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import com.kltyton.playeranimationlibrarymorerotation.config.PalMoreConfig;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PalMoreDebug {
    public static final String RESOURCE = "resource";
    public static final String FRAME = "frame";
    public static final String COPY = "copy";
    public static final String RENDER = "render";
    public static final String ITEM = "item";

    private static final Set<String> ONCE_KEYS = ConcurrentHashMap.newKeySet();
    private static final Map<String, AtomicInteger> CATEGORY_COUNTS = new ConcurrentHashMap<>();

    private PalMoreDebug() {
    }

    public static boolean enabled() {
        return PalMoreConfig.debugEnabled();
    }

    public static boolean verbose() {
        return PalMoreConfig.debugVerbose();
    }

    public static boolean shouldLog(@Nullable Identifier animationId) {
        if (!enabled()) {
            return false;
        }
        if (PalMoreConfig.debugLogAllAnimations()) {
            return true;
        }
        if (animationId == null) {
            return true;
        }

        String id = animationId.toString().toLowerCase(Locale.ROOT);
        String namespace = animationId.getNamespace().toLowerCase(Locale.ROOT);
        String path = animationId.getPath().toLowerCase(Locale.ROOT);
        String filter = PalMoreConfig.debugAnimationFilter();
        if (!filter.isEmpty()) {
            for (String token : filter.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty() && (id.contains(trimmed) || namespace.contains(trimmed) || path.contains(trimmed))) {
                    return true;
                }
            }
        }

        return Playeranimationlibrarymorerotation.MOD_ID.equals(animationId.getNamespace())
                || path.contains("animation.unknown");
    }

    public static void info(String message, Object... args) {
        if (enabled()) {
            PlayerAnimLib.LOGGER.info("[PalMore/BendDebug] " + message, args);
        }
    }

    public static void infoLimited(String category, String message, Object... args) {
        if (enabled() && allow(category)) {
            PlayerAnimLib.LOGGER.info("[PalMore/BendDebug/{}] " + message, prepend(category, args));
        }
    }

    public static void verboseLimited(String category, String message, Object... args) {
        if (verbose() && allow(category)) {
            PlayerAnimLib.LOGGER.info("[PalMore/BendDebug/{}] " + message, prepend(category, args));
        }
    }

    public static void infoOnce(String key, String message, Object... args) {
        if (enabled() && ONCE_KEYS.add(key)) {
            PlayerAnimLib.LOGGER.info("[PalMore/BendDebug] " + message, args);
        }
    }

    private static boolean allow(String category) {
        int limit = switch (category) {
            case RESOURCE -> PalMoreConfig.resourceLogLimit();
            case FRAME -> PalMoreConfig.frameLogLimit();
            case COPY -> PalMoreConfig.copyLogLimit();
            case RENDER -> PalMoreConfig.renderLogLimit();
            case ITEM -> PalMoreConfig.itemLogLimit();
            default -> 1000;
        };
        if (limit == 0) {
            return true;
        }

        int count = CATEGORY_COUNTS.computeIfAbsent(category, ignored -> new AtomicInteger()).incrementAndGet();
        if (count == limit + 1) {
            PlayerAnimLib.LOGGER.info("[PalMore/BendDebug/{}] log limit {} reached; suppressing further messages in this category", category, limit);
        }
        return count <= limit;
    }

    private static Object[] prepend(String category, Object[] args) {
        Object[] result = new Object[args.length + 1];
        result[0] = category;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
