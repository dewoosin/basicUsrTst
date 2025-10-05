package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.constants.ErrorCode;
import com.nsustest.loginAuth.dao.LoginDao;
import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 로그인 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * @author nsustest
 */
@Service
public class LoginService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
    
    @Autowired
    private LoginDao loginDao;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private CommonCodeService commonCodeService;
    
    /**
     * 아이디 중복 확인
     * 
     * @param usrLoginId 확인할 로그인 아이디
     * @return 중복 확인 결과 ApiResponse
     */
    public ApiResponse<Map<String, Object>> checkIdDuplicate(String usrLoginId) {
        try {
            // 아이디 유효성 검사
            ApiResponse<Map<String, Object>> validationResult = validateLoginId(usrLoginId);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            // 중복 확인 (0보다 크면 중복)
            int count = loginDao.checkIdDuplicate(usrLoginId);
            boolean isDuplicate = count > 0;
            
            Map<String, Object> data = new HashMap<>();
            data.put("duplicate", isDuplicate);
            data.put("usrLoginId", usrLoginId);
            
            if (isDuplicate) {
                return ApiResponse.error(getMessage("USER_003"), ErrorCode.USER_ID_DUPLICATE);
            } else {
                return ApiResponse.success(getMessage("SERVICE_001"), data);
            }
            
        } catch (Exception e) {
            logSecurityEvent("ID_CHECK_ERROR", null, usrLoginId, e);
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.SERVER_INTERNAL_ERROR), ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }
    
    /**
     * 회원가입 처리
     * 
     * @param signupData 회원가입 데이터
     * @return 회원가입 결과 ApiResponse
     */
    public ApiResponse<Map<String, Object>> signup(Map<String, Object> signupData) {
        String usrLoginId = null;
        try {
            logger.info("=== 회원가입 시작 ===");
            logger.debug("받은 데이터: {}", signupData);
            
            // 필수 필드 검증
            ApiResponse<Map<String, Object>> validationResult = validateSignupData(signupData);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            usrLoginId = (String) signupData.get("usrLoginId");
            String email = (String) signupData.get("email");
            String password = (String) signupData.get("password");
            
            // 중복 확인
            ApiResponse<Map<String, Object>> duplicateCheck = checkDuplicates(usrLoginId, email);
            if (!duplicateCheck.isSuccess()) {
                return duplicateCheck;
            }
            
            // 비밀번호 암호화 및 저장
            String encodedPassword = passwordEncoder.encode(password);
            Map<String, Object> userData = createUserData(signupData, encodedPassword);
            
            int result = loginDao.insertUser(userData);
            
            if (result > 0) {
                // 생성된 사용자 ID 가져오기 (MyBatis useGeneratedKeys로 자동 설정됨)
                Long usrId = ((Number) userData.get("usrId")).longValue();
                logger.info("생성된 사용자 ID: {}", usrId);
                
                // 신규 사용자 통계 정보 초기화
                Map<String, Object> userStats = new HashMap<>();
                userStats.put("usrId", usrId);
                
                loginDao.insertUserStats(userStats);
                logger.info("사용자 통계 정보 초기화 완료");
                
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("usrLoginId", usrLoginId);
                responseData.put("email", email);
                
                return ApiResponse.success(getMessage("SERVICE_002"), responseData);
            } else {
                return ApiResponse.error(getMessage("SERVICE_003"), ErrorCode.SERVER_INTERNAL_ERROR);
            }
            
        } catch (Exception e) {
            logger.error("회원가입 중 예외 발생: {}", e.getMessage(), e);
            logSecurityEvent("SIGNUP_ERROR", null, usrLoginId, e);
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.SERVER_INTERNAL_ERROR), ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }
    
    /**
     * 로그인 처리
     * 
     * @param loginData 로그인 데이터
     * @return 로그인 결과 ApiResponse
     */
    public ApiResponse<Map<String, Object>> login(Map<String, Object> loginData) {
        try {
            logger.info("=== 로그인 시작 ===");
            logger.debug("받은 데이터: {}", loginData);
            
            // 필수 필드 검증
            ApiResponse<Map<String, Object>> validationResult = validateLoginData(loginData);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            String usrLoginId = (String) loginData.get("usrLoginId");
            String password = (String) loginData.get("password");
            String ipAddr = (String) loginData.get("ipAddr"); // 클라이언트 IP (추후 추가)
            
            // IP 차단 상태 확인
            if (ipAddr != null) {
                Map<String, Object> blockedInfo = loginDao.checkIpBlocked(ipAddr);
                if (blockedInfo != null) {
                    return ApiResponse.error(getMessage("SERVICE_004"), ErrorCode.AUTH_ACCOUNT_DISABLED);
                }
            }
            
            // 사용자 정보 조회 및 검증
            Map<String, Object> user = loginDao.findByLoginId(usrLoginId);
            if (user == null) {
                // 로그인 실패 시도 기록
                if (ipAddr != null) {
                    recordLoginFailure(usrLoginId, ipAddr, "사용자 없음");
                }
                return ApiResponse.error(getMessage("AUTH_001"), ErrorCode.AUTH_INVALID_CREDENTIALS);
            }
            
            // 계정 상태 및 비밀번호 검증
            ApiResponse<Map<String, Object>> authResult = authenticateUser(user, password);
            if (!authResult.isSuccess()) {
                // 로그인 실패 시도 기록
                if (ipAddr != null) {
                    recordLoginFailure(usrLoginId, ipAddr, "비밀번호 오류");
                }
                return authResult;
            }
            
            logger.info("로그인 성공 - 사용자: {}", user.get("usr_nm"));
            
            // 로그인 성공 시 IP 차단 해제
            if (ipAddr != null) {
                loginDao.resetIpBlock(ipAddr);
            }
            
            // 로그인 성공 시 처리
            Long usrId = ((Number) user.get("usr_id")).longValue();
            saveLoginSuccess(usrId, usrLoginId, ipAddr, loginData);
            
            // JWT 토큰 생성 (Access Token + Refresh Token)
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);
            
            // 세션 저장 (토큰 포함)
            saveUserSession(usrId, accessToken, refreshToken, ipAddr, loginData);
            
            // 로그인 성공 응답 데이터 생성
            Map<String, Object> authData = new HashMap<>();
            authData.put("accessToken", accessToken);
            authData.put("refreshToken", refreshToken);
            authData.put("tokenType", "Bearer");
            authData.put("expiresIn", jwtUtil.getAccessTokenExpirationInSeconds()); // 15분 (초 단위)
            authData.put("user", Map.of(
                "usrId", user.get("usr_id"),
                "usrLoginId", user.get("usr_login_id"),
                "usrNm", user.get("usr_nm"),
                "email", user.get("email")
            ));
            
            return ApiResponse.success(getMessage("SERVICE_005"), authData);
            
        } catch (Exception e) {
            logger.error("로그인 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.SERVER_INTERNAL_ERROR), ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }
    
    // ==================== 검증 로직 함수들 ====================
    
    /**
     * 로그인 아이디 유효성 검사
     */
    private ApiResponse<Map<String, Object>> validateLoginId(String usrLoginId) {
        if (usrLoginId == null || usrLoginId.trim().isEmpty()) {
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.VALIDATION_REQUIRED_FIELD), ErrorCode.VALIDATION_REQUIRED_FIELD);
        }
        
        if (!usrLoginId.matches("^[a-zA-Z0-9]{4,20}$")) {
            return ApiResponse.error(getMessage("SERVICE_006"), ErrorCode.VALIDATION_INVALID_FORMAT);
        }
        
        return ApiResponse.success(getMessage("SERVICE_007"), null);
    }
    
    /**
     * 회원가입 데이터 유효성 검사
     */
    private ApiResponse<Map<String, Object>> validateSignupData(Map<String, Object> signupData) {
        String usrLoginId = (String) signupData.get("usrLoginId");
        String usrNm = (String) signupData.get("usrNm");
        String email = (String) signupData.get("email");
        String password = (String) signupData.get("password");
        
        // 아이디 검증
        ApiResponse<Map<String, Object>> idValidation = validateLoginId(usrLoginId);
        if (!idValidation.isSuccess()) {
            return idValidation;
        }
        
        // 이름 검증
        if (usrNm == null || usrNm.trim().isEmpty()) {
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.VALIDATION_REQUIRED_FIELD), ErrorCode.VALIDATION_REQUIRED_FIELD);
        }
        
        // 이메일 검증
        if (email == null || email.trim().isEmpty()) {
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.VALIDATION_REQUIRED_FIELD), ErrorCode.VALIDATION_REQUIRED_FIELD);
        }
        
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ApiResponse.error(getMessage("SERVICE_008"), ErrorCode.VALIDATION_INVALID_FORMAT);
        }
        
        // 비밀번호 검증
        if (password == null || password.trim().isEmpty()) {
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.VALIDATION_REQUIRED_FIELD), ErrorCode.VALIDATION_REQUIRED_FIELD);
        }
        
        if (!password.matches("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$")) {
            return ApiResponse.error(getMessage("PWD_005"), ErrorCode.PASSWORD_TOO_WEAK);
        }
        
        return ApiResponse.success(getMessage("SERVICE_009"), null);
    }
    
    /**
     * 로그인 데이터 유효성 검사
     */
    private ApiResponse<Map<String, Object>> validateLoginData(Map<String, Object> loginData) {
        String usrLoginId = (String) loginData.get("usrLoginId");
        String password = (String) loginData.get("password");
        
        if (usrLoginId == null || usrLoginId.trim().isEmpty()) {
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.VALIDATION_REQUIRED_FIELD), ErrorCode.VALIDATION_REQUIRED_FIELD);
        }
        
        if (password == null || password.trim().isEmpty()) {
            return ApiResponse.error(ErrorCode.getDescription(ErrorCode.VALIDATION_REQUIRED_FIELD), ErrorCode.VALIDATION_REQUIRED_FIELD);
        }
        
        return ApiResponse.success(getMessage("SERVICE_010"), null);
    }
    
    /**
     * 중복 확인
     */
    private ApiResponse<Map<String, Object>> checkDuplicates(String usrLoginId, String email) {
        // 아이디 중복 확인
        int idCount = loginDao.checkIdDuplicate(usrLoginId);
        if (idCount > 0) {
            return ApiResponse.error(getMessage("USER_003"), ErrorCode.USER_ID_DUPLICATE);
        }
        
        // 이메일 중복 확인
        int emailCount = loginDao.checkEmailDuplicate(email);
        if (emailCount > 0) {
            return ApiResponse.error(getMessage("USER_004"), ErrorCode.USER_EMAIL_DUPLICATE);
        }
        
        // 성공 응답
        Map<String, Object> data = new HashMap<>();
        data.put("usrLoginId", usrLoginId);
        data.put("email", email);
        return ApiResponse.success(getMessage("SERVICE_011"), data);
    }
    
    /**
     * 사용자 인증
     */
    private ApiResponse<Map<String, Object>> authenticateUser(Map<String, Object> user, String password) {
        // 계정 활성화 상태 확인
        Object isUseObj = user.get("is_use");
        boolean isUse = false;
        
        // boolean 값 처리 (1이면 true, 0이면 false)
        if (isUseObj instanceof Boolean) {
            isUse = (Boolean) isUseObj;
        } else if (isUseObj instanceof Number) {
            isUse = ((Number) isUseObj).intValue() == 1;
        }
        
        if (!isUse) {
            return ApiResponse.error(getMessage("AUTH_003"), ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        
        // 비밀번호 확인
        String storedPassword = (String) user.get("pwd");
        if (!passwordEncoder.matches(password, storedPassword)) {
            return ApiResponse.error(getMessage("AUTH_001"), ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        
        return ApiResponse.success(getMessage("SERVICE_012"), null);
    }
    
    /**
     * Refresh Token으로 새로운 Access Token 발급 (Refresh Token도 함께 재발급)
     * 
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token 및 Refresh Token
     */
    public ApiResponse<Map<String, Object>> refreshAccessToken(String refreshToken) {
        try {
            logger.info("=== 토큰 재발급 시작 ===");
            logger.debug("Refresh Token: {}", refreshToken);
            
            // Refresh Token으로 세션 검증
            Map<String, Object> sessionInfo = loginDao.findSessionByRefreshToken(refreshToken);
            if (sessionInfo == null) {
                return ApiResponse.error(getMessage("SERVICE_013"), ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
            }
            
            // 사용자 정보 조회
            Long usrId = ((Number) sessionInfo.get("usr_id")).longValue();
            Map<String, Object> user = loginDao.findById(usrId);
            if (user == null) {
                return ApiResponse.error(getMessage("SERVICE_014"), ErrorCode.USER_NOT_FOUND);
            }
            
            // 새로운 Access Token 생성
            String newAccessToken = jwtUtil.generateAccessToken(user);
            
            // 보안을 위해 Refresh Token도 재발급 (일반적인 패턴)
            String newRefreshToken = jwtUtil.generateRefreshToken(user);
            
            // 기존 세션 삭제 후 새 세션 저장
            loginDao.deleteActiveSessions(usrId);
            saveUserSession(usrId, newAccessToken, newRefreshToken, null, null);
            
            // 성공 응답 데이터 생성
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("accessToken", newAccessToken);
            tokenData.put("refreshToken", newRefreshToken);
            tokenData.put("tokenType", "Bearer");
            tokenData.put("expiresIn", jwtUtil.getAccessTokenExpirationInSeconds()); // 15분 (초 단위)
            
            return ApiResponse.success(getMessage("SERVICE_015"), tokenData);
            
        } catch (Exception e) {
            logger.error("토큰 재발급 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error(getMessage("SERVICE_016"), ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }
    
    /**
     * 로그아웃 처리
     * 
     * @param usrId 사용자 ID
     * @return 로그아웃 결과
     */
    public ApiResponse<Object> logout(Long usrId) {
        try {
            logger.info("=== 로그아웃 시작 ===");
            logger.debug("사용자 ID: {}", usrId);
            
            // 세션 로그아웃 처리 (로그아웃 시간 및 세션 지속시간 기록)
            Map<String, Object> logoutData = new HashMap<>();
            logoutData.put("usrId", usrId);
            int logoutResult = loginDao.updateSessionLogout(logoutData);
            logger.debug("세션 로그아웃 처리 결과: {}", logoutResult);
            
            return ApiResponse.success(getMessage("SERVICE_017"), null);
            
        } catch (Exception e) {
            logger.error("로그아웃 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error(getMessage("SERVICE_016"), ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }
    
    /**
     * 사용자 정보 조회
     * 
     * @param usrId 사용자 ID
     * @return 사용자 정보
     */
    public ApiResponse<Map<String, Object>> getUserInfo(Long usrId) {
        try {
            logger.info("=== 사용자 정보 조회 시작 ===");
            logger.debug("사용자 ID: {}", usrId);
            
            Map<String, Object> user = loginDao.findById(usrId);
            if (user == null) {
                return ApiResponse.error(getMessage("SERVICE_014"), ErrorCode.USER_NOT_FOUND);
            }
            
            // 비밀번호 제외한 사용자 정보 반환
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("usrId", user.get("usr_id"));
            userInfo.put("usrLoginId", user.get("usr_login_id"));
            userInfo.put("usrNm", user.get("usr_nm"));
            userInfo.put("email", user.get("email"));
            userInfo.put("usrTpCd", user.get("usr_tp_cd"));
            userInfo.put("phoneNum", user.get("phone_num"));
            userInfo.put("isUse", user.get("is_use"));
            userInfo.put("creDt", user.get("cre_dt"));
            userInfo.put("updDt", user.get("upd_dt"));
            
            return ApiResponse.success(getMessage("SERVICE_018"), userInfo);
            
        } catch (Exception e) {
            logger.error("사용자 정보 조회 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error(getMessage("SERVICE_016"), ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }
    
    // ==================== 응답 생성 헬퍼 함수들 ====================
    
    /**
     * 메시지 코드로부터 메시지 내용 조회 (Redis 캐시 우선)
     * 
     * @param msgCd 메시지 코드
     * @return 메시지 내용
     */
    private String getMessage(String msgCd) {
        try {
            Map<String, Object> messageInfo = commonCodeService.getMessageCode(msgCd);
            if (messageInfo != null && messageInfo.get("msg_cont") != null) {
                return (String) messageInfo.get("msg_cont");
            }
        } catch (Exception e) {
            logger.warn("메시지 코드 조회 실패: {}, 기본 메시지 사용", msgCd, e);
        }
        
        // 캐시에서 조회 실패 시 간단한 fallback 메시지 반환
        logger.error("메시지 코드 '{}'를 Redis 캐시에서 찾을 수 없습니다. 데이터베이스에 메시지가 등록되어 있는지 확인하세요.", msgCd);
        return "시스템 오류가 발생했습니다. 관리자에게 문의하세요.";
    }
    
    
    
    /**
     * 사용자 데이터 생성
     */
    private Map<String, Object> createUserData(Map<String, Object> signupData, String encodedPassword) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("usrLoginId", signupData.get("usrLoginId"));
        userData.put("usrNm", signupData.get("usrNm"));
        userData.put("email", signupData.get("email"));
        userData.put("password", encodedPassword);
        userData.put("phoneNum", signupData.get("phoneNum"));
        return userData;
    }
    
    /**
     * 로그인 성공 시 처리 (통계 업데이트 + 이력 저장)
     */
    private void saveLoginSuccess(Long usrId, String usrLoginId, String ipAddr, Map<String, Object> loginData) {
        try {
            // 사용자 통계 업데이트
            Map<String, Object> statsData = new HashMap<>();
            statsData.put("usrId", usrId);
            statsData.put("ipAddr", ipAddr != null ? ipAddr : "127.0.0.1");
            
            int statsResult = loginDao.updateUserStats(statsData);
            logger.debug("사용자 통계 업데이트 결과: {}", statsResult);
            
            // 로그인 성공 이력 저장
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("usrId", usrId);
            historyData.put("usrLoginId", usrLoginId);
            historyData.put("ipAddr", ipAddr != null ? ipAddr : "127.0.0.1");
            historyData.put("isSuccess", true);
            historyData.put("failReason", null);
            historyData.put("userAgent", loginData.get("userAgent"));
            historyData.put("attemptCnt", 1);
            historyData.put("isBlocked", false);
            historyData.put("blockedUntilDt", null); // 성공 시 차단 없음
            
            int histResult = loginDao.insertLoginHistory(historyData);
            logger.debug("로그인 성공 이력 저장 결과: {}", histResult);
            
        } catch (Exception e) {
            logger.warn("로그인 성공 처리 중 오류: {}", e.getMessage(), e);
            // 로그인 성공 처리 실패는 로그인 자체를 막지 않음
        }
    }
    
    /**
     * 사용자 세션 저장 (토큰 포함)
     */
    private void saveUserSession(Long usrId, String accessToken, String refreshToken, String ipAddr, Map<String, Object> loginData) {
        try {
            // 기존 활성 세션 삭제 (다중 로그인 방지)
            loginDao.deleteActiveSessions(usrId);
            
            // 새 세션 저장
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("usrId", usrId);
            sessionData.put("accessToken", accessToken);
            sessionData.put("refreshToken", refreshToken);
            sessionData.put("ipAddr", ipAddr != null ? ipAddr : "127.0.0.1");
            sessionData.put("userAgent", loginData != null ? loginData.get("userAgent") : null);
            sessionData.put("expDt", new Date(System.currentTimeMillis() + jwtUtil.getRefreshTokenExpirationInSeconds() * 1000));
            sessionData.put("blockedUntilDt", null); // 차단되지 않은 세션
            
            int sessionResult = loginDao.insertUserSession(sessionData);
            logger.debug("사용자 세션 저장 결과: {}", sessionResult);
            
        } catch (Exception e) {
            logger.warn("사용자 세션 저장 중 오류: {}", e.getMessage(), e);
            // 세션 저장 실패는 로그인 자체를 막지 않음
        }
    }
    
    
    
    /**
     * 로그인 실패 시도 기록
     * 
     * @param usrLoginId 로그인 시도한 아이디
     * @param ipAddr IP 주소
     * @param reason 실패 사유
     */
    private void recordLoginFailure(String usrLoginId, String ipAddr, String reason) {
        try {
            // IP별 시도 횟수 조회
            int attemptCount = loginDao.getLoginAttemptCount(ipAddr);
            int newAttemptCount = attemptCount + 1;
            
            // 차단 여부 결정 (5회 이상 시도 시 30분 차단)
            boolean isBlocked = newAttemptCount >= 5;
            Date blockedUntil = isBlocked ? new Date(System.currentTimeMillis() + 30 * 60 * 1000) : null;
            
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("usrId", null); // 실패 시에는 NULL
            historyData.put("usrLoginId", usrLoginId);
            historyData.put("ipAddr", ipAddr);
            historyData.put("isSuccess", false);
            historyData.put("failReason", reason);
            historyData.put("userAgent", null);
            historyData.put("attemptCnt", newAttemptCount);
            historyData.put("isBlocked", isBlocked);
            historyData.put("blockedUntilDt", blockedUntil); // 차단 시간 설정
            
            int result = loginDao.insertLoginHistory(historyData);
            logger.debug("로그인 실패 시도 기록 결과: {} - IP: {}, 사유: {}, 시도횟수: {}", result, ipAddr, reason, newAttemptCount);
            
        } catch (Exception e) {
            logger.warn("로그인 실패 시도 기록 중 오류: {}", e.getMessage(), e);
            // 로그인 실패 기록 실패는 로그인 자체를 막지 않음
        }
    }
    
    /**
     * 보안 이벤트 로깅
     * 
     * @param eventType 이벤트 타입
     * @param userId 사용자 ID
     * @param additionalInfo 추가 정보
     * @param exception 예외 (있는 경우)
     */
    private void logSecurityEvent(String eventType, Long userId, String additionalInfo, Exception exception) {
        try {
            if (exception != null) {
                logger.error("Security Event - Type: {}, UserId: {}, Info: {}, Exception: {}", 
                           eventType, userId, additionalInfo, exception.getMessage(), exception);
            } else {
                logger.info("Security Event - Type: {}, UserId: {}, Info: {}", 
                           eventType, userId, additionalInfo);
            }
            
            // 실제 운영환경에서는 로그 파일이나 외부 로깅 시스템에 저장
            // 예: logback, ELK Stack, Splunk 등
            
        } catch (Exception e) {
            // 로깅 실패는 시스템 동작에 영향을 주지 않음
            logger.error("보안 이벤트 로깅 실패: {}", e.getMessage(), e);
        }
    }
    
}
