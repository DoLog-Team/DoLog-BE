package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("local")
@Transactional(readOnly = true)
class ArtistProfileRepositoryQueryTest {

    @Autowired
    private ArtistProfileRepository profileRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Test
    @DisplayName("관리자용 프로필 목록 집계 쿼리는 MySQL에서 실행된다")
    void managementListQueryExecutes() {
        assertDoesNotThrow(() ->
                profileRepository.findListItemsByExhibitionId(
                        UUID.randomUUID()
                )
        );
    }

    @Test
    @DisplayName("프로필 상세의 공개 작품 조회 쿼리는 MySQL에서 실행된다")
    void profileDetailArtworkQueryExecutes() {
        assertDoesNotThrow(() ->
                artworkRepository.findVisibleInExhibitionByArtistId(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
        );
    }
}
