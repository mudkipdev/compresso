package dev.mudkip.compresso.config;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.mudkip.compresso.compression.Encoders;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ConfigScreen {
    private static final ScreenshotConfig DEFAULT = new ScreenshotConfig();

    private ConfigScreen() {

    }

    public static Screen create(@Nullable Screen parent) {
        var config = ScreenshotConfig.get();

        var enabled = booleanOption("compresso.option.enabled", () -> config.enabled, v -> config.enabled = v, DEFAULT.enabled);
        var format = enumOption("compresso.option.format", ImageFormat.class, v -> Component.literal(v.getDisplayName()), () -> config.format, value -> config.format = value, DEFAULT.format);
        var mode = enumOption("compresso.option.mode", CompressionMode.class, v -> Component.translatable(v.getTranslationKey()), () -> config.compressionMode, v -> config.compressionMode = v, DEFAULT.compressionMode);
        var quality = integerOption("compresso.option.quality", 0, 100, 5, () -> config.quality, v -> config.quality = v, DEFAULT.quality);
        var matchWindow = booleanOption("compresso.option.match_window", () -> config.matchWindowResolution, v -> config.matchWindowResolution = v, DEFAULT.matchWindowResolution);

        var pngLevel = integerOption("compresso.option.png_level", 0, 9, 1, () -> config.pngCompressionLevel, v -> config.pngCompressionLevel = v, DEFAULT.pngCompressionLevel);
        var webpMethod = integerOption("compresso.option.webp_method", 0, 6, 1, () -> config.webpMethod, v -> config.webpMethod = v, DEFAULT.webpMethod);
        var avifSpeed = integerOption("compresso.option.avif_speed", 0, 10, 1, () -> config.avifSpeed, v -> config.avifSpeed = v, DEFAULT.avifSpeed);

        var useExternal = booleanOption("compresso.option.use_external", () -> config.useExternalEncoders, v -> config.useExternalEncoders = v, DEFAULT.useExternalEncoders);
        var preferExternal = booleanOption("compresso.option.prefer_external", () -> config.preferExternalWhenAvailable, v -> config.preferExternalWhenAvailable = v, DEFAULT.preferExternalWhenAvailable);

        quality.setAvailable(config.effectiveMode() == CompressionMode.LOSSY);
        preferExternal.setAvailable(config.useExternalEncoders);

        mode.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) {
                quality.setAvailable(option.pendingValue() == CompressionMode.LOSSY);
            }
        });

        useExternal.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) {
                preferExternal.setAvailable(option.pendingValue());
            }
        });

        var options = List.of(enabled, format, mode, quality, matchWindow, pngLevel, webpMethod, avifSpeed, useExternal, preferExternal);

        Runnable rebuild = () -> {
            options.forEach(Option::applyValue);
            var minecraft = Minecraft.getInstance();
            //? if >=26.2 {
            /*minecraft.execute(() -> minecraft.setScreenAndShow(create(parent)));*/
            //?} else {
            minecraft.execute(() -> minecraft.setScreen(create(parent)));
            //?}
        };

        format.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) {
                rebuild.run();
            }
        });

        var general = OptionGroup.createBuilder()
                .name(Component.translatable("compresso.section.general"))
                .option(enabled).option(format);

        if (config.format.supportsLossless() && config.format.supportsLossy()) {
            general.option(mode);
        }

        if (config.format.supportsLossy()) {
            general.option(quality);
        }

        general.option(matchWindow);

        var category = ConfigCategory.createBuilder()
                .name(Component.literal("Compresso"))
                .group(general.build());

        if (config.format == ImageFormat.PNG) {
            category.group(OptionGroup.createBuilder()
                    .name(Component.literal(ImageFormat.PNG.getDisplayName()))
                    .option(pngLevel)
                    .build());
        } else if (config.format == ImageFormat.WEBP) {
            category.group(OptionGroup.createBuilder()
                    .name(Component.literal(ImageFormat.WEBP.getDisplayName()))
                    .option(webpMethod)
                    .build());
        } else if (config.format == ImageFormat.AVIF) {
            category.group(OptionGroup.createBuilder()
                    .name(Component.literal(ImageFormat.AVIF.getDisplayName()))
                    .option(avifSpeed)
                    .build());
        }

        category.group(OptionGroup.createBuilder()
                .name(Component.translatable("compresso.section.external"))
                .option(useExternal)
                .option(preferExternal)
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("compresso.option.check_external"))
                        .action((screen, button) -> {
                            Encoders.clearCache();
                            rebuild.run();
                        })
                        .build())
                .option(encoderStatus(config.avifEncoderPath))
                .option(encoderStatus(config.webpEncoderPath))
                .option(encoderStatus(config.pngOptimizerPath))
                .option(encoderStatus(config.jpegEncoderPath))
                .build());

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Compresso"))
                .category(category.build())
                .save(config::save)
                .build()
                .generateScreen(parent);
    }

    private static LabelOption encoderStatus(String command) {
        var installed = Encoders.isInstalled(command);
        return LabelOption.create(Component.translatable(installed ? "compresso.external.found" : "compresso.external.missing", command)
                .withStyle(installed ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static Option<Boolean> booleanOption(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultValue) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable(key))
                .description(OptionDescription.of(Component.translatable(key + ".description")))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> integerOption(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultValue) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable(key))
                .description(OptionDescription.of(Component.translatable(key + ".description")))
                .binding(defaultValue, getter, setter)
                .controller(option -> IntegerSliderControllerBuilder.create(option).range(min, max).step(step))
                .build();
    }

    private static <E extends Enum<E>> Option<E> enumOption(String key, Class<E> type, Function<E, Component> formatter, Supplier<E> getter, Consumer<E> setter, E defaultValue) {
        return Option.<E>createBuilder()
                .name(Component.translatable(key))
                .description(OptionDescription.of(Component.translatable(key + ".description")))
                .binding(defaultValue, getter, setter)
                .controller(option -> EnumControllerBuilder.create(option).enumClass(type).formatValue(formatter::apply))
                .build();
    }
}
