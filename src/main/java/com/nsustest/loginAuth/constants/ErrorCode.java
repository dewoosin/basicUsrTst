package com.nsustest.loginAuth.constants;

/**
 * 에러 코드 상수 클래스
 * 
 * @author nsustest
 */
public class ErrorCode {
    
    // 인증 관련 에러 코드
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_001";
    public static final String AUTH_ACCOUNT_LOCKED = "AUTH_002";
    public static final String AUTH_ACCOUNT_DISABLED = "AUTH_003";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_004";
    public static final String AUTH_TOKEN_INVALID = "AUTH_005";
    public static final String AUTH_REFRESH_TOKEN_EXPIRED = "AUTH_006";
    public static final String AUTH_REFRESH_TOKEN_INVALID = "AUTH_007";
    public static final String AUTH_UNAUTHORIZED = "AUTH_008";
    
    // 사용자 관련 에러 코드
    public static final String USER_NOT_FOUND = "USER_001";
    public static final String USER_ALREADY_EXISTS = "USER_002";
    public static final String USER_ID_DUPLICATE = "USER_003";
    public static final String USER_EMAIL_DUPLICATE = "USER_004";
    public static final String USER_INVALID_FORMAT = "USER_005";
    
    // 비밀번호 관련 에러 코드
    public static final String PASSWORD_INVALID_FORMAT = "PWD_001";
    public static final String PASSWORD_TOO_WEAK = "PWD_002";
    public static final String PASSWORD_MISMATCH = "PWD_003";
    public static final String PASSWORD_REUSE_NOT_ALLOWED = "PWD_004";
    
    // 유효성 검사 관련 에러 코드
    public static final String VALIDATION_REQUIRED_FIELD = "VAL_001";
    public static final String VALIDATION_INVALID_FORMAT = "VAL_002";
    public static final String VALIDATION_LENGTH_EXCEEDED = "VAL_003";
    
    // 서버 관련 에러 코드
    public static final String SERVER_INTERNAL_ERROR = "SRV_001";
    public static final String SERVER_DATABASE_ERROR = "SRV_002";
    public static final String SERVER_EXTERNAL_SERVICE_ERROR = "SRV_003";
    
    // 보안 관련 에러 코드
    public static final String SECURITY_RATE_LIMIT_EXCEEDED = "SEC_001";
    public static final String SECURITY_SUSPICIOUS_ACTIVITY = "SEC_002";
    public static final String SECURITY_IP_BLOCKED = "SEC_003";
    
    // 세션 관련 에러 코드
    public static final String SESSION_EXPIRED = "SES_001";
    public static final String SESSION_INVALID = "SES_002";
    public static final String SESSION_CONCURRENT_LOGIN = "SES_003";
    
    /**
     * 에러 코드에 대한 설명을 반환
     * 
     * @param errorCode 에러 코드
     * @return 에러 설명
     */
    public static String getDescription(String errorCode) {
        if (errorCode == null || errorCode.trim().isEmpty()) {
            return "알 수 없는 오류가 발생했습니다.";
        }
        
        switch (errorCode) {
            case AUTH_INVALID_CREDENTIALS:
                return "아이디 또는 비밀번호가 올바르지 않습니다.";
            case AUTH_ACCOUNT_LOCKED:
                return "로그인 실패 횟수가 초과되어 계정이 잠겼습니다.";
            case AUTH_ACCOUNT_DISABLED:
                return "비활성화된 계정입니다.";
            case AUTH_TOKEN_EXPIRED:
                return "토큰이 만료되었습니다.";
            case AUTH_TOKEN_INVALID:
                return "유효하지 않은 토큰입니다.";
            case AUTH_REFRESH_TOKEN_EXPIRED:
                return "리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.";
            case AUTH_REFRESH_TOKEN_INVALID:
                return "유효하지 않은 리프레시 토큰입니다.";
            case AUTH_UNAUTHORIZED:
                return "인증이 필요합니다.";
            case USER_NOT_FOUND:
                return "사용자 정보를 찾을 수 없습니다.";
            case USER_ALREADY_EXISTS:
                return "이미 존재하는 사용자입니다.";
            case USER_ID_DUPLICATE:
                return "이미 사용 중인 아이디입니다.";
            case USER_EMAIL_DUPLICATE:
                return "이미 사용 중인 이메일입니다.";
            case USER_INVALID_FORMAT:
                return "사용자 정보 형식이 올바르지 않습니다.";
            case PASSWORD_INVALID_FORMAT:
                return "비밀번호 형식이 올바르지 않습니다.";
            case PASSWORD_TOO_WEAK:
                return "비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.";
            case PASSWORD_MISMATCH:
                return "비밀번호가 일치하지 않습니다.";
            case PASSWORD_REUSE_NOT_ALLOWED:
                return "이전에 사용했던 비밀번호는 사용할 수 없습니다.";
            case VALIDATION_REQUIRED_FIELD:
                return "필수 입력 항목이 누락되었습니다.";
            case VALIDATION_INVALID_FORMAT:
                return "입력 형식이 올바르지 않습니다.";
            case VALIDATION_LENGTH_EXCEEDED:
                return "입력 길이가 허용 범위를 초과했습니다.";
            case SERVER_INTERNAL_ERROR:
                return "서버에 일시적인 오류가 발생했습니다.";
            case SERVER_DATABASE_ERROR:
                return "데이터베이스 처리 중 오류가 발생했습니다.";
            case SERVER_EXTERNAL_SERVICE_ERROR:
                return "외부 서비스 연동 중 오류가 발생했습니다.";
            case SECURITY_RATE_LIMIT_EXCEEDED:
                return "요청 횟수가 초과되었습니다. 잠시 후 다시 시도해주세요.";
            case SECURITY_SUSPICIOUS_ACTIVITY:
                return "의심스러운 활동이 감지되었습니다.";
            case SECURITY_IP_BLOCKED:
                return "차단된 IP 주소입니다.";
            case SESSION_EXPIRED:
                return "세션이 만료되었습니다.";
            case SESSION_INVALID:
                return "유효하지 않은 세션입니다.";
            case SESSION_CONCURRENT_LOGIN:
                return "다른 기기에서 로그인되어 현재 세션이 종료됩니다.";
            default:
                return "알 수 없는 오류가 발생했습니다.";
        }
    }
}
