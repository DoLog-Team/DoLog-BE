package com.dolog.server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${access-key}")
    private String accessKey;

    @Value("${secret-key}")
    private String secretKey;

    @Value("${region}")
    private String region;

    @Value("${bucket-name}")
    private String bucketName;

    // 공통 크리덴셜 생성 메소드
    private AwsBasicCredentials getCredentials() {
        return AwsBasicCredentials.create(accessKey, secretKey);
    }

    // [서버 환경용]
    @Bean
    @Profile("prod")
    public S3Client s3ClientProd() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                .build();
    }

    // [로컬 환경용]
    @Bean
    @Profile("local")
    public S3Client s3ClientLocal() {
        S3Client client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                .endpointOverride(java.net.URI.create("http://localhost:4566"))
                .forcePathStyle(true)
                .build();

        // 자동 생성 로직
        try {
            client.createBucket(b -> b.bucket(bucketName));
            System.out.println("✅ 로컬 테스트용 S3 버킷 생성 완료: " + bucketName);
        } catch (Exception e) {
            System.out.println("ℹ️ 로컬 버킷이 이미 존재합니다.");
        }

        return client;
        }
    }
