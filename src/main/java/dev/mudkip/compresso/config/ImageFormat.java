package dev.mudkip.compresso.config;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum ImageFormat implements StringRepresentable {
    PNG("png", "png", "PNG", true, false),
    JPEG("jpeg", "jpg", "JPEG", false, true),
    WEBP("webp", "webp", "WebP", true, true),
    AVIF("avif", "avif", "AVIF", true, true);

    public static final StringRepresentable.EnumCodec<ImageFormat> CODEC = StringRepresentable.fromEnum(ImageFormat::values);

    private final String serializedName;
    private final String extension;
    private final String displayName;
    private final boolean supportsLossless;
    private final boolean supportsLossy;

    ImageFormat(String serializedName, String extension, String displayName, boolean supportsLossless, boolean supportsLossy) {
        this.serializedName = serializedName;
        this.extension = extension;
        this.displayName = displayName;
        this.supportsLossless = supportsLossless;
        this.supportsLossy = supportsLossy;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.serializedName;
    }

    public String getExtension() {
        return this.extension;
    }

    public boolean supportsLossless() {
        return this.supportsLossless;
    }

    public boolean supportsLossy() {
        return this.supportsLossy;
    }

    public CompressionMode resolveMode(CompressionMode requested) {
        if (requested == CompressionMode.LOSSLESS && !this.supportsLossless) {
            return CompressionMode.LOSSY;
        }

        if (requested == CompressionMode.LOSSY && !this.supportsLossy) {
            return CompressionMode.LOSSLESS;
        }

        return requested;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
