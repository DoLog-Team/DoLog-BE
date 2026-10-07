package com.dolog.server.domain.artwork.web.dto.request;

import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 두록 어드민용 통합 수정. 선택 값은 안 보내면 기존 값을 유지한다 (작품 기본 정보 수정과 같은 규칙).
@Getter
@Setter
@NoArgsConstructor
public class ArtworkUpdateFullRequest implements ArtworkInfoFields {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String description;

    @NotBlank
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

    @NotNull
    private UUID zoneId;
    private Integer prevOrder;
    private Integer nextOrder;

    @NotEmpty
    private List<UUID> artistProfileIds;
    private Map<UUID, @Size(max = 100) String> artistRoles = new HashMap<>(); // artistProfileId → role (없으면 기존 역할 유지)

    // 안 보내면 이미지는 그대로 둔다. 보내면 이 목록으로 맞춘다 (빠진 기존 이미지는 삭제).
    private List<ImageUpdateDto> images;

    private ArtworkStatus status;
    private Boolean hidden;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ImageUpdateDto {
        private UUID id;             // 기존 이미지 유지/수정 시. 새 이미지는 null
        private String imageUrl;     // 명세 호환용. 기존 이미지는 id 로 찾으므로 쓰지 않는다
        private MultipartFile imageFile; // 새 이미지는 필수, 기존 이미지는 교체할 때만
        private String description;
        private Integer orderIndex;  // 없으면 기존 순서 유지, 새 이미지는 끝에 붙인다
    }
}
