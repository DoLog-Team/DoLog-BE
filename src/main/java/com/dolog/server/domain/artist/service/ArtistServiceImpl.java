package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.exception.artistError.ArtistAlreadyExistsException;
import com.dolog.server.domain.artist.exception.artistError.ArtistBadRequestException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.DuplicateArtistPhoneException;
import com.dolog.server.domain.artist.exception.artistError.ArtistHasLinkedDataException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.repository.ArtistSnsRepository;
import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.*;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artwork.repository.ArtworkArtistMapRepository;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.like.repository.ArtistProfileLikeRepository;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final AccountRepository accountRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistSnsRepository artistSnsRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;
    private final ArtworkArtistMapRepository artworkArtistMapRepository;
    private final ArtworkRepository artworkRepository;
    private final ArtistProfileLikeRepository artistProfileLikeRepository;

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim();
    }

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

        String requestEmail = normalizeEmail(request.getEmail());
        String accountEmail = normalizeEmail(account.getEmail());

        if (requestEmail != null
                && (accountEmail == null
                || !requestEmail.equalsIgnoreCase(accountEmail))) {
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
    public ArtistResponse updateArtist(UUID accountId, UUID artistId, ArtistUpdateRequest request) {

        Artist artist = artistRepository.findByIdAndAccountId(artistId, accountId)
                .orElseThrow(ArtistNotFoundException::new);

        String nameKo = normalizeOptionalNameKo(request.getNameKo());
        String phone = request.getPhone();

        if (phone != null) {
            phone = normalizePhone(phone);

            if (phone != null
                    && artistRepository.existsByPhoneAndIdNot(phone, artistId)) {
                throw new DuplicateArtistPhoneException();
            }
        }

        artist.updateArtistInfo(
                nameKo,
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
    public ArtistPublicResponse getArtist(UUID artistId, String visitorId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        ArtistProfile representativeProfile = artistProfileRepository
                .findLatestPublicJoinedProfile(
                        artistId,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);

        List<ArtistPublicResponse.SnsItem> snsList =
                representativeProfile == null
                        ? List.of()
                        : artistSnsRepository
                                .findByArtistProfileIdOrderByCreatedAtAscIdAsc(
                                        representativeProfile.getId()
                                )
                                .stream()
                                .map(sns -> new ArtistPublicResponse.SnsItem(
                                        sns.getPlatformName(),
                                        sns.getUrl()
                                ))
                                .toList();

        List<ArtistPublicResponse.ExhibitionItem> exhibitions =
                exhibitionArtistMapRepository
                        .findPublicJoinedExhibitionsByArtistId(artistId)
                        .stream()
                        .map(map -> {
                            var exhibition = map.getExhibition();
                            var detail = exhibition.getExhibitionDetail();
                            var exhibitionMap = exhibition.getExhibitionMap();

                            String location = null;
                            if (exhibitionMap != null) {
                                location = exhibitionMap.getDetailLocation();
                                if (location == null || location.isBlank()) {
                                    location = exhibitionMap.getAddress();
                                }
                            }

                            return new ArtistPublicResponse.ExhibitionItem(
                                    exhibition.getId(),
                                    detail.getTitle(),
                                    exhibition.getSlug(),
                                    detail.getExhibitionImg(),
                                    exhibition.getUnivName(),
                                    exhibition.getDeptName(),
                                    location,
                                    detail.getStartDate(),
                                    detail.getEndDate()
                            );
                        })
                        .toList();

        List<ArtistPublicResponse.ArtworkItem> artworks = artworkRepository
                .findPublishedByArtistId(artistId)
                .stream()
                .map(artwork -> new ArtistPublicResponse.ArtworkItem(
                        artwork.getId(),
                        artwork.getTitle(),
                        artwork.getMainImg()
                ))
                .toList();

        int likeCount = Math.toIntExact(
                artistProfileLikeRepository.countByArtistId(artistId)
        );
        boolean liked = visitorId != null
                && artistProfileLikeRepository.existsByArtistIdAndVisitorId(
                artistId,
                visitorId
        );
        long viewCount = artistProfileRepository
                .sumViewCountByArtistId(artistId);

        return ArtistPublicResponse.builder()
                .artistId(artist.getId())
                .nameKo(artist.getNameKo())
                .nameEn(artist.getNameEn())
                .bio(representativeProfile == null
                        ? null
                        : representativeProfile.getBio())
                .profileImg(representativeProfile == null
                        ? null
                        : representativeProfile.getProfileImg())
                .email(representativeProfile == null
                        ? null
                        : representativeProfile.getEmail())
                .snsList(snsList)
                .exhibitions(exhibitions)
                .artworks(artworks)
                .likeCount(likeCount)
                .liked(liked)
                .viewCount(viewCount)
                .build();
    }


}
