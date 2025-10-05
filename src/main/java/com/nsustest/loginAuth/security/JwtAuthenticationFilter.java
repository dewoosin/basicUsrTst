package com.nsustest.loginAuth.security;

import com.nsustest.loginAuth.util.IpAddressUtil;
import com.nsustest.loginAuth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 토큰을 통한 인증을 처리하는 필터
 * 
 * @author nsustest
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Authorization 헤더에서 JWT 토큰 추출
            String authorizationHeader = request.getHeader("Authorization");
            
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7); // "Bearer " 제거
                
                // 토큰 검증
                if (jwtUtil.validateToken(token)) {
                    // 토큰에서 사용자 정보 추출
                    Map<String, Object> userInfo = jwtUtil.getUserInfoFromToken(token);
                    
                    if (userInfo != null) {
                        // Spring Security 인증 객체 생성
                        UserDetails userDetails = createUserDetails(userInfo);
                        
                        // 사용자 정보를 포함한 인증 객체 생성
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                userDetails.getAuthorities()
                            );
                        
                        // 사용자 정보를 인증 객체의 details에 저장
                        Map<String, Object> userDetailsMap = new HashMap<>();
                        userDetailsMap.put("usrId", userInfo.get("usrId"));
                        userDetailsMap.put("usrLoginId", userInfo.get("usrLoginId"));
                        userDetailsMap.put("usrNm", userInfo.get("usrNm"));
                        userDetailsMap.put("email", userInfo.get("email"));
                        authentication.setDetails(userDetailsMap);
                        
                        // SecurityContext에 인증 정보 설정
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // 보안 로깅
                        logSecurityEvent("JWT_AUTH_SUCCESS", userInfo.get("usrId"), request);
                    }
                } else {
                    // 토큰 검증 실패 로깅
                    logSecurityEvent("JWT_AUTH_FAILED", null, request);
                }
            }
            
        } catch (Exception e) {
            // 예외 발생 시 보안 로깅
            logSecurityEvent("JWT_AUTH_ERROR", null, request);
            logger.error("JWT 인증 처리 중 오류 발생", e);
        }
        
        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
    
    /**
     * 토큰에서 추출한 사용자 정보로 UserDetails 객체 생성
     * 
     * @param userInfo 토큰에서 추출한 사용자 정보
     * @return UserDetails 객체
     */
    private UserDetails createUserDetails(Map<String, Object> userInfo) {
        String username = (String) userInfo.get("usrLoginId");
        String userType = (String) userInfo.get("usrTpCd");
        
        // 사용자 타입에 따른 권한 설정
        String authority = "USER";
        if ("01".equals(userType)) {
            authority = "ADMIN";
        }
        
        // 사용자 정보를 UserDetails에 추가로 저장
        User.UserBuilder builder = User.builder()
                .username(username)
                .password("") // JWT에서는 비밀번호 불필요
                .authorities(authority);
        
        // 사용자 ID를 추가 속성으로 저장
        Object usrId = userInfo.get("usrId");
        if (usrId != null) {
            builder.disabled(false);
        }
        
        return builder.build();
    }
    
    /**
     * 보안 이벤트 로깅
     * 
     * @param eventType 이벤트 타입
     * @param userId 사용자 ID
     * @param request HTTP 요청
     */
    private void logSecurityEvent(String eventType, Object userId, HttpServletRequest request) {
        String ipAddress = IpAddressUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        logger.info("Security Event - Type: " + eventType + ", UserId: " + userId + 
                   ", IP: " + ipAddress + ", UserAgent: " + userAgent);
    }
    
    
    /**
     * 특정 경로는 JWT 인증을 건너뛰도록 설정
     * 
     * @param request HTTP 요청
     * @return 인증을 건너뛸지 여부
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 공개 경로들은 JWT 인증을 건너뛰기
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/signup") ||
               path.startsWith("/api/check-id") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.equals("/") ||
               path.equals("/favicon.ico");
    }
}
