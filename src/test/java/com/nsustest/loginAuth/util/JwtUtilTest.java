package com.nsustest.loginAuth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 클래스의 단위 테스트
 * 
 * @author nsustest
 */
@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=testSecretKey123456789012345678901234567890123456789012345678901234567890",
    "jwt.access-token-expiration=900000",
    "jwt.refresh-token-expiration=604800000"
})
public class JwtUtilTest {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private Map<String, Object> testUser;
    
    @BeforeEach
    void setUp() {
        // 테스트용 사용자 정보 생성
        testUser = new HashMap<>();
        testUser.put("usr_id", 1L);
        testUser.put("usr_login_id", "testuser");
        testUser.put("usr_nm", "테스트사용자");
        testUser.put("email", "test@example.com");
    }
    
    /**
     * Access Token 생성 테스트
     */
    @Test
    void testGenerateAccessToken() {
        // Given & When
        String accessToken = jwtUtil.generateAccessToken(testUser);
        
        // Then
        assertNotNull(accessToken, "Access Token이 생성되어야 합니다");
        assertTrue(accessToken.contains("."), "JWT 토큰은 3개의 부분으로 구성되어야 합니다");
        
        String[] parts = accessToken.split("\\.");
        assertEquals(3, parts.length, "JWT 토큰은 header.payload.signature 형태여야 합니다");
    }
    
    /**
     * Refresh Token 생성 테스트
     */
    @Test
    void testGenerateRefreshToken() {
        // Given & When
        String refreshToken = jwtUtil.generateRefreshToken(testUser);
        
        // Then
        assertNotNull(refreshToken, "Refresh Token이 생성되어야 합니다");
        assertTrue(refreshToken.contains("."), "JWT 토큰은 3개의 부분으로 구성되어야 합니다");
        
        String[] parts = refreshToken.split("\\.");
        assertEquals(3, parts.length, "JWT 토큰은 header.payload.signature 형태여야 합니다");
    }
    
    /**
     * 유효한 토큰 검증 테스트
     */
    @Test
    void testValidateValidToken() {
        // Given
        String accessToken = jwtUtil.generateAccessToken(testUser);
        
        // When
        boolean isValid = jwtUtil.validateToken(accessToken);
        
        // Then
        assertTrue(isValid, "유효한 토큰은 검증을 통과해야 합니다");
    }
    
    /**
     * 잘못된 토큰 검증 테스트
     */
    @Test
    void testValidateInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When
        boolean isValid = jwtUtil.validateToken(invalidToken);
        
        // Then
        assertFalse(isValid, "잘못된 토큰은 검증을 통과하지 않아야 합니다");
    }
    
    /**
     * null 토큰 검증 테스트
     */
    @Test
    void testValidateNullToken() {
        // Given
        String nullToken = null;
        
        // When
        boolean isValid = jwtUtil.validateToken(nullToken);
        
        // Then
        assertFalse(isValid, "null 토큰은 검증을 통과하지 않아야 합니다");
    }
    
    /**
     * 빈 토큰 검증 테스트
     */
    @Test
    void testValidateEmptyToken() {
        // Given
        String emptyToken = "";
        
        // When
        boolean isValid = jwtUtil.validateToken(emptyToken);
        
        // Then
        assertFalse(isValid, "빈 토큰은 검증을 통과하지 않아야 합니다");
    }
    
    /**
     * 토큰에서 사용자 ID 추출 테스트
     */
    @Test
    void testGetUserIdFromToken() {
        // Given
        String accessToken = jwtUtil.generateAccessToken(testUser);
        
        // When
        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        
        // Then
        assertNotNull(userId, "사용자 ID가 추출되어야 합니다");
        assertEquals(1L, userId, "추출된 사용자 ID가 일치해야 합니다");
    }
    
    /**
     * 잘못된 토큰에서 사용자 ID 추출 테스트
     */
    @Test
    void testGetUserIdFromInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When
        Long userId = jwtUtil.getUserIdFromToken(invalidToken);
        
        // Then
        assertNull(userId, "잘못된 토큰에서는 사용자 ID를 추출할 수 없어야 합니다");
    }
    
    /**
     * 토큰에서 사용자 정보 추출 테스트
     */
    @Test
    void testGetUserInfoFromToken() {
        // Given
        String accessToken = jwtUtil.generateAccessToken(testUser);
        
        // When
        Map<String, Object> userInfo = jwtUtil.getUserInfoFromToken(accessToken);
        
        // Then
        assertNotNull(userInfo, "사용자 정보가 추출되어야 합니다");
        assertEquals(1, userInfo.get("usrId"), "사용자 ID가 일치해야 합니다");
        assertEquals("testuser", userInfo.get("usrLoginId"), "로그인 ID가 일치해야 합니다");
        assertEquals("테스트사용자", userInfo.get("usrNm"), "사용자 이름이 일치해야 합니다");
        assertEquals("test@example.com", userInfo.get("email"), "이메일이 일치해야 합니다");
    }
    
    /**
     * 잘못된 토큰에서 사용자 정보 추출 테스트
     */
    @Test
    void testGetUserInfoFromInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When
        Map<String, Object> userInfo = jwtUtil.getUserInfoFromToken(invalidToken);
        
        // Then
        assertNull(userInfo, "잘못된 토큰에서는 사용자 정보를 추출할 수 없어야 합니다");
    }
    
    /**
     * 토큰에서 subject 추출 테스트
     */
    @Test
    void testGetSubjectFromToken() {
        // Given
        String accessToken = jwtUtil.generateAccessToken(testUser);
        
        // When
        String subject = jwtUtil.getSubjectFromToken(accessToken);
        
        // Then
        assertNotNull(subject, "Subject가 추출되어야 합니다");
        assertEquals("testuser", subject, "Subject는 사용자 로그인 ID와 일치해야 합니다");
    }
    
    /**
     * 토큰 만료시간 확인 테스트
     */
    @Test
    void testIsTokenExpired() {
        // Given
        String accessToken = jwtUtil.generateAccessToken(testUser);
        
        // When
        boolean isExpired = jwtUtil.isTokenExpired(accessToken);
        
        // Then
        assertFalse(isExpired, "새로 생성된 토큰은 만료되지 않아야 합니다");
    }
    
    /**
     * Access Token 만료시간 반환 테스트
     */
    @Test
    void testGetAccessTokenExpirationInSeconds() {
        // When
        long expiration = jwtUtil.getAccessTokenExpirationInSeconds();
        
        // Then
        assertEquals(900, expiration, "Access Token 만료시간은 15분(900초)이어야 합니다");
    }
    
    /**
     * Refresh Token 만료시간 반환 테스트
     */
    @Test
    void testGetRefreshTokenExpirationInSeconds() {
        // When
        long expiration = jwtUtil.getRefreshTokenExpirationInSeconds();
        
        // Then
        assertEquals(604800, expiration, "Refresh Token 만료시간은 7일(604800초)이어야 합니다");
    }
}
