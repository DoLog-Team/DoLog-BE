package com.dolog.server.domain.exhibition.web.dto.request.basic;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class EntryCodeRequest {
    @Future(message = "코드 만료 시각은 현재보다 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
}
