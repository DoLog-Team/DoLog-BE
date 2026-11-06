package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.entity.ArtworkMaterialOption;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkMaterialOptionRepository;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkMaterialOptionReorderRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkMaterialOptionRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkMaterialOptionResponse;
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
public class ArtworkMaterialOptionServiceImpl implements ArtworkMaterialOptionService {

    private final ArtworkMaterialOptionRepository artworkMaterialOptionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ArtworkMaterialOptionResponse> getOptions() {
        return artworkMaterialOptionRepository.findAllByOrderByOrderIndexAsc().stream()
                .map(ArtworkMaterialOptionResponse::from)
                .toList();
    }

    @Override
    public void createOption(ArtworkMaterialOptionRequest request) {
        if (artworkMaterialOptionRepository.existsByName(request.name())) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_MATERIAL_OPTION_DUPLICATE_NAME);
        }

        int nextOrder = artworkMaterialOptionRepository.findMaxOrderIndex() + 1;

        artworkMaterialOptionRepository.save(ArtworkMaterialOption.builder()
                .name(request.name())
                .orderIndex(nextOrder)
                .build());
    }

    @Override
    public void updateOption(UUID materialOptionId, ArtworkMaterialOptionRequest request) {
        ArtworkMaterialOption option = artworkMaterialOptionRepository.findById(materialOptionId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_MATERIAL_OPTION_NOT_FOUND));

        if (artworkMaterialOptionRepository.existsByNameAndIdNot(request.name(), materialOptionId)) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_MATERIAL_OPTION_DUPLICATE_NAME);
        }

        option.updateName(request.name());
    }

    @Override
    public void deleteOption(UUID materialOptionId) {
        ArtworkMaterialOption option = artworkMaterialOptionRepository.findById(materialOptionId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_MATERIAL_OPTION_NOT_FOUND));

        artworkMaterialOptionRepository.delete(option);
    }

    @Override
    public void reorderOptions(List<ArtworkMaterialOptionReorderRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_MATERIAL_OPTION_REORDER_INVALID);
        }

        for (ArtworkMaterialOptionReorderRequest req : requests) {
            if (req == null || req.materialOptionId() == null || req.order() == null || req.order() < 1) {
                throw new ArtworkException(ArtworkErrorCode.ARTWORK_MATERIAL_OPTION_REORDER_INVALID);
            }
        }

        List<UUID> ids = requests.stream().map(ArtworkMaterialOptionReorderRequest::materialOptionId).toList();

        if (new HashSet<>(ids).size() != ids.size()) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_MATERIAL_OPTION_REORDER_INVALID);
        }

        List<ArtworkMaterialOption> options = artworkMaterialOptionRepository.findAllById(ids);
        if (options.size() != ids.size()) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_MATERIAL_OPTION_REORDER_INVALID);
        }

        Map<UUID, ArtworkMaterialOption> byId = options.stream()
                .collect(Collectors.toMap(ArtworkMaterialOption::getId, o -> o));
        for (ArtworkMaterialOptionReorderRequest req : requests) {
            byId.get(req.materialOptionId()).updateOrder(req.order());
        }
    }
}
