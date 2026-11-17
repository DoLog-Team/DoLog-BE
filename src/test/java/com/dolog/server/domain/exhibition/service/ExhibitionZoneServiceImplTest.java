package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExhibitionZoneServiceImplTest {

    @Mock private ExhibitionRepository exhibitionRepository;
    @Mock private ExhibitionZoneRepository exhibitionZoneRepository;
    @Mock private ExhibitionDetailRepository exhibitionDetailRepository;
    @Mock private NotificationService notificationService;
    @Mock private ArtworkRepository artworkRepository;
    @Mock private ExhibitionGuideMapRepository exhibitionGuideMapRepository;

    @InjectMocks private ExhibitionZoneServiceImpl service;

    private ExhibitionZoneCreateRequest requestWithOrderId(Integer orderId) {
        ExhibitionZoneCreateRequest request = new ExhibitionZoneCreateRequest();
        ReflectionTestUtils.setField(request, "name", "구역");
        ReflectionTestUtils.setField(request, "orderId", orderId);
        return request;
    }

    private Exhibition exhibitionOwnedBy(UUID exhibitionId, UUID ownerId) {
        Account account = Account.builder().id(ownerId).role(Role.EXHIBITION_ADMIN).build();
        return Exhibition.builder().id(exhibitionId).account(account).build();
    }

    @Test
    @DisplayName("orderId를 안 보내고 구역을 생성하면 기존 최대값 다음 순서로 자동 지정된다")
    void createZoneAutoAssignsOrderIdWhenOmitted() {
        UUID exhibitionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibitionOwnedBy(exhibitionId, ownerId)));
        when(exhibitionZoneRepository.findMaxOrderIdByExhibitionId(exhibitionId)).thenReturn(Optional.of(3));
        when(exhibitionZoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createZone(exhibitionId, ownerId, false, requestWithOrderId(null));

        ArgumentCaptor<ExhibitionZone> captor = ArgumentCaptor.forClass(ExhibitionZone.class);
        verify(exhibitionZoneRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getOrderId());
    }

    @Test
    @DisplayName("기존 구역이 없을 때 orderId를 안 보내면 1번으로 지정된다")
    void createZoneAutoAssignsFirstOrderIdWhenNoExistingZones() {
        UUID exhibitionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibitionOwnedBy(exhibitionId, ownerId)));
        when(exhibitionZoneRepository.findMaxOrderIdByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(exhibitionZoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createZone(exhibitionId, ownerId, false, requestWithOrderId(null));

        ArgumentCaptor<ExhibitionZone> captor = ArgumentCaptor.forClass(ExhibitionZone.class);
        verify(exhibitionZoneRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOrderId());
    }

    @Test
    @DisplayName("orderId를 보내면 그 값 그대로 쓰고 자동 지정은 건너뛴다")
    void createZoneKeepsExplicitOrderId() {
        UUID exhibitionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibitionOwnedBy(exhibitionId, ownerId)));
        when(exhibitionZoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createZone(exhibitionId, ownerId, false, requestWithOrderId(5));

        ArgumentCaptor<ExhibitionZone> captor = ArgumentCaptor.forClass(ExhibitionZone.class);
        verify(exhibitionZoneRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getOrderId());
    }
}
