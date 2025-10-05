# 로그인 인증 시스템 (Login Authentication System)

JWT 기반의 로그인 인증 시스템으로, Access Token과 Refresh Token을 사용하여 보안을 강화한 Spring Boot 애플리케이션입니다.

## 🚀 주요 기능

- ✅ **회원가입**: 이메일, 비밀번호로 회원가입 (BCrypt 해시 처리)
- ✅ **로그인**: JWT 기반 인증 (Access Token + Refresh Token)
- ✅ **토큰 재발급**: Access Token 만료 시 Refresh Token으로 자동 재발급
- ✅ **로그아웃**: Refresh Token 무효화
- ✅ **사용자 정보 조회**: JWT 검증을 통과한 사용자만 접근 가능
- ✅ **세션 관리**: 활성 연결 정보 및 접속 이력 관리

## 🛠 기술 스택

- **언어**: Java 21
- **프레임워크**: Spring Boot 3.x
- **빌드 도구**: Gradle
- **데이터베이스**: MySQL
- **ORM**: MyBatis
- **인증**: JWT (JSON Web Token)
- **보안**: Spring Security
- **프론트엔드**: HTML, CSS, JavaScript

## 📋 요구사항

- Java 21 이상
- MySQL 8.0 이상
- Gradle 7.0 이상

## 🚀 실행 방법

### 1. 데이터베이스 설정

```sql
-- MySQL에서 데이터베이스 생성
CREATE DATABASE login_auth;

-- 스키마 및 테이블 생성
source database_schema.sql;
```

### 2. 애플리케이션 설정

`src/main/resources/application.properties` 파일을 수정하세요:

```properties
# 데이터베이스 설정
spring.datasource.url=jdbc:mysql://localhost:3306/login_auth?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis 설정
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.type-aliases-package=com.nsustest.loginAuth

# 서버 포트
server.port=8080
```

### 3. 애플리케이션 실행

```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

### 4. 웹 브라우저에서 접속

- **메인 페이지**: http://localhost:8080
- **로그인 페이지**: http://localhost:8080/login.html
- **회원가입 페이지**: http://localhost:8080/signup.html
- **대시보드**: http://localhost:8080/dashboard.html

## 📚 API 명세

자세한 API 명세는 [API_SPECIFICATION.md](./API_SPECIFICATION.md)를 참조하세요.

### 주요 API 엔드포인트

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api/check-id` | 아이디 중복 확인 |
| POST | `/api/signup` | 회원가입 |
| POST | `/api/login` | 로그인 |
| POST | `/api/refresh` | 토큰 재발급 |
| POST | `/api/logout` | 로그아웃 |
| GET | `/api/user` | 사용자 정보 조회 |

## 🔐 보안 기능

### JWT 토큰 관리
- **Access Token**: 15분 만료 (API 요청용)
- **Refresh Token**: 7일 만료 (토큰 재발급용)
- **자동 재발급**: Access Token 만료 시 자동으로 Refresh Token으로 재발급

### 비밀번호 보안
- **BCrypt 해시**: 비밀번호를 안전하게 암호화
- **복잡도 검증**: 영문+숫자+특수문자 조합 필수

### 세션 관리
- **활성 연결 추적**: 사용자별 활성 세션 관리
- **접속 이력**: 로그인/로그아웃 이력 저장
- **다중 로그인 방지**: 기존 세션 무효화 후 새 세션 생성

## 🗄 데이터베이스 스키마

### 주요 테이블
- `users`: 사용자 기본 정보
- `user_login_info`: 사용자 로그인 관련 정보
- `refresh_tokens`: Refresh Token 관리
- `active_connections`: 활성 연결 정보
- `conn_hist`: 접속 이력

자세한 스키마는 [database_schema.sql](./database_schema.sql)를 참조하세요.

## 🧪 테스트

### 샘플 사용자 계정
- **아이디**: testuser
- **비밀번호**: password123!
- **이메일**: test@example.com

### 테스트 시나리오
1. 회원가입 → 로그인 → 대시보드 접근
2. 토큰 만료 시 자동 재발급 확인
3. 로그아웃 시 토큰 무효화 확인

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/nsustest/loginAuth/
│   │       ├── config/          # Spring Security 설정
│   │       ├── controller/      # REST API 컨트롤러
│   │       ├── dao/            # 데이터베이스 접근 계층
│   │       ├── service/        # 비즈니스 로직
│   │       └── util/           # 유틸리티 클래스
│   └── resources/
│       ├── mapper/             # MyBatis 매퍼 XML
│       └── static/             # 정적 리소스 (HTML, CSS, JS)
└── test/                       # 테스트 코드
```

## 🔧 개발 환경 설정

### IDE 설정
- **IntelliJ IDEA** 또는 **Eclipse** 권장
- **Lombok** 플러그인 설치 (필요시)
- **MySQL Workbench** 또는 **DBeaver** (데이터베이스 관리)

### 코드 스타일
- Java 기본 스타일 준수
- 중괄호는 한 줄 아래에 작성
- 들여쓰기는 공백 4칸
- 모든 클래스와 메서드에 주석 포함

## 🚨 주의사항

1. **보안**: 실제 운영 환경에서는 JWT 시크릿 키를 환경변수로 관리하세요
2. **HTTPS**: 프로덕션 환경에서는 반드시 HTTPS를 사용하세요
3. **토큰 저장**: XSS 공격에 대비하여 토큰 저장 방식을 검토하세요
4. **데이터베이스**: 정기적으로 만료된 토큰을 정리하세요

## 📞 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해 주세요.

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.
