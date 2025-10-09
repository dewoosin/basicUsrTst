package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.util.JwtUtil;
import com.nsustest.loginAuth.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * @author nsustest
 */
@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private LoginDao loginDao;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private CommonCodeService commonCodeService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private MessageUtil messageUtil;
    
    /**
     * 로그인 처리
     * 
     * @param loginData 로그인 데이터
     * @return 로그인 결과 ApiResponse
     */
    public ApiResponse<Map<String, Object>> login(Map<String, Object> loginData) {
        try {
            
            // 필수 필드 검증
            ApiResponse<Map<String, Object>> validationResult = validateLoginData(loginData);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            String usrLoginId = (String) loginData.get("usrLoginId");
            String password = (String) loginData.get("password");
            String ipAddr = (String) loginData.get("ipAddr");
            
            // IP 차단 상태 확인
            if (ipAddr != null) {
                Map<String, Object> blockedInfo = loginDao.checkIpBlocked(ipAddr);
                if (blockedInfo != null) {
                    return ApiResponse.error(messageUtil.getMessage("SERVICE_004"), "AUTH_003");
                }
            }
            
            // 사용자 정보 조회 및 검증
            Map<String, Object> user = loginDao.findByLoginId(usrLoginId);
            if (user == null) {
                // 로그인 실패 시도 기록
                if (ipAddr != null) {
                    recordLoginFailure(usrLoginId, ipAddr, "사용자 없음");
                }
                return ApiResponse.error(messageUtil.getMessage("AUTH_001"), "AUTH_001");
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
            
            
            // 로그인 성공 시 IP 차단 해제
            if (ipAddr != null) {
                loginDao.resetIpBlock(ipAddr);
            }
            
            // JWT 토큰 생성 (Access Token + Refresh Token)
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);
            
            // 로그인 성공 시 처리 (통계 업데이트 + 이력 저장)
            Long usrId = ((Number) user.get("usr_id")).longValue();
            sessionService.saveLoginSuccess(usrId, usrLoginId, ipAddr, loginData);
            
            // 세션 저장 (토큰 포함)
            sessionService.saveUserSession(usrId, accessToken, refreshToken, ipAddr, loginData);
            
            // 로그인 성공 응답 데이터 생성
            Map<String, Object> authData = new HashMap<>();
            authData.put("accessToken", accessToken);
            authData.put("refreshToken", refreshToken);
            authData.put("tokenType", "Bearer");
            authData.put("expiresIn", jwtUtil.getAccessTokenExpirationInSeconds());
            authData.put("user", Map.of(
                "usrId", user.get("usr_id"),
                "usrLoginId", user.get("usr_login_id"),
                "usrNm", user.get("usr_nm"),
                "email", user.get("email")
            ));
            
            return ApiResponse.success(messageUtil.getMessage("SERVICE_005"), authData);
            
        } catch (Exception e) {
            logger.error("로그인 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error("서버 오류가 발생했습니다.", "SRV_001");
        }
    }
    
    /**
     * Refresh Token으로 새로운 Access Token 발급
     * 
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token 및 Refresh Token
     */
    public ApiResponse<Map<String, Object>> refreshAccessToken(String refreshToken) {
        try {
            
            // Refresh Token으로 세션 검증
            Map<String, Object> sessionInfo = loginDao.findSessionByRefreshToken(refreshToken);
            if (sessionInfo == null) {
                return ApiResponse.error(messageUtil.getMessage("SERVICE_013"), "AUTH_007");
            }
            
            // 사용자 정보 조회
            Long usrId = ((Number) sessionInfo.get("usr_id")).longValue();
            Map<String, Object> user = loginDao.findById(usrId);
            if (user == null) {
                return ApiResponse.error(messageUtil.getMessage("SERVICE_014"), "USER_001");
            }
            
            // 새로운 Access Token 생성
            String newAccessToken = jwtUtil.generateAccessToken(user);
            
            // 보안을 위해 Refresh Token도 재발급
            String newRefreshToken = jwtUtil.generateRefreshToken(user);
            
            // 기존 세션 삭제 후 새 세션 저장
            sessionService.saveUserSession(usrId, newAccessToken, newRefreshToken, null, null);
            
            // 성공 응답 데이터 생성
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("accessToken", newAccessToken);
            tokenData.put("refreshToken", newRefreshToken);
            tokenData.put("tokenType", "Bearer");
            tokenData.put("expiresIn", jwtUtil.getAccessTokenExpirationInSeconds());
            
            return ApiResponse.success(messageUtil.getMessage("SERVICE_015"), tokenData);
            
        } catch (Exception e) {
            logger.error("토큰 재발급 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error(messageUtil.getMessage("SERVICE_016"), "SRV_001");
        }
    }
    
    // ==================== 검증 로직 함수들 ====================
    
    /**
     * 로그인 데이터 유효성 검사
     */
    private ApiResponse<Map<String, Object>> validateLoginData(Map<String, Object> loginData) {
        String usrLoginId = (String) loginData.get("usrLoginId");
        String password = (String) loginData.get("password");
        
        if (usrLoginId == null || usrLoginId.trim().isEmpty()) {
            return ApiResponse.error(messageUtil.getMessage("VAL_001"), "VAL_001");
        }
        
        if (password == null || password.trim().isEmpty()) {
            return ApiResponse.error(messageUtil.getMessage("VAL_001"), "VAL_001");
        }
        
        return ApiResponse.success(messageUtil.getMessage("SERVICE_010"), null);
    }
    
    /**
     * 사용자 인증
     */
    private ApiResponse<Map<String, Object>> authenticateUser(Map<String, Object> user, String password) {
        // 계정 활성화 상태 확인
        Object isUseObj = user.get("is_use");
        boolean isUse = false;
        
        if (isUseObj instanceof Boolean) {
            isUse = (Boolean) isUseObj;
        } else if (isUseObj instanceof Number) {
            isUse = ((Number) isUseObj).intValue() == 1;
        }
        
        if (!isUse) {
            return ApiResponse.error(messageUtil.getMessage("AUTH_003"), "AUTH_003");
        }
        
        // 비밀번호 확인
        String storedPassword = (String) user.get("pwd");
        if (!passwordEncoder.matches(password, storedPassword)) {
            return ApiResponse.error(messageUtil.getMessage("AUTH_001"), "AUTH_001");
        }
        
        return ApiResponse.success(messageUtil.getMessage("SERVICE_012"), null);
    }
    
    /**
     * 로그인 실패 시도 기록
     */
    private void recordLoginFailure(String usrLoginId, String ipAddr, String reason) {
        try {
            int attemptCount = loginDao.getLoginAttemptCount(ipAddr);
            int newAttemptCount = attemptCount + 1;
            
            boolean isBlocked = newAttemptCount >= 5;
            Date blockedUntil = isBlocked ? new Date(System.currentTimeMillis() + 30 * 60 * 1000) : null;
            
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("usrId", null);
            historyData.put("usrLoginId", usrLoginId);
            historyData.put("ipAddr", ipAddr);
            historyData.put("isSuccess", false);
            historyData.put("failReason", reason);
            historyData.put("userAgent", null);
            historyData.put("attemptCnt", newAttemptCount);
            historyData.put("isBlocked", isBlocked);
            historyData.put("blockedUntilDt", blockedUntil);
            
            loginDao.insertLoginHistory(historyData);
            
        } catch (Exception e) {
            logger.warn("로그인 실패 시도 기록 중 오류: {}", e.getMessage(), e);
        }
    }
}
