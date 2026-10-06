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

        if (file == null || file.isEmpty()) {
            return null;
        }

        String url = upload(file);
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

    private String upload(MultipartFile file) {

        try {
            return fileService.uploadFile(file, "artworks/main");
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
