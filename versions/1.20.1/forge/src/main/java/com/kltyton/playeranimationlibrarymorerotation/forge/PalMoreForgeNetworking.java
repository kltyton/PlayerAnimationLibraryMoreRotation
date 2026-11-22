package com.kltyton.playeranimationlibrarymorerotation.forge;

import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class PalMoreForgeNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            PlayerAnimationPayload.ID,
            () -> PROTOCOL_VERSION,
            version -> true,
            version -> true
    );

    private PalMoreForgeNetworking() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            CHANNEL.registerMessage(
                    0,
                    PlayerAnimationPayload.class,
                    PlayerAnimationPayload::encode,
                    PlayerAnimationPayload::decode,
                    PalMoreForgeNetworking::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT)
            );
        }
    }

    public static void send(ServerPlayer viewer, PlayerAnimationPayload payload) {
        if (CHANNEL.isRemotePresent(viewer.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), payload);
        }
    }

    private static void handle(PlayerAnimationPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                PlayeranimationlibrarymorerotationForgeClient.handlePayload(payload);
            }
        });
        context.setPacketHandled(true);
    }
}
