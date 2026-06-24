package com.kltyton.playeranimationlibrarymorerotation.client.network;

import com.kltyton.playeranimationlibrarymorerotation.client.PalMoreClientAnimations;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;

public final class PalMoreClientNetworking {
    private static boolean registered;

    private PalMoreClientNetworking() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }

        ClientPlayNetworking.registerGlobalReceiver(PlayerAnimationPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> handlePlayerAnimation(client, payload));
        });
        registered = true;
    }

    private static void handlePlayerAnimation(Minecraft client, PlayerAnimationPayload payload) {
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
