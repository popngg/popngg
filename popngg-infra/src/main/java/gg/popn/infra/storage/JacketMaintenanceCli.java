package gg.popn.infra.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.infra.notification.S3JacketStorageAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Run with API writes paused; dry-run by default. Retains every prior S3 object. */
public final class JacketMaintenanceCli {
    private JacketMaintenanceCli() {}
    record Song(long id, String hash, String url, int version) {}
    record Change(long songId, String oldUrl, String newUrl, String source, String status) {}

    public static void main(String[] args) throws Exception {
        try (var connection = DriverManager.getConnection(required("SPRING_DATASOURCE_URL"),
                    required("SPRING_DATASOURCE_USERNAME"), required("SPRING_DATASOURCE_PASSWORD"));
             var s3 = S3Client.builder().region(Region.of(env("AWS_REGION", "ap-northeast-2"))).build()) {
            run(args, connection, s3, required("AWS_S3_BUCKET"), env("AWS_S3_JACKET_PREFIX", "static"),
                    env("AWS_CLOUDFRONT_URL", "https://static.popn.gg"));
        }
    }

    static void run(String[] args, java.sql.Connection connection, S3Client s3,
                    String bucket, String prefix, String publicUrl) throws Exception {
        if (args.length < 2 || args.length > 4)
            throw new IllegalArgumentException("Usage: JacketMaintenanceCli official-directory report.json [--normalize-existing] [--apply]");
        var flags = Set.copyOf(Arrays.asList(args).subList(2, args.length));
        if (flags.stream().anyMatch(f -> !f.equals("--apply") && !f.equals("--normalize-existing")))
            throw new IllegalArgumentException("Unknown maintenance option");
        boolean apply = flags.contains("--apply"), normalizeExisting = flags.contains("--normalize-existing");
        Path directory = Path.of(args[0]).toAbsolutePath(), report = Path.of(args[1]).toAbsolutePath();
        Set<Long> excluded = new HashSet<>();
        Path exclusions = directory.resolve("excluded-song-ids.txt");
        if (Files.exists(exclusions)) for (String line : Files.readAllLines(exclusions))
            if (!line.isBlank()) excluded.add(Long.parseLong(line.trim()));
        var mapper = new ObjectMapper();
        var expected = new java.util.HashMap<Long, String>();
        Path snapshot = directory.resolve("catalog-before.json");
        if (Files.exists(snapshot)) for (var song : mapper.readTree(snapshot.toFile()))
            expected.put(song.path("songId").asLong(), song.path("songHash").asText());
        var changes = new ArrayList<Change>();
        try {
            var storage = new S3JacketStorageAdapter(s3, bucket, prefix, publicUrl);
            List<Song> songs = new ArrayList<>();
            try (var query = connection.prepareStatement("SELECT song_id,song_hash,jacket_url,version FROM songs ORDER BY song_id");
                 var rows = query.executeQuery()) {
                while (rows.next()) songs.add(new Song(rows.getLong(1), rows.getString(2), rows.getString(3), rows.getInt(4)));
            }
            for (var song : songs) {
                if (excluded.contains(song.id()) || song.url() == null || song.url().isBlank()) continue;
                Path official = directory.resolve(song.id() + ".png");
                boolean replacement = Files.exists(official);
                if (!replacement && !normalizeExisting) continue;
                if (replacement && !song.hash().equals(expected.get(song.id())))
                    throw new IllegalStateException("Official image identity no longer matches reviewed song " + song.id());
                byte[] source = replacement ? Files.readAllBytes(official) : storage.readPng(song.url());
                byte[] png = JacketImageProcessor.normalize(source);
                if (!replacement && Arrays.equals(source, png)) continue;
                String predicted = publicUrl.replaceAll("/+$", "")
                        + "/" + song.hash() + "-jacket-" + S3JacketStorageAdapter.digest(png) + ".png";
                if (predicted.equals(song.url())) continue;
                if (apply) {
                    String uploaded = storage.uploadVersionedPng(song.hash(), png);
                    if (!uploaded.equals(predicted)) throw new IllegalStateException("Jacket URL mismatch");
                    try (var update = connection.prepareStatement("""
                            UPDATE songs SET jacket_url=?, updated_at=CURRENT_TIMESTAMP
                             WHERE song_id=? AND BINARY song_hash=BINARY ? AND BINARY jacket_url=BINARY ?
                            """)) {
                        update.setString(1, uploaded); update.setLong(2, song.id());
                        update.setString(3, song.hash()); update.setString(4, song.url());
                        if (update.executeUpdate() != 1) throw new IllegalStateException("Jacket changed concurrently: song " + song.id());
                    }
                }
                changes.add(new Change(song.id(), song.url(), predicted, replacement ? "official" : "existing",
                        apply ? "applied" : "planned"));
            }
        } finally {
            Files.createDirectories(report.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(report.toFile(), changes);
        }
        System.out.println((apply ? "Applied " : "Planned ") + changes.size() + " jacket changes. Report: " + report);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }
}
