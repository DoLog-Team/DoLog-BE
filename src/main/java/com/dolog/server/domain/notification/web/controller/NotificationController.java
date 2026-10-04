package com.dolog.server.domain.notification.web.controller;

import com.dolog.server.domain.notification.service.NotificationService;
import com.dolog.server.domain.notification.web.dto.response.NotificationResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "notification")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 조회")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'EXHIBITION_ADMIN', 'ARTIST_ADMIN')")
    @GetMapping
    public SuccessResponse<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return SuccessResponse.ok(notificationService.getMyNotifications(user.getId()), "알림 목록 조회 성공");
    }

    @Operation(summary = "특정 알림 읽음 처리")
    @PreAuthorize("hasAnyRole('EXHIBITION_ADMIN', 'ARTIST_ADMIN')")
    @PatchMapping("/{notificationId}/read")
    public SuccessResponse<NotificationResponse> markAsRead(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID notificationId
    ) {
        return SuccessResponse.ok(notificationService.markAsRead(notificationId, user.getId()), "알림을 읽음 처리했습니다.");
    }

    @Operation(summary = "모든 알림 읽음 처리")
    @PreAuthorize("hasAnyRole('EXHIBITION_ADMIN', 'ARTIST_ADMIN')")
    @PatchMapping("/read-all")
    public SuccessResponse<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        notificationService.markAllAsRead(user.getId());
        return SuccessResponse.ok(null, "모든 알림을 읽음 처리했습니다.");
    }
}
