import React from 'react';
import { AlertTriangle, RotateCcw, Home } from 'lucide-react';

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  handleGoHome = () => {
    this.setState({ hasError: false, error: null });
    window.location.hash = '';
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-[400px] flex items-center justify-center p-6">
          <div className="max-w-md w-full bg-paper border-2 border-ink rounded-2xl p-6 shadow-[4px_4px_0_#1F1F1D] text-center space-y-4">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-error-container border-2 border-error text-error flex items-center justify-center shadow-[2px_2px_0_#ba1a1a]">
              <AlertTriangle className="w-8 h-8" />
            </div>
            <div>
              <h2 className="text-xl font-heading font-bold text-ink">Đã có lỗi xảy ra</h2>
              <p className="text-xs text-on-surface-variant font-medium mt-1">
                Giao diện gặp sự cố khi tải. Bạn có thể thử tải lại trang hoặc quay về trang chủ.
              </p>
              {this.state.error?.message && (
                <div className="mt-3 p-3 bg-surface-container-low border border-outline-variant rounded-xl text-left text-xs font-mono text-error overflow-auto max-h-32">
                  {this.state.error.message}
                </div>
              )}
            </div>
            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                type="button"
                onClick={this.handleReload}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-green-500 hover:bg-green-600 text-ink text-xs font-bold border-2 border-ink shadow-[2px_2px_0_#1F1F1D] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all cursor-pointer"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Tải lại</span>
              </button>
              <button
                type="button"
                onClick={this.handleGoHome}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-surface-container-lowest hover:bg-surface-variant text-ink text-xs font-bold border-2 border-ink shadow-[2px_2px_0_#1F1F1D] active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all cursor-pointer"
              >
                <Home className="w-4 h-4" />
                <span>Về trang chủ</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
