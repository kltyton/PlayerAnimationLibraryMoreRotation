package com.kltyton.playeranimationlibrarymorerotation.client.network;

import com.kltyton.playeranimationlibrarymorerotation.client.PalMoreClientAnimations;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;

public final class PalMoreClientPayloadHandler {
    private PalMoreClientPayloadHandler() {
    }

    public static void handle(Minecraft client, PlayerAnimationPayload payload) {
        if (client.level == null) {
            return;
        }

        Entity entity = client.level.getEntity(payload.avatarEntityId());
        if (entity instanceof Avatar avatar) {
            if (payload.stop()) {
                PalMoreClientAnimations.stopLocal(avatar);
            } else {
                PalMoreClientAnimations.playLocal(avatar, payload.animationId());
            }
        }
    }
}
