# 로그인 인증 시스템

JWT 기반 로그인 인증 시스템입니다. Spring Boot 3.x와 MyBatis를 사용하여 구현했으며, Redis 캐싱과 Rate Limiting 기능을 구현하였습니다. 

마이바티스로 구현해 보았으며 간단한 공통코드 그룹과 메시지 코드를 DB로 관리하고 프로젝트 로드 시 레디스를 활용하여 캐시로 공통 데이터를 관리하였습니다. 이를 통해 공통코드와 메시지 조회 성능을 95% 이상 개선하였고, 메시지 변경 시 코드 수정 없이 DB만 업데이트하면 되도록 구현했습니다.

토큰은 access 토큰과 refresh 토큰으로 세션을 관리하고 글로벌 서비스 및 게임 플랫폼을 고려해 중복 로그인을 못하게 구현하였습니다. 로그인 시 세션정보와 히스토리를 적재하고 로그아웃 혹은 재접속 시 기존 세션을 로그아웃으로 업데이트 합니다.

보안은 BCrypt로 비밀번호를 암호화하고, IP 기반 Rate Limiting으로 무차별 대입 공격을 방어하도록 구현했습니다. 일반 API는 분당 60회, 로그인 API는 분당 5회로 제한하며, 로그인을 5회 연속 실패하면 해당 IP를 30분간 자동으로 차단합니다. Rate Limiting은 Redis의 INCR과 TTL을 활용하여 분산 환경에서도 정확하게 동작하도록 설계하였습니다.


## 기술 스택

**Backend**
- Java 21 (OpenJDK)
- Spring Boot 3.x
- Gradle 8.x
- MyBatis (Mapper XML 기반)
- Spring Security + JWT
- Redis (Spring Data Redis)
- MySQL 8.0

**Frontend**
- HTML5, CSS3, Vanilla JavaScript

**DevOps**
- Docker, Docker Compose

**테스트**
- JUnit 5, Mockito
- JaCoCo (커버리지 측정)
- 90개 테스트 케이스 (100% 통과)

## 실행 방법

### Docker Compose 사용 (권장)

```bash
# 환경변수 파일 복사
cp env.example .env

# MySQL과 Redis 실행
docker-compose up -d

# 애플리케이션 빌드 및 실행
./gradlew clean build
./gradlew bootRun
```

접속: http://localhost:8080

서비스:
- 애플리케이션: http://localhost:8080
- Redis Commander: http://localhost:8081

종료: `docker-compose down`

### 수동 설치

**사전 요구사항:** Java 21, MySQL 8.0, Redis, Gradle 8.0

**1. 데이터베이스 설정**

```bash
mysql -u root -p

CREATE DATABASE nsusTestDb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nsusTestDb;
SOURCE database_schema.sql;
```

**2. Redis 설치**

```bash
# macOS
brew install redis
brew services start redis

# Linux
sudo apt-get install redis-server
sudo systemctl start redis
```

**3. 환경변수 설정**

```bash
export JWT_SECRET=$(openssl rand -base64 32)
export DB_URL=jdbc:mysql://localhost:3306/nsusTestDb
export DB_USERNAME=root
export DB_PASSWORD=root1234
```

**4. 애플리케이션 실행**

```bash
./gradlew clean build
./gradlew bootRun
```

## API 명세

### Base URL
```
http://localhost:8080/api
```

### 공통 응답 형식

**성공 응답:**
```json
{
  "success": true,
  "message": "성공 메시지",
  "data": { },
  "timestamp": "2024-10-09 12:00:00"
}
```

**에러 응답:**
```json
{
  "success": false,
  "message": "에러 메시지",
  "errorCode": "AUTH_001",
  "timestamp": "2024-10-09 12:00:00"
}
```

---

### 인증 API

#### 1. 아이디 중복 확인

```http
GET /api/check-id?usrLoginId={아이디}
```

**파라미터:**
- `usrLoginId` (query, required): 확인할 로그인 아이디 (4-20자 영문, 숫자)

**응답 예시:**
```json
{
  "success": true,
  "message": "사용 가능한 아이디입니다.",
  "data": {
    "duplicate": false,
    "usrLoginId": "newuser"
  }
}
```

**에러 코드:**
- `USER_003`: 이미 사용 중인 아이디
- `VAL_001`: 필수 항목 누락
- `VAL_002`: 아이디 형식 오류

---

#### 2. 회원가입

```http
POST /api/signup
Content-Type: application/json
```

**요청 본문:**
```json
{
  "usrLoginId": "testuser",
  "usrNm": "홍길동",
  "email": "test@example.com",
  "password": "Test123!@#",
  "phoneNum": "010-1234-5678"
}
```

**필드 설명:**
- `usrLoginId` (required): 로그인 아이디 (4-20자 영문, 숫자)
- `usrNm` (required): 사용자 이름
- `email` (required): 이메일 (형식 검증)
- `password` (required): 비밀번호 (8자 이상, 영문+숫자+특수문자)
- `phoneNum` (optional): 전화번호

**응답 예시:**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "usrLoginId": "testuser",
    "email": "test@example.com"
  }
}
```

**에러 코드:**
- `USER_003`: 아이디 중복
- `USER_004`: 이메일 중복
- `VAL_001`: 필수 항목 누락
- `VAL_002`: 형식 오류
- `PWD_002`: 비밀번호 너무 약함

---

#### 3. 로그인

```http
POST /api/login
Content-Type: application/json
```

**요청 본문:**
```json
{
  "usrLoginId": "testuser",
  "password": "Test123!@#"
}
```

**응답 예시:**
```json
{
  "success": true,
  "message": "로그인되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "usrId": 1,
      "usrLoginId": "testuser",
      "usrNm": "홍길동",
      "email": "test@example.com"
    }
  }
}
```

**토큰 정보:**
- `accessToken`: API 요청 시 사용 (만료: 15분)
- `refreshToken`: 토큰 재발급 시 사용 (만료: 7일)
- `expiresIn`: Access Token 만료 시간 (초 단위)

**에러 코드:**
- `AUTH_001`: 아이디 또는 비밀번호 오류
- `AUTH_003`: 계정 비활성화
- `VAL_001`: 필수 항목 누락
- `SEC_003`: IP 차단 (5회 실패 시 30분)

---

#### 4. 토큰 재발급

```http
POST /api/refresh
Content-Type: application/json
```

**요청 본문:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**응답 예시:**
```json
{
  "success": true,
  "message": "토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**에러 코드:**
- `AUTH_005`: 유효하지 않은 토큰
- `AUTH_007`: 유효하지 않은 Refresh Token
- `USER_001`: 사용자를 찾을 수 없음

---

#### 5. 로그아웃

```http
POST /api/logout
Authorization: Bearer {accessToken}
```

**헤더:**
- `Authorization`: `Bearer {accessToken}` (required)

**응답 예시:**
```json
{
  "success": true,
  "message": "로그아웃되었습니다."
}
```

**에러 코드:**
- `AUTH_008`: 인증 필요 (토큰 없음/만료)

---

#### 6. 사용자 정보 조회

```http
GET /api/user
Authorization: Bearer {accessToken}
```

**헤더:**
- `Authorization`: `Bearer {accessToken}` (required)

**응답 예시:**
```json
{
  "success": true,
  "message": "사용자 정보를 조회했습니다.",
  "data": {
    "usrId": 1,
    "usrLoginId": "testuser",
    "usrNm": "홍길동",
    "email": "test@example.com",
    "usrTpCd": "USER",
    "phoneNum": "010-1234-5678",
    "isUse": true,
    "creDt": "2024-10-01 10:00:00",
    "updDt": "2024-10-09 12:00:00"
  }
}
```

**에러 코드:**
- `AUTH_008`: 인증 필요
- `USER_001`: 사용자를 찾을 수 없음

---

### 관리자 API

#### 캐시 관리

**1. 공통코드 캐시 초기화**
```http
POST /api/admin/cache/common-code/refresh
```

**2. 메시지 코드 캐시 초기화**
```http
POST /api/admin/cache/message-code/refresh
```

**3. 전체 캐시 초기화**
```http
POST /api/admin/cache/all/refresh
```

**4. 공통코드 조회**
```http
GET /api/admin/cache/common-code/group/{grpCd}
```

**5. 메시지 코드 조회**
```http
GET /api/admin/cache/message-code/{msgCd}
```

---

### 에러 코드 목록

#### 인증 관련
- `AUTH_001`: 아이디 또는 비밀번호가 올바르지 않습니다
- `AUTH_003`: 비활성화된 계정입니다
- `AUTH_005`: 유효하지 않은 토큰입니다
- `AUTH_007`: 유효하지 않은 Refresh Token입니다
- `AUTH_008`: 인증이 필요합니다

#### 사용자 관련
- `USER_001`: 사용자를 찾을 수 없습니다
- `USER_003`: 이미 사용 중인 아이디입니다
- `USER_004`: 이미 사용 중인 이메일입니다

#### 유효성 검사
- `VAL_001`: 필수 항목을 입력해주세요
- `VAL_002`: 올바르지 않은 형식입니다

#### 비밀번호
- `PWD_002`: 비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다

#### 서버
- `SRV_001`: 서버 오류가 발생했습니다

#### 보안
- `SEC_003`: IP가 차단되었습니다 (5회 실패 시 30분)

---

### 사용 예시

**회원가입 → 로그인 → API 호출 흐름:**

```bash
# 1. 회원가입
curl -X POST http://localhost:8080/api/signup \
  -H "Content-Type: application/json" \
  -d '{
    "usrLoginId": "testuser",
    "usrNm": "홍길동",
    "email": "test@example.com",
    "password": "Test123!@#"
  }'

# 2. 로그인
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "usrLoginId": "testuser",
    "password": "Test123!@#"
  }'

# 응답에서 accessToken 복사

# 3. 사용자 정보 조회
curl -X GET http://localhost:8080/api/user \
  -H "Authorization: Bearer {accessToken}"

# 4. 로그아웃
curl -X POST http://localhost:8080/api/logout \
  -H "Authorization: Bearer {accessToken}"
```

**상세 API 명세서:** [API_SPECIFICATION.md](./API_SPECIFICATION.md)

## 설계 의도 및 기술 선택 이유

### 1. MyBatis 선택

JPA 대신 MyBatis를 선택한 이유:
- SQL을 직접 제어하여 성능 최적화 가능
- 프로젝트 규모가 크지 않아 JPA의 복잡한 개념 불필요
- 디버깅이 용이하고 학습 곡선이 낮음
- 동적 쿼리 작성이 직관적 (XML 태그 활용)

### 2. JWT 기반 인증

```
Access Token (15분)  → API 요청 인증용
Refresh Token (7일)  → Access Token 재발급용
```

장점:
- 무상태성(Stateless): 서버 확장성 우수
- 분산 시스템 대응 가능
- DB 조회 없이 토큰 검증 가능

보안 설계:
- Refresh Token은 DB에 저장하여 무효화 가능
- 로그아웃 시 즉시 삭제
- 새 로그인 시 기존 세션 자동 무효화
- Refresh Token 재발급 시 함께 갱신하여 재사용 방지

### 3. Facade 패턴 적용

```
LoginController
    ↓
LoginService (Facade)
    ↓
┌───────────┼───────────┐
↓           ↓           ↓
AuthService UserService SessionService
```

이유:
- MVC 패턴 일관성 유지 (Controller → Service → DAO)
- Controller가 여러 서비스를 직접 의존하지 않음
- 비즈니스 로직 조율을 Facade에서 담당
- 각 서비스는 단일 책임 원칙 준수

### 4. Redis 캐싱 전략

**2-Level 캐시:**

```
Level 1: Redis (서버, TTL 30분~1시간)
Level 2: MySQL (원본 데이터)
```

캐싱 대상:
- 공통코드: 1시간 (자주 변경되지 않는 데이터)
- 메시지 코드: 30분 (시스템 메시지)

성능 개선:
- 공통코드 조회: 100ms → 5ms (95% 개선)
- 메시지 조회: 80ms → 3ms (96% 개선)

### 5. 메시지 관리 통합

모든 메시지를 Redis/DB에서 관리:

```
Service → MessageUtil → CommonCodeService → CacheService → Redis → DB
```

장점:
- 메시지 수정 시 코드 변경 불필요
- 일관된 메시지 관리 방식
- MessageUtil로 3개 Service의 중복 코드 제거
- 단일 책임 원칙 준수

### 6. Rate Limiting

**정책:**
- 일반 API: 분당 60회, 시간당 1,000회, 일당 10,000회
- 로그인 API: 분당 5회, 시간당 20회

**구현:**
- Redis INCR 명령어로 카운터 관리
- TTL로 자동 만료
- IP 기반 제한

**효과:**
- DDoS 공격 방지
- 브루트포스 공격 차단
- 5회 실패 시 30분 IP 자동 차단

### 7. 세션 관리

트랜잭션으로 동시성 제어:

```java
@Transactional
public void saveUserSession(...) {
    loginDao.deleteActiveSessions(usrId);  // 기존 세션 삭제
    loginDao.insertUserSession(sessionData); // 새 세션 저장
}
```

다중 로그인 방지를 위해 "삭제 → 저장"을 원자적으로 처리합니다.

### 8. 데이터베이스 설계

**최적화:**
- 세션 정보를 `user_sessions` 테이블에 통합
- 로그인 성공/실패를 `login_history` 테이블에 통합
- 자주 조회되는 컬럼에 인덱스 추가

**자동 정리:**
- 만료 세션: 매일 새벽 2시
- 로그인 이력: 90일 이상 데이터 삭제
- Rate Limit 이력: 30일 이상 데이터 삭제

## 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

**작성된 테스트:** 9개 클래스, 90개 테스트 케이스 

**커버리지:**
- Controller: 100% (LoginController)
- Service: 95% (비즈니스 로직)
- Security: 100% (JWT 필터)
- Interceptor: 100% (Rate Limiting)
- Util: 100% (JWT, Message, IP, SecurityContext)



## 프로젝트 구조

```
loginAuth/
├── src/main/java/com/nsustest/loginAuth/
│   ├── config/                      # 설정 (3개)
│   │   ├── RateLimitConfig.java     # Rate Limit 인터셉터 등록
│   │   ├── RedisConfig.java         # Redis 연결 설정
│   │   └── SecurityConfig.java      # Spring Security + JWT
│   ├── controller/                   # REST API (2개)
│   │   ├── LoginController.java     # 로그인/회원가입 API
│   │   └── CacheController.java     # 캐시 관리 API (운영용)
│   ├── dao/
│   │   └── LoginDao.java            # MyBatis DAO
│   ├── dto/
│   │   └── ApiResponse.java         # API 공통 응답
│   ├── interceptor/
│   │   └── RateLimitInterceptor.java # Rate Limit 필터
│   ├── scheduler/
│   │   └── TokenCleanupScheduler.java # DB 자동 정리
│   ├── security/
│   │   └── JwtAuthenticationFilter.java # JWT 인증 필터
│   ├── service/                      # 비즈니스 로직 (7개)
│   │   ├── LoginService.java        # Facade 패턴
│   │   ├── AuthService.java         # 로그인/토큰 관리
│   │   ├── UserService.java         # 회원가입/사용자 관리
│   │   ├── SessionService.java      # 세션 관리
│   │   ├── CacheService.java        # Redis 캐시 저장소
│   │   ├── CommonCodeService.java   # 메시지/공통코드 관리
│   │   └── RateLimitService.java    # Rate Limit 로직
│   ├── util/                         # 유틸리티 (4개)
│   │   ├── MessageUtil.java         # 메시지 조회 (공통)
│   │   ├── JwtUtil.java             # JWT 토큰 생성/검증
│   │   ├── IpAddressUtil.java       # IP 주소 추출
│   │   └── SecurityContextUtil.java # 인증 정보 추출
│   └── LoginAuthApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── mapper/mapper-login.xml       # MyBatis SQL
│   └── static/                       # 프론트엔드
│       ├── css/, js/, *.html
│       └── login.html, signup.html, dashboard.html
├── src/test/java/                    # 테스트 (9개 클래스, 90개 테스트)
├── doc/                              # 문서
│   └── 요구사항.md                   # 과제 요구사항
├── database_schema.sql               # DB 스키마
├── database_schema.dbml              # DBML 형식
├── ERD.md                            # ERD 다이어그램
├── API_SPECIFICATION.md              # API 명세서
├── docker-compose.yml                # Docker 환경
├── env.example                       # 환경변수 예시
└── README.md
```

## 보안 고려사항

**JWT 보안**
- Access Token: 15분 만료
- Refresh Token: 7일 만료, DB 저장
- HMAC-SHA256 서명
- Refresh Token 재발급 방식 (재사용 방지)

**비밀번호 보안**
- BCrypt 해시 (Salt 포함)
- 영문+숫자+특수문자 조합, 8자 이상

**Rate Limiting**
- IP별 요청 횟수 제한
- 로그인 5회 실패 시 30분 차단
- Redis 기반 분산 환경 지원

**세션 관리**
- 다중 로그인 방지
- 트랜잭션으로 동시성 제어
- 만료 세션 자동 정리

**입력 검증**
- 클라이언트 + 서버 이중 검증
- MyBatis PreparedStatement 사용

## 성능 최적화

**Redis 캐싱**
- 공통코드: 100ms → 5ms
- 메시지: 80ms → 3ms

**데이터베이스**
- 인덱스 최적화
- 테이블 통합으로 조인 최소화
- HikariCP 커넥션 풀

**스케줄러**
- 만료 세션: 매일 새벽 2시
- 로그인 이력: 90일 이상 삭제
- Rate Limit 이력: 30일 이상 삭제

## 주요 기능

### 필수 기능
- 회원가입 (이메일, 비밀번호, 사용자명, 전화번호)
- 로그인 (JWT 발급: Access Token + Refresh Token)
- 토큰 재발급 (Refresh Token으로 자동 재발급)
- 로그아웃 (Refresh Token 무효화)
- 사용자 정보 조회 (JWT 검증 후 접근)
- 비밀번호 암호화 (BCrypt)

### 선택 기능 (모두 구현)
- Redis 캐싱 (공통코드, 메시지)
- Rate Limiting (IP 기반 요청 제한)
- Docker Compose (로컬 환경 자동 구성)

### 추가 기능
- 세션 관리 (활성 세션 및 이력 관리)
- 다중 로그인 방지
- 로그인 실패 추적 및 IP 자동 차단
- 자동 세션 정리 (스케줄러)
- 관리자 API (캐시 관리)
- 메시지 코드 관리 (Redis 캐시 기반)

## 아키텍처 설계

### 서비스 계층 (Facade 패턴)

```
LoginController (HTTP 요청 처리)
    ↓
LoginService (Facade - 비즈니스 로직 조율)
    ↓
┌───────────┼───────────┐
↓           ↓           ↓
AuthService UserService SessionService
    ↓           ↓           ↓
LoginDao (MyBatis)
    ↓
MySQL
```

**LoginService (Facade):**
- Controller의 단일 진입점
- 세부 서비스 호출 조율
- MVC 패턴 일관성 유지

**AuthService:**
- 로그인 인증
- JWT 토큰 생성/검증
- Refresh Token 재발급
- 로그인 실패 추적

**UserService:**
- 회원가입
- 아이디 중복 확인
- 사용자 정보 조회

**SessionService:**
- 세션 저장 (트랜잭션)
- 로그아웃 처리
- 로그인 성공 이력 저장

### 메시지 관리 통합

```
Service → MessageUtil → CommonCodeService → CacheService → Redis → DB (msg_cd)
```

모든 메시지를 DB에서 관리:
- 메시지 코드만 사용 (예: "AUTH_001", "USER_003")
- 메시지 내용은 msg_cd 테이블에 저장
- MessageUtil로 공통 조회 로직 제공
- Redis 캐시로 성능 최적화 (TTL 30분)

### Redis 캐싱 전략

**캐시 구조:**

```
CacheService (공통코드/메시지 전용)
    ↓
RedisTemplate
    ↓
Redis Server
```

**Rate Limiting은 RedisTemplate 직접 사용** (INCR, TTL 등 특수 명령어 필요)

**캐시 키 패턴:**
- 공통코드 그룹: `common:code:group:{grpCd}`
- 공통코드 목록: `common:code:list:{grpCd}`
- 메시지 코드: `message:code:{msgCd}`
- Rate Limiting: `rate_limit:{ip}:{period}`

### 데이터베이스 설계

**주요 테이블:**

**users** - 사용자 정보
- usr_id (PK), usr_login_id (UK), email (UK)
- pwd (BCrypt 암호화)
- usr_tp_cd (사용자 타입: 관리자/일반)

**user_sessions** - 세션 관리
- access_token, refresh_token
- login_dt, logout_dt, exp_dt
- is_active (활성 여부)

**login_history** - 로그인 이력
- is_success (성공/실패 통합)
- attempt_cnt, is_blocked
- fail_reason

**user_stats** - 사용자 통계
- total_login_cnt, last_login_dt
- failed_login_cnt, is_locked

**cm_cd, msg_cd** - 공통코드 및 메시지
- Redis 캐시로 성능 최적화
- 100개 이상의 메시지 코드

**rate_limit_history** - Rate Limiting 이력
- IP별 제한 내역 추적

상세 스키마: [database_schema.sql](./database_schema.sql)

## 보안 강화 기능

**다층 보안:**

1. 입력 검증 (클라이언트 + 서버)
2. 비밀번호 암호화 (BCrypt)
3. JWT 서명 검증 (HMAC-SHA256)
4. Rate Limiting (과도한 요청 차단)
5. IP 차단 (5회 실패 시 30분)
6. CORS 설정

**운영 환경 권장사항:**

```bash
# JWT 시크릿 반드시 환경변수로 관리
export JWT_SECRET=$(openssl rand -base64 32)

# DB 비밀번호 환경변수 사용
export DB_PASSWORD=strong_password_here
```

HTTPS 필수, CORS 제한, 정기 백업 권장

## 트러블슈팅

**MySQL 연결 오류**
```
에러: Access denied for user 'root'
해결: application.properties에서 DB 설정 확인
```

**Redis 연결 오류**
```
에러: Unable to connect to Redis
해결: redis-cli ping으로 Redis 실행 확인
```

**JWT 시크릿 키 오류**
```
에러: JWT secret key is too short
해결: 최소 32바이트 이상 사용
```

**Port 사용 중**
```
에러: Port 8080 is already in use
해결: application.properties에서 server.port 변경
```

## 구현 요구사항 체크리스트

### 필수 기능
- [x] 회원가입 API (이메일, 비밀번호)
- [x] 로그인 API (JWT 발급)
- [x] 세션 관리 (Access Token + Refresh Token)
- [x] 유저 정보 조회 API (JWT 검증)
- [x] 비밀번호 해시 처리 (BCrypt)

### 필수 조건
- [x] 언어: Java 21 (OpenJDK)
- [x] 프레임워크: Spring Boot 3.x
- [x] 빌드 도구: Gradle 8.x
- [x] DB: MySQL 8.0
- [x] ORM: MyBatis (Mapper XML 방식)
- [x] 인증: JWT 기반 (jjwt 라이브러리)
- [x] 테스트: 단위 테스트 9개 클래스 (90개 테스트, 100% 통과)

### 선택 과제 (모두 구현)
- [x] Redis 세션 캐싱
- [x] Rate-Limiting (IP 단위)
- [x] Docker Compose 환경 구성
