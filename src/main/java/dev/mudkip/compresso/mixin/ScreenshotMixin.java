package dev.mudkip.compresso.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.mudkip.compresso.compression.ScreenshotProcessor;
import dev.mudkip.compresso.config.ScreenshotConfig;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public class ScreenshotMixin {
    @Inject(
            method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void compresso$compress(File workDir, String forceName, RenderTarget target, int downscaleFactor, Consumer<Component> callback, CallbackInfo ci) {
        if (!ScreenshotConfig.get().enabled) {
            return;
        }

        ScreenshotProcessor.grab(workDir, forceName, target, downscaleFactor, callback);
        ci.cancel();
    }
}
