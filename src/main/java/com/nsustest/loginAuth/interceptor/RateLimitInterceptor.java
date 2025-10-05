package com.nsustest.loginAuth.interceptor;

import com.nsustest.loginAuth.service.RateLimitService;
import com.nsustest.loginAuth.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Rate-Limiting을 위한 인터셉터
 * IP 단위로 요청 횟수를 제한합니다.
 * 
 * @author nsustest
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * 요청 처리 전 Rate-Limiting 검사
     * 
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param handler 핸들러 객체
     * @return true: 요청 허용, false: 요청 차단
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = IpAddressUtil.getClientIpAddress(request);
        String requestPath = request.getRequestURI();
        
        // Rate-Limiting 검사
        boolean isAllowed = rateLimitService.checkRateLimit(clientIp, requestPath);
        
        if (!isAllowed) {
            // Rate-Limit 초과 시 429 Too Many Requests 응답
            sendRateLimitExceededResponse(response);
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Rate-Limit 초과 응답 전송
     * 
     * @param response HTTP 응답
     * @throws IOException IO 예외
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        
        // 간단한 JSON 응답 생성
        String jsonResponse = "{\"success\":false,\"message\":\"요청 횟수가 초과되었습니다. 잠시 후 다시 시도해주세요.\",\"errorCode\":\"SEC_001\"}";
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
