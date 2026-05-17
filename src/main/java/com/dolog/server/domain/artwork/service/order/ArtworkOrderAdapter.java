package com.dolog.server.domain.artwork.service.order;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.global.order.OrderProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ArtworkOrderAdapter {

    private final ArtworkRepository artworkRepository;
    private final ExhibitionZoneRepository zoneRepository;
    private final OrderProcessor orderProcessor;

    /**
     * 생성 시
     */
    public void assign(Artwork artwork) {
        List<Artwork> artworks =
                artworkRepository.findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
                        artwork.getExhibition().getId(),
                        artwork.getExhibitionZone().getId()
                );

        artwork.updateOrder(orderProcessor.assignNext(artworks));
    }

    /**
     * reorder
     */
    public void reorder(Artwork artwork, Integer prev, Integer next, UUID zoneId) {

        List<Artwork> artworks =
                artworkRepository.findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
                        artwork.getExhibition().getId(),
                        zoneId
                );

        Integer newOrder = orderProcessor.calculate(prev, next);

        // 공간 부족 → rebalance 후 다시 계산
        if (newOrder == null) {
            orderProcessor.rebalance(artworks);
            newOrder = orderProcessor.calculate(prev, next);
        }

        artwork.updateOrder(newOrder);
    }

    /**
     * zone 이동 + reorder
     */
    public void moveZone(Artwork artwork, UUID zoneId, Integer prev, Integer next) {

        UUID oldZoneId = artwork.getExhibitionZone().getId();

        ExhibitionZone newZone = zoneRepository.findById(zoneId)
                .orElseThrow();

        // 1. zone 변경
        artwork.updateZone(newZone);

        // 2. new zone 리스트
        List<Artwork> newZoneList =
                artworkRepository.findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
                        artwork.getExhibition().getId(),
                        zoneId
                );

        if (prev == null && next == null) {
            orderProcessor.rebalance(newZoneList);
        } else {
            reorder(artwork, prev, next, zoneId);
        }

        // 3. old zone 정리
        List<Artwork> oldZoneList =
                artworkRepository.findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
                        artwork.getExhibition().getId(),
                        oldZoneId
                );

        orderProcessor.rebalance(oldZoneList);
    }
}