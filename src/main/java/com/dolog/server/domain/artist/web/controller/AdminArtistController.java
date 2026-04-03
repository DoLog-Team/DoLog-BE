package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.artist.service.ArtistService;
import com.dolog.server.domain.artist.web.dto.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.ArtistResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor

@RequestMapping
public class AdminArtistController {

    private final ArtistService artistService;

    //작가 생성
    @PostMapping("/admin/artists")
    public SuccessResponse<ArtistResponse> createArtist(
            @RequestBody @Valid ArtistCreateRequest request
    ) {
        ArtistResponse data = artistService.createArtist(request);
        return SuccessResponse.created(data);
    }
}
