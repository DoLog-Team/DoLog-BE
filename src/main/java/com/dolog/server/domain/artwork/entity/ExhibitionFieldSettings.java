package com.dolog.server.domain.artwork.entity;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

// 전시별 작품 항목 설정. 행이 없으면 모든 항목이 선택/노출인 것으로 본다.
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "exhibition_field_settings")
public class ExhibitionFieldSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false, unique = true)
    private Exhibition exhibition;

    @Column(name = "required_main_img", nullable = false)
    private boolean requiredMainImg;

    @Column(name = "required_size", nullable = false)
    private boolean requiredSize;

    @Column(name = "required_materials", nullable = false)
    private boolean requiredMaterials;

    @Column(name = "required_location_map", nullable = false)
    private boolean requiredLocationMap;

    @Column(name = "hidden_size", nullable = false)
    private boolean hiddenSize;

    @Column(name = "hidden_materials", nullable = false)
    private boolean hiddenMaterials;

    @Column(name = "hidden_location_map", nullable = false)
    private boolean hiddenLocationMap;

    @Column(name = "hidden_production_period", nullable = false)
    private boolean hiddenProductionPeriod;

    @Column(name = "hidden_production_year", nullable = false)
    private boolean hiddenProductionYear;

    public static ExhibitionFieldSettings defaultsFor(Exhibition exhibition) {
        return ExhibitionFieldSettings.builder().exhibition(exhibition).build();
    }

    public void update(boolean requiredMainImg, boolean requiredSize, boolean requiredMaterials,
                       boolean requiredLocationMap, boolean hiddenSize, boolean hiddenMaterials,
                       boolean hiddenLocationMap, boolean hiddenProductionPeriod, boolean hiddenProductionYear) {
        this.requiredMainImg = requiredMainImg;
        this.requiredSize = requiredSize;
        this.requiredMaterials = requiredMaterials;
        this.requiredLocationMap = requiredLocationMap;
        this.hiddenSize = hiddenSize;
        this.hiddenMaterials = hiddenMaterials;
        this.hiddenLocationMap = hiddenLocationMap;
        this.hiddenProductionPeriod = hiddenProductionPeriod;
        this.hiddenProductionYear = hiddenProductionYear;
    }

    // 사이즈는 가로/세로만 본다. 높이는 부피가 있는 작품만 입력한다.
    public boolean isSatisfiedBy(Artwork artwork) {
        if (requiredMainImg && isBlank(artwork.getMainImg())) return false;
        if (requiredSize && (artwork.getWidth() == null || artwork.getHeight() == null)) return false;
        if (requiredMaterials && artwork.getMaterials().isEmpty()) return false;
        if (requiredLocationMap && isBlank(artwork.getLocationMap())) return false;
        return true;
    }

    // 출품 정보 쪽에서 필수로 켜둔 항목 수 (최대 4개). 작성률 계산에서 "출품 정보" 분모로 쓴다.
    public int requiredFieldCount() {
        int count = 0;
        if (requiredMainImg) count++;
        if (requiredSize) count++;
        if (requiredMaterials) count++;
        if (requiredLocationMap) count++;
        return count;
    }

    // 필수로 켠 항목 중 실제로 채운 개수. 작성률 계산에서 "출품 정보" 분자로 쓴다.
    public int filledRequiredFieldCount(Artwork artwork) {
        int filled = 0;
        if (requiredMainImg && !isBlank(artwork.getMainImg())) filled++;
        if (requiredSize && artwork.getWidth() != null && artwork.getHeight() != null) filled++;
        if (requiredMaterials && !artwork.getMaterials().isEmpty()) filled++;
        if (requiredLocationMap && !isBlank(artwork.getLocationMap())) filled++;
        return filled;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
