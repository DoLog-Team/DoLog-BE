package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.request.guideMap.ExhibitionGuideMapCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.guideMap.GuideMapReorderRequest;
import com.dolog.server.global.util.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExhibitionGuideMapServiceImplTest {

    @Mock private ExhibitionGuideMapRepository guideMapRepository;
    @Mock private ExhibitionRepository exhibitionRepository;
    @Mock private ExhibitionZoneRepository zoneRepository;
    @Mock private FileService fileService;

    @InjectMocks private ExhibitionGuideMapServiceImpl service;

    private final UUID exhibitionId = UUID.randomUUID();
    private final Exhibition exhibition = Exhibition.builder().id(exhibitionId).build();

    private ExhibitionGuideMap map(Exhibition ex, Integer order) {
        return ExhibitionGuideMap.builder().id(UUID.randomUUID()).exhibition(ex).imageUrl("u").orderIndex(order).build();
    }

    private GuideMapReorderRequest req(UUID id, Integer order) throws Exception {
        String json = "{\"guide_map_id\":" + (id == null ? "null" : "\"" + id + "\"") + ",\"order\":" + order + "}";
        return new ObjectMapper().readValue(json, GuideMapReorderRequest.class);
    }

    @Test
    @DisplayName("등록하면 해당 전시의 마지막 번호 + 1이 부여된다")
    void createAssignsNextOrder() throws Exception {
        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        when(guideMapRepository.findMaxOrderIndexByExhibitionId(exhibitionId)).thenReturn(3);
        when(fileService.uploadFile(any(), any())).thenReturn("url");

        ExhibitionGuideMapCreateRequest request = new ExhibitionGuideMapCreateRequest();
        request.setImage(new MockMultipartFile("image", "a.png", "image/png", new byte[]{1}));
        service.createGuideMap(exhibitionId, request);

        ArgumentCaptor<ExhibitionGuideMap> captor = ArgumentCaptor.forClass(ExhibitionGuideMap.class);
        verify(guideMapRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getOrderIndex());
    }

    @Test
    @DisplayName("순서 변경: 같은 전시의 이미지면 order 값이 반영된다")
    void reorderApplies() throws Exception {
        ExhibitionGuideMap a = map(exhibition, 1);
        ExhibitionGuideMap b = map(exhibition, 2);
        when(exhibitionRepository.existsById(exhibitionId)).thenReturn(true);
        when(guideMapRepository.findAllById(any())).thenReturn(List.of(a, b));

        service.reorderGuideMaps(exhibitionId, List.of(req(a.getId(), 2), req(b.getId(), 1)));

        assertEquals(2, a.getOrderIndex());
        assertEquals(1, b.getOrderIndex());
    }

    @Test
    @DisplayName("순서 변경: 다른 전시의 이미지가 섞이면 400(GUIDE_MAP_REORDER_INVALID)이고 아무것도 바뀌지 않는다")
    void reorderRejectsOtherExhibition() throws Exception {
        ExhibitionGuideMap mine = map(exhibition, 1);
        ExhibitionGuideMap other = map(Exhibition.builder().id(UUID.randomUUID()).build(), 1);
        when(exhibitionRepository.existsById(exhibitionId)).thenReturn(true);
        when(guideMapRepository.findAllById(any())).thenReturn(List.of(mine, other));

        ExhibitionException e = assertThrows(ExhibitionException.class, () ->
                service.reorderGuideMaps(exhibitionId, List.of(req(mine.getId(), 2), req(other.getId(), 1))));

        assertEquals(ExhibitionErrorCode.GUIDE_MAP_REORDER_INVALID.getCode(), e.getErrorCode().getCode());
        assertEquals(1, mine.getOrderIndex());
        assertEquals(1, other.getOrderIndex());
    }

    @Test
    @DisplayName("순서 변경: 존재하지 않는 id, 중복 id, 잘못된 order, 빈 목록은 400이다")
    void reorderRejectsInvalidInput() throws Exception {
        UUID missing = UUID.randomUUID();
        when(exhibitionRepository.existsById(exhibitionId)).thenReturn(true);
        when(guideMapRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(ExhibitionException.class, () ->
                service.reorderGuideMaps(exhibitionId, List.of(req(missing, 1))));
        assertThrows(ExhibitionException.class, () ->
                service.reorderGuideMaps(exhibitionId, List.of(req(missing, 1), req(missing, 2))));
        assertThrows(ExhibitionException.class, () ->
                service.reorderGuideMaps(exhibitionId, List.of(req(missing, 0))));
        assertThrows(ExhibitionException.class, () ->
                service.reorderGuideMaps(exhibitionId, List.of()));
    }

    @Test
    @DisplayName("삭제하면 남은 이미지 번호가 1부터 빈틈없이 다시 매겨진다")
    void deleteRenumbers() {
        ExhibitionGuideMap first = map(exhibition, 1);
        ExhibitionGuideMap second = map(exhibition, 2);
        ExhibitionGuideMap third = map(exhibition, 3);
        when(guideMapRepository.findById(second.getId())).thenReturn(Optional.of(second));
        when(guideMapRepository.findByExhibitionIdOrderByOrderIndexAscCreatedAtAsc(exhibitionId))
                .thenReturn(List.of(first, third));

        service.deleteGuideMap(second.getId());

        verify(guideMapRepository).delete(second);
        assertEquals(1, first.getOrderIndex());
        assertEquals(2, third.getOrderIndex());
    }
}
