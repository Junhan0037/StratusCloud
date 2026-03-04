# StratusCloud Backend

Kotlin + Spring Boot 멀티모듈 구조입니다.

## 모듈
- `apps/api`: 외부 REST API 진입점
- `modules/common`: 공통 기능(trace id, 에러 모델)
- `modules/*`: 도메인 모듈(IAM/Compute/Network/Storage/Governance/Audit)

## 실행

Gradle Wrapper(`./gradlew`)를 사용하므로 별도 Gradle 설치 없이 실행할 수 있습니다.

```bash
cd backend
./gradlew clean test
./gradlew :apps:api:bootRun
```
