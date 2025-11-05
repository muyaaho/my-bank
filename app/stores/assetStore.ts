import { create } from 'zustand';
import { Asset, AssetSummaryResponse, CategoryBreakdown } from '@/types/api';

interface AssetState {
  assets: Asset[];
  totalBalance: number;
  currency: string;
  categoryBreakdown: CategoryBreakdown[];
  isLoading: boolean;
  error: string | null;
  setAssets: (data: AssetSummaryResponse) => void;
  clearAssets: () => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

export const useAssetStore = create<AssetState>((set) => ({
  assets: [],
  totalBalance: 0,
  currency: 'KRW',
  categoryBreakdown: [],
  isLoading: false,
  error: null,
  setAssets: (data) =>
    set({
      assets: data.assets,
      totalBalance: data.totalBalance,
      currency: data.currency,
      categoryBreakdown: data.categoryBreakdown,
      error: null,
    }),
  clearAssets: () =>
    set({
      assets: [],
      totalBalance: 0,
      categoryBreakdown: [],
      error: null,
    }),
  setLoading: (loading) => set({ isLoading: loading }),
  setError: (error) => set({ error, isLoading: false }),
}));
