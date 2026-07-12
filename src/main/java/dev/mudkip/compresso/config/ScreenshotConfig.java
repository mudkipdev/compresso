package dev.mudkip.compresso.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.mudkip.compresso.Compresso;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ScreenshotConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            //? if fabric {
            net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("compresso.json");
            //?} else {
            /*net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("compresso.json");*/
            //?}
    private static ScreenshotConfig instance;

    public boolean enabled;
    public ImageFormat format;
    public CompressionMode compressionMode;
    public int quality;
    public boolean matchWindowResolution;

    public int pngCompressionLevel;
    public int webpMethod;
    public int avifSpeed;

    public boolean useExternalEncoders;
    public boolean preferExternalWhenAvailable;
    public String avifEncoderPath;
    public String webpEncoderPath;
    public String pngOptimizerPath;
    public String jpegEncoderPath;

    public ScreenshotConfig() {
        this.enabled = true;
        this.format = ImageFormat.WEBP;
        this.compressionMode = CompressionMode.LOSSLESS;
        this.quality = 85;
        this.matchWindowResolution = true;
        this.pngCompressionLevel = 9;
        this.webpMethod = 4;
        this.avifSpeed = 6;
        this.avifEncoderPath = "avifenc";
        this.webpEncoderPath = "cwebp";
        this.pngOptimizerPath = "oxipng";
        this.jpegEncoderPath = "cjpeg";
    }

    public static ScreenshotConfig get() {
        if (instance == null) {
            instance = load();
        }

        return instance;
    }

    public static ScreenshotConfig load() {
        ScreenshotConfig config;

        if (Files.exists(CONFIG_PATH)) {
            try {
                config = GSON.fromJson(Files.readString(CONFIG_PATH), ScreenshotConfig.class);

                if (config == null) {
                    config = new ScreenshotConfig();
                }
            } catch (Exception exception) {
                Compresso.LOGGER.warn("Failed to read config, using defaults", exception);
                config = new ScreenshotConfig();
            }
        } else {
            config = new ScreenshotConfig();
        }

        config.sanitize();
        instance = config;
        config.save();
        return config;
    }

    public void save() {
        this.sanitize();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException exception) {
            Compresso.LOGGER.warn("Failed to write config", exception);
        }
    }

    public void sanitize() {
        if (this.format == null) {
            this.format = ImageFormat.PNG;
        }

        if (this.compressionMode == null) {
            this.compressionMode = CompressionMode.LOSSLESS;
        }

        this.quality = Math.clamp(this.quality, 0, 100);
        this.pngCompressionLevel = Math.clamp(this.pngCompressionLevel, 0, 9);
        this.webpMethod = Math.clamp(this.webpMethod, 0, 6);
        this.avifSpeed = Math.clamp(this.avifSpeed, 0, 10);
        this.avifEncoderPath = orDefault(this.avifEncoderPath, "avifenc");
        this.webpEncoderPath = orDefault(this.webpEncoderPath, "cwebp");
        this.pngOptimizerPath = orDefault(this.pngOptimizerPath, "oxipng");
        this.jpegEncoderPath = orDefault(this.jpegEncoderPath, "cjpeg");
    }

    public CompressionMode effectiveMode() {
        return this.format.resolveMode(this.compressionMode);
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
