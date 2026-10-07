package com.dolog.server.domain.artist.support;

import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileImageInvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtistProfileImageValidatorTest {

    private final ArtistProfileImageValidator validator =
            new ArtistProfileImageValidator();

    @Test
    @DisplayName("JPG 또는 PNG 형식의 5MB 이하 7:9 이미지를 허용한다")
    void acceptsValidProfileImage() throws Exception {
        MockMultipartFile image = image(
                70,
                90,
                "png",
                "profile.png",
                "image/png"
        );

        assertDoesNotThrow(() -> validator.validate(image));
    }

    @Test
    @DisplayName("WebP 프로필 이미지를 거부한다")
    void rejectsWebpImage() {
        MockMultipartFile image = new MockMultipartFile(
                "profileImg",
                "profile.webp",
                "image/webp",
                new byte[]{1}
        );

        assertThrows(
                ArtistProfileImageInvalidException.class,
                () -> validator.validate(image)
        );
    }

    @Test
    @DisplayName("5MB를 초과하는 프로필 이미지를 거부한다")
    void rejectsOversizedImage() {
        MockMultipartFile image = new MockMultipartFile(
                "profileImg",
                "profile.png",
                "image/png",
                new byte[5 * 1024 * 1024 + 1]
        );

        assertThrows(
                ArtistProfileImageInvalidException.class,
                () -> validator.validate(image)
        );
    }

    @Test
    @DisplayName("다른 비율의 이미지도 프론트 크롭을 위해 허용한다")
    void acceptsImageWithDifferentAspectRatio() throws Exception {
        MockMultipartFile image = image(
                100,
                100,
                "png",
                "profile.png",
                "image/png"
        );

        assertDoesNotThrow(() -> validator.validate(image));
    }

    private MockMultipartFile image(
            int width,
            int height,
            String format,
            String filename,
            String contentType
    ) throws Exception {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);

        return new MockMultipartFile(
                "profileImg",
                filename,
                contentType,
                output.toByteArray()
        );
    }
}
