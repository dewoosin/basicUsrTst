package com.nsustest.loginAuth.security;

import com.nsustest.loginAuth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 단위 테스트
 * 
 * JWT 토큰 검증 및 Spring Security 인증 처리를 테스트합니다.
 * 
 * @author nsustest
 */
@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {
    
    @Mock
    private JwtUtil jwtUtil;
    
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;
    
    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        
        // SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }
    
    /**
     * 테스트 1: 유효한 JWT 토큰 - 인증 성공
     */
    @Test
    void testDoFilterInternal_ValidToken_Success() throws Exception {
        // Given
        String validToken = "valid.jwt.token";
        Map<String, Object> userInfo = createMockUserInfo(1L, "testuser", "테스트사용자", "test@example.com", "02");
        
        request.addHeader("Authorization", "Bearer " + validToken);
        
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUserInfoFromToken(validToken)).thenReturn(userInfo);
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "인증 객체가 생성되어야 함");
        assertEquals("testuser", authentication.getName(), "사용자명이 일치해야 함");
        assertTrue(authentication.isAuthenticated(), "인증 상태여야 함");
        
        verify(jwtUtil).validateToken(validToken);
        verify(jwtUtil).getUserInfoFromToken(validToken);
    }
    
    /**
     * 테스트 2: 관리자 권한 토큰 - ADMIN 권한 부여
     */
    @Test
    void testDoFilterInternal_AdminToken_HasAdminAuthority() throws Exception {
        // Given
        String adminToken = "admin.jwt.token";
        Map<String, Object> adminInfo = createMockUserInfo(1L, "admin", "관리자", "admin@example.com", "01");
        
        request.addHeader("Authorization", "Bearer " + adminToken);
        
        when(jwtUtil.validateToken(adminToken)).thenReturn(true);
        when(jwtUtil.getUserInfoFromToken(adminToken)).thenReturn(adminInfo);
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ADMIN")), 
            "ADMIN 권한이 있어야 함");
    }
    
    /**
     * 테스트 3: 유효하지 않은 JWT 토큰 - 인증 실패
     */
    @Test
    void testDoFilterInternal_InvalidToken_AuthenticationFails() throws Exception {
        // Given
        String invalidToken = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + invalidToken);
        
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "인증 객체가 생성되지 않아야 함");
        
        verify(jwtUtil).validateToken(invalidToken);
        verify(jwtUtil, never()).getUserInfoFromToken(anyString());
    }
    
    /**
     * 테스트 4: Authorization 헤더 없음 - 인증 스킵
     */
    @Test
    void testDoFilterInternal_NoAuthorizationHeader_SkipsAuthentication() throws Exception {
        // Given
        // Authorization 헤더를 추가하지 않음
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "인증 객체가 생성되지 않아야 함");
        
        verify(jwtUtil, never()).validateToken(anyString());
        verify(jwtUtil, never()).getUserInfoFromToken(anyString());
    }
    
    /**
     * 테스트 5: Bearer 없는 Authorization 헤더 - 인증 스킵
     */
    @Test
    void testDoFilterInternal_NoBearerPrefix_SkipsAuthentication() throws Exception {
        // Given
        request.addHeader("Authorization", "InvalidPrefix token");
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "인증 객체가 생성되지 않아야 함");
        
        verify(jwtUtil, never()).validateToken(anyString());
    }
    
    /**
     * 테스트 6: JWT 검증 중 예외 발생 - 예외 처리
     */
    @Test
    void testDoFilterInternal_ExceptionDuringValidation_HandlesGracefully() throws Exception {
        // Given
        String token = "exception.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        
        when(jwtUtil.validateToken(token)).thenThrow(new RuntimeException("JWT 검증 오류"));
        
        // When & Then
        assertDoesNotThrow(() -> {
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        }, "예외가 발생해도 필터 체인은 계속되어야 함");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "인증 객체가 생성되지 않아야 함");
    }
    
    /**
     * 테스트 7: UserInfo가 null인 경우 - 인증 실패
     */
    @Test
    void testDoFilterInternal_NullUserInfo_SkipsAuthentication() throws Exception {
        // Given
        String token = "valid.but.no.userinfo.token";
        request.addHeader("Authorization", "Bearer " + token);
        
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserInfoFromToken(token)).thenReturn(null);
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "UserInfo가 null이면 인증 객체가 생성되지 않아야 함");
    }
    
    /**
     * 테스트 8: shouldNotFilter - 공개 경로는 필터링 제외
     */
    @Test
    void testShouldNotFilter_PublicPaths_ReturnsTrue() throws Exception {
        // Given
        String[] publicPaths = {
            "/api/check-id",
            "/static/css/style.css",
            "/css/login.css",
            "/js/script.js",
            "/images/logo.png",
            "/",
            "/favicon.ico"
        };
        
        // When & Then
        for (String path : publicPaths) {
            MockHttpServletRequest publicRequest = new MockHttpServletRequest();
            publicRequest.setRequestURI(path);
            
            boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(publicRequest);
            assertTrue(shouldNotFilter, path + "는 필터링을 건너뛰어야 함");
        }
    }
    
    /**
     * 테스트 9: shouldNotFilter - 보호된 경로는 필터링 수행
     */
    @Test
    void testShouldNotFilter_ProtectedPaths_ReturnsFalse() throws Exception {
        // Given
        String[] protectedPaths = {
            "/api/user",
            "/api/logout",
            "/api/admin/cache/refresh",
            "/dashboard"
        };
        
        // When & Then
        for (String path : protectedPaths) {
            MockHttpServletRequest protectedRequest = new MockHttpServletRequest();
            protectedRequest.setRequestURI(path);
            
            boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(protectedRequest);
            assertFalse(shouldNotFilter, path + "는 필터링을 수행해야 함");
        }
    }
    
    /**
     * 테스트 10: Authentication Details에 사용자 정보 포함
     */
    @Test
    void testDoFilterInternal_ValidToken_SetsDetailsWithUserInfo() throws Exception {
        // Given
        String validToken = "valid.jwt.token";
        Long expectedUsrId = 123L;
        String expectedLoginId = "testuser";
        String expectedName = "테스트사용자";
        String expectedEmail = "test@example.com";
        
        Map<String, Object> userInfo = createMockUserInfo(
            expectedUsrId, expectedLoginId, expectedName, expectedEmail, "02"
        );
        
        request.addHeader("Authorization", "Bearer " + validToken);
        
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUserInfoFromToken(validToken)).thenReturn(userInfo);
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        assertNotNull(details, "Details에 사용자 정보가 포함되어야 함");
        assertEquals(expectedUsrId, details.get("usrId"));
        assertEquals(expectedLoginId, details.get("usrLoginId"));
        assertEquals(expectedName, details.get("usrNm"));
        assertEquals(expectedEmail, details.get("email"));
    }
    
    /**
     * Mock 사용자 정보 생성 헬퍼 메서드
     * 
     * @param usrId 사용자 ID
     * @param usrLoginId 로그인 아이디
     * @param usrNm 사용자 이름
     * @param email 이메일
     * @param usrTpCd 사용자 타입 코드
     * @return 사용자 정보 Map
     */
    private Map<String, Object> createMockUserInfo(Long usrId, String usrLoginId, 
                                                    String usrNm, String email, String usrTpCd) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("usrId", usrId);
        userInfo.put("usrLoginId", usrLoginId);
        userInfo.put("usrNm", usrNm);
        userInfo.put("email", email);
        userInfo.put("usrTpCd", usrTpCd);
        return userInfo;
    }
}

