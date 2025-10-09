package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.service.CommonCodeService;
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
 * UserService 단위 테스트
 * 요구사항: 회원가입 API, 아이디 중복 확인, 사용자 정보 조회
 * 
 * @author nsustest
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    
    @Mock
    private LoginDao loginDao;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private CommonCodeService commonCodeService;
    
    @Mock
    private MessageUtil messageUtil;
    
    @InjectMocks
    private UserService userService;
    
    private Map<String, Object> validSignupData;
    private Map<String, Object> mockUser;
    
    @BeforeEach
    void setUp() {
        // 유효한 회원가입 데이터
        validSignupData = new HashMap<>();
        validSignupData.put("usrLoginId", "testuser");
        validSignupData.put("usrNm", "테스트사용자");
        validSignupData.put("email", "test@example.com");
        validSignupData.put("password", "Test123!@#");
        validSignupData.put("phoneNum", "010-1234-5678");
        
        // Mock 사용자 정보
        mockUser = new HashMap<>();
        mockUser.put("usr_id", 1L);
        mockUser.put("usr_login_id", "testuser");
        mockUser.put("usr_nm", "테스트사용자");
        mockUser.put("email", "test@example.com");
        mockUser.put("usr_tp_cd", "02");
        mockUser.put("phone_num", "010-1234-5678");
        mockUser.put("is_use", true);
    }
    
    /**
     * 아이디 중복 확인 - 사용 가능한 아이디
     */
    @Test
    void testCheckIdDuplicate_Available() {
        // Given
        String usrLoginId = "newuser";
        when(loginDao.checkIdDuplicate(usrLoginId)).thenReturn(0);
        when(messageUtil.getMessage(anyString())).thenReturn("사용 가능한 아이디입니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.checkIdDuplicate(usrLoginId);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("사용 가능한 아이디입니다.", response.getMessage());
        assertNotNull(response.getData());
        assertFalse((Boolean) response.getData().get("duplicate"));
        assertEquals(usrLoginId, response.getData().get("usrLoginId"));
        
        verify(loginDao).checkIdDuplicate(usrLoginId);
    }
    
    /**
     * 아이디 중복 확인 - 이미 사용 중인 아이디
     */
    @Test
    void testCheckIdDuplicate_Duplicate() {
        // Given
        String usrLoginId = "existinguser";
        when(loginDao.checkIdDuplicate(usrLoginId)).thenReturn(1);
        when(messageUtil.getMessage(anyString())).thenReturn("이미 사용 중인 아이디입니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.checkIdDuplicate(usrLoginId);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("이미 사용 중인 아이디입니다.", response.getMessage());
        assertEquals("USER_003", response.getErrorCode());
        
        verify(loginDao).checkIdDuplicate(usrLoginId);
    }
    
    /**
     * 아이디 중복 확인 - 잘못된 형식의 아이디
     */
    @Test
    void testCheckIdDuplicate_InvalidFormat() {
        // Given
        String invalidId = "ab"; // 4자 미만
        
        // When
        ApiResponse<Map<String, Object>> response = userService.checkIdDuplicate(invalidId);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("VAL_002", response.getErrorCode());
        
        verify(loginDao, never()).checkIdDuplicate(anyString());
    }
    
    /**
     * 회원가입 - 성공 케이스
     */
    @Test
    void testSignup_Success() {
        // Given
        String encodedPassword = "$2a$10$encodedPasswordHash";
        when(loginDao.checkIdDuplicate("testuser")).thenReturn(0);
        when(loginDao.checkEmailDuplicate("test@example.com")).thenReturn(0);
        when(passwordEncoder.encode("Test123!@#")).thenReturn(encodedPassword);
        when(loginDao.insertUser(any(Map.class))).thenAnswer(invocation -> {
            Map<String, Object> userData = invocation.getArgument(0);
            userData.put("usrId", 1L); // MyBatis useGeneratedKeys 시뮬레이션
            return 1;
        });
        when(loginDao.insertUserStats(any(Map.class))).thenReturn(1);
        when(messageUtil.getMessage(anyString())).thenReturn("회원가입이 완료되었습니다!");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.signup(validSignupData);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("회원가입이 완료되었습니다!", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("testuser", response.getData().get("usrLoginId"));
        assertEquals("test@example.com", response.getData().get("email"));
        
        verify(loginDao).checkIdDuplicate("testuser");
        verify(loginDao).checkEmailDuplicate("test@example.com");
        verify(passwordEncoder).encode("Test123!@#");
        verify(loginDao).insertUser(any(Map.class));
        verify(loginDao).insertUserStats(any(Map.class));
    }
    
    /**
     * 회원가입 - 아이디 중복으로 실패
     */
    @Test
    void testSignup_DuplicateId() {
        // Given
        when(loginDao.checkIdDuplicate("testuser")).thenReturn(1);
        when(messageUtil.getMessage(anyString())).thenReturn("이미 사용 중인 아이디입니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.signup(validSignupData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("이미 사용 중인 아이디입니다.", response.getMessage());
        assertEquals("USER_003", response.getErrorCode());
        
        verify(loginDao).checkIdDuplicate("testuser");
        verify(loginDao, never()).checkEmailDuplicate(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(loginDao, never()).insertUser(any(Map.class));
    }
    
    /**
     * 회원가입 - 이메일 중복으로 실패
     */
    @Test
    void testSignup_DuplicateEmail() {
        // Given
        when(loginDao.checkIdDuplicate("testuser")).thenReturn(0);
        when(loginDao.checkEmailDuplicate("test@example.com")).thenReturn(1);
        when(messageUtil.getMessage(anyString())).thenReturn("이미 사용 중인 이메일입니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.signup(validSignupData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("이미 사용 중인 이메일입니다.", response.getMessage());
        assertEquals("USER_004", response.getErrorCode());
        
        verify(loginDao).checkIdDuplicate("testuser");
        verify(loginDao).checkEmailDuplicate("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(loginDao, never()).insertUser(any(Map.class));
    }
    
    /**
     * 회원가입 - 비밀번호 형식 오류
     */
    @Test
    void testSignup_InvalidPassword() {
        // Given
        Map<String, Object> invalidData = new HashMap<>(validSignupData);
        invalidData.put("password", "weak"); // 약한 비밀번호
        when(messageUtil.getMessage(anyString())).thenReturn("비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.signup(invalidData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.", response.getMessage());
        assertEquals("PWD_002", response.getErrorCode());
        
        verify(loginDao, never()).checkIdDuplicate(anyString());
        verify(loginDao, never()).checkEmailDuplicate(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(loginDao, never()).insertUser(any(Map.class));
    }
    
    /**
     * 사용자 정보 조회 - 성공
     */
    @Test
    void testGetUserInfo_Success() {
        // Given
        Long usrId = 1L;
        when(loginDao.findById(usrId)).thenReturn(mockUser);
        when(messageUtil.getMessage(anyString())).thenReturn("사용자 정보를 조회했습니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.getUserInfo(usrId);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("사용자 정보를 조회했습니다.", response.getMessage());
        assertNotNull(response.getData());
        
        Map<String, Object> userInfo = response.getData();
        assertEquals(1L, userInfo.get("usrId"));
        assertEquals("testuser", userInfo.get("usrLoginId"));
        assertEquals("테스트사용자", userInfo.get("usrNm"));
        assertEquals("test@example.com", userInfo.get("email"));
        assertEquals("02", userInfo.get("usrTpCd"));
        assertEquals("010-1234-5678", userInfo.get("phoneNum"));
        assertEquals(true, userInfo.get("isUse"));
        
        // 비밀번호는 포함되지 않아야 함 (보안)
        assertFalse(userInfo.containsKey("pwd"));
        
        verify(loginDao).findById(usrId);
    }
    
    /**
     * 사용자 정보 조회 - 사용자 없음
     */
    @Test
    void testGetUserInfo_NotFound() {
        // Given
        Long usrId = 999L;
        when(loginDao.findById(usrId)).thenReturn(null);
        when(messageUtil.getMessage(anyString())).thenReturn("사용자 정보를 찾을 수 없습니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.getUserInfo(usrId);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("사용자 정보를 찾을 수 없습니다.", response.getMessage());
        assertEquals("USER_001", response.getErrorCode());
        
        verify(loginDao).findById(usrId);
    }
    
    /**
     * 회원가입 - 필수 필드 누락
     */
    @Test
    void testSignup_MissingRequiredFields() {
        // Given
        Map<String, Object> incompleteData = new HashMap<>();
        incompleteData.put("usrLoginId", "testuser");
        // usrNm, email, password 누락
        
        // When
        ApiResponse<Map<String, Object>> response = userService.signup(incompleteData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("VAL_001", response.getErrorCode());
        
        verify(loginDao, never()).checkIdDuplicate(anyString());
        verify(loginDao, never()).checkEmailDuplicate(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(loginDao, never()).insertUser(any(Map.class));
    }
    
    /**
     * 회원가입 - 이메일 형식 오류
     */
    @Test
    void testSignup_InvalidEmailFormat() {
        // Given
        Map<String, Object> invalidEmailData = new HashMap<>(validSignupData);
        invalidEmailData.put("email", "invalid-email");
        when(messageUtil.getMessage(anyString())).thenReturn("올바른 이메일 형식이 아닙니다.");
        
        // When
        ApiResponse<Map<String, Object>> response = userService.signup(invalidEmailData);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("올바른 이메일 형식이 아닙니다.", response.getMessage());
        assertEquals("VAL_002", response.getErrorCode());
        
        verify(loginDao, never()).checkIdDuplicate(anyString());
        verify(loginDao, never()).checkEmailDuplicate(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(loginDao, never()).insertUser(any(Map.class));
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
