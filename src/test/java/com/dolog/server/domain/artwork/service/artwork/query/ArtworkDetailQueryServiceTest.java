package com.dolog.server.domain.artwork.service.artwork.query;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkDetailResponse;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtworkDetailQueryServiceTest {

    @Mock private ArtworkRepository artworkRepository;
    @Mock private BtsRepository btsRepository;

    @InjectMocks private ArtworkDetailQueryService service;

    private Artwork artwork(ExhibitionZone zone) {
        Artwork a = Artwork.builder().id(UUID.randomUUID()).title("t").exhibitionZone(zone).build();
        ReflectionTestUtils.setField(a, "artworkArtistMaps", new ArrayList<>());
        ReflectionTestUtils.setField(a, "artworkImg", new ArrayList<>());
        return a;
    }

    @Test
    @DisplayName("작품 상세는 작품이 속한 구역 이름(zoneName)을 함께 내려준다")
    void returnsZoneName() {
        UUID exhibitionId = UUID.randomUUID();
        Artwork a = artwork(ExhibitionZone.builder().id(UUID.randomUUID()).name("2층 전시관").build());
        when(artworkRepository.findDetailById(exhibitionId, a.getId())).thenReturn(Optional.of(a));

        ArtworkDetailResponse response = service.getArtworkDetail(exhibitionId, a.getId());

        assertEquals("2층 전시관", response.getZoneName());
    }

    @Test
    @DisplayName("구역이 미지정이면 zoneName은 null이다")
    void zoneNameIsNullWhenNoZone() {
        UUID exhibitionId = UUID.randomUUID();
        Artwork a = artwork(null);
        when(artworkRepository.findDetailById(exhibitionId, a.getId())).thenReturn(Optional.of(a));

        ArtworkDetailResponse response = service.getArtworkDetail(exhibitionId, a.getId());

        assertNull(response.getZoneName());
    }
}
