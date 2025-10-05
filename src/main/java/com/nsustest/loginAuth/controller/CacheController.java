package com.nsustest.loginAuth.controller;

import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.service.CommonCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 캐시 관리 컨트롤러
 * Redis 캐시 초기화 및 관리 기능 제공 (관리자 전용)
 * 
 * @author nsustest
 */
@RestController
@RequestMapping("/api/admin/cache")
public class CacheController {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);
    
    @Autowired
    private CommonCodeService commonCodeService;
    
    /**
     * 공통코드 캐시 초기화
     * 
     * @return 초기화 결과
     */
    @PostMapping("/common-code/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshCommonCodeCache() {
        try {
            commonCodeService.refreshCommonCodeCache();
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "공통코드 캐시가 초기화되었습니다.");
            data.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("공통코드 캐시 초기화 완료", data));
        } catch (Exception e) {
            logger.error("공통코드 캐시 초기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("공통코드 캐시 초기화에 실패했습니다.", "CACHE_001"));
        }
    }
    
    /**
     * 메시지 코드 캐시 초기화
     * 
     * @return 초기화 결과
     */
    @PostMapping("/message-code/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshMessageCodeCache() {
        try {
            commonCodeService.refreshMessageCodeCache();
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "메시지 코드 캐시가 초기화되었습니다.");
            data.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("메시지 코드 캐시 초기화 완료", data));
        } catch (Exception e) {
            logger.error("메시지 코드 캐시 초기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("메시지 코드 캐시 초기화에 실패했습니다.", "CACHE_002"));
        }
    }
    
    /**
     * 전체 캐시 초기화
     * 
     * @return 초기화 결과
     */
    @PostMapping("/all/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshAllCache() {
        try {
            commonCodeService.refreshAllCache();
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "전체 캐시가 초기화되었습니다.");
            data.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("전체 캐시 초기화 완료", data));
        } catch (Exception e) {
            logger.error("전체 캐시 초기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("전체 캐시 초기화에 실패했습니다.", "CACHE_003"));
        }
    }
    
    /**
     * 공통코드 그룹 정보 조회 (캐시 테스트용)
     * 
     * @param grpCd 그룹 코드
     * @return 그룹 정보
     */
    @GetMapping("/common-code/group/{grpCd}")
    public ResponseEntity<ApiResponse<Object>> getCommonCodeGroup(@PathVariable String grpCd) {
        try {
            Map<String, Object> groupInfo = commonCodeService.getCommonCodeGroup(grpCd);
            
            if (groupInfo != null) {
                return ResponseEntity.ok(ApiResponse.success("공통코드 그룹 조회 성공", groupInfo));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("공통코드 그룹 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("공통코드 그룹 조회에 실패했습니다.", "CACHE_004"));
        }
    }
    
    /**
     * 메시지 코드 조회 (캐시 테스트용)
     * 
     * @param msgCd 메시지 코드
     * @return 메시지 정보
     */
    @GetMapping("/message-code/{msgCd}")
    public ResponseEntity<ApiResponse<Object>> getMessageCode(@PathVariable String msgCd) {
        try {
            Map<String, Object> messageInfo = commonCodeService.getMessageCode(msgCd);
            
            if (messageInfo != null) {
                return ResponseEntity.ok(ApiResponse.success("메시지 코드 조회 성공", messageInfo));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("메시지 코드 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("메시지 코드 조회에 실패했습니다.", "CACHE_005"));
        }
    }
}
