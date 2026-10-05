package com.dolog.server.domain.like.web.dto.request;

// 쿠키를 못 쓰는 환경에서 로컬스토리지에 보관한 visitorId 를 보낼 때만 사용한다.
public record LikeRequest(
        String visitorId
) {}
