# 4. Git·PR·CI

[목차](00-start.md)

## 작업 흐름

1. `git status`로 기존 변경을 확인한다. 다른 작업의 미커밋 변경을 되돌리지 않는다.
2. 간단한 이슈를 먼저 만들고 이슈에 맞는 작업 브랜치로 전환한다.
3. 관련 코드·호출부를 읽고 변경 및 검증한다.
4. PR은 보통 `dev` 대상이다. 선행 PR에 의존하는 경우 실제 기반 브랜치를 명시한다.

기존 브랜치 예시는 `feat/#327-eunho`, `settings/#317-eunho`다. 커밋 예시는 `feat : 로컬 개발용 시드 데이터 추가 (#327)`다. 새 문서 작업은 같은 형태로 유형·이슈 번호를 붙인다. 포크 전용 흐름이나 다른 저장소의 CODEOWNERS 규칙을 가져오지 않는다.

```bash
git status --short --branch
git fetch origin
# 이슈 생성 후, 기존 작업을 보존하고 기준 브랜치를 확인한 경우
# git switch -c 'docs/#이슈번호-이름' origin/dev
git diff --check
./gradlew test
```

PR 본문은 `.github/PULL_REQUEST_TEMPLATE.md`를 사용한다. 체크리스트는 실제 수행한 검증만 체크한다. 문서만 바꿨다면 링크·명령·설정 대조 결과를 적는다.

## 자동화

| 파일 | 동작 |
|---|---|
| CI.yml | dev 대상 PR, dev push, 수동 실행에서 Gradle build/test |
| deploy-dev.yml | dev CI 완료 성공 또는 수동 실행 후 dev 배포 |
| deploy-prod.yml | 정식 Release published로 운영 배포, pre-release 제외 |
| db-diff.yml | 마이그레이션 변경의 전후 스키마 비교 |
| backup-check.yml | 매일 UTC 21:00 백업 검사 및 실패 알림 |

CI의 MySQL은 8.0이며 로컬/배포 Compose는 8.4다. CI에서만 통과한 SQL도 실제 대상 버전에서 확인한다. dev push의 CI는 멀티아치 이미지를 `dev`와 커밋 SHA 태그로 게시한다. 운영은 별도 릴리즈 태그를 사용한다.

## 공유 문서와 로컬 메모

`docs/onboarding`은 공유 문서다. `AGENTS.md`, `CLAUDE.md`, `docs/handoff.md`는 로컬 전용이며 강제로 추가하지 않는다. 문서와 코드의 변경이 함께 리뷰되도록 한다.
