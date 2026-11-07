package com.dolog.server.domain.artwork.entity.enums;

// 작품 사이즈 유형. 작가 입력은 아직 DIMENSIONS만 받고, ROUND·VARIABLE은 v1 데이터 정리로만 들어간다.
public enum ArtworkSizeType {
    // 가로(width)·세로(height)·높이(depth, 부피가 있는 경우)
    DIMENSIONS,
    // 지름(width)·높이(depth, 선택). height는 쓰지 않는다.
    ROUND,
    // 가변크기. 숫자는 비어 있다.
    VARIABLE
}
