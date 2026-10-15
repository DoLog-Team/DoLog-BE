package com.dolog.server.domain.artwork.service.exhibition;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtworkExhibitionServiceImplTest {

    @Mock private ExhibitionRepository exhibitionRepository;
    @Mock private ExhibitionGuideMapRepository exhibitionGuideMapRepository;
    @Mock private ExhibitionDetailRepository exhibitionDetailRepository;
    @Mock private ArtworkRepository artworkRepository;

    @InjectMocks private ArtworkExhibitionServiceImpl service;

    private Artwork artwork(String category) {
        Artwork a = Artwork.builder().id(UUID.randomUUID()).title("t").category(category).build();
        ReflectionTestUtils.setField(a, "artworkArtistMaps", new ArrayList<>());
        return a;
    }

    @Test
    @DisplayName("카테고리가 미지정(null/공백)이면 '기타'로 바꾸지 않고 categoryName=null 그룹으로 내려준다")
    void uncategorizedArtworksAreReturnedWithNullCategoryName() {
        UUID exhibitionId = UUID.randomUUID();
        when(exhibitionRepository.existsById(exhibitionId)).thenReturn(true);
        when(exhibitionGuideMapRepository.findByExhibitionIdOrderByCreatedAtAsc(exhibitionId)).thenReturn(List.of());
        when(artworkRepository.findArtworksForList(eq(exhibitionId), any(), any()))
                .thenReturn(List.of(artwork(null), artwork(""), artwork("  "), artwork("회화"), artwork("회화")));

        ExhibitionArtworkListResponse response = service.getExhibitionArtworkList(exhibitionId, null, null, null);

        List<com.dolog.server.domain.artwork.web.dto.response.CategoryArtworkResponse> categories =
                response.getZones().get(0).getCategories();
        assertEquals(2, categories.size());

        Set<String> names = categories.stream().map(c -> c.getCategoryName()).collect(Collectors.toCollection(HashSet::new));
        assertTrue(names.contains(null));
        assertTrue(names.contains("회화"));
        assertFalse(names.contains("기타"));

        var uncategorized = categories.stream().filter(c -> c.getCategoryName() == null).findFirst().orElseThrow();
        assertEquals(3, uncategorized.getArtworks().size());
        var painting = categories.stream().filter(c -> "회화".equals(c.getCategoryName())).findFirst().orElseThrow();
        assertEquals(2, painting.getArtworks().size());
    }
}
