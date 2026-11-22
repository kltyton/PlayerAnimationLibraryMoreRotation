package com.kltyton.playeranimationlibrarymorerotation.forge;

import com.kltyton.playeranimationlibrarymorerotation.Playeranimationlibrarymorerotation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Playeranimationlibrarymorerotation.MOD_ID)
public class PlayeranimationlibrarymorerotationForge {
    public PlayeranimationlibrarymorerotationForge() {
        PalMoreForgeNetworking.register();
        if (FMLEnvironment.dist.isClient()) {
            PlayeranimationlibrarymorerotationForgeClient.init();
        }
    }
}
