package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ArtworkUpdateRequest {

    // 보낸 필드만 반영한다. 빈 문자열로 필수값을 지우는 것은 막는다.
    @Pattern(regexp = "(?s).*\\S.*")
    @Size(max = 255)
    private String title;

    @Size(max = 100)
    private String category;

    // 보내면 전체 교체, 빈 값 하나만 보내면 전부 삭제
    private List<@Size(max = 100) String> materials;

    @PositiveOrZero
    @Digits(integer = 8, fraction = 2)
    private BigDecimal width;

    @PositiveOrZero
    @Digits(integer = 8, fraction = 2)
    private BigDecimal height;

    @PositiveOrZero
    @Digits(integer = 8, fraction = 2)
    private BigDecimal depth;

    @Pattern(regexp = "(?s).*\\S.*")
    private String description;

    @Size(max = 255)
    private String shortIntro;

    @Min(1000) @Max(9999)
    private Integer productionStartYear;

    @Min(1) @Max(12)
    private Integer productionStartMonth;

    @Min(1) @Max(31)
    private Integer productionStartDay;

    @Min(1000) @Max(9999)
    private Integer productionEndYear;

    @Min(1) @Max(12)
    private Integer productionEndMonth;

    @Min(1) @Max(31)
    private Integer productionEndDay;

    @Size(max = 255)
    private String purchaseUrl;

    @Size(max = 255)
    private String purchaseChatUrl;

    private Boolean showPurchaseButton;

    @Size(max = 255)
    private String youtubeUrl;

    @Size(max = 100)
    private String artistRole;

    private MultipartFile mainImageFile;
}
