import React, { useState, useEffect, Suspense, lazy } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/common/Navbar';
import { Sidebar } from './components/common/Sidebar';
import { Toast } from './components/common/Toast';
import { PageSkeleton } from './components/common/Skeleton';
import { ProfileModal } from './components/profile/ProfileModal';
import { RbacMatrixModal } from './components/users/RbacMatrixModal';
import { CreateUserModal } from './components/users/CreateUserModal';

// Nhóm 7: Quản Trị Hệ Thống (Admin)
const AdminUserPage = lazy(() => import('./pages/AdminUserPage').then(m => ({ default: m.AdminUserPage })));
const AdminSubjectPage = lazy(() => import('./pages/AdminSubjectPage').then(m => ({ default: m.AdminSubjectPage })));
const AdminVoiceLabPage = lazy(() => import('./pages/AdminVoiceLabPage').then(m => ({ default: m.AdminVoiceLabPage })));

// Nhóm 1: Quản Lý Ngân Hàng Câu Hỏi & Rubric (Lecturer)
const LecturerQuestionBankPage = lazy(() => import('./pages/LecturerQuestionBankPage').then(m => ({ default: m.LecturerQuestionBankPage })));
const LecturerDocsRagPage = lazy(() => import('./pages/LecturerDocsRagPage').then(m => ({ default: m.LecturerDocsRagPage })));
const RubricStudioPage = lazy(() => import('./pages/RubricStudioPage').then(m => ({ default: m.RubricStudioPage })));

// Nhóm 3: Lõi Phỏng Vấn AI (Student - Oral Exam Room)
const DiscoverPage = lazy(() => import('./pages/DiscoverPage').then(m => ({ default: m.DiscoverPage })));
const CalibrationPage = lazy(() => import('./pages/CalibrationPage').then(m => ({ default: m.CalibrationPage })));
const OralExamRoomPage = lazy(() => import('./pages/OralExamRoomPage').then(m => ({ default: m.OralExamRoomPage })));
const StudentSubmissionSuccessPage = lazy(() => import('./pages/StudentSubmissionSuccessPage').then(m => ({ default: m.StudentSubmissionSuccessPage })));

// Authentication & Security
const LoginPage = lazy(() => import('./pages/LoginPage').then(m => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('./pages/RegisterPage').then(m => ({ default: m.RegisterPage })));
const ForceChangePasswordPage = lazy(() => import('./pages/ForceChangePasswordPage').then(m => ({ default: m.ForceChangePasswordPage })));

function AppContent() {
  const { currentUser, isLoading, isAdmin, isLecturer, mustChangePassword } = useAuth();

  // Helper to parse current view from URL hash (#admin-users -> admin-users)
  const getHashView = () => {
    const hash = window.location.hash.replace(/^#\/?/, '');
    if (hash === 'register' || hash === 'login') return hash;
    return hash || '';
  };

  const [currentView, setCurrentViewState] = useState(getHashView);

  // Synchronize internal view state and browser URL hash
  const setCurrentView = (view) => {
    setCurrentViewState(view);
    if (window.location.hash !== `#${view}`) {
      window.location.hash = `#${view}`;
    }
  };

  // Sync state when user clicks browser back/forward or modifies URL hash
  useEffect(() => {
    const handleHashChange = () => {
      const hash = window.location.hash.replace(/^#\/?/, '');
      if (hash) {
        setCurrentViewState(hash);
      }
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  // Modals state
  const [profileModalState, setProfileModalState] = useState({ isOpen: false, tab: 'info' });
  const [isMatrixOpen, setIsMatrixOpen] = useState(false);
  const [isQuickAddOpen, setIsQuickAddOpen] = useState(false);

  // Global Toast state
  const [toast, setToast] = useState(null);

  // Interactive Voice Lab State
  const [isMicTesting, setIsMicTesting] = useState(false);
  const [selectedVoiceModel, setSelectedVoiceModel] = useState('vi-VN-SophiaNeural');
  const [micVolume, setMicVolume] = useState(0.7);

  const showToast = (toastData) => {
    setToast(toastData);
  };

  // Views each role may open — Strictly limited to 3 Core Functional Groups
  const allowedViews = [
    ...(isAdmin ? [
      'admin-users', 
      'admin-subjects', 
      'admin-voice-lab', 
      'voice-lab'
    ] : []),
    ...(isAdmin || isLecturer ? [
      'lecturer-questions', 
      'lecturer-docs-rag', 
      'rubric-studio'
    ] : []),
    'discover',
    'calibration',
    'exam-room',
    'submission-success'
  ];

  const defaultRoleHome = isAdmin ? 'admin-users' : isLecturer ? 'lecturer-questions' : 'discover';
  const activeView = allowedViews.includes(currentView) ? currentView : defaultRoleHome;

  // Keep browser URL hash synchronized with activeView (must be before any early return)
  useEffect(() => {
    if (isLoading) return;
    if (!currentUser) {
      if (currentView === 'register' && window.location.hash !== '#register') {
        window.location.hash = '#register';
      } else if (currentView !== 'register' && window.location.hash !== '#login') {
        window.location.hash = '#login';
      }
    } else if (activeView && window.location.hash !== `#${activeView}`) {
      window.location.hash = `#${activeView}`;
    }
  }, [isLoading, currentUser, activeView, currentView]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center text-white space-y-3 flex-col">
        <div className="w-10 h-10 border-4 border-sky-400 border-t-transparent rounded-full animate-spin"></div>
        <p className="font-heading text-sm font-semibold tracking-wide">Đang khởi tạo AIVES System...</p>
      </div>
    );
  }

  // Require authentication: if not logged in, present login or register screen
  if (!currentUser) {
    if (currentView === 'register') {
      return (
        <Suspense fallback={<PageSkeleton />}>
          <RegisterPage onNavigate={(view) => setCurrentView(view)} showToast={showToast} />
          <Toast toast={toast} onClose={() => setToast(null)} />
        </Suspense>
      );
    }
    return (
      <Suspense fallback={<PageSkeleton />}>
        <LoginPage onNavigate={(view) => setCurrentView(view)} showToast={showToast} />
        <Toast toast={toast} onClose={() => setToast(null)} />
      </Suspense>
    );
  }

  // Admin-created account (or admin-reset password): replace the temporary password before anything else
  if (mustChangePassword) {
    return (
      <Suspense fallback={<PageSkeleton />}>
        <ForceChangePasswordPage showToast={showToast} />
        <Toast toast={toast} onClose={() => setToast(null)} />
      </Suspense>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-canvasBg text-slate-800 antialiased font-sans selection:bg-sky-500 selection:text-white">

      {/* GLOBAL TOP NAV */}
      <Navbar
        currentTab={activeView}
        onNavigate={(view) => setCurrentView(view)}
        onOpenAddUser={() => setIsQuickAddOpen(true)}
        onOpenProfile={(tab) => setProfileModalState({ isOpen: true, tab })}
        onOpenMatrix={() => setIsMatrixOpen(true)}
      />

      {/* MAIN CONTAINER WITH SIDEBAR */}
      <div className="flex-1 flex w-full">

        {/* LEFT PERSISTENT SIDEBAR */}
        {activeView !== 'calibration' && activeView !== 'exam-room' && (
          <Sidebar
            currentTab={activeView}
            onNavigate={(view) => setCurrentView(view)}
            onOpenMatrix={() => setIsMatrixOpen(true)}
          />
        )}

        {/* CENTER MAIN CANVAS */}
        <main className="flex-1 canvas-dot-grid py-8 px-4 sm:px-8 overflow-y-auto">
          <Suspense fallback={<PageSkeleton />}>
            {/* Nhóm 7: Quản Trị Hệ Thống (Admin) */}
            {activeView === 'admin-users' && (
              <AdminUserPage
                onOpenMatrix={() => setIsMatrixOpen(true)}
                showToast={showToast}
              />
            )}

            {activeView === 'admin-subjects' && (
              <AdminSubjectPage
                showToast={showToast}
              />
            )}

            {(activeView === 'admin-voice-lab' || activeView === 'voice-lab') && (
              <AdminVoiceLabPage
                showToast={showToast}
              />
            )}

            {/* Nhóm 1: Quản Lý Ngân Hàng Câu Hỏi & Rubric (Lecturer) */}
            {activeView === 'lecturer-questions' && (
              <LecturerQuestionBankPage
                showToast={showToast}
              />
            )}

            {activeView === 'lecturer-docs-rag' && (
              <LecturerDocsRagPage
                onNavigate={(view) => setCurrentView(view)}
                showToast={showToast}
              />
            )}

            {activeView === 'rubric-studio' && (
              <RubricStudioPage
                showToast={showToast}
              />
            )}

            {/* Nhóm 3: Lõi Phỏng Vấn AI (Student) */}
            {(activeView === 'discover' || activeView === 'home') && (
              <DiscoverPage
                onNavigate={(view) => setCurrentView(view)}
                showToast={showToast}
              />
            )}

            {activeView === 'calibration' && (
              <CalibrationPage
                onNavigate={(view) => setCurrentView(view)}
                showToast={showToast}
              />
            )}

            {activeView === 'exam-room' && (
              <OralExamRoomPage
                onNavigate={(view) => setCurrentView(view)}
                showToast={showToast}
              />
            )}

            {(activeView === 'submission-success' || activeView === 'exam-success') && (
              <StudentSubmissionSuccessPage
                onNavigate={(view) => setCurrentView(view)}
                showToast={showToast}
              />
            )}
          </Suspense>
        </main>

      </div>

      {/* GLOBAL MODALS */}
      <ProfileModal
        isOpen={profileModalState.isOpen}
        initialTab={profileModalState.tab}
        onClose={() => setProfileModalState({ isOpen: false, tab: 'info' })}
        showToast={showToast}
      />

      <RbacMatrixModal
        isOpen={isMatrixOpen}
        onClose={() => setIsMatrixOpen(false)}
      />

      <CreateUserModal
        isOpen={isQuickAddOpen}
        onClose={() => setIsQuickAddOpen(false)}
        onSuccess={() => {
          showToast({ type: 'success', message: 'Tạo tài khoản thành công!' });
        }}
        showToast={showToast}
      />

      {/* TOAST NOTIFICATION CONTAINER */}
      <Toast toast={toast} onClose={() => setToast(null)} />

    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
