import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse, ApiError } from '@/types/api';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

class ApiClient {
  private client: AxiosInstance;
  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: API_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor - Add JWT token
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = this.accessToken;
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor - Handle errors and token refresh
    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

        // If 401 and not already retried, try to refresh token
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const refreshToken = this.refreshToken;
            if (refreshToken) {
              const { data } = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
                `${API_URL}/api/v1/auth/refresh`,
                { refreshToken }
              );

              if (data.success) {
                // Update tokens in memory
                this.setAuth(data.data.accessToken, data.data.refreshToken);

                // Update authStore
                if (typeof window !== 'undefined') {
                  const { useAuthStore } = await import('@/stores/authStore');
                  useAuthStore.getState().setTokens(data.data.accessToken, data.data.refreshToken);
                }

                if (originalRequest.headers) {
                  originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`;
                }
                return this.client(originalRequest);
              }
            }
          } catch (refreshError) {
            this.clearAuth();
            if (typeof window !== 'undefined') {
              // Clear authStore
              const { useAuthStore } = await import('@/stores/authStore');
              useAuthStore.getState().clearUser();
              useAuthStore.getState().clearTokens();
              window.location.href = '/login';
            }
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  // Generic request methods
  async get<T>(url: string, config = {}) {
    const response = await this.client.get<ApiResponse<T>>(url, config);
    return response.data;
  }

  async post<T>(url: string, data?: unknown, config = {}) {
    const response = await this.client.post<ApiResponse<T>>(url, data, config);
    return response.data;
  }

  async put<T>(url: string, data?: unknown, config = {}) {
    const response = await this.client.put<ApiResponse<T>>(url, data, config);
    return response.data;
  }

  async delete<T>(url: string, config = {}) {
    const response = await this.client.delete<ApiResponse<T>>(url, config);
    return response.data;
  }

  // Auth methods
  setAuth(accessToken: string, refreshToken: string) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  clearAuth() {
    this.accessToken = null;
    this.refreshToken = null;
  }

  isAuthenticated(): boolean {
    return !!this.accessToken;
  }
}

export const apiClient = new ApiClient();
