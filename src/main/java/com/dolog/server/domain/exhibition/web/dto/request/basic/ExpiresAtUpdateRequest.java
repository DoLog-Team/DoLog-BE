package com.dolog.server.domain.exhibition.web.dto.request.basic;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ExpiresAtUpdateRequest {
    // 과거 시각도 허용한다 (전시 종료 후 기록 정리, 즉시 만료 처리 등). 안 보내면 만료 없음으로 설정된다.
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
}
