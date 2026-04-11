package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomSplashResponse;

import java.util.UUID;

public interface ExhibitionCustomSplashService {

    ExhibitionCustomSplashResponse upsertCustomSplash(UUID exhibitionId, String splashImg);
}
