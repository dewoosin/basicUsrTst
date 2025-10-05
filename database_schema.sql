-- 최적화된 로그인 인증 시스템 데이터베이스 스키마
-- MySQL 8.0 이상 버전 기준
-- 테이블 수를 줄이고 중복을 제거하여 더 효율적인 구조로 개선

-- 1. 사용자 테이블 (기본 정보)
CREATE TABLE users (
    usr_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usr_login_id VARCHAR(50) NOT NULL UNIQUE,
    usr_nm VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    pwd VARCHAR(255) NOT NULL,
    usr_tp_cd VARCHAR(2) DEFAULT '02',
    phone_num VARCHAR(20),
    is_use BOOLEAN DEFAULT TRUE,
    cre_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    upd_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 인덱스
    INDEX idx_usr_login_id (usr_login_id),
    INDEX idx_email (email),
    INDEX idx_is_use (is_use)
);

-- 2. 사용자 세션 테이블 (통합된 세션 관리)
-- 기존 user_login_info, active_connections, refresh_tokens를 통합
CREATE TABLE user_sessions (
    session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usr_id BIGINT NOT NULL,
    conn_tp_cd VARCHAR(2) DEFAULT '02', -- 연결 타입 (01: REFRESH_TOKEN, 02: SESSION)
    access_token TEXT,
    refresh_token TEXT,
    ip_addr VARCHAR(45),
    user_agent TEXT,
    login_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_dt TIMESTAMP NULL,
    last_act_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    exp_dt TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    session_dur_sec INT DEFAULT 0,
    cre_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    upd_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. 로그인 이력 테이블 (성공/실패 통합)
-- 기존 conn_hist와 login_attempts를 통합
CREATE TABLE login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usr_id BIGINT NULL, -- 실패 시에는 NULL
    usr_login_id VARCHAR(50), -- 실패 시에도 기록
    ip_addr VARCHAR(45) NOT NULL,
    login_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_success BOOLEAN NOT NULL,
    fail_reason VARCHAR(255) NULL,
    user_agent TEXT,
    attempt_cnt INT DEFAULT 1, -- IP별 시도 횟수
    is_blocked BOOLEAN DEFAULT FALSE,
    blocked_until_dt TIMESTAMP NULL,
    cre_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    upd_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    

    
    -- 인덱스
    INDEX idx_usr_id (usr_id),
    INDEX idx_usr_login_id (usr_login_id),
    INDEX idx_ip_addr (ip_addr),
    INDEX idx_login_dt (login_dt),
    INDEX idx_is_success (is_success),
    INDEX idx_is_blocked (is_blocked),
    INDEX idx_blocked_until (blocked_until_dt)
);

-- 4. 사용자 통계 테이블 (집계 정보)
-- 자주 조회되는 통계 정보를 별도 관리
CREATE TABLE user_stats (
    usr_id BIGINT PRIMARY KEY,
    total_login_cnt INT DEFAULT 0,
    last_login_dt TIMESTAMP NULL,
    last_login_ip VARCHAR(45),
    failed_login_cnt INT DEFAULT 0,
    is_locked BOOLEAN DEFAULT FALSE,
    locked_until_dt TIMESTAMP NULL,
    cre_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    upd_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 기존 테이블 삭제를 위한 DROP 문 (필요시 주석 해제)
-- DROP TABLE IF EXISTS user_login_info;
-- DROP TABLE IF EXISTS conn_hist;
-- DROP TABLE IF EXISTS active_connections;
-- DROP TABLE IF EXISTS refresh_tokens;
-- DROP TABLE IF EXISTS login_attempts;

-- 샘플 데이터 (테스트용)
INSERT INTO users (usr_login_id, usr_nm, email, pwd, usr_tp_cd, phone_num) VALUES
('testuser', '테스트사용자', 'test@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '02', '010-1234-5678');

-- 5. Rate-Limiting 이력 테이블
CREATE TABLE rate_limit_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_addr VARCHAR(45) NOT NULL,
    limit_type VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    block_dt TIMESTAMP NOT NULL,
    cre_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    upd_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 인덱스
    INDEX idx_ip_addr (ip_addr),
    INDEX idx_limit_type (limit_type),
    INDEX idx_block_dt (block_dt)
);

-- 6. 공통코드 그룹 테이블 (Redis 캐시 대상)
CREATE TABLE cm_cd_grp (
    grp_cd VARCHAR(20) NOT NULL PRIMARY KEY,
    grp_nm VARCHAR(100) NOT NULL,
    grp_desc VARCHAR(255) NULL,
    is_use BOOLEAN DEFAULT TRUE,
    upd_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    upd_id BIGINT NULL,
    cre_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cre_id BIGINT NULL
);

-- 7. 공통코드 테이블 (Redis 캐시 대상)
CREATE TABLE cm_cd (
    grp_cd VARCHAR(20) NOT NULL,
    cd VARCHAR(20) NOT NULL,
    cd_nm VARCHAR(100) NOT NULL,
    cd_eng_nm VARCHAR(100) NOT NULL,
    cd_desc VARCHAR(255) NULL,
    sort_ord INT DEFAULT 0,
    is_use BOOLEAN DEFAULT TRUE,
    upd_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    upd_id BIGINT NULL,
    cre_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cre_id BIGINT NULL,
    PRIMARY KEY (grp_cd, cd)
);

-- 8. 메시지 코드 테이블 (Redis 캐시 대상)
CREATE TABLE msg_cd (
    msg_cd VARCHAR(20) NOT NULL PRIMARY KEY,
    msg_tp_cd VARCHAR(2) NOT NULL,
    msg_nm VARCHAR(100) NOT NULL,
    msg_cont VARCHAR(500) NOT NULL,
    msg_desc VARCHAR(255) NULL,
    is_use BOOLEAN DEFAULT TRUE,
    upd_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    upd_id BIGINT NULL,
    cre_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cre_id BIGINT NULL
);

-- 공통코드 그룹 초기 데이터
INSERT INTO cm_cd_grp (grp_cd, grp_nm, grp_desc) VALUES
('USR_TP', '사용자 타입', '사용자의 권한 구분'),
('CONN_TP', '연결 타입', '세션/토큰 연결 타입 구분'),
('MSG_TP', '메시지 타입', '메시지 분류 타입');

-- 공통코드 초기 데이터
INSERT INTO cm_cd (grp_cd, cd, cd_nm, cd_eng_nm, cd_desc, sort_ord) VALUES
('USR_TP', '01', '관리자', 'ADMIN', '시스템 관리자', 1),
('USR_TP', '02', '일반사용자', 'USER', '일반 사용자', 2),
('CONN_TP', '01', '리프레시 토큰', 'REFRESH_TOKEN', '토큰 갱신용', 1),
('CONN_TP', '02', '세션', 'SESSION', '일반 세션', 2),
('MSG_TP', '01', '성공', 'SUCCESS', '성공 메시지', 1),
('MSG_TP', '02', '정보', 'INFO', '정보 메시지', 2),
('MSG_TP', '03', '경고', 'WARN', '경고 메시지', 3),
('MSG_TP', '04', '오류', 'ERROR', '오류 메시지', 4);

-- 메시지 코드 초기 데이터
INSERT INTO msg_cd (msg_cd, msg_tp_cd, msg_nm, msg_cont, msg_desc) VALUES
-- 회원가입 관련
('SIGNUP_001', '01', '회원가입 성공', '회원가입이 완료되었습니다.', '회원가입 성공 메시지'),
('SIGNUP_002', '04', '이메일 중복', '이미 사용 중인 이메일입니다.', '이메일 중복 오류'),
('SIGNUP_003', '04', '아이디 중복', '이미 사용 중인 아이디입니다.', '아이디 중복 오류'),
('SIGNUP_004', '04', '비밀번호 정책 오류', '비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.', '비밀번호 정책 위반'),

-- 로그인 관련
('LOGIN_001', '01', '로그인 성공', '로그인되었습니다.', '로그인 성공 메시지'),
('LOGIN_002', '04', '로그인 실패', '아이디 또는 비밀번호가 올바르지 않습니다.', '로그인 실패'),
('LOGIN_003', '04', '계정 잠금', '로그인 실패 횟수가 초과되어 계정이 잠겼습니다.', '계정 잠금 상태'),
('LOGIN_004', '04', '계정 비활성', '비활성화된 계정입니다.', '계정 비활성화 상태'),

-- 토큰 관련
('TOKEN_001', '01', '토큰 재발급 성공', '토큰이 재발급되었습니다.', '토큰 재발급 성공'),
('TOKEN_002', '04', '토큰 만료', '토큰이 만료되었습니다.', '토큰 만료 오류'),
('TOKEN_003', '04', '토큰 무효', '유효하지 않은 토큰입니다.', '토큰 무효 오류'),
('TOKEN_004', '04', '리프레시 토큰 만료', '리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.', '리프레시 토큰 만료'),

-- 로그아웃 관련
('LOGOUT_001', '01', '로그아웃 완료', '로그아웃되었습니다.', '로그아웃 성공 메시지'),

-- 비밀번호 관련
('PWD_001', '01', '비밀번호 변경 성공', '비밀번호가 변경되었습니다.', '비밀번호 변경 성공'),
('PWD_002', '04', '현재 비밀번호 오류', '현재 비밀번호가 올바르지 않습니다.', '현재 비밀번호 오류'),
('PWD_003', '04', '이전 비밀번호 재사용', '이전에 사용했던 비밀번호는 사용할 수 없습니다.', '비밀번호 재사용 금지'),

-- 공통 메시지
('COMMON_001', '02', '처리 중', '요청을 처리하고 있습니다.', '처리 중 메시지'),
('COMMON_002', '04', '서버 오류', '서버에 일시적인 오류가 발생했습니다.', '서버 오류'),
('COMMON_003', '04', '권한 없음', '해당 기능에 대한 권한이 없습니다.', '권한 부족'),
('COMMON_004', '03', '세션 만료 임박', '세션이 곧 만료됩니다. 연장하시겠습니까?', '세션 만료 알림'),

-- ErrorCode.java에서 분석된 추가 메시지들
-- 인증 관련 추가 메시지
('AUTH_001', '04', '인증 실패', '아이디 또는 비밀번호가 올바르지 않습니다.', '로그인 인증 실패'),
('AUTH_002', '04', '계정 잠금', '로그인 실패 횟수가 초과되어 계정이 잠겼습니다.', '계정 잠금 상태'),
('AUTH_003', '04', '계정 비활성', '비활성화된 계정입니다.', '계정 비활성화 상태'),
('AUTH_004', '04', '토큰 만료', '토큰이 만료되었습니다.', '토큰 만료 오류'),
('AUTH_005', '04', '토큰 무효', '유효하지 않은 토큰입니다.', '토큰 무효 오류'),
('AUTH_006', '04', '리프레시 토큰 만료', '리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.', '리프레시 토큰 만료'),
('AUTH_007', '04', '리프레시 토큰 무효', '유효하지 않은 리프레시 토큰입니다.', '리프레시 토큰 무효'),
('AUTH_008', '04', '인증 필요', '인증이 필요합니다.', '인증 필요 오류'),

-- 사용자 관련 추가 메시지
('USER_001', '04', '사용자 없음', '사용자 정보를 찾을 수 없습니다.', '사용자 정보 없음'),
('USER_002', '04', '사용자 중복', '이미 존재하는 사용자입니다.', '사용자 중복 오류'),
('USER_003', '04', '아이디 중복', '이미 사용 중인 아이디입니다.', '아이디 중복 오류'),
('USER_004', '04', '이메일 중복', '이미 사용 중인 이메일입니다.', '이메일 중복 오류'),
('USER_005', '04', '사용자 형식 오류', '사용자 정보 형식이 올바르지 않습니다.', '사용자 정보 형식 오류'),

-- 비밀번호 관련 추가 메시지
('PWD_004', '04', '비밀번호 형식 오류', '비밀번호 형식이 올바르지 않습니다.', '비밀번호 형식 오류'),
('PWD_005', '04', '비밀번호 약함', '비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.', '비밀번호 강도 부족'),
('PWD_006', '04', '비밀번호 불일치', '비밀번호가 일치하지 않습니다.', '비밀번호 불일치'),
('PWD_007', '04', '비밀번호 재사용 금지', '이전에 사용했던 비밀번호는 사용할 수 없습니다.', '비밀번호 재사용 금지'),

-- 유효성 검사 관련 메시지
('VAL_001', '04', '필수 입력 누락', '필수 입력 항목이 누락되었습니다.', '필수 입력 항목 누락'),
('VAL_002', '04', '입력 형식 오류', '입력 형식이 올바르지 않습니다.', '입력 형식 오류'),
('VAL_003', '04', '입력 길이 초과', '입력 길이가 허용 범위를 초과했습니다.', '입력 길이 초과'),

-- 서버 관련 메시지
('SRV_001', '04', '서버 내부 오류', '서버에 일시적인 오류가 발생했습니다.', '서버 내부 오류'),
('SRV_002', '04', '데이터베이스 오류', '데이터베이스 처리 중 오류가 발생했습니다.', '데이터베이스 오류'),
('SRV_003', '04', '외부 서비스 오류', '외부 서비스 연동 중 오류가 발생했습니다.', '외부 서비스 오류'),

-- 보안 관련 메시지
('SEC_001', '04', '요청 한도 초과', '요청 횟수가 초과되었습니다. 잠시 후 다시 시도해주세요.', 'Rate Limiting 초과'),
('SEC_002', '04', '의심스러운 활동', '의심스러운 활동이 감지되었습니다.', '보안 위협 감지'),
('SEC_003', '04', 'IP 차단', '차단된 IP 주소입니다.', 'IP 주소 차단'),

-- 세션 관련 메시지
('SES_001', '04', '세션 만료', '세션이 만료되었습니다.', '세션 만료'),
('SES_002', '04', '세션 무효', '유효하지 않은 세션입니다.', '세션 무효'),
('SES_003', '04', '동시 로그인', '다른 기기에서 로그인되어 현재 세션이 종료됩니다.', '동시 로그인 감지'),

-- 서비스에서 사용되는 추가 메시지들
('SERVICE_001', '01', '사용 가능한 아이디', '사용 가능한 아이디입니다.', '아이디 중복 확인 성공'),
('SERVICE_002', '01', '회원가입 완료', '회원가입이 완료되었습니다!', '회원가입 성공'),
('SERVICE_003', '04', '회원가입 실패', '회원가입에 실패했습니다.', '회원가입 실패'),
('SERVICE_004', '04', '로그인 시도 제한', '로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.', '로그인 시도 제한'),
('SERVICE_005', '01', '로그인 성공', '로그인되었습니다.', '로그인 성공'),
('SERVICE_006', '04', '아이디 형식 오류', '아이디는 영문, 숫자 조합 4-20자로 입력해주세요.', '아이디 형식 오류'),
('SERVICE_007', '01', '유효한 아이디', '유효한 아이디입니다.', '아이디 유효성 검사 성공'),
('SERVICE_008', '04', '이메일 형식 오류', '올바른 이메일 형식이 아닙니다.', '이메일 형식 오류'),
('SERVICE_009', '01', '유효한 회원가입 데이터', '유효한 회원가입 데이터입니다.', '회원가입 데이터 유효성 검사 성공'),
('SERVICE_010', '01', '유효한 로그인 데이터', '유효한 로그인 데이터입니다.', '로그인 데이터 유효성 검사 성공'),
('SERVICE_011', '01', '중복 확인 완료', '중복 확인 완료', '중복 확인 성공'),
('SERVICE_012', '01', '인증 완료', '인증이 완료되었습니다.', '인증 성공'),
('SERVICE_013', '04', '리프레시 토큰 무효', '유효하지 않은 Refresh Token입니다.', '리프레시 토큰 무효'),
('SERVICE_014', '04', '사용자 정보 없음', '사용자 정보를 찾을 수 없습니다.', '사용자 정보 조회 실패'),
('SERVICE_015', '01', '토큰 재발급 성공', '토큰이 재발급되었습니다.', '토큰 재발급 성공'),
('SERVICE_016', '04', '서버 오류', '서버 오류가 발생했습니다.', '서버 오류'),
('SERVICE_017', '01', '로그아웃 완료', '로그아웃되었습니다.', '로그아웃 성공'),
('SERVICE_018', '01', '사용자 정보 조회 성공', '사용자 정보를 조회했습니다.', '사용자 정보 조회 성공'),

-- Rate Limiting 관련 메시지
('RATE_001', '01', 'Rate-Limiting 캐시 초기화', 'IP {ip}의 Rate-Limiting 캐시가 초기화되었습니다.', 'Rate-Limiting 캐시 초기화'),
('RATE_002', '01', '전체 Rate-Limiting 캐시 초기화', '전체 Rate-Limiting 캐시가 초기화되었습니다.', '전체 Rate-Limiting 캐시 초기화'),
('RATE_003', '04', '분당 요청 한도 초과', '분당 요청 한도 초과', '분당 요청 한도 초과'),
('RATE_004', '04', '시간당 요청 한도 초과', '시간당 요청 한도 초과', '시간당 요청 한도 초과'),
('RATE_005', '04', '일당 요청 한도 초과', '일당 요청 한도 초과', '일당 요청 한도 초과'),
('RATE_006', '04', '분당 로그인 시도 한도 초과', '분당 로그인 시도 한도 초과', '분당 로그인 시도 한도 초과'),
('RATE_007', '04', '시간당 로그인 시도 한도 초과', '시간당 로그인 시도 한도 초과', '시간당 로그인 시도 한도 초과'),

-- 스케줄러 관련 메시지
('SCHED_001', '02', '만료된 세션 정리 완료', '만료된 세션 정리 완료 - 삭제된 세션 수: {count}', '세션 정리 완료'),
('SCHED_002', '04', '세션 정리 오류', '만료된 세션 정리 중 오류 발생: {error}', '세션 정리 오류'),
('SCHED_003', '02', '로그인 이력 정리 완료', '오래된 로그인 이력 정리 완료 - 삭제된 이력 수: {count}', '로그인 이력 정리 완료'),
('SCHED_004', '04', '로그인 이력 정리 오류', '오래된 로그인 이력 정리 중 오류 발생: {error}', '로그인 이력 정리 오류'),
('SCHED_005', '02', 'Rate-Limit 이력 정리 완료', '오래된 Rate-Limit 이력 정리 완료 - 삭제된 이력 수: {count}', 'Rate-Limit 이력 정리 완료'),
('SCHED_006', '04', 'Rate-Limit 이력 정리 오류', '오래된 Rate-Limit 이력 정리 중 오류 발생: {error}', 'Rate-Limit 이력 정리 오류'),

-- 프론트엔드 JavaScript 메시지들
('FRONT_001', '01', '로그인 성공', '로그인되었습니다!', '프론트엔드 로그인 성공'),
('FRONT_002', '04', '로그인 실패', '로그인에 실패했습니다.', '프론트엔드 로그인 실패'),
('FRONT_003', '04', '서버 통신 실패', '서버와의 통신에 실패했습니다.', '프론트엔드 서버 통신 실패'),
('FRONT_004', '04', '아이디 입력 필요', '아이디를 입력해주세요.', '아이디 입력 필요'),
('FRONT_005', '04', '아이디 형식 오류', '아이디는 영문, 숫자 조합 4-20자로 입력해주세요.', '아이디 형식 오류'),
('FRONT_006', '04', '아이디 중복', '이미 사용 중인 아이디입니다.', '아이디 중복'),
('FRONT_007', '01', '아이디 사용 가능', '사용 가능한 아이디입니다.', '아이디 사용 가능'),
('FRONT_008', '04', '중복 확인 오류', '중복 확인 중 오류가 발생했습니다.', '중복 확인 오류'),
('FRONT_009', '04', '서버 통신 오류', '서버와의 통신 중 오류가 발생했습니다.', '서버 통신 오류'),
('FRONT_010', '04', '비밀번호 불일치', '비밀번호가 일치하지 않습니다.', '비밀번호 불일치'),
('FRONT_011', '04', '아이디 형식 오류', '아이디는 영문, 숫자 조합 4-20자여야 합니다.', '아이디 형식 오류'),
('FRONT_012', '04', '아이디 중복확인 필요', '아이디 중복확인을 해주세요.', '아이디 중복확인 필요'),
('FRONT_013', '04', '이메일 형식 오류', '올바른 이메일 형식이 아닙니다.', '이메일 형식 오류'),
('FRONT_014', '04', '비밀번호 강도 부족', '비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.', '비밀번호 강도 부족'),
('FRONT_015', '04', '이용약관 동의 필요', '이용약관에 동의해주세요.', '이용약관 동의 필요'),
('FRONT_016', '01', '회원가입 완료', '회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.', '회원가입 완료'),
('FRONT_017', '04', '회원가입 실패', '회원가입에 실패했습니다.', '회원가입 실패'),
('FRONT_018', '02', '비밀번호 강도 약함', '비밀번호 강도: 약함', '비밀번호 강도 약함'),
('FRONT_019', '02', '비밀번호 강도 보통', '비밀번호 강도: 보통', '비밀번호 강도 보통'),
('FRONT_020', '02', '비밀번호 강도 강함', '비밀번호 강도: 강함', '비밀번호 강도 강함'),

-- 알 수 없는 오류
('UNKNOWN_001', '04', '알 수 없는 오류', '알 수 없는 오류가 발생했습니다.', '알 수 없는 오류');

-- 사용자 통계 초기화
INSERT INTO user_stats (usr_id, total_login_cnt, failed_login_cnt, is_locked) VALUES
(1, 0, 0, FALSE);

