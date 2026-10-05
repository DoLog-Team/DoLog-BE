package com.dolog.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) {
		// 서버 위치(UTC 등)와 관계없이 LocalDateTime.now()가 한국 시간을 쓰도록 고정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(ServerApplication.class, args);
	}

}
