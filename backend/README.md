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

## 인증/인가(Week 3)

- JWT(OIDC): `Authorization: Bearer <JWT>`
- API Key: `X-API-Key: <raw-key>`
- 로컬 Keycloak 예시 환경변수:

```bash
export SECURITY_OIDC_ISSUER_URI=http://localhost:8081/realms/stratuscloud
export SECURITY_OIDC_JWK_SET_URI=http://localhost:8081/realms/stratuscloud/protocol/openid-connect/certs
```

- 주요 IAM-2 API:
  - `POST /v1/iam/policies`
  - `GET /v1/iam/policies?tenantId=...`
  - `POST /v1/iam/roles`
  - `POST /v1/iam/api-keys`
  - `DELETE /v1/iam/api-keys/{keyId}`

## 운영 안정화(Week 11)

- 익명 헬스체크: `GET /actuator/health`, `GET /actuator/health/liveness`
- 관리자 운영 API:
  - `GET /v1/system/operations/summary`
  - `GET /v1/system/operations/http-metrics`
- 성능 스모크 단독 실행:

```bash
cd backend
./gradlew :apps:api:test --tests 'com.stratuscloud.api.system.OperationsPerformanceSmokeTest'
```
