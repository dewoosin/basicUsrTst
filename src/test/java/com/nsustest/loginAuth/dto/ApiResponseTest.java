package com.nsustest.loginAuth.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiResponse 클래스의 단위 테스트
 * 
 * @author nsustest
 */
public class ApiResponseTest {
    
    /**
     * 성공 응답 생성 테스트 (데이터 없음)
     */
    @Test
    void testSuccessResponseWithoutData() {
        // Given & When
        ApiResponse<Object> response = ApiResponse.success("작업이 완료되었습니다.");
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("작업이 완료되었습니다.", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * 성공 응답 생성 테스트 (데이터 포함)
     */
    @Test
    void testSuccessResponseWithData() {
        // Given
        Map<String, Object> testData = new HashMap<>();
        testData.put("id", 1);
        testData.put("name", "테스트");
        
        // When
        ApiResponse<Map<String, Object>> response = ApiResponse.success("데이터를 조회했습니다.", testData);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("데이터를 조회했습니다.", response.getMessage());
        assertEquals(testData, response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * 에러 응답 생성 테스트 (에러 코드 포함)
     */
    @Test
    void testErrorResponseWithErrorCode() {
        // Given & When
        ApiResponse<Object> response = ApiResponse.error("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND");
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("사용자를 찾을 수 없습니다.", response.getMessage());
        assertNull(response.getData());
        assertEquals("USER_NOT_FOUND", response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * 에러 응답 생성 테스트 (에러 코드 없음)
     */
    @Test
    void testErrorResponseWithoutErrorCode() {
        // Given & When
        ApiResponse<Object> response = ApiResponse.error("알 수 없는 오류가 발생했습니다.");
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("알 수 없는 오류가 발생했습니다.", response.getMessage());
        assertNull(response.getData());
        assertEquals("UNKNOWN_ERROR", response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * 기본 생성자 테스트
     */
    @Test
    void testDefaultConstructor() {
        // Given & When
        ApiResponse<Object> response = new ApiResponse<>();
        
        // Then
        assertFalse(response.isSuccess()); // 기본값은 false
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * 성공 응답 생성자 테스트
     */
    @Test
    void testSuccessConstructor() {
        // Given
        Map<String, Object> testData = new HashMap<>();
        testData.put("result", "success");
        
        // When
        ApiResponse<Map<String, Object>> response = new ApiResponse<>("성공했습니다.", testData);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("성공했습니다.", response.getMessage());
        assertEquals(testData, response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * 에러 응답 생성자 테스트
     */
    @Test
    void testErrorConstructor() {
        // Given & When
        ApiResponse<Object> response = new ApiResponse<>("오류가 발생했습니다.", "ERROR_001");
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals("오류가 발생했습니다.", response.getMessage());
        assertNull(response.getData());
        assertEquals("ERROR_001", response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * Setter 메서드 테스트
     */
    @Test
    void testSetters() {
        // Given
        ApiResponse<String> response = new ApiResponse<>();
        Map<String, Object> testData = new HashMap<>();
        testData.put("test", "value");
        
        // When
        response.setSuccess(true);
        response.setMessage("테스트 메시지");
        response.setData("테스트 데이터");
        response.setErrorCode("TEST_ERROR");
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("테스트 메시지", response.getMessage());
        assertEquals("테스트 데이터", response.getData());
        assertEquals("TEST_ERROR", response.getErrorCode());
    }
    
    /**
     * toString 메서드 테스트
     */
    @Test
    void testToString() {
        // Given
        ApiResponse<String> response = ApiResponse.success("테스트 성공", "테스트 데이터");
        
        // When
        String result = response.toString();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("ApiResponse"));
        assertTrue(result.contains("success=true"));
        assertTrue(result.contains("테스트 성공"));
        assertTrue(result.contains("테스트 데이터"));
    }
    
    /**
     * 제네릭 타입 테스트
     */
    @Test
    void testGenericTypes() {
        // Given & When
        ApiResponse<Integer> intResponse = ApiResponse.success("정수 데이터", 42);
        ApiResponse<String> stringResponse = ApiResponse.success("문자열 데이터", "Hello");
        ApiResponse<Map<String, Object>> mapResponse = ApiResponse.success("맵 데이터", new HashMap<>());
        
        // Then
        assertEquals(Integer.class, intResponse.getData().getClass());
        assertEquals(String.class, stringResponse.getData().getClass());
        assertEquals(HashMap.class, mapResponse.getData().getClass());
    }
}
