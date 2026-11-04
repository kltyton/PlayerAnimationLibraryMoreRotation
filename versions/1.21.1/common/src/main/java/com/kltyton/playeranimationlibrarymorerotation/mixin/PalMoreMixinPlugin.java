package com.kltyton.playeranimationlibrarymorerotation.mixin;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Selects optional legacy rendering integrations while retaining all PAL extension mixins. */
public final class PalMoreMixinPlugin implements IMixinConfigPlugin {
    private boolean bendy;
    private boolean capture;

    @Override
    public void onLoad(String mixinPackage) {
        bendy = hasClass("io.github.kosmx.bendylib.MutableCuboid");
        capture = hasClass("com.kltyton.bedrockruntime.client.render.vanilla.VanillaModelCapture")
                && hasClass("com.kltyton.bedrockruntime.client.render.item.HeldItemCapture");
    }

    private static boolean hasClass(String name) {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode(name);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read optional integration class " + name, exception);
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".LegacyBendyCubeMixin")) {
            return bendy;
        }
        if (mixinClassName.contains(".compat.bendy.")) {
            return bendy && capture;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
