package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.web.dto.request.ArtworkCategoryReorderRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCategoryRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface ArtworkCategoryService {

    List<ArtworkCategoryResponse> getCategories();

    void createCategory(ArtworkCategoryRequest request);

    void updateCategory(UUID categoryId, ArtworkCategoryRequest request);

    void deleteCategory(UUID categoryId);

    void reorderCategories(List<ArtworkCategoryReorderRequest> requests);
}
