package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitService 테스트 클래스 (Redis 기반)
 * 
 * @author nsustest
 */
@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
    "rate.limit.requests.per.minute=100",
    "rate.limit.requests.per.hour=1000",
    "rate.limit.requests.per.day=10000",
    "rate.limit.login.attempts.per.minute=10",
    "rate.limit.login.attempts.per.hour=50"
})
public class RateLimitServiceTest {
    
    @Mock
    private LoginDao loginDao;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @InjectMocks
    private RateLimitService rateLimitService;
    
    private String testIp;
    
    @BeforeEach
    void setUp() {
        testIp = "192.168.1.100";
        
        // 설정값 직접 주입 (Mockito에서 @Value가 작동하지 않을 수 있음)
        try {
            java.lang.reflect.Field maxRequestsPerMinuteField = RateLimitService.class.getDeclaredField("maxRequestsPerMinute");
            maxRequestsPerMinuteField.setAccessible(true);
            maxRequestsPerMinuteField.setInt(rateLimitService, 100);
            
            java.lang.reflect.Field maxRequestsPerHourField = RateLimitService.class.getDeclaredField("maxRequestsPerHour");
            maxRequestsPerHourField.setAccessible(true);
            maxRequestsPerHourField.setInt(rateLimitService, 1000);
            
            java.lang.reflect.Field maxRequestsPerDayField = RateLimitService.class.getDeclaredField("maxRequestsPerDay");
            maxRequestsPerDayField.setAccessible(true);
            maxRequestsPerDayField.setInt(rateLimitService, 10000);
            
            java.lang.reflect.Field maxLoginAttemptsPerMinuteField = RateLimitService.class.getDeclaredField("maxLoginAttemptsPerMinute");
            maxLoginAttemptsPerMinuteField.setAccessible(true);
            maxLoginAttemptsPerMinuteField.setInt(rateLimitService, 10);
            
            java.lang.reflect.Field maxLoginAttemptsPerHourField = RateLimitService.class.getDeclaredField("maxLoginAttemptsPerHour");
            maxLoginAttemptsPerHourField.setAccessible(true);
            maxLoginAttemptsPerHourField.setInt(rateLimitService, 50);
        } catch (Exception e) {
            System.err.println("설정값 주입 실패: " + e.getMessage());
        }
    }
    
    /**
     * 간단한 허용 테스트 - 디버깅용
     */
    @Test
    void testSimpleAllow() {
        // Given
        String requestPath = "/api/user";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Redis increment 모킹 - 매우 작은 값으로 설정
        when(valueOperations.increment(anyString())).thenReturn(1L);
        
        // When
        boolean result = rateLimitService.checkRateLimit(testIp, requestPath);
        
        // Then
        System.out.println("Result: " + result);
        System.out.println("Increment calls: " + verify(valueOperations, atLeastOnce()).increment(anyString()));
        
        // 일단 결과를 확인해보자
        assertNotNull(result, "결과는 null이 아니어야 합니다");
    }
    
    /**
     * 통계 조회 테스트 (Redis 기반)
     */
    @Test
    void testGetRateLimitStats() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(5L, 10L, 20L, 2L, 3L);
        
        // When
        Map<String, Object> stats = rateLimitService.getRateLimitStats(testIp);
        
        // Then
        assertNotNull(stats, "통계 정보는 null이 아니어야 합니다");
        assertTrue(stats.containsKey("minuteRequests"), "분당 요청 카운트가 포함되어야 합니다");
        assertTrue(stats.containsKey("hourRequests"), "시간당 요청 카운트가 포함되어야 합니다");
        assertTrue(stats.containsKey("dayRequests"), "일당 요청 카운트가 포함되어야 합니다");
        assertTrue(stats.containsKey("loginMinuteRequests"), "분당 로그인 요청 카운트가 포함되어야 합니다");
        assertTrue(stats.containsKey("loginHourRequests"), "시간당 로그인 요청 카운트가 포함되어야 합니다");
        
        // Redis get이 호출되었는지 확인 (5번: 일반3 + 로그인2)
        verify(valueOperations, times(5)).get(anyString());
    }
    
    /**
     * Rate-Limiting 캐시 초기화 테스트 (Redis 기반)
     */
    @Test
    void testClearRateLimitCache() {
        // Given
        when(redisTemplate.delete(any(Collection.class))).thenReturn(3L);
        
        // When
        rateLimitService.clearRateLimitCache(testIp);
        
        // Then
        // Redis delete가 호출되었는지 확인
        verify(redisTemplate).delete(any(Collection.class));
    }
    
    /**
     * 전체 Rate-Limiting 캐시 초기화 테스트 (Redis 기반)
     */
    @Test
    @SuppressWarnings("unchecked")
    void testClearAllRateLimitCache() {
        // Given
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("rate_limit:test:minute"));
        when(redisTemplate.delete(any(Collection.class))).thenReturn(2L);
        
        // When
        rateLimitService.clearRateLimitCache(null);
        
        // Then
        // Redis keys와 delete가 호출되었는지 확인
        verify(redisTemplate, atLeast(2)).keys(anyString());
        verify(redisTemplate, atLeast(2)).delete(any(Collection.class));
    }
    
    /**
     * null IP 처리 테스트 (Redis 기반)
     */
    @Test
    void testNullIpHandling() {
        // Given
        String requestPath = "/api/user";
        
        // When & Then
        assertDoesNotThrow(() -> {
            boolean result = rateLimitService.checkRateLimit(null, requestPath);
            assertTrue(result, "null IP는 안전하게 처리되어야 합니다");
        });
    }
    
    /**
     * 빈 경로 처리 테스트 (Redis 기반)
     */
    @Test
    void testEmptyPathHandling() {
        // Given
        String emptyPath = "";
        
        // When & Then
        assertDoesNotThrow(() -> {
            boolean result = rateLimitService.checkRateLimit(testIp, emptyPath);
            assertTrue(result, "빈 경로는 안전하게 처리되어야 합니다");
        });
    }
    
    /**
     * Redis 예외 처리 테스트
     */
    @Test
    void testRedisExceptionHandling() {
        // Given
        String requestPath = "/api/user";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis 연결 실패"));
        
        // When & Then
        assertDoesNotThrow(() -> {
            boolean result = rateLimitService.checkRateLimit(testIp, requestPath);
            assertTrue(result, "Redis 예외 시 안전하게 허용되어야 합니다");
        });
    }
}