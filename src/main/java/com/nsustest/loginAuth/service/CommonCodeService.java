package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 공통코드 및 메시지 코드 서비스
 * Redis 캐시를 활용한 공통코드 관리
 * 
 * @author nsustest
 */
@Service
public class CommonCodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommonCodeService.class);
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private LoginDao loginDao;
    
    /**
     * 공통코드 그룹 정보 조회 (캐시 우선)
     * 
     * @param grpCd 그룹 코드
     * @return 그룹 정보
     */
    public Map<String, Object> getCommonCodeGroup(String grpCd) {
        // 1. 캐시에서 조회
        Object cached = cacheService.getCommonCodeGroup(grpCd);
        if (cached != null) {
            logger.debug("공통코드 그룹 캐시 HIT: {}", grpCd);
            return (Map<String, Object>) cached;
        }
        
        // 2. DB에서 조회
        logger.debug("공통코드 그룹 캐시 MISS, DB 조회: {}", grpCd);
        Map<String, Object> groupInfo = loginDao.selectCommonCodeGroup(grpCd);
        
        // 3. 캐시에 저장
        if (groupInfo != null) {
            cacheService.setCommonCodeGroup(grpCd, groupInfo);
        }
        
        return groupInfo;
    }
    
    /**
     * 특정 공통코드 조회 (캐시 우선)
     * 
     * @param grpCd 그룹 코드
     * @param cd 코드
     * @return 코드 정보
     */
    public Map<String, Object> getCommonCode(String grpCd, String cd) {
        // 1. 캐시에서 조회
        Object cached = cacheService.getCommonCode(grpCd, cd);
        if (cached != null) {
            logger.debug("공통코드 캐시 HIT: {}:{}", grpCd, cd);
            return (Map<String, Object>) cached;
        }
        
        // 2. DB에서 조회
        logger.debug("공통코드 캐시 MISS, DB 조회: {}:{}", grpCd, cd);
        Map<String, Object> codeInfo = loginDao.selectCommonCode(grpCd, cd);
        
        // 3. 캐시에 저장
        if (codeInfo != null) {
            cacheService.setCommonCode(grpCd, cd, codeInfo);
        }
        
        return codeInfo;
    }
    
    /**
     * 그룹별 공통코드 목록 조회 (캐시 우선)
     * 
     * @param grpCd 그룹 코드
     * @return 코드 목록
     */
    public List<Map<String, Object>> getCommonCodeList(String grpCd) {
        // 1. 캐시에서 조회
        List<Object> cached = cacheService.getCommonCodeList(grpCd);
        if (cached != null) {
            logger.debug("공통코드 목록 캐시 HIT: {}", grpCd);
            return (List<Map<String, Object>>) (List<?>) cached;
        }
        
        // 2. DB에서 조회
        logger.debug("공통코드 목록 캐시 MISS, DB 조회: {}", grpCd);
        List<Map<String, Object>> codeList = loginDao.selectCommonCodeList(grpCd);
        
        // 3. 캐시에 저장
        if (codeList != null && !codeList.isEmpty()) {
            cacheService.setCommonCodeList(grpCd, (List<Object>) (List<?>) codeList);
        }
        
        return codeList;
    }
    
    /**
     * 메시지 코드 조회 (캐시 우선)
     * 
     * @param msgCd 메시지 코드
     * @return 메시지 정보
     */
    public Map<String, Object> getMessageCode(String msgCd) {
        // 1. 캐시에서 조회
        Object cached = cacheService.getMessageCode(msgCd);
        if (cached != null) {
            logger.debug("메시지 코드 캐시 HIT: {}", msgCd);
            return (Map<String, Object>) cached;
        }
        
        // 2. DB에서 조회
        logger.debug("메시지 코드 캐시 MISS, DB 조회: {}", msgCd);
        Map<String, Object> messageInfo = loginDao.selectMessageCode(msgCd);
        
        // 3. 캐시에 저장
        if (messageInfo != null) {
            cacheService.setMessageCode(msgCd, messageInfo);
        }
        
        return messageInfo;
    }
    
    /**
     * 타입별 메시지 목록 조회 (캐시 우선)
     * 
     * @param msgTpCd 메시지 타입 코드
     * @return 메시지 목록
     */
    public List<Map<String, Object>> getMessageListByType(String msgTpCd) {
        // 1. 캐시에서 조회
        List<Object> cached = cacheService.getMessageListByType(msgTpCd);
        if (cached != null) {
            logger.debug("타입별 메시지 목록 캐시 HIT: {}", msgTpCd);
            return (List<Map<String, Object>>) (List<?>) cached;
        }
        
        // 2. DB에서 조회
        logger.debug("타입별 메시지 목록 캐시 MISS, DB 조회: {}", msgTpCd);
        List<Map<String, Object>> messageList = loginDao.selectMessageListByType(msgTpCd);
        
        // 3. 캐시에 저장
        if (messageList != null && !messageList.isEmpty()) {
            cacheService.setMessageListByType(msgTpCd, (List<Object>) (List<?>) messageList);
        }
        
        return messageList;
    }
    
    /**
     * 공통코드 캐시 초기화
     */
    public void refreshCommonCodeCache() {
        cacheService.clearCommonCodeCache();
        logger.info("공통코드 캐시 초기화 완료");
    }
    
    /**
     * 메시지 코드 캐시 초기화
     */
    public void refreshMessageCodeCache() {
        cacheService.clearMessageCodeCache();
        logger.info("메시지 코드 캐시 초기화 완료");
    }
    
    /**
     * 전체 캐시 초기화
     */
    public void refreshAllCache() {
        cacheService.clearAllCache();
        logger.info("전체 캐시 초기화 완료");
    }
}
