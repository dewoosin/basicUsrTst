package com.nsustest.loginAuth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * CacheService 단위 테스트
 * Redis 캐시 저장/조회/삭제 기능을 테스트합니다.
 * 
 * @author nsustest
 */
@ExtendWith(MockitoExtension.class)
public class CacheServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private RedisConnectionFactory connectionFactory;
    
    @Mock
    private RedisConnection connection;
    
    @InjectMocks
    private CacheService cacheService;
    
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    /**
     * 테스트 1: 공통코드 그룹 저장 및 조회
     */
    @Test
    void testSetAndGetCommonCodeGroup() {
        // Given
        String grpCd = "USR_TP";
        Map<String, Object> groupInfo = new HashMap<>();
        groupInfo.put("grp_cd", grpCd);
        groupInfo.put("grp_nm", "사용자 타입");
        
        String expectedKey = "common:code:group:" + grpCd;
        when(valueOperations.get(expectedKey)).thenReturn(groupInfo);
        
        // When
        cacheService.setCommonCodeGroup(grpCd, groupInfo);
        Object result = cacheService.getCommonCodeGroup(grpCd);
        
        // Then
        verify(valueOperations).set(eq(expectedKey), eq(groupInfo), eq(3600L), eq(TimeUnit.SECONDS));
        verify(valueOperations).get(expectedKey);
        assertNotNull(result);
        assertEquals(groupInfo, result);
    }
    
    /**
     * 테스트 2: 공통코드 목록 저장 및 조회
     */
    @Test
    void testSetAndGetCommonCodeList() {
        // Given
        String grpCd = "USR_TP";
        List<Object> codeList = Arrays.asList(
            createCode("USR_TP", "01", "관리자"),
            createCode("USR_TP", "02", "일반사용자")
        );
        
        String expectedKey = "common:code:list:" + grpCd;
        when(valueOperations.get(expectedKey)).thenReturn(codeList);
        
        // When
        cacheService.setCommonCodeList(grpCd, codeList);
        List<Object> result = cacheService.getCommonCodeList(grpCd);
        
        // Then
        verify(valueOperations).set(eq(expectedKey), eq(codeList), eq(3600L), eq(TimeUnit.SECONDS));
        verify(valueOperations).get(expectedKey);
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    /**
     * 테스트 3: 메시지 코드 저장 및 조회
     */
    @Test
    void testSetAndGetMessageCode() {
        // Given
        String msgCd = "AUTH_001";
        Map<String, Object> messageInfo = new HashMap<>();
        messageInfo.put("msg_cd", msgCd);
        messageInfo.put("msg_cont", "인증 실패");
        
        String expectedKey = "message:code:" + msgCd;
        when(valueOperations.get(expectedKey)).thenReturn(messageInfo);
        
        // When
        cacheService.setMessageCode(msgCd, messageInfo);
        Object result = cacheService.getMessageCode(msgCd);
        
        // Then
        verify(valueOperations).set(eq(expectedKey), eq(messageInfo), eq(1800L), eq(TimeUnit.SECONDS));
        verify(valueOperations).get(expectedKey);
        assertNotNull(result);
        assertEquals(messageInfo, result);
    }
    
    /**
     * 테스트 4: 타입별 메시지 목록 저장 및 조회
     */
    @Test
    void testSetAndGetMessageListByType() {
        // Given
        String msgTpCd = "04";
        List<Object> messageList = Arrays.asList(
            createMessage("AUTH_001", "04", "인증 실패"),
            createMessage("AUTH_002", "04", "계정 잠금")
        );
        
        String expectedKey = "message:type:" + msgTpCd;
        when(valueOperations.get(expectedKey)).thenReturn(messageList);
        
        // When
        cacheService.setMessageListByType(msgTpCd, messageList);
        List<Object> result = cacheService.getMessageListByType(msgTpCd);
        
        // Then
        verify(valueOperations).set(eq(expectedKey), eq(messageList), eq(1800L), eq(TimeUnit.SECONDS));
        verify(valueOperations).get(expectedKey);
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    /**
     * 테스트 5: 패턴별 캐시 키 삭제
     */
    @Test
    void testDeleteKeysByPattern() {
        // Given
        String pattern = "common:code:*";
        Set<String> keys = Set.of("common:code:group:USR_TP", "common:code:list:USR_TP");
        
        when(redisTemplate.keys(pattern)).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(2L);
        
        // When
        long deletedCount = cacheService.deleteKeysByPattern(pattern);
        
        // Then
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(keys);
        assertEquals(2, deletedCount);
    }
    
    /**
     * 테스트 6: 공통코드 캐시 전체 삭제
     */
    @Test
    void testClearCommonCodeCache() {
        // Given
        Set<String> groupKeys = Set.of("common:code:group:USR_TP");
        Set<String> listKeys = Set.of("common:code:list:USR_TP");
        
        when(redisTemplate.keys("common:code:group:*")).thenReturn(groupKeys);
        when(redisTemplate.keys("common:code:list:*")).thenReturn(listKeys);
        when(redisTemplate.delete(any(Set.class))).thenReturn(1L);
        
        // When
        cacheService.clearCommonCodeCache();
        
        // Then
        verify(redisTemplate, times(2)).keys(anyString());
        verify(redisTemplate, times(2)).delete(any(Set.class));
    }
    
    /**
     * 테스트 7: 메시지 코드 캐시 전체 삭제
     */
    @Test
    void testClearMessageCodeCache() {
        // Given
        Set<String> messageKeys = Set.of("message:code:AUTH_001");
        Set<String> typeKeys = Set.of("message:type:04");
        
        when(redisTemplate.keys("message:code:*")).thenReturn(messageKeys);
        when(redisTemplate.keys("message:type:*")).thenReturn(typeKeys);
        when(redisTemplate.delete(any(Set.class))).thenReturn(1L);
        
        // When
        cacheService.clearMessageCodeCache();
        
        // Then
        verify(redisTemplate, times(2)).keys(anyString());
        verify(redisTemplate, times(2)).delete(any(Set.class));
    }
    
    /**
     * 테스트 8: 전체 캐시 삭제
     */
    @Test
    void testClearAllCache() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        
        // When
        cacheService.clearAllCache();
        
        // Then
        verify(connection).flushAll();
    }
    
    /**
     * 테스트 9: 캐시 조회 실패 시 null 반환
     */
    @Test
    void testGetCommonCodeGroup_RedisException_ReturnsNull() {
        // Given
        String grpCd = "USR_TP";
        String key = "common:code:group:" + grpCd;
        when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis 연결 오류"));
        
        // When
        Object result = cacheService.getCommonCodeGroup(grpCd);
        
        // Then
        assertNull(result, "예외 발생 시 null을 반환해야 함");
    }
    
    /**
     * 테스트 10: 캐시 저장 실패 시 예외 처리
     */
    @Test
    void testSetMessageCode_RedisException_HandlesGracefully() {
        // Given
        String msgCd = "AUTH_001";
        Map<String, Object> messageInfo = new HashMap<>();
        doThrow(new RuntimeException("Redis 연결 오류"))
            .when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        
        // When & Then
        assertDoesNotThrow(() -> {
            cacheService.setMessageCode(msgCd, messageInfo);
        }, "예외가 발생해도 메서드는 정상 종료해야 함");
    }
    
    /**
     * 테스트 11: 패턴 매칭 키가 없을 때
     */
    @Test
    void testDeleteKeysByPattern_NoKeysFound_ReturnsZero() {
        // Given
        String pattern = "nonexistent:*";
        when(redisTemplate.keys(pattern)).thenReturn(null);
        
        // When
        long deletedCount = cacheService.deleteKeysByPattern(pattern);
        
        // Then
        assertEquals(0, deletedCount, "매칭되는 키가 없으면 0을 반환해야 함");
        verify(redisTemplate, never()).delete(any(Set.class));
    }
    
    // ========== 헬퍼 메서드 ==========
    
    /**
     * 공통코드 Mock 데이터 생성
     */
    private Map<String, Object> createCode(String grpCd, String cd, String cdNm) {
        Map<String, Object> code = new HashMap<>();
        code.put("grp_cd", grpCd);
        code.put("cd", cd);
        code.put("cd_nm", cdNm);
        return code;
    }
    
    /**
     * 메시지 코드 Mock 데이터 생성
     */
    private Map<String, Object> createMessage(String msgCd, String msgTpCd, String msgCont) {
        Map<String, Object> message = new HashMap<>();
        message.put("msg_cd", msgCd);
        message.put("msg_tp_cd", msgTpCd);
        message.put("msg_cont", msgCont);
        return message;
    }
}

