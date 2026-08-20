package gg.popn.domain.chart.model.field;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

/** Stable metadata fingerprint for catalog songs. Internal identity remains song_id. */
public final class SongHashGenerator {
    private SongHashGenerator() {
    }

    public static String generate(String genre, String title, String artist, int debutVersion) {
        String canonical = "popngg-song:v2\n"
                + "genre=" + normalize(genre) + "\n"
                + "title=" + normalize(title) + "\n"
                + "artist=" + normalize(artist) + "\n"
                + "debutVersion=" + debutVersion;
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFC)
                .trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
