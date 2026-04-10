package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.custom.ExhibitionCustomThemeRequest;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomThemeResponse;

import java.util.UUID;

public interface ExhibitionCustomThemeService {

    ExhibitionCustomThemeResponse upsertCustomTheme(UUID exhibitionId, ExhibitionCustomThemeRequest request);
}
