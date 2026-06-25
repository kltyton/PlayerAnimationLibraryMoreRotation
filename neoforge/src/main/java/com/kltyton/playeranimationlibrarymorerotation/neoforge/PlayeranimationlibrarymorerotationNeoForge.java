package com.kltyton.playeranimationlibrarymorerotation.neoforge;

import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import com.kltyton.playeranimationlibrarymorerotation.config.PalMoreConfig;
import com.kltyton.playeranimationlibrarymorerotation.network.payload.PlayerAnimationPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Playeranimationlibrarymorerotation.MOD_ID)
public class PlayeranimationlibrarymorerotationNeoForge {
    public PlayeranimationlibrarymorerotationNeoForge(IEventBus eventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, PalMoreConfig.CLIENT_SPEC, PalMoreConfig.CLIENT_CONFIG_FILE);
        eventBus.addListener(this::registerPayloads);
        if (FMLEnvironment.getDist().isClient()) {
            PlayeranimationlibrarymorerotationNeoForgeClient.register(eventBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .optional()
                .playToClient(PlayerAnimationPayload.ID, PlayerAnimationPayload.CODEC);
    }
}
