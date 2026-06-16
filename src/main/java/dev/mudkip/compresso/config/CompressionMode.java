package dev.mudkip.compresso.config;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum CompressionMode implements StringRepresentable {
    LOSSLESS("lossless"),
    LOSSY("lossy");

    public static final StringRepresentable.EnumCodec<CompressionMode> CODEC = StringRepresentable.fromEnum(CompressionMode::values);

    private final String serializedName;

    CompressionMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.serializedName;
    }

    public String getTranslationKey() {
        return "compresso.option.mode." + this.serializedName;
    }
}
