package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;

import java.util.UUID;

public record MyArtistProfileResponse(
        UUID artistId,
        UUID profileId,
        String nameKo,
        String nameEn,
        String bio,
        String email,
        String purchaseContactUrl,
        String profileImg
) {

    public static MyArtistProfileResponse from(
            Artist artist,
            ArtistProfile profile
    ) {
        return new MyArtistProfileResponse(
                artist.getId(),
                profile.getId(),
                profile.getNameKo(),
                profile.getNameEn(),
                profile.getBio(),
                profile.getEmail(),
                profile.getPurchaseContactUrl(),
                profile.getProfileImg()
        );
    }
}
