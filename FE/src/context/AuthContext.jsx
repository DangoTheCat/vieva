import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi';
import { apiClient } from '../api/client';
import { getUserRole } from '../utils/roles';

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
            selectedRole: getUserRole(profile) || savedRole || (currentUser?.selectedRole ?? 'ROLE_STUDENT')
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
    // BE answers 403/1062 while an admin-issued password has not been changed yet
    const handlePasswordChangeRequired = () => {
      setCurrentUser(prev => prev ? { ...prev, mustChangePassword: true } : prev);
    };
    window.addEventListener('aives:unauthorized', handleUnauthorized);
    window.addEventListener('aives:password-change-required', handlePasswordChangeRequired);
    return () => {
      window.removeEventListener('aives:unauthorized', handleUnauthorized);
      window.removeEventListener('aives:password-change-required', handlePasswordChangeRequired);
    };
  }, []);

  const handleLogin = async (credentials, roleOverride) => {
    const emailKey = credentials?.email ? `aives_user_role_${credentials.email.trim().toLowerCase()}` : null;
    const rememberedRole = emailKey ? localStorage.getItem(emailKey) : null;
    const selectedRole = roleOverride || rememberedRole || localStorage.getItem('aives_user_role') || (credentials?.email?.includes('lecturer') ? 'ROLE_LECTURER' : 'ROLE_STUDENT');

    try {
      const data = await authApi.login(credentials);
      // An account has exactly one role: the BE role wins over the tab picked on the login page
      const actualRole = getUserRole(data.user) || selectedRole;
      if (actualRole) {
        localStorage.setItem('aives_user_role', actualRole);
        if (emailKey) localStorage.setItem(emailKey, actualRole);
      }
      const mergedUser = {
        ...(data.user || {}),
        selectedRole: actualRole
      };
      setToken(data.accessToken);
      setCurrentUser(mergedUser);
      localStorage.setItem('aives_user', JSON.stringify(mergedUser));
      setIsDemoMode(false);
      setIsLiveBackendReachable(true);
      return { ...data, user: mergedUser, effectiveRole: actualRole };
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
      // Self-registered accounts are always ROLE_STUDENT on the BE; its role wins over the picked tab
      const actualRole = getUserRole(data.user) || selectedRole;
      localStorage.setItem('aives_user_role', actualRole);
      if (emailKey) localStorage.setItem(emailKey, actualRole);

      const mergedUser = {
        ...(data.user || {}),
        selectedRole: actualRole
      };
      setToken(data.accessToken);
      setCurrentUser(mergedUser);
      localStorage.setItem('aives_user', JSON.stringify(mergedUser));
      setIsDemoMode(false);
      setIsLiveBackendReachable(true);
      return { ...data, user: mergedUser, effectiveRole: actualRole };
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
        selectedRole: getUserRole(updated) || savedRole || currentUser?.selectedRole || 'ROLE_STUDENT'
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

  /**
   * First login after an admin created the account (or reset its password):
   * BE revokes the current token once the password changes, so sign out and ask for a fresh login.
   */
  const completeForcedPasswordChange = () => {
    handleLogout();
  };

  // An account has exactly one role; the demo (offline) session has none and uses the picked tab
  const effectiveRole = getUserRole(currentUser) || currentUser?.selectedRole;
  const isAdmin = effectiveRole === 'ROLE_ADMIN';
  const isLecturer = effectiveRole === 'ROLE_LECTURER';
  const mustChangePassword = !!currentUser?.mustChangePassword && !isDemoMode;

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        token,
        isLoading,
        isAdmin,
        isLecturer,
        mustChangePassword,
        completeForcedPasswordChange,
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
