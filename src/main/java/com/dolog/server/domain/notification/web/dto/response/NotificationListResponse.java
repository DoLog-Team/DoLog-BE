package com.dolog.server.domain.notification.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationListResponse {

    // 최신순, size개까지
    private List<NotificationResponse> notifications;

    // 읽지 않은 알림 전체 수 (size와 무관)
    private long unreadCount;
}
