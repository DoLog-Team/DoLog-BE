package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistAlreadyExistsException;
import com.dolog.server.domain.artist.exception.artistError.ArtistBadRequestException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.DuplicateArtistPhoneException;
import com.dolog.server.domain.artist.exception.artistError.ArtistHasLinkedDataException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistResponse;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artwork.repository.ArtworkArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final AccountRepository accountRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;
    private final ArtworkArtistMapRepository artworkArtistMapRepository;

    // 작가 생성
    @Override
    public ArtistCreateResponse createArtist(
            UUID accountId,
            ArtistCreateRequest request
    ) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);

        if (account.getRole() != Role.ARTIST_ADMIN) {
            throw new AccessDeniedException(
                    "작가 계정만 작가 정보를 생성할 수 있습니다."
            );
        }

        if (artistRepository.existsByAccount(account)) {
            throw new ArtistAlreadyExistsException();
        }

        if (request.getEmail() != null
                && !request.getEmail().equalsIgnoreCase(account.getEmail())) {
            throw new ArtistBadRequestException();
        }

        String phone = normalizePhone(request.getPhone());

        if (phone != null && artistRepository.existsByPhone(phone)) {
            throw new DuplicateArtistPhoneException();
        }

        Artist artist = Artist.builder()
                .account(account)
                .nameKo(request.getNameKo().trim())
                .nameEn(request.getNameEn())
                .phone(phone)
                .build();

        artistRepository.save(artist);

        return ArtistCreateResponse.from(artist);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }

    // 업데이트
    @Override
    public ArtistResponse updateArtist(UUID accountId, UUID artistId, ArtistUpdateRequest request) {

        Artist artist = artistRepository.findByIdAndAccountId(artistId, accountId)
                .orElseThrow(ArtistNotFoundException::new);

        String phone = request.getPhone();

        if (phone != null) {
            phone = normalizePhone(phone);

            if (phone != null
                    && artistRepository.existsByPhoneAndIdNot(phone, artistId)) {
                throw new DuplicateArtistPhoneException();
            }
        }

        artist.updateArtistInfo(
                request.getNameKo(),
                request.getNameEn(),
                phone
        );

        return ArtistResponse.from(artist);
    }

    // 삭제
    @Override
    public void deleteArtist(
            UUID accountId,
            UUID artistId
    ) {
        Artist artist = artistRepository
                .findByIdAndAccountId(artistId, accountId)
                .orElseThrow(ArtistNotFoundException::new);

        if (artistProfileRepository.existsByArtistId(artistId)
                || exhibitionArtistMapRepository.existsByArtistId(artistId)
                || artworkArtistMapRepository.existsByArtistId(artistId)) {
            throw new ArtistHasLinkedDataException();
        }

        artistRepository.delete(artist);
    }


    // 작가 목록 조회
    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponse> getArtists() {

        return artistRepository.findAll()
                .stream()
                .map(ArtistResponse::from)
                .collect(Collectors.toList());
    }

    // 작가 상세 조회
    @Override
    @Transactional(readOnly = true)
    public ArtistResponse getArtist(UUID artistId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        return ArtistResponse.from(artist);
    }


}