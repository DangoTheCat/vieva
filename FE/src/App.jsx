import React, { useState, useEffect, useCallback, Suspense, lazy } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/common/Navbar';
import { Sidebar } from './components/common/Sidebar';
import { Toast } from './components/common/Toast';
import { PageSkeleton } from './components/common/Skeleton';
import { RbacMatrixModal } from './components/users/RbacMatrixModal';
import { CreateUserModal } from './components/users/CreateUserModal';
import { ErrorBoundary } from './components/common/ErrorBoundary';

// Account & Profile Management
const ProfilePage = lazy(() => import('./pages/ProfilePage').then(m => ({ default: m.ProfilePage })));

// Nhóm 7: Quản Trị Hệ Thống (Admin)
const AdminUserPage = lazy(() => import('./pages/AdminUserPage').then(m => ({ default: m.AdminUserPage })));
const AdminSubjectPage = lazy(() => import('./pages/AdminSubjectPage').then(m => ({ default: m.AdminSubjectPage })));
const AdminVoiceLabPage = lazy(() => import('./pages/AdminVoiceLabPage').then(m => ({ default: m.AdminVoiceLabPage })));
const AdminOperationsPage = lazy(() => import('./pages/AdminOperationsPage').then(m => ({ default: m.AdminOperationsPage })));

// Nhóm 1: Quản Lý Ngân Hàng Câu Hỏi & Rubric (Lecturer)
const LecturerQuestionBankPage = lazy(() => import('./pages/LecturerQuestionBankPage').then(m => ({ default: m.LecturerQuestionBankPage })));
const LecturerDocsRagPage = lazy(() => import('./pages/LecturerDocsRagPage').then(m => ({ default: m.LecturerDocsRagPage })));
const RubricStudioPage = lazy(() => import('./pages/RubricStudioPage').then(m => ({ default: m.RubricStudioPage })));
const LecturerDashboardPage = lazy(() => import('./pages/LecturerDashboardPage').then(m => ({ default: m.LecturerDashboardPage })));
const LecturerExamsPage = lazy(() => import('./pages/LecturerExamsPage').then(m => ({ default: m.LecturerExamsPage })));
const LecturerGradingQueuePage = lazy(() => import('./pages/LecturerGradingQueuePage').then(m => ({ default: m.LecturerGradingQueuePage })));
const LecturerReviewRegradingPage = lazy(() => import('./pages/LecturerReviewRegradingPage').then(m => ({ default: m.LecturerReviewRegradingPage })));

// Nhóm 3: Lõi Phỏng Vấn AI (Student - Oral Exam Room)
const DiscoverPage = lazy(() => import('./pages/DiscoverPage').then(m => ({ default: m.DiscoverPage })));
const CalibrationPage = lazy(() => import('./pages/CalibrationPage').then(m => ({ default: m.CalibrationPage })));
const OralExamRoomPage = lazy(() => import('./pages/OralExamRoomPage').then(m => ({ default: m.OralExamRoomPage })));
const StudentResultsPage = lazy(() => import('./pages/StudentResultsPage').then(m => ({ default: m.StudentResultsPage })));

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
  const [isMatrixOpen, setIsMatrixOpen] = useState(false);
  const [isQuickAddOpen, setIsQuickAddOpen] = useState(false);

  const handleOpenProfile = () => {
    setCurrentView('profile');
  };

  // Global toast queue: messages are shown one after another instead of replacing each other
  const [toastQueue, setToastQueue] = useState([]);
  const toast = toastQueue[0] || null;
  const dismissToast = useCallback(() => setToastQueue((queue) => queue.slice(1)), []);

  const showToast = useCallback((toastData) => {
    if (!toastData) return;
    setToastQueue((queue) => {
      const last = queue[queue.length - 1];
      if (last && last.message === toastData.message && last.title === toastData.title) return queue;
      return [...queue, toastData].slice(-4);
    });
  }, []);

  // Views each role may open — Strictly limited to 3 Core Functional Groups + Profile Page
  const allowedViews = [
    ...(isAdmin ? [
      'admin-stats',
      'admin-users', 
      'admin-subjects', 
      'admin-voice-lab', 
      'voice-lab'
    ] : []),
    ...(isAdmin || isLecturer ? [
      'lecturer-dashboard',
      'lecturer-questions',
      'lecturer-docs-rag',
      'rubric-studio',
      'lecturer-exams',
      'lecturer-grading',
      'lecturer-scores'
    ] : []),
    // Student exam flow — BE restricts /student/** to ROLE_STUDENT
    ...(!isAdmin && !isLecturer ? [
      'discover',
      'calibration',
      'exam-room',
      'my-results'
    ] : []),
    'profile',
    'profile-password'
  ];

  const defaultRoleHome = isAdmin ? 'admin-users' : isLecturer ? 'lecturer-dashboard' : 'discover';
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
      <div className="min-h-screen bg-canvasBg flex items-center justify-center p-6 text-ink selection:bg-pink-300 selection:text-ink relative">
        <div className="bg-white border border-pink-200 rounded-2xl p-8 sm:p-10 shadow-md max-w-sm sm:max-w-md w-full text-center space-y-6 animate-in fade-in zoom-in-95 duration-200">
          {/* Brand Mark */}
          <div className="w-14 h-14 rounded-2xl bg-pink-700 text-white flex items-center justify-center shadow-xs mx-auto">
            <svg className="w-7 h-7 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
          </div>

          {/* Heading & Subtitle */}
          <div>
            <span className="font-heading font-black text-3xl tracking-tight text-slate-900 leading-none">
              aives<span className="text-pink-600">.</span>
            </span>
            <span className="block text-xs font-medium text-slate-500 mt-2">
              Hệ thống Khảo thí Vấn đáp Trực tuyến AI • FPT University
            </span>
          </div>

          {/* Loading Indicator */}
          <div className="space-y-3 pt-1">
            <div className="flex items-center justify-center gap-2.5 text-xs font-semibold text-slate-700">
              <span className="w-4 h-4 border-2 border-pink-600 border-t-transparent rounded-full animate-spin"></span>
              <span>Đang khởi tạo AIVES System...</span>
            </div>
            <p className="text-[11px] text-slate-500 font-normal">
              Đang xác thực bảo mật & chuẩn bị dữ liệu bài thi
            </p>

            {/* Clean Progress Bar */}
            <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
              <div className="bg-pink-600 h-full rounded-full animate-pulse w-3/4"></div>
            </div>
          </div>

          {/* Footer security badge */}
          <div className="pt-3 border-t border-slate-100 flex items-center justify-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span className="text-[11px] font-mono font-medium text-slate-400">
              AIVES • FPT University
            </span>
          </div>
        </div>
      </div>
    );
  }

  // Require authentication: if not logged in, present login or register screen
  if (!currentUser) {
    if (currentView === 'register') {
      return (
        <Suspense fallback={<PageSkeleton />}>
          <RegisterPage onNavigate={(view) => setCurrentView(view)} showToast={showToast} />
          <Toast toast={toast} onClose={dismissToast} />
        </Suspense>
      );
    }
    return (
      <Suspense fallback={<PageSkeleton />}>
        <LoginPage onNavigate={(view) => setCurrentView(view)} showToast={showToast} />
        <Toast toast={toast} onClose={dismissToast} />
      </Suspense>
    );
  }

  // Admin-created account (or admin-reset password): replace the temporary password before anything else
  if (mustChangePassword) {
    return (
      <Suspense fallback={<PageSkeleton />}>
        <ForceChangePasswordPage showToast={showToast} />
        <Toast toast={toast} onClose={dismissToast} />
      </Suspense>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-canvasBg text-ink antialiased font-sans selection:bg-pink-300 selection:text-ink">

      {/* GLOBAL TOP NAV */}
      <Navbar
        currentTab={activeView}
        onNavigate={(view) => setCurrentView(view)}
        onOpenAddUser={() => setIsQuickAddOpen(true)}
        onOpenProfile={handleOpenProfile}
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
          {isAdmin && (activeView.startsWith('lecturer-') || activeView === 'rubric-studio') && (
            <div role="status" className="max-w-6xl mx-auto mb-4 px-4 py-2.5 rounded-xl border border-amber-300 bg-amber-50 text-amber-900 text-sm">
              Chế độ xem của quản trị viên: bạn xem được dữ liệu của giảng viên nhưng không tạo, sửa hay duyệt.
              Việc này do giảng viên được phân công môn học thực hiện.
            </div>
          )}
          <ErrorBoundary key={activeView}>
            <Suspense fallback={<PageSkeleton />}>
            {/* Nhóm 7: Quản Trị Hệ Thống (Admin) */}
            {activeView === 'admin-stats' && (
              <AdminOperationsPage
                showToast={showToast}
              />
            )}

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
            {activeView === 'lecturer-dashboard' && (
              <LecturerDashboardPage
                onNavigate={(view) => setCurrentView(view)}
                showToast={showToast}
              />
            )}

            {activeView === 'lecturer-exams' && (
              <LecturerExamsPage
                showToast={showToast}
              />
            )}

            {activeView === 'lecturer-grading' && (
              <LecturerGradingQueuePage
                showToast={showToast}
              />
            )}

            {activeView === 'lecturer-scores' && (
              <LecturerReviewRegradingPage
                showToast={showToast}
              />
            )}

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

            {activeView === 'my-results' && (
              <StudentResultsPage
                showToast={showToast}
              />
            )}

            {/* Quản Lý Hồ Sơ & Bảo Mật Tài Khoản (Dạng Page Toàn Diện) */}
            {(activeView === 'profile' || activeView === 'profile-password') && (
              <ProfilePage
                onNavigate={(view) => setCurrentView(view)}
                showToast={showToast}
                initialTab={activeView === 'profile-password' ? 'password' : 'info'}
              />
            )}
          </Suspense>
        </ErrorBoundary>
      </main>

      </div>

      {/* GLOBAL MODALS */}
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
      <Toast toast={toast} onClose={dismissToast} />

    </div>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ErrorBoundary>
  );
}
