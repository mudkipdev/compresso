package dev.mudkip.compresso.compression;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import dev.mudkip.compresso.Compresso;
import dev.mudkip.compresso.config.ImageFormat;
import dev.mudkip.compresso.config.ScreenshotConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;*/
//?}
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

public final class ScreenshotProcessor {
    private ScreenshotProcessor() {

    }

    public static void grab(File workingDirectory, @Nullable String forceName, RenderTarget target,
            //? if >=1.21.11 {
            int downscaleFactor,
            //?}
            Consumer<Component> callback) {
        //? if >=1.21.11 {
        Screenshot.takeScreenshot(target, downscaleFactor, nativeImage -> {
            BufferedImage image;

            try (nativeImage) {
                image = toBufferedImage(nativeImage);
            }

            dispatch(workingDirectory, forceName, image, callback);
        });
        //?} else {
        /*var nativeImage = Screenshot.takeScreenshot(target);
        BufferedImage image;

        try (nativeImage) {
            image = toBufferedImage(nativeImage);
        }

        dispatch(workingDirectory, forceName, image, callback);
        *///?}
    }

    private static void dispatch(File workingDirectory, @Nullable String forceName, BufferedImage image, Consumer<Component> callback) {
        var window = Minecraft.getInstance().getWindow();
        var contentScaleX = new float[1];
        var contentScaleY = new float[1];
        //? if >=1.21.11 {
        GLFW.glfwGetWindowContentScale(window.handle(), contentScaleX, contentScaleY);
        //?} else {
        /*GLFW.glfwGetWindowContentScale(window.getWindow(), contentScaleX, contentScaleY);
        *///?}
        var scaleX = contentScaleX[0] > 0 ? contentScaleX[0] : 1;
        var scaleY = contentScaleY[0] > 0 ? contentScaleY[0] : 1;
        var screenWidth = Math.round(window.getWidth() / scaleX);
        var screenHeight = Math.round(window.getHeight() / scaleY);
        Util.ioPool().execute(() -> process(workingDirectory, forceName, image, screenWidth, screenHeight, ScreenshotConfig.get(), callback));
    }

    private static void process(File workingDirectory, @Nullable String forceName, BufferedImage source, int screenWidth, int screenHeight, ScreenshotConfig config, Consumer<Component> callback) {
        try {
            var image = config.matchWindowResolution ? matchWindow(source, screenWidth, screenHeight) : source;
            var directory = workingDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR);
            Files.createDirectories(directory);
            var target = uniqueFile(directory, forceName, Encoders.resolveFormat(config));

            String backend;

            try {
                backend = Encoders.encode(image, config.format, config, target);
            } catch (Throwable encodeError) {
                Compresso.LOGGER.warn("Encoder failed for {}, falling back to PNG", config.format, encodeError);
                Files.deleteIfExists(target);
                target = uniqueFile(directory, forceName, ImageFormat.PNG);
                Encoders.encode(image, ImageFormat.PNG, config, target);
                backend = "png (fallback)";
            }

            var fileBytes = Files.size(target);
            Compresso.LOGGER.info("Saved screenshot {} ({} bytes, {})", target.getFileName(), fileBytes, backend);
            callback.accept(savedMessage(target.toFile(), fileBytes));
        } catch (Throwable throwable) {
            Compresso.LOGGER.error("Failed to save compressed screenshot", throwable);
            callback.accept(Component.translatable("screenshot.failure", String.valueOf(throwable.getMessage())));
        }
    }

    private static BufferedImage toBufferedImage(NativeImage nativeImage) {
        var width = nativeImage.getWidth();
        var height = nativeImage.getHeight();
        //? if >=1.21.11 {
        var pixels = nativeImage.getPixels();
        //?} else {
        /*var pixels = nativeImage.makePixelArray();
        *///?}
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private static BufferedImage matchWindow(BufferedImage image, int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0 || image.getWidth() <= screenWidth) {
            return image;
        }

        var scaled = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
        var graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(image, 0, 0, screenWidth, screenHeight, null);
        graphics.dispose();
        return scaled;
    }

    private static Path uniqueFile(Path directory, @Nullable String forceName, ImageFormat format) {
        var extension = format.getExtension();

        if (forceName != null) {
            return directory.resolve(stripExtension(forceName) + "." + extension);
        }

        var stamp = Util.getFilenameFormattedDateTime();
        var count = 1;

        while (true) {
            var suffix = count == 1 ? "" : "_" + count;
            var candidate = directory.resolve(stamp + suffix + "." + extension);

            if (!Files.exists(candidate)) {
                return candidate;
            }

            count++;
        }
    }

    private static Component savedMessage(File file, long fileBytes) {
        Component link = Component.literal(file.getName())
                .withStyle(ChatFormatting.UNDERLINE)
                //? if >=1.21.11 {
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(file.getAbsoluteFile())));
                //?} else {
                /*.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath())));
                *///?}

        return Component.translatable("screenshot.success", link)
                .append(Component.literal(" (" + formatBytes(fileBytes) + ")").withStyle(ChatFormatting.GRAY));
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KiB", bytes / 1024.0);
        }

        return String.format(Locale.ROOT, "%.2f MiB", bytes / (1024.0 * 1024.0));
    }

    private static String stripExtension(String name) {
        var dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
