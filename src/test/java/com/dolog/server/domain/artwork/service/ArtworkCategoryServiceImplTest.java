package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.entity.ArtworkCategory;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkCategoryRepository;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCategoryReorderRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCategoryRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCategoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtworkCategoryServiceImplTest {

    @Mock private ArtworkCategoryRepository artworkCategoryRepository;

    @InjectMocks private ArtworkCategoryServiceImpl service;

    @Test
    @DisplayName("목록은 orderIndex 순으로 응답 DTO로 변환된다")
    void getsCategoriesInOrder() {
        ArtworkCategory a = ArtworkCategory.builder().id(UUID.randomUUID()).name("회화").orderIndex(1).build();
        ArtworkCategory b = ArtworkCategory.builder().id(UUID.randomUUID()).name("사진").orderIndex(2).build();
        when(artworkCategoryRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(a, b));

        List<ArtworkCategoryResponse> result = service.getCategories();

        assertEquals(List.of(
                new ArtworkCategoryResponse(a.getId(), "회화", 1),
                new ArtworkCategoryResponse(b.getId(), "사진", 2)
        ), result);
    }

    @Test
    @DisplayName("생성 시 기존 최대 orderIndex 다음 번호를 받고, 이름이 중복이면 409")
    void createsWithNextOrderAndRejectsDuplicateName() {
        when(artworkCategoryRepository.existsByName("기타")).thenReturn(false);
        when(artworkCategoryRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(
                ArtworkCategory.builder().id(UUID.randomUUID()).name("회화").orderIndex(1).build(),
                ArtworkCategory.builder().id(UUID.randomUUID()).name("사진").orderIndex(3).build()
        ));

        service.createCategory(new ArtworkCategoryRequest("기타"));

        verify(artworkCategoryRepository).save(argThat(c -> c.getName().equals("기타") && c.getOrderIndex() == 4));

        when(artworkCategoryRepository.existsByName("회화")).thenReturn(true);
        ArtworkException ex = assertThrows(ArtworkException.class,
                () -> service.createCategory(new ArtworkCategoryRequest("회화")));
        assertEquals("ARTWORK_024", ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("없는 카테고리를 수정/삭제하면 404")
    void rejectsMissingCategoryOnUpdateAndDelete() {
        UUID id = UUID.randomUUID();
        when(artworkCategoryRepository.findById(id)).thenReturn(java.util.Optional.empty());

        ArtworkException updateEx = assertThrows(ArtworkException.class,
                () -> service.updateCategory(id, new ArtworkCategoryRequest("조각")));
        assertEquals("ARTWORK_023", updateEx.getErrorCode().getCode());

        ArtworkException deleteEx = assertThrows(ArtworkException.class,
                () -> service.deleteCategory(id));
        assertEquals("ARTWORK_023", deleteEx.getErrorCode().getCode());
    }

    @Test
    @DisplayName("다른 카테고리와 이름이 겹치게 수정하면 409, 겹치지 않으면 이름이 바뀐다")
    void updateRejectsDuplicateNameOtherwiseRenames() {
        UUID id = UUID.randomUUID();
        ArtworkCategory category = ArtworkCategory.builder().id(id).name("회화").orderIndex(1).build();
        when(artworkCategoryRepository.findById(id)).thenReturn(java.util.Optional.of(category));
        when(artworkCategoryRepository.existsByNameAndIdNot("사진", id)).thenReturn(true);

        ArtworkException ex = assertThrows(ArtworkException.class,
                () -> service.updateCategory(id, new ArtworkCategoryRequest("사진")));
        assertEquals("ARTWORK_024", ex.getErrorCode().getCode());

        when(artworkCategoryRepository.existsByNameAndIdNot("수채화", id)).thenReturn(false);
        service.updateCategory(id, new ArtworkCategoryRequest("수채화"));
        assertEquals("수채화", category.getName());
    }

    @Test
    @DisplayName("순서 변경은 중복 id, 빈 목록, 존재하지 않는 id가 있으면 400이고 아무것도 안 바꾼다")
    void reorderValidatesRequests() {
        assertThrows(ArtworkException.class, () -> service.reorderCategories(List.of()));
        assertThrows(ArtworkException.class, () -> service.reorderCategories(null));

        UUID id = UUID.randomUUID();
        assertThrows(ArtworkException.class, () -> service.reorderCategories(List.of(
                new ArtworkCategoryReorderRequest(id, 1),
                new ArtworkCategoryReorderRequest(id, 2)
        )));

        when(artworkCategoryRepository.findAllById(any())).thenReturn(List.of());
        assertThrows(ArtworkException.class, () -> service.reorderCategories(List.of(
                new ArtworkCategoryReorderRequest(id, 1)
        )));
    }

    @Test
    @DisplayName("순서 변경 요청이 유효하면 각 카테고리의 순서가 바뀐다")
    void reordersValidRequests() {
        ArtworkCategory a = ArtworkCategory.builder().id(UUID.randomUUID()).name("회화").orderIndex(1).build();
        ArtworkCategory b = ArtworkCategory.builder().id(UUID.randomUUID()).name("사진").orderIndex(2).build();
        when(artworkCategoryRepository.findAllById(any())).thenReturn(List.of(a, b));

        service.reorderCategories(List.of(
                new ArtworkCategoryReorderRequest(a.getId(), 2),
                new ArtworkCategoryReorderRequest(b.getId(), 1)
        ));

        assertEquals(2, a.getOrderIndex());
        assertEquals(1, b.getOrderIndex());
    }
}
