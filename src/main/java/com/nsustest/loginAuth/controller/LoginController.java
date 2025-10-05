package com.nsustest.loginAuth.controller;

import com.nsustest.loginAuth.constants.ErrorCode;
import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.service.LoginService;
import com.nsustest.loginAuth.util.IpAddressUtil;
import com.nsustest.loginAuth.util.JwtUtil;
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
    private LoginService loginService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 아이디 중복 확인 API
     * 
     * @param usrLoginId 확인할 로그인 아이디
     * @return 중복 확인 결과
     */
    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIdDuplicate(@RequestParam("usrLoginId") String usrLoginId) {
        ApiResponse<Map<String, Object>> response = loginService.checkIdDuplicate(usrLoginId);
        
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
        ApiResponse<Map<String, Object>> response = loginService.signup(signupData);
        
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
        
        ApiResponse<Map<String, Object>> response = loginService.login(loginData);
        
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
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, Object> request) {
        String refreshToken = (String) request.get("refreshToken");
        
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Refresh Token이 필요합니다."
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        Map<String, Object> response = loginService.refreshAccessToken(refreshToken);
        
        if ((Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 로그아웃 API
     * 
     * @param authorization Authorization 헤더 (Bearer Token)
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authorization) {
        try {
            // Authorization 헤더에서 토큰 추출
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "인증 토큰이 필요합니다."
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String token = authorization.substring(7); // "Bearer " 제거
            
            // JWT 토큰 검증
            if (!jwtUtil.validateToken(token)) {
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "유효하지 않은 토큰입니다."
                );
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            // 토큰에서 사용자 ID 추출
            Long usrId = jwtUtil.getUserIdFromToken(token);
            if (usrId == null) {
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "사용자 정보를 찾을 수 없습니다."
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> response = loginService.logout(usrId);
            
            if ((Boolean) response.get("success")) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "서버 오류가 발생했습니다."
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 사용자 정보 조회 API
     * 
     * @param authorization Authorization 헤더 (Bearer Token)
     * @return 사용자 정보
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Object>> getUserInfo(@RequestHeader("Authorization") String authorization) {
        try {
            // Authorization 헤더에서 토큰 추출
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                ApiResponse<Object> errorResponse = ApiResponse.error(
                    ErrorCode.getDescription(ErrorCode.AUTH_UNAUTHORIZED), 
                    ErrorCode.AUTH_UNAUTHORIZED
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String token = authorization.substring(7); // "Bearer " 제거
            
            // JWT 토큰 검증
            if (!jwtUtil.validateToken(token)) {
                ApiResponse<Object> errorResponse = ApiResponse.error(
                    ErrorCode.getDescription(ErrorCode.AUTH_TOKEN_INVALID), 
                    ErrorCode.AUTH_TOKEN_INVALID
                );
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            // 토큰에서 사용자 ID 추출
            Long usrId = jwtUtil.getUserIdFromToken(token);
            if (usrId == null) {
                ApiResponse<Object> errorResponse = ApiResponse.error(
                    ErrorCode.getDescription(ErrorCode.USER_NOT_FOUND), 
                    ErrorCode.USER_NOT_FOUND
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 사용자 정보 조회
            Map<String, Object> response = loginService.getUserInfo(usrId);
            
            if ((Boolean) response.get("success")) {
                ApiResponse<Object> apiResponse = ApiResponse.success("사용자 정보를 조회했습니다.", response.get("user"));
                return ResponseEntity.ok(apiResponse);
            } else {
                ApiResponse<Object> apiResponse = ApiResponse.error((String) response.get("message"), ErrorCode.USER_NOT_FOUND);
                return ResponseEntity.badRequest().body(apiResponse);
            }
            
        } catch (Exception e) {
            ApiResponse<Object> errorResponse = ApiResponse.error(
                ErrorCode.getDescription(ErrorCode.SERVER_INTERNAL_ERROR), 
                ErrorCode.SERVER_INTERNAL_ERROR
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
