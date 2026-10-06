package com.dolog.server.domain.like.entity;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

// artist_profile_likes 에는 updated_at 이 없어서 BaseEntity 를 상속하지 않는다.
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "artist_profile_likes")
public class ArtistProfileLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @Column(name = "visitor_id", length = 64, nullable = false)
    private String visitorId;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
