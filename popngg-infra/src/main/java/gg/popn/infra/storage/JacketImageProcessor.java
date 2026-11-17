package gg.popn.infra.storage;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Official banners use a 244 x 58 canvas. Retain the full image and transparency. */
public final class JacketImageProcessor {
    public static final int WIDTH = 244;
    public static final int HEIGHT = 58;
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_777_216L;

    private JacketImageProcessor() {}

    public static byte[] normalize(byte[] source) {
        if (source == null || source.length == 0 || source.length > MAX_BYTES)
            throw new IllegalArgumentException("자켓 이미지는 5MB 이하여야 합니다.");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("올바른 자켓 이미지가 아닙니다.");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                // Check metadata before allocating the decoded raster.
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS)
                    throw new IllegalArgumentException("자켓 이미지 해상도가 너무 큽니다.");
                var decoded = reader.read(0);
                if (width == WIDTH && height == HEIGHT && reader.getFormatName().equalsIgnoreCase("png"))
                    return source;
                var canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                var graphics = canvas.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    double scale = Math.min((double) WIDTH / width, (double) HEIGHT / height);
                    int w = Math.max(1, (int) Math.round(width * scale));
                    int h = Math.max(1, (int) Math.round(height * scale));
                    graphics.drawImage(decoded, (WIDTH - w) / 2, (HEIGHT - h) / 2, w, h, null);
                } finally { graphics.dispose(); }
                var output = new ByteArrayOutputStream();
                if (!ImageIO.write(canvas, "png", output)) throw new IllegalArgumentException("PNG 변환에 실패했습니다.");
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException exception) {
            throw new IllegalArgumentException("자켓 이미지를 처리할 수 없습니다.", exception);
        }
    }
}
