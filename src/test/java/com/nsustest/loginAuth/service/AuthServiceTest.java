package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.util.JwtUtil;
import com.nsustest.loginAuth.util.MessageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 단위 테스트
 * 요구사항: 로그인 API, JWT 발급 (Access Token + Refresh Token), 토큰 재발급
 * 
 * @author nsustest
 */
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    
    @Mock
    private LoginDao loginDao;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private SessionService sessionService;
    
    @Mock
    private CommonCodeService commonCodeService;
    
    @Mock
    private MessageUtil messageUtil;
    
    @InjectMocks
    private AuthService authService;
    
    private Map<String, Object> validLoginData;
    private Map<String, Object> mockUser;
    private Map<String, Object> mockSession;
    
    @BeforeEach
    void setUp() {
        // 유효한 로그인 데이터
        validLoginData = new HashMap<>();
        validLoginData.put("usrLoginId", "testuser");
        validLoginData.put("password", "Test123!@#");
        validLoginData.put("ipAddr", "192.168.1.100");
        validLoginData.put("userAgent", "Mozilla/5.0");
        
        // Mock 사용자 정보
        mockUser = new HashMap<>();
        mockUser.put("usr_id", 1L);
        mockUser.put("usr_login_id", "testuser");
        mockUser.put("usr_nm", "테스트사용자");
        mockUser.put("email", "test@example.com");
        mockUser.put("pwd", "$2a$10$encodedPasswordHash");
        mockUser.put("is_use", true);
        
        // Mock 세션 정보
        mockSession = new HashMap<>();
        mockSession.put("usr_id", 1L);
        mockSession.put("usr_login_id", "testuser");
        mockSession.put("usr_nm", "테스트사용자");
        mockSession.put("email", "test@example.com");
    }
    
    /**
     * 로그인 - 성공 케이스
     */
    @Test
    void testLogin_Success() {
        // Given
        String accessToken = "access.token.here";

        
        String refreshToken = "refresh.token.here";
        
        when(loginDao.checkIpBlocked("192.168.1.100")).thenReturn(null);
        when(loginDao.findByLoginId("testuser")).thenReturn(mockUser);
        when(passwordEncoder.matches("Test123!@#", "$2a$10$encodedPasswordHash")).thenReturn(true);
        when(loginDao.resetIpBlock("192.168.1.100")).thenReturn(1);
        when(jwtUtil.generateAccessToken(mockUser)).thenReturn(accessToken);
        when(jwtUtil.generateRefreshToken(mockUser)).thenReturn(refreshToken);
        when(jwtUtil.getAccessTokenExpirationInSeconds()).thenReturn(900L);
        when(messageUtil.getMessage(anyString())).thenReturn("로그인되었습니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.login(validLoginData);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("로그인되었습니다.", response.getMessage());
        assertNotNull(response.getData());
        
        Map<String, Object> authData = response.getData();
        assertEquals(accessToken, authData.get("accessToken"));
        assertEquals(refreshToken, authData.get("refreshToken"));
        assertEquals("Bearer", authData.get("tokenType"));
        assertEquals(900L, authData.get("expiresIn"));
        assertNotNull(authData.get("user"));
        
        verify(loginDao).checkIpBlocked("192.168.1.100");
        verify(loginDao).findByLoginId("testuser");
        verify(passwordEncoder).matches("Test123!@#", "$2a$10$encodedPasswordHash");
        verify(loginDao).resetIpBlock("192.168.1.100");
        verify(jwtUtil).generateAccessToken(mockUser);
        verify(jwtUtil).generateRefreshToken(mockUser);
        verify(sessionService).saveLoginSuccess(eq(1L), eq("testuser"), eq("192.168.1.100"), any(Map.class));
        verify(sessionService).saveUserSession(eq(1L), eq(accessToken), eq(refreshToken), eq("192.168.1.100"), any(Map.class));
    }
    
    /**
     * 로그인 - 잘못된 아이디
     */
    @Test
    void testLogin_InvalidUserId() {
        // Given
        when(loginDao.checkIpBlocked("192.168.1.100")).thenReturn(null);
        when(loginDao.findByLoginId("testuser")).thenReturn(null);
        when(messageUtil.getMessage(anyString())).thenReturn("아이디 또는 비밀번호가 올바르지 않습니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.login(validLoginData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("아이디 또는 비밀번호가 올바르지 않습니다.", response.getMessage());
        assertEquals("AUTH_001", response.getErrorCode());
        
        verify(loginDao).checkIpBlocked("192.168.1.100");
        verify(loginDao).findByLoginId("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateAccessToken(any(Map.class));
        verify(jwtUtil, never()).generateRefreshToken(any(Map.class));
        verify(sessionService, never()).saveLoginSuccess(anyLong(), anyString(), anyString(), any(Map.class));
    }
    
    /**
     * 로그인 - 잘못된 비밀번호
     */
    @Test
    void testLogin_InvalidPassword() {
        // Given
        when(loginDao.checkIpBlocked("192.168.1.100")).thenReturn(null);
        when(loginDao.findByLoginId("testuser")).thenReturn(mockUser);
        when(passwordEncoder.matches("Test123!@#", "$2a$10$encodedPasswordHash")).thenReturn(false);
        when(messageUtil.getMessage(anyString())).thenReturn("아이디 또는 비밀번호가 올바르지 않습니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.login(validLoginData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("아이디 또는 비밀번호가 올바르지 않습니다.", response.getMessage());
        assertEquals("AUTH_001", response.getErrorCode());
        
        verify(loginDao).checkIpBlocked("192.168.1.100");
        verify(loginDao).findByLoginId("testuser");
        verify(passwordEncoder).matches("Test123!@#", "$2a$10$encodedPasswordHash");
        verify(jwtUtil, never()).generateAccessToken(any(Map.class));
        verify(jwtUtil, never()).generateRefreshToken(any(Map.class));
        verify(sessionService, never()).saveLoginSuccess(anyLong(), anyString(), anyString(), any(Map.class));
    }
    
    /**
     * 로그인 - 비활성화된 계정
     */
    @Test
    void testLogin_DisabledAccount() {
        // Given
        Map<String, Object> disabledUser = new HashMap<>(mockUser);
        disabledUser.put("is_use", false);
        
        when(loginDao.checkIpBlocked("192.168.1.100")).thenReturn(null);
        when(loginDao.findByLoginId("testuser")).thenReturn(disabledUser);
        when(messageUtil.getMessage(anyString())).thenReturn("비활성화된 계정입니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.login(validLoginData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("비활성화된 계정입니다.", response.getMessage());
        assertEquals("AUTH_003", response.getErrorCode());
        
        verify(loginDao).checkIpBlocked("192.168.1.100");
        verify(loginDao).findByLoginId("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateAccessToken(any(Map.class));
        verify(jwtUtil, never()).generateRefreshToken(any(Map.class));
    }
    
    /**
     * 로그인 - IP 차단된 상태
     */
    @Test
    void testLogin_IpBlocked() {
        // Given
        Map<String, Object> blockedInfo = new HashMap<>();
        blockedInfo.put("ip_addr", "192.168.1.100");
        blockedInfo.put("is_blocked", true);
        
        when(loginDao.checkIpBlocked("192.168.1.100")).thenReturn(blockedInfo);
        when(messageUtil.getMessage(anyString())).thenReturn("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.login(validLoginData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.", response.getMessage());
        assertEquals("AUTH_003", response.getErrorCode());
        
        verify(loginDao).checkIpBlocked("192.168.1.100");
        verify(loginDao, never()).findByLoginId(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateAccessToken(any(Map.class));
        verify(jwtUtil, never()).generateRefreshToken(any(Map.class));
    }
    
    /**
     * 로그인 - 필수 필드 누락
     */
    @Test
    void testLogin_MissingRequiredFields() {
        // Given
        Map<String, Object> incompleteData = new HashMap<>();
        incompleteData.put("usrLoginId", "testuser");
        // password 누락
        
        // When
        ApiResponse<Map<String, Object>> response = authService.login(incompleteData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("VAL_001", response.getErrorCode());
        
        verify(loginDao, never()).checkIpBlocked(anyString());
        verify(loginDao, never()).findByLoginId(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateAccessToken(any(Map.class));
        verify(jwtUtil, never()).generateRefreshToken(any(Map.class));
    }
    
    /**
     * 토큰 재발급 - 성공
     */
    @Test
    void testRefreshAccessToken_Success() {
        // Given
        String refreshToken = "valid.refresh.token";
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";
        
        when(loginDao.findSessionByRefreshToken(refreshToken)).thenReturn(mockSession);
        when(loginDao.findById(1L)).thenReturn(mockUser);
        when(jwtUtil.generateAccessToken(mockUser)).thenReturn(newAccessToken);
        when(jwtUtil.generateRefreshToken(mockUser)).thenReturn(newRefreshToken);
        when(jwtUtil.getAccessTokenExpirationInSeconds()).thenReturn(900L);
        when(messageUtil.getMessage(anyString())).thenReturn("토큰이 재발급되었습니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.refreshAccessToken(refreshToken);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("토큰이 재발급되었습니다.", response.getMessage());
        assertNotNull(response.getData());
        
        Map<String, Object> tokenData = response.getData();
        assertEquals(newAccessToken, tokenData.get("accessToken"));
        assertEquals(newRefreshToken, tokenData.get("refreshToken"));
        assertEquals("Bearer", tokenData.get("tokenType"));
        assertEquals(900L, tokenData.get("expiresIn"));
        
        verify(loginDao).findSessionByRefreshToken(refreshToken);
        verify(loginDao).findById(1L);
        verify(jwtUtil).generateAccessToken(mockUser);
        verify(jwtUtil).generateRefreshToken(mockUser);
        verify(sessionService).saveUserSession(eq(1L), eq(newAccessToken), eq(newRefreshToken), isNull(), isNull());
    }
    
    /**
     * 토큰 재발급 - 유효하지 않은 Refresh Token
     */
    @Test
    void testRefreshAccessToken_InvalidRefreshToken() {
        // Given
        String invalidRefreshToken = "invalid.refresh.token";
        when(loginDao.findSessionByRefreshToken(invalidRefreshToken)).thenReturn(null);
        when(messageUtil.getMessage(anyString())).thenReturn("유효하지 않은 Refresh Token입니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.refreshAccessToken(invalidRefreshToken);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("유효하지 않은 Refresh Token입니다.", response.getMessage());
        assertEquals("AUTH_007", response.getErrorCode());
        
        verify(loginDao).findSessionByRefreshToken(invalidRefreshToken);
        verify(loginDao, never()).findById(anyLong());
        verify(jwtUtil, never()).generateAccessToken(any(Map.class));
        verify(jwtUtil, never()).generateRefreshToken(any(Map.class));
        verify(sessionService, never()).saveUserSession(anyLong(), anyString(), anyString(), any(), any());
    }
    
    /**
     * 토큰 재발급 - 사용자 정보 없음
     */
    @Test
    void testRefreshAccessToken_UserNotFound() {
        // Given
        String refreshToken = "valid.refresh.token";
        when(loginDao.findSessionByRefreshToken(refreshToken)).thenReturn(mockSession);
        when(loginDao.findById(1L)).thenReturn(null);
        when(messageUtil.getMessage(anyString())).thenReturn("사용자 정보를 찾을 수 없습니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.refreshAccessToken(refreshToken);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("사용자 정보를 찾을 수 없습니다.", response.getMessage());
        assertEquals("USER_001", response.getErrorCode());
        
        verify(loginDao).findSessionByRefreshToken(refreshToken);
        verify(loginDao).findById(1L);
        verify(jwtUtil, never()).generateAccessToken(any(Map.class));
        verify(jwtUtil, never()).generateRefreshToken(any(Map.class));
        verify(sessionService, never()).saveUserSession(anyLong(), anyString(), anyString(), any(), any());
    }
    
    /**
     * 토큰 재발급 - null Refresh Token
     */
    @Test
    void testRefreshAccessToken_NullRefreshToken() {
        // Given
        String nullRefreshToken = null;
        when(loginDao.findSessionByRefreshToken(null)).thenReturn(null);
        when(messageUtil.getMessage(anyString())).thenReturn("유효하지 않은 Refresh Token입니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = authService.refreshAccessToken(nullRefreshToken);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("AUTH_007", response.getErrorCode());
        
        verify(loginDao).findSessionByRefreshToken(null);
        verify(loginDao, never()).findById(anyLong());
        verify(jwtUtil, never()).generateAccessToken(any(Map.class));
        verify(jwtUtil, never()).generateRefreshToken(any(Map.class));
        verify(sessionService, never()).saveUserSession(anyLong(), anyString(), anyString(), any(), any());
    }
    
    /**
     * Mock 메시지 정보 생성 헬퍼 메서드
     */
    private Map<String, Object> createMessageInfo(String message) {
        Map<String, Object> messageInfo = new HashMap<>();
        messageInfo.put("msg_cont", message);
        return messageInfo;
    }
}
