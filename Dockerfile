# Runtime image (JRE) — 패치버전 고정으로 재현성 확보 (dependabot(docker)이 자동 갱신)
FROM eclipse-temurin:17.0.19_10-jre

WORKDIR /app

RUN useradd -r -u 1001 appuser

COPY --chown=appuser:appuser build/libs/*.jar /app/app.jar

USER appuser
EXPOSE 8080

# exec로 java를 PID 1로 → docker stop의 SIGTERM이 전달되어 graceful shutdown
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
