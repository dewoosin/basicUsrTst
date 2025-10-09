package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Redis 키 접두사
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String LOGIN_LIMIT_PREFIX = "login_limit:";
    
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
     * 일반적인 Rate-Limiting 검사 (Redis 기반)
     * 
     * @param clientIp 클라이언트 IP 주소
     * @return true: 허용, false: 차단
     */
    private boolean checkGeneralRateLimit(String clientIp) {
        try {
            // 분당 제한 검사
            String minuteKey = RATE_LIMIT_PREFIX + clientIp + ":minute";
            Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
            if (minuteCount == 1) {
                redisTemplate.expire(minuteKey, 60, TimeUnit.SECONDS);
            }
            if (minuteCount > maxRequestsPerMinute) {
                recordRateLimitExceeded(clientIp, "MINUTE_LIMIT", "분당 요청 한도 초과");
                return false;
            }
            
            // 시간당 제한 검사
            String hourKey = RATE_LIMIT_PREFIX + clientIp + ":hour";
            Long hourCount = redisTemplate.opsForValue().increment(hourKey);
            if (hourCount == 1) {
                redisTemplate.expire(hourKey, 3600, TimeUnit.SECONDS);
            }
            if (hourCount > maxRequestsPerHour) {
                recordRateLimitExceeded(clientIp, "HOUR_LIMIT", "시간당 요청 한도 초과");
                return false;
            }
            
            // 일당 제한 검사
            String dayKey = RATE_LIMIT_PREFIX + clientIp + ":day";
            Long dayCount = redisTemplate.opsForValue().increment(dayKey);
            if (dayCount == 1) {
                redisTemplate.expire(dayKey, 86400, TimeUnit.SECONDS);
            }
            if (dayCount > maxRequestsPerDay) {
                recordRateLimitExceeded(clientIp, "DAY_LIMIT", "일당 요청 한도 초과");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Redis Rate-Limiting 검사 중 오류: {}", e.getMessage(), e);
            // Redis 오류 시 안전하게 허용
            return true;
        }
    }
    
    /**
     * 로그인 API Rate-Limiting 검사 (Redis 기반)
     * 
     * @param clientIp 클라이언트 IP 주소
     * @return true: 허용, false: 차단
     */
    private boolean checkLoginRateLimit(String clientIp) {
        try {
            // 분당 로그인 시도 제한
            String loginMinuteKey = LOGIN_LIMIT_PREFIX + clientIp + ":minute";
            Long loginMinuteCount = redisTemplate.opsForValue().increment(loginMinuteKey);
            if (loginMinuteCount == 1) {
                redisTemplate.expire(loginMinuteKey, 60, TimeUnit.SECONDS);
            }
            if (loginMinuteCount > maxLoginAttemptsPerMinute) {
                recordRateLimitExceeded(clientIp, "LOGIN_MINUTE_LIMIT", "분당 로그인 시도 한도 초과");
                return false;
            }
            
            // 시간당 로그인 시도 제한
            String loginHourKey = LOGIN_LIMIT_PREFIX + clientIp + ":hour";
            Long loginHourCount = redisTemplate.opsForValue().increment(loginHourKey);
            if (loginHourCount == 1) {
                redisTemplate.expire(loginHourKey, 3600, TimeUnit.SECONDS);
            }
            if (loginHourCount > maxLoginAttemptsPerHour) {
                recordRateLimitExceeded(clientIp, "LOGIN_HOUR_LIMIT", "시간당 로그인 시도 한도 초과");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Redis 로그인 Rate-Limiting 검사 중 오류: {}", e.getMessage(), e);
            // Redis 오류 시 안전하게 허용
            return true;
        }
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
     * Rate-Limiting 통계 조회 (Redis 기반)
     * 
     * @param clientIp 클라이언트 IP 주소
     * @return Rate-Limiting 통계 정보
     */
    public Map<String, Object> getRateLimitStats(String clientIp) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 일반 요청 통계
            String minuteKey = RATE_LIMIT_PREFIX + clientIp + ":minute";
            String hourKey = RATE_LIMIT_PREFIX + clientIp + ":hour";
            String dayKey = RATE_LIMIT_PREFIX + clientIp + ":day";
            
            Long minuteRequests = (Long) redisTemplate.opsForValue().get(minuteKey);
            Long hourRequests = (Long) redisTemplate.opsForValue().get(hourKey);
            Long dayRequests = (Long) redisTemplate.opsForValue().get(dayKey);
            
            // 로그인 요청 통계
            String loginMinuteKey = LOGIN_LIMIT_PREFIX + clientIp + ":minute";
            String loginHourKey = LOGIN_LIMIT_PREFIX + clientIp + ":hour";
            
            Long loginMinuteRequests = (Long) redisTemplate.opsForValue().get(loginMinuteKey);
            Long loginHourRequests = (Long) redisTemplate.opsForValue().get(loginHourKey);
            
            stats.put("minuteRequests", minuteRequests != null ? minuteRequests : 0);
            stats.put("hourRequests", hourRequests != null ? hourRequests : 0);
            stats.put("dayRequests", dayRequests != null ? dayRequests : 0);
            stats.put("loginMinuteRequests", loginMinuteRequests != null ? loginMinuteRequests : 0);
            stats.put("loginHourRequests", loginHourRequests != null ? loginHourRequests : 0);
            stats.put("maxMinuteRequests", maxRequestsPerMinute);
            stats.put("maxHourRequests", maxRequestsPerHour);
            stats.put("maxDayRequests", maxRequestsPerDay);
            stats.put("maxLoginMinuteRequests", maxLoginAttemptsPerMinute);
            stats.put("maxLoginHourRequests", maxLoginAttemptsPerHour);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Rate-Limiting 통계 조회 중 오류: {}", e.getMessage(), e);
            return Map.of("message", "Rate-Limiting 정보 조회에 실패했습니다.");
        }
    }
    
    /**
     * Rate-Limiting 캐시 초기화 (관리자용, Redis 기반)
     * 
     * @param clientIp 클라이언트 IP 주소 (null이면 전체 초기화)
     */
    public void clearRateLimitCache(String clientIp) {
        try {
            if (clientIp == null) {
                // 전체 Rate-Limiting 키 삭제
                redisTemplate.delete(redisTemplate.keys(RATE_LIMIT_PREFIX + "*"));
                redisTemplate.delete(redisTemplate.keys(LOGIN_LIMIT_PREFIX + "*"));
                logger.info("전체 Rate-Limiting 캐시가 초기화되었습니다.");
            } else {
                // 특정 IP의 Rate-Limiting 키 삭제
                String[] keysToDelete = {
                    RATE_LIMIT_PREFIX + clientIp + ":minute",
                    RATE_LIMIT_PREFIX + clientIp + ":hour",
                    RATE_LIMIT_PREFIX + clientIp + ":day",
                    LOGIN_LIMIT_PREFIX + clientIp + ":minute",
                    LOGIN_LIMIT_PREFIX + clientIp + ":hour"
                };
                redisTemplate.delete(java.util.Arrays.asList(keysToDelete));
                logger.info("IP {}의 Rate-Limiting 캐시가 초기화되었습니다.", clientIp);
            }
        } catch (Exception e) {
            logger.error("Rate-Limiting 캐시 초기화 중 오류: {}", e.getMessage(), e);
        }
    }
}
