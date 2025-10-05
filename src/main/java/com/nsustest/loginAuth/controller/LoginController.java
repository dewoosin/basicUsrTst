package com.nsustest.loginAuth.controller;

import com.nsustest.loginAuth.constants.ErrorCode;
import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.service.AuthService;
import com.nsustest.loginAuth.service.SessionService;
import com.nsustest.loginAuth.service.UserService;
import com.nsustest.loginAuth.util.IpAddressUtil;
import com.nsustest.loginAuth.util.SecurityContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 로그인 및 회원가입 관련 요청을 처리하는 컨트롤러
 * 
 * @author nsustest
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoginController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private SecurityContextUtil securityContextUtil;
    
    /**
     * 아이디 중복 확인 API
     * 
     * @param usrLoginId 확인할 로그인 아이디
     * @return 중복 확인 결과
     */
    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIdDuplicate(@RequestParam("usrLoginId") String usrLoginId) {
        ApiResponse<Map<String, Object>> response = userService.checkIdDuplicate(usrLoginId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 회원가입 API
     * 
     * @param signupData 회원가입 데이터
     * @return 회원가입 결과
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(@RequestBody Map<String, Object> signupData) {
        ApiResponse<Map<String, Object>> response = userService.signup(signupData);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 로그인 API
     * 
     * @param loginData 로그인 데이터
     * @param request HTTP 요청 (IP 주소 추출용)
     * @return 로그인 결과
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, Object> loginData, HttpServletRequest request) {
        // 클라이언트 IP 주소 추출
        String clientIp = IpAddressUtil.getClientIpAddress(request);
        loginData.put("ipAddr", clientIp);
        loginData.put("userAgent", request.getHeader("User-Agent"));
        
        ApiResponse<Map<String, Object>> response = authService.login(loginData);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    
    /**
     * 토큰 재발급 API
     * 
     * @param request Refresh Token이 포함된 요청
     * @return 새로운 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@RequestBody Map<String, Object> request) {
        String refreshToken = (String) request.get("refreshToken");
        
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.error(
                "Refresh Token이 필요합니다.", 
                ErrorCode.AUTH_TOKEN_INVALID
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        ApiResponse<Map<String, Object>> response = authService.refreshAccessToken(refreshToken);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 로그아웃 API
     * 
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout() {
        try {
            // SecurityContext에서 사용자 ID 추출
            Long usrId = securityContextUtil.getCurrentUserId();
            if (usrId == null) {
                ApiResponse<Object> errorResponse = ApiResponse.error(
                    "인증이 필요합니다.", 
                    ErrorCode.AUTH_UNAUTHORIZED
                );
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            ApiResponse<Object> response = sessionService.logout(usrId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            ApiResponse<Object> errorResponse = ApiResponse.error(
                "서버 오류가 발생했습니다.", 
                ErrorCode.SERVER_INTERNAL_ERROR
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 사용자 정보 조회 API
     * 
     * @return 사용자 정보
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserInfo() {
        try {
            // SecurityContext에서 사용자 ID 추출
            Long usrId = securityContextUtil.getCurrentUserId();
            if (usrId == null) {
                ApiResponse<Map<String, Object>> errorResponse = ApiResponse.error(
                    ErrorCode.getDescription(ErrorCode.AUTH_UNAUTHORIZED), 
                    ErrorCode.AUTH_UNAUTHORIZED
                );
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            // 사용자 정보 조회
            ApiResponse<Map<String, Object>> response = userService.getUserInfo(usrId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.error(
                ErrorCode.getDescription(ErrorCode.SERVER_INTERNAL_ERROR), 
                ErrorCode.SERVER_INTERNAL_ERROR
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
