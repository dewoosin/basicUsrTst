package com.nsustest.loginAuth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/**
 * JWT 토큰 생성 및 검증을 위한 유틸리티 클래스
 * 표준 JWT 라이브러리(jjwt)를 사용하여 보안을 강화
 * 
 * @author nsustest
 */
@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    
    /**
     * JWT 시크릿 키 생성
     * 보안을 강화하여 Base64 디코딩 및 키 길이 검증
     * 
     * @return HMAC-SHA256용 시크릿 키
     */
    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes;
            
            // Base64 인코딩된 키인지 확인
            if (isBase64Encoded(jwtSecret)) {
                keyBytes = java.util.Base64.getDecoder().decode(jwtSecret);
            } else {
                // 일반 문자열인 경우 UTF-8로 인코딩
                keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            
            // HMAC-SHA256은 최소 256비트(32바이트) 키가 필요
            if (keyBytes.length < 32) {
                // 키가 너무 짧으면 패딩
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            } else if (keyBytes.length > 64) {
                // 키가 너무 길면 해시하여 32바이트로 축소
                java.security.MessageDigest sha256 = java.security.MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(keyBytes);
            }
            
            return Keys.hmacShaKeyFor(keyBytes);
            
        } catch (Exception e) {
            logger.error("JWT 시크릿 키 생성 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("JWT 시크릿 키 생성 실패", e);
        }
    }
    
    /**
     * Base64 인코딩된 문자열인지 확인
     * 
     * @param str 확인할 문자열
     * @return Base64 인코딩 여부
     */
    private boolean isBase64Encoded(String str) {
        try {
            java.util.Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * JWT 토큰 생성
     * 
     * @param user 사용자 정보
     * @param expirationTimeMs 만료 시간 (밀리초)
     * @return 생성된 JWT 토큰
     */
    public String generateToken(Map<String, Object> user, long expirationTimeMs) {
        try {
            Date now = new Date();
            Date expiration = new Date(now.getTime() + expirationTimeMs);
            
            return Jwts.builder()
                    .subject((String) user.get("usr_login_id"))
                    .claim("usrId", user.get("usr_id"))
                    .claim("usrLoginId", user.get("usr_login_id"))
                    .claim("usrNm", user.get("usr_nm"))
                    .claim("email", user.get("email"))
                    .issuedAt(now)
                    .expiration(expiration)
                    .signWith(getSigningKey(), Jwts.SIG.HS256)
                    .compact();
                    
        } catch (Exception e) {
            logger.error("JWT 토큰 생성 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Access Token 생성
     * 
     * @param user 사용자 정보
     * @return 생성된 Access Token
     */
    public String generateAccessToken(Map<String, Object> user) {
        return generateToken(user, accessTokenExpiration);
    }
    
    /**
     * Refresh Token 생성
     * 
     * @param user 사용자 정보
     * @return 생성된 Refresh Token
     */
    public String generateRefreshToken(Map<String, Object> user) {
        return generateToken(user, refreshTokenExpiration);
    }
    
    /**
     * JWT 토큰 검증
     * 
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            
            return true;
            
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("JWT 토큰 검증 중 오류: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * JWT 토큰에서 사용자 ID 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 ID (추출 실패 시 null)
     */
    public Long getUserIdFromToken(String token) {
        try {
            if (!validateToken(token)) {
                return null;
            }
            
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Object usrId = claims.get("usrId");
            if (usrId instanceof Number) {
                return ((Number) usrId).longValue();
            }
            
            return null;
            
        } catch (Exception e) {
            System.out.println("JWT 토큰에서 사용자 ID 추출 중 오류: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JWT 토큰에서 사용자 정보 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 정보 Map (추출 실패 시 null)
     */
    public Map<String, Object> getUserInfoFromToken(String token) {
        try {
            if (!validateToken(token)) {
                return null;
            }
            
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("usrId", claims.get("usrId"));
            userInfo.put("usrLoginId", claims.get("usrLoginId"));
            userInfo.put("usrNm", claims.get("usrNm"));
            userInfo.put("email", claims.get("email"));
            userInfo.put("subject", claims.getSubject());
            userInfo.put("issuedAt", claims.getIssuedAt());
            userInfo.put("expiration", claims.getExpiration());
            
            return userInfo;
            
        } catch (Exception e) {
            System.out.println("JWT 토큰에서 사용자 정보 추출 중 오류: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JWT 토큰에서 만료시간 추출
     * 
     * @param token JWT 토큰
     * @return 만료시간 (Date 객체, 추출 실패 시 null)
     */
    public Date getExpirationFromToken(String token) {
        try {
            if (!validateToken(token)) {
                return null;
            }
            
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration();
            
        } catch (Exception e) {
            System.out.println("JWT 토큰에서 만료시간 추출 중 오류: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JWT 토큰이 만료되었는지 확인
     * 
     * @param token JWT 토큰
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            if (expiration == null) {
                return true;
            }
            
            return expiration.before(new Date());
            
        } catch (Exception e) {
            System.out.println("JWT 토큰 만료 확인 중 오류: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * JWT 토큰에서 subject(사용자 로그인 ID) 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 로그인 ID (추출 실패 시 null)
     */
    public String getSubjectFromToken(String token) {
        try {
            if (!validateToken(token)) {
                return null;
            }
            
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
            
        } catch (Exception e) {
            System.out.println("JWT 토큰에서 subject 추출 중 오류: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Access Token 만료시간 반환 (초 단위)
     * 
     * @return Access Token 만료시간 (초)
     */
    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }
    
    /**
     * Refresh Token 만료시간 반환 (초 단위)
     * 
     * @return Refresh Token 만료시간 (초)
     */
    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpiration / 1000;
    }
}