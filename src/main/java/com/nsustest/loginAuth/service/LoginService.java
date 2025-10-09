package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 로그인 관련 Facade 서비스 클래스
 * 
 * Controller와 세부 서비스 계층 사이의 Facade 역할을 수행합니다.
 * MVC 패턴의 일관성을 유지하기 위해 Controller는 이 서비스만 의존합니다.
 * 
 * 역할 분리:
 * - LoginController: HTTP 요청 처리 및 응답 반환
 * - LoginService (Facade): 비즈니스 로직 조율 및 세부 서비스 호출
 * - AuthService: 로그인, 토큰 관리
 * - UserService: 회원가입, 사용자 정보 관리
 * - SessionService: 세션 관리, 로그아웃
 * 
 * @author nsustest
 * @version 2.0
 * @since 2024-01-01
 */
@Service
public class LoginService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SessionService sessionService;
    
    /**
     * 아이디 중복 확인
     * 
     * UserService에 위임하여 처리합니다.
     * 
     * @param usrLoginId 확인할 로그인 아이디 (4-20자 영문, 숫자)
     * @return ApiResponse<Map<String, Object>> 중복 확인 결과
     *         - success: true/false
     *         - data: {"duplicate": boolean, "usrLoginId": string}
     *         - message: 결과 메시지
     */
    public ApiResponse<Map<String, Object>> checkIdDuplicate(String usrLoginId) {
        logger.debug("아이디 중복 확인 요청: {}", usrLoginId);
        return userService.checkIdDuplicate(usrLoginId);
    }
    
    /**
     * 회원가입 처리
     * 
     * UserService에 위임하여 처리합니다.
     * 새로운 사용자 계정을 생성합니다.
     * 
     * @param signupData 회원가입 데이터 Map
     *                   - usrLoginId: 로그인 아이디 (필수)
     *                   - usrNm: 사용자 이름 (필수)
     *                   - email: 이메일 주소 (필수)
     *                   - password: 비밀번호 (필수)
     *                   - phoneNum: 전화번호 (선택)
     * @return ApiResponse<Map<String, Object>> 회원가입 결과
     *         - success: true/false
     *         - data: {"usrLoginId": string, "email": string}
     *         - message: 결과 메시지
     */
    public ApiResponse<Map<String, Object>> signup(Map<String, Object> signupData) {
        logger.debug("회원가입 요청: {}", signupData.get("usrLoginId"));
        return userService.signup(signupData);
    }
    
    /**
     * 로그인 처리
     * 
     * AuthService에 위임하여 처리합니다.
     * 사용자 인증을 수행하고 JWT 토큰을 발급합니다.
     * 
     * @param loginData 로그인 데이터 Map
     *                  - usrLoginId: 로그인 아이디 (필수)
     *                  - password: 비밀번호 (필수)
     *                  - ipAddr: 클라이언트 IP 주소 (선택)
     *                  - userAgent: 사용자 에이전트 (선택)
     * @return ApiResponse<Map<String, Object>> 로그인 결과
     *         - success: true/false
     *         - data: {"accessToken": string, "refreshToken": string, "tokenType": "Bearer", 
     *                 "expiresIn": number, "user": object}
     *         - message: 결과 메시지
     */
    public ApiResponse<Map<String, Object>> login(Map<String, Object> loginData) {
        logger.debug("로그인 요청: {}", loginData.get("usrLoginId"));
        return authService.login(loginData);
    }
    
    /**
     * Refresh Token으로 Access Token 재발급
     * 
     * AuthService에 위임하여 처리합니다.
     * Refresh Token을 검증하여 새로운 Access Token과 Refresh Token을 발급합니다.
     * 
     * @param refreshToken 검증할 Refresh Token
     * @return ApiResponse<Map<String, Object>> 토큰 재발급 결과
     *         - success: true/false
     *         - data: {"accessToken": string, "refreshToken": string, "tokenType": "Bearer", "expiresIn": number}
     *         - message: 결과 메시지
     */
    public ApiResponse<Map<String, Object>> refreshAccessToken(String refreshToken) {
        logger.debug("토큰 재발급 요청");
        return authService.refreshAccessToken(refreshToken);
    }
    
    /**
     * 로그아웃 처리
     * 
     * SessionService에 위임하여 처리합니다.
     * 사용자의 활성 세션을 비활성화하고 로그아웃 시간을 기록합니다.
     * 
     * @param usrId 로그아웃할 사용자 ID
     * @return ApiResponse<Object> 로그아웃 결과
     *         - success: true/false
     *         - message: 결과 메시지
     */
    public ApiResponse<Object> logout(Long usrId) {
        logger.debug("로그아웃 요청: usrId={}", usrId);
        return sessionService.logout(usrId);
    }
    
    /**
     * 사용자 정보 조회
     * 
     * UserService에 위임하여 처리합니다.
     * 사용자 ID로 사용자 정보를 조회합니다. 보안을 위해 비밀번호는 제외하고 반환합니다.
     * 
     * @param usrId 조회할 사용자 ID
     * @return ApiResponse<Map<String, Object>> 사용자 정보 조회 결과
     *         - success: true/false
     *         - data: {"usrId": number, "usrLoginId": string, "usrNm": string, 
     *                 "email": string, "usrTpCd": string, "phoneNum": string, 
     *                 "isUse": boolean, "creDt": date, "updDt": date}
     *         - message: 결과 메시지
     */
    public ApiResponse<Map<String, Object>> getUserInfo(Long usrId) {
        logger.debug("사용자 정보 조회 요청: usrId={}", usrId);
        return userService.getUserInfo(usrId);
    }
}
