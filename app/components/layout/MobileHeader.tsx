'use client';

import { useState } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { Bell, Search, Menu, X, LogOut } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/api/client';
import { authApi } from '@/lib/api/endpoints';

export function MobileHeader() {
  const { user, clearUser, clearTokens } = useAuthStore();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      clearUser();
      clearTokens();
      apiClient.clearAuth();
      router.push('/login');
    }
  };

  return (
    <>
      {/* Header */}
      <header className="fixed top-0 left-0 right-0 z-40 bg-white border-b border-gray-200">
        <div className="flex items-center justify-between px-4 py-3">
          {/* Left: User name */}
          <div className="flex items-center space-x-2">
            <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
              <span className="text-primary-600 font-semibold text-lg">
                {user?.name?.charAt(0) || 'M'}
              </span>
            </div>
            <div>
              <p className="text-sm font-semibold text-gray-900">
                {user?.name || '고객'}님
              </p>
              <p className="text-xs text-gray-500">안녕하세요</p>
            </div>
          </div>

          {/* Right: Icons */}
          <div className="flex items-center space-x-3">
            <button
              className="p-2 hover:bg-gray-100 rounded-full transition-colors relative"
              aria-label="알람"
            >
              <Bell className="w-6 h-6 text-gray-700" />
              {/* Notification badge */}
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
            </button>

            <button
              className="p-2 hover:bg-gray-100 rounded-full transition-colors"
              aria-label="검색"
            >
              <Search className="w-6 h-6 text-gray-700" />
            </button>

            <button
              className="p-2 hover:bg-gray-100 rounded-full transition-colors"
              aria-label="메뉴"
              onClick={() => setIsMenuOpen(!isMenuOpen)}
            >
              {isMenuOpen ? (
                <X className="w-6 h-6 text-gray-700" />
              ) : (
                <Menu className="w-6 h-6 text-gray-700" />
              )}
            </button>
          </div>
        </div>
      </header>

      {/* Dropdown Menu */}
      {isMenuOpen && (
        <>
          {/* Overlay */}
          <div
            className="fixed inset-0 bg-black bg-opacity-30 z-30"
            onClick={() => setIsMenuOpen(false)}
          />

          {/* Menu */}
          <div className="fixed top-16 right-4 z-40 w-64 bg-white rounded-lg shadow-xl border border-gray-200">
            <div className="p-4">
              {/* User Info */}
              <div className="pb-4 border-b border-gray-200">
                <p className="font-semibold text-gray-900">{user?.name || '고객'}</p>
                <p className="text-sm text-gray-500">{user?.email || ''}</p>
              </div>

              {/* Menu Items */}
              <nav className="py-2">
                <button
                  className="w-full text-left px-4 py-3 hover:bg-gray-50 rounded-lg transition-colors text-gray-700"
                  onClick={() => {
                    setIsMenuOpen(false);
                    router.push('/settings');
                  }}
                >
                  설정
                </button>
                <button
                  className="w-full text-left px-4 py-3 hover:bg-gray-50 rounded-lg transition-colors text-gray-700"
                  onClick={() => {
                    setIsMenuOpen(false);
                    router.push('/profile');
                  }}
                >
                  프로필
                </button>
                <button
                  className="w-full text-left px-4 py-3 hover:bg-gray-50 rounded-lg transition-colors text-gray-700"
                  onClick={() => {
                    setIsMenuOpen(false);
                    router.push('/help');
                  }}
                >
                  도움말
                </button>
              </nav>

              {/* Logout */}
              <div className="pt-2 border-t border-gray-200">
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center px-4 py-3 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <LogOut className="w-5 h-5 mr-3" />
                  로그아웃
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
