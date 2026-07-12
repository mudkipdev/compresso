//? if fabric {
package dev.mudkip.compresso;

import net.fabricmc.api.ClientModInitializer;

public final class CompressoFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Compresso.init();
    }
}
//?}
