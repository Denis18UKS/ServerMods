package dev.ingamecreator.core.content;

import java.util.Arrays;
import java.util.Locale;

public final class TextureAsset {
    private final String id;
    private final int width;
    private final int height;
    private final int[] pixels;

    public TextureAsset(String id, int width, int height, int[] pixels) {
        this.id = normalizeId(id);
        if (width < 1 || width > 128 || height < 1 || height > 128) throw new IllegalArgumentException("texture size must be 1..128");
        if (pixels == null || pixels.length != width * height) throw new IllegalArgumentException("pixel array size mismatch");
        this.width = width;
        this.height = height;
        this.pixels = Arrays.copyOf(pixels, pixels.length);
    }

    public static TextureAsset blank(String id, int width, int height) {
        return new TextureAsset(id, width, height, new int[width * height]);
    }

    public String id() { return id; }
    public int width() { return width; }
    public int height() { return height; }
    public int pixel(int x, int y) { return pixels[index(x, y)]; }
    public void setPixel(int x, int y, int argb) { pixels[index(x, y)] = argb; }
    public int[] pixels() { return Arrays.copyOf(pixels, pixels.length); }

    private int index(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) throw new IllegalArgumentException("pixel out of bounds");
        return y * width + x;
    }

    public static String normalizeId(String value) {
        if (value == null) throw new IllegalArgumentException("texture id is required");
        String id = value.trim().toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z][a-z0-9_-]{1,63}")) throw new IllegalArgumentException("invalid texture id: " + value);
        return id;
    }
}
