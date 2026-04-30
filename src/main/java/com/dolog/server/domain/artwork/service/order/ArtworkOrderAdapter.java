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
     * 생성 시 → zone 기준 마지막에 붙이기
     */
    public void assign(Artwork artwork) {
        List<Artwork> artworks = artworkRepository
                .findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
                        artwork.getExhibition().getId(),
                        artwork.getExhibitionZone().getId()
                )
                .stream()
                .filter(a -> !a.getId().equals(artwork.getId()))
                .toList();

        artwork.updateOrder(orderProcessor.assignNext(artworks));
    }
    /**
     * 같은 zone 내 reorder
     */
    public void reorder(Artwork artwork, Integer prev, Integer next) {
        try {
            artwork.updateOrder(orderProcessor.calculate(prev, next));
        } catch (IllegalStateException e) {

            List<Artwork> artworks = artworkRepository
                    .findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
                            artwork.getExhibition().getId(),
                            artwork.getExhibitionZone().getId()
                    );

            orderProcessor.rebalance(artworks);

            artwork.updateOrder(orderProcessor.calculate(prev, next));
        }
    }

    /**
     * zone 이동 + reorder
     */
    public void moveZone(Artwork artwork, UUID zoneId, Integer prev, Integer next) {

        // 기존 zone 저장
        UUID oldZoneId = artwork.getExhibitionZone().getId();

        ExhibitionZone newZone = zoneRepository.findById(zoneId)
                .orElseThrow();

        artwork.updateZone(newZone);
        artworkRepository.flush();

        // 새로운 zone 처리
        if (prev == null && next == null) {
            assign(artwork);
        } else {
            reorder(artwork, prev, next);
        }

        // 기존 zone 정리
        List<Artwork> oldZoneArtworks = artworkRepository
                .findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
                        artwork.getExhibition().getId(),
                        oldZoneId
                );

        orderProcessor.rebalance(oldZoneArtworks);
    }
}