import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi';
import { apiClient } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => apiClient.getToken());
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem('aives_user');
    const role = localStorage.getItem('aives_user_role');
    if (!saved) return null;
    try {
      const userObj = JSON.parse(saved);
      if (role && !userObj.selectedRole) {
        userObj.selectedRole = role;
      }
      return userObj;
    } catch (e) {
      return null;
    }
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isDemoMode, setIsDemoMode] = useState(false);
  const [isLiveBackendReachable, setIsLiveBackendReachable] = useState(true);

  // Check initial connection and refresh current user profile
  useEffect(() => {
    async function initAuth() {
      const existingToken = apiClient.getToken();
      if (existingToken) {
        try {
          const profile = await userApi.getCurrentUser();
          const savedRole = localStorage.getItem('aives_user_role');
          const mergedUser = {
            ...profile,
            selectedRole: savedRole || profile?.selectedRole || (currentUser?.selectedRole ?? 'ROLE_STUDENT')
          };
          setCurrentUser(mergedUser);
          localStorage.setItem('aives_user', JSON.stringify(mergedUser));
          setIsLiveBackendReachable(true);
        } catch (err) {
          // If 401 or network error
          if (err.status === 401) {
            handleLogout();
          } else {
            // Network unreachable, keep offline user if any
            setIsLiveBackendReachable(false);
          }
        }
      } else {
        // If not logged in, ensure currentUser is null so LoginPage is presented
        setCurrentUser(null);
        localStorage.removeItem('aives_user');
        localStorage.removeItem('aives_user_role');
      }
      setIsLoading(false);
    }

    initAuth();

    // Listen to 401 unauthenticated events
    const handleUnauthorized = () => {
      handleLogout();
    };
    window.addEventListener('aives:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('aives:unauthorized', handleUnauthorized);
  }, []);

  const handleLogin = async (credentials, roleOverride) => {
    const emailKey = credentials?.email ? `aives_user_role_${credentials.email.trim().toLowerCase()}` : null;
    const rememberedRole = emailKey ? localStorage.getItem(emailKey) : null;
    const selectedRole = roleOverride || rememberedRole || localStorage.getItem('aives_user_role') || (credentials?.email?.includes('lecturer') ? 'ROLE_LECTURER' : 'ROLE_STUDENT');

    try {
      const data = await authApi.login(credentials);
      if (selectedRole) {
        localStorage.setItem('aives_user_role', selectedRole);
        if (emailKey) localStorage.setItem(emailKey, selectedRole);
      }
      const mergedUser = {
        ...(data.user || {}),
        selectedRole: selectedRole
      };
      setToken(data.accessToken);
      setCurrentUser(mergedUser);
      localStorage.setItem('aives_user', JSON.stringify(mergedUser));
      setIsDemoMode(false);
      setIsLiveBackendReachable(true);
      return { ...data, user: mergedUser, effectiveRole: selectedRole };
    } catch (err) {
      // If server error (500) or network unreachable, fallback to local offline mode for testing
      if (err.status === 500 || err.status === 502 || err.status === 503 || err.status === 504 || !err.status) {
        console.warn('Backend server error or unreachable, activating local session fallback.', err);
        const fallbackUser = {
          userId: `demo-${Date.now()}`,
          email: credentials?.email || 'user@fpt.edu.vn',
          fullName: selectedRole === 'ROLE_LECTURER' ? 'Giảng Viên (Offline)' : 'Sinh Viên (Offline)',
          selectedRole: selectedRole,
          status: 'ACTIVE'
        };
        const mockToken = `demo-token-${Date.now()}`;
        apiClient.setToken(mockToken);
        setToken(mockToken);
        setCurrentUser(fallbackUser);
        localStorage.setItem('aives_user', JSON.stringify(fallbackUser));
        localStorage.setItem('aives_user_role', selectedRole);
        if (emailKey) localStorage.setItem(emailKey, selectedRole);
        setIsDemoMode(true);
        setIsLiveBackendReachable(false);
        return { accessToken: mockToken, user: fallbackUser, effectiveRole: selectedRole, isDemoFallback: true };
      }
      throw err;
    }
  };

  const handleRegister = async (payload, roleOverride) => {
    const selectedRole = roleOverride || localStorage.getItem('aives_user_role') || 'ROLE_STUDENT';
    const emailKey = payload?.email ? `aives_user_role_${payload.email.trim().toLowerCase()}` : null;

    try {
      const data = await authApi.register(payload);
      localStorage.setItem('aives_user_role', selectedRole);
      if (emailKey) localStorage.setItem(emailKey, selectedRole);

      const mergedUser = {
        ...(data.user || {}),
        selectedRole: selectedRole
      };
      setToken(data.accessToken);
      setCurrentUser(mergedUser);
      localStorage.setItem('aives_user', JSON.stringify(mergedUser));
      setIsDemoMode(false);
      setIsLiveBackendReachable(true);
      return { ...data, user: mergedUser, effectiveRole: selectedRole };
    } catch (err) {
      // Re-throw domain validation errors (e.g., duplicate user 1001)
      if (err.code === '1001' || err.code === 'USER_EXISTED' || (err.status === 400 || err.status === 409)) {
        throw err;
      }
      // If server error (500) or network unreachable, fallback to local offline mode for testing
      if (err.status === 500 || err.status === 502 || err.status === 503 || err.status === 504 || !err.status) {
        console.warn('Backend registration failed with server error, fallback to local session mode.', err);
        const fallbackUser = {
          userId: `demo-${Date.now()}`,
          email: payload.email,
          fullName: payload.fullName || (selectedRole === 'ROLE_LECTURER' ? 'Giảng Viên FPT' : 'Sinh Viên FPT'),
          phoneNumber: payload.phoneNumber || null,
          selectedRole: selectedRole,
          status: 'ACTIVE'
        };
        const mockToken = `demo-token-${Date.now()}`;
        apiClient.setToken(mockToken);
        setToken(mockToken);
        setCurrentUser(fallbackUser);
        localStorage.setItem('aives_user', JSON.stringify(fallbackUser));
        localStorage.setItem('aives_user_role', selectedRole);
        if (emailKey) localStorage.setItem(emailKey, selectedRole);
        setIsDemoMode(true);
        setIsLiveBackendReachable(false);
        return { accessToken: mockToken, user: fallbackUser, effectiveRole: selectedRole, isDemoFallback: true };
      }
      throw err;
    }
  };

  const handleLogout = () => {
    authApi.logout();
    setToken(null);
    setCurrentUser(null);
    localStorage.removeItem('aives_user');
    localStorage.removeItem('aives_user_role');
  };

  const refreshProfile = async () => {
    if (!token) return;
    try {
      const updated = await userApi.getCurrentUser();
      const savedRole = localStorage.getItem('aives_user_role');
      const mergedUser = {
        ...updated,
        selectedRole: savedRole || updated?.selectedRole || currentUser?.selectedRole || 'ROLE_STUDENT'
      };
      setCurrentUser(mergedUser);
      localStorage.setItem('aives_user', JSON.stringify(mergedUser));
      setIsLiveBackendReachable(true);
    } catch (err) {
      console.error('Failed to refresh profile', err);
    }
  };

  const setDemoRole = (role) => {
    if (currentUser) {
      const updated = { ...currentUser, selectedRole: role };
      localStorage.setItem('aives_user_role', role);
      localStorage.setItem('aives_user', JSON.stringify(updated));
      setCurrentUser(updated);
    }
  };

  const isAdmin = currentUser?.roles?.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN') || currentUser?.selectedRole === 'ROLE_ADMIN';
  const isLecturer = currentUser?.roles?.some(r => r === 'ROLE_LECTURER' || r === 'LECTURER') || currentUser?.selectedRole === 'ROLE_LECTURER';

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        token,
        isLoading,
        isAdmin,
        isLecturer,
        isDemoMode,
        isLiveBackendReachable,
        setIsDemoMode,
        login: handleLogin,
        register: handleRegister,
        logout: handleLogout,
        refreshProfile,
        setDemoRole,
        setCurrentUser
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
