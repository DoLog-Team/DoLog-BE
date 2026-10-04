package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionCustomThemeRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.basic.ArtistJoinCodeResponse;
import com.dolog.server.global.util.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExhibitionServiceImplTest {

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private ExhibitionDetailRepository exhibitionDetailRepository;

    @Mock
    private ExhibitionMapRepository exhibitionMapRepository;

    @Mock
    private ExhibitionCustomThemeRepository exhibitionCustomThemeRepository;

    @Mock
    private FileService fileService;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ExhibitionServiceImpl service;

    @Test
    @DisplayName("작가 참여 코드를 허용된 문자로 구성된 8자리 코드로 재발급한다")
    void reissuesArtistJoinCode() {
        UUID exhibitionId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        Exhibition exhibition = Exhibition.builder()
                .id(exhibitionId)
                .artistJoinCode("2345ABCD")
                .build();

        when(exhibitionRepository.findByIdForUpdate(exhibitionId))
                .thenReturn(Optional.of(exhibition));
        when(exhibitionRepository.existsByEntryCodeOrArtistJoinCode(
                anyString(),
                anyString()
        )).thenReturn(false);

        ArtistJoinCodeResponse response =
                service.reissueArtistJoinCode(exhibitionId, expiresAt);

        assertTrue(response.artistJoinCode()
                .matches("[2-9A-HJ-KM-NP-Z]{8}"));
        assertEquals(expiresAt, response.artistJoinCodeExpiresAt());
        assertEquals(response.artistJoinCode(), exhibition.getArtistJoinCode());
        assertEquals(expiresAt, exhibition.getArtistJoinCodeExpiresAt());
        verify(exhibitionRepository).existsByEntryCodeOrArtistJoinCode(
                response.artistJoinCode(),
                response.artistJoinCode()
        );
    }

    @Test
    @DisplayName("과거 만료 시각으로는 작가 참여 코드를 재발급할 수 없다")
    void rejectsPastArtistJoinCodeExpiry() {
        ExhibitionException exception = assertThrows(
                ExhibitionException.class,
                () -> service.reissueArtistJoinCode(
                        UUID.randomUUID(),
                        LocalDateTime.now().minusMinutes(1)
                )
        );

        assertEquals(
                ExhibitionErrorCode.ARTIST_JOIN_CODE_EXPIRY_INVALID,
                exception.getErrorCode()
        );
        verifyNoInteractions(exhibitionRepository);
    }
}
