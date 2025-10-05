package com.nsustest.loginAuth.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SecurityContext에서 사용자 정보를 추출하는 유틸리티 클래스
 * 
 * @author nsustest
 */
@Component
public class SecurityContextUtil {
    
    /**
     * 현재 인증된 사용자의 ID를 반환
     * 
     * @return 사용자 ID (인증되지 않은 경우 null)
     */
    public Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            // 인증 객체의 details에서 사용자 정보 추출
            Object details = authentication.getDetails();
            if (details instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userDetailsMap = (Map<String, Object>) details;
                Object usrId = userDetailsMap.get("usrId");
                if (usrId instanceof Number) {
                    return ((Number) usrId).longValue();
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 현재 인증된 사용자의 로그인 ID를 반환
     * 
     * @return 사용자 로그인 ID (인증되지 않은 경우 null)
     */
    public String getCurrentUserLoginId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            // 인증 객체의 details에서 사용자 정보 추출
            Object details = authentication.getDetails();
            if (details instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userDetailsMap = (Map<String, Object>) details;
                return (String) userDetailsMap.get("usrLoginId");
            }
            
            // fallback: principal에서 username 추출
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                return userDetails.getUsername();
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 현재 인증된 사용자의 권한을 반환
     * 
     * @return 사용자 권한 (인증되지 않은 경우 null)
     */
    public String getCurrentUserAuthority() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            return authentication.getAuthorities().iterator().next().getAuthority();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 현재 인증된 사용자가 관리자인지 확인
     * 
     * @return 관리자 여부
     */
    public boolean isAdmin() {
        String authority = getCurrentUserAuthority();
        return "ADMIN".equals(authority);
    }
    
    /**
     * 현재 인증된 사용자가 인증되었는지 확인
     * 
     * @return 인증 여부
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }
    
}
