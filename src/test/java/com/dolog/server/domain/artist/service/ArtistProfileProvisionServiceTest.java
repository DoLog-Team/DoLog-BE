package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistProfileProvisionServiceTest {

    @Mock
    private ArtistProfileRepository artistProfileRepository;

    @InjectMocks
    private ArtistProfileProvisionService service;

    @Test
    @DisplayName("프로필이 없는 작가에게 기본 정보를 채운 공통 프로필을 생성한다")
    void createsProfileWithArtistDefaults() {
        UUID artistId = UUID.randomUUID();
        Account account = Account.builder()
                .email("artist@example.com")
                .build();
        Artist artist = Artist.builder()
                .id(artistId)
                .account(account)
                .nameKo("김두록")
                .nameEn("Dolog Kim")
                .build();

        when(artistProfileRepository.findByArtistId(artistId))
                .thenReturn(Optional.empty());
        when(artistProfileRepository.save(any(ArtistProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArtistProfile result = service.ensureProfile(artist);

        ArgumentCaptor<ArtistProfile> captor =
                ArgumentCaptor.forClass(ArtistProfile.class);
        verify(artistProfileRepository).save(captor.capture());
        ArtistProfile savedProfile = captor.getValue();
        assertSame(savedProfile, result);
        assertSame(artist, savedProfile.getArtist());
        assertEquals("김두록", savedProfile.getNameKo());
        assertEquals("Dolog Kim", savedProfile.getNameEn());
        assertEquals("artist@example.com", savedProfile.getEmail());
        assertEquals(0L, savedProfile.getViewCount());
    }

    @Test
    @DisplayName("이미 공통 프로필이 있으면 중복 생성하지 않는다")
    void returnsExistingProfile() {
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder().id(artistId).build();
        ArtistProfile existingProfile = ArtistProfile.builder()
                .id(UUID.randomUUID())
                .artist(artist)
                .build();

        when(artistProfileRepository.findByArtistId(artistId))
                .thenReturn(Optional.of(existingProfile));

        ArtistProfile result = service.ensureProfile(artist);

        assertSame(existingProfile, result);
        verify(artistProfileRepository, never()).save(any());
    }
}
