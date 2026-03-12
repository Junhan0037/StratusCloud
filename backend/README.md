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

## 로컬 Docker 의존성

Spring Boot 앱은 로컬에서 직접 실행하고, 외부 의존성만 Docker로 띄우는 구성을 제공한다.

### 1. PostgreSQL + Keycloak 기동

```bash
cd backend
docker compose -f docker-compose.local.yml up -d --build
```

- PostgreSQL: `localhost:5432`
- Keycloak: `http://localhost:8081`
- Keycloak 관리자 계정: `admin / admin`

### 2. 환경변수 설정

기본 설정값만 사용할 경우 DB 환경변수는 생략해도 된다. 로컬 인증 테스트를 쉽게 하려면 아래 값을 함께 설정한다.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/stratuscloud
export DB_USERNAME=stratus
export DB_PASSWORD=stratus
export SECURITY_OIDC_ISSUER_URI=http://localhost:8081/realms/stratuscloud
export SECURITY_OIDC_JWK_SET_URI=http://localhost:8081/realms/stratuscloud/protocol/openid-connect/certs
export SECURITY_LEGACY_RBAC_HEADER_ENABLED=true
```

### 3. Spring Boot 실행

```bash
cd backend
./gradlew :apps:api:bootRun
```

### 4. JWT 발급 예시

Keycloak realm import에는 `stratuscloud-local` public client와 `local-admin / local-admin` 테스트 사용자가 포함되어 있다.

```bash
curl -X POST http://localhost:8081/realms/stratuscloud/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=stratuscloud-local' \
  -d 'username=local-admin' \
  -d 'password=local-admin'
```

토큰에는 로컬 검증용 `tenant_id`, `global_roles=["TENANT_ADMIN"]` claim이 포함되도록 realm이 준비되어 있다.

### 5. 레거시 헤더 인증 예시

JWT 없이 빠르게 API를 확인하려면 `SECURITY_LEGACY_RBAC_HEADER_ENABLED=true`를 켜고 아래 헤더를 사용하면 된다.

```text
X-Project-Role: TENANT_ADMIN
X-Tenant-Id: 00000000-0000-0000-0000-000000000001
X-Actor-Id: 00000000-0000-0000-0000-000000000010
```

### 6. 종료

```bash
cd backend
docker compose -f docker-compose.local.yml down
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
