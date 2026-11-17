package gg.popn.infra.storage;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;

class JacketImageProcessorTest {
    @Test
    void fitsPortraitWithoutStretchingOrCroppingAndPreservesTransparency() throws Exception {
        var source = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);
        var g = source.createGraphics();
        g.setColor(Color.RED); g.fillRect(0, 0, 100, 200); g.dispose();
        var image = ImageIO.read(new ByteArrayInputStream(JacketImageProcessor.normalize(encode(source, "png"))));
        assertThat(image.getWidth()).isEqualTo(244);
        assertThat(image.getHeight()).isEqualTo(58);
        assertThat(image.getRGB(0, 29) >>> 24).isZero();
        assertThat(image.getRGB(122, 0)).isEqualTo(Color.RED.getRGB());
        assertThat(image.getRGB(122, 57)).isEqualTo(Color.RED.getRGB());
        assertThat(image.getRGB(106, 29) >>> 24).isZero();
    }

    @Test
    void preservesOfficialPngBytesAndNormalizesJpeg() throws Exception {
        byte[] original = encode(new BufferedImage(244, 58, BufferedImage.TYPE_INT_ARGB), "png");
        assertThat(JacketImageProcessor.normalize(original)).isEqualTo(original);
        byte[] jpg = encode(new BufferedImage(488, 116, BufferedImage.TYPE_INT_RGB), "jpeg");
        byte[] result = JacketImageProcessor.normalize(jpg);
        assertThat(result).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
        var image = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(image.getWidth()).isEqualTo(244);
        assertThat(image.getHeight()).isEqualTo(58);
        assertThat(JacketImageProcessor.normalize(result)).isEqualTo(result);
    }

    @Test
    void rejectsCorruptLargeFilesAndExcessiveDimensionsBeforeDecoding() throws Exception {
        assertThatThrownBy(() -> JacketImageProcessor.normalize(new byte[]{1, 2, 3})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JacketImageProcessor.normalize(new byte[5 * 1024 * 1024 + 1])).isInstanceOf(IllegalArgumentException.class);
        byte[] image = encode(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png");
        ByteBuffer.wrap(image).putInt(16, 100_000).putInt(20, 100_000);
        assertThatThrownBy(() -> JacketImageProcessor.normalize(image)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해상도");
    }

    static byte[] encode(BufferedImage image, String format) throws Exception {
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image, format, bytes); return bytes.toByteArray();
    }
}
