package com.nsustest.loginAuth.controller;

import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.service.RateLimitService;
import com.nsustest.loginAuth.util.IpAddressUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Rate-Limiting 관리 컨트롤러
 * 관리자용 Rate-Limiting 통계 조회 및 캐시 관리
 * 
 * @author nsustest
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class RateLimitController {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    // Rate-Limiting 설정값 주입
    @Value("${rate.limit.requests.per.minute:60}")
    private int maxRequestsPerMinute;
    
    @Value("${rate.limit.requests.per.hour:1000}")
    private int maxRequestsPerHour;
    
    @Value("${rate.limit.requests.per.day:10000}")
    private int maxRequestsPerDay;
    
    @Value("${rate.limit.login.attempts.per.minute:5}")
    private int maxLoginAttemptsPerMinute;
    
    @Value("${rate.limit.login.attempts.per.hour:20}")
    private int maxLoginAttemptsPerHour;
    
    /**
     * Rate-Limiting 통계 조회
     * 
     * @param ipAddr IP 주소 (선택사항)
     * @param request HTTP 요청 (IP 추출용)
     * @return Rate-Limiting 통계 정보
     */
    @GetMapping("/rate-limit/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimitStats(
            @RequestParam(value = "ip", required = false) String ipAddr,
            HttpServletRequest request) {
        
        try {
            // IP 주소가 제공되지 않으면 요청 IP 사용
            if (ipAddr == null || ipAddr.trim().isEmpty()) {
                ipAddr = IpAddressUtil.getClientIpAddress(request);
            }
            
            Map<String, Object> stats = rateLimitService.getRateLimitStats(ipAddr);
            
            return ResponseEntity.ok(ApiResponse.success("Rate-Limiting 통계를 조회했습니다.", stats));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Rate-Limiting 통계 조회 중 오류가 발생했습니다.", "RATE_LIMIT_STATS_ERROR")
            );
        }
    }
    
    /**
     * Rate-Limiting 캐시 초기화
     * 
     * @param ipAddr IP 주소 (선택사항, null이면 전체 초기화)
     * @param request HTTP 요청
     * @return 초기화 결과
     */
    @PostMapping("/rate-limit/clear-cache")
    public ResponseEntity<ApiResponse<Object>> clearRateLimitCache(
            @RequestParam(value = "ip", required = false) String ipAddr,
            HttpServletRequest request) {
        
        try {
            // IP 주소가 제공되지 않으면 요청 IP 사용
            if (ipAddr == null || ipAddr.trim().isEmpty()) {
                ipAddr = IpAddressUtil.getClientIpAddress(request);
            }
            
            rateLimitService.clearRateLimitCache(ipAddr);
            
            String message = ipAddr != null ? 
                "IP " + ipAddr + "의 Rate-Limiting 캐시가 초기화되었습니다." :
                "전체 Rate-Limiting 캐시가 초기화되었습니다.";
            
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Rate-Limiting 캐시 초기화 중 오류가 발생했습니다.", "RATE_LIMIT_CLEAR_ERROR")
            );
        }
    }
    
    /**
     * Rate-Limiting 상태 확인
     * 
     * @param request HTTP 요청
     * @return Rate-Limiting 상태 정보
     */
    @GetMapping("/rate-limit/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimitStatus(HttpServletRequest request) {
        
        try {
            String clientIp = IpAddressUtil.getClientIpAddress(request);
            Map<String, Object> stats = rateLimitService.getRateLimitStats(clientIp);
            
            // Rate-Limiting 설정 정보 추가
            Map<String, Object> status = Map.of(
                "clientIp", clientIp,
                "currentStats", stats,
                "limits", Map.of(
                    "maxRequestsPerMinute", maxRequestsPerMinute,
                    "maxRequestsPerHour", maxRequestsPerHour,
                    "maxRequestsPerDay", maxRequestsPerDay,
                    "maxLoginAttemptsPerMinute", maxLoginAttemptsPerMinute,
                    "maxLoginAttemptsPerHour", maxLoginAttemptsPerHour
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success("Rate-Limiting 상태를 조회했습니다.", status));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Rate-Limiting 상태 조회 중 오류가 발생했습니다.", "RATE_LIMIT_STATUS_ERROR")
            );
        }
    }
    
}
