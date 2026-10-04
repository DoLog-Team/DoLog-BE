package com.dolog.server.domain.exhibition.web.dto.request.artist;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExhibitionArtistStatusUpdateRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    @DisplayName("상태 일괄 변경은 100명까지 요청할 수 있다")
    void acceptsOneHundredArtists() {
        ExhibitionArtistStatusUpdateRequest request = requestWithArtistCount(100);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("상태 일괄 변경은 100명을 초과할 수 없다")
    void rejectsMoreThanOneHundredArtists() {
        ExhibitionArtistStatusUpdateRequest request = requestWithArtistCount(101);

        assertFalse(validator.validate(request).isEmpty());
    }

    private ExhibitionArtistStatusUpdateRequest requestWithArtistCount(int count) {
        List<UUID> artistIds = IntStream.range(0, count)
                .mapToObj(index -> UUID.randomUUID())
                .toList();

        return new ExhibitionArtistStatusUpdateRequest(
                artistIds,
                ExhibitionArtistStatus.JOINED
        );
    }
}
