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
        // ipAddr와 userAgent는 서버에서 자동으로 추출됨
    };
    
    const loginBtn = document.querySelector('.login-btn');
    const btnText = document.querySelector('.login-btn .btn-text');
    const loadingIcon = document.querySelector('.login-btn .loading-icon');
    
    // 로딩 상태 표시
    loginBtn.disabled = true;
    btnText.style.display = 'none';
    loadingIcon.style.display = 'inline-block';
    
    try {
        console.log('로그인 요청:', loginData);
        
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(loginData)
        });
        
        const result = await response.json();
        console.log('로그인 응답:', result);
        
        if (response.ok && result.success) {
            // 로그인 성공 - 서버 메시지 우선, 없으면 메시지 코드 사용
            showMessage(result.message || getMessage('FRONT_001'), 'success');
            
            // 사용자 정보 및 토큰 저장 (ApiResponse.data에서 가져오기)
            if (result.data) {
                if (result.data.user) {
                    localStorage.setItem('user', JSON.stringify(result.data.user));
                }
                if (result.data.accessToken) {
                    localStorage.setItem('accessToken', result.data.accessToken);
                }
                if (result.data.refreshToken) {
                    localStorage.setItem('refreshToken', result.data.refreshToken);
                }
            }
            
            // 2초 후 대시보드 페이지로 이동
            setTimeout(() => {
                window.location.href = '/dashboard.html';
            }, 2000);
        } else {
            // 로그인 실패 - 서버 메시지 우선, 없으면 메시지 코드 사용
            showMessage(result.message || getMessage('FRONT_002'), 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showMessage(getMessage('FRONT_003'), 'error');
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
    
    // 5초 후 메시지 숨기기
    setTimeout(() => {
        messageDiv.style.display = 'none';
    }, 5000);
}

// Enter 키 처리
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        document.getElementById('loginForm').dispatchEvent(new Event('submit'));
    }
});

// ==================== 토큰 관리 유틸리티 함수들 ====================

/**
 * Access Token 재발급
 * @returns {Promise<string|null>} 새로운 Access Token 또는 null
 */
async function refreshAccessToken() {
    try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            console.log('Refresh Token이 없습니다.');
            return null;
        }

        const response = await fetch('/api/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ refreshToken: refreshToken })
        });

        const result = await response.json();
        
        if (response.ok && result.success) {
            // 새로운 Access Token 저장 (ApiResponse.data에서 가져오기)
            if (result.data) {
                localStorage.setItem('accessToken', result.data.accessToken);
                
                // 새로운 Refresh Token도 함께 저장 (있는 경우)
                if (result.data.refreshToken) {
                    localStorage.setItem('refreshToken', result.data.refreshToken);
                    console.log('Access Token과 Refresh Token이 재발급되었습니다.');
                } else {
                    console.log('Access Token이 재발급되었습니다.');
                }
                
                return result.data.accessToken;
            }
        } else {
            console.log('토큰 재발급 실패:', result.message);
            // Refresh Token도 무효화된 경우 로그아웃 처리
            if (result.message.includes('유효하지 않은') || result.message.includes('만료')) {
                logout();
            }
            return null;
        }
    } catch (error) {
        console.error('토큰 재발급 중 오류:', error);
        return null;
    }
}

/**
 * 인증이 필요한 API 요청을 위한 헬퍼 함수
 * @param {string} url 요청 URL
 * @param {object} options fetch 옵션
 * @returns {Promise<Response>} fetch 응답
 */
async function authenticatedFetch(url, options = {}) {
    let accessToken = localStorage.getItem('accessToken');
    
    // 기본 헤더 설정
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    // Access Token이 있으면 Authorization 헤더 추가
    if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }
    
    // 첫 번째 요청 시도
    let response = await fetch(url, {
        ...options,
        headers
    });
    
    // 401 Unauthorized 응답이고 Refresh Token이 있는 경우 토큰 재발급 시도
    if (response.status === 401 && localStorage.getItem('refreshToken')) {
        console.log('Access Token 만료, 재발급 시도...');
        
        const newAccessToken = await refreshAccessToken();
        if (newAccessToken) {
            // 새로운 Access Token으로 재요청
            headers['Authorization'] = `Bearer ${newAccessToken}`;
            response = await fetch(url, {
                ...options,
                headers
            });
        }
    }
    
    return response;
}

/**
 * 로그아웃 처리
 */
async function logout() {
    try {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        const usrId = user.usrId;
        
        if (usrId) {
            // 서버에 로그아웃 요청
            await fetch('/api/logout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ usrId: usrId })
            });
        }
    } catch (error) {
        console.error('로그아웃 요청 중 오류:', error);
    } finally {
        // 로컬 스토리지 정리
        localStorage.removeItem('user');
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        
        // 로그인 페이지로 이동
        window.location.href = '/login.html';
    }
}

/**
 * 사용자 정보 조회
 * @returns {Promise<object|null>} 사용자 정보 또는 null
 */
async function getUserInfo() {
    try {
        const response = await authenticatedFetch('/api/user');
        const result = await response.json();
        
        if (response.ok && result.success) {
            return result.data; // ApiResponse.data에서 사용자 정보 가져오기
        } else {
            console.log('사용자 정보 조회 실패:', result.message);
            return null;
        }
    } catch (error) {
        console.error('사용자 정보 조회 중 오류:', error);
        return null;
    }
}
