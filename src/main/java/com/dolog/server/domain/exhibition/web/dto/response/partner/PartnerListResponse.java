package com.dolog.server.domain.exhibition.web.dto.response.partner;

import com.dolog.server.domain.exhibition.entity.Partner;
import com.dolog.server.domain.exhibition.entity.PartnerMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class PartnerListResponse {

    private List<PartItem> parts;

    public static PartnerListResponse from(List<Partner> partners, List<PartnerMember> allMembers) {
        return PartnerListResponse.builder()
                .parts(partners.stream()
                        .map(partner -> PartItem.from(partner, allMembers.stream()
                                .filter(m -> m.getPartner().getId().equals(partner.getId()))
                                .collect(Collectors.toList())))
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PartItem {

        @JsonProperty("part_id")
        private UUID partId;

        @JsonProperty("part_name")
        private String partName;

        private Integer order;

        private List<MemberItem> members;

        public static PartItem from(Partner partner, List<PartnerMember> members) {
            return PartItem.builder()
                    .partId(partner.getId())
                    .partName(partner.getName())
                    .order(partner.getOrder())
                    .members(members.stream().map(MemberItem::from).collect(Collectors.toList()))
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MemberItem {

        @JsonProperty("member_id")
        private UUID memberId;

        @JsonProperty("member_name")
        private String memberName;

        @JsonProperty("member_email")
        private String memberEmail;

        @JsonProperty("member_image_url")
        private String memberImageUrl;

        public static MemberItem from(PartnerMember member) {
            return MemberItem.builder()
                    .memberId(member.getId())
                    .memberName(member.getName())
                    .memberEmail(member.getEmail())
                    .memberImageUrl(member.getImageUrl())
                    .build();
        }
    }
}
