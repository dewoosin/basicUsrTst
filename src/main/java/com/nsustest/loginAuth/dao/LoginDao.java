package com.nsustest.loginAuth.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 로그인 관련 데이터베이스 접근을 위한 DAO 인터페이스
 * 최적화된 스키마에 맞춰 업데이트
 * 
 * @author nsustest
 */
@Mapper
public interface LoginDao {
    
    // ==================== 사용자 관련 ====================
    
    /**
     * 로그인 아이디로 사용자 존재 여부 확인
     * 
     * @param usrLoginId 확인할 로그인 아이디
     * @return 해당 아이디를 가진 사용자 수 (0 또는 1)
     */
    int checkIdDuplicate(String usrLoginId);
    
    /**
     * 이메일로 사용자 존재 여부 확인
     * 
     * @param email 확인할 이메일
     * @return 해당 이메일을 가진 사용자 수 (0 또는 1)
     */
    int checkEmailDuplicate(String email);
    
    /**
     * 사용자 정보 저장
     * 
     * @param userData 저장할 사용자 정보
     * @return 저장된 행 수
     */
    int insertUser(Map<String, Object> userData);
    
    /**
     * 로그인 아이디로 사용자 정보 조회
     * 
     * @param usrLoginId 조회할 로그인 아이디
     * @return 사용자 정보 (없으면 null)
     */
    Map<String, Object> findByLoginId(String usrLoginId);
    
    /**
     * 사용자 ID로 사용자 정보 조회
     * 
     * @param usrId 사용자 ID
     * @return 사용자 정보 (없으면 null)
     */
    Map<String, Object> findById(Long usrId);
    
    // ==================== 사용자 통계 관련 ====================
    
    /**
     * 사용자 통계 정보 초기화 (회원가입 시)
     * 
     * @param userStats 사용자 통계 정보
     * @return 저장된 행 수
     */
    int insertUserStats(Map<String, Object> userStats);
    
    /**
     * 사용자 통계 정보 업데이트 (로그인 성공 시)
     * 
     * @param statsData 업데이트할 통계 데이터
     * @return 업데이트된 행 수
     */
    int updateUserStats(Map<String, Object> statsData);
    
    // ==================== 세션 관련 ====================
    
    /**
     * 사용자 세션 저장 (로그인 시)
     * 
     * @param sessionData 세션 데이터
     * @return 저장된 행 수
     */
    int insertUserSession(Map<String, Object> sessionData);
    
    /**
     * 기존 활성 세션 삭제 (다중 로그인 방지)
     * 
     * @param usrId 사용자 ID
     * @return 삭제된 행 수
     */
    int deleteActiveSessions(Long usrId);
    
    /**
     * 세션 로그아웃 처리 (로그아웃 시)
     * 
     * @param logoutData 로그아웃 데이터 (usrId 포함)
     * @return 업데이트된 행 수
     */
    int updateSessionLogout(Map<String, Object> logoutData);
    
    /**
     * Refresh Token으로 세션 조회
     * 
     * @param refreshToken 조회할 Refresh Token
     * @return 세션 정보 (없으면 null)
     */
    Map<String, Object> findSessionByRefreshToken(String refreshToken);
    
    
    // ==================== 로그인 이력 관련 ====================
    
    /**
     * 로그인 이력 저장 (성공/실패 통합)
     * 
     * @param historyData 로그인 이력 데이터
     * @return 저장된 행 수
     */
    int insertLoginHistory(Map<String, Object> historyData);
    
    /**
     * IP별 로그인 시도 횟수 조회
     * 
     * @param ipAddr IP 주소
     * @return 시도 횟수
     */
    int getLoginAttemptCount(String ipAddr);
    
    /**
     * IP 차단 상태 확인
     * 
     * @param ipAddr IP 주소
     * @return 차단 정보 (차단되지 않았으면 null)
     */
    Map<String, Object> checkIpBlocked(String ipAddr);
    
    /**
     * 로그인 성공 시 IP 차단 해제
     * 
     * @param ipAddr IP 주소
     * @return 업데이트된 행 수
     */
    int resetIpBlock(String ipAddr);
    
    // ==================== 정리 작업 ====================
    
    /**
     * 만료된 세션 정리
     * 
     * @return 삭제된 행 수
     */
    int cleanupExpiredSessions();
    
    /**
     * 오래된 로그인 이력 삭제
     * 
     * @return 삭제된 행 수
     */
    int cleanupOldLoginHistory();
    
    // ==================== Rate-Limiting 관련 ====================
    
    /**
     * Rate-Limit 초과 이력 저장
     * 
     * @param rateLimitData Rate-Limit 초과 데이터
     * @return 저장된 행 수
     */
    int insertRateLimitHistory(Map<String, Object> rateLimitData);
    
    /**
     * IP별 Rate-Limit 통계 조회
     * 
     * @param ipAddr IP 주소
     * @return Rate-Limit 통계 정보
     */
    Map<String, Object> getRateLimitStats(String ipAddr);
    
    /**
     * 오래된 Rate-Limit 이력 삭제
     * 
     * @return 삭제된 행 수
     */
    int cleanupOldRateLimitHistory();
}
