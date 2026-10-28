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
  const [isLiveBackendReachable, setIsLiveBackendReachable] = useState(true);

  // Check initial connection and refresh current user profile
  useEffect(() => {
    async function initAuth() {
      try {
        Object.keys(localStorage).forEach(k => {
          if (k.startsWith('aives_user_role_') || k.startsWith('aives_custom_topics_')) {
            localStorage.removeItem(k);
          }
        });
      } catch (_) {}

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
          if (err.status === 401) {
            handleLogout();
          } else {
            // BE unreachable: keep the stored session, pages surface their own API errors
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
    try {
      const data = await authApi.login(credentials);
      const actualRole = getUserRole(data.user);

      // Strict role enforcement: If the user selected a specific portal/role,
      // require that the account's actual role matches that selection.
      if (roleOverride && actualRole && actualRole !== roleOverride) {
        // authApi.login already stored the token; drop it so a reload doesn't sign the user in
        authApi.logout();
        const mismatchErr = new Error('ROLE_MISMATCH');
        mismatchErr.code = 'ROLE_MISMATCH';
        mismatchErr.actualRole = actualRole;
        mismatchErr.expectedRole = roleOverride;
        throw mismatchErr;
      }

      const effectiveRole = actualRole || roleOverride || 'ROLE_STUDENT';
      const mergedUser = {
        ...(data.user || {}),
        selectedRole: effectiveRole
      };
      setToken(data.accessToken);
      setCurrentUser(mergedUser);
      localStorage.setItem('aives_user', JSON.stringify(mergedUser));
      localStorage.setItem('aives_user_role', effectiveRole);
      setIsLiveBackendReachable(true);
      return { ...data, user: mergedUser, effectiveRole };
    } catch (err) {
      if (!err.status && err.code !== 'ROLE_MISMATCH') setIsLiveBackendReachable(false);
      throw err;
    }
  };

  const handleRegister = async (payload) => {
    try {
      const data = await authApi.register(payload);
      // Self-registered accounts are always ROLE_STUDENT on the BE
      const actualRole = getUserRole(data.user) || 'ROLE_STUDENT';
      localStorage.setItem('aives_user_role', actualRole);

      const mergedUser = {
        ...(data.user || {}),
        selectedRole: actualRole
      };
      setToken(data.accessToken);
      setCurrentUser(mergedUser);
      localStorage.setItem('aives_user', JSON.stringify(mergedUser));
      setIsLiveBackendReachable(true);
      return { ...data, user: mergedUser, effectiveRole: actualRole };
    } catch (err) {
      if (!err.status) setIsLiveBackendReachable(false);
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

  /**
   * First login after an admin created the account (or reset its password):
   * BE revokes the current token once the password changes, so sign out and ask for a fresh login.
   */
  const completeForcedPasswordChange = () => {
    handleLogout();
  };

  // An account has exactly one role
  const effectiveRole = getUserRole(currentUser) || currentUser?.selectedRole;
  const isAdmin = effectiveRole === 'ROLE_ADMIN';
  const isLecturer = effectiveRole === 'ROLE_LECTURER';
  const mustChangePassword = !!currentUser?.mustChangePassword;

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
        isLiveBackendReachable,
        login: handleLogin,
        register: handleRegister,
        logout: handleLogout,
        refreshProfile,
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
