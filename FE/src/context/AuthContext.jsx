import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi';
import { apiClient } from '../api/client';
import { INITIAL_MOCK_USERS } from '../utils/mockData';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => apiClient.getToken());
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem('aives_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isDemoMode, setIsDemoMode] = useState(false);
  const [isLiveBackendReachable, setIsLiveBackendReachable] = useState(true);

  // Check initial connection and refresh current user profile from BE /api/v1/users/me
  useEffect(() => {
    async function initAuth() {
      const existingToken = apiClient.getToken();
      if (existingToken) {
        try {
          const profile = await userApi.getCurrentUser();
          setCurrentUser(profile);
          localStorage.setItem('aives_user', JSON.stringify(profile));
          setIsLiveBackendReachable(true);
          setIsDemoMode(false);
        } catch (err) {
          if (err.status === 401) {
            handleLogout();
          } else {
            setIsLiveBackendReachable(false);
          }
        }
      } else {
        // No token: user can login/register on live BE
        setIsLiveBackendReachable(true);
      }
      setIsLoading(false);
    }

    initAuth();

    const handleUnauthorized = () => {
      handleLogout();
    };
    window.addEventListener('aives:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('aives:unauthorized', handleUnauthorized);
  }, []);

  const handleLogin = async (credentials) => {
    if (isDemoMode) {
      const matched = INITIAL_MOCK_USERS.find(u => u.email.toLowerCase() === credentials.email.toLowerCase()) || {
        userId: '11111111-1111-1111-1111-111111111111',
        email: credentials.email,
        userCode: 'USR-DEMO',
        fullName: credentials.email.split('@')[0],
        phoneNumber: '0901234567',
        status: 'ACTIVE',
        roles: credentials.email.includes('admin') ? ['ROLE_ADMIN'] : ['ROLE_USER'],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      setCurrentUser(matched);
      localStorage.setItem('aives_user', JSON.stringify(matched));
      return { user: matched, accessToken: 'demo-jwt-token' };
    }

    // Real BE API call: POST /api/v1/auth/login
    const data = await authApi.login(credentials);
    apiClient.setToken(data.accessToken);
    setToken(data.accessToken);
    setCurrentUser(data.user);
    localStorage.setItem('aives_user', JSON.stringify(data.user));
    setIsDemoMode(false);
    setIsLiveBackendReachable(true);
    return data;
  };

  const handleRegister = async (payload) => {
    if (isDemoMode) {
      const newUser = {
        userId: crypto.randomUUID ? crypto.randomUUID() : 'demo-' + Date.now(),
        email: payload.email,
        userCode: 'USR-' + Math.floor(100000 + Math.random() * 900000),
        fullName: payload.fullName,
        phoneNumber: payload.phoneNumber,
        status: 'ACTIVE',
        roles: ['ROLE_USER'],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      setCurrentUser(newUser);
      localStorage.setItem('aives_user', JSON.stringify(newUser));
      return { user: newUser, accessToken: 'demo-jwt-token' };
    }

    // Real BE API call: POST /api/v1/auth/register
    const data = await authApi.register(payload);
    apiClient.setToken(data.accessToken);
    setToken(data.accessToken);
    setCurrentUser(data.user);
    localStorage.setItem('aives_user', JSON.stringify(data.user));
    setIsDemoMode(false);
    setIsLiveBackendReachable(true);
    return data;
  };

  const handleLogout = () => {
    authApi.logout();
    apiClient.setToken(null);
    setToken(null);
    setCurrentUser(null);
    localStorage.removeItem('aives_user');
  };

  const refreshProfile = async () => {
    if (isDemoMode || !apiClient.getToken()) return;
    try {
      const updated = await userApi.getCurrentUser();
      setCurrentUser(updated);
      localStorage.setItem('aives_user', JSON.stringify(updated));
      setIsLiveBackendReachable(true);
    } catch (err) {
      console.error('Failed to refresh profile from BE', err);
    }
  };

  const setDemoRole = (role) => {
    setIsDemoMode(true);
    let target = INITIAL_MOCK_USERS.find(u => u.roles.includes(role));
    if (!target) target = INITIAL_MOCK_USERS[0];
    setCurrentUser(target);
    localStorage.setItem('aives_user', JSON.stringify(target));
  };

  const isAdmin = currentUser?.roles?.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN') ?? false;

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        token,
        isLoading,
        isAdmin,
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
