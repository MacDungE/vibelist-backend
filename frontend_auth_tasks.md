
# Frontend Authentication Task List (Vite + React + TypeScript)

이 문서는 새롭게 구성된 백엔드 API와 연동하여 OAuth2 소셜 로그인을 구현하기 위한 프론트엔드 작업 목록입니다.

**Frontend Stack:**
*   **Framework:** React 18+
*   **Build Tool:** Vite
*   **Language:** TypeScript
*   **Styling:** Tailwind CSS v4
*   **Routing:** `react-router-dom`
*   **API Client:** `axios`

---

## 🏁 1. 프로젝트 초기 설정 (Setup)

### 1.1. 환경 변수 설정
백엔드 API의 기본 URL을 환경 변수로 관리합니다. 프로젝트 루트에 `.env` 파일을 생성하세요.

```bash
# .env

VITE_API_BASE_URL=http://localhost:8081
```
*   **참고:** Vite에서는 `VITE_` 접두사가 붙은 환경 변수만 클라이언트 사이드 코드에서 접근 가능합니다.

### 1.2. 필요 라이브러리 설치
라우팅과 API 통신을 위한 라이브러리를 설치합니다.

```bash
npm install react-router-dom axios
npm install -D @types/react-router-dom
```

### 1.3. API 클라이언트 인스턴스 생성
`axios` 인스턴스를 생성하여 API 요청을 중앙에서 관리합니다. 이 인스턴스는 백엔드와의 통신 시 항상 쿠키를 포함하도록 설정해야 합니다. (백엔드가 `http-only` 리프레시 토큰 쿠키를 사용하기 때문)

```typescript
// src/api/axiosInstance.ts

import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // 쿠키 전송을 위해 필수!
});

// Access Token을 헤더에 추가하는 인터셉터 (5단계에서 구현)
axiosInstance.interceptors.request.use(
  (config) => {
    // Zustand, Redux, Context 등에서 Access Token을 가져옵니다.
    const accessToken = // ... (상태 관리 라이브러리에서 토큰 가져오기)
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);


export default axiosInstance;
```

### 1.4. 라우터 설정
애플리케이션의 전체적인 페이지 라우팅 구조를 설정합니다.

```tsx
// src/main.tsx (or src/App.tsx)

import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import OAuthCallbackPage from './pages/OAuthCallbackPage';
import HomePage from './pages/HomePage';
import ProtectedRoute from './components/ProtectedRoute';
import SocialSignupPage from './pages/SocialSignupPage';
import { AuthProvider } from './context/AuthContext'; // 4단계에서 생성

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* 공개 경로 */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

          {/* 신규 사용자 등록 경로 */}
          <Route path="/social-signup" element={<SocialSignupPage />} />

          {/* 인증이 필요한 경로 */}
          <Route 
            path="/" 
            element={
              <ProtectedRoute>
                <HomePage />
              </ProtectedRoute>
            } 
          />
          {/* ... 기타 인증 필요한 라우트들 */}
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
```

---

## 🎨 2. UI 컴포넌트 구현

### 2.1. 로그인 페이지 (`/login`)
각 소셜 로그인 버튼을 포함하는 페이지입니다. **중요: `onClick` 이벤트로 API를 호출하는 것이 아니라, 백엔드의 OAuth2 인증 시작 URL로 직접 이동시키는 `<a>` 태그를 사용해야 합니다.**

```tsx
// src/pages/LoginPage.tsx

const LoginPage = () => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <h1 className="text-2xl font-bold mb-8">VibeList 로그인</h1>
      <div className="space-y-4">
        <a 
          href={`${apiBaseUrl}/oauth2/authorization/google`}
          className="block w-full text-center bg-red-500 text-white py-2 px-4 rounded"
        >
          Google로 로그인
        </a>
        <a 
          href={`${apiBaseUrl}/oauth2/authorization/kakao`}
          className="block w-full text-center bg-yellow-400 text-black py-2 px-4 rounded"
        >
          카카오로 로그인
        </a>
         <a 
          href={`${apiBaseUrl}/oauth2/authorization/spotify`}
          className="block w-full text-center bg-green-500 text-white py-2 px-4 rounded"
        >
          Spotify로 로그인
        </a>
      </div>
    </div>
  );
};

export default LoginPage;
```

### 2.2. OAuth 콜백 페이지 (`/oauth/callback`)
사용자가 소셜 로그인을 마치고 백엔드에서 리디렉션될 페이지입니다. 이 페이지는 UI가 거의 없으며, 인증 처리를 위한 로직만 수행합니다.

```tsx
// src/pages/OAuthCallbackPage.tsx
import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext'; // 4단계에서 생성

const OAuthCallbackPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();

  useEffect(() => {
    const isNewUser = searchParams.get('isNewUser') === 'true';
    const tempUserId = searchParams.get('tempUserId');
    const provider = searchParams.get('provider');

    if (isNewUser) {
      // 신규 사용자는 추가 정보 입력 페이지로 이동
      navigate(`/social-signup?tempUserId=${tempUserId}&provider=${provider}`);
    } else {
      // 기존 사용자는 Access Token을 요청하고 로그인 처리
      // 백엔드는 http-only 쿠키로 Refresh Token을 이미 설정했음
      login().finally(() => {
        navigate('/'); // 로그인 완료 후 홈으로 이동
      });
    }
  }, [searchParams, navigate, login]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <p>로그인 처리 중...</p>
      {/* 로딩 스피너 등을 표시하면 좋습니다. */}
    </div>
  );
};

export default OAuthCallbackPage;
```

---

## 🔐 3. 전역 상태 관리 (Global State)

### 3.1. 인증 컨텍스트(Auth Context) 생성
`React Context`를 사용하여 로그인 상태, 사용자 정보, Access Token을 전역적으로 관리합니다.

```tsx
// src/context/AuthContext.tsx
import { createContext, useState, useContext, ReactNode, useCallback } from 'react';
import axiosInstance from '../api/axiosInstance';

interface AuthContextType {
  isAuthenticated: boolean;
  accessToken: string | null;
  user: { id: string; name: string } | null; // 사용자 정보 타입은 백엔드에 맞게 확장
  login: () => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthContextType['user']>(null);

  // 백엔드에 Access Token 재발급 요청
  const login = useCallback(async () => {
    try {
      // 백엔드는 http-only 쿠키의 Refresh Token을 사용하여 Access Token을 발급
      const response = await axiosInstance.post('/v1/auth/token/refresh'); 
      const { accessToken: newAccessToken, user: userInfo } = response.data;
      
      setAccessToken(newAccessToken);
      setUser(userInfo);
    } catch (error) {
      console.error('Failed to refresh access token', error);
      setAccessToken(null);
      setUser(null);
    }
  }, []);

  const logout = async () => {
    try {
      await axiosInstance.post('/logout');
    } catch (error) {
      console.error('Logout failed', error);
    } finally {
      setAccessToken(null);
      setUser(null);
    }
  };

  const isAuthenticated = !!accessToken;

  return (
    <AuthContext.Provider value={{ isAuthenticated, accessToken, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```
*   **참고:** `/v1/auth/token/refresh` 엔드포인트는 백엔드에 실제 구현된 경로에 따라 수정해야 합니다.

---

## 🛡️ 4. 라우트 보호 및 인증된 요청

### 4.1. 보호된 라우트 컴포넌트 (`ProtectedRoute`)
인증된 사용자만 접근할 수 있는 페이지를 보호하는 컴포넌트입니다.

```tsx
// src/components/ProtectedRoute.tsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated, isLoading } = useAuth(); // isLoading은 Context에 추가 구현하면 좋음

  // 로딩 중일 때 (예: 최초 토큰 확인 중)
  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

export default ProtectedRoute;
```

### 4.2. 인증된 API 요청
`axiosInstance`를 사용하여 API를 요청하면, 1.3 단계에서 설정한 인터셉터가 자동으로 `Authorization` 헤더에 Access Token을 추가해줍니다.

```tsx
// 페이지 또는 컴포넌트 내에서 API 사용 예시
import axiosInstance from '../api/axiosInstance';

const fetchMyData = async () => {
  try {
    // 인터셉터가 자동으로 헤더에 토큰을 추가해줌
    const response = await axiosInstance.get('/v1/user/me');
    console.log(response.data);
  } catch (error) {
    // 토큰 만료 등의 에러 처리 로직 (예: 401 에러 시 토큰 재발급 시도)
    console.error(error);
  }
};
```

---

## 👋 5. 로그아웃 구현

로그아웃 버튼 클릭 시 `useAuth` 훅의 `logout` 함수를 호출합니다.

```tsx
// src/components/LogoutButton.tsx
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const LogoutButton = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <button onClick={handleLogout} className="bg-gray-500 text-white py-2 px-4 rounded">
      로그아웃
    </button>
  );
};

export default LogoutButton;
```
*   **중요:** `logout` 함수는 백엔드 `/logout` 엔드포인트를 호출하여 서버 세션을 무효화하고, `http-only` 쿠키를 삭제하도록 해야 합니다.

---

## 📝 최종 작업 요약

1.  **로그인 페이지:** 사용자가 소셜 로그인 버튼(`<a>` 태그)을 클릭하면 백엔드 인증 URL로 이동합니다.
2.  **인증 및 리디렉션:** 백엔드가 소셜 제공자와 인증을 처리하고, 성공 시 프론트엔드의 `/oauth/callback`으로 리디렉션합니다. 이때 백엔드는 `http-only` 쿠키에 **리프레시 토큰**을 담아 응답합니다.
3.  **콜백 처리:** `/oauth/callback` 페이지는 URL 쿼리 파라미터를 확인합니다.
    *   **기존 유저:** `login` 함수를 호출하여 백엔드에 **액세스 토큰**을 요청하고, 전역 상태를 업데이트한 후 메인 페이지로 이동합니다.
    *   **신규 유저:** 소셜 정보 추가 입력 페이지(`/social-signup`)로 이동합니다.
4.  **인증 유지:** 사용자는 이제 `isAuthenticated` 상태가 됩니다. `axiosInstance`는 모든 요청에 **액세스 토큰**을 담아 보냅니다.
5.  **로그아웃:** 사용자가 로그아웃 버튼을 클릭하면 백엔드 `/logout`을 호출하여 쿠키를 삭제하고, 프론트엔드의 전역 상태도 초기화합니다.
