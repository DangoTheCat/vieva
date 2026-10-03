import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi';
import { apiClient } from '../api/client';

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

  // Check initial connection and refresh current user profile
  useEffect(() => {
    async function initAuth() {
      const existingToken = apiClient.getToken();
      if (existingToken) {
        try {
          const profile = await userApi.getCurrentUser();
          setCurrentUser(profile);
          localStorage.setItem('aives_user', JSON.stringify(profile));
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

  const handleLogin = async (credentials) => {
    const data = await authApi.login(credentials);
    setToken(data.accessToken);
    setCurrentUser(data.user);
    localStorage.setItem('aives_user', JSON.stringify(data.user));
    setIsDemoMode(false);
    setIsLiveBackendReachable(true);
    return data;
  };

  const handleRegister = async (payload) => {
    const data = await authApi.register(payload);
    setToken(data.accessToken);
    setCurrentUser(data.user);
    localStorage.setItem('aives_user', JSON.stringify(data.user));
    setIsDemoMode(false);
    setIsLiveBackendReachable(true);
    return data;
  };

  const handleLogout = () => {
    authApi.logout();
    setToken(null);
    setCurrentUser(null);
    localStorage.removeItem('aives_user');
  };

  const refreshProfile = async () => {
    if (!token) return;
    try {
      const updated = await userApi.getCurrentUser();
      setCurrentUser(updated);
      localStorage.setItem('aives_user', JSON.stringify(updated));
      setIsLiveBackendReachable(true);
    } catch (err) {
      console.error('Failed to refresh profile', err);
    }
  };

  const setDemoRole = () => {};

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
