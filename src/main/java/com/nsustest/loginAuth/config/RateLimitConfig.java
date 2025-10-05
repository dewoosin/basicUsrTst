package com.nsustest.loginAuth.config;

import com.nsustest.loginAuth.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rate-Limiting 인터셉터 설정
 * 
 * @author nsustest
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    /**
     * 인터셉터 등록
     * 
     * @param registry 인터셉터 레지스트리
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**") // API 경로에만 적용
                .excludePathPatterns(
                    "/api/health",           // 헬스체크는 제외
                    "/api/status",           // 상태 확인은 제외
                    "/static/**",            // 정적 리소스는 제외
                    "/css/**",               // CSS 파일은 제외
                    "/js/**",                // JavaScript 파일은 제외
                    "/images/**",            // 이미지 파일은 제외
                    "/favicon.ico"           // 파비콘은 제외
                );
    }
}
