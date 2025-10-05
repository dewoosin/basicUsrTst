# 로그인 인증 시스템 API 명세서

## 개요
JWT 기반의 로그인 인증 시스템으로, Access Token과 Refresh Token을 사용하여 보안을 강화한 REST API입니다.

## 기본 정보
- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **인증 방식**: Bearer Token (JWT)

## API 엔드포인트

### 1. 아이디 중복 확인
- **URL**: `GET /api/check-id`
- **설명**: 회원가입 시 아이디 중복 여부를 확인합니다.
- **파라미터**:
  - `usrLoginId` (query): 확인할 로그인 아이디
- **응답 예시**:
```json
{
  "success": true,
  "message": "사용 가능한 아이디입니다.",
  "duplicate": false
}
```

### 2. 회원가입
- **URL**: `POST /api/signup`
- **설명**: 새로운 사용자를 등록합니다.
- **요청 본문**:
```json
{
  "usrLoginId": "testuser",
  "usrNm": "테스트사용자",
  "email": "test@example.com",
  "password": "password123!",
  "phoneNum": "010-1234-5678"
}
```
- **응답 예시**:
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다!"
}
```

### 3. 로그인
- **URL**: `POST /api/login`
- **설명**: 사용자 로그인을 처리하고 JWT 토큰을 발급합니다.
- **요청 본문**:
```json
{
  "usrLoginId": "testuser",
  "password": "password123!"
}
```
- **응답 예시**:
```json
{
  "success": true,
  "message": "로그인되었습니다.",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "usrId": 1,
    "usrLoginId": "testuser",
    "usrNm": "테스트사용자",
    "email": "test@example.com"
  }
}
```

### 4. 토큰 재발급
- **URL**: `POST /api/refresh`
- **설명**: Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급합니다.
- **요청 본문**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
- **응답 예시**:
```json
{
  "success": true,
  "message": "토큰이 재발급되었습니다.",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### 5. 로그아웃
- **URL**: `POST /api/logout`
- **설명**: 사용자 로그아웃을 처리하고 Refresh Token을 무효화합니다.
- **헤더**:
  - `Authorization`: `Bearer {accessToken}`
- **응답 예시**:
```json
{
  "success": true,
  "message": "로그아웃되었습니다."
}
```

### 6. 사용자 정보 조회
- **URL**: `GET /api/user`
- **설명**: JWT 토큰을 검증하여 사용자 정보를 조회합니다.
- **헤더**:
  - `Authorization`: `Bearer {accessToken}`
- **응답 예시**:
```json
{
  "success": true,
  "message": "사용자 정보를 조회했습니다.",
  "user": {
    "usrId": 1,
    "usrLoginId": "testuser",
    "usrNm": "테스트사용자",
    "email": "test@example.com",
    "usrTpCd": "02",
    "phoneNum": "010-1234-5678",
    "isUse": true,
    "creDt": "2024-01-01T00:00:00",
    "updDt": "2024-01-01T00:00:00"
  }
}
```

## 토큰 정보

### Access Token
- **만료 시간**: 15분
- **용도**: API 요청 시 인증
- **저장 위치**: 클라이언트 (localStorage)

### Refresh Token
- **만료 시간**: 7일
- **용도**: Access Token 재발급
- **저장 위치**: 클라이언트 (localStorage) + 서버 (데이터베이스)

## 에러 응답

### 공통 에러 형식
```json
{
  "success": false,
  "message": "에러 메시지"
}
```

### HTTP 상태 코드
- `200`: 성공
- `400`: 잘못된 요청 (Bad Request)
- `401`: 인증 실패 (Unauthorized)
- `500`: 서버 오류 (Internal Server Error)

## 보안 고려사항

1. **토큰 만료**: Access Token은 15분, Refresh Token은 7일로 설정
2. **토큰 무효화**: 로그아웃 시 Refresh Token을 서버에서 무효화
3. **자동 재발급**: Access Token 만료 시 자동으로 Refresh Token으로 재발급
4. **HTTPS 사용**: 실제 운영 환경에서는 HTTPS 사용 권장
5. **토큰 저장**: 클라이언트에서는 localStorage에 저장 (XSS 공격에 주의)

## 사용 예시

### JavaScript에서 API 호출
```javascript
// 로그인
const loginResponse = await fetch('/api/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    usrLoginId: 'testuser',
    password: 'password123!'
  })
});

// 인증이 필요한 API 호출
const userResponse = await fetch('/api/user', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

### 토큰 자동 재발급
```javascript
// 토큰 만료 시 자동 재발급
async function authenticatedFetch(url, options = {}) {
  let response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${accessToken}`
    }
  });
  
  if (response.status === 401) {
    // 토큰 재발급
    const newToken = await refreshAccessToken();
    if (newToken) {
      response = await fetch(url, {
        ...options,
        headers: {
          ...options.headers,
          'Authorization': `Bearer ${newToken}`
        }
      });
    }
  }
  
  return response;
}
```
