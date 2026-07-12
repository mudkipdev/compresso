package dev.mudkip.compresso;

import dev.mudkip.compresso.config.ScreenshotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Compresso {
    public static final Logger LOGGER = LoggerFactory.getLogger("Compresso");

    private Compresso() {

    }

    public static void init() {
        ScreenshotConfig.load();
    }
}
