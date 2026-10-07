package com.dolog.server.domain.artwork.support.file;

import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

// S3 는 DB 롤백을 따라가지 않으므로 파일 삭제는 트랜잭션 결과에 맞춰 미룬다.
@Component
@RequiredArgsConstructor
public class ArtworkFileHandler {

    private final FileService fileService;

    /**
     * 대표 이미지 업로드. 파일이 없으면 null. 롤백되면 올린 파일을 지운다.
     */
    public String uploadMainImage(MultipartFile file) {
        return uploadTo(file, "artworks/main");
    }

    /**
     * 작품 위치 지도 업로드 (출품 시). 파일이 없으면 null. 롤백되면 올린 파일을 지운다.
     */
    public String uploadLocationMap(MultipartFile file) {
        return uploadTo(file, "artworks/maps");
    }

    /**
     * 작품 상세 이미지 업로드. 파일이 없으면 null. 롤백되면 올린 파일을 지운다.
     */
    public String uploadDetailImage(MultipartFile file) {
        return uploadTo(file, "artworks/detail");
    }

    /**
     * 커밋된 뒤에 파일을 지운다. 트랜잭션 밖이면 바로 지운다.
     */
    public void deleteAfterCommit(String fileUrl) {
        afterCommit(fileUrl);
    }

    private String uploadTo(MultipartFile file, String folder) {

        if (file == null || file.isEmpty()) {
            return null;
        }

        String url = upload(file, folder);
        afterRollback(url);
        return url;
    }

    /**
     * 대표 이미지 교체. 새 파일이 없으면 null 을 반환하고 기존 파일은 그대로 둔다.
     * 기존 파일은 커밋된 뒤에만 지운다.
     */
    public String replaceMainImage(String oldUrl, MultipartFile file) {

        String newUrl = uploadMainImage(file);

        if (newUrl != null) {
            afterCommit(oldUrl);
        }

        return newUrl;
    }

    private String upload(MultipartFile file, String folder) {

        try {
            return fileService.uploadFile(file, folder);
        } catch (IOException e) {
            throw new ArtworkException(ArtworkErrorCode.FILE_UPLOAD_ERROR);
        } catch (IllegalArgumentException e) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private void afterCommit(String fileUrl) {

        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileService.deleteFile(fileUrl);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileService.deleteFile(fileUrl);
            }
        });
    }

    private void afterRollback(String fileUrl) {

        if (fileUrl == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    fileService.deleteFile(fileUrl);
                }
            }
        });
    }
}
