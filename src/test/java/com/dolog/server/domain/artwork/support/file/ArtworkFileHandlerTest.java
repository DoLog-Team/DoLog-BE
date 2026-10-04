package com.dolog.server.domain.artwork.support.file;

import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.global.util.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArtworkFileHandlerTest {

    private final FileService fileService = mock(FileService.class);
    private final ArtworkFileHandler handler = new ArtworkFileHandler(fileService);
    private final MockMultipartFile file = new MockMultipartFile("mainImageFile", "a.png", "image/png", new byte[]{1});

    @BeforeEach
    void startTransaction() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void endTransaction() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("교체 시 기존 파일은 커밋 전에는 지우지 않고 커밋 후에 지운다")
    void deletesOldFileOnlyAfterCommit() throws IOException {
        when(fileService.uploadFile(any(), eq("artworks/main"))).thenReturn("https://s3/new.webp");

        assertEquals("https://s3/new.webp", handler.replaceMainImage("https://s3/old.webp", file));
        verify(fileService, never()).deleteFile(any());

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileService).deleteFile("https://s3/old.webp");
        verify(fileService, never()).deleteFile("https://s3/new.webp");
    }

    @Test
    @DisplayName("교체 후 롤백되면 기존 파일은 남기고 새로 올린 파일을 지운다")
    void deletesNewFileOnRollback() throws IOException {
        when(fileService.uploadFile(any(), eq("artworks/main"))).thenReturn("https://s3/new.webp");

        handler.replaceMainImage("https://s3/old.webp", file);
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileService).deleteFile("https://s3/new.webp");
        verify(fileService, never()).deleteFile("https://s3/old.webp");
    }

    @Test
    @DisplayName("등록 후 롤백되면 올린 파일을 지운다")
    void deletesUploadedFileOnRollback() throws IOException {
        when(fileService.uploadFile(any(), eq("artworks/main"))).thenReturn("https://s3/new.webp");

        handler.uploadMainImage(file);
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileService).deleteFile("https://s3/new.webp");
    }

    @Test
    @DisplayName("등록 후 커밋되면 올린 파일은 지우지 않는다")
    void keepsUploadedFileOnCommit() throws IOException {
        when(fileService.uploadFile(any(), eq("artworks/main"))).thenReturn("https://s3/new.webp");

        handler.uploadMainImage(file);
        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileService, never()).deleteFile(any());
    }

    @Test
    @DisplayName("새 파일이 없으면 아무 파일도 지우지 않는다")
    void noFileNoDelete() {
        assertNull(handler.replaceMainImage("https://s3/old.webp", null));
        complete(TransactionSynchronization.STATUS_COMMITTED);

        verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("업로드 IO 오류는 ARTWORK_004, 허용되지 않는 형식은 ARTWORK_008 로 던진다")
    void mapsUploadErrors() throws IOException {
        when(fileService.uploadFile(any(), any())).thenThrow(new IOException("io"));
        ArtworkException io = assertThrows(ArtworkException.class, () -> handler.uploadMainImage(file));
        assertEquals("ARTWORK_004", io.getErrorCode().getCode());

        reset(fileService);
        when(fileService.uploadFile(any(), any())).thenThrow(new IllegalArgumentException("형식"));
        ArtworkException ext = assertThrows(ArtworkException.class, () -> handler.uploadMainImage(file));
        assertEquals("ARTWORK_008", ext.getErrorCode().getCode());
    }

    private void complete(int status) {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                sync.afterCommit();
            }
            sync.afterCompletion(status);
        }
    }
}
