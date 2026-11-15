package com.dolog.server;

import com.dolog.server.global.util.SiteUrlProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local")
class ServerApplicationTests {

	@Autowired
	private SiteUrlProvider siteUrlProvider;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("local 프로파일의 dolog.site-url 이 전시 주소에 쓰인다")
	void readsSiteUrlFromLocalProfile() {
		assertEquals("http://localhost:3000/local-demo", siteUrlProvider.exhibitionUrl("local-demo"));
	}

}
