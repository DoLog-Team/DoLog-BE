package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.service.ExhibitionArtistService;
import com.dolog.server.domain.exhibition.web.dto.request.artist.AddArtistRequest;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistAddResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistItemResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExhibitionArtistControllerTest {

    @Mock
    private ExhibitionArtistService exhibitionArtistService;

    @InjectMocks
    private ExhibitionArtistController controller;

    @Test
    @DisplayName("전시 참여 작가 목록은 artists와 totalCount 구조로 반환한다")
    void returnsArtistsWithTotalCount() {
        UUID exhibitionId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        ExhibitionArtistItemResponse artist =
                new ExhibitionArtistItemResponse(
                        profileId,
                        artistId,
                        "김두록",
                        "Dolog Kim",
                        "https://cdn.test/profile.webp"
                );
        ExhibitionArtistListResponse serviceResponse =
                ExhibitionArtistListResponse.from(List.of(artist));

        when(exhibitionArtistService.getArtistsByExhibition(
                exhibitionId,
                "NAME"
        )).thenReturn(serviceResponse);

        SuccessResponse<ExhibitionArtistListResponse> response =
                controller.getArtists(exhibitionId, "NAME");

        assertEquals(200, response.getHttpStatus());
        assertEquals("SUCCESS_200", response.getCode());
        assertEquals("전시 작가 목록 조회 성공", response.getMessage());
        assertEquals(1, response.getData().getTotalCount());

        JsonNode data = new ObjectMapper().valueToTree(response.getData());
        assertTrue(data.has("artists"));
        assertTrue(data.has("totalCount"));
        assertEquals(profileId.toString(), data.at("/artists/0/profileId").asText());
        assertEquals(artistId.toString(), data.at("/artists/0/artistId").asText());
        assertFalse(data.at("/artists/0").has("isPublic"));

        verify(exhibitionArtistService).getArtistsByExhibition(
                exhibitionId,
                "NAME"
        );
    }

    @Test
    @DisplayName("전시 작가 직접 추가는 201과 축소된 응답을 반환한다")
    void addsArtistAndReturnsCreatedResponse() {
        UUID accountId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        UUID exhibitionArtistId = UUID.randomUUID();
        CustomUserDetails user = new CustomUserDetails(
                accountId,
                1L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EXHIBITION_ADMIN"))
        );
        ExhibitionArtistAddResponse serviceResponse =
                new ExhibitionArtistAddResponse(
                        exhibitionArtistId,
                        ExhibitionArtistStatus.JOINED
                );

        when(exhibitionArtistService.addArtistToExhibition(
                accountId,
                exhibitionId,
                artistId
        )).thenReturn(serviceResponse);

        ResponseEntity<SuccessResponse<ExhibitionArtistAddResponse>> response =
                controller.addArtist(
                        user,
                        exhibitionId,
                        new AddArtistRequest(artistId)
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(201, response.getBody().getHttpStatus());
        assertEquals("CREATED_201", response.getBody().getCode());
        assertEquals("전시 작가 추가 성공", response.getBody().getMessage());
        assertEquals(exhibitionArtistId, response.getBody().getData().exhibitionArtistId());
        assertEquals(ExhibitionArtistStatus.JOINED, response.getBody().getData().status());
        verify(exhibitionArtistService).addArtistToExhibition(
                accountId,
                exhibitionId,
                artistId
        );
    }

    @Test
    @DisplayName("전시 작가 제외는 path의 artistId를 사용하고 data 없이 200을 반환한다")
    void removesArtistAndReturnsEmptyResponse() {
        UUID accountId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        CustomUserDetails user = new CustomUserDetails(
                accountId,
                1L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EXHIBITION_ADMIN"))
        );

        SuccessResponse<Void> response = controller.removeArtist(
                user,
                exhibitionId,
                artistId
        );

        assertEquals(200, response.getHttpStatus());
        assertEquals("SUCCESS_200", response.getCode());
        assertEquals("전시 작가 삭제 성공", response.getMessage());
        assertNull(response.getData());
        verify(exhibitionArtistService).removeArtistFromExhibition(
                accountId,
                exhibitionId,
                artistId
        );
    }
}
