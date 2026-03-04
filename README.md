# StratusCloud

StratusCloud는 개인/소규모 팀을 위한 셀프서비스 클라우드 플랫폼 프로젝트입니다.

## 디렉토리 구조

- `backend`: Kotlin + Spring Boot 멀티모듈 API 서버
- `frontend`: Next.js 기반 운영 콘솔
- `contracts`: OpenAPI 계약 문서

## 빠른 시작

### Backend

```bash
cd backend
./gradlew clean test
./gradlew :apps:api:bootRun
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## 현재 구현 상태

- 1주차 기반 구조(모듈/CI/OpenAPI/샘플 API/콘솔 랜딩) 구현
- `GET /v1/system/ping` 샘플 API 및 테스트 추가
