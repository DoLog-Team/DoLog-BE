package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ArtworkFieldRequirementTest {

    @Mock
    private ExhibitionFieldSettingsRepository fieldSettingsRepository;

    @Mock
    private ExhibitionZoneRepository exhibitionZoneRepository;

    @InjectMocks
    private ArtworkFieldRequirement artworkFieldRequirement;

    @Test
    @DisplayName("미출품 작품은 기본/상세 필수값(카테고리, 제작 종료년도)만으로 완료율을 계산한다")
    void completionRateWithoutExhibition() {
        assertEquals(0, artworkFieldRequirement.completionRate(Artwork.builder().build()));
        assertEquals(50, artworkFieldRequirement.completionRate(Artwork.builder().category("회화").build()));
        assertEquals(100, artworkFieldRequirement.completionRate(
                Artwork.builder().category("회화").productionEndYear(2024).build()));
    }

    @Test
    @DisplayName("출품 작품은 기본/상세 필수값에 전시의 출품 정보 필수값을 더해 완료율을 계산한다")
    void completionRateWithExhibition() {
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).build();
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.builder()
                .requiredMainImg(true).requiredLocationMap(true).build();
        lenient().when(fieldSettingsRepository.findByExhibitionId(exhibition.getId()))
                .thenReturn(Optional.of(settings));

        Artwork empty = Artwork.builder().exhibition(exhibition).build();
        assertEquals(0, artworkFieldRequirement.completionRate(empty));

        Artwork halfFilled = Artwork.builder()
                .exhibition(exhibition)
                .category("회화")
                .productionEndYear(2024)
                .build();
        assertEquals(50, artworkFieldRequirement.completionRate(halfFilled));

        Artwork fullyFilled = Artwork.builder()
                .exhibition(exhibition)
                .category("회화")
                .productionEndYear(2024)
                .mainImg("https://s3/main.webp")
                .locationMap("https://s3/map.webp")
                .build();
        assertEquals(100, artworkFieldRequirement.completionRate(fullyFilled));
    }

    @Test
    @DisplayName("출품했지만 전시에 출품 정보 설정이 없으면 기본/상세 필수값만으로 완료율을 계산한다")
    void completionRateWithExhibitionButNoSettings() {
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).build();
        lenient().when(fieldSettingsRepository.findByExhibitionId(exhibition.getId()))
                .thenReturn(Optional.empty());

        Artwork artwork = Artwork.builder()
                .exhibition(exhibition)
                .category("회화")
                .productionEndYear(2024)
                .build();

        assertEquals(100, artworkFieldRequirement.completionRate(artwork));
    }

    // isSatisfied와 completionRate는 같은 체크 목록(requirementChecks)에서 파생되므로, 항상 "충족 == 100%"가 일치해야 한다.
    @Test
    @DisplayName("구역이 있는 전시인데 그룹을 안 고르면 completionRate는 100이 아니고 isSatisfied도 false다")
    void zoneRequiredButMissingKeepsBothInSync() {
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).build();
        lenient().when(fieldSettingsRepository.findByExhibitionId(exhibition.getId()))
                .thenReturn(Optional.empty());
        lenient().when(exhibitionZoneRepository.existsByExhibitionId(exhibition.getId()))
                .thenReturn(true);

        Artwork artwork = Artwork.builder()
                .exhibition(exhibition)
                .category("회화")
                .productionEndYear(2024)
                .build(); // 구역 미지정

        assertFalse(artworkFieldRequirement.isSatisfied(artwork));
        assertTrue(artworkFieldRequirement.completionRate(artwork) < 100);
    }

    @Test
    @DisplayName("구역이 있는 전시에서 그룹까지 고르면 completionRate는 100이고 isSatisfied도 true다")
    void zoneRequiredAndChosenKeepsBothInSync() {
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).build();
        ExhibitionZone zone = ExhibitionZone.builder().id(UUID.randomUUID()).exhibition(exhibition).name("1구역").build();
        lenient().when(fieldSettingsRepository.findByExhibitionId(exhibition.getId()))
                .thenReturn(Optional.empty());
        lenient().when(exhibitionZoneRepository.existsByExhibitionId(exhibition.getId()))
                .thenReturn(true);

        Artwork artwork = Artwork.builder()
                .exhibition(exhibition)
                .category("회화")
                .productionEndYear(2024)
                .exhibitionZone(zone)
                .build();

        assertTrue(artworkFieldRequirement.isSatisfied(artwork));
        assertEquals(100, artworkFieldRequirement.completionRate(artwork));
    }

    @Test
    @DisplayName("구역이 하나도 없는 전시는 그룹을 안 골라도 completionRate 100%, isSatisfied도 true다")
    void zoneNotRequiredWhenExhibitionHasNoZones() {
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).build();
        lenient().when(fieldSettingsRepository.findByExhibitionId(exhibition.getId()))
                .thenReturn(Optional.empty());
        lenient().when(exhibitionZoneRepository.existsByExhibitionId(exhibition.getId()))
                .thenReturn(false);

        Artwork artwork = Artwork.builder()
                .exhibition(exhibition)
                .category("회화")
                .productionEndYear(2024)
                .build();

        assertTrue(artworkFieldRequirement.isSatisfied(artwork));
        assertEquals(100, artworkFieldRequirement.completionRate(artwork));
    }
}
