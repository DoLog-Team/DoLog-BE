package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.entity.ArtworkCategory;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkCategoryRepository;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCategoryReorderRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCategoryRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkCategoryServiceImpl implements ArtworkCategoryService {

    private final ArtworkCategoryRepository artworkCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ArtworkCategoryResponse> getCategories() {
        return artworkCategoryRepository.findAllByOrderByOrderIndexAsc().stream()
                .map(ArtworkCategoryResponse::from)
                .toList();
    }

    @Override
    public void createCategory(ArtworkCategoryRequest request) {
        if (artworkCategoryRepository.existsByName(request.name())) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_CATEGORY_DUPLICATE_NAME);
        }

        int nextOrder = artworkCategoryRepository.findAllByOrderByOrderIndexAsc().stream()
                .mapToInt(ArtworkCategory::getOrderIndex)
                .max()
                .orElse(0) + 1;

        artworkCategoryRepository.save(ArtworkCategory.builder()
                .name(request.name())
                .orderIndex(nextOrder)
                .build());
    }

    @Override
    public void updateCategory(UUID categoryId, ArtworkCategoryRequest request) {
        ArtworkCategory category = artworkCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_CATEGORY_NOT_FOUND));

        if (artworkCategoryRepository.existsByNameAndIdNot(request.name(), categoryId)) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_CATEGORY_DUPLICATE_NAME);
        }

        category.updateName(request.name());
    }

    @Override
    public void deleteCategory(UUID categoryId) {
        ArtworkCategory category = artworkCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_CATEGORY_NOT_FOUND));

        artworkCategoryRepository.delete(category);
    }

    @Override
    public void reorderCategories(List<ArtworkCategoryReorderRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_CATEGORY_REORDER_INVALID);
        }

        List<UUID> ids = requests.stream().map(req -> {
            if (req == null || req.categoryId() == null || req.order() == null || req.order() < 1) {
                throw new ArtworkException(ArtworkErrorCode.ARTWORK_CATEGORY_REORDER_INVALID);
            }
            return req.categoryId();
        }).toList();

        if (new HashSet<>(ids).size() != ids.size()) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_CATEGORY_REORDER_INVALID);
        }

        List<ArtworkCategory> categories = artworkCategoryRepository.findAllById(ids);
        if (categories.size() != ids.size()) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_CATEGORY_REORDER_INVALID);
        }

        Map<UUID, ArtworkCategory> byId = categories.stream()
                .collect(Collectors.toMap(ArtworkCategory::getId, c -> c));
        for (ArtworkCategoryReorderRequest req : requests) {
            byId.get(req.categoryId()).updateOrder(req.order());
        }
    }
}
