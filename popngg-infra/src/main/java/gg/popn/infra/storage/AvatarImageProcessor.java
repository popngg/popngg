package gg.popn.infra.storage;

import gg.popn.application.account.port.out.AvatarProcessingException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class AvatarImageProcessor {
    static final int OUTPUT_SIZE = 256;
    private static final long MAX_SOURCE_PIXELS = 16_777_216L;

    public byte[] thumbnail(byte[] source) {
        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(source))) {
            if (input == null) throw new AvatarProcessingException("Avatar image is empty.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new AvatarProcessingException("Unsupported avatar image.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0
                        || (long) width * height > MAX_SOURCE_PIXELS) {
                    throw new AvatarProcessingException("Avatar dimensions are invalid.");
                }
                return encodePng(resize(reader.read(0)));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AvatarProcessingException processingException) {
                throw processingException;
            }
            throw new AvatarProcessingException("Avatar image processing failed.", exception);
        }
    }

    private static BufferedImage resize(BufferedImage source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        BufferedImage target = new BufferedImage(
                OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE,
                    x, y, x + side, y + side, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) {
            throw new AvatarProcessingException("PNG encoder is unavailable.");
        }
        return output.toByteArray();
    }
}
