package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private AccountRepository accountRepository;

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    @Mock
    private ArtworkRepository artworkRepository;

    @InjectMocks
    private ExhibitionArtistServiceImpl service;

    @Test
    @DisplayName("DELETE 제외는 참여 행을 유지한 채 REMOVED로 전환하고 출품작 연결을 해제한다")
    void softlyRemovesArtistAndCancelsArtworkSubmissions() {
        UUID ownerId = UUID.randomUUID();
        Account owner = account(ownerId, Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(ownerId);
        Artist artist = artist("제외 대상", "removed-delete@test.com");
        ExhibitionArtistMap map = map(
                exhibition,
                artist,
                ExhibitionArtistStatus.JOINED
        );
        Artwork artwork = Artwork.builder()
                .id(UUID.randomUUID())
                .exhibition(exhibition)
                .build();

        when(accountRepository.findById(ownerId))
                .thenReturn(Optional.of(owner));
        when(exhibitionArtistMapRepository.findByExhibitionIdAndArtistId(
                exhibition.getId(),
                artist.getId()
        )).thenReturn(Optional.of(map));
        when(artworkRepository.findSubmittedArtworksByExhibitionIdAndArtistIdIn(
                exhibition.getId(),
                List.of(artist.getId())
        )).thenReturn(List.of(artwork));

        var response = service.removeArtistFromExhibition(
                ownerId,
                exhibition.getId(),
                artist.getId()
        );

        assertEquals(ExhibitionArtistStatus.REMOVED, map.getStatus());
        assertNull(artwork.getExhibition());
        assertNull(artwork.getExhibitionZone());
        assertEquals(artist.getId(), response.getArtistId());
        assertEquals("제외 대상 님이 두록대학교에서 제외되었습니다.", response.getMessage());
        verify(exhibitionArtistMapRepository, never()).delete(any());
    }

    @Test
    @DisplayName("전시 관리자는 다른 관리자의 전시에서 작가를 제외할 수 없다")
    void exhibitionAdminCannotRemoveArtistFromAnotherExhibition() {
        UUID actorId = UUID.randomUUID();
        Account actor = account(actorId, Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(UUID.randomUUID());

        when(accountRepository.findById(actorId))
                .thenReturn(Optional.of(actor));

        assertThrows(
                AccessDeniedException.class,
                () -> service.removeArtistFromExhibition(
                        actorId,
                        exhibition.getId(),
                        UUID.randomUUID()
                )
        );

        verifyNoInteractions(exhibitionArtistMapRepository);
        verifyNoInteractions(artworkRepository);
    }

    @Test
    @DisplayName("전시 관리자는 자신의 전시에 작가를 즉시 JOINED 상태로 추가한다")
    void exhibitionAdminAddsArtistAsJoined() {
        UUID ownerId = UUID.randomUUID();
        Account owner = account(ownerId, Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(ownerId);
        Artist artist = artist("직접 추가", "direct@test.com");
        UUID mapId = UUID.randomUUID();

        when(accountRepository.findById(ownerId))
                .thenReturn(Optional.of(owner));
        when(artistRepository.findById(artist.getId()))
                .thenReturn(Optional.of(artist));
        when(exhibitionArtistMapRepository.findByExhibitionIdAndArtistId(
                exhibition.getId(),
                artist.getId()
        )).thenReturn(Optional.empty());
        when(exhibitionArtistMapRepository.save(any(ExhibitionArtistMap.class)))
                .thenAnswer(invocation -> {
                    ExhibitionArtistMap unsaved = invocation.getArgument(0);
                    return ExhibitionArtistMap.builder()
                            .id(mapId)
                            .exhibition(unsaved.getExhibition())
                            .artist(unsaved.getArtist())
                            .status(unsaved.getStatus())
                            .greeting(unsaved.getGreeting())
                            .build();
                });
        when(artistProfileRepository
                .findArtistIdsByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of());

        var response = service.addArtistToExhibition(
                ownerId,
                exhibition.getId(),
                artist.getId()
        );

        assertEquals(mapId, response.exhibitionArtistId());
        assertEquals(ExhibitionArtistStatus.JOINED, response.status());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ArtistProfile>> profilesCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(artistProfileRepository).saveAll(profilesCaptor.capture());

        List<ArtistProfile> profiles = StreamSupport.stream(
                profilesCaptor.getValue().spliterator(),
                false
        ).toList();
        assertEquals(1, profiles.size());
        assertEquals(artist.getId(), profiles.get(0).getArtist().getId());
        assertEquals(exhibition.getId(), profiles.get(0).getExhibition().getId());
    }

    @Test
    @DisplayName("두록 관리자는 자신이 소유하지 않은 전시에도 작가를 추가할 수 있다")
    void dologAdminCanAddArtistToAnyExhibition() {
        UUID ownerId = UUID.randomUUID();
        UUID dologAdminId = UUID.randomUUID();
        Account dologAdmin = account(dologAdminId, Role.DOLOG_ADMIN);
        Exhibition exhibition = exhibition(ownerId);
        Artist artist = artist("두록 추가", "dolog@test.com");
        UUID mapId = UUID.randomUUID();

        when(accountRepository.findById(dologAdminId))
                .thenReturn(Optional.of(dologAdmin));
        when(artistRepository.findById(artist.getId()))
                .thenReturn(Optional.of(artist));
        when(exhibitionArtistMapRepository.findByExhibitionIdAndArtistId(
                exhibition.getId(),
                artist.getId()
        )).thenReturn(Optional.empty());
        when(exhibitionArtistMapRepository.save(any(ExhibitionArtistMap.class)))
                .thenAnswer(invocation -> {
                    ExhibitionArtistMap unsaved = invocation.getArgument(0);
                    return ExhibitionArtistMap.builder()
                            .id(mapId)
                            .exhibition(unsaved.getExhibition())
                            .artist(unsaved.getArtist())
                            .status(unsaved.getStatus())
                            .build();
                });
        when(artistProfileRepository
                .findArtistIdsByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(artist.getId()));

        var response = service.addArtistToExhibition(
                dologAdminId,
                exhibition.getId(),
                artist.getId()
        );

        assertEquals(mapId, response.exhibitionArtistId());
        assertEquals(ExhibitionArtistStatus.JOINED, response.status());
        verify(artistProfileRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("기존 참여 이력은 새 행을 만들지 않고 JOINED로 복구하며 greeting을 보존한다")
    void reusesExistingParticipationHistory() {
        UUID ownerId = UUID.randomUUID();
        Account owner = account(ownerId, Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(ownerId);
        Artist artist = artist("재추가", "readd@test.com");
        ExhibitionArtistMap existingMap = ExhibitionArtistMap.builder()
                .id(UUID.randomUUID())
                .exhibition(exhibition)
                .artist(artist)
                .status(ExhibitionArtistStatus.REMOVED)
                .greeting("기존 인사말")
                .build();

        when(accountRepository.findById(ownerId))
                .thenReturn(Optional.of(owner));
        when(artistRepository.findById(artist.getId()))
                .thenReturn(Optional.of(artist));
        when(exhibitionArtistMapRepository.findByExhibitionIdAndArtistId(
                exhibition.getId(),
                artist.getId()
        )).thenReturn(Optional.of(existingMap));
        when(artistProfileRepository
                .findArtistIdsByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(artist.getId()));

        var response = service.addArtistToExhibition(
                ownerId,
                exhibition.getId(),
                artist.getId()
        );

        assertEquals(existingMap.getId(), response.exhibitionArtistId());
        assertEquals(ExhibitionArtistStatus.JOINED, existingMap.getStatus());
        assertEquals("기존 인사말", existingMap.getGreeting());
        verify(exhibitionArtistMapRepository, never())
                .save(any(ExhibitionArtistMap.class));
        verify(artistProfileRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("이미 JOINED 상태인 작가는 중복 추가할 수 없다")
    void rejectsAlreadyJoinedArtist() {
        UUID ownerId = UUID.randomUUID();
        Account owner = account(ownerId, Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(ownerId);
        Artist artist = artist("이미 참여", "joined-direct@test.com");
        ExhibitionArtistMap existingMap = map(
                exhibition,
                artist,
                ExhibitionArtistStatus.JOINED
        );

        when(accountRepository.findById(ownerId))
                .thenReturn(Optional.of(owner));
        when(artistRepository.findById(artist.getId()))
                .thenReturn(Optional.of(artist));
        when(exhibitionArtistMapRepository.findByExhibitionIdAndArtistId(
                exhibition.getId(),
                artist.getId()
        )).thenReturn(Optional.of(existingMap));

        ExhibitionException exception = assertThrows(
                ExhibitionException.class,
                () -> service.addArtistToExhibition(
                        ownerId,
                        exhibition.getId(),
                        artist.getId()
                )
        );

        assertEquals(
                ExhibitionErrorCode.EXHIBITION_ARTIST_ALREADY_EXISTS,
                exception.getErrorCode()
        );
        verifyNoInteractions(artistProfileRepository);
    }

    @Test
    @DisplayName("전시 관리자는 다른 관리자의 전시에 작가를 추가할 수 없다")
    void exhibitionAdminCannotAddArtistToAnotherExhibition() {
        UUID actorId = UUID.randomUUID();
        Account actor = account(actorId, Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(UUID.randomUUID());

        when(accountRepository.findById(actorId))
                .thenReturn(Optional.of(actor));

        assertThrows(
                AccessDeniedException.class,
                () -> service.addArtistToExhibition(
                        actorId,
                        exhibition.getId(),
                        UUID.randomUUID()
                )
        );

        verifyNoInteractions(artistRepository);
        verifyNoInteractions(exhibitionArtistMapRepository);
        verifyNoInteractions(artistProfileRepository);
    }

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
        Artwork artwork = Artwork.builder()
                .id(UUID.randomUUID())
                .exhibition(exhibition)
                .build();

        when(exhibitionArtistMapRepository
                .findAllByExhibitionIdAndArtistIdIn(
                        eq(exhibition.getId()),
                        anyCollection()
                ))
                .thenReturn(List.of(map));
        when(artworkRepository.findSubmittedArtworksByExhibitionIdAndArtistIdIn(
                exhibition.getId(),
                List.of(artist.getId())
        )).thenReturn(List.of(artwork));

        service.updateArtistStatuses(
                ownerId,
                exhibition.getId(),
                List.of(artist.getId()),
                ExhibitionArtistStatus.REMOVED
        );

        assertEquals(ExhibitionArtistStatus.REMOVED, map.getStatus());
        assertNull(artwork.getExhibition());
        assertNull(artwork.getExhibitionZone());
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
        Account owner = account(ownerId, Role.EXHIBITION_ADMIN);

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

    private Account account(UUID accountId, Role role) {
        return Account.builder()
                .id(accountId)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
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
