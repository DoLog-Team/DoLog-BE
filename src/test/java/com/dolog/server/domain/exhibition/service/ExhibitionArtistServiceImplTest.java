package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExhibitionArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistProfileRepository artistProfileRepository;

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    @InjectMocks
    private ExhibitionArtistServiceImpl service;

    @Test
    @DisplayName("대기 중인 작가를 일괄 수락하고 없는 전시 프로필만 생성한다")
    void acceptsPendingArtistsAndCreatesOnlyMissingProfiles() {
        UUID ownerId = UUID.randomUUID();
        Exhibition exhibition = exhibition(ownerId);
        Artist firstArtist = artist("첫 번째", "first@test.com");
        Artist secondArtist = artist("두 번째", "second@test.com");
        ExhibitionArtistMap firstMap = map(
                exhibition,
                firstArtist,
                ExhibitionArtistStatus.PENDING
        );
        ExhibitionArtistMap secondMap = map(
                exhibition,
                secondArtist,
                ExhibitionArtistStatus.PENDING
        );
        List<UUID> artistIds = List.of(
                firstArtist.getId(),
                secondArtist.getId()
        );

        when(exhibitionArtistMapRepository
                .findAllByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(firstMap, secondMap));
        when(artistProfileRepository
                .findArtistIdsByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(firstArtist.getId()));

        var response = service.updateArtistStatuses(
                ownerId,
                exhibition.getId(),
                artistIds,
                ExhibitionArtistStatus.JOINED
        );

        assertEquals(2, response.updatedCount());
        assertEquals(ExhibitionArtistStatus.JOINED, firstMap.getStatus());
        assertEquals(ExhibitionArtistStatus.JOINED, secondMap.getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ArtistProfile>> profilesCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(artistProfileRepository).saveAll(profilesCaptor.capture());

        List<ArtistProfile> createdProfiles = StreamSupport.stream(
                profilesCaptor.getValue().spliterator(),
                false
        ).toList();
        assertEquals(1, createdProfiles.size());
        assertEquals(secondArtist.getId(), createdProfiles.get(0).getArtist().getId());
        assertEquals("두 번째", createdProfiles.get(0).getNameKo());
        assertEquals("second@test.com", createdProfiles.get(0).getEmail());
        assertTrue(createdProfiles.get(0).isPublic());
    }

    @Test
    @DisplayName("일괄 요청 중 잘못된 상태 전이가 있으면 어떤 작가도 변경하지 않는다")
    void rejectsWholeRequestWhenAnyTransitionIsInvalid() {
        UUID ownerId = UUID.randomUUID();
        Exhibition exhibition = exhibition(ownerId);
        Artist pendingArtist = artist("대기", "pending@test.com");
        Artist joinedArtist = artist("참여", "joined@test.com");
        ExhibitionArtistMap pendingMap = map(
                exhibition,
                pendingArtist,
                ExhibitionArtistStatus.PENDING
        );
        ExhibitionArtistMap joinedMap = map(
                exhibition,
                joinedArtist,
                ExhibitionArtistStatus.JOINED
        );

        when(exhibitionArtistMapRepository
                .findAllByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(pendingMap, joinedMap));

        ExhibitionException exception = assertThrows(
                ExhibitionException.class,
                () -> service.updateArtistStatuses(
                        ownerId,
                        exhibition.getId(),
                        List.of(pendingArtist.getId(), joinedArtist.getId()),
                        ExhibitionArtistStatus.JOINED
                )
        );

        assertEquals(
                ExhibitionErrorCode.EXHIBITION_ARTIST_STATUS_INVALID,
                exception.getErrorCode()
        );
        assertEquals(ExhibitionArtistStatus.PENDING, pendingMap.getStatus());
        assertEquals(ExhibitionArtistStatus.JOINED, joinedMap.getStatus());
        verify(artistProfileRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("대기 중인 작가를 거절하면 프로필을 생성하지 않는다")
    void deniesPendingArtistWithoutCreatingProfile() {
        UUID ownerId = UUID.randomUUID();
        Exhibition exhibition = exhibition(ownerId);
        Artist artist = artist("거절 대상", "denied@test.com");
        ExhibitionArtistMap map = map(
                exhibition,
                artist,
                ExhibitionArtistStatus.PENDING
        );

        when(exhibitionArtistMapRepository
                .findAllByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(map));

        var response = service.updateArtistStatuses(
                ownerId,
                exhibition.getId(),
                List.of(artist.getId()),
                ExhibitionArtistStatus.DENIED
        );

        assertEquals(1, response.updatedCount());
        assertEquals(ExhibitionArtistStatus.DENIED, map.getStatus());
        verifyNoInteractions(artistProfileRepository);
    }

    @Test
    @DisplayName("참여 중인 작가는 관리자에 의해 제외 상태로 변경될 수 있다")
    void removesJoinedArtist() {
        UUID ownerId = UUID.randomUUID();
        Exhibition exhibition = exhibition(ownerId);
        Artist artist = artist("제외 대상", "removed@test.com");
        ExhibitionArtistMap map = map(
                exhibition,
                artist,
                ExhibitionArtistStatus.JOINED
        );

        when(exhibitionArtistMapRepository
                .findAllByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(map));

        service.updateArtistStatuses(
                ownerId,
                exhibition.getId(),
                List.of(artist.getId()),
                ExhibitionArtistStatus.REMOVED
        );

        assertEquals(ExhibitionArtistStatus.REMOVED, map.getStatus());
        verifyNoInteractions(artistProfileRepository);
    }

    @Test
    @DisplayName("다른 전시 관리자의 전시 상태를 변경할 수 없다")
    void rejectsDifferentExhibitionOwner() {
        UUID ownerId = UUID.randomUUID();
        Exhibition exhibition = exhibition(ownerId);

        assertThrows(
                AccessDeniedException.class,
                () -> service.updateArtistStatuses(
                        UUID.randomUUID(),
                        exhibition.getId(),
                        List.of(UUID.randomUUID()),
                        ExhibitionArtistStatus.JOINED
                )
        );

        verifyNoInteractions(exhibitionArtistMapRepository);
        verifyNoInteractions(artistProfileRepository);
    }

    @Test
    @DisplayName("일괄 변경 API에서 PENDING과 WITHDRAWN은 목표 상태로 받을 수 없다")
    void rejectsUnsupportedTargetStatus() {
        UUID ownerId = UUID.randomUUID();
        Exhibition exhibition = exhibition(ownerId);

        for (ExhibitionArtistStatus targetStatus : List.of(
                ExhibitionArtistStatus.PENDING,
                ExhibitionArtistStatus.WITHDRAWN
        )) {
            ExhibitionException exception = assertThrows(
                    ExhibitionException.class,
                    () -> service.updateArtistStatuses(
                            ownerId,
                            exhibition.getId(),
                            List.of(UUID.randomUUID()),
                            targetStatus
                    )
            );

            assertEquals(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_STATUS_INVALID,
                    exception.getErrorCode()
            );
        }

        verifyNoInteractions(exhibitionArtistMapRepository);
        verifyNoInteractions(artistProfileRepository);
    }

    @Test
    @DisplayName("요청한 작가 중 전시 참여 관계가 없는 작가가 있으면 전체 요청을 거절한다")
    void rejectsWholeRequestWhenAnyArtistMapIsMissing() {
        UUID ownerId = UUID.randomUUID();
        Exhibition exhibition = exhibition(ownerId);
        Artist existingArtist = artist("신청자", "applicant@test.com");
        ExhibitionArtistMap existingMap = map(
                exhibition,
                existingArtist,
                ExhibitionArtistStatus.PENDING
        );

        when(exhibitionArtistMapRepository
                .findAllByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(existingMap));

        ExhibitionException exception = assertThrows(
                ExhibitionException.class,
                () -> service.updateArtistStatuses(
                        ownerId,
                        exhibition.getId(),
                        List.of(existingArtist.getId(), UUID.randomUUID()),
                        ExhibitionArtistStatus.JOINED
                )
        );

        assertEquals(
                ExhibitionErrorCode.EXHIBITION_ARTIST_NOT_FOUND,
                exception.getErrorCode()
        );
        assertEquals(ExhibitionArtistStatus.PENDING, existingMap.getStatus());
        verifyNoInteractions(artistProfileRepository);
    }

    private Exhibition exhibition(UUID ownerId) {
        Account owner = Account.builder()
                .id(ownerId)
                .role(Role.EXHIBITION_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Exhibition exhibition = Exhibition.builder()
                .id(UUID.randomUUID())
                .account(owner)
                .univName("두록대학교")
                .deptName("시각디자인학과")
                .slug("test-" + UUID.randomUUID())
                .build();

        when(exhibitionRepository.findById(exhibition.getId()))
                .thenReturn(Optional.of(exhibition));
        return exhibition;
    }

    private Artist artist(String name, String email) {
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .email(email)
                .role(Role.ARTIST_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        return Artist.builder()
                .id(UUID.randomUUID())
                .account(account)
                .nameKo(name)
                .build();
    }

    private ExhibitionArtistMap map(
            Exhibition exhibition,
            Artist artist,
            ExhibitionArtistStatus status
    ) {
        return ExhibitionArtistMap.builder()
                .id(UUID.randomUUID())
                .exhibition(exhibition)
                .artist(artist)
                .status(status)
                .build();
    }
}
