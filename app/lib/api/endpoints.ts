import { apiClient } from './client';
import {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  AssetSummaryResponse,
  SpendingAnalysisResponse,
  TransferRequest,
  PaymentResponse,
  InvestmentSummaryResponse,
  Transaction,
  User,
} from '@/types/api';

// Authentication endpoints
export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<LoginResponse>('/api/v1/auth/login', data),

  register: (data: RegisterRequest) =>
    apiClient.post<LoginResponse>('/api/v1/auth/register', data),

  logout: () =>
    apiClient.post('/api/v1/auth/logout'),

  refresh: (refreshToken: string) =>
    apiClient.post<{ accessToken: string; refreshToken: string }>('/api/v1/auth/refresh', { refreshToken }),

  getCurrentUser: () =>
    apiClient.get<User>('/api/v1/user/me'),

  kakaoLogin: (code: string) =>
    apiClient.post<LoginResponse>('/api/v1/auth/kakao/callback', { code }),
};

// Asset endpoints (DDD-compliant)
export const assetApi = {
  getAssets: () =>
    apiClient.get<AssetSummaryResponse>('/api/v1/asset/summary'),

  syncAssets: () =>
    apiClient.post('/api/v1/asset/sync'),
};

// Analytics endpoints (DDD-compliant)
export const analyticsApi = {
  getSpendingAnalysis: (daysBack = 30) =>
    apiClient.get<SpendingAnalysisResponse>(`/api/v1/analytics/spending?daysBack=${daysBack}`),
};

// Payment endpoints
export const paymentApi = {
  transfer: (data: TransferRequest) =>
    apiClient.post<PaymentResponse>('/api/v1/payment/transfer', data),

  getPayment: (paymentId: string) =>
    apiClient.get<PaymentResponse>(`/api/v1/payment/${paymentId}`),

  getPaymentHistory: (params?: { page?: number; size?: number }) =>
    apiClient.get<{ payments: PaymentResponse[]; totalPages: number; totalElements: number }>(
      `/api/v1/payment/history${params ? `?${new URLSearchParams(params as any).toString()}` : ''}`
    ),
};

// Investment endpoints
export const investmentApi = {
  getSummary: () =>
    apiClient.get<InvestmentSummaryResponse>('/api/v1/invest/summary'),

  enableRoundUp: (accountId: string) =>
    apiClient.post(`/api/v1/invest/roundup/enable/${accountId}`),

  disableRoundUp: (accountId: string) =>
    apiClient.post(`/api/v1/invest/roundup/disable/${accountId}`),
};
