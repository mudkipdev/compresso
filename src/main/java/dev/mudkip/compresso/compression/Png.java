package dev.mudkip.compresso.compression;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class Png {
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int CHANNELS = 3;

    private Png() {

    }

    public static void write(BufferedImage image, int level, Path target) throws IOException {
        var width = image.getWidth();
        var height = image.getHeight();
        var pixels = image.getRGB(0, 0, width, height, null, 0, width);

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target))) {
            out.write(SIGNATURE);
            writeIhdr(out, width, height);
            writeIdat(out, pixels, width, height, Math.clamp(level, 0, 9));
            writeChunk(out, "IEND", new byte[0]);
        }
    }

    private static void writeIhdr(OutputStream out, int width, int height) throws IOException {
        var data = new byte[13];
        writeInt(data, 0, width);
        writeInt(data, 4, height);
        data[8] = 8;
        data[9] = 2;
        writeChunk(out, "IHDR", data);
    }

    private static void writeIdat(OutputStream out, int[] pixels, int width, int height, int level) throws IOException {
        var stride = width * CHANNELS;
        var current = new byte[stride];
        var previous = new byte[stride];
        var candidate = new byte[5][stride];

        var deflater = new Deflater(level);
        var rawIdat = new ByteArrayOutputStream();

        try (var deflated = new DeflaterOutputStream(rawIdat, deflater, 8192)) {
            for (var y = 0; y < height; y++) {
                fillScanline(current, pixels, y, width);
                var filterType = chooseFilter(current, previous, candidate);
                deflated.write(filterType);
                deflated.write(candidate[filterType], 0, stride);
                var swap = previous;
                previous = current;
                current = swap;
            }
        } finally {
            deflater.end();
        }

        writeChunk(out, "IDAT", rawIdat.toByteArray());
    }

    private static void fillScanline(byte[] dest, int[] pixels, int y, int width) {
        var row = y * width;
        var index = 0;

        for (var x = 0; x < width; x++) {
            var argb = pixels[row + x];
            dest[index++] = (byte) (argb >> 16);
            dest[index++] = (byte) (argb >> 8);
            dest[index++] = (byte) argb;
        }
    }

    private static int chooseFilter(byte[] current, byte[] previous, byte[][] candidate) {
        var best = 0;
        var bestScore = Long.MAX_VALUE;

        for (var type = 0; type < 5; type++) {
            applyFilter(type, current, previous, candidate[type]);
            var score = absSum(candidate[type]);

            if (score < bestScore) {
                bestScore = score;
                best = type;
            }
        }

        return best;
    }

    private static void applyFilter(int type, byte[] current, byte[] previous, byte[] out) {
        var length = current.length;

        switch (type) {
            case 0 -> System.arraycopy(current, 0, out, 0, length);

            case 1 -> {
                for (var i = 0; i < length; i++) {
                    var a = i >= CHANNELS ? (current[i - CHANNELS] & 0xFF) : 0;
                    out[i] = (byte) ((current[i] & 0xFF) - a);
                }
            }

            case 2 -> {
                for (var i = 0; i < length; i++) {
                    out[i] = (byte) ((current[i] & 0xFF) - (previous[i] & 0xFF));
                }
            }

            case 3 -> {
                for (var i = 0; i < length; i++) {
                    var a = i >= CHANNELS ? (current[i - CHANNELS] & 0xFF) : 0;
                    var b = previous[i] & 0xFF;
                    out[i] = (byte) ((current[i] & 0xFF) - ((a + b) >> 1));
                }
            }

            case 4 -> {
                for (var i = 0; i < length; i++) {
                    var a = i >= CHANNELS ? (current[i - CHANNELS] & 0xFF) : 0;
                    var b = previous[i] & 0xFF;
                    var c = i >= CHANNELS ? (previous[i - CHANNELS] & 0xFF) : 0;
                    out[i] = (byte) ((current[i] & 0xFF) - paeth(a, b, c));
                }
            }

            default -> throw new IllegalArgumentException("Unknown filter " + type);
        }
    }

    private static int paeth(int a, int b, int c) {
        var p = a + b - c;
        var pa = Math.abs(p - a);
        var pb = Math.abs(p - b);
        var pc = Math.abs(p - c);

        if (pa <= pb && pa <= pc) {
            return a;
        }

        return pb <= pc ? b : c;
    }

    private static long absSum(byte[] data) {
        long sum = 0;

        for (var value : data) {
            sum += Math.abs(value);
        }

        return sum;
    }

    private static void writeChunk(OutputStream out, String type, byte[] data) throws IOException {
        var header = new byte[4];
        writeInt(header, 0, data.length);
        out.write(header);
        var typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.write(typeBytes);
        out.write(data);
        var crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        var crcBytes = new byte[4];
        writeInt(crcBytes, 0, (int) crc.getValue());
        out.write(crcBytes);
    }

    private static void writeInt(byte[] dest, int offset, int value) {
        dest[offset] = (byte) (value >>> 24);
        dest[offset + 1] = (byte) (value >>> 16);
        dest[offset + 2] = (byte) (value >>> 8);
        dest[offset + 3] = (byte) value;
    }
}
