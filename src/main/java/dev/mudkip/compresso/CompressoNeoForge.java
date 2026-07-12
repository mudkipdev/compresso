//? if neoforge {
/*package dev.mudkip.compresso;

import dev.mudkip.compresso.config.ConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = "compresso", dist = Dist.CLIENT)
public final class CompressoNeoForge {
    public CompressoNeoForge(ModContainer container) {
        Compresso.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> ConfigScreen.create(parent));
    }
}
*///?}
