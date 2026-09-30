package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.UUID;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {

    boolean existsByAccount(Account account);
    // 기존 코드에서 사용
    Optional<Artist> findByAccountId(UUID accountId);
    boolean existsByPhone(String phone);

    // 수정·삭제 시 본인 Artist 조회
    Optional<Artist> findByIdAndAccountId(UUID artistId, UUID accountId);
    boolean existsByPhoneAndIdNot(String phone, UUID artistId);

    @EntityGraph(attributePaths = "account")
    Page<Artist> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "account")
    Page<Artist>
    findByNameKoContainingIgnoreCaseOrNameEnContainingIgnoreCaseOrAccount_EmailContainingIgnoreCase(
            String nameKo,
            String nameEn,
            String accountEmail,
            Pageable pageable
    );
}
