# StratusCloud

StratusCloud는 개인 개발자와 소규모 팀을 위한 셀프서비스 클라우드 플랫폼 MVP입니다. 복잡한 대형 클라우드의 핵심 운영 경험을 작은 팀도 이해하고 통제할 수 있는 형태로 제공하는 것을 목표로 합니다.

현재 저장소 기준으로 1차 MVP 구현은 Week 11 범위까지 반영되어 있으며, 남은 단계는 출시 준비와 운영 전환입니다. 이 README는 `etc/` 문서를 보지 않아도 프로젝트의 현재 상태와 실행 방법을 이해할 수 있도록 정리했습니다.

![project.png](images/project.png)

## 현재 구현 범위

### IAM / 보안

- 프로젝트 생성 및 멤버 관리
- JWT(OIDC) 기반 인증
- API Key 발급, 조회, 회수
- 정책(Policy) 생성 및 역할(Role) 바인딩
- Secret 저장 및 관리

### Audit / 운영 추적

- 감사 로그 조회
- 권한 거부 및 주요 관리 이벤트 추적

### Compute

- 이미지 카탈로그 관리
- 인스턴스 생성 및 상태 관리
- 오토스케일링 그룹
- 헬스체크 정책

### Network

- VPC
- Subnet
- Route Table / Route
- Security Group
- Load Balancer
- Elastic IP
- NAT Gateway
- DNS Record

### Storage / Governance

- Object Storage 버킷 및 오브젝트 관리
- Presigned upload / download
- 버킷 및 오브젝트 태그
- 스토리지 정책(Quota / Rate Limit)
- 프로젝트 및 버킷 단위 미터링

### Operations

- 시스템 상태 확인 API
- 운영 요약 지표
- HTTP 메트릭 집계
- 익명 헬스체크

## 기술 구성

- Backend: Kotlin + Spring Boot 멀티모듈
- Frontend: Next.js 16 + React 19 + TypeScript
- Database: PostgreSQL
- Auth Reference IdP: Keycloak
- API Contract: OpenAPI 3.1

## 저장소 구조

- `backend`: Spring Boot 기반 멀티모듈 API 서버
- `frontend`: 운영 콘솔
- `contracts`: OpenAPI 계약 문서
- `etc`: 제품 요구사항, 기술 메모, 진행 계획 문서
- `docs/plans`: 구현 계획 문서
- `images`: README 이미지 리소스

## 빠른 시작

### 1. Backend 실행

로컬에서는 Spring Boot 애플리케이션은 직접 실행하고, 외부 의존성은 Docker로 띄우는 구성을 사용합니다.

```bash
cd backend
docker compose -f docker-compose.local.yml up -d --build
./gradlew clean test
./gradlew :apps:api:bootRun
```

기본 로컬 의존성은 다음과 같습니다.

- PostgreSQL: `localhost:5432`
- Keycloak: `http://localhost:8081`
- Keycloak 관리자 계정: `admin / admin`

### 2. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

개발 서버가 실행되면 운영 콘솔은 `/console/projects`에서 확인할 수 있습니다.

## 로컬 인증 방식

StratusCloud는 현재 세 가지 방식으로 로컬 검증이 가능합니다.

### JWT(OIDC)

- 헤더: `Authorization: Bearer <JWT>`
- 기본 OIDC issuer: `http://localhost:8081/realms/stratuscloud`
- Keycloak 기반 토큰 발급 예시는 [backend/README.md](backend/README.md)를 참고하면 됩니다.

### API Key

- 헤더: `X-API-Key: <raw-key>`
- API Key는 IAM API 또는 콘솔에서 발급하고 회수할 수 있습니다.

### 레거시 헤더 RBAC

빠른 로컬 검증용으로 `SECURITY_LEGACY_RBAC_HEADER_ENABLED=true`를 켜면 레거시 헤더 인증을 사용할 수 있습니다.

```text
X-Project-Role: TENANT_ADMIN
X-Tenant-Id: 00000000-0000-0000-0000-000000000001
X-Actor-Id: 00000000-0000-0000-0000-000000000010
```

## 바로 확인할 수 있는 경로

- 운영 콘솔: `/console/projects`
- 시스템 핑: `GET /v1/system/ping`
- 운영 요약: `GET /v1/system/operations/summary`
- HTTP 메트릭: `GET /v1/system/operations/http-metrics`
- 헬스체크: `GET /actuator/health`, `GET /actuator/health/liveness`
- API 계약: `contracts/openapi.yaml`

## 검증 명령

현재 저장소 기준으로 가장 기본적인 검증 흐름은 다음과 같습니다.

### Backend

```bash
cd backend
./gradlew clean test
```

운영 성능 스모크 테스트만 단독으로 실행하려면:

```bash
cd backend
./gradlew :apps:api:test --tests 'com.stratuscloud.api.system.OperationsPerformanceSmokeTest'
```

### Frontend

```bash
cd frontend
npm run lint
npm run typecheck
npm run build
```

## 관련 문서

- 백엔드 로컬 실행, 환경변수, JWT 발급 예시: [backend/README.md](backend/README.md)
- 프론트엔드 실행 문서: [frontend/README.md](frontend/README.md)
- API 계약서: [contracts/openapi.yaml](contracts/openapi.yaml)

## 참고

`etc/` 폴더에는 제품 요구사항과 진행 계획 문서가 있지만, 루트 README는 해당 문서를 모르는 독자도 바로 사용할 수 있도록 현재 구현 상태를 직접 요약해 제공합니다.
