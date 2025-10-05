package com.nsustest.loginAuth.interceptor;

import com.nsustest.loginAuth.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitInterceptor 클래스의 단위 테스트
 * 
 * @author nsustest
 */
@ExtendWith(MockitoExtension.class)
public class RateLimitInterceptorTest {
    
    @Mock
    private RateLimitService rateLimitService;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;
    
    private String testIp;
    private String testPath;
    
    @BeforeEach
    void setUp() {
        testIp = "192.168.1.100";
        testPath = "/api/user";
        
        // 기본적인 request 설정
        when(request.getRequestURI()).thenReturn(testPath);
        when(request.getRemoteAddr()).thenReturn(testIp);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
    }
    
    /**
     * Rate-Limiting 허용 테스트
     */
    @Test
    void testPreHandle_Allowed() throws Exception {
        // Given
        when(rateLimitService.checkRateLimit(testIp, testPath)).thenReturn(true);
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, null);
        
        // Then
        assertTrue(result, "Rate-Limiting이 허용되면 true를 반환해야 합니다");
        verify(rateLimitService).checkRateLimit(testIp, testPath);
        verify(response, never()).setStatus(anyInt());
    }
    
    /**
     * Rate-Limiting 차단 테스트
     */
    @Test
    void testPreHandle_Blocked() throws Exception {
        // Given
        when(rateLimitService.checkRateLimit(testIp, testPath)).thenReturn(false);
        java.io.PrintWriter mockWriter = mock(java.io.PrintWriter.class);
        when(response.getWriter()).thenReturn(mockWriter);
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, null);
        
        // Then
        assertFalse(result, "Rate-Limiting이 차단되면 false를 반환해야 합니다");
        verify(rateLimitService).checkRateLimit(testIp, testPath);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(mockWriter).write(anyString());
        verify(mockWriter).flush();
    }
    
    /**
     * IPv6 localhost를 IPv4로 변환 테스트
     */
    @Test
    void testPreHandle_IPv6Localhost() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        when(rateLimitService.checkRateLimit("127.0.0.1", testPath)).thenReturn(true);
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, null);
        
        // Then
        assertTrue(result);
        verify(rateLimitService).checkRateLimit("127.0.0.1", testPath);
    }
    
    /**
     * IPv6 localhost (축약형) 변환 테스트
     */
    @Test
    void testPreHandle_IPv6LocalhostShort() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("::1");
        when(rateLimitService.checkRateLimit("127.0.0.1", testPath)).thenReturn(true);
        
        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, null);
        
        // Then
        assertTrue(result);
        verify(rateLimitService).checkRateLimit("127.0.0.1", testPath);
    }
}