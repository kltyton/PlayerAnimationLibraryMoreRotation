package com.kltyton.playeranimationlibrarymorerotation.network.payload;

import com.kltyton.playeranimationlibrarymorerotation.PalMoreAnimationController;
import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Clientbound player animation playback instruction for one player entity in the current dimension.
 *
 * @param avatarEntityId target player entity id
 * @param animationId    animation id to play
 * @param stop           true stops the current triggered animation and ignores animationId
 * @param controllerId   client-side PalMore playback controller id
 */
public record PlayerAnimationPayload(
        int avatarEntityId,
        ResourceLocation animationId,
        boolean stop,
        ResourceLocation controllerId
) {
    public static final ResourceLocation ID = Playeranimationlibrarymorerotation.id("player_animation");

    public PlayerAnimationPayload(int avatarEntityId, ResourceLocation animationId, boolean stop) {
        this(avatarEntityId, animationId, stop, PalMoreAnimationController.DEFAULT_ID);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.avatarEntityId);
        buf.writeResourceLocation(this.animationId);
        buf.writeBoolean(this.stop);
        buf.writeResourceLocation(this.controllerId);
    }

    public static PlayerAnimationPayload decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ResourceLocation animationId = buf.readResourceLocation();
        boolean stop = buf.readBoolean();
        ResourceLocation controllerId = buf.readResourceLocation();
        return new PlayerAnimationPayload(entityId, animationId, stop, controllerId);
    }
}
