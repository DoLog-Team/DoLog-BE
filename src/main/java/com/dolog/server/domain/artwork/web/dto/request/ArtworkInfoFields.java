package com.dolog.server.domain.artwork.web.dto.request;

import java.math.BigDecimal;
import java.util.List;

// 작품 기본 정보 수정(C-06)과 통합 수정(C-08)이 같이 쓰는 작품 정보 필드
public interface ArtworkInfoFields {
    String getTitle();
    String getCategory();
    String getDescription();
    String getShortIntro();
    List<String> getMaterials();
    BigDecimal getWidth();
    BigDecimal getHeight();
    BigDecimal getDepth();
    Integer getProductionStartYear();
    Integer getProductionStartMonth();
    Integer getProductionStartDay();
    Integer getProductionEndYear();
    Integer getProductionEndMonth();
    Integer getProductionEndDay();
    String getPurchaseUrl();
    String getPurchaseChatUrl();
    Boolean getShowPurchaseButton();
    String getYoutubeUrl();
}
