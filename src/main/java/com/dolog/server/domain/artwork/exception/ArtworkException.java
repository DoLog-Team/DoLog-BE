package com.dolog.server.domain.artwork.exception;
import com.dolog.server.global.exception.BaseException; // BaseException 임포트
import lombok.Getter;

@Getter
public class ArtworkException extends BaseException {

    public ArtworkException(ArtworkErrorCode errorCode) {
        super(errorCode);
    }


}