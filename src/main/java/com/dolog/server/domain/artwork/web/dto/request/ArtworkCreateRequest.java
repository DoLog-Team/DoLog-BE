package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkCreateRequest {

    // 로그인한 작가 기준으로 등록하며, 값이 오면 본인 artistId 와 같은지만 확인한다.
    private UUID artistId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 100)
    private String category;

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

    @NotBlank
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
