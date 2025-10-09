package com.nsustest.loginAuth.service;

import com.nsustest.loginAuth.dao.LoginDao;
import com.nsustest.loginAuth.dto.ApiResponse;
import com.nsustest.loginAuth.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * @author nsustest
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private LoginDao loginDao;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CommonCodeService commonCodeService;
    
    @Autowired
    private MessageUtil messageUtil;
    
    /**
     * 아이디 중복 확인
     * 
     * @param usrLoginId 확인할 로그인 아이디
     * @return 중복 확인 결과 ApiResponse
     */
    public ApiResponse<Map<String, Object>> checkIdDuplicate(String usrLoginId) {
        try {
            // 아이디 유효성 검사
            ApiResponse<Map<String, Object>> validationResult = validateLoginId(usrLoginId);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            // 중복 확인 (0보다 크면 중복)
            int count = loginDao.checkIdDuplicate(usrLoginId);
            boolean isDuplicate = count > 0;
            
            Map<String, Object> data = new HashMap<>();
            data.put("duplicate", isDuplicate);
            data.put("usrLoginId", usrLoginId);
            
            if (isDuplicate) {
                return ApiResponse.error(messageUtil.getMessage("USER_003"), "USER_003");
            } else {
                return ApiResponse.success(messageUtil.getMessage("SERVICE_001"), data);
            }
            
        } catch (Exception e) {
            logger.error("아이디 중복 확인 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error("서버 오류가 발생했습니다.", "SRV_001");
        }
    }
    
    /**
     * 회원가입 처리
     * 
     * @param signupData 회원가입 데이터
     * @return 회원가입 결과 ApiResponse
     */
    public ApiResponse<Map<String, Object>> signup(Map<String, Object> signupData) {
        String usrLoginId = null;
        try {
            
            // 필수 필드 검증
            ApiResponse<Map<String, Object>> validationResult = validateSignupData(signupData);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            usrLoginId = (String) signupData.get("usrLoginId");
            String email = (String) signupData.get("email");
            String password = (String) signupData.get("password");
            
            // 중복 확인
            ApiResponse<Map<String, Object>> duplicateCheck = checkDuplicates(usrLoginId, email);
            if (!duplicateCheck.isSuccess()) {
                return duplicateCheck;
            }
            
            // 비밀번호 암호화 및 저장
            String encodedPassword = passwordEncoder.encode(password);
            Map<String, Object> userData = createUserData(signupData, encodedPassword);
            
            int result = loginDao.insertUser(userData);
            
            if (result > 0) {
                // 생성된 사용자 ID 가져오기
                Long usrId = ((Number) userData.get("usrId")).longValue();
                
                // 신규 사용자 통계 정보 초기화
                Map<String, Object> userStats = new HashMap<>();
                userStats.put("usrId", usrId);
                
                loginDao.insertUserStats(userStats);
                
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("usrLoginId", usrLoginId);
                responseData.put("email", email);
                
                return ApiResponse.success(messageUtil.getMessage("SERVICE_002"), responseData);
            } else {
                return ApiResponse.error(messageUtil.getMessage("SERVICE_003"), "SRV_001");
            }
            
        } catch (Exception e) {
            logger.error("회원가입 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error("서버 오류가 발생했습니다.", "SRV_001");
        }
    }
    
    /**
     * 사용자 정보 조회
     * 
     * @param usrId 사용자 ID
     * @return 사용자 정보
     */
    public ApiResponse<Map<String, Object>> getUserInfo(Long usrId) {
        try {
            
            Map<String, Object> user = loginDao.findById(usrId);
            if (user == null) {
                return ApiResponse.error(messageUtil.getMessage("SERVICE_014"), "USER_001");
            }
            
            // 비밀번호 제외한 사용자 정보 반환
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("usrId", user.get("usr_id"));
            userInfo.put("usrLoginId", user.get("usr_login_id"));
            userInfo.put("usrNm", user.get("usr_nm"));
            userInfo.put("email", user.get("email"));
            userInfo.put("usrTpCd", user.get("usr_tp_cd"));
            userInfo.put("phoneNum", user.get("phone_num"));
            userInfo.put("isUse", user.get("is_use"));
            userInfo.put("creDt", user.get("cre_dt"));
            userInfo.put("updDt", user.get("upd_dt"));
            
            return ApiResponse.success(messageUtil.getMessage("SERVICE_018"), userInfo);
            
        } catch (Exception e) {
            logger.error("사용자 정보 조회 중 예외 발생: {}", e.getMessage(), e);
            return ApiResponse.error(messageUtil.getMessage("SERVICE_016"), "SRV_001");
        }
    }
    
    // ==================== 검증 로직 함수들 ====================
    
    /**
     * 로그인 아이디 유효성 검사
     */
    private ApiResponse<Map<String, Object>> validateLoginId(String usrLoginId) {
        if (usrLoginId == null || usrLoginId.trim().isEmpty()) {
            return ApiResponse.error(messageUtil.getMessage("VAL_001"), "VAL_001");
        }
        
        if (!usrLoginId.matches("^[a-zA-Z0-9]{4,20}$")) {
            return ApiResponse.error(messageUtil.getMessage("SERVICE_006"), "VAL_002");
        }
        
        return ApiResponse.success(messageUtil.getMessage("SERVICE_007"), null);
    }
    
    /**
     * 회원가입 데이터 유효성 검사
     */
    private ApiResponse<Map<String, Object>> validateSignupData(Map<String, Object> signupData) {
        String usrLoginId = (String) signupData.get("usrLoginId");
        String usrNm = (String) signupData.get("usrNm");
        String email = (String) signupData.get("email");
        String password = (String) signupData.get("password");
        
        // 아이디 검증
        ApiResponse<Map<String, Object>> idValidation = validateLoginId(usrLoginId);
        if (!idValidation.isSuccess()) {
            return idValidation;
        }
        
        // 이름 검증
        if (usrNm == null || usrNm.trim().isEmpty()) {
            return ApiResponse.error(messageUtil.getMessage("VAL_001"), "VAL_001");
        }
        
        // 이메일 검증
        if (email == null || email.trim().isEmpty()) {
            return ApiResponse.error(messageUtil.getMessage("VAL_001"), "VAL_001");
        }
        
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ApiResponse.error(messageUtil.getMessage("SERVICE_008"), "VAL_002");
        }
        
        // 비밀번호 검증
        if (password == null || password.trim().isEmpty()) {
            return ApiResponse.error(messageUtil.getMessage("VAL_001"), "VAL_001");
        }
        
        if (!password.matches("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$")) {
            return ApiResponse.error(messageUtil.getMessage("PWD_005"), "PWD_002");
        }
        
        return ApiResponse.success(messageUtil.getMessage("SERVICE_009"), null);
    }
    
    /**
     * 중복 확인
     */
    private ApiResponse<Map<String, Object>> checkDuplicates(String usrLoginId, String email) {
        // 아이디 중복 확인
        int idCount = loginDao.checkIdDuplicate(usrLoginId);
        if (idCount > 0) {
            return ApiResponse.error(messageUtil.getMessage("USER_003"), "USER_003");
        }
        
        // 이메일 중복 확인
        int emailCount = loginDao.checkEmailDuplicate(email);
        if (emailCount > 0) {
            return ApiResponse.error(messageUtil.getMessage("USER_004"), "USER_004");
        }
        
        // 성공 응답
        Map<String, Object> data = new HashMap<>();
        data.put("usrLoginId", usrLoginId);
        data.put("email", email);
        return ApiResponse.success(messageUtil.getMessage("SERVICE_011"), data);
    }
    
    /**
     * 사용자 데이터 생성
     */
    private Map<String, Object> createUserData(Map<String, Object> signupData, String encodedPassword) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("usrLoginId", signupData.get("usrLoginId"));
        userData.put("usrNm", signupData.get("usrNm"));
        userData.put("email", signupData.get("email"));
        userData.put("password", encodedPassword);
        userData.put("phoneNum", signupData.get("phoneNum"));
        return userData;
    }
}
