package com.nsustest.loginAuth.controller;

import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.service.LoginService;
import com.nsustest.loginAuth.util.SecurityContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LoginController 단위 테스트
 * 요구사항: REST API 엔드포인트 테스트
 * 
 * @author nsustest
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LoginControllerTest {
    
    @Mock
    private LoginService loginService;
    
    @Mock
    private SecurityContextUtil securityContextUtil;
    
    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private LoginController loginController;
    
    private Map<String, Object> validSignupData;
    private Map<String, Object> validLoginData;
    private Map<String, Object> validRefreshData;
    
    @BeforeEach
    void setUp() {
        // 유효한 회원가입 데이터
        validSignupData = new HashMap<>();
        validSignupData.put("usrLoginId", "testuser");
        validSignupData.put("usrNm", "테스트사용자");
        validSignupData.put("email", "test@example.com");
        validSignupData.put("password", "Test123!@#");
        validSignupData.put("phoneNum", "010-1234-5678");
        
        // 유효한 로그인 데이터
        validLoginData = new HashMap<>();
        validLoginData.put("usrLoginId", "testuser");
        validLoginData.put("password", "Test123!@#");
        
        // 유효한 토큰 재발급 데이터
        validRefreshData = new HashMap<>();
        validRefreshData.put("refreshToken", "valid.refresh.token");
    }
    
    /**
     * 아이디 중복 확인 - 성공
     */
    @Test
    void testCheckIdDuplicate_Success() {
        // Given
        String usrLoginId = "newuser";
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("duplicate", false);
        responseData.put("usrLoginId", usrLoginId);
        
        ApiResponse<Map<String, Object>> successResponse = ApiResponse.success("사용 가능한 아이디입니다.", responseData);
        when(loginService.checkIdDuplicate(usrLoginId)).thenReturn(successResponse);
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.checkIdDuplicate(usrLoginId);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("사용 가능한 아이디입니다.", response.getBody().getMessage());
        assertFalse((Boolean) response.getBody().getData().get("duplicate"));
        
        verify(loginService).checkIdDuplicate(usrLoginId);
    }
    
    /**
     * 아이디 중복 확인 - 중복으로 실패
     */
    @Test
    void testCheckIdDuplicate_Duplicate() {
        // Given
        String usrLoginId = "existinguser";
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("duplicate", true);
        responseData.put("usrLoginId", usrLoginId);
        
        ApiResponse<Map<String, Object>> errorResponse = ApiResponse.error("이미 사용 중인 아이디입니다.", "USER_003");
        when(loginService.checkIdDuplicate(usrLoginId)).thenReturn(errorResponse);
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.checkIdDuplicate(usrLoginId);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("이미 사용 중인 아이디입니다.", response.getBody().getMessage());
        assertEquals("USER_003", response.getBody().getErrorCode());
        
        verify(loginService).checkIdDuplicate(usrLoginId);
    }
    
    /**
     * 회원가입 - 성공
     */
    @Test
    void testSignup_Success() {
        // Given
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("usrLoginId", "testuser");
        responseData.put("email", "test@example.com");
        
        ApiResponse<Map<String, Object>> successResponse = ApiResponse.success("회원가입이 완료되었습니다!", responseData);
        when(loginService.signup(validSignupData)).thenReturn(successResponse);
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.signup(validSignupData);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("회원가입이 완료되었습니다!", response.getBody().getMessage());
        assertEquals("testuser", response.getBody().getData().get("usrLoginId"));
        assertEquals("test@example.com", response.getBody().getData().get("email"));
        
        verify(loginService).signup(validSignupData);
    }
    
    /**
     * 회원가입 - 실패
     */
    @Test
    void testSignup_Failure() {
        // Given
        ApiResponse<Map<String, Object>> errorResponse = ApiResponse.error("회원가입에 실패했습니다.", "SRV_001");
        when(loginService.signup(validSignupData)).thenReturn(errorResponse);
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.signup(validSignupData);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("회원가입에 실패했습니다.", response.getBody().getMessage());
        assertEquals("SRV_001", response.getBody().getErrorCode());
        
        verify(loginService).signup(validSignupData);
    }
    
    /**
     * 로그인 - 성공
     */
    @Test
    void testLogin_Success() {
        // Given
        Map<String, Object> authData = new HashMap<>();
        authData.put("accessToken", "access.token.here");
        authData.put("refreshToken", "refresh.token.here");
        authData.put("tokenType", "Bearer");
        authData.put("expiresIn", 900L);
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("usrId", 1L);
        userInfo.put("usrLoginId", "testuser");
        userInfo.put("usrNm", "테스트사용자");
        userInfo.put("email", "test@example.com");
        authData.put("user", userInfo);
        
        ApiResponse<Map<String, Object>> successResponse = ApiResponse.success("로그인되었습니다.", authData);
        when(loginService.login(any(Map.class))).thenReturn(successResponse);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.login(validLoginData, request);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("로그인되었습니다.", response.getBody().getMessage());
        
        Map<String, Object> responseData = response.getBody().getData();
        assertEquals("access.token.here", responseData.get("accessToken"));
        assertEquals("refresh.token.here", responseData.get("refreshToken"));
        assertEquals("Bearer", responseData.get("tokenType"));
        assertEquals(900L, responseData.get("expiresIn"));
        assertNotNull(responseData.get("user"));
        
        verify(loginService).login(argThat(loginData -> 
            loginData.containsKey("usrLoginId") &&
            loginData.containsKey("password") &&
            loginData.containsKey("ipAddr") &&
            loginData.containsKey("userAgent")
        ));
    }
    
    /**
     * 로그인 - 실패
     */
    @Test
    void testLogin_Failure() {
        // Given
        ApiResponse<Map<String, Object>> errorResponse = ApiResponse.error("아이디 또는 비밀번호가 올바르지 않습니다.", "AUTH_001");
        when(loginService.login(any(Map.class))).thenReturn(errorResponse);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.login(validLoginData, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("아이디 또는 비밀번호가 올바르지 않습니다.", response.getBody().getMessage());
        assertEquals("AUTH_001", response.getBody().getErrorCode());
        
        verify(loginService).login(any(Map.class));
    }
    
    /**
     * 토큰 재발급 - 성공
     */
    @Test
    void testRefreshToken_Success() {
        // Given
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("accessToken", "new.access.token");
        tokenData.put("refreshToken", "new.refresh.token");
        tokenData.put("tokenType", "Bearer");
        tokenData.put("expiresIn", 900L);
        
        ApiResponse<Map<String, Object>> successResponse = ApiResponse.success("토큰이 재발급되었습니다.", tokenData);
        when(loginService.refreshAccessToken("valid.refresh.token")).thenReturn(successResponse);
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.refreshToken(validRefreshData);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("토큰이 재발급되었습니다.", response.getBody().getMessage());
        
        Map<String, Object> responseData = response.getBody().getData();
        assertEquals("new.access.token", responseData.get("accessToken"));
        assertEquals("new.refresh.token", responseData.get("refreshToken"));
        assertEquals("Bearer", responseData.get("tokenType"));
        assertEquals(900L, responseData.get("expiresIn"));
        
        verify(loginService).refreshAccessToken("valid.refresh.token");
    }
    
    /**
     * 토큰 재발급 - Refresh Token 누락
     */
    @Test
    void testRefreshToken_MissingRefreshToken() {
        // Given
        Map<String, Object> invalidData = new HashMap<>();
        // refreshToken 누락
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.refreshToken(invalidData);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Refresh Token이 필요합니다.", response.getBody().getMessage());
        assertEquals("AUTH_005", response.getBody().getErrorCode());
        
        verify(loginService, never()).refreshAccessToken(anyString());
    }
    
    /**
     * 토큰 재발급 - 빈 Refresh Token
     */
    @Test
    void testRefreshToken_EmptyRefreshToken() {
        // Given
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("refreshToken", "");
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.refreshToken(invalidData);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Refresh Token이 필요합니다.", response.getBody().getMessage());
        assertEquals("AUTH_005", response.getBody().getErrorCode());
        
        verify(loginService, never()).refreshAccessToken(anyString());
    }
    
    /**
     * 로그아웃 - 성공
     */
    @Test
    void testLogout_Success() {
        // Given
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        ApiResponse<Object> successResponse = ApiResponse.success("로그아웃되었습니다.", null);
        when(loginService.logout(1L)).thenReturn(successResponse);
        
        // When
        ResponseEntity<ApiResponse<Object>> response = loginController.logout();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("로그아웃되었습니다.", response.getBody().getMessage());
        
        verify(securityContextUtil).getCurrentUserId();
        verify(loginService).logout(1L);
    }
    
    /**
     * 로그아웃 - 인증되지 않은 사용자
     */
    @Test
    void testLogout_Unauthorized() {
        // Given
        when(securityContextUtil.getCurrentUserId()).thenReturn(null);
        
        // When
        ResponseEntity<ApiResponse<Object>> response = loginController.logout();
        
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("인증이 필요합니다.", response.getBody().getMessage());
        assertEquals("AUTH_008", response.getBody().getErrorCode());
        
        verify(securityContextUtil).getCurrentUserId();
        verify(loginService, never()).logout(anyLong());
    }
    
    /**
     * 사용자 정보 조회 - 성공
     */
    @Test
    void testGetUserInfo_Success() {
        // Given
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("usrId", 1L);
        userInfo.put("usrLoginId", "testuser");
        userInfo.put("usrNm", "테스트사용자");
        userInfo.put("email", "test@example.com");
        userInfo.put("usrTpCd", "02");
        userInfo.put("phoneNum", "010-1234-5678");
        userInfo.put("isUse", true);
        
        ApiResponse<Map<String, Object>> successResponse = ApiResponse.success("사용자 정보를 조회했습니다.", userInfo);
        when(loginService.getUserInfo(1L)).thenReturn(successResponse);
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.getUserInfo();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("사용자 정보를 조회했습니다.", response.getBody().getMessage());
        
        Map<String, Object> responseData = response.getBody().getData();
        assertEquals(1L, responseData.get("usrId"));
        assertEquals("testuser", responseData.get("usrLoginId"));
        assertEquals("테스트사용자", responseData.get("usrNm"));
        assertEquals("test@example.com", responseData.get("email"));
        
        verify(securityContextUtil).getCurrentUserId();
        verify(loginService).getUserInfo(1L);
    }
    
    /**
     * 사용자 정보 조회 - 인증되지 않은 사용자
     */
    @Test
    void testGetUserInfo_Unauthorized() {
        // Given
        when(securityContextUtil.getCurrentUserId()).thenReturn(null);
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.getUserInfo();
        
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("인증이 필요합니다.", response.getBody().getMessage());
        assertEquals("AUTH_008", response.getBody().getErrorCode());
        
        verify(securityContextUtil).getCurrentUserId();
        verify(loginService, never()).getUserInfo(anyLong());
    }
    
    /**
     * 사용자 정보 조회 - 서버 오류
     */
    @Test
    void testGetUserInfo_ServerError() {
        // Given
        when(securityContextUtil.getCurrentUserId()).thenReturn(1L);
        when(loginService.getUserInfo(1L)).thenThrow(new RuntimeException("서버 오류"));
        
        // When
        ResponseEntity<ApiResponse<Map<String, Object>>> response = loginController.getUserInfo();
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("서버 오류가 발생했습니다.", response.getBody().getMessage());
        assertEquals("SRV_001", response.getBody().getErrorCode());
        
        verify(securityContextUtil).getCurrentUserId();
        verify(loginService).getUserInfo(1L);
    }
}
