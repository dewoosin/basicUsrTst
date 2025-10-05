package com.nsustest.loginAuth.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 주소 관련 유틸리티 클래스
 * 클라이언트 IP 주소 추출 로직을 공통화
 * 
 * @author nsustest
 */
public class IpAddressUtil {
    
    /**
     * HTTP 요청에서 클라이언트 IP 주소를 추출
     * 프록시나 로드밸런서를 고려한 IP 추출 로직
     * 
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        // X-Forwarded-For 헤더 확인 (프록시/로드밸런서 환경)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // X-Real-IP 헤더 확인 (Nginx 등)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // 직접 연결된 클라이언트 IP
        String remoteAddr = request.getRemoteAddr();
        
        // IPv6 localhost를 IPv4 localhost로 변환
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        
        return remoteAddr;
    }
}
