package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionHostService;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionHostUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionHostResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionHostController {

    private final ExhibitionHostService exhibitionHostService;

    @PutMapping("/{exhibitionId}/host")
    public SuccessResponse<ExhibitionHostResponse> upsertHost(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionHostUpsertRequest request) {

        ExhibitionHostResponse response = exhibitionHostService.upsertExhibitionHost(exhibitionId, request);
        return SuccessResponse.ok(response, "주최 기관 정보 등록/수정 성공");
    }
}
