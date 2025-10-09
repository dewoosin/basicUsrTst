package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.util.JwtUtil;
import com.nsustest.loginAuth.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 세션 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * @author nsustest
 */
@Service
public class SessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    
    @Autowired
    private LoginDao loginDao;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private CommonCodeService commonCodeService;
    
    @Autowired
    private MessageUtil messageUtil;
    
    /**
     * 로그아웃 처리
     * 
     * @param usrId 사용자 ID
     * @return 로그아웃 결과
     */
    public ApiResponse<Object> logout(Long usrId) {
        try {
            
            // 세션 로그아웃 처리 (로그아웃 시간 및 세션 지속시간 기록)
            Map<String, Object> logoutData = new HashMap<>();
            logoutData.put("usrId", usrId);
            loginDao.updateSessionLogout(logoutData);
            
            return ApiResponse.success(messageUtil.getMessage("SERVICE_017"), null);
            
        } catch (Exception e) {
            logger.error("로그아웃 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error(messageUtil.getMessage("SERVICE_016"), "SRV_001");
        }
    }
    
    /**
     * 사용자 세션 저장 (토큰 포함)
     * 
     * @param usrId 사용자 ID
     * @param accessToken Access Token
     * @param refreshToken Refresh Token
     * @param ipAddr IP 주소
     * @param loginData 로그인 데이터
     */
    public void saveUserSession(Long usrId, String accessToken, String refreshToken, String ipAddr, Map<String, Object> loginData) {
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
            sessionData.put("blockedUntilDt", null);
            
            loginDao.insertUserSession(sessionData);
            
        } catch (Exception e) {
            logger.warn("사용자 세션 저장 중 오류: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 로그인 성공 시 처리 (통계 업데이트 + 이력 저장)
     * 
     * @param usrId 사용자 ID
     * @param usrLoginId 사용자 로그인 ID
     * @param ipAddr IP 주소
     * @param loginData 로그인 데이터
     */
    public void saveLoginSuccess(Long usrId, String usrLoginId, String ipAddr, Map<String, Object> loginData) {
        try {
            // 사용자 통계 업데이트
            Map<String, Object> statsData = new HashMap<>();
            statsData.put("usrId", usrId);
            statsData.put("ipAddr", ipAddr != null ? ipAddr : "127.0.0.1");
            
            loginDao.updateUserStats(statsData);
            
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
            historyData.put("blockedUntilDt", null);
            
            loginDao.insertLoginHistory(historyData);
            
        } catch (Exception e) {
            logger.warn("로그인 성공 처리 중 오류: {}", e.getMessage(), e);
        }
    }
}
