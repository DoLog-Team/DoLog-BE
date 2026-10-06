package com.dolog.server.domain.exhibition.web.dto.request.zone;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ExhibitionZoneBulkSaveRequest {

    // 그룹 전체 목록. 이 목록으로 전체를 교체한다
    @NotNull
    @Valid
    private List<ZoneItem> zones;

    @Getter
    @NoArgsConstructor
    public static class ZoneItem {

        // null이면 새로 생성, 기존 id면 수정. 목록에 없는 기존 id는 삭제된다
        private UUID id;

        @NotBlank
        private String name;

        @NotNull
        private Integer orderId;

        private String description;
    }
}
