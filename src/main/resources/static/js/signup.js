// 전역 변수
let isIdChecked = false;

// 비밀번호 보기/숨기기 토글
function togglePassword(inputId) {
    const passwordInput = document.getElementById(inputId);
    const toggleIcon = document.getElementById(inputId + 'ToggleIcon');
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleIcon.classList.remove('fa-eye');
        toggleIcon.classList.add('fa-eye-slash');
    } else {
        passwordInput.type = 'password';
        toggleIcon.classList.remove('fa-eye-slash');
        toggleIcon.classList.add('fa-eye');
    }
}

/**
 * 아이디 중복 확인 함수
 */
async function checkIdDuplicate() {
    const usrLoginId = document.getElementById('usrLoginId').value.trim();
    const checkBtn = document.getElementById('checkIdBtn');
    const loadingIcon = checkBtn.querySelector('.loading-icon');
    const btnText = checkBtn.querySelector('.btn-text');
    const resultDiv = document.getElementById('idCheckResult');
    
    console.log('checkIdDuplicate 호출됨, usrLoginId:', usrLoginId); // 디버깅용
    
    // 입력값 검증
    if (!usrLoginId) {
        showIdCheckResult('아이디를 입력해주세요.', 'error');
        return;
    }
    
    if (!usrLoginId.match(/^[a-zA-Z0-9]{4,20}$/)) {
        showIdCheckResult('아이디는 영문, 숫자 조합 4-20자로 입력해주세요.', 'error');
        return;
    }
    
    // 로딩 상태 표시
    checkBtn.disabled = true;
    loadingIcon.style.display = 'inline-block';
    btnText.textContent = '확인중...';
    
    try {
        console.log('API 호출 시작:', `/api/check-id?usrLoginId=${encodeURIComponent(usrLoginId)}`); // 디버깅용
        
        const response = await fetch(`/api/check-id?usrLoginId=${encodeURIComponent(usrLoginId)}`);
        const data = await response.json();
        
        console.log('API 응답:', data); // 디버깅용
        
        if (data.success) {
            if (data.data && data.data.duplicate) {
                showIdCheckResult('이미 사용 중인 아이디입니다.', 'error');
            } else {
                showIdCheckResult(data.message || '사용 가능한 아이디입니다.', 'success');
                isIdChecked = true; // 중복확인 완료 표시
            }
        } else {
            showIdCheckResult(data.message || '중복 확인 중 오류가 발생했습니다.', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showIdCheckResult('서버와의 통신 중 오류가 발생했습니다.', 'error');
    } finally {
        // 로딩 상태 해제
        checkBtn.disabled = false;
        loadingIcon.style.display = 'none';
        btnText.textContent = '중복확인';
    }
}

/**
 * 아이디 중복 확인 결과 표시
 * 
 * @param {string} message - 표시할 메시지
 * @param {string} type - 메시지 타입 ('success' 또는 'error')
 */
function showIdCheckResult(message, type) {
    const resultDiv = document.getElementById('idCheckResult');
    resultDiv.textContent = message;
    resultDiv.className = `check-result ${type}`;
    resultDiv.style.display = 'block';
    
    // 3초 후 메시지 숨기기 (성공 메시지인 경우)
    if (type === 'success') {
        setTimeout(() => {
            resultDiv.style.display = 'none';
        }, 3000);
    }
}

// 아이디 중복확인 결과 숨기기
function hideIdCheckResult() {
    const resultDiv = document.getElementById('idCheckResult');
    resultDiv.style.display = 'none';
}

// 아이디 변경 시 중복확인 상태 초기화
document.getElementById('usrLoginId').addEventListener('input', function() {
    isIdChecked = false;
    hideIdCheckResult();
});

// 비밀번호 강도 검사
function checkPasswordStrength(password) {
    let score = 0;
    const checks = {
        length: password.length >= 8,
        lowercase: /[a-z]/.test(password),
        uppercase: /[A-Z]/.test(password),
        number: /\d/.test(password),
        special: /[!@#$%^&*(),.?":{}|<>]/.test(password)
    };
    
    Object.values(checks).forEach(check => {
        if (check) score++;
    });
    
    if (score < 3) return 'weak';
    if (score < 5) return 'medium';
    return 'strong';
}

// 비밀번호 일치 검사
function validatePasswordMatch() {
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const confirmGroup = document.getElementById('confirmPassword').closest('.form-group');
    
    if (confirmPassword && password !== confirmPassword) {
        confirmGroup.classList.add('error');
        confirmGroup.classList.remove('success');
        showFieldError(confirmGroup, '비밀번호가 일치하지 않습니다.');
        return false;
    } else if (confirmPassword) {
        confirmGroup.classList.remove('error');
        confirmGroup.classList.add('success');
        hideFieldError(confirmGroup);
        return true;
    }
    return true;
}

// 필드 에러 표시
function showFieldError(group, message) {
    hideFieldError(group);
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    group.appendChild(errorDiv);
}

// 필드 에러 숨기기
function hideFieldError(group) {
    const existingError = group.querySelector('.error-message');
    if (existingError) {
        existingError.remove();
    }
}

// 폼 유효성 검사
function validateForm() {
    let isValid = true;
    
    // 아이디 검사
    const loginId = document.getElementById('usrLoginId').value;
    const loginIdGroup = document.getElementById('usrLoginId').closest('.form-group');
    
    if (!/^[a-zA-Z0-9]{4,20}$/.test(loginId)) {
        loginIdGroup.classList.add('error');
        showFieldError(loginIdGroup, '아이디는 영문, 숫자 조합 4-20자여야 합니다.');
        isValid = false;
    } else if (!isIdChecked) {
        loginIdGroup.classList.add('error');
        showFieldError(loginIdGroup, '아이디 중복확인을 해주세요.');
        isValid = false;
    } else {
        loginIdGroup.classList.remove('error');
        hideFieldError(loginIdGroup);
    }
    
    // 이메일 검사
    const email = document.getElementById('email').value;
    const emailGroup = document.getElementById('email').closest('.form-group');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        emailGroup.classList.add('error');
        showFieldError(emailGroup, '올바른 이메일 형식이 아닙니다.');
        isValid = false;
    } else {
        emailGroup.classList.remove('error');
        hideFieldError(emailGroup);
    }
    
    // 비밀번호 검사
    const password = document.getElementById('password').value;
    const passwordGroup = document.getElementById('password').closest('.form-group');
    const strength = checkPasswordStrength(password);
    
    if (strength === 'weak') {
        passwordGroup.classList.add('error');
        showFieldError(passwordGroup, '비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다.');
        isValid = false;
    } else {
        passwordGroup.classList.remove('error');
        hideFieldError(passwordGroup);
    }
    
    // 비밀번호 일치 검사
    if (!validatePasswordMatch()) {
        isValid = false;
    }
    
    // 이용약관 동의 검사
    const agreeTerms = document.getElementById('agreeTerms').checked;
    if (!agreeTerms) {
        showMessage('이용약관에 동의해주세요.', 'error');
        isValid = false;
    }
    
    return isValid;
}

// 회원가입 폼 제출 처리
document.getElementById('signupForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    if (!validateForm()) {
        return;
    }
    
    const formData = new FormData(this);
    const signupData = {
        usrLoginId: formData.get('usrLoginId'),
        usrNm: formData.get('usrNm'),
        email: formData.get('email'),
        password: formData.get('password'),
        phoneNum: formData.get('phoneNum') || null
    };
    
    const signupBtn = document.querySelector('.login-btn');
    const btnText = document.querySelector('.login-btn .btn-text');
    const loadingIcon = document.querySelector('.login-btn .loading-icon');
    
    // 로딩 상태 표시
    signupBtn.disabled = true;
    btnText.style.display = 'none';
    loadingIcon.style.display = 'inline-block';
    
    try {
        const response = await fetch('/api/signup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(signupData)
        });
        
        const result = await response.json();
        
        if (response.ok && result.success) {
            // 회원가입 성공
            showMessage(result.message || '회원가입이 완료되었습니다! 로그인 페이지로 이동합니다.', 'success');
            
            // 2초 후 로그인 페이지로 이동
            setTimeout(() => {
                window.location.href = '/login.html';
            }, 2000);
        } else {
            // 회원가입 실패
            showMessage(result.message || '회원가입에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Signup error:', error);
        showMessage('서버와의 통신에 실패했습니다.', 'error');
    } finally {
        // 로딩 상태 해제
        signupBtn.disabled = false;
        btnText.style.display = 'inline-block';
        loadingIcon.style.display = 'none';
    }
});

// 실시간 유효성 검사
document.getElementById('password').addEventListener('input', function() {
    const password = this.value;
    const strength = checkPasswordStrength(password);
    const strengthText = document.querySelector('.password-strength');
    
    if (strengthText) {
        strengthText.remove();
    }
    
    if (password.length > 0) {
        const strengthDiv = document.createElement('div');
        strengthDiv.className = 'password-strength';
        
        if (strength === 'weak') {
            strengthDiv.textContent = '비밀번호 강도: 약함';
            strengthDiv.className += ' strength-weak';
        } else if (strength === 'medium') {
            strengthDiv.textContent = '비밀번호 강도: 보통';
            strengthDiv.className += ' strength-medium';
        } else {
            strengthDiv.textContent = '비밀번호 강도: 강함';
            strengthDiv.className += ' strength-strong';
        }
        
        this.closest('.form-group').appendChild(strengthDiv);
    }
});

document.getElementById('confirmPassword').addEventListener('input', validatePasswordMatch);

// 메시지 표시 함수
function showMessage(text, type) {
    const messageDiv = document.getElementById('message');
    messageDiv.textContent = text;
    messageDiv.className = `message ${type}`;
    messageDiv.style.display = 'block';
    
    // 5초 후 메시지 숨기기
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 5000);
}

// Enter 키 처리
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        // 아이디 입력 필드에서 Enter 시 중복확인
        if (e.target.id === 'usrLoginId') {
            checkIdDuplicate();
        } else {
            document.getElementById('signupForm').dispatchEvent(new Event('submit'));
        }
    }
});
