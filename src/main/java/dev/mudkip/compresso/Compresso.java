package dev.mudkip.compresso;

import dev.mudkip.compresso.config.ScreenshotConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Compresso implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Compresso");

    @Override
    public void onInitializeClient() {
        ScreenshotConfig.load();
    }
}
