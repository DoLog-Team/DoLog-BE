package com.dolog.server.domain.exhibition.support;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.dolog.server.domain.exhibition.entity.Host;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// 전시 게시(공개) 필수값 8개를 한곳에서 판단한다: admin-home 응답과 게시 API 검증이 같은 기준을 본다.
// 조회는 호출하는 쪽(ExhibitionServiceImpl)에서 하고, 여기는 이미 조회된 값만 받아 판정한다.
@Component
public class ExhibitionPublishRequirement {

    public List<String> missingFields(Exhibition exhibition, ExhibitionDetail detail, ExhibitionMap map, Host host) {

        List<String> missing = new ArrayList<>();
        if (detail == null || isBlank(detail.getTitle())) missing.add("title");
        if (exhibition.getExhibitionType() == null) missing.add("exhibitionType");
        if (detail == null || detail.getStartDate() == null) missing.add("startDate");
        if (detail == null || detail.getEndDate() == null) missing.add("endDate");
        if (detail == null || isBlank(detail.getDescription())) missing.add("description");
        if (map == null || isBlank(map.getAddress())) missing.add("address");
        if (host == null || isBlank(host.getName())) missing.add("hostName");
        if (host == null || isBlank(host.getDescription())) missing.add("hostDescription");
        return missing;
    }

    public boolean isSatisfied(Exhibition exhibition, ExhibitionDetail detail, ExhibitionMap map, Host host) {
        return missingFields(exhibition, detail, map, host).isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
