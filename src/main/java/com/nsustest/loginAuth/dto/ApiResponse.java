package com.nsustest.loginAuth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * API 응답을 위한 공통 응답 클래스
 * 
 * @param <T> 응답 데이터 타입
 * @author nsustest
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 기본 생성자
     */
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.success = false; // 기본값을 false로 설정
    }
    
    /**
     * 성공 응답 생성자
     * 
     * @param message 응답 메시지
     * @param data 응답 데이터
     */
    public ApiResponse(String message, T data) {
        this();
        this.success = true;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 에러 응답 생성자
     * 
     * @param message 에러 메시지
     * @param errorCode 에러 코드
     */
    public ApiResponse(String message, String errorCode) {
        this();
        this.success = false;
        this.message = message;
        this.errorCode = errorCode;
    }
    
    /**
     * 성공 응답 생성 (데이터 없음)
     * 
     * @param message 응답 메시지
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
    
    /**
     * 성공 응답 생성 (데이터 포함)
     * 
     * @param message 응답 메시지
     * @param data 응답 데이터
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data);
    }
    
    /**
     * 에러 응답 생성
     * 
     * @param message 에러 메시지
     * @param errorCode 에러 코드
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(message, errorCode);
    }
    
    /**
     * 에러 응답 생성 (에러 코드 없음)
     * 
     * @param message 에러 메시지
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, "UNKNOWN_ERROR");
    }
    
    // Getter and Setter methods
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", errorCode='" + errorCode + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
