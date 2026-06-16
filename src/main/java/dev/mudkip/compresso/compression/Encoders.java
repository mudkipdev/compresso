package dev.mudkip.compresso.compression;

import com.luciad.imageio.webp.WebPWriteParam;
import dev.mudkip.compresso.config.CompressionMode;
import dev.mudkip.compresso.config.ImageFormat;
import dev.mudkip.compresso.config.ScreenshotConfig;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class Encoders {
    private static final Map<String, Boolean> INSTALLED = new ConcurrentHashMap<>();

    private Encoders() {
    }

    private enum Backend { PNG, IMAGEIO, EXTERNAL }

    public static ImageFormat resolveFormat(ScreenshotConfig config) {
        return backend(config.format, config) == null ? ImageFormat.PNG : config.format;
    }

    public static String encode(BufferedImage image, ImageFormat format, ScreenshotConfig config, Path target) throws IOException, InterruptedException {
        var backend = backend(format, config);
        switch (backend == null ? Backend.PNG : backend) {
            case PNG -> Png.write(image, config.pngCompressionLevel, target);
            case IMAGEIO -> writeImageIo(image, format, config, target);
            case EXTERNAL -> runExternal(image, format, config, target);
        }
        return backend == null ? "png" : backend.name().toLowerCase();
    }

    private static Backend backend(ImageFormat format, ScreenshotConfig config) {
        if (format == ImageFormat.AVIF) {
            return externalAvailable(config, format) ? Backend.EXTERNAL : null;
        }

        var preferExternal = config.useExternalEncoders && config.preferExternalWhenAvailable;
        if (preferExternal && externalAvailable(config, format)) {
            return Backend.EXTERNAL;
        }
        if (internalAvailable(format)) {
            return format == ImageFormat.PNG ? Backend.PNG : Backend.IMAGEIO;
        }
        if (externalAvailable(config, format)) {
            return Backend.EXTERNAL;
        }
        return null;
    }

    private static boolean lossless(ImageFormat format, ScreenshotConfig config) {
        return format.resolveMode(config.compressionMode) == CompressionMode.LOSSLESS;
    }

    private static boolean internalAvailable(ImageFormat format) {
        return switch (format) {
            case PNG -> true;
            case JPEG -> hasImageIoWriter("jpeg");
            case WEBP -> hasImageIoWriter("webp");
            case AVIF -> false;
        };
    }

    private static boolean externalAvailable(ScreenshotConfig config, ImageFormat format) {
        if (format != ImageFormat.AVIF && !config.useExternalEncoders) {
            return false;
        }
        return isInstalled(commandFor(config, format));
    }

    private static void writeImageIo(BufferedImage image, ImageFormat format, ScreenshotConfig config, Path target) throws IOException {
        var quality = config.quality / 100.0F;

        if (format == ImageFormat.JPEG) {
            var writer = firstImageIoWriter("jpeg");
            var param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            write(writer, toRgb(image), param, target);
        } else {
            var writer = firstImageIoWriter("webp");
            var param = new WebPWriteParam(writer.getLocale());
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            var type = lossless(format, config) ? WebPWriteParam.LOSSLESS_COMPRESSION : WebPWriteParam.LOSSY_COMPRESSION;
            param.setCompressionType(param.getCompressionTypes()[type]);
            param.setCompressionQuality(quality);
            write(writer, image, param, target);
        }
    }

    private static void write(ImageWriter writer, BufferedImage image, ImageWriteParam param, Path target) throws IOException {
        try (var outputStream = Files.newOutputStream(target); var stream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private static BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }

        var rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = rgb.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    private static boolean hasImageIoWriter(String formatName) {
        try {
            return ImageIO.getImageWritersByFormatName(formatName).hasNext();
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static ImageWriter firstImageIoWriter(String formatName) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            throw new IOException("No ImageIO writer registered for " + formatName);
        }
        return writers.next();
    }

    private static void runExternal(BufferedImage image, ImageFormat format, ScreenshotConfig config, Path target) throws IOException, InterruptedException {
        var temporaryPng = Files.createTempFile("compresso-", ".png");
        try {
            Png.write(image, 1, temporaryPng);
            var command = buildCommand(format, config, temporaryPng, target);
            runProcess(command);
            if (!Files.exists(target) || Files.size(target) == 0) {
                throw new IOException("External encoder produced no output: " + String.join(" ", command));
            }
        } finally {
            Files.deleteIfExists(temporaryPng);
        }
    }

    private static List<String> buildCommand(ImageFormat format, ScreenshotConfig config, Path source, Path target) {
        var lossless = lossless(format, config);
        var command = new ArrayList<String>();

        switch (format) {
            case AVIF -> {
                command.add(config.avifEncoderPath);
                command.add("--speed");
                command.add(Integer.toString(config.avifSpeed));

                if (lossless) {
                    command.add("--lossless");
                } else {
                    var quantizer = Math.round((100 - config.quality) / 100.0F * 63);
                    command.add("--min");
                    command.add(Integer.toString(quantizer));
                    command.add("--max");
                    command.add(Integer.toString(quantizer));
                }

                command.add(source.toString());
                command.add(target.toString());
            }

            case WEBP -> {
                command.add(config.webpEncoderPath);

                if (lossless) {
                    command.add("-lossless");
                } else {
                    command.add("-q");
                    command.add(Integer.toString(config.quality));
                }

                command.add("-m");
                command.add(Integer.toString(config.webpMethod));
                command.add(source.toString());
                command.add("-o");
                command.add(target.toString());
            }

            case PNG -> {
                command.add(config.pngOptimizerPath);
                command.add("-o");
                command.add(config.pngCompressionLevel >= 9 ? "max" : Integer.toString(Math.min(config.pngCompressionLevel, 6)));
                command.add("--strip");
                command.add("safe");
                command.add("--out");
                command.add(target.toString());
                command.add(source.toString());
            }

            case JPEG -> {
                command.add(config.jpegEncoderPath);
                command.add("-quality");
                command.add(Integer.toString(config.quality));
                command.add("-outfile");
                command.add(target.toString());
                command.add(source.toString());
            }
        }

        return command;
    }

    private static void runProcess(List<String> command) throws IOException, InterruptedException {
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var output = process.getInputStream().readAllBytes();

        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("External encoder timed out: " + command.getFirst());
        }

        if (process.exitValue() != 0) {
            throw new IOException(command.getFirst() + " exited with " + process.exitValue() + ": " + new String(output).strip());
        }
    }

    private static String commandFor(ScreenshotConfig config, ImageFormat format) {
        return switch (format) {
            case AVIF -> config.avifEncoderPath;
            case WEBP -> config.webpEncoderPath;
            case PNG -> config.pngOptimizerPath;
            case JPEG -> config.jpegEncoderPath;
        };
    }

    public static boolean isInstalled(String command) {
        return INSTALLED.computeIfAbsent(command, Encoders::probe);
    }

    public static void clearCache() {
        INSTALLED.clear();
    }

    private static boolean probe(String command) {
        try {
            var process = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            process.waitFor(5, TimeUnit.SECONDS);
            process.destroyForcibly();
            return true;
        } catch (IOException notFound) {
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
