package com.dolog.server.domain.bts.web.controller;

import com.dolog.server.domain.bts.service.BtsService;
import com.dolog.server.domain.bts.web.dto.request.BtsCreateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsCreateResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exhibitions")
public class BtsController {

    private final BtsService btsService;

    @PostMapping("/bts")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<BtsCreateResponse> createBts(
            @ModelAttribute BtsCreateRequest request
    ) throws IOException {
        BtsCreateResponse response = btsService.createBts(request);
        return SuccessResponse.ok(response, "BTS 콘텐츠 등록 성공");
    }
}