package com.nsustest.loginAuth.util;

import com.nsustest.loginAuth.service.CommonCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 메시지 조회 유틸리티 클래스
 * 
 * Redis 캐시(CommonCodeService)를 통해 메시지 코드에 해당하는 메시지 내용을 조회합니다.
 * 모든 서비스 계층에서 공통으로 사용하는 메시지 조회 기능을 제공합니다.
 * 
 * @author nsustest
 * @version 1.0
 */
@Component
public class MessageUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageUtil.class);
    
    @Autowired
    private CommonCodeService commonCodeService;
    
    /**
     * 메시지 코드로부터 메시지 내용 조회 (Redis 캐시 우선)
     * 
     * CommonCodeService를 통해 Redis 캐시에서 메시지 코드에 해당하는 메시지 내용을 조회합니다.
     * 캐시에서 조회 실패 시 기본 오류 메시지를 반환합니다.
     * 
     * 조회 우선순위:
     * 1. Redis 캐시 (TTL: 30분)
     * 2. 데이터베이스 (msg_cd 테이블)
     * 3. 기본 오류 메시지 (조회 실패 시)
     * 
     * @param msgCd 조회할 메시지 코드 (예: "AUTH_001", "SERVICE_001", "USER_003" 등)
     * @return String 메시지 내용
     *         - 정상: 데이터베이스에 저장된 메시지 내용
     *         - 실패: "시스템 오류가 발생했습니다. 관리자에게 문의하세요."
     */
    public String getMessage(String msgCd) {
        try {
            // Redis 캐시에서 메시지 코드 조회
            Map<String, Object> messageInfo = commonCodeService.getMessageCode(msgCd);
            if (messageInfo != null && messageInfo.get("msg_cont") != null) {
                return (String) messageInfo.get("msg_cont");
            }
        } catch (Exception e) {
            logger.warn("메시지 코드 조회 실패: {}, 기본 메시지 사용", msgCd, e);
        }
        
        // 캐시에서 조회 실패 시 기본 오류 메시지 반환
        logger.error("메시지 코드 '{}'를 Redis 캐시에서 찾을 수 없습니다. 데이터베이스에 메시지가 등록되어 있는지 확인하세요.", msgCd);
        return "시스템 오류가 발생했습니다. 관리자에게 문의하세요.";
    }
    
    /**
     * 메시지 코드로부터 메시지 내용 조회 (기본값 지정 가능)
     * 
     * Redis 캐시에서 메시지를 조회하되, 조회 실패 시 사용자가 지정한 기본값을 반환합니다.
     * 
     * @param msgCd 조회할 메시지 코드
     * @param defaultMessage 조회 실패 시 반환할 기본 메시지
     * @return String 메시지 내용 또는 기본 메시지
     */
    public String getMessage(String msgCd, String defaultMessage) {
        try {
            Map<String, Object> messageInfo = commonCodeService.getMessageCode(msgCd);
            if (messageInfo != null && messageInfo.get("msg_cont") != null) {
                return (String) messageInfo.get("msg_cont");
            }
        } catch (Exception e) {
            logger.warn("메시지 코드 조회 실패: {}, 기본 메시지 사용", msgCd, e);
        }
        
        logger.warn("메시지 코드 '{}'를 찾을 수 없습니다. 기본 메시지 반환: {}", msgCd, defaultMessage);
        return defaultMessage;
    }
}

