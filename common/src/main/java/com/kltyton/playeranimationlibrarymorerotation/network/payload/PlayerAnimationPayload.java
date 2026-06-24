package com.kltyton.playeranimationlibrarymorerotation.network.payload;

import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Clientbound PAL playback instruction for one player entity in the current dimension.
 *
 * @param avatarEntityId target Avatar entity id
 * @param animationId    PAL animation id to play
 * @param stop           true stops the current triggered animation and ignores animationId
 */
public record PlayerAnimationPayload(int avatarEntityId, Identifier animationId, boolean stop)
        implements CustomPacketPayload {
    public static final Type<PlayerAnimationPayload> ID =
            new Type<>(Playeranimationlibrarymorerotation.id("player_animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerAnimationPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerAnimationPayload::avatarEntityId,
            Identifier.STREAM_CODEC, PlayerAnimationPayload::animationId,
            ByteBufCodecs.BOOL, PlayerAnimationPayload::stop,
            PlayerAnimationPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
