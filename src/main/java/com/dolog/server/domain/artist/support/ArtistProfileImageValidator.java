package com.dolog.server.domain.artist.support;

import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileImageInvalidException;
import com.sksamuel.scrimage.ImmutableImage;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Component
public class ArtistProfileImageValidator {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        if (file.getSize() > MAX_FILE_SIZE
                || !hasAllowedExtension(file.getOriginalFilename())
                || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ArtistProfileImageInvalidException();
        }

        try (InputStream inputStream = file.getInputStream()) {
            ImmutableImage.loader().fromStream(inputStream);
        } catch (Exception e) {
            throw new ArtistProfileImageInvalidException();
        }
    }

    private boolean hasAllowedExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }

        String extension = filename.substring(
                filename.lastIndexOf('.') + 1
        ).toLowerCase(Locale.ROOT);

        return ALLOWED_EXTENSIONS.contains(extension);
    }
}
