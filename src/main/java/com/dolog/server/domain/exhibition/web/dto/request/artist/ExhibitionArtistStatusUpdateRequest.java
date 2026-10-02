package com.dolog.server.domain.exhibition.web.dto.request.artist;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ExhibitionArtistStatusUpdateRequest(
        @NotEmpty(message = "상태를 변경할 작가를 한 명 이상 선택해주세요.")
        @Size(
                max = 100,
                message = "한 번에 최대 100명의 작가만 변경할 수 있습니다."
        )
        List<@Valid @NotNull UUID> artistIds,

        @NotNull(message = "변경할 참여 상태를 입력해주세요.")
        ExhibitionArtistStatus status
) {
}
