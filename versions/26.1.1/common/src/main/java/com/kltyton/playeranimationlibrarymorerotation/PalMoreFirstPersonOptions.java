package com.kltyton.playeranimationlibrarymorerotation;

/**
 * Optional first-person rendering flags for local PAL playback.
 */
public record PalMoreFirstPersonOptions(
        boolean enabled,
        boolean showArmor,
        boolean showRightArm,
        boolean showLeftArm,
        boolean showRightItem,
        boolean showLeftItem
) {
    public static final PalMoreFirstPersonOptions DISABLED =
            new PalMoreFirstPersonOptions(false, false, false, false, false, false);

    public static final PalMoreFirstPersonOptions SHOW_ARMS_AND_ITEMS =
            new PalMoreFirstPersonOptions(true, false, true, true, true, true);

    public static final PalMoreFirstPersonOptions SHOW_ARMS_ITEMS_AND_ARMOR =
            new PalMoreFirstPersonOptions(true, true, true, true, true, true);
}
