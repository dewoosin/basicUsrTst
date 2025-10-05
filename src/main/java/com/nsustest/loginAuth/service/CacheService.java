package com.nsustest.loginAuth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 캐시 서비스
 * 공통코드, 메시지 코드 등을 캐시로 관리
 * 
 * @author nsustest
 */
@Service
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    // 캐시 키 접두사
    private static final String COMMON_CODE_GROUP_PREFIX = "common:code:group:";
    private static final String COMMON_CODE_PREFIX = "common:code:";
    private static final String COMMON_CODE_LIST_PREFIX = "common:code:list:";
    private static final String MESSAGE_CODE_PREFIX = "message:code:";
    private static final String MESSAGE_TYPE_PREFIX = "message:type:";
    
    // TTL 설정 (초)
    private static final long COMMON_CODE_TTL = 3600; // 1시간
    private static final long MESSAGE_CODE_TTL = 1800; // 30분
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 공통코드 그룹 정보를 캐시에 저장
     * 
     * @param grpCd 그룹 코드
     * @param groupInfo 그룹 정보
     */
    public void setCommonCodeGroup(String grpCd, Object groupInfo) {
        try {
            String key = COMMON_CODE_GROUP_PREFIX + grpCd;
            redisTemplate.opsForValue().set(key, groupInfo, COMMON_CODE_TTL, TimeUnit.SECONDS);
            logger.debug("공통코드 그룹 캐시 저장: {}", key);
        } catch (Exception e) {
            logger.error("공통코드 그룹 캐시 저장 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 공통코드 그룹 정보를 캐시에서 조회
     * 
     * @param grpCd 그룹 코드
     * @return 그룹 정보
     */
    public Object getCommonCodeGroup(String grpCd) {
        try {
            String key = COMMON_CODE_GROUP_PREFIX + grpCd;
            Object result = redisTemplate.opsForValue().get(key);
            logger.debug("공통코드 그룹 캐시 조회: {} = {}", key, result != null ? "HIT" : "MISS");
            return result;
        } catch (Exception e) {
            logger.error("공통코드 그룹 캐시 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 특정 공통코드를 캐시에 저장
     * 
     * @param grpCd 그룹 코드
     * @param cd 코드
     * @param codeInfo 코드 정보
     */
    public void setCommonCode(String grpCd, String cd, Object codeInfo) {
        try {
            String key = COMMON_CODE_PREFIX + grpCd + ":" + cd;
            redisTemplate.opsForValue().set(key, codeInfo, COMMON_CODE_TTL, TimeUnit.SECONDS);
            logger.debug("공통코드 캐시 저장: {}", key);
        } catch (Exception e) {
            logger.error("공통코드 캐시 저장 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 특정 공통코드를 캐시에서 조회
     * 
     * @param grpCd 그룹 코드
     * @param cd 코드
     * @return 코드 정보
     */
    public Object getCommonCode(String grpCd, String cd) {
        try {
            String key = COMMON_CODE_PREFIX + grpCd + ":" + cd;
            Object result = redisTemplate.opsForValue().get(key);
            logger.debug("공통코드 캐시 조회: {} = {}", key, result != null ? "HIT" : "MISS");
            return result;
        } catch (Exception e) {
            logger.error("공통코드 캐시 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 그룹별 공통코드 목록을 캐시에 저장
     * 
     * @param grpCd 그룹 코드
     * @param codeList 코드 목록
     */
    public void setCommonCodeList(String grpCd, List<Object> codeList) {
        try {
            String key = COMMON_CODE_LIST_PREFIX + grpCd;
            redisTemplate.opsForValue().set(key, codeList, COMMON_CODE_TTL, TimeUnit.SECONDS);
            logger.debug("공통코드 목록 캐시 저장: {}", key);
        } catch (Exception e) {
            logger.error("공통코드 목록 캐시 저장 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 그룹별 공통코드 목록을 캐시에서 조회
     * 
     * @param grpCd 그룹 코드
     * @return 코드 목록
     */
    @SuppressWarnings("unchecked")
    public List<Object> getCommonCodeList(String grpCd) {
        try {
            String key = COMMON_CODE_LIST_PREFIX + grpCd;
            Object result = redisTemplate.opsForValue().get(key);
            logger.debug("공통코드 목록 캐시 조회: {} = {}", key, result != null ? "HIT" : "MISS");
            return (List<Object>) result;
        } catch (Exception e) {
            logger.error("공통코드 목록 캐시 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 메시지 코드를 캐시에 저장
     * 
     * @param msgCd 메시지 코드
     * @param messageInfo 메시지 정보
     */
    public void setMessageCode(String msgCd, Object messageInfo) {
        try {
            String key = MESSAGE_CODE_PREFIX + msgCd;
            redisTemplate.opsForValue().set(key, messageInfo, MESSAGE_CODE_TTL, TimeUnit.SECONDS);
            logger.debug("메시지 코드 캐시 저장: {}", key);
        } catch (Exception e) {
            logger.error("메시지 코드 캐시 저장 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 메시지 코드를 캐시에서 조회
     * 
     * @param msgCd 메시지 코드
     * @return 메시지 정보
     */
    public Object getMessageCode(String msgCd) {
        try {
            String key = MESSAGE_CODE_PREFIX + msgCd;
            Object result = redisTemplate.opsForValue().get(key);
            logger.debug("메시지 코드 캐시 조회: {} = {}", key, result != null ? "HIT" : "MISS");
            return result;
        } catch (Exception e) {
            logger.error("메시지 코드 캐시 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 타입별 메시지 목록을 캐시에 저장
     * 
     * @param msgTpCd 메시지 타입 코드
     * @param messageList 메시지 목록
     */
    public void setMessageListByType(String msgTpCd, List<Object> messageList) {
        try {
            String key = MESSAGE_TYPE_PREFIX + msgTpCd;
            redisTemplate.opsForValue().set(key, messageList, MESSAGE_CODE_TTL, TimeUnit.SECONDS);
            logger.debug("타입별 메시지 목록 캐시 저장: {}", key);
        } catch (Exception e) {
            logger.error("타입별 메시지 목록 캐시 저장 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 타입별 메시지 목록을 캐시에서 조회
     * 
     * @param msgTpCd 메시지 타입 코드
     * @return 메시지 목록
     */
    @SuppressWarnings("unchecked")
    public List<Object> getMessageListByType(String msgTpCd) {
        try {
            String key = MESSAGE_TYPE_PREFIX + msgTpCd;
            Object result = redisTemplate.opsForValue().get(key);
            logger.debug("타입별 메시지 목록 캐시 조회: {} = {}", key, result != null ? "HIT" : "MISS");
            return (List<Object>) result;
        } catch (Exception e) {
            logger.error("타입별 메시지 목록 캐시 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 특정 패턴의 캐시 키들을 삭제
     * 
     * @param pattern 패턴
     * @return 삭제된 키 개수
     */
    public long deleteKeysByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                logger.info("패턴 '{}'에 해당하는 캐시 {}개 삭제", pattern, deletedCount);
                return deletedCount != null ? deletedCount : 0;
            }
            return 0;
        } catch (Exception e) {
            logger.error("패턴별 캐시 삭제 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 공통코드 관련 캐시 전체 삭제
     */
    public void clearCommonCodeCache() {
        deleteKeysByPattern(COMMON_CODE_GROUP_PREFIX + "*");
        deleteKeysByPattern(COMMON_CODE_PREFIX + "*");
        deleteKeysByPattern(COMMON_CODE_LIST_PREFIX + "*");
        logger.info("공통코드 캐시 전체 삭제 완료");
    }
    
    /**
     * 메시지 코드 관련 캐시 전체 삭제
     */
    public void clearMessageCodeCache() {
        deleteKeysByPattern(MESSAGE_CODE_PREFIX + "*");
        deleteKeysByPattern(MESSAGE_TYPE_PREFIX + "*");
        logger.info("메시지 코드 캐시 전체 삭제 완료");
    }
    
    /**
     * 전체 캐시 삭제
     */
    public void clearAllCache() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            logger.info("전체 캐시 삭제 완료");
        } catch (Exception e) {
            logger.error("전체 캐시 삭제 실패: {}", e.getMessage(), e);
        }
    }
}
