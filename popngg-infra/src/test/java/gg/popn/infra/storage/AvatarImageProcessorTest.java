package gg.popn.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gg.popn.application.account.port.out.AvatarProcessingException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AvatarImageProcessorTest {
    private final AvatarImageProcessor processor = new AvatarImageProcessor();

    @Test
    void centerCropsAndCreatesA256PixelPng() throws Exception {
        BufferedImage source = new BufferedImage(512, 256, BufferedImage.TYPE_INT_RGB);
        var graphics = source.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 128, 256);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(128, 0, 256, 256);
        graphics.setColor(Color.GREEN);
        graphics.fillRect(384, 0, 128, 256);
        graphics.dispose();
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(source, "png", encoded);

        byte[] thumbnail = processor.thumbnail(encoded.toByteArray());
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(thumbnail));

        assertThat(result.getWidth()).isEqualTo(256);
        assertThat(result.getHeight()).isEqualTo(256);
        assertThat(result.getRGB(128, 128)).isEqualTo(Color.BLUE.getRGB());
        assertThat(thumbnail).startsWith(
                (byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    void rejectsUndecodableInput() {
        assertThatThrownBy(() -> processor.thumbnail(new byte[]{1, 2, 3}))
                .isInstanceOf(AvatarProcessingException.class);
    }

    @Test
    void decodesWebpInputAndWritesPng() throws Exception {
        byte[] webp = Base64.getDecoder().decode(
                "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEALmk0mk0iIiIiIgBoSygABc6zbAAA");

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(
                processor.thumbnail(webp)));

        assertThat(result.getWidth()).isEqualTo(256);
        assertThat(result.getHeight()).isEqualTo(256);
    }
}
