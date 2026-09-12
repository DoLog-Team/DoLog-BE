package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record EntryCodeResponse(String entryCode,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime entryCodeExpiresAt) {}
