'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { apiClient } from '@/lib/api/client';
import { useAuthStore } from '@/stores/authStore';
import { authApi } from '@/lib/api/endpoints';

function AuthProvider({ children }: { children: React.ReactNode }) {
  const { setUser, clearUser, clearTokens } = useAuthStore();
  const [isRestoring, setIsRestoring] = useState(true);

  useEffect(() => {
    const restoreAuth = async () => {
      try {
        const accessToken = localStorage.getItem('accessToken');
        const refreshToken = localStorage.getItem('refreshToken');

        if (accessToken && refreshToken) {
          console.log('[AuthProvider] Restoring auth from tokens');

          // Set tokens in apiClient first
          apiClient.setAuth(accessToken, refreshToken);

          // Fetch current user info from API
          const response = await authApi.getCurrentUser();

          if (response.success) {
            console.log('[AuthProvider] User restored:', response.data.email);
            setUser(response.data);
          } else {
            console.warn('[AuthProvider] Failed to get user, clearing tokens');
            clearUser();
            clearTokens();
            apiClient.clearAuth();
          }
        }
      } catch (error) {
        console.error('[AuthProvider] Error restoring auth:', error);
        clearUser();
        clearTokens();
        apiClient.clearAuth();
      } finally {
        setIsRestoring(false);
      }
    };

    restoreAuth();
  }, [setUser, clearUser, clearTokens]);

  // Optional: Show loading state while restoring
  if (isRestoring) {
    return <div>Loading...</div>;
  }

  return <>{children}</>;
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000, // 1 minute
            retry: 1,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        {children}
      </AuthProvider>
    </QueryClientProvider>
  );
}
