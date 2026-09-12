package com.dolog.server;

import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.global.util.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
class DependencyUpgradeTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtTokenProvider jwt;
    @Autowired AccountRepository accounts;
    @Value("${admin.email}") String email;
    @Value("${admin.password}") String password;

    @Test
    void loginRefreshAndSwaggerRemainUsable() throws Exception {
        var response = mvc.perform(post("/api/auth/login").contextPath("/api").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        var tokens = json.readTree(response.getResponse().getContentAsString()).path("data");
        var accountId = accounts.findByEmail(email).orElseThrow().getId();
        assertEquals(accountId, jwt.parseAccessToken(tokens.path("accessToken").asText()).accountId());
        var refresh = mvc.perform(post("/api/auth/refresh").contextPath("/api").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("refreshToken", tokens.path("refreshToken").asText()))))
                .andExpect(status().isOk()).andReturn();
        assertEquals(accountId, jwt.parseAccessToken(json.readTree(refresh.getResponse().getContentAsString())
                .path("data").path("accessToken").asText()).accountId());
        mvc.perform(get("/api/v3/api-docs").contextPath("/api")).andExpect(status().isOk()).andExpect(jsonPath("$.paths").isNotEmpty());
        assertThrows(RuntimeException.class, () -> jwt.parseAccessToken("invalid.token.value"));
    }

    @Test
    void imageConversionProducesWebpForS3Upload() throws Exception {
        var s3 = mock(S3Client.class);
        var utilities = mock(S3Utilities.class);
        when(s3.utilities()).thenReturn(utilities);
        when(utilities.getUrl(any(GetUrlRequest.class))).thenReturn(new URL("https://example.com/image.webp"));
        var service = new FileService(s3);
        ReflectionTestUtils.setField(service, "bucket", "test-bucket");
        var png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", png);
        service.uploadFile(new MockMultipartFile("file", "test.png", "image/png", png.toByteArray()), "test");
        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(request.capture(), body.capture());
        assertEquals("image/webp", request.getValue().contentType());
        try (var stream = body.getValue().contentStreamProvider().newStream()) {
            byte[] header = stream.readNBytes(12);
            assertEquals("RIFF", new String(header, 0, 4, StandardCharsets.US_ASCII));
            assertEquals("WEBP", new String(header, 8, 4, StandardCharsets.US_ASCII));
        }
    }
}
