// 비밀번호 보기/숨기기 토글
function togglePassword() {
    const passwordInput = document.getElementById('password');
    const toggleIcon = document.getElementById('passwordToggleIcon');
    
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

// 로그인 폼 제출 처리
document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const formData = new FormData(this);
    const loginData = {
        usrLoginId: formData.get('usrLoginId'),
        password: formData.get('password')
    };
    
    const loginBtn = document.querySelector('.login-btn');
    const btnText = document.querySelector('.btn-text');
    const loadingIcon = document.querySelector('.loading-icon');
    
    // 로딩 상태 표시
    loginBtn.disabled = true;
    btnText.style.display = 'none';
    loadingIcon.style.display = 'inline-block';
    
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(loginData)
        });
        
        const result = await response.json();
        
        if (response.ok) {
            // 로그인 성공
            showMessage('로그인되었습니다!', 'success');
            
            // 토큰 저장
            localStorage.setItem('accessToken', result.data.accessToken);
            localStorage.setItem('refreshToken', result.data.refreshToken);
            
            // 대시보드로 이동
            setTimeout(() => {
                window.location.href = '/dashboard.html';
            }, 1000);
        } else {
            // 로그인 실패
            showMessage(result.message || '로그인에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showMessage('서버와의 통신에 실패했습니다.', 'error');
    } finally {
        // 로딩 상태 해제
        loginBtn.disabled = false;
        btnText.style.display = 'inline-block';
        loadingIcon.style.display = 'none';
    }
});

// 메시지 표시 함수
function showMessage(text, type) {
    const messageDiv = document.getElementById('message');
    messageDiv.textContent = text;
    messageDiv.className = `message ${type}`;
    messageDiv.style.display = 'block';
    
    // 3초 후 메시지 숨기기
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 3000);
}

// Enter 키 처리
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        document.getElementById('loginForm').dispatchEvent(new Event('submit'));
    }
});
