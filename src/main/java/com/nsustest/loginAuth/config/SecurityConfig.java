package com.nsustest.loginAuth.config;

import com.nsustest.loginAuth.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스
 * 
 * @author nsustest
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈 등록
     * 
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * SecurityFilterChain 설정
     * 
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain
     * @throws Exception 설정 오류 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화
            .csrf(csrf -> csrf.disable())
            
            // 세션 정책 설정
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 요청별 접근 권한 설정
            .authorizeHttpRequests(authz -> authz
                // 공개 경로 (인증 불필요)
                .requestMatchers("/api/login", "/api/signup", "/api/check-id", "/api/refresh").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/", "/index.html", "/login.html", "/signup.html", "/dashboard.html").permitAll()
                // 관리자 전용 경로
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 기타 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // JWT 인증 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 기본 로그인 페이지 비활성화
            .formLogin(form -> form.disable())
            
            // 기본 로그아웃 페이지 비활성화
            .logout(logout -> logout.disable())
            
            // HTTP Basic 인증 비활성화
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
