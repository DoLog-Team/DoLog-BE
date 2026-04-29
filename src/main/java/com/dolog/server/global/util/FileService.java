package com.dolog.server.global.util;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client; // v2 방식의 클라이언트

    @Value("${bucket-name}") // yml에 설정된 버킷명
    private String bucket;

    // 허용할 확장자 리스트
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");

    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String originalFilename = file.getOriginalFilename();
        validateImageExtension(originalFilename);

        // 2. 이미지를 Scrimage 객체로 로드
        ImmutableImage image = ImmutableImage.loader().fromStream(file.getInputStream());

        // 3. 리사이징 (가로 1600px 제한)
        // scaleToWidth는 비율을 유지하면서 가로를 조절합니다.
        if (image.width > 1600) {
            image = image.scaleToWidth(1600);
        }

        // 4. WebP 변환 및 바이트 배열 생성
        // WebpWriter.DEFAULT는 적절한 압축률을 제공합니다.
        byte[] webpData = image.bytes(WebpWriter.DEFAULT);

        String storeFilename = subFolder + "/" + UUID.randomUUID() + ".webp";

        // S3에 파일 업로드 요청 (v2 방식)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storeFilename)
                .contentType("image/webp")
                .acl("public-read") // 권한 설정
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(webpData));

        // 저장된 URL 반환
        return s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(storeFilename).build()).toString();
    }

    private void validateImageExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
        }
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, jpeg, png, webp만 가능)");
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null) return;

        try {
            // URL에서 Key값 추출 (v2 주소 체계 대응)
            String key = fileUrl.substring(fileUrl.lastIndexOf(".com/") + 5);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            System.err.println("S3 파일 삭제 실패: " + fileUrl);
        }
    }
}