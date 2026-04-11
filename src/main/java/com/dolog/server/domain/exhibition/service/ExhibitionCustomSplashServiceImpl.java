package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomSplashResponse;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionCustomSplashServiceImpl implements ExhibitionCustomSplashService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public ExhibitionCustomSplashResponse upsertCustomSplash(UUID exhibitionId, String splashImg) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        checkPlanSupport(exhibition);
        validateSplashImg(splashImg);

        ExhibitionDetail detail = exhibitionDetailRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_DETAIL_NOT_FOUND));

        detail.updateSplashImg(splashImg);

        return ExhibitionCustomSplashResponse.of(exhibitionId, splashImg);
    }

    private void checkPlanSupport(Exhibition exhibition) {
        if (exhibition.getAccount() == null) {
            return;
        }
        boolean hasActivePlan = subscriptionRepository
                .existsByAccountIdAndStatus(exhibition.getAccount().getId(), SubscriptionStatus.ACTIVE);
        if (!hasActivePlan) {
            throw new ExhibitionException(ExhibitionErrorCode.SPLASH_PLAN_NOT_SUPPORTED);
        }
    }

    private void validateSplashImg(String splashImg) {
        if (splashImg == null) return;
        try {
            URI uri = new URI(splashImg);
            String scheme = uri.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme) || uri.getHost() == null) {
                throw new ExhibitionException(ExhibitionErrorCode.SPLASH_INVALID_IMAGE_URL);
            }
        } catch (java.net.URISyntaxException e) {
            throw new ExhibitionException(ExhibitionErrorCode.SPLASH_INVALID_IMAGE_URL);
        }
    }
}
