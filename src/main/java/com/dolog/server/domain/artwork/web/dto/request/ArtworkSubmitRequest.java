package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ArtworkSubmitRequest {

    @NotNull
    private UUID exhibitionId;

    // 그룹 없이 임시저장 가능 (피그마 확정) — 공개 전환 시 그룹이 있는 전시라면 필수로 체크한다.
    private UUID zoneId;

    private MultipartFile locationMapFile;
}
