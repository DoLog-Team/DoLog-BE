package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionCustomTheme;
import com.dolog.server.domain.exhibition.entity.enums.ThemeMode;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionCustomThemeRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.request.custom.ExhibitionCustomThemeRequest;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomThemeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionCustomThemeServiceImpl implements ExhibitionCustomThemeService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionCustomThemeRepository exhibitionCustomThemeRepository;

    @Override
    public ExhibitionCustomThemeResponse upsertCustomTheme(UUID exhibitionId, ExhibitionCustomThemeRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        Optional<ExhibitionCustomTheme> existing = exhibitionCustomThemeRepository.findByExhibitionId(exhibitionId);

        ExhibitionCustomTheme theme;
        if (existing.isPresent()) {
            theme = existing.get();
            theme.update(request.getThemeMode(), request.getBtnBg(), request.getBtnText(),
                    request.getCtaBg(), request.getCtaText());
        } else {
            ThemeMode themeMode = request.getThemeMode() != null ? request.getThemeMode() : ThemeMode.LIGHT;
            theme = ExhibitionCustomTheme.builder()
                    .exhibition(exhibition)
                    .themeMode(themeMode)
                    .btnBg(request.getBtnBg())
                    .btnText(request.getBtnText())
                    .ctaBg(request.getCtaBg())
                    .ctaText(request.getCtaText())
                    .build();
            exhibitionCustomThemeRepository.save(theme);
        }

        return ExhibitionCustomThemeResponse.from(theme);
    }
}
