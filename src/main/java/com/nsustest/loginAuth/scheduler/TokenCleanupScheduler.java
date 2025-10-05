package com.nsustest.loginAuth.scheduler;

import com.nsustest.loginAuth.dao.LoginDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 만료된 세션과 오래된 로그인 이력을 정리하는 스케줄러
 * 최적화된 스키마에 맞춰 업데이트
 * 
 * @author nsustest
 */
@Component
public class TokenCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupScheduler.class);
    
    @Autowired
    private LoginDao loginDao;
    
    /**
     * 만료된 세션 정리
     * 매일 새벽 2시에 실행
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredSessions() {
        try {
            logger.info("=== 만료된 세션 정리 시작 ===");
            
            int deletedCount = loginDao.cleanupExpiredSessions();
            
            logger.info("만료된 세션 정리 완료 - 삭제된 세션 수: {}", deletedCount);
            
        } catch (Exception e) {
            logger.error("만료된 세션 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 오래된 로그인 이력 정리
     * 매주 일요일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupOldLoginHistory() {
        try {
            logger.info("=== 오래된 로그인 이력 정리 시작 ===");
            
            int deletedCount = loginDao.cleanupOldLoginHistory();
            
            logger.info("오래된 로그인 이력 정리 완료 - 삭제된 이력 수: {}", deletedCount);
            
        } catch (Exception e) {
            logger.error("오래된 로그인 이력 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 오래된 Rate-Limit 이력 정리
     * 매주 일요일 새벽 4시에 실행
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    public void cleanupOldRateLimitHistory() {
        try {
            logger.info("=== 오래된 Rate-Limit 이력 정리 시작 ===");
            
            int deletedCount = loginDao.cleanupOldRateLimitHistory();
            
            logger.info("오래된 Rate-Limit 이력 정리 완료 - 삭제된 이력 수: {}", deletedCount);
            
        } catch (Exception e) {
            logger.error("오래된 Rate-Limit 이력 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
