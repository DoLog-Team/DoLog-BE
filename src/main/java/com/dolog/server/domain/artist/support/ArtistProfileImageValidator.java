package com.dolog.server.domain.artist.support;

import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileImageInvalidException;
import com.sksamuel.scrimage.ImmutableImage;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ArtistProfileImageValidator {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final long HEIC_DECODE_TIMEOUT_SECONDS = 10;
    private static final String HEIC_EXTENSION = "heic";
    private static final String HEIC_CONTENT_TYPE = "image/heic";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            HEIC_EXTENSION
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            HEIC_CONTENT_TYPE
    );

    public ImmutableImage validateAndDecode(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String extension = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        boolean heic = HEIC_EXTENSION.equals(extension);
        if (file.getSize() > MAX_FILE_SIZE
                || !ALLOWED_EXTENSIONS.contains(extension)
                || contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType)
                || heic != HEIC_CONTENT_TYPE.equals(contentType)) {
            throw new ArtistProfileImageInvalidException();
        }

        try {
            if (heic) {
                return decodeHeic(file);
            }
            try (InputStream inputStream = file.getInputStream()) {
                return ImmutableImage.loader().fromStream(inputStream);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArtistProfileImageInvalidException();
        } catch (Exception e) {
            throw new ArtistProfileImageInvalidException();
        }
    }

    private ImmutableImage decodeHeic(MultipartFile file) throws IOException, InterruptedException {
        Path directory = Files.createTempDirectory("artist-profile-heic-");
        try {
            Path input = directory.resolve("input.heic");
            Path output = directory.resolve("output.png");
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, input);
            }

            Process decoder = new ProcessBuilder("heif-convert", "-d", "libde265", input.toString(), output.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            try {
                if (!decoder.waitFor(HEIC_DECODE_TIMEOUT_SECONDS, TimeUnit.SECONDS) || decoder.exitValue() != 0) {
                    throw new IOException("Could not decode HEIC profile image");
                }
            } finally {
                if (decoder.isAlive()) {
                    decoder.destroyForcibly().onExit().join();
                }
            }

            // heif-convert numbers the outputs when the container holds multiple images.
            Path decodedImage = Files.exists(output) ? output : directory.resolve("output-1.png");
            return ImmutableImage.loader().fromPath(decodedImage);
        } finally {
            FileSystemUtils.deleteRecursively(directory);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }

        return filename.substring(
                filename.lastIndexOf('.') + 1
        ).toLowerCase(Locale.ROOT);
    }
}
