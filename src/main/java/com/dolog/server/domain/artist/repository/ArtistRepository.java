package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {

    boolean existsByAccount(Account account);
    // 기존 코드에서 사용
    Optional<Artist> findByAccountId(UUID accountId);
    boolean existsByPhone(String phone);

    // @SQLRestriction으로 숨겨진 Artist까지 조회하여 동일한 행을 복구한다.
    @Query(value = "SELECT * FROM artists WHERE account_id = :accountId", nativeQuery = true)
    Optional<Artist> findByAccountIdIncludingDeleted(@Param("accountId") UUID accountId);

    @Query(value = "SELECT COUNT(*) FROM artists WHERE phone = :phone", nativeQuery = true)
    long countByPhoneIncludingDeleted(@Param("phone") String phone);

    @Query(value = """
            SELECT COUNT(*)
            FROM artists
            WHERE phone = :phone
              AND id <> :artistId
            """, nativeQuery = true)
    long countByPhoneIncludingDeletedAndIdNot(
            @Param("phone") String phone,
            @Param("artistId") UUID artistId
    );

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

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update Artist e set e.deletedAt = :at, e.updatedAt = :at where e.id = :id and e.deletedAt is null")
    void hideById(@org.springframework.data.repository.query.Param("id") java.util.UUID id,
            @org.springframework.data.repository.query.Param("at") java.time.LocalDateTime at);

}
