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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SessionService 단위 테스트
 * 요구사항: 세션 관리, 로그아웃 시 Refresh Token 무효화
 * 
 * @author nsustest
 */
@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {
    
    @Mock
    private LoginDao loginDao;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private CommonCodeService commonCodeService;
    
    @Mock
    private MessageUtil messageUtil;
    
    @InjectMocks
    private SessionService sessionService;
    
    private Long testUserId;
    private String testAccessToken;
    private String testRefreshToken;
    private String testIpAddr;
    private Map<String, Object> testLoginData;
    
    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testAccessToken = "test.access.token";
        testRefreshToken = "test.refresh.token";
        testIpAddr = "192.168.1.100";
        
        testLoginData = new HashMap<>();
        testLoginData.put("userAgent", "Mozilla/5.0");
        testLoginData.put("ipAddr", testIpAddr);
    }
    
    /**
     * 로그아웃 - 성공
     */
    @Test
    void testLogout_Success() {
        // Given
        when(loginDao.updateSessionLogout(any(Map.class))).thenReturn(1);
        when(messageUtil.getMessage(anyString())).thenReturn("로그아웃되었습니다.");
        
        // When
        ApiResponse<Object> response = sessionService.logout(testUserId);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("로그아웃되었습니다.", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getErrorCode());
        
        verify(loginDao).updateSessionLogout(argThat(logoutData -> 
            testUserId.equals(logoutData.get("usrId"))
        ));
    }
    
    /**
     * 로그아웃 - 데이터베이스 오류
     */
    @Test
    void testLogout_DatabaseError() {
        // Given
        when(loginDao.updateSessionLogout(any(Map.class))).thenThrow(new RuntimeException("DB 연결 실패"));
        when(messageUtil.getMessage(anyString())).thenReturn("서버 오류가 발생했습니다.");
        
        // When
        ApiResponse<Object> response = sessionService.logout(testUserId);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("서버 오류가 발생했습니다.", response.getMessage());
        assertEquals("SRV_001", response.getErrorCode());
        
        verify(loginDao).updateSessionLogout(any(Map.class));
    }
    
    /**
     * 사용자 세션 저장 - 성공
     */
    @Test
    void testSaveUserSession_Success() {
        // Given
        Date expirationDate = new Date(System.currentTimeMillis() + 604800000L); // 7일 후
        when(loginDao.deleteActiveSessions(testUserId)).thenReturn(2);
        when(loginDao.insertUserSession(any(Map.class))).thenReturn(1);
        when(jwtUtil.getRefreshTokenExpirationInSeconds()).thenReturn(604800L);
        
        // When
        assertDoesNotThrow(() -> {
            sessionService.saveUserSession(testUserId, testAccessToken, testRefreshToken, testIpAddr, testLoginData);
        });
        
        // Then
        verify(loginDao).deleteActiveSessions(testUserId);
        verify(loginDao).insertUserSession(any(Map.class));
    }
    
    /**
     * 사용자 세션 저장 - null IP 처리
     */
    @Test
    void testSaveUserSession_NullIp() {
        // Given
        when(loginDao.deleteActiveSessions(testUserId)).thenReturn(0);
        when(loginDao.insertUserSession(any(Map.class))).thenReturn(1);
        when(jwtUtil.getRefreshTokenExpirationInSeconds()).thenReturn(604800L);
        
        // When
        assertDoesNotThrow(() -> {
            sessionService.saveUserSession(testUserId, testAccessToken, testRefreshToken, null, testLoginData);
        });
        
        // Then
        verify(loginDao).deleteActiveSessions(testUserId);
        verify(loginDao).insertUserSession(any(Map.class));
    }
    
    /**
     * 사용자 세션 저장 - null 로그인 데이터 처리
     */
    @Test
    void testSaveUserSession_NullLoginData() {
        // Given
        when(loginDao.deleteActiveSessions(testUserId)).thenReturn(0);
        when(loginDao.insertUserSession(any(Map.class))).thenReturn(1);
        when(jwtUtil.getRefreshTokenExpirationInSeconds()).thenReturn(604800L);
        
        // When
        assertDoesNotThrow(() -> {
            sessionService.saveUserSession(testUserId, testAccessToken, testRefreshToken, testIpAddr, null);
        });
        
        // Then
        verify(loginDao).deleteActiveSessions(testUserId);
        verify(loginDao).insertUserSession(any(Map.class));
    }
    
    /**
     * 로그인 성공 처리 - 성공
     */
    @Test
    void testSaveLoginSuccess_Success() {
        // Given
        when(loginDao.updateUserStats(any(Map.class))).thenReturn(1);
        when(loginDao.insertLoginHistory(any(Map.class))).thenReturn(1);
        
        // When
        assertDoesNotThrow(() -> {
            sessionService.saveLoginSuccess(testUserId, "testuser", testIpAddr, testLoginData);
        });
        
        // Then
        verify(loginDao).updateUserStats(any(Map.class));
        verify(loginDao).insertLoginHistory(any(Map.class));
    }
    
    /**
     * 로그인 성공 처리 - null IP 처리
     */
    @Test
    void testSaveLoginSuccess_NullIp() {
        // Given
        when(loginDao.updateUserStats(any(Map.class))).thenReturn(1);
        when(loginDao.insertLoginHistory(any(Map.class))).thenReturn(1);
        
        // When
        assertDoesNotThrow(() -> {
            sessionService.saveLoginSuccess(testUserId, "testuser", null, testLoginData);
        });
        
        // Then
        verify(loginDao).updateUserStats(any(Map.class));
        verify(loginDao).insertLoginHistory(any(Map.class));
    }
    
    /**
     * 로그인 성공 처리 - 데이터베이스 오류 (로그인 자체는 막지 않음)
     */
    @Test
    void testSaveLoginSuccess_DatabaseError() {
        // Given
        when(loginDao.updateUserStats(any(Map.class))).thenThrow(new RuntimeException("DB 연결 실패"));
        
        // When & Then - 예외가 발생해도 로그인 자체는 막지 않음
        assertDoesNotThrow(() -> {
            sessionService.saveLoginSuccess(testUserId, "testuser", testIpAddr, testLoginData);
        });
        
        verify(loginDao).updateUserStats(any(Map.class));
        verify(loginDao, never()).insertLoginHistory(any(Map.class));
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
