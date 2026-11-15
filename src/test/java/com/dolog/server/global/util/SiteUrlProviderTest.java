package com.dolog.server.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SiteUrlProviderTest {

    @Test
    @DisplayName("전시 주소는 사이트 주소에 slug 를 붙인다")
    void buildsExhibitionUrl() {
        assertEquals("https://dolog.kr/dev-test-03", new SiteUrlProvider("https://dolog.kr").exhibitionUrl("dev-test-03"));
    }

    @Test
    @DisplayName("사이트 주소 끝에 / 가 있어도 // 가 되지 않는다")
    void ignoresTrailingSlash() {
        assertEquals("https://dolog.kr/dev-test-03", new SiteUrlProvider("https://dolog.kr/").exhibitionUrl("dev-test-03"));
    }
}
