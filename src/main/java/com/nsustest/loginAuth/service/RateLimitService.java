package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate-Limiting 서비스
 * IP 단위로 요청 횟수를 제한하고 관리합니다.
 * 
 * @author nsustest
 */
@Service
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    @Autowired
    private LoginDao loginDao;
    
    // 메모리 기반 Rate-Limiting (실제 운영에서는 Redis 사용 권장)
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    
    // Rate-Limiting 설정값 주입
    @Value("${rate.limit.requests.per.minute:60}")
    private int maxRequestsPerMinute;
    
    @Value("${rate.limit.requests.per.hour:1000}")
    private int maxRequestsPerHour;
    
    @Value("${rate.limit.requests.per.day:10000}")
    private int maxRequestsPerDay;
    
    @Value("${rate.limit.login.attempts.per.minute:5}")
    private int maxLoginAttemptsPerMinute;
    
    @Value("${rate.limit.login.attempts.per.hour:20}")
    private int maxLoginAttemptsPerHour;
    
    /**
     * Rate-Limiting 검사
     * 
     * @param clientIp 클라이언트 IP 주소
     * @param requestPath 요청 경로
     * @return true: 허용, false: 차단
     */
    public boolean checkRateLimit(String clientIp, String requestPath) {
        try {
            // IP 기반 Rate-Limiting
            boolean generalLimit = checkGeneralRateLimit(clientIp);
            
            // 로그인 API에 대한 특별 제한
            boolean loginLimit = true;
            if (isLoginApi(requestPath)) {
                loginLimit = checkLoginRateLimit(clientIp);
            }
            
            return generalLimit && loginLimit;
            
        } catch (Exception e) {
            // Rate-Limiting 검사 실패 시 안전하게 허용
            logger.error("Rate-Limiting 검사 중 오류 발생: {}", e.getMessage(), e);
            return true;
        }
    }
    
    /**
     * 일반적인 Rate-Limiting 검사
     * 
     * @param clientIp 클라이언트 IP 주소
     * @return true: 허용, false: 차단
     */
    private boolean checkGeneralRateLimit(String clientIp) {
        long currentTime = System.currentTimeMillis();
        RateLimitInfo info = rateLimitCache.computeIfAbsent(clientIp, k -> new RateLimitInfo());
        
        // 만료된 기록 정리
        cleanupExpiredRequests(info, currentTime);
        
        // 분당 제한 검사
        if (info.minuteRequests.size() >= maxRequestsPerMinute) {
            recordRateLimitExceeded(clientIp, "MINUTE_LIMIT", "분당 요청 한도 초과");
            return false;
        }
        
        // 시간당 제한 검사
        if (info.hourRequests.size() >= maxRequestsPerHour) {
            recordRateLimitExceeded(clientIp, "HOUR_LIMIT", "시간당 요청 한도 초과");
            return false;
        }
        
        // 일당 제한 검사
        if (info.dayRequests.size() >= maxRequestsPerDay) {
            recordRateLimitExceeded(clientIp, "DAY_LIMIT", "일당 요청 한도 초과");
            return false;
        }
        
        // 요청 기록 추가
        info.minuteRequests.put(currentTime, new AtomicInteger(1));
        info.hourRequests.put(currentTime, new AtomicInteger(1));
        info.dayRequests.put(currentTime, new AtomicInteger(1));
        
        return true;
    }
    
    /**
     * 로그인 API Rate-Limiting 검사
     * 
     * @param clientIp 클라이언트 IP 주소
     * @return true: 허용, false: 차단
     */
    private boolean checkLoginRateLimit(String clientIp) {
        long currentTime = System.currentTimeMillis();
        RateLimitInfo info = rateLimitCache.computeIfAbsent(clientIp, k -> new RateLimitInfo());
        
        // 로그인 시도 기록 정리
        cleanupExpiredLoginAttempts(info, currentTime);
        
        // 분당 로그인 시도 제한
        if (info.loginMinuteRequests.size() >= maxLoginAttemptsPerMinute) {
            recordRateLimitExceeded(clientIp, "LOGIN_MINUTE_LIMIT", "분당 로그인 시도 한도 초과");
            return false;
        }
        
        // 시간당 로그인 시도 제한
        if (info.loginHourRequests.size() >= maxLoginAttemptsPerHour) {
            recordRateLimitExceeded(clientIp, "LOGIN_HOUR_LIMIT", "시간당 로그인 시도 한도 초과");
            return false;
        }
        
        // 로그인 시도 기록 추가
        info.loginMinuteRequests.put(currentTime, new AtomicInteger(1));
        info.loginHourRequests.put(currentTime, new AtomicInteger(1));
        
        return true;
    }
    
    /**
     * 로그인 API 여부 확인
     * 
     * @param requestPath 요청 경로
     * @return true: 로그인 API, false: 일반 API
     */
    private boolean isLoginApi(String requestPath) {
        return requestPath != null && (
            requestPath.contains("/api/login") ||
            requestPath.contains("/api/signup") ||
            requestPath.contains("/api/check-id")
        );
    }
    
    /**
     * 만료된 요청 기록 정리
     * 
     * @param info Rate-Limiting 정보
     * @param currentTime 현재 시간
     */
    private void cleanupExpiredRequests(RateLimitInfo info, long currentTime) {
        long oneMinuteAgo = currentTime - 60 * 1000;
        long oneHourAgo = currentTime - 60 * 60 * 1000;
        long oneDayAgo = currentTime - 24 * 60 * 60 * 1000;
        
        info.minuteRequests.entrySet().removeIf(entry -> entry.getKey() < oneMinuteAgo);
        info.hourRequests.entrySet().removeIf(entry -> entry.getKey() < oneHourAgo);
        info.dayRequests.entrySet().removeIf(entry -> entry.getKey() < oneDayAgo);
    }
    
    /**
     * 만료된 로그인 시도 기록 정리
     * 
     * @param info Rate-Limiting 정보
     * @param currentTime 현재 시간
     */
    private void cleanupExpiredLoginAttempts(RateLimitInfo info, long currentTime) {
        long oneMinuteAgo = currentTime - 60 * 1000;
        long oneHourAgo = currentTime - 60 * 60 * 1000;
        
        info.loginMinuteRequests.entrySet().removeIf(entry -> entry.getKey() < oneMinuteAgo);
        info.loginHourRequests.entrySet().removeIf(entry -> entry.getKey() < oneHourAgo);
    }
    
    /**
     * Rate-Limit 초과 기록
     * 
     * @param clientIp 클라이언트 IP 주소
     * @param limitType 제한 타입
     * @param reason 사유
     */
    private void recordRateLimitExceeded(String clientIp, String limitType, String reason) {
        try {
            Map<String, Object> rateLimitData = new HashMap<>();
            rateLimitData.put("ipAddr", clientIp);
            rateLimitData.put("limitType", limitType);
            rateLimitData.put("reason", reason);
            rateLimitData.put("blockDt", new java.util.Date());
            
            loginDao.insertRateLimitHistory(rateLimitData);
            
            logger.warn("Rate-Limit 초과 기록: IP={}, Type={}, Reason={}", clientIp, limitType, reason);
            
        } catch (Exception e) {
            logger.error("Rate-Limit 초과 기록 저장 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Rate-Limiting 정보 클래스
     */
    private static class RateLimitInfo {
        // 일반 요청 기록 (시간 -> 요청 수)
        final Map<Long, AtomicInteger> minuteRequests = new ConcurrentHashMap<>();
        final Map<Long, AtomicInteger> hourRequests = new ConcurrentHashMap<>();
        final Map<Long, AtomicInteger> dayRequests = new ConcurrentHashMap<>();
        
        // 로그인 요청 기록
        final Map<Long, AtomicInteger> loginMinuteRequests = new ConcurrentHashMap<>();
        final Map<Long, AtomicInteger> loginHourRequests = new ConcurrentHashMap<>();
    }
    
    /**
     * Rate-Limiting 통계 조회
     * 
     * @param clientIp 클라이언트 IP 주소
     * @return Rate-Limiting 통계 정보
     */
    public Map<String, Object> getRateLimitStats(String clientIp) {
        RateLimitInfo info = rateLimitCache.get(clientIp);
        if (info == null) {
            return Map.of("message", "Rate-Limiting 정보가 없습니다.");
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("minuteRequests", info.minuteRequests.size());
        stats.put("hourRequests", info.hourRequests.size());
        stats.put("dayRequests", info.dayRequests.size());
        stats.put("loginMinuteRequests", info.loginMinuteRequests.size());
        stats.put("loginHourRequests", info.loginHourRequests.size());
        stats.put("maxMinuteRequests", maxRequestsPerMinute);
        stats.put("maxHourRequests", maxRequestsPerHour);
        stats.put("maxDayRequests", maxRequestsPerDay);
        stats.put("maxLoginMinuteRequests", maxLoginAttemptsPerMinute);
        stats.put("maxLoginHourRequests", maxLoginAttemptsPerHour);
        
        return stats;
    }
    
    /**
     * Rate-Limiting 캐시 초기화 (관리자용)
     * 
     * @param clientIp 클라이언트 IP 주소 (null이면 전체 초기화)
     */
    public void clearRateLimitCache(String clientIp) {
        if (clientIp == null) {
            rateLimitCache.clear();
            logger.info("전체 Rate-Limiting 캐시가 초기화되었습니다.");
        } else {
            rateLimitCache.remove(clientIp);
            logger.info("IP {}의 Rate-Limiting 캐시가 초기화되었습니다.", clientIp);
        }
    }
}
