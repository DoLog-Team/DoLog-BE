package com.dolog.server.domain.artist.repository.projection;

public interface ArtistProfileListItemProjection {

    String getProfileId();

    String getArtistId();

    String getNameKo();

    String getNameEn();

    String getProfileImg();

    Boolean getIsPublic();

    long getViewCount();

    long getLikeCount();
}
