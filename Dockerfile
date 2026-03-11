# Runtime image (JRE만)
FROM eclipse-temurin:17-jre

# 애플리케이션 실행 디렉토리
WORKDIR /app

# CI에서 만든 JAR 복사
COPY build/libs/*.jar /app/app.jar

# 실행
ENTRYPOINT ["java","-jar","/app/app.jar"]