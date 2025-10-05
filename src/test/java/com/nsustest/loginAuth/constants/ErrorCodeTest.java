package com.nsustest.loginAuth.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode 클래스의 단위 테스트
 * 
 * @author nsustest
 */
public class ErrorCodeTest {
    
    /**
     * 인증 관련 에러 코드 테스트
     */
    @Test
    void testAuthErrorCodes() {
        // Given & When & Then
        assertEquals("AUTH_001", ErrorCode.AUTH_INVALID_CREDENTIALS);
        assertEquals("AUTH_002", ErrorCode.AUTH_ACCOUNT_LOCKED);
        assertEquals("AUTH_003", ErrorCode.AUTH_ACCOUNT_DISABLED);
        assertEquals("AUTH_004", ErrorCode.AUTH_TOKEN_EXPIRED);
        assertEquals("AUTH_005", ErrorCode.AUTH_TOKEN_INVALID);
        assertEquals("AUTH_006", ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        assertEquals("AUTH_007", ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        assertEquals("AUTH_008", ErrorCode.AUTH_UNAUTHORIZED);
    }
    
    /**
     * 사용자 관련 에러 코드 테스트
     */
    @Test
    void testUserErrorCodes() {
        // Given & When & Then
        assertEquals("USER_001", ErrorCode.USER_NOT_FOUND);
        assertEquals("USER_002", ErrorCode.USER_ALREADY_EXISTS);
        assertEquals("USER_003", ErrorCode.USER_ID_DUPLICATE);
        assertEquals("USER_004", ErrorCode.USER_EMAIL_DUPLICATE);
        assertEquals("USER_005", ErrorCode.USER_INVALID_FORMAT);
    }
    
    /**
     * 비밀번호 관련 에러 코드 테스트
     */
    @Test
    void testPasswordErrorCodes() {
        // Given & When & Then
        assertEquals("PWD_001", ErrorCode.PASSWORD_INVALID_FORMAT);
        assertEquals("PWD_002", ErrorCode.PASSWORD_TOO_WEAK);
        assertEquals("PWD_003", ErrorCode.PASSWORD_MISMATCH);
        assertEquals("PWD_004", ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
    }
    
    /**
     * 서버 관련 에러 코드 테스트
     */
    @Test
    void testServerErrorCodes() {
        // Given & When & Then
        assertEquals("SRV_001", ErrorCode.SERVER_INTERNAL_ERROR);
        assertEquals("SRV_002", ErrorCode.SERVER_DATABASE_ERROR);
        assertEquals("SRV_003", ErrorCode.SERVER_EXTERNAL_SERVICE_ERROR);
    }
    
    /**
     * 보안 관련 에러 코드 테스트
     */
    @Test
    void testSecurityErrorCodes() {
        // Given & When & Then
        assertEquals("SEC_001", ErrorCode.SECURITY_RATE_LIMIT_EXCEEDED);
        assertEquals("SEC_002", ErrorCode.SECURITY_SUSPICIOUS_ACTIVITY);
        assertEquals("SEC_003", ErrorCode.SECURITY_IP_BLOCKED);
    }
    
    /**
     * 에러 코드 설명 테스트 - 인증 관련
     */
    @Test
    void testGetDescriptionAuth() {
        // Given & When & Then
        assertEquals("아이디 또는 비밀번호가 올바르지 않습니다.", 
                    ErrorCode.getDescription(ErrorCode.AUTH_INVALID_CREDENTIALS));
        assertEquals("로그인 실패 횟수가 초과되어 계정이 잠겼습니다.", 
                    ErrorCode.getDescription(ErrorCode.AUTH_ACCOUNT_LOCKED));
        assertEquals("비활성화된 계정입니다.", 
                    ErrorCode.getDescription(ErrorCode.AUTH_ACCOUNT_DISABLED));
        assertEquals("토큰이 만료되었습니다.", 
                    ErrorCode.getDescription(ErrorCode.AUTH_TOKEN_EXPIRED));
        assertEquals("유효하지 않은 토큰입니다.", 
                    ErrorCode.getDescription(ErrorCode.AUTH_TOKEN_INVALID));
    }
    
    /**
     * 에러 코드 설명 테스트 - 사용자 관련
     */
    @Test
    void testGetDescriptionUser() {
        // Given & When & Then
        assertEquals("사용자 정보를 찾을 수 없습니다.", 
                    ErrorCode.getDescription(ErrorCode.USER_NOT_FOUND));
        assertEquals("이미 존재하는 사용자입니다.", 
                    ErrorCode.getDescription(ErrorCode.USER_ALREADY_EXISTS));
        assertEquals("이미 사용 중인 아이디입니다.", 
                    ErrorCode.getDescription(ErrorCode.USER_ID_DUPLICATE));
        assertEquals("이미 사용 중인 이메일입니다.", 
                    ErrorCode.getDescription(ErrorCode.USER_EMAIL_DUPLICATE));
    }
    
    /**
     * 에러 코드 설명 테스트 - 비밀번호 관련
     */
    @Test
    void testGetDescriptionPassword() {
        // Given & When & Then
        assertEquals("비밀번호 형식이 올바르지 않습니다.", 
                    ErrorCode.getDescription(ErrorCode.PASSWORD_INVALID_FORMAT));
        assertEquals("비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.", 
                    ErrorCode.getDescription(ErrorCode.PASSWORD_TOO_WEAK));
        assertEquals("비밀번호가 일치하지 않습니다.", 
                    ErrorCode.getDescription(ErrorCode.PASSWORD_MISMATCH));
        assertEquals("이전에 사용했던 비밀번호는 사용할 수 없습니다.", 
                    ErrorCode.getDescription(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED));
    }
    
    /**
     * 에러 코드 설명 테스트 - 서버 관련
     */
    @Test
    void testGetDescriptionServer() {
        // Given & When & Then
        assertEquals("서버에 일시적인 오류가 발생했습니다.", 
                    ErrorCode.getDescription(ErrorCode.SERVER_INTERNAL_ERROR));
        assertEquals("데이터베이스 처리 중 오류가 발생했습니다.", 
                    ErrorCode.getDescription(ErrorCode.SERVER_DATABASE_ERROR));
        assertEquals("외부 서비스 연동 중 오류가 발생했습니다.", 
                    ErrorCode.getDescription(ErrorCode.SERVER_EXTERNAL_SERVICE_ERROR));
    }
    
    /**
     * 에러 코드 설명 테스트 - 보안 관련
     */
    @Test
    void testGetDescriptionSecurity() {
        // Given & When & Then
        assertEquals("요청 횟수가 초과되었습니다. 잠시 후 다시 시도해주세요.", 
                    ErrorCode.getDescription(ErrorCode.SECURITY_RATE_LIMIT_EXCEEDED));
        assertEquals("의심스러운 활동이 감지되었습니다.", 
                    ErrorCode.getDescription(ErrorCode.SECURITY_SUSPICIOUS_ACTIVITY));
        assertEquals("차단된 IP 주소입니다.", 
                    ErrorCode.getDescription(ErrorCode.SECURITY_IP_BLOCKED));
    }
    
    /**
     * 알 수 없는 에러 코드 설명 테스트
     */
    @Test
    void testGetDescriptionUnknown() {
        // Given & When & Then
        assertEquals("알 수 없는 오류가 발생했습니다.", 
                    ErrorCode.getDescription("UNKNOWN_ERROR_CODE"));
    }
    
    /**
     * null 에러 코드 설명 테스트
     */
    @Test
    void testGetDescriptionNull() {
        // Given & When & Then
        assertEquals("알 수 없는 오류가 발생했습니다.", 
                    ErrorCode.getDescription(null));
    }
    
    /**
     * 빈 문자열 에러 코드 설명 테스트
     */
    @Test
    void testGetDescriptionEmpty() {
        // Given & When & Then
        assertEquals("알 수 없는 오류가 발생했습니다.", 
                    ErrorCode.getDescription(""));
    }
}
