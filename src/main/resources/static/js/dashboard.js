// 페이지 로드 시 사용자 정보 표시
document.addEventListener('DOMContentLoaded', function() {
    loadUserInfo();
});

// 사용자 정보 로드
function loadUserInfo() {
    const userInfo = JSON.parse(localStorage.getItem('user') || '{}');
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');
    
    if (!userInfo.usrId) {
        // 로그인 정보가 없으면 로그인 페이지로 리다이렉트
        window.location.href = '/login.html';
        return;
    }
    
    // 사용자 정보 표시
    const userInfoDiv = document.getElementById('userInfo');
    userInfoDiv.innerHTML = `
        <div class="info-item">
            <span class="info-label">사용자 ID:</span>
            <span class="info-value">${userInfo.usrId}</span>
        </div>
        <div class="info-item">
            <span class="info-label">아이디:</span>
            <span class="info-value">${userInfo.usrLoginId}</span>
        </div>
        <div class="info-item">
            <span class="info-label">이름:</span>
            <span class="info-value">${userInfo.usrNm}</span>
        </div>
        <div class="info-item">
            <span class="info-label">이메일:</span>
            <span class="info-value">${userInfo.email}</span>
        </div>
        <div class="info-item">
            <span class="info-label">로그인 시간:</span>
            <span class="info-value">${new Date().toLocaleString()}</span>
        </div>
    `;
    
    // 토큰 정보 저장
    if (accessToken) {
        document.getElementById('accessToken').textContent = accessToken;
    }
    
    if (refreshToken) {
        document.getElementById('refreshToken').textContent = refreshToken;
    }
}

// 토큰 정보 보기/숨기기
function showTokenInfo() {
    const tokenInfo = document.getElementById('tokenInfo');
    const button = document.querySelector('.btn-primary');
    
    if (tokenInfo.style.display === 'none') {
        tokenInfo.style.display = 'block';
        button.innerHTML = '<i class="fas fa-eye-slash"></i> 토큰 정보 숨기기';
    } else {
        tokenInfo.style.display = 'none';
        button.innerHTML = '<i class="fas fa-key"></i> 토큰 정보 보기';
    }
}

// 로그아웃
async function logout() {
    if (confirm('정말 로그아웃하시겠습니까?')) {
        try {
            const accessToken = localStorage.getItem('accessToken');
            
            if (accessToken) {
                // 서버에 로그아웃 요청 (Authorization 헤더에 토큰 포함)
                const response = await fetch('/api/logout', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${accessToken}`,
                        'Content-Type': 'application/json',
                    }
                });
                
                const result = await response.json();
                if (result.success) {
                    console.log('로그아웃 성공');
                } else {
                    console.log('로그아웃 요청 실패:', result.message);
                }
            } else {
                console.log('토큰이 없어서 서버 로그아웃을 건너뜁니다.');
            }
        } catch (error) {
            console.error('로그아웃 요청 중 오류:', error);
        } finally {
            // 로컬 스토리지 정리
            localStorage.removeItem('user');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            
            // 로그인 페이지로 리다이렉트
            window.location.href = '/login.html';
        }
    }
}
