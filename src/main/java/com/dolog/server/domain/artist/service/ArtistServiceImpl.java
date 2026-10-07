package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistAccountNotEligibleException;
import com.dolog.server.domain.artist.exception.artistError.ArtistAccountNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.ArtistAlreadyExistsException;
import com.dolog.server.domain.artist.exception.artistError.ArtistBadRequestException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.DuplicateArtistPhoneException;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.*;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final AccountRepository accountRepository;

    // 작가 생성
    @Override
    public ArtistCreateResponse createArtist(
            UUID actorAccountId,
            ArtistCreateRequest request
    ) {
        requireDologAdmin(actorAccountId);

        Account targetAccount = accountRepository
                .findByIdForUpdate(request.getAccountId())
                .orElseThrow(ArtistAccountNotFoundException::new);
        requireEligibleArtistAccount(targetAccount);

        String phone = normalizePhone(request.getPhone());
        String nameKo = request.getNameKo().trim();

        var existingArtist = artistRepository
                .findByAccountIdIncludingDeleted(targetAccount.getId());

        if (existingArtist.isPresent()) {
            Artist artist = existingArtist.get();

            if (artist.getDeletedAt() == null) {
                throw new ArtistAlreadyExistsException();
            }

            if (phone != null
                    && artistRepository.countByPhoneIncludingDeletedAndIdNot(
                    phone,
                    artist.getId()
            ) > 0) {
                throw new DuplicateArtistPhoneException();
            }

            artist.restore(nameKo, request.getNameEn(), phone);
            return ArtistCreateResponse.from(artist);
        }

        if (phone != null
                && artistRepository.countByPhoneIncludingDeleted(phone) > 0) {
            throw new DuplicateArtistPhoneException();
        }

        Artist artist = Artist.builder()
                .account(targetAccount)
                .nameKo(nameKo)
                .nameEn(request.getNameEn())
                .phone(phone)
                .build();

        artistRepository.save(artist);

        return ArtistCreateResponse.from(artist);
    }

    private Account requireDologAdmin(UUID actorAccountId) {
        Account actor = accountRepository.findById(actorAccountId)
                .orElseThrow(JwtInvalidException::new);

        if (actor.getRole() != Role.DOLOG_ADMIN) {
            throw new AccessDeniedException("두록 관리자만 작가 정보를 관리할 수 있습니다.");
        }

        return actor;
    }

    private void requireEligibleArtistAccount(Account targetAccount) {
        if (targetAccount.getRole() != Role.ARTIST_ADMIN
                || targetAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ArtistAccountNotEligibleException();
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String normalized = phone.replaceAll("[\\s-]+", "");

        if (!normalized.matches("\\d+")) {
            throw new ArtistBadRequestException();
        }

        return normalized;
    }

    private String normalizeOptionalNameKo(String nameKo) {
        if (nameKo == null) {
            return null;
        }

        if (nameKo.isBlank()) {
            throw new ArtistBadRequestException();
        }

        return nameKo.trim();
    }

    // 업데이트
    @Override
    public ArtistUpdateResponse updateArtist(
            UUID actorAccountId,
            UUID artistId,
            ArtistUpdateRequest request
    ) {
        requireDologAdmin(actorAccountId);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        String nameKo = normalizeOptionalNameKo(request.getNameKo());
        String phone = request.getPhone();

        if (phone != null) {
            phone = normalizePhone(phone);

            if (phone != null
                    && artistRepository.countByPhoneIncludingDeletedAndIdNot(
                    phone,
                    artistId
            ) > 0) {
                throw new DuplicateArtistPhoneException();
            }
        }

        artist.updateArtistInfo(
                nameKo,
                request.getNameEn(),
                phone
        );

        return ArtistUpdateResponse.from(artist);
    }

    // 삭제
    @Override
    public void deleteArtist(
            UUID actorAccountId,
            UUID artistId
    ) {
        requireDologAdmin(actorAccountId);

        Artist artist = artistRepository
                .findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        artist.softDelete(LocalDateTime.now());
    }


    // 작가 목록 조회
    @Override
    @Transactional(readOnly = true)
    public ArtistListResponse getArtists(
            String search,
            int page,
            int size
    ) {
        if (page < 0 || size < 1) {
            throw new ArtistBadRequestException();
        }

        String keyword = search == null || search.isBlank()
                ? null
                : search.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Artist> result;

        if (keyword == null) {
            result = artistRepository.findAll(pageable);
        } else {
            result =
                    artistRepository
                            .findByNameKoContainingIgnoreCaseOrNameEnContainingIgnoreCaseOrAccount_EmailContainingIgnoreCase(
                                    keyword,
                                    keyword,
                                    keyword,
                                    pageable
                            );
        }

        return ArtistListResponse.builder()
                .artists(
                        result.getContent()
                                .stream()
                                .map(ArtistListItemResponse::from)
                                .toList()
                )
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    // 작가 상세 조회
    @Override
    @Transactional(readOnly = true)
    public ArtistPublicResponse getArtist(UUID artistId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        return ArtistPublicResponse.from(artist);
    }


}
