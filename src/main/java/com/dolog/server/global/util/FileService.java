package com.dolog.server.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileService {

    // application.properties의 설정을 가져오되, 없으면 기본값으로 C:/uploads 사용
    @Value("${file.upload-dir:C:/uploads}")
    private String rootDir;

    /**
     * 파일 업로드 로직
     * @param file 멀티파트 파일
     * @param subFolder 세부 폴더 (예: "artist-profiles")
     * @return DB에 저장할 상대 경로 (예: "/uploads/artist-profiles/uuid_name.png")
     */
    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) return null;

        // 1. 파일명 생성 (중복 방지)
        String originalFilename = file.getOriginalFilename();
        String storeFilename = UUID.randomUUID() + "_" + originalFilename;

        // 2. 저장 경로 설정 및 폴더 생성
        Path uploadPath = Paths.get(rootDir, subFolder).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 3. 파일 물리 저장
        Path filePath = uploadPath.resolve(storeFilename);
        file.transferTo(filePath.toFile());

        // 4. DB 저장용 가상 경로 반환
        return "/uploads/" + subFolder + "/" + storeFilename;
    }

    /**
     * 파일 삭제 로직
     * @param dbPath DB에 저장된 가상 경로
     */
    public void deleteFile(String dbPath) {
        if (dbPath == null || !dbPath.startsWith("/uploads/")) return;

        try {
            // "/uploads/sub/file.jpg" -> "C:/uploads/sub/file.jpg"로 변환
            // 8번째 글자부터가 실제 폴더 구조 (예: "sub/file.jpg")
            String relativePath = dbPath.substring(9);
            Path filePath = Paths.get(rootDir).resolve(relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 삭제 실패 시 로그만 남김 (비즈니스 로직에 지장을 주지 않기 위함)
            System.err.println("파일 삭제 실패: " + dbPath);
        }
    }
}