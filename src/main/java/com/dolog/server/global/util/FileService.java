package com.dolog.server.global.util;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client; // v2 방식의 클라이언트

    @Value("${bucket-name}") // yml에 설정된 버킷명
    private String bucket;

    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String originalFilename = file.getOriginalFilename();
        String storeFilename = subFolder + "/" + UUID.randomUUID() + "_" + originalFilename;

        // S3에 파일 업로드 요청 (v2 방식)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storeFilename)
                .contentType(file.getContentType())
                .acl("public-read") // 권한 설정
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 저장된 URL 반환
        return s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(storeFilename).build()).toString();
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