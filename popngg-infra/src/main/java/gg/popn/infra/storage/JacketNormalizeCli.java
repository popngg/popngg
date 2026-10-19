package gg.popn.infra.storage;

import java.nio.file.Files;
import java.nio.file.Path;

/** Offline normalization of a directory of reviewed image assets. */
public final class JacketNormalizeCli {
    private JacketNormalizeCli() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: JacketNormalizeCli source-directory target-directory");
        Path target = Path.of(args[1]).toAbsolutePath();
        Files.createDirectories(target);
        try (var files = Files.list(Path.of(args[0]))) {
            for (var file : files.filter(p -> p.getFileName().toString().endsWith(".png")).toList())
                Files.write(target.resolve(file.getFileName()), JacketImageProcessor.normalize(Files.readAllBytes(file)));
        }
    }
}
