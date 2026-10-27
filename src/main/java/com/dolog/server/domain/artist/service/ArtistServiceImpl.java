package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistAccountNotEligibleException;
import com.dolog.server.domain.artist.exception.artistError.ArtistAccountNotFoundException;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.exception.artistError.ArtistAlreadyExistsException;
import com.dolog.server.domain.artist.exception.artistError.ArtistBadRequestException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.DuplicateArtistPhoneException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileAccessDeniedException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.repository.ArtistSnsRepository;
import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.*;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artist.support.ArtistProfileImageValidator;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.like.repository.ArtistProfileLikeRepository;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import com.dolog.server.global.util.FileService;
import com.dolog.server.global.util.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final AccountRepository accountRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistProfileProvisionService artistProfileProvisionService;
    private final ArtistSnsRepository artistSnsRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;
    private final ArtworkRepository artworkRepository;
    private final ArtistProfileLikeRepository artistProfileLikeRepository;
    private final FileService fileService;
    private final ArtistProfileImageValidator profileImageValidator;

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim();
    }

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
            artistProfileProvisionService.ensureProfile(artist);
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
        artistProfileProvisionService.ensureProfile(artist);

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

        return updateArtistInfo(artist, request);
    }

    @Override
    public ArtistUpdateResponse updateMyArtist(
            UUID accountId,
            ArtistUpdateRequest request
    ) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(JwtInvalidException::new);
        requireEligibleArtistAccount(account);
        Artist artist = artistRepository.findByAccountId(accountId)
                .orElseThrow(ArtistNotFoundException::new);

        return updateArtistInfo(artist, request);
    }

    @Override
    public ArtistProfileUpdateResponse updateMyArtistProfile(
            UUID accountId,
            ArtistProfileUpdateRequest request,
            MultipartFile profileImg
    ) throws IOException {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(JwtInvalidException::new);
        requireEligibleArtistAccount(account);
        Artist artist = artistRepository.findByAccountId(accountId)
                .orElseThrow(ArtistNotFoundException::new);
        ArtistProfile profile = artistProfileRepository
                .findByArtistId(artist.getId())
                .orElseThrow(ArtistProfileNotFoundException::new);

        return applyProfileUpdate(profile, request, profileImg);
    }

    @Override
    public ArtistProfileUpdateResponse updateArtistProfile(
            UUID accountId,
            UUID profileId,
            ArtistProfileUpdateRequest request,
            MultipartFile profileImg
    ) throws IOException {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(JwtInvalidException::new);
        ArtistProfile profile = artistProfileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);

        requireCanUpdateProfile(account, profile);
        return applyProfileUpdate(profile, request, profileImg);
    }

    private void requireCanUpdateProfile(
            Account account,
            ArtistProfile profile
    ) {
        if (account.getRole() == Role.DOLOG_ADMIN) {
            return;
        }

        boolean isOwner = account.getRole() == Role.ARTIST_ADMIN
                && profile.getArtist().getAccount() != null
                && profile.getArtist().getAccount().getId()
                .equals(account.getId());

        if (!isOwner) {
            throw new ArtistProfileAccessDeniedException();
        }
    }

    private ArtistProfileUpdateResponse applyProfileUpdate(
            ArtistProfile profile,
            ArtistProfileUpdateRequest request,
            MultipartFile profileImg
    ) throws IOException {

        profileImageValidator.validate(profileImg);
        String oldImageUrl = profile.getProfileImg();
        String newImageUrl = null;

        if (profileImg != null && profileImg.isEmpty()) {
            profile.clearProfileImg();
            fileService.deleteFile(oldImageUrl);
        } else if (profileImg != null) {
            newImageUrl = fileService.uploadFile(
                    profileImg,
                    "artist-profiles"
            );
            fileService.deleteFile(oldImageUrl);
        }

        profile.updateProfile(
                request.getNameKo(),
                request.getNameEn(),
                request.getBio() == null
                        ? null
                        : TextUtils.normalizeNewlines(request.getBio()),
                request.getEmail(),
                request.getPurchaseContactUrl(),
                newImageUrl
        );

        return new ArtistProfileUpdateResponse(profile.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public MyArtistProfileResponse getMyArtistProfile(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);
        requireEligibleArtistAccount(account);

        Artist artist = artistRepository.findByAccountId(accountId)
                .orElseThrow(ArtistNotFoundException::new);
        ArtistProfile profile = artistProfileRepository
                .findByArtistId(artist.getId())
                .orElseThrow(ArtistProfileNotFoundException::new);

        return MyArtistProfileResponse.from(artist, profile);
    }

    private ArtistUpdateResponse updateArtistInfo(Artist artist, ArtistUpdateRequest request) {
        return updateArtistInfo(
                artist,
                request.getNameKo(),
                request.getNameEn(),
                request.getPhone()
        );
    }

    private ArtistUpdateResponse updateArtistInfo(
            Artist artist,
            String requestedNameKo,
            String requestedNameEn,
            String requestedPhone
    ) {
        String nameKo = normalizeOptionalNameKo(requestedNameKo);
        String phone = requestedPhone;

        if (phone != null) {
            phone = normalizePhone(phone);

            if (phone != null
                    && artistRepository.countByPhoneIncludingDeletedAndIdNot(
                    phone,
                    artist.getId()
            ) > 0) {
                throw new DuplicateArtistPhoneException();
            }
        }

        artist.updateArtistInfo(
                nameKo,
                requestedNameEn,
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
                            .findByProfile_NameKoContainingIgnoreCaseOrProfile_NameEnContainingIgnoreCaseOrAccount_EmailContainingIgnoreCase(
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
                .findByArtistId(artistId)
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
                .profileId(representativeProfile == null
                        ? null
                        : representativeProfile.getId())
                .nameKo(representativeProfile == null ? null : representativeProfile.getNameKo())
                .nameEn(representativeProfile == null ? null : representativeProfile.getNameEn())
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
