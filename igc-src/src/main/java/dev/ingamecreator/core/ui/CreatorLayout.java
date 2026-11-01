package dev.ingamecreator.core.ui;

public record CreatorLayout(
        int width,
        int height,
        int topHeight,
        int statusHeight,
        int sidebarWidth,
        int paletteWidth,
        int contentX,
        int contentTop,
        int contentBottom,
        int paletteX,
        int canvasX,
        int canvasWidth,
        int canvasHeight) {

    public static CreatorLayout compute(int width, int height) {
        if (width < 320 || height < 200) throw new IllegalArgumentException("window is too small");
        int top = clamp(height / 18, 30, 38);
        int status = clamp(height / 28, 18, 22);
        int side = clamp(width / 7, 96, 160);
        int margin = clamp(width / 100, 8, 18);
        int contentX = side + margin;
        int contentTop = top + 10;
        int contentBottom = height - status - 8;
        int available = Math.max(240, width - contentX - margin);
        int palette = clamp(available / 4, 140, 240);
        int paletteX = contentX;
        int canvasX = paletteX + palette + 8;
        int canvasWidth = Math.max(220, width - canvasX - 8);
        int canvasHeight = Math.max(120, contentBottom - contentTop);
        return new CreatorLayout(width, height, top, status, side, palette, contentX, contentTop, contentBottom,
                paletteX, canvasX, canvasWidth, canvasHeight);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
