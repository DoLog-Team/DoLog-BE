package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.global.util.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArtworkSubmissionCancellerTest {

    private final FileService fileService = mock(FileService.class);
    private final ArtworkSubmissionCanceller canceller =
            new ArtworkSubmissionCanceller(new ArtworkFileHandler(fileService));

    @BeforeEach
    void start() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void end() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("출품 취소는 위치 지도를 비우고, 파일은 커밋된 뒤에 지운다")
    void deletesLocationMapAfterCommit() {
        Artwork artwork = Artwork.builder().exhibition(mock(Exhibition.class))
                .locationMap("https://s3/map.webp").build();

        canceller.cancel(artwork);

        assertNull(artwork.getExhibition());
        assertNull(artwork.getLocationMap());
        verify(fileService, never()).deleteFile(any());
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> {
            sync.afterCommit();
            sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        });
        verify(fileService).deleteFile("https://s3/map.webp");
    }

    @Test
    @DisplayName("롤백되면 위치 지도 파일을 지우지 않는다")
    void keepsFileOnRollback() {
        Artwork artwork = Artwork.builder().exhibition(mock(Exhibition.class))
                .locationMap("https://s3/map.webp").build();

        canceller.cancel(artwork);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(fileService, never()).deleteFile(any());
    }
}
